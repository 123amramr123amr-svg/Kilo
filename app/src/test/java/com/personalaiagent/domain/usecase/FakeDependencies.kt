package com.personalaiagent.domain.usecase

import com.personalaiagent.core.AppError
import com.personalaiagent.core.IdGenerator
import com.personalaiagent.core.Outcome
import com.personalaiagent.core.TimeProvider
import com.personalaiagent.data.remote.ChatChunk
import com.personalaiagent.data.remote.ChatRequest
import com.personalaiagent.data.remote.ProviderClient
import com.personalaiagent.domain.model.AiModel
import com.personalaiagent.domain.model.AppLanguage
import com.personalaiagent.domain.model.AppSettings
import com.personalaiagent.domain.model.ChatMessage
import com.personalaiagent.domain.model.Conversation
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.MessageStatus
import com.personalaiagent.domain.model.ModelParams
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.model.ProviderDraft
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.model.ProviderStatus
import com.personalaiagent.domain.model.RemoteModel
import com.personalaiagent.domain.model.TaskRun
import com.personalaiagent.domain.model.TaskStatus
import com.personalaiagent.domain.model.TaskStepName
import com.personalaiagent.domain.model.TaskStepStatus
import com.personalaiagent.domain.model.ThemeMode
import com.personalaiagent.domain.repository.CatalogSyncResult
import com.personalaiagent.domain.repository.ChatRepository
import com.personalaiagent.domain.repository.CredentialStore
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import com.personalaiagent.domain.repository.SettingsRepository
import com.personalaiagent.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Deterministic clock for tests. */
class FakeTime(private var current: Long = 1_000L) : TimeProvider {
    override fun now(): Long {
        current += 10
        return current
    }
}

/** Sequential ids so assertions can be exact. */
class FakeIds : IdGenerator {
    private var counter = 0
    override fun newId(): String = "id-${++counter}"
}

class FakeCredentialStore : CredentialStore {
    val keys = mutableMapOf<String, String>()

    override suspend fun save(providerId: String, apiKey: String) {
        keys[providerId] = apiKey
    }

    override suspend fun read(providerId: String): String? = keys[providerId]
    override suspend fun exists(providerId: String): Boolean = keys.containsKey(providerId)
    override suspend fun delete(providerId: String) {
        keys.remove(providerId)
    }

    override suspend fun clear() = keys.clear()
}

class FakeSettingsRepository(
    initial: AppSettings = AppSettings(language = AppLanguage.ENGLISH, onboardingCompleted = true),
) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = state
    override suspend fun current(): AppSettings = state.value
    override suspend fun setLanguage(language: AppLanguage) = update { it.copy(language = language) }
    override suspend fun setTheme(theme: ThemeMode) = update { it.copy(theme = theme) }
    override suspend fun setTtsEnabled(enabled: Boolean) = update { it.copy(ttsEnabled = enabled) }
    override suspend fun setHapticsEnabled(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }
    override suspend fun setStreamingEnabled(enabled: Boolean) = update { it.copy(streamingEnabled = enabled) }
    override suspend fun setOnboardingCompleted(completed: Boolean) = update { it.copy(onboardingCompleted = completed) }
    override suspend fun setDisplayName(name: String) = update { it.copy(displayName = name) }
    override suspend fun setWorkspace(uri: String?, displayName: String?) =
        update { it.copy(workspaceUri = uri, workspaceName = displayName) }
    override suspend fun setAgentFileToolsEnabled(enabled: Boolean) =
        update { it.copy(agentFileToolsEnabled = enabled) }
    override suspend fun setReviewRequests(enabled: Boolean) =
        update { it.copy(reviewRequests = enabled) }
    override suspend fun setShowAgentActivity(enabled: Boolean) =
        update { it.copy(showAgentActivity = enabled) }
    override suspend fun setPinnedConversations(ids: Set<String>) =
        update { it.copy(pinnedConversations = ids) }
    override suspend fun clearAll() = update { AppSettings() }

    private fun update(transform: (AppSettings) -> AppSettings) {
        state.value = transform(state.value)
    }
}

class FakeChatRepository : ChatRepository {
    val conversations = mutableListOf<Conversation>()
    val messages = mutableListOf<ChatMessage>()
    private val messageFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var idCounter = 0

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageFlow.map { all -> all.filter { it.conversationId == conversationId } }

    override fun observeLatestConversation(): Flow<Conversation?> = flowOf(conversations.lastOrNull())

    override suspend fun getConversation(id: String): Conversation? = conversations.firstOrNull { it.id == id }

    override suspend fun getMessages(conversationId: String): List<ChatMessage> =
        messages.filter { it.conversationId == conversationId }

    override suspend fun recentConversations(
        excludeConversationId: String?,
        limit: Int,
    ): List<Conversation> = conversations
        .filter { it.id != excludeConversationId }
        .sortedByDescending { it.updatedAt }
        .take(limit)

    override suspend fun createConversation(title: String, providerId: String?, modelId: String?): String {
        val id = "conv-${++idCounter}"
        conversations += Conversation(id, title, 1000L, 1000L, providerId, modelId)
        return id
    }

    override suspend fun updateConversationModel(id: String, providerId: String?, modelId: String?) {
        val index = conversations.indexOfFirst { it.id == id }
        if (index >= 0) conversations[index] = conversations[index].copy(providerId = providerId, modelId = modelId)
    }

    override suspend fun appendMessage(message: ChatMessage) {
        messages += message
        messageFlow.value = messages.toList()
    }

    override suspend fun updateAssistantMessage(
        id: String,
        content: String,
        status: MessageStatus,
        error: String?,
        tokenIn: Int?,
        tokenOut: Int?,
    ) {
        val index = messages.indexOfFirst { it.id == id }
        if (index >= 0) {
            messages[index] = messages[index].copy(
                content = content,
                status = status,
                errorMessage = error,
                tokenIn = tokenIn,
                tokenOut = tokenOut,
            )
            messageFlow.value = messages.toList()
        }
    }

    override fun observeConversationSummaries(
        query: String,
    ): Flow<List<com.personalaiagent.domain.model.ConversationSummary>> =
        messageFlow.map { all ->
            conversations
                .filter { conversation ->
                    query.isBlank() ||
                        conversation.title.contains(query, ignoreCase = true) ||
                        all.any {
                            it.conversationId == conversation.id &&
                                it.content.contains(query, ignoreCase = true)
                        }
                }
                .sortedByDescending { it.updatedAt }
                .map { conversation ->
                    val own = all.filter { it.conversationId == conversation.id }
                    com.personalaiagent.domain.model.ConversationSummary(
                        id = conversation.id,
                        title = conversation.title,
                        preview = own.lastOrNull()?.content.orEmpty(),
                        updatedAt = conversation.updatedAt,
                        messageCount = own.size,
                    )
                }
        }

    override fun observeConversationTokens(
        conversationId: String,
    ): Flow<com.personalaiagent.domain.model.TokenUsage> = flowOf(tokenUsage)

    override fun observeTotalTokens(): Flow<com.personalaiagent.domain.model.TokenUsage> =
        flowOf(tokenUsage)

    var tokenUsage = com.personalaiagent.domain.model.TokenUsage()

    override suspend fun renameConversation(id: String, title: String) {
        val index = conversations.indexOfFirst { it.id == id }
        if (index >= 0) conversations[index] = conversations[index].copy(title = title)
    }

    override suspend fun deleteConversation(id: String) {
        conversations.removeAll { it.id == id }
        messages.removeAll { it.conversationId == id }
        messageFlow.value = messages.toList()
    }

    override suspend fun clearAll() {
        conversations.clear()
        messages.clear()
        messageFlow.value = emptyList()
    }
}

class FakeTaskRepository : TaskRepository {
    data class Run(val id: String, val prompt: String, var status: TaskStatus?)
    data class Step(val id: String, val taskId: String, val name: TaskStepName, var status: TaskStepStatus)

    val runs = mutableListOf<Run>()
    val steps = mutableListOf<Step>()
    private var counter = 0

    override fun observeTasks(): Flow<List<TaskRun>> = flowOf(emptyList())
    override fun observeTask(id: String): Flow<TaskRun?> = flowOf(null)
    override suspend fun getTask(id: String): TaskRun? = null

    override suspend fun createTask(
        prompt: String,
        providerId: String?,
        modelId: String?,
        conversationId: String?,
    ): String {
        val id = "task-${++counter}"
        runs += Run(id, prompt, TaskStatus.RUNNING)
        return id
    }

    override suspend fun startStep(taskId: String, name: TaskStepName): String {
        val id = "step-${++counter}"
        steps += Step(id, taskId, name, TaskStepStatus.RUNNING)
        return id
    }

    override suspend fun finishStep(stepId: String, status: TaskStepStatus, detail: String?) {
        steps.firstOrNull { it.id == stepId }?.let { it.status = status }
    }

    override suspend fun finishTask(
        id: String,
        status: TaskStatus,
        error: String?,
        tokensIn: Int?,
        tokensOut: Int?,
    ) {
        runs.firstOrNull { it.id == id }?.status = status
    }

    override suspend fun clearAll() {
        runs.clear()
        steps.clear()
    }
}

class FakeProviderRepository(
    private var provider: Provider?,
    private val availableModels: List<RemoteModel> = emptyList(),
    private val failUpdates: Boolean = false,
    private val failFetch: AppError? = null,
    /** When true, an id that does not match [provider] resolves to null (deleted row). */
    private val strictLookup: Boolean = false,
) : ProviderRepository {

    val updates = mutableListOf<Triple<String, ProviderDraft, String?>>()
    val addedDrafts = mutableListOf<Pair<ProviderDraft, String>>()
    override fun observeProviders(): Flow<List<Provider>> = flowOf(listOfNotNull(provider))
    override fun observeProvider(id: String): Flow<Provider?> = flowOf(provider)
    override suspend fun getProvider(id: String): Provider? =
        if (strictLookup && id != provider?.id) null else provider
    override suspend fun getDefaultProvider(): Provider? = provider
    override suspend fun addProvider(draft: ProviderDraft, apiKey: String): Outcome<String> {
        addedDrafts += draft to apiKey
        return Outcome.Success("p-new")
    }

    override suspend fun updateProvider(id: String, draft: ProviderDraft, apiKey: String?): Outcome<Unit> {
        updates += Triple(id, draft, apiKey)
        return if (failUpdates) Outcome.Failure(AppError.Auth) else Outcome.Success(Unit)
    }

    override suspend fun deleteProvider(id: String): Outcome<Unit> =
        Outcome.Failure(AppError.Unknown("not used in this test"))

    override suspend fun setDefaultProvider(id: String): Outcome<Unit> =
        Outcome.Failure(AppError.Unknown("not used in this test"))

    override suspend fun testConnection(
        providerId: String?,
        draft: ProviderDraft,
        apiKey: String?,
    ): Outcome<com.personalaiagent.domain.model.ConnectionTestResult> =
        Outcome.Failure(AppError.Unknown("not used in this test"))

    override suspend fun fetchAvailableModels(
        providerId: String?,
        draft: ProviderDraft,
        apiKey: String?,
    ): Outcome<List<RemoteModel>> = failFetch?.let { Outcome.Failure(it) } ?: Outcome.Success(availableModels)

    override suspend fun clearAll() {
        provider = null
    }
}

class FakeModelRepository(
    private var model: AiModel?,
    private val failRefresh: AppError? = null,
) : ModelRepository {

    val storedCatalogs = mutableListOf<Pair<String, List<RemoteModel>>>()
    var refreshCalls: Int = 0
        private set
    override fun observeModels(): Flow<List<AiModel>> = flowOf(listOfNotNull(model))
    override fun observeEnabledModels(): Flow<List<AiModel>> = flowOf(listOfNotNull(model?.takeIf { it.enabled }))
    override fun observeModel(id: String): Flow<AiModel?> = flowOf(model)
    override suspend fun getModel(id: String): AiModel? = model?.takeIf { it.id == id }
    override suspend fun getDefaultModel(): AiModel? = model
    override suspend fun firstEnabledModel(): AiModel? = model
    override suspend fun setEnabled(id: String, enabled: Boolean) = Unit
    override suspend fun setDefault(id: String) = Unit
    override suspend fun updateParams(id: String, params: ModelParams) = Unit

    override suspend fun refreshCatalog(providerId: String): Outcome<CatalogSyncResult> {
        refreshCalls++
        failRefresh?.let { return Outcome.Failure(it) }
        return Outcome.Success(CatalogSyncResult(0, fromRemote = true))
    }

    override suspend fun storeCatalog(
        providerId: String,
        models: List<RemoteModel>,
        enabledRemoteIds: Set<String>,
    ): Outcome<Int> {
        storedCatalogs += providerId to models
        return Outcome.Success(models.size)
    }

    override suspend fun applySelection(providerId: String, enabledRemoteIds: Set<String>) = Unit

    override suspend fun clearAll() {
        model = null
    }
}

/** Emits the configured chunks, or fails with the configured error. */
class FakeProviderClient(
    private val chunks: List<ChatChunk> = listOf(ChatChunk.Delta("Hello"), ChatChunk.Completed),
    private val failure: AppError? = null,
    /** Fails this many calls with [transientFailure] before answering normally. */
    private val failingCalls: Int = 0,
    private val transientFailure: AppError = AppError.Network,
) : ProviderClient {

    val requests = mutableListOf<ChatRequest>()

    override fun stream(request: ChatRequest): Flow<ChatChunk> {
        requests += request
        val attempt = requests.size
        return kotlinx.coroutines.flow.flow {
            if (attempt <= failingCalls) throw com.personalaiagent.data.remote.ProviderException(transientFailure)
            failure?.let { throw com.personalaiagent.data.remote.ProviderException(it) }
            chunks.forEach { emit(it) }
        }
    }

    override suspend fun fetchModels(
        provider: Provider,
        apiKey: String,
        baseUrlOverride: String?,
    ): Outcome<List<RemoteModel>> = Outcome.Success(emptyList())
}

object SampleData {
    fun provider(hasKey: Boolean = true) = Provider(
        id = "p1",
        name = "OpenAI",
        kind = ProviderKind.OPENAI_COMPATIBLE,
        baseUrl = "https://api.openai.com/v1",
        isDefault = true,
        hasKey = hasKey,
        status = ProviderStatus.CONNECTED,
        lastCheckedAt = 1_000L,
        createdAt = 1_000L,
    )

    fun model(params: ModelParams = ModelParams(systemPrompt = "Be brief")) = AiModel(
        id = "p1::gpt-4o",
        providerId = "p1",
        providerName = "OpenAI",
        remoteId = "gpt-4o",
        displayName = "gpt-4o",
        capabilities = setOf(com.personalaiagent.domain.model.ModelCapability.TEXT),
        contextWindow = 128_000,
        enabled = true,
        isDefault = true,
        params = params,
    )

    val userRole = MessageRole.USER
}

/**
 * Skills as plain objects: the asset library is replaced so tests never touch
 * Android's AssetManager.
 */
class FakeSkillSource(
    private val skills: List<com.personalaiagent.domain.usecase.agent.Skill> = listOf(
        com.personalaiagent.domain.usecase.agent.Skill(
            name = "code-review",
            description = "Review a change before it ships.",
            body = "Read the diff, then list real problems only.",
        ),
    ),
) : com.personalaiagent.domain.usecase.agent.SkillSource {

    val added = mutableListOf<String>()
    val deleted = mutableListOf<String>()

    override fun all() = skills
    override fun byName(name: String) =
        skills.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    override fun catalog() = skills.joinToString("\n") { "- ${it.name}: ${it.description}" }

    override suspend fun add(name: String, description: String, body: String): Result<com.personalaiagent.domain.usecase.agent.Skill> {
        added += name
        return Result.success(com.personalaiagent.domain.usecase.agent.Skill(name, description, body, custom = true))
    }

    override suspend fun delete(name: String): Result<Unit> {
        deleted += name
        return Result.success(Unit)
    }
}

/** Records the command and returns a canned result. */
class FakeShellExecutor(
    var result: com.personalaiagent.platform.ShellResult =
        com.personalaiagent.platform.ShellResult(0, "total 0", false, "/data/user/0/app/files/agent-home"),
) : com.personalaiagent.platform.ShellExecutor {

    val commands = mutableListOf<String>()
    var lastTimeout: Long = 0

    override suspend fun run(
        command: String,
        workdir: java.io.File,
        timeoutMillis: Long,
        maxOutputChars: Int,
    ): com.personalaiagent.platform.ShellResult {
        commands += command
        lastTimeout = timeoutMillis
        return result
    }

    override fun homeDirectory(): java.io.File = java.io.File("/data/user/0/app/files/agent-home")
}

/**
 * One place that builds the executor with every collaborator faked, so adding a
 * tool dependency does not mean editing a dozen call sites again.
 */
fun testExecutor(
    workspace: FakeWorkspaceRepository = FakeWorkspaceRepository(),
    json: kotlinx.serialization.json.Json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true },
    device: com.personalaiagent.domain.usecase.agent.DeviceControl = FakeDeviceControl(),
    image: com.personalaiagent.domain.usecase.agent.ImageGeneration = FakeImageGeneration(),
    skills: com.personalaiagent.domain.usecase.agent.SkillSource = FakeSkillSource(),
    shell: com.personalaiagent.platform.ShellExecutor = FakeShellExecutor(),
    questions: com.personalaiagent.domain.usecase.agent.QuestionBroker =
        com.personalaiagent.domain.usecase.agent.QuestionBroker(),
    settings: com.personalaiagent.domain.repository.SettingsRepository = FakeSettingsRepository(),
): com.personalaiagent.domain.usecase.agent.AgentToolExecutor =
    com.personalaiagent.domain.usecase.agent.AgentToolExecutor(
        workspace = workspace,
        json = json,
        device = device,
        imageGeneration = image,
        skills = skills,
        shell = shell,
        search = com.personalaiagent.domain.usecase.agent.WorkspaceSearch(workspace),
        questions = questions,
        settings = settings,
    )
