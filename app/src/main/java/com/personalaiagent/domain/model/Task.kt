package com.personalaiagent.domain.model

data class TaskRun(
    val id: String,
    val conversationId: String?,
    val prompt: String,
    val status: TaskStatus,
    val startedAt: Long,
    val finishedAt: Long?,
    val error: String?,
    val providerId: String?,
    val modelId: String?,
    val tokensIn: Int?,
    val tokensOut: Int?,
    val steps: List<TaskStep> = emptyList(),
) {
    val durationMillis: Long?
        get() = finishedAt?.let { (it - startedAt).coerceAtLeast(0) }
}

data class TaskStep(
    val id: String,
    val taskId: String,
    val name: TaskStepName,
    val status: TaskStepStatus,
    val startedAt: Long,
    val finishedAt: Long?,
    val detail: String?,
)
