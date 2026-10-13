package com.amrstudio.ui.agent

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amrstudio.data.model.AgentTask
import com.amrstudio.data.repository.AgentTaskRepository
import com.amrstudio.utils.IdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val taskRepository: AgentTaskRepository,
    application: Application
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<AgentTask>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _currentTask = MutableStateFlow<AgentTask?>(null)
    val currentTask = _currentTask.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    var currentUserId: String? = null

    fun loadUserTasks(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            taskRepository.getUserTasks(userId).collect { _tasks.value = it }
        }
    }

    fun selectTask(task: AgentTask) {
        _currentTask.value = task
    }

    fun createTask(title: String, description: String): AgentTask {
        val task = AgentTask(
            id = IdGenerator.generate(),
            userId = currentUserId ?: "",
            title = title,
            description = description,
            status = AgentTask.TaskStatus.PENDING,
            plan = "[]",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        viewModelScope.launch {
            taskRepository.createTask(task)
        }
        return task
    }

    fun generatePlan(task: AgentTask) {
        _currentTask.value = task.copy(status = AgentTask.TaskStatus.PLANNING, updatedAt = LocalDateTime.now())
        viewModelScope.launch {
            taskRepository.updateTask(_currentTask.value!!)
            // Simulate plan generation
            kotlinx.coroutines.delay(2000)
            val plan = generatePlanSteps(task.description)
            val updated = task.copy(
                status = AgentTask.TaskStatus.EXECUTING,
                plan = Json { prettyPrint = true }.encodeToString(plan),
                currentStep = 0,
                updatedAt = LocalDateTime.now()
            )
            _currentTask.value = updated
            taskRepository.updateTask(updated)
            executePlan(updated)
        }
    }

    private fun generatePlanSteps(description: String): List<PlanStep> {
        // Simulated plan generation
        return listOf(
            PlanStep(1, "تحليل المهمة", "فهم المتطلبات وتقسيمها", PlanStep.Type.ANALYSIS),
            PlanStep(2, "إنشاء هيكل المشروع", "إعداد الملفات والمجلدات الأساسية", PlanStep.Type.CREATE_FILES),
            PlanStep(3, "كتابة الكود", "تنفيذ الوظائف المطلوبة", PlanStep.Type.WRITE_CODE),
            PlanStep(4, "اختبار وتشغيل", "التحقق من عمل الكود بشكل صحيح", PlanStep.Type.TEST),
            PlanStep(5, "توثيق ونشر", "إعداد التوثيق والنشر", PlanStep.Type.DEPLOY)
        )
    }

    private fun executePlan(task: AgentTask) {
        viewModelScope.launch {
            val plan = Json { }.decodeFromString<List<PlanStep>>(task.plan)
            plan.forEachIndexed { index, step ->
                val updated = task.copy(
                    currentStep = index + 1,
                    updatedAt = LocalDateTime.now()
                )
                _currentTask.value = updated
                taskRepository.updateTask(updated)
                // Simulate step execution
                kotlinx.coroutines.delay(3000)
            }
            val completed = task.copy(
                status = AgentTask.TaskStatus.COMPLETED,
                completedAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            _currentTask.value = completed
            taskRepository.updateTask(completed)
            // Send notification
        }
    }

    fun pauseTask(task: AgentTask) {
        val updated = task.copy(status = AgentTask.TaskStatus.PAUSED, updatedAt = LocalDateTime.now())
        viewModelScope.launch {
            taskRepository.updateTask(updated)
            if (_currentTask.value?.id == task.id) _currentTask.value = updated
        }
    }

    fun resumeTask(task: AgentTask) {
        val updated = task.copy(status = AgentTask.TaskStatus.EXECUTING, updatedAt = LocalDateTime.now())
        viewModelScope.launch {
            taskRepository.updateTask(updated)
            if (_currentTask.value?.id == task.id) _currentTask.value = updated
            executePlan(updated)
        }
    }

    fun cancelTask(task: AgentTask) {
        val updated = task.copy(status = AgentTask.TaskStatus.CANCELLED, updatedAt = LocalDateTime.now())
        viewModelScope.launch {
            taskRepository.updateTask(updated)
            if (_currentTask.value?.id == task.id) _currentTask.value = updated
        }
    }

    fun deleteTask(task: AgentTask) {
        viewModelScope.launch {
            taskRepository.deleteTask(task.id)
            _tasks.value = _tasks.value.filter { it.id != task.id }
            if (_currentTask.value?.id == task.id) _currentTask.value = null
        }
    }

    data class PlanStep(
        val step: Int,
        val title: String,
        val description: String,
        val type: Type
    ) {
        enum class Type { ANALYSIS, CREATE_FILES, WRITE_CODE, TEST, DEPLOY, CUSTOM }
    }
}