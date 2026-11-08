package com.personalaiagent.data.remote

import com.personalaiagent.core.AppConstants
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Client for the Anthropic Messages API (`/v1/messages`), including tool use
 * (`tool_use` blocks and `tool_result` turns).
 */
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
            val body = buildRequestBody(request, json)

            if (request.params.streaming) {
                val toolCalls = AnthropicToolCallAccumulator()
                http.streamEvents(url, headers, body).collect { event ->
                    val dto = runCatching {
                        json.decodeFromString(AnthropicStreamEventDto.serializer(), event.data.trim())
                    }.getOrNull() ?: return@collect

                    when (dto.type ?: event.event) {
                        "content_block_start" -> {
                            val block = dto.contentBlock
                            if (block?.type == "tool_use") {
                                toolCalls.start(dto.index ?: 0, block.id, block.name)
                            }
                        }

                        "content_block_delta" -> {
                            when (dto.delta?.type) {
                                "text_delta" -> dto.delta?.text?.takeIf { it.isNotEmpty() }
                                    ?.let { emit(ChatChunk.Delta(it)) }

                                "input_json_delta" ->
                                    toolCalls.appendJson(dto.index ?: 0, dto.delta?.partialJson)
                            }
                        }

                        "message_start" -> dto.message?.usage?.inputTokens?.let {
                            emit(ChatChunk.Usage(it, null))
                        }

                        "message_delta" -> dto.usage?.outputTokens?.let {
                            emit(ChatChunk.Usage(null, it))
                        }

                        "error" -> throw ProviderException(com.personalaiagent.core.AppError.Server(0))
                    }
                }
                toolCalls.build().forEach { emit(ChatChunk.ToolCallChunk(it)) }
            } else {
                val raw = http.postJson(url, headers, body)
                val parsed = json.decodeFromString(AnthropicMessageResponseDto.serializer(), raw)
                parsed.content.forEach { block ->
                    when (block.type) {
                        "text" -> block.text?.takeIf { it.isNotEmpty() }?.let { emit(ChatChunk.Delta(it)) }
                        "tool_use" -> block.name?.let { name ->
                            emit(
                                ChatChunk.ToolCallChunk(
                                    ToolCall(
                                        id = block.id ?: "tool_0",
                                        name = name,
                                        arguments = block.input?.toString() ?: "{}",
                                    ),
                                ),
                            )
                        }
                    }
                }
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

    /** Visible for tests: exact JSON body sent to the provider. */
    internal fun buildRequestBody(request: ChatRequest, json: Json): String {
        val system = request.params.systemPrompt.ifBlank { null }
        val body = buildJsonObject {
            put("model", request.modelId)
            put("max_tokens", request.params.maxTokens)
            put("temperature", request.params.temperature)
            put("top_p", request.params.topP)
            put("stream", request.params.streaming)
            if (system != null) put("system", system)
            put("messages", messagesToJson(request.messages, json))
            if (request.tools.isNotEmpty()) put("tools", toolsToJson(request.tools, json))
        }
        return json.encodeToString(JsonObject.serializer(), body)
    }

    /**
     * Anthropic is stricter than OpenAI: no system turns, alternating user/assistant,
     * and tool results are user turns carrying `tool_result` blocks.
     */
    private fun messagesToJson(turns: List<ChatTurn>, json: Json): JsonArray = buildJsonArray {
        turns.filter { it.role != MessageRole.SYSTEM }.forEach { turn ->
            addJsonObject {
                when {
                    turn.role == MessageRole.TOOL -> {
                        put("role", "user")
                        putJsonArray("content") {
                            addJsonObject {
                                put("type", "tool_result")
                                put("tool_use_id", turn.toolCallId ?: "")
                                put("content", turn.content)
                            }
                        }
                    }

                    turn.toolCalls.isNotEmpty() -> {
                        put("role", "assistant")
                        putJsonArray("content") {
                            if (turn.content.isNotBlank()) {
                                addJsonObject {
                                    put("type", "text")
                                    put("text", turn.content)
                                }
                            }
                            turn.toolCalls.forEach { call ->
                                addJsonObject {
                                    put("type", "tool_use")
                                    put("id", call.id)
                                    put("name", call.name)
                                    put("input", parseJsonObject(call.arguments, json))
                                }
                            }
                        }
                    }

                    turn.attachments.isNotEmpty() -> {
                        put("role", "user")
                        putJsonArray("content") {
                            addJsonObject {
                                put("type", "text")
                                put("text", turn.content)
                            }
                            turn.attachments.forEach { attachment ->
                                addJsonObject {
                                    put("type", "image")
                                    putJsonObject("source") {
                                        put("type", "base64")
                                        put("media_type", attachment.mimeType)
                                        put("data", attachment.base64)
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        put("role", if (turn.role == MessageRole.ASSISTANT) "assistant" else "user")
                        put("content", turn.content)
                    }
                }
            }
        }
    }

    private fun toolsToJson(tools: List<AgentTool>, json: Json): JsonArray = buildJsonArray {
        tools.forEach { tool ->
            addJsonObject {
                put("name", tool.name)
                put("description", tool.description)
                put("input_schema", parseJsonObject(tool.parametersJson, json))
            }
        }
    }

    private fun parseJsonObject(raw: String, json: Json): JsonObject = runCatching {
        json.parseToJsonElement(raw.ifBlank { "{}" }) as? JsonObject
    }.getOrNull() ?: JsonObject(emptyMap())

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
                    contextWindow = ModelCatalog.defaultContextWindow(it.id),
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
