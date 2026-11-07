package com.personalaiagent.data.local

import com.personalaiagent.domain.model.AiModel
import com.personalaiagent.domain.model.ChatMessage
import com.personalaiagent.domain.model.Conversation
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.MessageStatus
import com.personalaiagent.domain.model.ModelCapability
import com.personalaiagent.domain.model.ModelParams
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.model.ProviderStatus
import com.personalaiagent.domain.model.TaskRun
import com.personalaiagent.domain.model.TaskStatus
import com.personalaiagent.domain.model.TaskStep
import com.personalaiagent.domain.model.TaskStepName
import com.personalaiagent.domain.model.TaskStepStatus

private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T =
    this?.let { value -> enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) } } ?: fallback

fun ProviderEntity.toDomain(hasKey: Boolean): Provider = Provider(
    id = id,
    name = name,
    kind = kind.toEnum(ProviderKind.OPENAI_COMPATIBLE),
    baseUrl = baseUrl,
    isDefault = isDefault,
    hasKey = hasKey,
    status = status.toEnum(ProviderStatus.UNKNOWN),
    lastCheckedAt = lastCheckedAt,
    createdAt = createdAt,
)

fun Provider.toEntity(): ProviderEntity = ProviderEntity(
    id = id,
    name = name,
    kind = kind.name,
    baseUrl = baseUrl,
    isDefault = isDefault,
    status = status.name,
    lastCheckedAt = lastCheckedAt,
    createdAt = createdAt,
)

fun ModelEntity.toDomain(providerName: String): AiModel = AiModel(
    id = id,
    providerId = providerId,
    providerName = providerName,
    remoteId = remoteId,
    displayName = displayName,
    capabilities = capabilities
        .split(',')
        .mapNotNull { raw -> ModelCapability.entries.firstOrNull { it.name == raw.trim() } }
        .toSet(),
    contextWindow = contextWindow,
    enabled = enabled,
    isDefault = isDefault,
    params = ModelParams(
        temperature = temperature,
        maxTokens = maxTokens,
        topP = topP,
        systemPrompt = systemPrompt,
        streaming = streaming,
    ),
)

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    providerId = providerId,
    modelId = modelId,
)

fun MessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    conversationId = conversationId,
    role = role.toEnum(MessageRole.USER),
    content = content,
    createdAt = createdAt,
    status = status.toEnum(MessageStatus.COMPLETE),
    providerId = providerId,
    modelId = modelId,
    tokenIn = tokenIn,
    tokenOut = tokenOut,
    errorMessage = errorMessage,
)

fun ChatMessage.toEntity(): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    role = role.name,
    content = content,
    createdAt = createdAt,
    status = status.name,
    providerId = providerId,
    modelId = modelId,
    tokenIn = tokenIn,
    tokenOut = tokenOut,
    errorMessage = errorMessage,
)

fun TaskRunEntity.toDomain(steps: List<TaskStep> = emptyList()): TaskRun = TaskRun(
    id = id,
    conversationId = conversationId,
    prompt = prompt,
    status = status.toEnum(TaskStatus.QUEUED),
    startedAt = startedAt,
    finishedAt = finishedAt,
    error = error,
    providerId = providerId,
    modelId = modelId,
    tokensIn = tokensIn,
    tokensOut = tokensOut,
    steps = steps,
)

fun TaskStepEntity.toDomain(): TaskStep = TaskStep(
    id = id,
    taskId = taskId,
    name = name.toEnum(TaskStepName.SEND_REQUEST),
    status = status.toEnum(TaskStepStatus.PENDING),
    startedAt = startedAt,
    finishedAt = finishedAt,
    detail = detail,
)
