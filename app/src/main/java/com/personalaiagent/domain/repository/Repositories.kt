package com.personalaiagent.domain.repository

import com.personalaiagent.core.AppConstants
import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.model.AiModel
import com.personalaiagent.domain.model.AppLanguage
import com.personalaiagent.domain.model.AppSettings
import com.personalaiagent.domain.model.ChatMessage
import com.personalaiagent.domain.model.Conversation
import com.personalaiagent.domain.model.ConnectionTestResult
import com.personalaiagent.domain.model.ModelParams
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.model.ProviderDraft
import com.personalaiagent.domain.model.RemoteModel
import com.personalaiagent.domain.model.TaskRun
import com.personalaiagent.domain.model.TaskStatus
import com.personalaiagent.domain.model.TaskStepName
import com.personalaiagent.domain.model.TaskStepStatus
import com.personalaiagent.domain.model.Workspace
import com.personalaiagent.domain.model.WorkspaceEntry
import com.personalaiagent.domain.model.WorkspaceFile
import com.personalaiagent.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ProviderRepository {
    fun observeProviders(): Flow<List<Provider>>
    fun observeProvider(id: String): Flow<Provider?>
    suspend fun getProvider(id: String): Provider?
    suspend fun getDefaultProvider(): Provider?
    suspend fun addProvider(draft: ProviderDraft, apiKey: String): Outcome<String>
    suspend fun updateProvider(id: String, draft: ProviderDraft, apiKey: String?): Outcome<Unit>
    suspend fun deleteProvider(id: String): Outcome<Unit>
    suspend fun setDefaultProvider(id: String): Outcome<Unit>
    /**
     * Fetches the model catalog for the given credentials without persisting
     * anything. Used by the "fetch models" button before the provider exists.
     */
    suspend fun fetchAvailableModels(
        providerId: String?,
        draft: ProviderDraft,
        apiKey: String?,
    ): Outcome<List<RemoteModel>>

    /** Tests credentials without persisting anything. [providerId] is null for a new provider. */
    suspend fun testConnection(
        providerId: String?,
        draft: ProviderDraft,
        apiKey: String?,
    ): Outcome<ConnectionTestResult>
    suspend fun clearAll()
}

/**
 * Credential persistence. Implementations must never log key material and must
 * never expose stored values in UI-facing models.
 */
interface CredentialStore {
    suspend fun save(providerId: String, apiKey: String)
    suspend fun read(providerId: String): String?
    suspend fun exists(providerId: String): Boolean
    suspend fun delete(providerId: String)
    suspend fun clear()
}

data class CatalogSyncResult(val modelCount: Int, val fromRemote: Boolean)

interface ModelRepository {
    fun observeModels(): Flow<List<AiModel>>
    fun observeEnabledModels(): Flow<List<AiModel>>
    fun observeModel(id: String): Flow<AiModel?>
    suspend fun getModel(id: String): AiModel?
    suspend fun getDefaultModel(): AiModel?
    suspend fun firstEnabledModel(): AiModel?
    suspend fun setEnabled(id: String, enabled: Boolean)
    suspend fun setDefault(id: String)
    suspend fun updateParams(id: String, params: ModelParams)
    suspend fun refreshCatalog(providerId: String): Outcome<CatalogSyncResult>

    /**
     * Enables exactly [enabledRemoteIds] for the provider and disables the rest.
     * Used by the "fetch models" dialog to apply what the user ticked.
     */
    suspend fun applySelection(providerId: String, enabledRemoteIds: Set<String>)

    /**
     * Stores a catalog that was already fetched by the UI (no network call), then
     * enables exactly [enabledRemoteIds]. Used by the fetch-models dialog so the
     * models the user just saw are the models that get saved.
     */
    suspend fun storeCatalog(
        providerId: String,
        models: List<RemoteModel>,
        enabledRemoteIds: Set<String>,
    ): Outcome<Int>
    suspend fun clearAll()
}

/**
 * File access for the agent. The implementation is backed by the Storage Access
 * Framework tree the user picked, so every operation stays inside that folder and
 * needs no broad storage permission.
 */
interface WorkspaceRepository {
    fun observeWorkspace(): Flow<Workspace?>

    /** Persists the tree URI and takes a persistable read/write permission. */
    suspend fun connect(treeUri: String): Outcome<Workspace>

    suspend fun disconnect()

    /** Lists a directory (`""` or "/" = workspace root). */
    suspend fun list(path: String): Outcome<List<WorkspaceEntry>>

    suspend fun read(path: String, maxBytes: Int = AppConstants.MAX_TOOL_READ_BYTES): Outcome<WorkspaceFile>

    /** Creates or overwrites a text file, creating parent folders as needed. */
    suspend fun write(path: String, content: String, append: Boolean = false): Outcome<WorkspaceEntry>

    /** Creates or overwrites a binary file (generated images, exports). */
    suspend fun writeBytes(path: String, bytes: ByteArray, mimeType: String): Outcome<WorkspaceEntry>

    suspend fun createFolder(path: String): Outcome<WorkspaceEntry>

    suspend fun delete(path: String): Outcome<Unit>
}

interface ChatRepository {
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
    fun observeLatestConversation(): Flow<Conversation?>
    suspend fun getConversation(id: String): Conversation?
    suspend fun getMessages(conversationId: String): List<ChatMessage>
    suspend fun createConversation(title: String, providerId: String?, modelId: String?): String
    suspend fun updateConversationModel(id: String, providerId: String?, modelId: String?)
    suspend fun appendMessage(message: ChatMessage)
    suspend fun updateAssistantMessage(
        id: String,
        content: String,
        status: com.personalaiagent.domain.model.MessageStatus,
        error: String? = null,
        tokenIn: Int? = null,
        tokenOut: Int? = null,
    )
    suspend fun deleteConversation(id: String)
    suspend fun clearAll()
}

interface TaskRepository {
    fun observeTasks(): Flow<List<TaskRun>>
    fun observeTask(id: String): Flow<TaskRun?>
    suspend fun getTask(id: String): TaskRun?
    suspend fun createTask(prompt: String, providerId: String?, modelId: String?, conversationId: String?): String
    suspend fun startStep(taskId: String, name: TaskStepName): String
    suspend fun finishStep(stepId: String, status: TaskStepStatus, detail: String? = null)
    suspend fun finishTask(
        id: String,
        status: TaskStatus,
        error: String? = null,
        tokensIn: Int? = null,
        tokensOut: Int? = null,
    )
    suspend fun clearAll()
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setTheme(theme: ThemeMode)
    suspend fun setTtsEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setStreamingEnabled(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setDisplayName(name: String)
    suspend fun setWorkspace(uri: String?, displayName: String?)
    suspend fun setAgentFileToolsEnabled(enabled: Boolean)
    suspend fun setReviewRequests(enabled: Boolean)
    suspend fun clearAll()
}
