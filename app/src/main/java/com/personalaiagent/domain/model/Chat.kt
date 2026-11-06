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

data class ChatTurn(
    val role: MessageRole,
    val content: String,
)
