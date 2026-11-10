package com.personalaiagent.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TokenUsageTest {

    @Test
    fun `zero is empty so no card shows a meaningless chip`() {
        assertEquals("", TokenUsage().short())
    }

    @Test
    fun `small numbers stay exact`() {
        assertEquals("42", TokenUsage(40, 2).short())
    }

    @Test
    fun `thousands and millions are shortened`() {
        assertEquals("8.5k", TokenUsage(4_000, 4_500).short())
        assertEquals("21k", TokenUsage(12_000, 9_000).short())
        assertEquals("1.2M", TokenUsage(1_000_000, 200_000).short())
    }

    @Test
    fun `total adds both directions`() {
        assertEquals(30, TokenUsage(20, 10).total)
    }
}
