package com.personalaiagent.domain.usecase

/**
 * Names a conversation from the first thing the user said.
 *
 * It is deliberately local: no extra model call, no extra tokens, and the name
 * is available the moment the message is sent, so an old chat can always be
 * recognised in the history list.
 */
object ConversationTitles {

    const val MAX_LENGTH = 48

    /** Fallbacks written before the first message; treated as "no name yet". */
    private val placeholders = setOf("new conversation", "محادثة جديدة", "untitled conversation")

    /**
     * The first line that still has words in it, cleaned of markdown, collapsed
     * to one line and capped at [maxLength]. Empty when there is nothing usable
     * (for example a prompt that is only an attachment chip).
     */
    fun from(prompt: String, maxLength: Int = MAX_LENGTH): String {
        val text = prompt.lineSequence()
            .map { cleanLine(it) }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
        if (text.isEmpty()) return ""
        if (text.lowercase() in placeholders) return ""
        return if (text.length <= maxLength) text else text.take(maxLength).trimEnd() + "…"
    }

    private fun cleanLine(line: String): String = line
        // "## ", "> ", "- " — heading, quote and bullet markers.
        .replace(Regex("^[#>*\\-+\\s]+"), "")
        // Code fences: a line that is only "```" becomes empty and is skipped.
        .replace(Regex("`{1,3}"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trim('.', ',', ':', ';', '،', '؟', '?', '!', '"', '\'')
}
