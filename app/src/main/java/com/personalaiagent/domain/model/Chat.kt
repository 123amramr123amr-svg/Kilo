package com.personalaiagent.domain.model

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val providerId: String?,
    val modelId: String?,
)

/**
 * One row of the conversation history: enough to draw a card without loading
 * every message of every chat into memory.
 */
data class ConversationSummary(
    val id: String,
    val title: String,
    val preview: String,
    val updatedAt: Long,
    val messageCount: Int,
    val pinned: Boolean = false,
    /** Prompt tokens summed over every run in this conversation. */
    val tokensIn: Int = 0,
    /** Completion tokens summed over every run in this conversation. */
    val tokensOut: Int = 0,
) {
    val totalTokens: Int get() = tokensIn + tokensOut

    /** True when the row still carries the placeholder created before the first message. */
    val isUntitled: Boolean
        get() = title.isBlank() || title == "New conversation" || title == "محادثة جديدة"
}

/** How many tokens a conversation (or the whole app) has spent. */
data class TokenUsage(
    val tokensIn: Int = 0,
    val tokensOut: Int = 0,
) {
    val total: Int get() = tokensIn + tokensOut

    /**
     * "1.2k" / "12.4k" / "1.2M" — short enough for a chip, exact enough to be useful.
     * Empty for zero so a card never shows a meaningless "0 tokens".
     */
    fun short(): String = when {
        total <= 0 -> ""
        total < 1_000 -> total.toString()
        total < 10_000 -> oneDecimal(total / 1_000.0) + "k"
        total < 1_000_000 -> "${total / 1_000}k"
        else -> oneDecimal(total / 1_000_000.0) + "M"
    }

    private fun oneDecimal(value: Double): String {
        val rounded = kotlin.math.round(value * 10) / 10
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val createdAt: Long,
    val status: MessageStatus,
    val providerId: String? = null,
    val modelId: String? = null,
    val tokenIn: Int? = null,
    val tokenOut: Int? = null,
    val errorMessage: String? = null,
)

/**
 * A turn sent to a provider.
 *
 * Besides plain user/assistant/system turns, this carries the two extra shapes
 * needed for tool calling: an assistant turn that requests tools, and a tool
 * turn that carries one result back.
 */
data class ChatTurn(
    val role: MessageRole,
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    /** Set on [MessageRole.TOOL] turns: the call this result belongs to. */
    val toolCallId: String? = null,
    /** Images/PDF pages attached to a user turn (vision models only). */
    val attachments: List<Attachment> = emptyList(),
)

/** A single tool invocation requested by the model. */
data class ToolCall(
    val id: String,
    val name: String,
    /** Raw JSON object with the arguments, as produced by the model. */
    val arguments: String = "{}",
)
