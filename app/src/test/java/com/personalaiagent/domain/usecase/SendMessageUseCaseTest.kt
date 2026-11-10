package com.personalaiagent.domain.usecase

import com.personalaiagent.core.AppError
import com.personalaiagent.data.remote.ChatChunk
import com.personalaiagent.data.remote.ProviderClientRouter
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.MessageStatus
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.model.TaskStatus
import com.personalaiagent.domain.model.TaskStepName
import com.personalaiagent.domain.model.TaskStepStatus
import com.personalaiagent.domain.usecase.agent.AgentToolExecutor
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SendMessageUseCaseTest {

    private val chatRepository = FakeChatRepository()
    private val taskRepository = FakeTaskRepository()
    private val credentials = FakeCredentialStore()
    private val settings = FakeSettingsRepository()
    private val ids = FakeIds()
    private val time = FakeTime()

    private fun useCase(
        client: FakeProviderClient = FakeProviderClient(),
        provider: com.personalaiagent.domain.model.Provider? = SampleData.provider(),
        model: com.personalaiagent.domain.model.AiModel? = SampleData.model(),
    ): SendMessageUseCase {
        if (provider != null) credentials.keys[provider.id] = "sk-test"
        return SendMessageUseCase(
            chatRepository = chatRepository,
            providerRepository = FakeProviderRepository(provider),
            modelRepository = FakeModelRepository(model),
            credentialStore = credentials,
            taskRepository = taskRepository,
            settingsRepository = settings,
            clients = ProviderClientRouter(client, client),
            toolExecutor = testExecutor(workspace = FakeWorkspaceRepository()),
            json = Json { ignoreUnknownKeys = true },
            time = time,
            ids = ids,
            skillSource = FakeSkillSource(),
        )
    }

    @Test
    fun `happy path streams, persists and completes the task`() = runTest {
        val client = FakeProviderClient(
            chunks = listOf(
                ChatChunk.Delta("Hel"),
                ChatChunk.Delta("lo"),
                ChatChunk.Usage(3, 2),
                ChatChunk.Completed,
            ),
        )
        val events = useCase(client).invoke(null, "  Hi there  ", null).toList()

        val started = events.filterIsInstance<SendMessageEvent.Started>().single()
        assertEquals("Hello", events.filterIsInstance<SendMessageEvent.Delta>().joinToString("") { it.text })
        val completed = events.filterIsInstance<SendMessageEvent.Completed>().single()

        assertEquals("Hello", completed.fullText)
        assertEquals(started.conversationId, chatRepository.conversations.single().id)
        assertEquals("Hi there", chatRepository.conversations.single().title)
        assertEquals("Hi there", chatRepository.messages.first { it.role == MessageRole.USER }.content)
        val assistant = chatRepository.messages.first { it.role == MessageRole.ASSISTANT }
        assertEquals("Hello", assistant.content)
        assertEquals(MessageStatus.COMPLETE, assistant.status)
        assertEquals(3, assistant.tokenIn)
        assertEquals(2, assistant.tokenOut)
        assertEquals(TaskStatus.SUCCEEDED, taskRepository.runs.single().status)
    }

    @Test
    fun `every execution step is recorded`() = runTest {
        useCase().invoke(null, "hello", null).toList()

        val names = taskRepository.steps.map { it.name }
        // EXECUTE_TOOL only runs when the agent actually calls a file tool.
        assertTrue(names.containsAll(TaskStepName.entries - TaskStepName.EXECUTE_TOOL))
        assertFalse(names.contains(TaskStepName.EXECUTE_TOOL))
        assertTrue(taskRepository.steps.all { it.status == TaskStepStatus.SUCCEEDED })
    }

    @Test
    fun `sends provider id and configured prompt to the client`() = runTest {
        val client = FakeProviderClient()
        useCase(client).invoke(null, "hello", null).toList()

        val request = client.requests.single()
        assertEquals("gpt-4o", request.modelId)
        assertEquals(ProviderKind.OPENAI_COMPATIBLE, request.kind)
        assertEquals("sk-test", request.apiKey)
        assertEquals(MessageRole.SYSTEM, request.messages.first().role)
        assertEquals("Be brief", request.messages.first().content)
        assertEquals("hello", request.messages.last().content)
    }

    @Test
    fun `blank input fails without creating a task`() = runTest {
        val events = useCase().invoke(null, "   ", null).toList()
        val failed = events.filterIsInstance<SendMessageEvent.Failed>().single()
        assertTrue(failed.error is AppError.InvalidInput)
        assertTrue(taskRepository.runs.isEmpty())
        assertTrue(chatRepository.messages.isEmpty())
    }

    @Test
    fun `missing key fails with provider name`() = runTest {
        val provider = SampleData.provider()
        val useCase = SendMessageUseCase(
            chatRepository = chatRepository,
            providerRepository = FakeProviderRepository(provider),
            modelRepository = FakeModelRepository(SampleData.model()),
            credentialStore = FakeCredentialStore(),
            taskRepository = taskRepository,
            settingsRepository = settings,
            clients = ProviderClientRouter(FakeProviderClient(), FakeProviderClient()),
            toolExecutor = testExecutor(workspace = FakeWorkspaceRepository()),
            json = Json { ignoreUnknownKeys = true },
            time = time,
            ids = ids,
            skillSource = FakeSkillSource(),
        )
        val failed = useCase(null, "hello", null).toList()
            .filterIsInstance<SendMessageEvent.Failed>().single()

        assertEquals(AppError.MissingKey("OpenAI"), failed.error)
        assertTrue(chatRepository.messages.isEmpty())
    }

    @Test
    fun `no model fails fast`() = runTest {
        val events = useCase(model = null).invoke(null, "hello", null).toList()
        assertEquals(AppError.NoModel, events.filterIsInstance<SendMessageEvent.Failed>().single().error)
    }

    @Test
    fun `no provider fails fast`() = runTest {
        val events = useCase(provider = null).invoke(null, "hello", null).toList()
        assertEquals(AppError.NoProvider, events.filterIsInstance<SendMessageEvent.Failed>().single().error)
    }

    @Test
    fun `provider error marks message and task as failed`() = runTest {
        val client = FakeProviderClient(failure = AppError.Auth)
        val events = useCase(client).invoke(null, "hello", null).toList()

        val failed = events.filterIsInstance<SendMessageEvent.Failed>().single()
        assertEquals(AppError.Auth, failed.error)

        val assistant = chatRepository.messages.first { it.role == MessageRole.ASSISTANT }
        assertEquals(MessageStatus.FAILED, assistant.status)
        assertEquals("auth", assistant.errorMessage)
        assertEquals(TaskStatus.FAILED, taskRepository.runs.single().status)
        assertNotNull(assistant.errorMessage)
    }

    @Test
    fun `empty completion is treated as a parse failure`() = runTest {
        val client = FakeProviderClient(chunks = listOf(ChatChunk.Completed))
        val failed = useCase(client).invoke(null, "hello", null).toList()
            .filterIsInstance<SendMessageEvent.Failed>().single()
        assertEquals(AppError.Parse, failed.error)
    }

    @Test
    fun `existing conversation is reused`() = runTest {
        val first = useCase().invoke(null, "first", null).toList()
            .filterIsInstance<SendMessageEvent.Started>().single()
        val second = useCase().invoke(first.conversationId, "second", null).toList()
            .filterIsInstance<SendMessageEvent.Started>().single()

        assertEquals(first.conversationId, second.conversationId)
        assertEquals(1, chatRepository.conversations.size)
        assertEquals(4, chatRepository.messages.size)
    }

    @Test
    fun `a new chat carries context from earlier conversations`() = runTest {
        // Memory is built from stored messages on the device: no extra request.
        chatRepository.conversations += com.personalaiagent.domain.model.Conversation(
            id = "c-old",
            title = "Cairo trip",
            createdAt = 500,
            updatedAt = 900,
            providerId = "p1",
            modelId = "p1::gpt-4o",
        )
        chatRepository.messages += com.personalaiagent.domain.model.ChatMessage(
            id = "m-old",
            conversationId = "c-old",
            role = MessageRole.USER,
            content = "I am planning a trip to Cairo in May",
            createdAt = 501,
            status = MessageStatus.COMPLETE,
        )

        val client = FakeProviderClient()
        useCase(client).invoke(null, "and what about the weather?", null).toList()

        val system = client.requests.single().messages.first { it.role == MessageRole.SYSTEM }
        assertTrue(system.content.contains("Earlier conversations"))
        assertTrue(system.content.contains("Cairo"))
        // The remembered chat stays out of the turn list of the new conversation:
        // it is background context in the system prompt, not fake turns.
        assertTrue(
            client.requests.single().messages
                .filter { it.role != MessageRole.SYSTEM }
                .none { it.content.contains("planning a trip") },
        )
    }

    @Test
    fun `a transient failure is retried once and then succeeds`() = runTest {
        val client = FakeProviderClient(failingCalls = 1)
        val events = useCase(client).invoke(null, "hello", null).toList()

        assertEquals(2, client.requests.size)
        assertEquals("Hello", events.filterIsInstance<SendMessageEvent.Completed>().single().fullText)
    }

    @Test
    fun `a rejected key is not retried`() = runTest {
        val client = FakeProviderClient(failure = AppError.Auth)
        useCase(client).invoke(null, "hello", null).toList()
        assertEquals(1, client.requests.size)
    }

    @Test
    fun `only momentary errors are worth a retry`() {
        assertTrue(AppError.Network.isTransient())
        assertTrue(AppError.Timeout.isTransient())
        assertTrue(AppError.RateLimit().isTransient())
        assertTrue(AppError.Server(503).isTransient())
        assertFalse(AppError.Auth.isTransient())
        assertFalse(AppError.HttpNotFound.isTransient())
        assertFalse(AppError.Server(400).isTransient())
        assertFalse(AppError.Parse.isTransient())
    }
}
