package com.personalaiagent.data.remote

import com.personalaiagent.core.AppError
import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.model.AgentTool
import com.personalaiagent.domain.model.ChatTurn
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.model.RemoteModel
import com.personalaiagent.domain.model.ToolCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Client for any OpenAI-compatible `/chat/completions` endpoint
 * (OpenAI, Groq, DeepSeek, Gemini compatibility layer, local gateways, ...).
 * Supports streaming text, streamed usage and function/tool calling.
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
            val body = buildRequestBody(request, json)

            if (request.params.streaming) {
                val decoder = OpenAiStreamDecoder(json)
                val toolCalls = OpenAiToolCallAccumulator()
                var usage: ChatChunk.Usage? = null
                http.streamEvents(url, headers, body).collect { event ->
                    when (val chunk = decoder.decode(event)) {
                        is ChatChunk.Delta -> emit(chunk)
                        is ChatChunk.Usage -> usage = chunk
                        is ChatChunk.ToolCallChunk -> Unit
                        ChatChunk.Completed -> Unit
                        null -> Unit
                    }
                    decoder.lastToolCallDeltas.forEach { delta ->
                        toolCalls.add(delta.index, delta.id, delta.function?.name, delta.function?.arguments)
                    }
                }
                toolCalls.build().forEach { emit(ChatChunk.ToolCallChunk(it)) }
                usage?.let { emit(it) }
            } else {
                val raw = http.postJson(url, headers, body)
                val parsed = json.decodeFromString(OpenAiChunkDto.serializer(), raw)
                val message = parsed.choices.firstOrNull()?.message
                message?.content?.takeIf { it.isNotEmpty() }?.let { emit(ChatChunk.Delta(it)) }
                message?.toolCalls.orEmpty().forEachIndexed { index, dto ->
                    val name = dto.function?.name ?: return@forEachIndexed
                    emit(
                        ChatChunk.ToolCallChunk(
                            ToolCall(
                                id = dto.id ?: "call_$index",
                                name = name,
                                arguments = dto.function?.arguments ?: "{}",
                            ),
                        ),
                    )
                }
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

    /** Visible for tests: exact JSON body sent to the provider. */
    internal fun buildRequestBody(request: ChatRequest, json: Json): String {
        val messages = buildJsonArray {
            request.messages.forEach { turn -> add(turnToJson(turn, json)) }
        }
        val body = buildJsonObject {
            put("model", request.modelId)
            put("messages", messages)
            put("temperature", request.params.temperature)
            put("max_tokens", request.params.maxTokens)
            put("top_p", request.params.topP)
            put("stream", request.params.streaming)
            if (request.params.streaming) {
                putJsonObject("stream_options") { put("include_usage", true) }
            }
            if (request.tools.isNotEmpty()) {
                put("tools", toolsToJson(request.tools, json))
            }
        }
        return json.encodeToString(JsonObject.serializer(), body)
    }

    private fun turnToJson(turn: ChatTurn, json: Json): JsonObject = buildJsonObject {
        put("role", turn.role.wireName)
        when {
            turn.role == MessageRole.TOOL -> {
                put("tool_call_id", turn.toolCallId ?: "")
                put("content", turn.content)
            }

            turn.toolCalls.isNotEmpty() -> {
                if (turn.content.isBlank()) put("content", JsonNull) else put("content", turn.content)
                putJsonArray("tool_calls") {
                    turn.toolCalls.forEach { call ->
                        addJsonObject {
                            put("id", call.id)
                            put("type", "function")
                            putJsonObject("function") {
                                put("name", call.name)
                                put("arguments", call.arguments)
                            }
                        }
                    }
                }
            }

            else -> put("content", turn.content)
        }
    }

    private fun toolsToJson(tools: List<AgentTool>, json: Json): JsonArray = buildJsonArray {
        tools.forEach { tool ->
            addJsonObject {
                put("type", "function")
                putJsonObject("function") {
                    put("name", tool.name)
                    put("description", tool.description)
                    put("parameters", parseSchema(tool.parametersJson, json))
                }
            }
        }
    }

    private fun parseSchema(raw: String, json: Json) = runCatching {
        json.parseToJsonElement(raw)
    }.getOrElse { JsonObject(emptyMap()) }

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
        Outcome.Success(
            ModelCatalog.filter(
                parsed.data.map {
                    RemoteModel(
                        remoteId = it.id,
                        displayName = it.id,
                        contextWindow = it.contextWindow ?: ModelCatalog.defaultContextWindow(it.id),
                    )
                },
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: ProviderException) {
        Outcome.Failure(e.error)
    } catch (e: Exception) {
        Outcome.Failure(e.toAppError())
    }

    @Suppress("unused")
    private fun primitive(value: String?) = value?.let { JsonPrimitive(it) } ?: JsonNull
}
