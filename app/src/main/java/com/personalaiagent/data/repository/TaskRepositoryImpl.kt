package com.personalaiagent.data.repository

import com.personalaiagent.core.IdGenerator
import com.personalaiagent.core.TimeProvider
import com.personalaiagent.data.local.TaskDao
import com.personalaiagent.data.local.TaskRunEntity
import com.personalaiagent.data.local.TaskStepEntity
import com.personalaiagent.data.local.toDomain
import com.personalaiagent.domain.model.TaskRun
import com.personalaiagent.domain.model.TaskStatus
import com.personalaiagent.domain.model.TaskStepName
import com.personalaiagent.domain.model.TaskStepStatus
import com.personalaiagent.domain.repository.TaskRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : TaskRepository {

    override fun observeTasks(): Flow<List<TaskRun>> =
        taskDao.observeAll().map { runs -> runs.map { it.toDomain() } }

    override fun observeTask(id: String): Flow<TaskRun?> =
        combine(taskDao.observeById(id), taskDao.observeSteps(id)) { run, steps ->
            run?.toDomain(steps.map { it.toDomain() })
        }

    override suspend fun getTask(id: String): TaskRun? =
        taskDao.getById(id)?.toDomain(taskDao.getSteps(id).map { it.toDomain() })

    override suspend fun createTask(
        prompt: String,
        providerId: String?,
        modelId: String?,
        conversationId: String?,
    ): String {
        val id = ids.newId()
        taskDao.insertRun(
            TaskRunEntity(
                id = id,
                conversationId = conversationId,
                prompt = prompt,
                status = TaskStatus.RUNNING.name,
                startedAt = time.now(),
                providerId = providerId,
                modelId = modelId,
            ),
        )
        return id
    }

    override suspend fun startStep(taskId: String, name: TaskStepName): String {
        val id = ids.newId()
        taskDao.insertStep(
            TaskStepEntity(
                id = id,
                taskId = taskId,
                name = name.name,
                status = TaskStepStatus.RUNNING.name,
                startedAt = time.now(),
            ),
        )
        return id
    }

    override suspend fun finishStep(stepId: String, status: TaskStepStatus, detail: String?) {
        taskDao.finishStep(stepId, status.name, time.now(), detail)
    }

    override suspend fun finishTask(
        id: String,
        status: TaskStatus,
        error: String?,
        tokensIn: Int?,
        tokensOut: Int?,
    ) {
        taskDao.finishRun(id, status.name, time.now(), error, tokensIn, tokensOut)
    }

    override suspend fun clearAll() {
        taskDao.clearSteps()
        taskDao.clearRuns()
    }
}
