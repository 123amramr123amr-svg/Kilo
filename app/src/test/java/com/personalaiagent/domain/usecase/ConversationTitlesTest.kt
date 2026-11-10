package com.personalaiagent.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationTitlesTest {

    @Test
    fun `the first line names the conversation`() {
        assertEquals(
            "اكتب لي رسالة شكر لمديري",
            ConversationTitles.from("اكتب لي رسالة شكر لمديري\n\nوتكون رسمية"),
        )
    }

    @Test
    fun `markdown noise and repeated spaces are removed`() {
        assertEquals(
            "ملخص الملف",
            ConversationTitles.from("###   ملخص الملف  "),
        )
        assertEquals(
            "code sample",
            ConversationTitles.from("```\ncode sample\n```"),
        )
    }

    @Test
    fun `trailing punctuation is dropped`() {
        assertEquals("what is a pyramid", ConversationTitles.from("what is a pyramid?"))
        assertEquals("ايه هو الهرم", ConversationTitles.from("ايه هو الهرم؟"))
    }

    @Test
    fun `a long prompt is shortened on one line`() {
        val title = ConversationTitles.from("a".repeat(200))
        assertEquals(ConversationTitles.MAX_LENGTH + 1, title.length)
        assertEquals('…', title.last())
    }

    @Test
    fun `a placeholder is not a name`() {
        assertEquals("", ConversationTitles.from("New conversation"))
        assertEquals("", ConversationTitles.from("   \n  "))
    }
}
