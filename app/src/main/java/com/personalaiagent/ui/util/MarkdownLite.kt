package com.personalaiagent.ui.util

/**
 * Very small Markdown subset used to render assistant answers with modern
 * typography (headings, bullets, quotes, fenced code, bold/italic/inline code).
 *
 * Parsing is pure Kotlin so it can be unit tested; the Compose layer only turns
 * these blocks into an `AnnotatedString`.
 */
sealed interface MdBlock {
    data class Heading(val level: Int, val inlines: List<MdInline>) : MdBlock
    data class Paragraph(val inlines: List<MdInline>) : MdBlock
    data class Bullet(val marker: String, val inlines: List<MdInline>) : MdBlock
    data class Quote(val inlines: List<MdInline>) : MdBlock
    data class Code(val language: String?, val code: String) : MdBlock
}

enum class MdStyle { PLAIN, BOLD, ITALIC, CODE }

data class MdInline(val text: String, val style: MdStyle = MdStyle.PLAIN)

object MarkdownLite {

    fun parse(raw: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val paragraph = mutableListOf<String>()
        val code = StringBuilder()
        var inCode = false
        var codeLanguage: String? = null

        fun flushParagraph() {
            if (paragraph.isEmpty()) return
            val text = paragraph.joinToString("\n").trim()
            paragraph.clear()
            if (text.isNotEmpty()) blocks += MdBlock.Paragraph(inline(text))
        }

        raw.replace("\r\n", "\n").lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("```") -> {
                    if (inCode) {
                        blocks += MdBlock.Code(codeLanguage, code.toString().trimEnd('\n'))
                        code.clear()
                        codeLanguage = null
                        inCode = false
                    } else {
                        flushParagraph()
                        codeLanguage = trimmed.removePrefix("```").trim().ifEmpty { null }
                        inCode = true
                    }
                }

                inCode -> code.append(line).append('\n')

                trimmed.isEmpty() -> flushParagraph()

                trimmed.startsWith("#") -> {
                    flushParagraph()
                    val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
                    val text = trimmed.drop(level).trim()
                    if (text.isNotEmpty()) blocks += MdBlock.Heading(level, inline(text))
                }

                trimmed.startsWith("> ") || trimmed == ">" -> {
                    flushParagraph()
                    blocks += MdBlock.Quote(inline(trimmed.removePrefix(">").trim()))
                }

                BULLET.matches(trimmed) -> {
                    flushParagraph()
                    val marker = trimmed.substringBefore(' ').ifEmpty { "•" }
                    blocks += MdBlock.Bullet(normalizeMarker(marker), inline(trimmed.substringAfter(' ').trim()))
                }

                else -> paragraph += trimmed
            }
        }
        if (inCode) blocks += MdBlock.Code(codeLanguage, code.toString().trimEnd('\n'))
        flushParagraph()
        return blocks
    }

    private fun normalizeMarker(marker: String): String =
        if (marker.firstOrNull()?.isDigit() == true) marker else "•"

    private val BULLET = Regex("^([-*•]|\\d{1,2}[.)])\\s+.+")

    /** Splits a line into styled runs: `**bold**`, `*italic*`, `` `code` ``. */
    fun inline(text: String): List<MdInline> {
        val result = mutableListOf<MdInline>()
        val buffer = StringBuilder()
        var index = 0

        fun flush() {
            if (buffer.isNotEmpty()) {
                result += MdInline(buffer.toString())
                buffer.clear()
            }
        }

        while (index < text.length) {
            val rest = text.substring(index)
            val match = PATTERN.find(rest)
            if (match == null || match.range.first > 0) {
                // Copy plain text up to the next marker (or the end).
                val plainEnd = if (match == null) text.length else index + match.range.first
                buffer.append(text, index, plainEnd)
                index = plainEnd
                if (match == null) break
            } else {
                flush()
                val style = when {
                    match.value.startsWith("**") -> MdStyle.BOLD
                    match.value.startsWith("`") -> MdStyle.CODE
                    else -> MdStyle.ITALIC
                }
                result += MdInline(match.groupValues[1], style)
                index += match.value.length
            }
        }
        flush()
        return result.ifEmpty { listOf(MdInline(text)) }
    }

    private val PATTERN = Regex("(\\*\\*[^*]+\\*\\*|`[^`]+`|\\*[^*]+\\*)")
}
