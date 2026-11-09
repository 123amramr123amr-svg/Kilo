package com.personalaiagent.data.remote

import com.personalaiagent.core.AppError
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamDecoderTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun `openai decoder emits delta text`() {
        val decoder = OpenAiStreamDecoder(json)
        val chunk = decoder.decode(
            SseEvent(null, """{"choices":[{"delta":{"content":"مرحبا"}}]}"""),
        )
        assertEquals(ChatChunk.Delta("مرحبا"), chunk)
    }

    @Test
    fun `openai decoder emits usage when present`() {
        val decoder = OpenAiStreamDecoder(json)
        val chunk = decoder.decode(
            SseEvent(
                null,
                """{"choices":[],"usage":{"prompt_tokens":12,"completion_tokens":34}}""",
            ),
        )
        assertEquals(ChatChunk.Usage(12, 34), chunk)
    }

    @Test
    fun `openai decoder signals completion on DONE`() {
        val decoder = OpenAiStreamDecoder(json)
        assertEquals(ChatChunk.Completed, decoder.decode(SseEvent(null, "[DONE]")))
    }

    @Test
    fun `openai decoder ignores role-only chunk`() {
        val decoder = OpenAiStreamDecoder(json)
        assertNull(decoder.decode(SseEvent(null, """{"choices":[{"delta":{"role":"assistant"}}]}""")))
    }

    @Test
    fun `openai decoder maps malformed json to parse error`() {
        val decoder = OpenAiStreamDecoder(json)
        val error = runCatching { decoder.decode(SseEvent(null, "{oops")) }.exceptionOrNull()
        assertTrue(error is ProviderException)
        assertEquals(AppError.Parse, (error as ProviderException).error)
    }

    @Test
    fun `anthropic decoder emits text delta`() {
        val decoder = AnthropicStreamDecoder(json)
        val chunk = decoder.decode(
            SseEvent(
                "content_block_delta",
                """{"type":"content_block_delta","delta":{"type":"text_delta","text":"Hi"}}""",
            ),
        )
        assertEquals(ChatChunk.Delta("Hi"), chunk)
    }

    @Test
    fun `anthropic decoder reads input tokens from message start`() {
        val decoder = AnthropicStreamDecoder(json)
        val chunk = decoder.decode(
            SseEvent(
                "message_start",
                """{"type":"message_start","message":{"id":"m","usage":{"input_tokens":7}}}""",
            ),
        )
        assertEquals(ChatChunk.Usage(7, null), chunk)
    }

    @Test
    fun `anthropic decoder reads output tokens from message delta`() {
        val decoder = AnthropicStreamDecoder(json)
        val chunk = decoder.decode(
            SseEvent("message_delta", """{"type":"message_delta","usage":{"output_tokens":21}}"""),
        )
        assertEquals(ChatChunk.Usage(null, 21), chunk)
    }

    @Test
    fun `anthropic decoder completes on message stop`() {
        val decoder = AnthropicStreamDecoder(json)
        assertEquals(ChatChunk.Completed, decoder.decode(SseEvent("message_stop", """{"type":"message_stop"}""")))
    }

    @Test
    fun `anthropic error event becomes provider exception`() {
        val decoder = AnthropicStreamDecoder(json)
        val error = runCatching {
            decoder.decode(SseEvent("error", """{"type":"error","error":{"type":"overloaded_error"}}"""))
        }.exceptionOrNull()
        assertTrue(error is ProviderException)
    }
}
