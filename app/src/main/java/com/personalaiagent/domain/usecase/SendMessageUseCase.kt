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
import com.personalaiagent.domain.model.Attachment
import com.personalaiagent.domain.model.ChatMessage
import com.personalaiagent.domain.model.ChatTurn
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.MessageStatus
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.model.TaskStatus
import com.personalaiagent.domain.model.TaskStepName
import com.personalaiagent.domain.model.TaskStepStatus
import com.personalaiagent.domain.usecase.agent.SkillSource
import com.personalaiagent.domain.model.ToolCall
import com.personalaiagent.domain.repository.ChatRepository
import com.personalaiagent.domain.repository.CredentialStore
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import com.personalaiagent.domain.repository.SettingsRepository
import com.personalaiagent.domain.repository.TaskRepository
import com.personalaiagent.domain.usecase.agent.AgentToolCatalog
import com.personalaiagent.domain.usecase.agent.AgentToolExecutor
import com.personalaiagent.domain.usecase.agent.ToolContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Everything the chat UI needs to know while a message is being executed. */
sealed interface SendMessageEvent {
    data class Started(
        val conversationId: String,
        val taskId: String,
        val assistantMessageId: String,
    ) : SendMessageEvent

    data class Delta(val text: String) : SendMessageEvent

    /** The model is thinking out loud (providers that expose reasoning). */
    data class Reasoning(val text: String) : SendMessageEvent

    /**
     * A tool ran (or failed) on the phone workspace.
     *
     * @param detail what the step touched (path, query, url, app, prompt)
     * @param output a short preview of what the tool returned, for the activity panel
     */
    data class ToolActivity(
        val toolName: String,
        val detail: String,
        val ok: Boolean,
        val output: String = "",
    ) : SendMessageEvent

    /**
     * The request failed transiently and will be tried again automatically.
     * The UI shows "المزوّد مزدحم — إعادة المحاولة بعد ٧ ثوان (٢/٤)" instead of
     * an error banner that the user cannot act on.
     */
    data class Retrying(
        val attempt: Int,
        val maxAttempts: Int,
        val delayMillis: Long,
        val reason: String,
    ) : SendMessageEvent

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
 * The core agent task:
 *
 * resolve provider → build prompt → (stream answer → run tools → stream again)* → persist.
 *
 * When a workspace folder is connected, the model receives the file tools and the
 * phone genuinely becomes its workspace: it can list, read, create, edit and delete
 * files, and every tool call is recorded in the execution log.
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
    private val credentialStore: CredentialStore,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val clients: ProviderClientRouter,
    private val toolExecutor: AgentToolExecutor,
    private val json: Json,
    private val time: TimeProvider,
    private val ids: IdGenerator,
    private val skillSource: SkillSource,
) {

    /** Retries a failed request with backoff, honouring the provider's own hint. */
    private val retryPolicy = RetryPolicy()

    operator fun invoke(
        conversationId: String?,
        text: String,
        preferredModelId: String?,
        attachments: List<com.personalaiagent.domain.model.Attachment> = emptyList(),
    ): Flow<SendMessageEvent> = flow {
        val prompt = text.trim()
        if (prompt.isEmpty() && attachments.isEmpty()) {
            emit(SendMessageEvent.Failed(AppError.InvalidInput("empty"), null, ""))
            return@flow
        }

        val settings = settingsRepository.current()
        val toolsEnabled = settings.hasWorkspace && settings.agentFileToolsEnabled

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
            // The name the history list shows: one clean line from what was asked.
            title = ConversationTitles.from(prompt).ifBlank { prompt.take(ConversationTitles.MAX_LENGTH) },
            providerId = provider.id,
            modelId = model.id,
        )

        val taskId = taskRepository.createTask(prompt, provider.id, model.id, convId)
        val resolveStep = taskRepository.startStep(taskId, TaskStepName.RESOLVE_PROVIDER)
        taskRepository.finishStep(
            resolveStep,
            TaskStepStatus.SUCCEEDED,
            detail = "${provider.name} · ${model.displayName}" + if (toolsEnabled) " · tools" else "",
        )

        val now = time.now()
        chatRepository.appendMessage(
            ChatMessage(
                id = ids.newId(),
                conversationId = convId,
                role = MessageRole.USER,
                content = buildString {
                    append(prompt)
                    if (attachments.isNotEmpty()) {
                        if (isNotEmpty()) append("\n\n")
                        append(attachments.joinToString("\n") { "📎 ${it.sourceLabel ?: it.name}" })
                    }
                },
                createdAt = now,
                status = MessageStatus.COMPLETE,
                providerId = provider.id,
                modelId = model.id,
            ),
        )

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
        // The whole conversation (bounded by count *and* size) so the model keeps
        // knowing what was said earlier instead of losing the thread.
        val history = historyFor(convId, assistantId)
        val memory = memoryBlock(convId)

        val systemPrompt = buildString {
            if (model.params.systemPrompt.isNotBlank()) {
                append(model.params.systemPrompt.trim())
                append("\n\n")
            }
            if (memory.isNotBlank()) {
                append(memory)
                append("\n\n")
            }
            if (toolsEnabled) {
                append(
                    AgentToolCatalog.systemPrompt(
                        workspaceName = settings.workspaceName ?: "Workspace",
                        skills = runCatching { skillSource.catalog() }.getOrDefault(emptyList()),
                    ),
                )
            }
        }.trim()

        val lastUserId = history.lastOrNull { it.role == MessageRole.USER }?.id
        val turns = buildList {
            // OpenAI-compatible endpoints take the system prompt as a message;
            // Anthropic receives it through the `system` field instead.
            if (provider.kind == ProviderKind.OPENAI_COMPATIBLE && systemPrompt.isNotBlank()) {
                add(ChatTurn(MessageRole.SYSTEM, systemPrompt))
            }
            // Documents are sent as *text*, images as pixels: that is what lets a
            // plain (non-vision) model read a PDF, and it costs no image tokens.
            val images = attachments.filter { it.isImage }
            val documents = documentBlock(attachments)
            history.forEach { message ->
                val isLastUser = message.id == lastUserId
                add(
                    ChatTurn(
                        role = message.role,
                        content = if (isLastUser && documents.isNotBlank()) {
                            message.content + "\n\n" + documents
                        } else {
                            message.content
                        },
                        // Attachments only belong to the message that carried them.
                        attachments = if (isLastUser) images else emptyList(),
                    ),
                )
            }
        }.toMutableList()

        taskRepository.finishStep(
            promptStep,
            TaskStepStatus.SUCCEEDED,
            detail = buildString {
                append("${history.size} turns")
                if (memory.isNotBlank()) append(" · memory ${memory.length} chars")
                if (toolsEnabled) append(" · ${AgentToolCatalog.tools.size} tools")
            },
        )

        // ---- 4. Agent loop ---------------------------------------------------
        val streaming = settings.streamingEnabled && model.params.streaming
        val params = model.params.copy(
            streaming = streaming,
            systemPrompt = if (provider.kind == ProviderKind.ANTHROPIC) systemPrompt else "",
        )
        val tools = if (toolsEnabled) AgentToolCatalog.tools else emptyList()

        val answer = StringBuilder()
        val toolSummaries = mutableListOf<String>()
        var tokensIn: Int? = null
        var tokensOut: Int? = null
        var round = 0

        try {
            while (true) {
                round++
                val request = ChatRequest(
                    kind = provider.kind,
                    baseUrl = provider.baseUrl,
                    apiKey = apiKey,
                    modelId = model.remoteId,
                    messages = turns.toList(),
                    params = params,
                    tools = tools,
                )

                val sendStep = taskRepository.startStep(taskId, TaskStepName.SEND_REQUEST)
                val receiveStep = taskRepository.startStep(taskId, TaskStepName.RECEIVE_RESPONSE)
                val roundText = StringBuilder()
                val roundCalls = mutableListOf<ToolCall>()

                // A transient failure (network blip, 429, 5xx, a stream cut before
                // its terminator) is retried with exponential backoff while nothing
                // of this round was shown yet — so text never appears twice.
                var attempt = 0
                var trimmedContext = false
                var currentRequest = request
                while (true) {
                    attempt++
                    var terminated = false
                    try {
                        clients.forKind(provider.kind).stream(currentRequest).collect { chunk ->
                            when (chunk) {
                                is ChatChunk.Delta -> if (chunk.text.isNotEmpty()) {
                                    roundText.append(chunk.text)
                                    emit(SendMessageEvent.Delta(chunk.text))
                                }

                                is ChatChunk.Reasoning -> if (chunk.text.isNotEmpty()) {
                                    emit(SendMessageEvent.Reasoning(chunk.text))
                                }

                                is ChatChunk.Usage -> {
                                    tokensIn = chunk.tokenIn ?: tokensIn
                                    tokensOut = chunk.tokenOut ?: tokensOut
                                }

                                is ChatChunk.ToolCallChunk -> roundCalls += chunk.call
                                ChatChunk.Completed -> terminated = true
                            }
                        }
                        // A stream that closes without its terminator was cut off
                        // (proxy timeout, dropped connection). Retry it when nothing
                        // was shown yet instead of persisting a truncated answer.
                        if (!terminated && roundText.isEmpty() && roundCalls.isEmpty()) {
                            throw ProviderException(AppError.Network)
                        }
                        break
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (e: Exception) {
                        val providerError = e as? ProviderException
                        val error = providerError?.error ?: e.toAppError()
                        val decision = retryPolicy.decide(
                            error = error,
                            attempt = attempt,
                            retryAfterSeconds = providerError?.retryAfterSeconds,
                            partialAnswerShown = roundText.isNotEmpty() || roundCalls.isNotEmpty(),
                            trimmedContextAlready = trimmedContext,
                        )
                        if (!decision.retry) throw e
                        taskRepository.finishStep(
                            sendStep,
                            TaskStepStatus.FAILED,
                            detail = "retry ${attempt}/${retryPolicy.maxAttempts} after ${error.describe()}",
                        )
                        if (decision.trimContext) {
                            trimmedContext = true
                            trimContext(turns)
                            currentRequest = currentRequest.copy(messages = turns.toList())
                        }
                        emit(
                            SendMessageEvent.Retrying(
                                attempt = attempt,
                                maxAttempts = retryPolicy.maxAttempts,
                                delayMillis = decision.delayMillis,
                                reason = decision.reason,
                            ),
                        )
                        if (decision.delayMillis > 0L) delay(decision.delayMillis)
                    }
                }

                taskRepository.finishStep(sendStep, TaskStepStatus.SUCCEEDED, detail = "${provider.name} · round $round")
                taskRepository.finishStep(
                    receiveStep,
                    TaskStepStatus.SUCCEEDED,
                    detail = "${roundText.length} chars" + if (roundCalls.isNotEmpty()) " · ${roundCalls.size} tool(s)" else "",
                )

                if (roundCalls.isEmpty()) {
                    if (roundText.isNotBlank()) answer.append(roundText)
                    break
                }

                if (round >= AppConstants.MAX_AGENT_ROUNDS) {
                    throw ProviderException(AppError.Unknown("agent_round_limit"))
                }

                // The assistant turn that requested the tools, then one turn per result.
                turns += ChatTurn(
                    role = MessageRole.ASSISTANT,
                    content = roundText.toString(),
                    toolCalls = roundCalls,
                )
                if (roundText.isNotBlank()) {
                    answer.append(roundText).append("\n\n")
                }

                for (call in roundCalls) {
                    // What the step touched: a file, a query, a link or a prompt.
                    // This is what the chat shows in the agent panel.
                    val path = detailOf(call)
                    val step = taskRepository.startStep(taskId, TaskStepName.EXECUTE_TOOL)
                    val result = toolExecutor.execute(
                        call,
                        ToolContext(providerId = provider.id, modelId = model.remoteId),
                    )
                    taskRepository.finishStep(
                        step,
                        if (result.ok) TaskStepStatus.SUCCEEDED else TaskStepStatus.FAILED,
                        detail = if (path.isBlank()) call.name else "$path",
                    )
                    emit(
                        SendMessageEvent.ToolActivity(
                            toolName = call.name,
                            detail = path,
                            ok = result.ok,
                            output = result.output.take(400),
                        ),
                    )
                    toolSummaries += "${call.name} ${path}".trim()
                    turns += ChatTurn(
                        role = MessageRole.TOOL,
                        content = result.output,
                        toolCallId = call.id,
                    )
                }
            }

            var fullText = answer.toString().trim()
            if (fullText.isBlank()) {
                if (toolSummaries.isEmpty()) throw ProviderException(AppError.Parse)
                fullText = toolSummaries.joinToString("\n") { "✓ $it" }
            }

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
            withContext(NonCancellable) {
                chatRepository.updateAssistantMessage(
                    id = assistantId,
                    content = answer.toString().trim(),
                    status = MessageStatus.FAILED,
                    error = "cancelled",
                    tokenIn = tokensIn,
                    tokenOut = tokensOut,
                )
                taskRepository.finishTask(taskId, TaskStatus.CANCELLED, "cancelled", tokensIn, tokensOut)
            }
            throw cancelled
        } catch (e: Exception) {
            val error = (e as? ProviderException)?.error ?: e.toAppError()
            val partial = answer.toString().trim()
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

    /**
     * Halves the prompt after the provider said it is too long: the system
     * prompt and the newest turns always survive, because dropping the system
     * prompt would change the agent's behaviour mid-task.
     */
    private fun trimContext(turns: MutableList<ChatTurn>) {
        if (turns.isEmpty()) return
        val system = turns.first().takeIf { it.role == MessageRole.SYSTEM }
        val body = turns.filter { it !== system }
        if (body.size <= 2) {
            // Nothing else to drop: shorten the longest tool result instead.
            turns.replaceAll { turn ->
                if (turn.role == MessageRole.TOOL && turn.content.length > 2_000) {
                    turn.copy(content = turn.content.take(2_000) + "\n…(truncated)")
                } else {
                    turn
                }
            }
            return
        }
        val keep = body.takeLast(maxOf(2, body.size / 2))
        turns.clear()
        system?.let { turns += it }
        turns += keep
    }

    /** Recent turns of this conversation, capped by message count and by size. */
    private suspend fun historyFor(conversationId: String, assistantId: String): List<ChatMessage> {
        val all = chatRepository.getMessages(conversationId)
            .filter { it.id != assistantId && it.content.isNotBlank() && it.status != MessageStatus.FAILED }
            .takeLast(AppConstants.MAX_CONTEXT_MESSAGES)
        var budget = AppConstants.MAX_CONTEXT_CHARS
        val kept = ArrayDeque<ChatMessage>()
        for (message in all.asReversed()) {
            if (kept.isNotEmpty() && message.content.length > budget) break
            budget -= message.content.length
            kept.addFirst(message)
        }
        return kept.toList()
    }

    /**
     * A short, local-only summary of what the user talked about before, so a new
     * chat does not start from zero. Built from stored messages — no extra request,
     * no cost, and nothing leaves the phone except into the user's own provider.
     */
    /**
     * Document text for the model, with the size stated instead of silently cut.
     *
     * A 300-page PDF cannot fit in any context window, so the block says exactly
     * how much was sent and how much was left out — the model can then tell the
     * user what it did not see instead of pretending it read everything.
     */
    private fun documentBlock(attachments: List<Attachment>): String {
        val documents = attachments.mapNotNull { attachment -> attachment.text?.let { attachment to it } }
        if (documents.isEmpty()) return ""
        val budget = MAX_DOCUMENT_CHARS / documents.size
        return documents.joinToString("\n\n") { (attachment, text) ->
            buildString {
                append("### الملف المرفق: ").append(attachment.name).append('\n')
                if (text.length <= budget) {
                    append(text)
                } else {
                    val head = (budget * 3) / 4
                    val tail = budget - head
                    append(text.take(head))
                    append("\n\n[... تم اختصار ")
                    append(text.length - budget)
                    append(" حرف من أصل ")
                    append(text.length)
                    append(" — الملف أطول من المساحة المتاحة في هذه الرسالة ...]\n\n")
                    append(text.takeLast(tail))
                }
            }
        }
    }

    private suspend fun memoryBlock(currentConversationId: String?): String {
        val previous = runCatching {
            chatRepository.recentConversations(currentConversationId, AppConstants.MEMORY_CONVERSATIONS)
        }.getOrDefault(emptyList())
        if (previous.isEmpty()) return ""

        val body = StringBuilder()
        for (conversation in previous) {
            val exchanges = runCatching {
                chatRepository.getMessages(conversation.id)
                    .filter { it.content.isNotBlank() && it.status != MessageStatus.FAILED }
                    .takeLast(2)
            }.getOrDefault(emptyList())
            if (exchanges.isEmpty()) continue
            val entry = StringBuilder()
            entry.append("• ").append(conversation.title.take(60)).append('\n')
            exchanges.forEach { message ->
                val who = if (message.role == MessageRole.USER) "User" else "Assistant"
                entry.append("  ").append(who).append(": ")
                    .append(message.content.replace('\n', ' ').take(AppConstants.MEMORY_MESSAGE_CHARS))
                    .append('\n')
            }
            if (body.length + entry.length > AppConstants.MEMORY_TOTAL_CHARS) break
            body.append(entry)
        }
        if (body.isEmpty()) return ""
        return buildString {
            append("Earlier conversations on this device (background only; do not repeat them verbatim):")
            append('\n')
            append(body.toString().trim())
        }
    }

    private suspend fun resolveModel(preferredModelId: String?): AiModel? {
        preferredModelId?.let { id ->
            modelRepository.getModel(id)?.takeIf { it.enabled }?.let { return it }
        }
        modelRepository.getDefaultModel()?.takeIf { it.enabled }?.let { return it }
        return modelRepository.firstEnabledModel()
    }

    /** The most useful argument of a tool call, for the activity log and the UI. */
    private fun detailOf(call: ToolCall): String = when (call.name) {
        AgentToolCatalog.WEB_SEARCH -> argumentOf(call, "query")
        AgentToolCatalog.OPEN_URL -> argumentOf(call, "url")
        AgentToolCatalog.GENERATE_IMAGE -> argumentOf(call, "prompt")
        AgentToolCatalog.OPEN_APP -> argumentOf(call, "app")
        else -> argumentOf(call, "path")
    }

    /** Reads a string field from the tool-call arguments without failing on bad JSON. */
    private fun argumentOf(call: ToolCall, key: String): String = runCatching {
        (json.parseToJsonElement(call.arguments) as? JsonObject)
            ?.get(key)
            ?.let { (it as? JsonPrimitive)?.contentOrNull }
            .orEmpty()
    }.getOrDefault("")
}

/**
 * Errors worth an automatic retry: they are usually momentary.
 * Kept as the single entry point the tests use; the schedule itself lives in
 * [RetryPolicy].
 */
internal fun AppError.isTransient(): Boolean = isRetryable()

/** Stable, non-sensitive identifier for logs and the execution log screen. */
/** Characters of document text per message (~12k tokens), shared by all files. */
private const val MAX_DOCUMENT_CHARS = 48_000

fun AppError.describe(): String = when (this) {
    AppError.NoProvider -> "no_provider"
    AppError.NoModel -> "no_model"
    is AppError.MissingKey -> "missing_key: $providerName"
    is AppError.InvalidInput -> "invalid_input: $reason"
    AppError.Network -> "network"
    AppError.Timeout -> "timeout"
    AppError.Auth -> "auth"
    is AppError.RateLimit -> "rate_limit"
    is AppError.QuotaExceeded -> "quota_exceeded"
    AppError.ContextTooLong -> "context_too_long"
    is AppError.Server -> "server: $code"
    AppError.HttpNotFound -> "http_not_found"
    AppError.NotFound -> "not_found"
    AppError.Parse -> "parse"
    AppError.Cancelled -> "cancelled"
    AppError.NoWorkspace -> "no_workspace"
    AppError.SecureStorage -> "secure_storage"
    is AppError.Unknown -> "unknown: ${message ?: ""}"
}
