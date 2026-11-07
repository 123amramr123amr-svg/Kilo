package com.personalaiagent.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SseStreamParserTest {

    @Test
    fun `parses a single data event on blank line`() {
        val parser = SseStreamParser()
        assertNull(parser.consume("data: {\"a\":1}"))
        val event = parser.consume("")
        assertEquals("{\"a\":1}", event?.data)
        assertNull(event?.event)
    }

    @Test
    fun `parses named event`() {
        val parser = SseStreamParser()
        assertNull(parser.consume("event: content_block_delta"))
        assertNull(parser.consume("data: hello"))
        val event = parser.consume("")
        assertEquals("content_block_delta", event?.event)
        assertEquals("hello", event?.data)
    }

    @Test
    fun `joins multiple data lines with newline`() {
        val parser = SseStreamParser()
        parser.consume("data: line1")
        parser.consume("data: line2")
        assertEquals("line1\nline2", parser.consume("")?.data)
    }

    @Test
    fun `ignores comment lines`() {
        val parser = SseStreamParser()
        assertNull(parser.consume(": keep-alive"))
        assertNull(parser.consume(""))
    }

    @Test
    fun `flush emits trailing event without blank line`() {
        val parser = SseStreamParser()
        parser.consume("data: [DONE]")
        assertEquals("[DONE]", parser.flush()?.data)
        assertNull(parser.flush())
    }

    @Test
    fun `data field without colon yields empty payload`() {
        val parser = SseStreamParser()
        parser.consume("data")
        assertEquals("", parser.consume("")?.data)
    }

    @Test
    fun `resets state between events`() {
        val parser = SseStreamParser()
        parser.consume("event: a")
        parser.consume("data: 1")
        parser.consume("")
        parser.consume("data: 2")
        val second = parser.consume("")
        assertNull(second?.event)
        assertEquals("2", second?.data)
    }
}
