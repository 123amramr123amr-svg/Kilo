package com.personalaiagent.data.remote

import com.personalaiagent.core.AppError
import kotlinx.serialization.json.Json

/**
 * Translates provider SSE payloads into [ChatChunk]s.
 * Both decoders are pure functions of the event, which keeps them unit-testable.
 */
class OpenAiStreamDecoder(private val json: Json) {

    /** Tool-call fragments seen in the most recent [decode] call (may be parallel). */
    var lastToolCallDeltas: List<OpenAiToolCallDto> = emptyList()
        private set

    fun decode(event: SseEvent): ChatChunk? {
        lastToolCallDeltas = emptyList()
        val data = event.data.trim()
        if (data.isEmpty()) return null
        if (data == SseStreamParser.DONE) return ChatChunk.Completed

        val chunk = try {
            json.decodeFromString(OpenAiChunkDto.serializer(), data)
        } catch (e: Exception) {
            throw ProviderException(AppError.Parse, e)
        }
        lastToolCallDeltas = chunk.choices.firstOrNull()?.delta?.toolCalls.orEmpty()

        // "Thinking" models stream their reasoning first; surface it separately so
        // the chat can show it in the activity panel and never mix it into the answer.
        val reasoning = chunk.choices.firstOrNull()?.delta?.let { it.reasoningContent ?: it.reasoning }
        if (!reasoning.isNullOrEmpty()) return ChatChunk.Reasoning(reasoning)

        val usage = chunk.usage
        if (usage?.promptTokens != null || usage?.completionTokens != null) {
            // Usage arrives on its own final chunk in OpenAI streams; emit text first.
            val text = chunk.choices.firstOrNull()?.delta?.content
            return when {
                !text.isNullOrEmpty() -> ChatChunk.Delta(text)
                chunk.choices.any { it.finishReason != null } -> ChatChunk.Usage(usage?.promptTokens, usage?.completionTokens)
                else -> ChatChunk.Usage(usage?.promptTokens, usage?.completionTokens)
            }
        }
        val text = chunk.choices.firstOrNull()?.delta?.content
        return if (!text.isNullOrEmpty()) ChatChunk.Delta(text) else null
    }
}

class AnthropicStreamDecoder(private val json: Json) {

    fun decode(event: SseEvent): ChatChunk? {
        val data = event.data.trim()
        if (data.isEmpty() || data == SseStreamParser.DONE) return null

        val dto = try {
            json.decodeFromString(AnthropicStreamEventDto.serializer(), data)
        } catch (e: Exception) {
            throw ProviderException(AppError.Parse, e)
        }

        val type = dto.type ?: event.event
        return when (type) {
            "content_block_delta" -> dto.delta?.text
                ?.takeIf { it.isNotEmpty() }
                ?.let { ChatChunk.Delta(it) }

            "message_start" -> dto.message?.usage?.inputTokens?.let { ChatChunk.Usage(it, null) }

            "message_delta" -> dto.usage?.outputTokens?.let { ChatChunk.Usage(null, it) }

            "message_stop" -> ChatChunk.Completed

            "error" -> throw ProviderException(AppError.Server(0))

            else -> null
        }
    }
}
