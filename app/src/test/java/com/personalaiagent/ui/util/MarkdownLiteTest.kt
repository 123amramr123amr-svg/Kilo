package com.personalaiagent.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownLiteTest {

    @Test
    fun `parses headings, bullets and paragraphs`() {
        val blocks = MarkdownLite.parse(
            """
            ## خطة العمل
            سأقوم بالآتي:
            - قراءة الملف
            1. كتابة الملخص
            """.trimIndent(),
        )

        assertEquals(MdBlock.Heading::class, blocks[0]::class)
        assertEquals(2, (blocks[0] as MdBlock.Heading).level)
        assertEquals("•", (blocks[2] as MdBlock.Bullet).marker)
        assertEquals("1.", (blocks[3] as MdBlock.Bullet).marker)
    }

    @Test
    fun `fenced code keeps its language and content`() {
        val blocks = MarkdownLite.parse("before\n```kotlin\nval x = 1\n```\nafter")
        val code = blocks.filterIsInstance<MdBlock.Code>().single()
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1", code.code)
        assertEquals(3, blocks.size)
    }

    @Test
    fun `inline styles are extracted`() {
        val parts = MarkdownLite.inline("Use **bold** and `code` and *soft*")
        assertEquals(MdStyle.BOLD, parts.first { it.text == "bold" }.style)
        assertEquals(MdStyle.CODE, parts.first { it.text == "code" }.style)
        assertEquals(MdStyle.ITALIC, parts.first { it.text == "soft" }.style)
        assertEquals("Use bold and code and soft", parts.joinToString("") { it.text })
    }

    @Test
    fun `plain text stays a single run`() {
        val parts = MarkdownLite.inline("مرحبا بلا أي تنسيق")
        assertEquals(1, parts.size)
        assertEquals(MdStyle.PLAIN, parts.single().style)
    }

    @Test
    fun `unclosed code fence is still rendered as code`() {
        val blocks = MarkdownLite.parse("```\nstill code")
        assertTrue(blocks.single() is MdBlock.Code)
    }

    @Test
    fun `empty input produces no blocks`() {
        assertTrue(MarkdownLite.parse("   \n\n ").isEmpty())
    }
}
