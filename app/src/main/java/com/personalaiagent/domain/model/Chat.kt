package com.personalaiagent.domain.model

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val providerId: String?,
    val modelId: String?,
)

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
)

/** A single tool invocation requested by the model. */
data class ToolCall(
    val id: String,
    val name: String,
    /** Raw JSON object with the arguments, as produced by the model. */
    val arguments: String = "{}",
)
