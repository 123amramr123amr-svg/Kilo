package com.personalaiagent.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {

    @Query("SELECT * FROM providers ORDER BY isDefault DESC, createdAt ASC")
    fun observeAll(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE id = :id")
    fun observeById(id: String): Flow<ProviderEntity?>

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getById(id: String): ProviderEntity?

    @Query("SELECT * FROM providers ORDER BY createdAt ASC")
    suspend fun getAll(): List<ProviderEntity>

    @Query("SELECT * FROM providers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): ProviderEntity?

    @Query("SELECT * FROM providers ORDER BY createdAt ASC LIMIT 1")
    suspend fun getFirst(): ProviderEntity?

    @Query("SELECT COUNT(*) FROM providers")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProviderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ProviderEntity>)

    @Update
    suspend fun update(entity: ProviderEntity)

    @Query("UPDATE providers SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE providers SET isDefault = 1 WHERE id = :id")
    suspend fun markDefault(id: String)

    @Query("UPDATE providers SET status = :status, lastCheckedAt = :checkedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, checkedAt: Long)

    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM providers")
    suspend fun clear()
}

@Dao
interface ModelDao {

    @Query("SELECT * FROM models ORDER BY sortOrder ASC, displayName ASC")
    fun observeAll(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE enabled = 1 ORDER BY isDefault DESC, displayName ASC")
    fun observeEnabled(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE id = :id")
    fun observeById(id: String): Flow<ModelEntity?>

    @Query("SELECT * FROM models ORDER BY sortOrder ASC")
    suspend fun getAll(): List<ModelEntity>

    @Query("SELECT * FROM models WHERE id = :id")
    suspend fun getById(id: String): ModelEntity?

    @Query("SELECT * FROM models WHERE providerId = :providerId ORDER BY sortOrder ASC")
    suspend fun getByProvider(providerId: String): List<ModelEntity>

    @Query("SELECT * FROM models WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): ModelEntity?

    @Query("SELECT * FROM models WHERE enabled = 1 ORDER BY isDefault DESC, sortOrder ASC LIMIT 1")
    suspend fun firstEnabled(): ModelEntity?

    @Query("SELECT COUNT(*) FROM models WHERE providerId = :providerId")
    suspend fun countByProvider(providerId: String): Int

    @Upsert
    suspend fun upsertAll(entities: List<ModelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ModelEntity)

    @Query("UPDATE models SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("UPDATE models SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE models SET isDefault = 1 WHERE id = :id")
    suspend fun markDefault(id: String)

    @Query(
        "UPDATE models SET temperature = :temperature, maxTokens = :maxTokens, topP = :topP, " +
            "systemPrompt = :systemPrompt, streaming = :streaming WHERE id = :id",
    )
    suspend fun updateParams(
        id: String,
        temperature: Float,
        maxTokens: Int,
        topP: Float,
        systemPrompt: String,
        streaming: Boolean,
    )

    @Query("DELETE FROM models WHERE providerId = :providerId")
    suspend fun deleteByProvider(providerId: String)

    @Query("DELETE FROM models")
    suspend fun clear()
}

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT 1")
    fun observeLatest(): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations ORDER BY createdAt ASC")
    suspend fun getAll(): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ConversationEntity>)

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: String, updatedAt: Long)

    @Query("UPDATE conversations SET providerId = :providerId, modelId = :modelId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateModel(id: String, providerId: String?, modelId: String?, updatedAt: Long)

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun rename(id: String, title: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM conversations")
    suspend fun clear()
}

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    fun observeByConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    suspend fun getByConversation(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY createdAt ASC")
    suspend fun getAll(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MessageEntity>)

    @Query(
        "UPDATE messages SET content = :content, status = :status, errorMessage = :error, " +
            "tokenIn = COALESCE(:tokenIn, tokenIn), tokenOut = COALESCE(:tokenOut, tokenOut) WHERE id = :id",
    )
    suspend fun updateAssistant(
        id: String,
        content: String,
        status: String,
        error: String?,
        tokenIn: Int?,
        tokenOut: Int?,
    )

    @Query("DELETE FROM messages")
    suspend fun clear()
}

@Dao
interface TaskDao {

    @Query("SELECT * FROM task_runs ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<TaskRunEntity>>

    @Query("SELECT * FROM task_runs ORDER BY startedAt ASC")
    suspend fun getAllRuns(): List<TaskRunEntity>

    @Query("SELECT * FROM task_runs WHERE id = :id")
    fun observeById(id: String): Flow<TaskRunEntity?>

    @Query("SELECT * FROM task_runs WHERE id = :id")
    suspend fun getById(id: String): TaskRunEntity?

    @Query("SELECT * FROM task_steps WHERE taskId = :taskId ORDER BY startedAt ASC, id ASC")
    fun observeSteps(taskId: String): Flow<List<TaskStepEntity>>

    @Query("SELECT * FROM task_steps ORDER BY startedAt ASC")
    suspend fun getAllSteps(): List<TaskStepEntity>

    @Query("SELECT * FROM task_steps WHERE taskId = :taskId ORDER BY startedAt ASC")
    suspend fun getSteps(taskId: String): List<TaskStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(entity: TaskRunEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRuns(entities: List<TaskRunEntity>)

    @Query(
        "UPDATE task_runs SET status = :status, finishedAt = :finishedAt, error = :error, " +
            "tokensIn = :tokensIn, tokensOut = :tokensOut WHERE id = :id",
    )
    suspend fun finishRun(
        id: String,
        status: String,
        finishedAt: Long,
        error: String?,
        tokensIn: Int?,
        tokensOut: Int?,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(entity: TaskStepEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSteps(entities: List<TaskStepEntity>)

    @Query("UPDATE task_steps SET status = :status, finishedAt = :finishedAt, detail = :detail WHERE id = :id")
    suspend fun finishStep(id: String, status: String, finishedAt: Long, detail: String?)

    @Query("DELETE FROM task_runs")
    suspend fun clearRuns()

    @Query("DELETE FROM task_steps")
    suspend fun clearSteps()
}
