package com.amrstudio.data.database

import androidx.room.*
import com.amrstudio.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User): Int

    @Delete
    suspend fun delete(user: User): Int

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): User?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE phone = :phone")
    suspend fun getByPhone(phone: String): User?

    @Query("SELECT * FROM users WHERE isDeveloper = 1")
    suspend fun getDevelopers(): List<User>

    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveUsers(): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE isActive = 1 AND expiresAt < :now")
    suspend fun getExpiredUsers(now: String): List<User>
}

@Dao
interface ApiKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apiKey: ApiKey): Long

    @Update
    suspend fun update(apiKey: ApiKey): Int

    @Delete
    suspend fun delete(apiKey: ApiKey): Int

    @Query("DELETE FROM api_keys WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM api_keys WHERE id = :id")
    suspend fun getById(id: String): ApiKey?

    @Query("SELECT * FROM api_keys WHERE providerName = :providerName")
    suspend fun getByProvider(providerName: String): ApiKey?

    @Query("SELECT * FROM api_keys WHERE isActive = 1")
    fun getActiveKeys(): Flow<List<ApiKey>>

    @Query("SELECT * FROM api_keys ORDER BY createdAt DESC")
    fun getAllKeys(): Flow<List<ApiKey>>
}

@Dao
interface ModelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(model: Model): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(models: List<Model>): List<Long>

    @Update
    suspend fun update(model: Model): Int

    @Delete
    suspend fun delete(model: Model): Int

    @Query("DELETE FROM models WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM models WHERE id = :id")
    suspend fun getById(id: String): Model?

    @Query("SELECT * FROM models WHERE providerId = :providerId")
    suspend fun getByProvider(providerId: String): List<Model>

    @Query("SELECT * FROM models WHERE isActive = 1 ORDER BY displayOrder, name")
    fun getActiveModels(): Flow<List<Model>>

    @Query("SELECT * FROM models WHERE isActive = 1 AND id IN (:allowedIds) ORDER BY displayOrder, name")
    fun getAllowedModels(allowedIds: List<String>): Flow<List<Model>>

    @Query("SELECT * FROM models ORDER BY displayOrder, name")
    fun getAllModels(): Flow<List<Model>>

    @Query("UPDATE models SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: String, active: Boolean): Int

    @Query("UPDATE models SET displayOrder = :order WHERE id = :id")
    suspend fun setOrder(id: String, order: Int): Int
}

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chat: Chat): Long

    @Update
    suspend fun update(chat: Chat): Int

    @Delete
    suspend fun delete(chat: Chat): Int

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getById(id: String): Chat?

    @Query("SELECT * FROM chats WHERE userId = :userId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getUserChats(userId: String): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllUserChats(userId: String): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE userId = :userId AND title LIKE :query ORDER BY updatedAt DESC")
    fun searchChats(userId: String, query: String): Flow<List<Chat>>
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<Message>): List<Long>

    @Update
    suspend fun update(message: Message): Int

    @Delete
    suspend fun delete(message: Message): Int

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteByChatId(chatId: String): Int

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    fun getChatMessages(chatId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isStreaming = 1 ORDER BY createdAt ASC")
    suspend fun getStreamingMessages(chatId: String): List<Message>

    @Query("UPDATE messages SET isStreaming = 0 WHERE chatId = :chatId AND isStreaming = 1")
    suspend fun clearStreamingFlags(chatId: String): Int
}

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project): Int

    @Delete
    suspend fun delete(project: Project): Int

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): Project?

    @Query("SELECT * FROM projects WHERE userId = :userId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getUserProjects(userId: String): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllUserProjects(userId: String): Flow<List<Project>>
}

@Dao
interface ProjectFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: ProjectFile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<ProjectFile>): List<Long>

    @Update
    suspend fun update(file: ProjectFile): Int

    @Delete
    suspend fun delete(file: ProjectFile): Int

    @Query("DELETE FROM project_files WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM project_files WHERE projectId = :projectId")
    suspend fun deleteByProjectId(projectId: String): Int

    @Query("SELECT * FROM project_files WHERE id = :id")
    suspend fun getById(id: String): ProjectFile?

    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY path, name")
    fun getProjectFiles(projectId: String): Flow<List<ProjectFile>>
}

@Dao
interface ProjectVersionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(version: ProjectVersion): Long

    @Delete
    suspend fun delete(version: ProjectVersion): Int

    @Query("DELETE FROM project_versions WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM project_versions WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getProjectVersions(projectId: String): Flow<List<ProjectVersion>>

    @Query("SELECT * FROM project_versions WHERE projectId = :projectId AND id = :id")
    suspend fun getVersion(projectId: String, id: String): ProjectVersion?
}

@Dao
interface ConnectorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connector: Connector): Long

    @Update
    suspend fun update(connector: Connector): Int

    @Delete
    suspend fun delete(connector: Connector): Int

    @Query("DELETE FROM connectors WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM connectors WHERE id = :id")
    suspend fun getById(id: String): Connector?

    @Query("SELECT * FROM connectors WHERE type = :type")
    suspend fun getByType(type: Connector.ConnectorType): Connector?

    @Query("SELECT * FROM connectors ORDER BY createdAt DESC")
    fun getAllConnectors(): Flow<List<Connector>>
}

@Dao
interface AgentTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: AgentTask): Long

    @Update
    suspend fun update(task: AgentTask): Int

    @Delete
    suspend fun delete(task: AgentTask): Int

    @Query("DELETE FROM agent_tasks WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM agent_tasks WHERE id = :id")
    suspend fun getById(id: String): AgentTask?

    @Query("SELECT * FROM agent_tasks WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserTasks(userId: String): Flow<List<AgentTask>>

    @Query("SELECT * FROM agent_tasks WHERE userId = :userId AND status IN ('EXECUTING', 'PLANNING', 'PAUSED')")
    fun getActiveTasks(userId: String): Flow<List<AgentTask>>
}

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download): Long

    @Update
    suspend fun update(download: Download): Int

    @Delete
    suspend fun delete(download: Download): Int

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: String): Download?

    @Query("SELECT * FROM downloads WHERE modelId = :modelId")
    suspend fun getByModelId(modelId: String): Download?

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<Download>>

    @Query("SELECT * FROM downloads WHERE status IN ('DOWNLOADING', 'PAUSED', 'PENDING')")
    fun getActiveDownloads(): Flow<List<Download>>
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: Notification): Long

    @Update
    suspend fun update(notification: Notification): Int

    @Delete
    suspend fun delete(notification: Notification): Int

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserNotifications(userId: String): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE userId = :userId AND isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadNotifications(userId: String): Flow<List<Notification>>

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String): Int
}

@Dao
interface PromptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prompt: Prompt): Long

    @Update
    suspend fun update(prompt: Prompt): Int

    @Delete
    suspend fun delete(prompt: Prompt): Int

    @Query("DELETE FROM prompts WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM prompts WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getUserPrompts(userId: String): Flow<List<Prompt>>

    @Query("SELECT * FROM prompts WHERE userId = :userId AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoritePrompts(userId: String): Flow<List<Prompt>>
}

@Dao
interface SettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: Setting): Long

    @Query("SELECT * FROM settings WHERE key = :key")
    suspend fun getByKey(key: String): Setting?

    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<Setting>>

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteByKey(key: String): Int
}