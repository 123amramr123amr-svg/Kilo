package com.personalaiagent.domain.usecase

import com.personalaiagent.core.Outcome
import com.personalaiagent.data.remote.ChatChunk
import com.personalaiagent.data.remote.ChatRequest
import com.personalaiagent.data.remote.ProviderClient
import com.personalaiagent.data.remote.ProviderClientRouter
import com.personalaiagent.data.remote.ProviderException
import com.personalaiagent.domain.model.AppSettings
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.ToolCall
import com.personalaiagent.domain.usecase.agent.AgentToolExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Streams a different chunk list per round, like a real tool-calling model. */
private class ScriptedClient(private val rounds: List<List<ChatChunk>>) : ProviderClient {
    val requests = mutableListOf<ChatRequest>()
    private var index = 0

    override fun stream(request: ChatRequest): Flow<ChatChunk> {
        requests += request
        val chunks = rounds.getOrElse(index++) { listOf(ChatChunk.Delta("done"), ChatChunk.Completed) }
        return flow { chunks.forEach { emit(it) } }
    }

    override suspend fun fetchModels(
        provider: com.personalaiagent.domain.model.Provider,
        apiKey: String,
        baseUrlOverride: String?,
    ): Outcome<List<com.personalaiagent.domain.model.RemoteModel>> = Outcome.Success(emptyList())
}

private class FailingClient : ProviderClient {
    override fun stream(request: ChatRequest): Flow<ChatChunk> =
        flow { throw ProviderException(com.personalaiagent.core.AppError.Network) }

    override suspend fun fetchModels(
        provider: com.personalaiagent.domain.model.Provider,
        apiKey: String,
        baseUrlOverride: String?,
    ): Outcome<List<com.personalaiagent.domain.model.RemoteModel>> = Outcome.Success(emptyList())
}

class AgentLoopTest {

    private val workspace = FakeWorkspaceRepository()
    private val json = Json { ignoreUnknownKeys = true }
    private val chatRepository = FakeChatRepository()
    private val taskRepository = FakeTaskRepository()
    private val credentials = FakeCredentialStore()
    private val settings = FakeSettingsRepository(
        AppSettings(
            workspaceUri = "content://test/tree",
            workspaceName = "Test",
            agentFileToolsEnabled = true,
            onboardingCompleted = true,
        ),
    )

    private fun useCase(client: ProviderClient): SendMessageUseCase {
        credentials.keys["p1"] = "sk-test"
        return SendMessageUseCase(
            chatRepository = chatRepository,
            providerRepository = FakeProviderRepository(SampleData.provider()),
            modelRepository = FakeModelRepository(SampleData.model()),
            credentialStore = credentials,
            taskRepository = taskRepository,
            settingsRepository = settings,
            clients = ProviderClientRouter(client, client),
            toolExecutor = AgentToolExecutor(workspace, json),
            json = json,
            time = FakeTime(),
            ids = FakeIds(),
        )
    }

    @Test
    fun `agent writes a file and reports the tool activity`() = runTest {
        val client = ScriptedClient(
            listOf(
                listOf(
                    ChatChunk.ToolCallChunk(
                        ToolCall("call_1", "write_file", """{"path":"notes/todo.md","content":"- milk"}"""),
                    ),
                    ChatChunk.Completed,
                ),
                listOf(ChatChunk.Delta("Saved notes/todo.md"), ChatChunk.Completed),
            ),
        )

        val events = useCase(client).invoke(null, "write a todo file", null).toList()

        assertEquals("- milk", workspace.files["notes/todo.md"])
        val activity = events.filterIsInstance<SendMessageEvent.ToolActivity>()
        assertEquals(1, activity.size)
        assertTrue(activity.single().ok)
        assertEquals("write_file", activity.single().toolName)
        assertEquals("notes/todo.md", activity.single().detail)
        assertEquals("Saved notes/todo.md", events.filterIsInstance<SendMessageEvent.Completed>().single().fullText)

        // The tools are advertised to the provider and the tool result is fed back.
        assertEquals(6, client.requests.first().tools.size)
        val toolTurn = client.requests.last().messages.last()
        assertEquals(MessageRole.TOOL, toolTurn.role)
        assertEquals("call_1", toolTurn.toolCallId)
    }

    @Test
    fun `a failing tool is reported and the model still answers`() = runTest {
        val client = ScriptedClient(
            listOf(
                listOf(
                    ChatChunk.ToolCallChunk(ToolCall("call_2", "read_file", """{"path":"missing.txt"}""")),
                    ChatChunk.Completed,
                ),
                listOf(ChatChunk.Delta("That file does not exist."), ChatChunk.Completed),
            ),
        )

        val events = useCase(client).invoke(null, "read missing.txt", null).toList()

        val activity = events.filterIsInstance<SendMessageEvent.ToolActivity>().single()
        assertFalse(activity.ok)
        val toolTurn = client.requests.last().messages.last()
        assertTrue(toolTurn.content.startsWith("ERROR"))
        assertEquals("That file does not exist.", events.filterIsInstance<SendMessageEvent.Completed>().single().fullText)
    }

    @Test
    fun `tools are not advertised without a workspace`() = runTest {
        val noWorkspace = FakeSettingsRepository(AppSettings(onboardingCompleted = true))
        val client = ScriptedClient(listOf(listOf(ChatChunk.Delta("hi"), ChatChunk.Completed)))
        credentials.keys["p1"] = "sk-test"
        val useCase = SendMessageUseCase(
            chatRepository = chatRepository,
            providerRepository = FakeProviderRepository(SampleData.provider()),
            modelRepository = FakeModelRepository(SampleData.model()),
            credentialStore = credentials,
            taskRepository = taskRepository,
            settingsRepository = noWorkspace,
            clients = ProviderClientRouter(client, client),
            toolExecutor = AgentToolExecutor(workspace, json),
            json = json,
            time = FakeTime(),
            ids = FakeIds(),
        )

        useCase.invoke(null, "hello", null).toList()

        assertTrue(client.requests.single().tools.isEmpty())
    }

    @Test
    fun `provider failure surfaces as a failed event`() = runTest {
        val events = useCase(FailingClient()).invoke(null, "hello", null).toList()

        val failed = events.filterIsInstance<SendMessageEvent.Failed>().single()
        assertEquals(com.personalaiagent.core.AppError.Network, failed.error)
    }
}
