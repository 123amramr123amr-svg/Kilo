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
) {
    /** True when the row still carries the placeholder created before the first message. */
    val isUntitled: Boolean
        get() = title.isBlank() || title == "New conversation" || title == "محادثة جديدة"
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
