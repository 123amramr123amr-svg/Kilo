package com.personalaiagent.data.backup

import kotlinx.serialization.Serializable

/**
 * Backup file format.
 *
 * Deliberately contains **no credential material**: there is no field for an API
 * key, and [keysIncluded] is always false so a consumer can verify that.
 * DTOs use primitives only, which keeps old backups readable after refactors.
 */
@Serializable
data class BackupBundle(
    val version: Int,
    val exportedAt: Long,
    val appVersion: String,
    val keysIncluded: Boolean = false,
    val settings: BackupSettings? = null,
    val providers: List<BackupProvider> = emptyList(),
    val models: List<BackupModel> = emptyList(),
    val conversations: List<BackupConversation> = emptyList(),
    val messages: List<BackupMessage> = emptyList(),
    val tasks: List<BackupTask> = emptyList(),
    val taskSteps: List<BackupTaskStep> = emptyList(),
)

@Serializable
data class BackupSettings(
    val language: String = "en",
    val theme: String = "MIDNIGHT",
    val ttsEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val streamingEnabled: Boolean = true,
    val onboardingCompleted: Boolean = true,
    val displayName: String = "",
)

@Serializable
data class BackupProvider(
    val id: String,
    val name: String,
    val kind: String,
    val baseUrl: String,
    val isDefault: Boolean,
    val status: String,
    val lastCheckedAt: Long?,
    val createdAt: Long,
)

@Serializable
data class BackupModel(
    val id: String,
    val providerId: String,
    val remoteId: String,
    val displayName: String,
    val capabilities: String,
    val contextWindow: Int?,
    val enabled: Boolean,
    val isDefault: Boolean,
    val temperature: Float,
    val maxTokens: Int,
    val topP: Float,
    val systemPrompt: String,
    val streaming: Boolean,
    val sortOrder: Int,
)

@Serializable
data class BackupConversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val providerId: String?,
    val modelId: String?,
)

@Serializable
data class BackupMessage(
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val status: String,
    val providerId: String? = null,
    val modelId: String? = null,
    val tokenIn: Int? = null,
    val tokenOut: Int? = null,
)

@Serializable
data class BackupTask(
    val id: String,
    val conversationId: String?,
    val prompt: String,
    val status: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val error: String?,
    val providerId: String?,
    val modelId: String?,
    val tokensIn: Int?,
    val tokensOut: Int?,
)

@Serializable
data class BackupTaskStep(
    val id: String,
    val taskId: String,
    val name: String,
    val status: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val detail: String?,
)
