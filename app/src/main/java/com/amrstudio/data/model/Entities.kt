package com.amrstudio.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.amrstudio.data.database.Converters
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
@TypeConverters(Converters::class)
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val isDeveloper: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val expiresAt: LocalDateTime? = null,
    val allowedModelIds: List<String> = emptyList(),
    val trialDurationHours: Int = 24
) {
    val isExpired: Boolean
        get() = expiresAt?.let { it < LocalDateTime.now() } ?: false

    val remainingHours: Long
        get() = expiresAt?.let { LocalDateTime.now().until(it, kotlinx.datetime.TimeUnit.Hours) } ?: 0

    val remainingDays: Long
        get() = expiresAt?.let { LocalDateTime.now().until(it, kotlinx.datetime.TimeUnit.Days) } ?: 0
}

@Serializable
@Entity(tableName = "api_keys")
@TypeConverters(Converters::class)
data class ApiKey(
    @PrimaryKey val id: String,
    val providerName: String,
    val baseUrl: String,
    val apiKeyEncrypted: String,
    val defaultModel: String,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Serializable
@Entity(tableName = "models")
@TypeConverters(Converters::class)
data class Model(
    @PrimaryKey val id: String,
    val name: String,
    val providerId: String,
    val type: ModelType,
    val isFree: Boolean,
    val isActive: Boolean = true,
    val allowedUserIds: List<String> = emptyList(),
    val displayOrder: Int = 0,
    val description: String = "",
    val contextLength: Int = 4096,
    val pricing: String = ""
) {
    enum class ModelType {
        CHAT, CODE, VISION, IMAGE, VIDEO, AUDIO
    }
}

@Serializable
@Entity(tableName = "chats")
@TypeConverters(Converters::class)
data class Chat(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val modelId: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isArchived: Boolean = false
)

@Serializable
@Entity(tableName = "messages")
@TypeConverters(Converters::class)
data class Message(
    @PrimaryKey val id: String,
    val chatId: String,
    val role: MessageRole,
    val content: String,
    val modelId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val isStreaming: Boolean = false,
    val metadata: String = ""
) {
    enum class MessageRole {
        USER, ASSISTANT, SYSTEM, TOOL
    }
}

@Serializable
@Entity(tableName = "projects")
@TypeConverters(Converters::class)
data class Project(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val type: ProjectType,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isArchived: Boolean = false
) {
    enum class ProjectType {
        HTML, LANDING_PAGE, WEB_APP, DASHBOARD, STORE, API
    }
}

@Serializable
@Entity(tableName = "project_files")
@TypeConverters(Converters::class)
data class ProjectFile(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val path: String,
    val content: String,
    val language: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Serializable
@Entity(tableName = "project_versions")
@TypeConverters(Converters::class)
data class ProjectVersion(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val description: String,
    val filesSnapshot: String, // JSON
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Serializable
@Entity(tableName = "connectors")
@TypeConverters(Converters::class)
data class Connector(
    @PrimaryKey val id: String,
    val name: String,
    val type: ConnectorType,
    val configEncrypted: String,
    val isConnected: Boolean = false,
    val lastTestedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    enum class ConnectorType {
        GITHUB, GMAIL, N8N, OPENROUTER, NVIDIA, OLLAMA, NINE_ROUTER, HERMES
    }
}

@Serializable
@Entity(tableName = "agent_tasks")
@TypeConverters(Converters::class)
data class AgentTask(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val plan: String, // JSON
    val currentStep: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null
) {
    enum class TaskStatus {
        PENDING, PLANNING, EXECUTING, PAUSED, COMPLETED, FAILED, CANCELLED
    }
}

@Serializable
@Entity(tableName = "downloads")
@TypeConverters(Converters::class)
data class Download(
    @PrimaryKey val id: String,
    val modelId: String,
    val name: String,
    val url: String,
    val destinationPath: String,
    val totalBytes: Long,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null,
    val errorMessage: String? = null
) {
    enum class DownloadStatus {
        PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
    }

    val progress: Float
        get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
}

@Serializable
@Entity(tableName = "notifications")
@TypeConverters(Converters::class)
data class Notification(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val data: String = "",
    val isRead: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    enum class NotificationType {
        CHAT_COMPLETE, TASK_COMPLETE, DOWNLOAD_COMPLETE, ERROR, INFO, WARNING
    }
}

@Serializable
@Entity(tableName = "prompts")
@TypeConverters(Converters::class)
data class Prompt(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Serializable
@Entity(tableName = "settings")
@TypeConverters(Converters::class)
data class Setting(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: LocalDateTime = LocalDateTime.now()
)