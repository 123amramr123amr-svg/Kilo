package com.personalaiagent.domain.repository

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
import com.personalaiagent.domain.model.TaskRun
import com.personalaiagent.domain.model.TaskStatus
import com.personalaiagent.domain.model.TaskStepName
import com.personalaiagent.domain.model.TaskStepStatus
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
    suspend fun clearAll()
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
    suspend fun clearAll()
}
