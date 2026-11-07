package com.personalaiagent.data.remote

import com.personalaiagent.core.AppError
import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.model.ChatTurn
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.model.RemoteModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * Client for any OpenAI-compatible `/chat/completions` endpoint
 * (OpenAI, Groq, DeepSeek, Gemini compatibility layer, local gateways, ...).
 */
class OpenAiCompatibleClient(
    private val http: HttpEngine,
    private val json: Json,
) : ProviderClient {

    override fun stream(request: ChatRequest): Flow<ChatChunk> = flow {
        try {
            val url = "${request.baseUrl.trimEnd('/')}/chat/completions"
            val headers = mapOf(
                "Authorization" to "Bearer ${request.apiKey}",
                "Accept" to if (request.params.streaming) "text/event-stream" else "application/json",
            )
            val dto = OpenAiChatRequestDto(
                model = request.modelId,
                messages = request.messages.map { it.toDto() },
                temperature = request.params.temperature,
                maxTokens = request.params.maxTokens,
                topP = request.params.topP,
                stream = request.params.streaming,
                streamOptions = if (request.params.streaming) OpenAiStreamOptionsDto() else null,
            )
            val body = json.encodeToString(OpenAiChatRequestDto.serializer(), dto)

            if (request.params.streaming) {
                val decoder = OpenAiStreamDecoder(json)
                var usage: ChatChunk.Usage? = null
                http.streamEvents(url, headers, body).collect { event ->
                    when (val chunk = decoder.decode(event)) {
                        is ChatChunk.Delta -> emit(chunk)
                        is ChatChunk.Usage -> usage = chunk
                        ChatChunk.Completed -> Unit
                        null -> Unit
                    }
                }
                usage?.let { emit(it) }
            } else {
                val raw = http.postJson(url, headers, body)
                val parsed = json.decodeFromString(OpenAiChunkDto.serializer(), raw)
                val text = parsed.choices.firstOrNull()?.message?.content.orEmpty()
                emit(ChatChunk.Delta(text))
                parsed.usage?.let { emit(ChatChunk.Usage(it.promptTokens, it.completionTokens)) }
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
            "$base/models",
            mapOf("Authorization" to "Bearer $apiKey", "Accept" to "application/json"),
        )
        val parsed = json.decodeFromString(OpenAiModelListDto.serializer(), raw)
        val models = ModelCatalog.filter(
            parsed.data.map {
                RemoteModel(
                    remoteId = it.id,
                    displayName = it.id,
                    contextWindow = it.contextWindow,
                )
            },
        )
        Outcome.Success(models)
    } catch (e: CancellationException) {
        throw e
    } catch (e: ProviderException) {
        Outcome.Failure(e.error)
    } catch (e: Exception) {
        Outcome.Failure(e.toAppError())
    }

    private fun ChatTurn.toDto(): OpenAiMessageDto = OpenAiMessageDto(
        role = when (role) {
            MessageRole.USER -> "user"
            MessageRole.ASSISTANT -> "assistant"
            MessageRole.SYSTEM -> "system"
        },
        content = content,
    )
}
