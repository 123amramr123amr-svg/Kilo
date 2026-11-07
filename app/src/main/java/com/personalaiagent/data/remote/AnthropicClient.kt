package com.personalaiagent.data.remote

import com.personalaiagent.core.AppConstants
import com.personalaiagent.core.AppError
import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.model.RemoteModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/** Client for the Anthropic Messages API (`/v1/messages`). */
class AnthropicClient(
    private val http: HttpEngine,
    private val json: Json,
) : ProviderClient {

    override fun stream(request: ChatRequest): Flow<ChatChunk> = flow {
        try {
            val url = "${request.baseUrl.trimEnd('/')}/messages"
            val headers = mapOf(
                "x-api-key" to request.apiKey,
                "anthropic-version" to AppConstants.ANTHROPIC_VERSION,
                "Accept" to if (request.params.streaming) "text/event-stream" else "application/json",
            )

            // Anthropic requires the system prompt as a top-level field, not a message.
            val system = request.params.systemPrompt.ifBlank { null }
            val turns = request.messages
                .filter { it.role != MessageRole.SYSTEM }
                .map { AnthropicMessageDto(role = if (it.role == MessageRole.ASSISTANT) "assistant" else "user", content = it.content) }

            val dto = AnthropicRequestDto(
                model = request.modelId,
                maxTokens = request.params.maxTokens,
                temperature = request.params.temperature,
                topP = request.params.topP,
                system = system,
                messages = turns,
                stream = request.params.streaming,
            )
            val body = json.encodeToString(AnthropicRequestDto.serializer(), dto)

            if (request.params.streaming) {
                val decoder = AnthropicStreamDecoder(json)
                http.streamEvents(url, headers, body).collect { event ->
                    when (val chunk = decoder.decode(event)) {
                        is ChatChunk.Delta -> emit(chunk)
                        is ChatChunk.Usage -> emit(chunk)
                        ChatChunk.Completed -> Unit
                        null -> Unit
                    }
                }
            } else {
                val raw = http.postJson(url, headers, body)
                val parsed = json.decodeFromString(AnthropicMessageResponseDto.serializer(), raw)
                val text = parsed.content.filter { it.type == null || it.type == "text" }
                    .mapNotNull { it.text }
                    .joinToString("")
                emit(ChatChunk.Delta(text))
                parsed.usage?.let { emit(ChatChunk.Usage(it.inputTokens, it.outputTokens)) }
            }
            emit(ChatChunk.Completed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ProviderException) {
            throw e
        } catch (e: Exception) {
            throw ProviderException(e.toAppError(), e)
        }
    }

    override suspend fun fetchModels(
        provider: Provider,
        apiKey: String,
        baseUrlOverride: String?,
    ): Outcome<List<RemoteModel>> = try {
        val base = (baseUrlOverride ?: provider.baseUrl).trimEnd('/')
        val raw = http.getJson(
            "$base/models?limit=100",
            mapOf(
                "x-api-key" to apiKey,
                "anthropic-version" to AppConstants.ANTHROPIC_VERSION,
                "Accept" to "application/json",
            ),
        )
        val parsed = json.decodeFromString(AnthropicModelListDto.serializer(), raw)
        Outcome.Success(
            parsed.data.map {
                RemoteModel(
                    remoteId = it.id,
                    displayName = it.displayName ?: it.id,
                    contextWindow = null,
                )
            },
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: ProviderException) {
        Outcome.Failure(e.error)
    } catch (e: Exception) {
        Outcome.Failure(e.toAppError())
    }
}
