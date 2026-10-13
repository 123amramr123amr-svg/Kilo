package com.amrstudio.data.repository

import com.amrstudio.data.database.AmrStudioDatabase
import com.amrstudio.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun createUser(user: User): Result<User> {
        return try {
            val existing = database.userDao().getByEmail(user.email)
            if (existing != null) {
                Result.failure(IllegalArgumentException("Email already registered"))
            } else {
                val phoneExists = database.userDao().getByPhone(user.phone)
                if (phoneExists != null) {
                    Result.failure(IllegalArgumentException("Phone already registered"))
                } else {
                    database.userDao().insert(user)
                    Result.success(user)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun authenticate(email: String, passwordHash: String): Result<User> {
        return try {
            val user = database.userDao().getByEmail(email)
            if (user != null && user.passwordHash == passwordHash && user.isActive) {
                Result.success(user)
            } else {
                Result.failure(IllegalArgumentException("Invalid credentials"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): User? = database.userDao().getById(userId)

    val activeUsers: Flow<List<User>> = database.userDao().getActiveUsers()

    val allUsers: Flow<List<User>> = database.userDao().getAllUsers()

    suspend fun updateUser(user: User): Int = database.userDao().update(user)

    suspend fun deleteUser(userId: String): Int = database.userDao().deleteById(userId)

    suspend fun extendUserExpiry(userId: String, hours: Long): Result<User> {
        return try {
            val user = database.userDao().getById(userId) ?: return Result.failure(IllegalArgumentException("User not found"))
            val newExpiry = if (user.expiresAt != null && user.expiresAt!! > LocalDateTime.now()) {
                user.expiresAt!!.plusHours(hours)
            } else {
                LocalDateTime.now().plusHours(hours)
            }
            val updated = user.copy(expiresAt = newExpiry)
            database.userDao().update(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setAllowedModels(userId: String, modelIds: List<String>): Result<User> {
        return try {
            val user = database.userDao().getById(userId) ?: return Result.failure(IllegalArgumentException("User not found"))
            val updated = user.copy(allowedModelIds = modelIds)
            database.userDao().update(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleUserActive(userId: String, active: Boolean): Result<User> {
        return try {
            val user = database.userDao().getById(userId) ?: return Result.failure(IllegalArgumentException("User not found"))
            val updated = user.copy(isActive = active)
            database.userDao().update(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExpiredUsers(): List<User> = database.userDao().getExpiredUsers(LocalDateTime.now().format(Converters().formatter))
}

@Singleton
class ApiKeyRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun saveApiKey(apiKey: ApiKey): Result<ApiKey> {
        return try {
            database.apiKeyDao().insert(apiKey)
            Result.success(apiKey)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateApiKey(apiKey: ApiKey): Result<ApiKey> {
        return try {
            database.apiKeyDao().update(apiKey)
            Result.success(apiKey)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteApiKey(id: String): Int = database.apiKeyDao().deleteById(id)

    suspend fun getApiKey(id: String): ApiKey? = database.apiKeyDao().getById(id)

    suspend fun getApiKeyByProvider(provider: String): ApiKey? = database.apiKeyDao().getByProvider(provider)

    val activeKeys: Flow<List<ApiKey>> = database.apiKeyDao().getActiveKeys()

    val allKeys: Flow<List<ApiKey>> = database.apiKeyDao().getAllKeys()
}

@Singleton
class ModelRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun saveModel(model: Model): Result<Model> {
        return try {
            database.modelDao().insert(model)
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveModels(models: List<Model>): List<Long> = database.modelDao().insertAll(models)

    suspend fun updateModel(model: Model): Result<Model> {
        return try {
            database.modelDao().update(model)
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteModel(id: String): Int = database.modelDao().deleteById(id)

    suspend fun getModel(id: String): Model? = database.modelDao().getById(id)

    suspend fun getModelsByProvider(providerId: String): List<Model> = database.modelDao().getByProvider(providerId)

    val activeModels: Flow<List<Model>> = database.modelDao().getActiveModels()

    fun getAllowedModels(userId: String): Flow<List<Model>> = database.userDao().getById(userId)
        ?.let { user -> database.modelDao().getAllowedModels(user.allowedModelIds) }
        ?: database.modelDao().getActiveModels()

    val allModels: Flow<List<Model>> = database.modelDao().getAllModels()

    suspend fun setModelActive(id: String, active: Boolean): Int = database.modelDao().setActive(id, active)

    suspend fun setModelOrder(id: String, order: Int): Int = database.modelDao().setOrder(id, order)
}

@Singleton
class ChatRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun createChat(chat: Chat): Result<Chat> {
        return try {
            database.chatDao().insert(chat)
            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChat(chat: Chat): Int = database.chatDao().update(chat)

    suspend fun deleteChat(chatId: String): Int = database.chatDao().deleteById(chatId)

    suspend fun getChat(chatId: String): Chat? = database.chatDao().getById(chatId)

    fun getUserChats(userId: String): Flow<List<Chat>> = database.chatDao().getUserChats(userId)

    fun getAllUserChats(userId: String): Flow<List<Chat>> = database.chatDao().getAllUserChats(userId)

    fun searchChats(userId: String, query: String): Flow<List<Chat>> = database.chatDao().searchChats(userId, "%$query%")
}

@Singleton
class MessageRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun saveMessage(message: Message): Result<Message> {
        return try {
            database.messageDao().insert(message)
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMessages(messages: List<Message>): List<Long> = database.messageDao().insertAll(messages)

    suspend fun updateMessage(message: Message): Int = database.messageDao().update(message)

    suspend fun deleteMessage(messageId: String): Int = database.messageDao().deleteById(messageId)

    suspend fun deleteChatMessages(chatId: String): Int = database.messageDao().deleteByChatId(chatId)

    fun getChatMessages(chatId: String): Flow<List<Message>> = database.messageDao().getChatMessages(chatId)

    suspend fun getStreamingMessages(chatId: String): List<Message> = database.messageDao().getStreamingMessages(chatId)

    suspend fun clearStreamingFlags(chatId: String): Int = database.messageDao().clearStreamingFlags(chatId)
}

@Singleton
class ProjectRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun createProject(project: Project): Result<Project> {
        return try {
            database.projectDao().insert(project)
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProject(project: Project): Int = database.projectDao().update(project)

    suspend fun deleteProject(projectId: String): Int = database.projectDao().deleteById(projectId)

    suspend fun getProject(projectId: String): Project? = database.projectDao().getById(projectId)

    fun getUserProjects(userId: String): Flow<List<Project>> = database.projectDao().getUserProjects(userId)

    fun getAllUserProjects(userId: String): Flow<List<Project>> = database.projectDao().getAllUserProjects(userId)
}

@Singleton
class ProjectFileRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun saveFile(file: ProjectFile): Result<ProjectFile> {
        return try {
            database.projectFileDao().insert(file)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveFiles(files: List<ProjectFile>): List<Long> = database.projectFileDao().insertAll(files)

    suspend fun updateFile(file: ProjectFile): Int = database.projectFileDao().update(file)

    suspend fun deleteFile(fileId: String): Int = database.projectFileDao().deleteById(fileId)

    suspend fun deleteProjectFiles(projectId: String): Int = database.projectFileDao().deleteByProjectId(projectId)

    suspend fun getFile(fileId: String): ProjectFile? = database.projectFileDao().getById(fileId)

    fun getProjectFiles(projectId: String): Flow<List<ProjectFile>> = database.projectFileDao().getProjectFiles(projectId)
}

@Singleton
class ProjectVersionRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun saveVersion(version: ProjectVersion): Result<ProjectVersion> {
        return try {
            database.projectVersionDao().insert(version)
            Result.success(version)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVersion(versionId: String): Int = database.projectVersionDao().deleteById(versionId)

    suspend fun getVersion(projectId: String, versionId: String): ProjectVersion? = database.projectVersionDao().getVersion(projectId, versionId)

    fun getProjectVersions(projectId: String): Flow<List<ProjectVersion>> = database.projectVersionDao().getProjectVersions(projectId)
}

@Singleton
class ConnectorRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun saveConnector(connector: Connector): Result<Connector> {
        return try {
            database.connectorDao().insert(connector)
            Result.success(connector)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateConnector(connector: Connector): Int = database.connectorDao().update(connector)

    suspend fun deleteConnector(id: String): Int = database.connectorDao().deleteById(id)

    suspend fun getConnector(id: String): Connector? = database.connectorDao().getById(id)

    suspend fun getConnectorByType(type: Connector.ConnectorType): Connector? = database.connectorDao().getByType(type)

    val allConnectors: Flow<List<Connector>> = database.connectorDao().getAllConnectors()
}

@Singleton
class AgentTaskRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun createTask(task: AgentTask): Result<AgentTask> {
        return try {
            database.agentTaskDao().insert(task)
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: AgentTask): Int = database.agentTaskDao().update(task)

    suspend fun deleteTask(taskId: String): Int = database.agentTaskDao().deleteById(taskId)

    suspend fun getTask(taskId: String): AgentTask? = database.agentTaskDao().getById(taskId)

    fun getUserTasks(userId: String): Flow<List<AgentTask>> = database.agentTaskDao().getUserTasks(userId)

    fun getActiveTasks(userId: String): Flow<List<AgentTask>> = database.agentTaskDao().getActiveTasks(userId)
}

@Singleton
class DownloadRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun createDownload(download: Download): Result<Download> {
        return try {
            database.downloadDao().insert(download)
            Result.success(download)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDownload(download: Download): Int = database.downloadDao().update(download)

    suspend fun deleteDownload(downloadId: String): Int = database.downloadDao().deleteById(downloadId)

    suspend fun getDownload(downloadId: String): Download? = database.downloadDao().getById(downloadId)

    suspend fun getDownloadByModel(modelId: String): Download? = database.downloadDao().getByModelId(modelId)

    val allDownloads: Flow<List<Download>> = database.downloadDao().getAllDownloads()

    val activeDownloads: Flow<List<Download>> = database.downloadDao().getActiveDownloads()
}

@Singleton
class NotificationRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun createNotification(notification: Notification): Result<Notification> {
        return try {
            database.notificationDao().insert(notification)
            Result.success(notification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: String): Int {
        val notification = database.notificationDao().getById(notificationId)
        return notification?.let {
            database.notificationDao().update(it.copy(isRead = true))
        } ?: 0
    }

    suspend fun markAllAsRead(userId: String): Int = database.notificationDao().markAllAsRead(userId)

    fun getUserNotifications(userId: String): Flow<List<Notification>> = database.notificationDao().getUserNotifications(userId)

    fun getUnreadNotifications(userId: String): Flow<List<Notification>> = database.notificationDao().getUnreadNotifications(userId)
}

@Singleton
class PromptRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun savePrompt(prompt: Prompt): Result<Prompt> {
        return try {
            database.promptDao().insert(prompt)
            Result.success(prompt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePrompt(prompt: Prompt): Int = database.promptDao().update(prompt)

    suspend fun deletePrompt(promptId: String): Int = database.promptDao().deleteById(promptId)

    fun getUserPrompts(userId: String): Flow<List<Prompt>> = database.promptDao().getUserPrompts(userId)

    fun getFavoritePrompts(userId: String): Flow<List<Prompt>> = database.promptDao().getFavoritePrompts(userId)
}

@Singleton
class SettingRepository @Inject constructor(
    private val database: AmrStudioDatabase
) {
    suspend fun setSetting(key: String, value: String): Result<Setting> {
        return try {
            val setting = Setting(key, value)
            database.settingDao().insert(setting)
            Result.success(setting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSetting(key: String): String? = database.settingDao().getByKey(key)?.value

    val allSettings: Flow<List<Setting>> = database.settingDao().getAllSettings()
}