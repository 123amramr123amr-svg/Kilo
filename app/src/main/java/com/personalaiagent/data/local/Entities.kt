package com.personalaiagent.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val baseUrl: String,
    val isDefault: Boolean = false,
    val status: String = "UNKNOWN",
    val lastCheckedAt: Long? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "models",
    indices = [Index("providerId"), Index(value = ["isDefault"])],
    foreignKeys = [
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ModelEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val remoteId: String,
    val displayName: String,
    /** Comma separated [com.personalaiagent.domain.model.ModelCapability] names. */
    val capabilities: String,
    val contextWindow: Int?,
    val enabled: Boolean = true,
    val isDefault: Boolean = false,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val topP: Float = 1f,
    val systemPrompt: String = "",
    val streaming: Boolean = true,
    val sortOrder: Int = 0,
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val providerId: String? = null,
    val modelId: String? = null,
)

@Entity(
    tableName = "messages",
    indices = [Index("conversationId")],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    /** [com.personalaiagent.domain.model.MessageRole] name. */
    val role: String,
    val content: String,
    val createdAt: Long,
    /** [com.personalaiagent.domain.model.MessageStatus] name. */
    val status: String,
    val providerId: String? = null,
    val modelId: String? = null,
    val tokenIn: Int? = null,
    val tokenOut: Int? = null,
    val errorMessage: String? = null,
)

@Entity(tableName = "task_runs", indices = [Index("startedAt")])
data class TaskRunEntity(
    @PrimaryKey val id: String,
    val conversationId: String?,
    val prompt: String,
    /** [com.personalaiagent.domain.model.TaskStatus] name. */
    val status: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val error: String? = null,
    val providerId: String? = null,
    val modelId: String? = null,
    val tokensIn: Int? = null,
    val tokensOut: Int? = null,
)

@Entity(
    tableName = "task_steps",
    indices = [Index("taskId")],
    foreignKeys = [
        ForeignKey(
            entity = TaskRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TaskStepEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    /** [com.personalaiagent.domain.model.TaskStepName] name. */
    val name: String,
    /** [com.personalaiagent.domain.model.TaskStepStatus] name. */
    val status: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val detail: String? = null,
)
