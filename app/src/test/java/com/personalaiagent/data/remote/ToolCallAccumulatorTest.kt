package com.personalaiagent.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallAccumulatorTest {

    @Test
    fun `openai stream fragments are joined into one call`() {
        val accumulator = OpenAiToolCallAccumulator()
        accumulator.add(0, "call_abc", "write_file", "{\"path\":")
        accumulator.add(0, null, null, "\"notes/a.txt\",")
        accumulator.add(0, null, null, "\"content\":\"hi\"}")

        val calls = accumulator.build()
        assertEquals(1, calls.size)
        assertEquals("call_abc", calls[0].id)
        assertEquals("write_file", calls[0].name)
        assertEquals("{\"path\":\"notes/a.txt\",\"content\":\"hi\"}", calls[0].arguments)
    }

    @Test
    fun `parallel tool calls keep their own argument buffers`() {
        val accumulator = OpenAiToolCallAccumulator()
        accumulator.add(0, "a", "list_files", "{\"path\":")
        accumulator.add(1, "b", "read_file", "{\"path\":\"x")
        accumulator.add(0, null, null, "\"\"}")
        accumulator.add(1, null, null, "\"}")

        val calls = accumulator.build()
        assertEquals(listOf("a", "b"), calls.map { it.id })
        assertEquals("{\"path\":\"\"}", calls[0].arguments)
        assertEquals("{\"path\":\"x\"}", calls[1].arguments)
    }

    @Test
    fun `anthropic tool_use blocks accumulate partial json`() {
        val accumulator = AnthropicToolCallAccumulator()
        accumulator.start(1, "toolu_1", "write_file")
        accumulator.appendJson(1, "{\"path\":\"a.txt\",")
        accumulator.appendJson(1, "\"content\":\"x\"}")

        val calls = accumulator.build()
        assertEquals(1, calls.size)
        assertEquals("toolu_1", calls[0].id)
        assertEquals("{\"path\":\"a.txt\",\"content\":\"x\"}", calls[0].arguments)
    }

    @Test
    fun `calls without a name are ignored`() {
        val accumulator = OpenAiToolCallAccumulator()
        accumulator.add(0, "id", null, "{}")
        assertTrue(accumulator.build().isEmpty())
    }

    @Test
    fun `missing arguments default to an empty object`() {
        val accumulator = AnthropicToolCallAccumulator()
        accumulator.start(0, "t", "list_files")
        assertEquals("{}", accumulator.build().single().arguments)
    }
}
