package com.personalaiagent.domain.usecase

import com.personalaiagent.core.AppConstants
import com.personalaiagent.core.AppError
import com.personalaiagent.core.IdGenerator
import com.personalaiagent.core.TimeProvider
import com.personalaiagent.data.remote.ChatChunk
import com.personalaiagent.data.remote.ChatRequest
import com.personalaiagent.data.remote.ProviderClientRouter
import com.personalaiagent.data.remote.ProviderException
import com.personalaiagent.data.remote.toAppError
import com.personalaiagent.domain.model.AiModel
import com.personalaiagent.domain.model.AppSettings
import com.personalaiagent.domain.model.ChatMessage
import com.personalaiagent.domain.model.ChatTurn
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.MessageStatus
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.model.TaskStatus
import com.personalaiagent.domain.model.TaskStepName
import com.personalaiagent.domain.model.TaskStepStatus
import com.personalaiagent.domain.repository.ChatRepository
import com.personalaiagent.domain.repository.CredentialStore
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import com.personalaiagent.domain.repository.SettingsRepository
import com.personalaiagent.domain.repository.TaskRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/** Everything the chat UI needs to know while a message is being executed. */
sealed interface SendMessageEvent {
    data class Started(
        val conversationId: String,
        val taskId: String,
        val assistantMessageId: String,
    ) : SendMessageEvent

    data class Delta(val text: String) : SendMessageEvent

    data class Completed(
        val taskId: String,
        val messageId: String,
        val fullText: String,
    ) : SendMessageEvent

    data class Failed(
        val error: AppError,
        val taskId: String?,
        val partialText: String,
    ) : SendMessageEvent
}

/**
 * The core "agent task": resolve provider → build prompt → stream completion →
 * persist answer. Every step is recorded in the execution log, and a failed or
 * cancelled stream never loses the text that was already produced.
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
    private val credentialStore: CredentialStore,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val clients: ProviderClientRouter,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {

    operator fun invoke(
        conversationId: String?,
        text: String,
        preferredModelId: String?,
    ): Flow<SendMessageEvent> = flow {
        val prompt = text.trim()
        if (prompt.isEmpty()) {
            emit(SendMessageEvent.Failed(AppError.InvalidInput("empty"), null, ""))
            return@flow
        }

        val settings = settingsRepository.current()

        // ---- 1. Resolve model + provider -------------------------------------
        val model = resolveModel(preferredModelId)
        if (model == null) {
            emit(SendMessageEvent.Failed(AppError.NoModel, null, ""))
            return@flow
        }
        val provider = providerRepository.getProvider(model.providerId)
        if (provider == null) {
            emit(SendMessageEvent.Failed(AppError.NoProvider, null, ""))
            return@flow
        }
        val apiKey = credentialStore.read(provider.id)
        if (apiKey.isNullOrBlank()) {
            emit(SendMessageEvent.Failed(AppError.MissingKey(provider.name), null, ""))
            return@flow
        }

        // ---- 2. Conversation + task -----------------------------------------
        val convId = conversationId?.also { existing ->
            chatRepository.updateConversationModel(existing, provider.id, model.id)
        } ?: chatRepository.createConversation(
            title = prompt.take(48),
            providerId = provider.id,
            modelId = model.id,
        )

        val taskId = taskRepository.createTask(prompt, provider.id, model.id, convId)
        val resolveStep = taskRepository.startStep(taskId, TaskStepName.RESOLVE_PROVIDER)
        taskRepository.finishStep(
            resolveStep,
            TaskStepStatus.SUCCEEDED,
            detail = "${provider.name} · ${model.displayName}",
        )

        val now = time.now()
        val userMessage = ChatMessage(
            id = ids.newId(),
            conversationId = convId,
            role = MessageRole.USER,
            content = prompt,
            createdAt = now,
            status = MessageStatus.COMPLETE,
            providerId = provider.id,
            modelId = model.id,
        )
        chatRepository.appendMessage(userMessage)

        val assistantId = ids.newId()
        chatRepository.appendMessage(
            ChatMessage(
                id = assistantId,
                conversationId = convId,
                role = MessageRole.ASSISTANT,
                content = "",
                createdAt = now + 1,
                status = MessageStatus.STREAMING,
                providerId = provider.id,
                modelId = model.id,
            ),
        )
        emit(SendMessageEvent.Started(convId, taskId, assistantId))

        // ---- 3. Build prompt -------------------------------------------------
        val promptStep = taskRepository.startStep(taskId, TaskStepName.BUILD_PROMPT)
        val history = chatRepository.getMessages(convId)
            .filter { it.id != assistantId && it.content.isNotBlank() && it.status != MessageStatus.FAILED }
            .takeLast(AppConstants.MAX_CONTEXT_MESSAGES)
        val turns = buildList {
            if (provider.kind == ProviderKind.OPENAI_COMPATIBLE && model.params.systemPrompt.isNotBlank()) {
                add(ChatTurn(MessageRole.SYSTEM, model.params.systemPrompt))
            }
            history.forEach { add(ChatTurn(it.role, it.content)) }
        }
        taskRepository.finishStep(
            promptStep,
            TaskStepStatus.SUCCEEDED,
            detail = "${turns.size} turns",
        )

        // ---- 4. Stream -------------------------------------------------------
        val streaming = settings.streamingEnabled && model.params.streaming
        val request = ChatRequest(
            kind = provider.kind,
            baseUrl = provider.baseUrl,
            apiKey = apiKey,
            modelId = model.remoteId,
            messages = turns,
            params = model.params.copy(streaming = streaming),
        )
        val sendStep = taskRepository.startStep(taskId, TaskStepName.SEND_REQUEST)
        val receiveStep = taskRepository.startStep(taskId, TaskStepName.RECEIVE_RESPONSE)

        val buffer = StringBuilder()
        var tokensIn: Int? = null
        var tokensOut: Int? = null

        try {
            clients.forKind(provider.kind).stream(request).collect { chunk ->
                when (chunk) {
                    is ChatChunk.Delta -> {
                        if (chunk.text.isNotEmpty()) {
                            buffer.append(chunk.text)
                            emit(SendMessageEvent.Delta(chunk.text))
                        }
                    }
                    is ChatChunk.Usage -> {
                        tokensIn = chunk.tokenIn ?: tokensIn
                        tokensOut = chunk.tokenOut ?: tokensOut
                    }
                    ChatChunk.Completed -> Unit
                }
            }

            val fullText = buffer.toString()
            if (fullText.isBlank()) {
                throw ProviderException(AppError.Parse)
            }

            taskRepository.finishStep(sendStep, TaskStepStatus.SUCCEEDED, detail = provider.name)
            taskRepository.finishStep(
                receiveStep,
                TaskStepStatus.SUCCEEDED,
                detail = "${fullText.length} chars",
            )

            val persistStep = taskRepository.startStep(taskId, TaskStepName.PERSIST_MESSAGE)
            chatRepository.updateAssistantMessage(
                id = assistantId,
                content = fullText,
                status = MessageStatus.COMPLETE,
                error = null,
                tokenIn = tokensIn,
                tokenOut = tokensOut,
            )
            taskRepository.finishStep(persistStep, TaskStepStatus.SUCCEEDED)
            taskRepository.finishTask(taskId, TaskStatus.SUCCEEDED, null, tokensIn, tokensOut)

            emit(SendMessageEvent.Completed(taskId, assistantId, fullText))
        } catch (cancelled: CancellationException) {
            // Persist partial output before propagating cancellation.
            withContext(NonCancellable) {
                val partial = buffer.toString()
                chatRepository.updateAssistantMessage(
                    id = assistantId,
                    content = partial,
                    status = MessageStatus.FAILED,
                    error = "cancelled",
                    tokenIn = tokensIn,
                    tokenOut = tokensOut,
                )
                taskRepository.finishStep(sendStep, TaskStepStatus.SKIPPED)
                taskRepository.finishStep(receiveStep, TaskStepStatus.FAILED, detail = "cancelled")
                taskRepository.finishTask(taskId, TaskStatus.CANCELLED, "cancelled", tokensIn, tokensOut)
            }
            throw cancelled
        } catch (e: Exception) {
            val error = (e as? ProviderException)?.error ?: e.toAppError()
            val partial = buffer.toString()
            taskRepository.finishStep(sendStep, TaskStepStatus.FAILED, detail = error.describe())
            taskRepository.finishStep(receiveStep, TaskStepStatus.FAILED, detail = error.describe())
            chatRepository.updateAssistantMessage(
                id = assistantId,
                content = partial,
                status = MessageStatus.FAILED,
                error = error.describe(),
                tokenIn = tokensIn,
                tokenOut = tokensOut,
            )
            taskRepository.finishTask(taskId, TaskStatus.FAILED, error.describe(), tokensIn, tokensOut)
            emit(SendMessageEvent.Failed(error, taskId, partial))
        }
    }

    private suspend fun resolveModel(preferredModelId: String?): AiModel? {
        preferredModelId?.let { id ->
            modelRepository.getModel(id)?.takeIf { it.enabled }?.let { return it }
        }
        modelRepository.getDefaultModel()?.takeIf { it.enabled }?.let { return it }
        return modelRepository.firstEnabledModel()
    }
}

/** Stable, non-sensitive identifier for logs and the execution log screen. */
fun AppError.describe(): String = when (this) {
    AppError.NoProvider -> "no_provider"
    AppError.NoModel -> "no_model"
    is AppError.MissingKey -> "missing_key: $providerName"
    is AppError.InvalidInput -> "invalid_input: $reason"
    AppError.Network -> "network"
    AppError.Timeout -> "timeout"
    AppError.Auth -> "auth"
    AppError.RateLimit -> "rate_limit"
    is AppError.Server -> "server: $code"
    AppError.Parse -> "parse"
    AppError.Cancelled -> "cancelled"
    AppError.SecureStorage -> "secure_storage"
    is AppError.Unknown -> "unknown: ${message ?: ""}"
}
