package com.personalaiagent.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// OpenAI-compatible wire format
// ---------------------------------------------------------------------------

@Serializable
data class OpenAiToolCallFunctionDto(
    val name: String? = null,
    val arguments: String? = null,
)

@Serializable
data class OpenAiToolCallDto(
    val index: Int? = null,
    val id: String? = null,
    val type: String? = null,
    val function: OpenAiToolCallFunctionDto? = null,
)

@Serializable
data class OpenAiMessageDto(
    val role: String,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenAiToolCallDto> = emptyList(),
    @SerialName("tool_call_id") val toolCallId: String? = null,
)

@Serializable
data class OpenAiStreamOptionsDto(
    @SerialName("include_usage") val includeUsage: Boolean = true,
)

@Serializable
data class OpenAiChatRequestDto(
    val model: String,
    val messages: List<OpenAiMessageDto>,
    val temperature: Float,
    @SerialName("max_tokens") val maxTokens: Int,
    @SerialName("top_p") val topP: Float,
    val stream: Boolean,
    @SerialName("stream_options") val streamOptions: OpenAiStreamOptionsDto? = null,
)

@Serializable
data class OpenAiDeltaDto(
    val role: String? = null,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenAiToolCallDto> = emptyList(),
)

@Serializable
data class OpenAiChoiceDto(
    val index: Int = 0,
    val delta: OpenAiDeltaDto? = null,
    val message: OpenAiMessageDto? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class OpenAiUsageDto(
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    @SerialName("completion_tokens") val completionTokens: Int? = null,
)

@Serializable
data class OpenAiChunkDto(
    val choices: List<OpenAiChoiceDto> = emptyList(),
    val usage: OpenAiUsageDto? = null,
)

@Serializable
data class OpenAiModelDto(
    val id: String,
    @SerialName("owned_by") val ownedBy: String? = null,
    @SerialName("context_window") val contextWindow: Int? = null,
)

@Serializable
data class OpenAiModelListDto(
    val data: List<OpenAiModelDto> = emptyList(),
)

// ---------------------------------------------------------------------------
// Anthropic Messages wire format
// ---------------------------------------------------------------------------

@Serializable
data class AnthropicMessageDto(
    val role: String,
    val content: String,
)

@Serializable
data class AnthropicRequestDto(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val temperature: Float,
    @SerialName("top_p") val topP: Float,
    val system: String? = null,
    val messages: List<AnthropicMessageDto>,
    val stream: Boolean,
)

@Serializable
data class AnthropicDeltaDto(
    val type: String? = null,
    val text: String? = null,
    @SerialName("partial_json") val partialJson: String? = null,
    @SerialName("stop_reason") val stopReason: String? = null,
)

@Serializable
data class AnthropicContentBlockDto(
    val type: String? = null,
    val id: String? = null,
    val name: String? = null,
    val text: String? = null,
    val input: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
data class AnthropicUsageDto(
    @SerialName("input_tokens") val inputTokens: Int? = null,
    @SerialName("output_tokens") val outputTokens: Int? = null,
)

@Serializable
data class AnthropicContentDto(
    val type: String? = null,
    val text: String? = null,
)

@Serializable
data class AnthropicStreamEventDto(
    val type: String? = null,
    val index: Int? = null,
    val delta: AnthropicDeltaDto? = null,
    val usage: AnthropicUsageDto? = null,
    val message: AnthropicMessageResponseDto? = null,
    @SerialName("content_block") val contentBlock: AnthropicContentBlockDto? = null,
    val content: List<AnthropicContentBlockDto> = emptyList(),
)

@Serializable
data class AnthropicMessageResponseDto(
    val id: String? = null,
    val content: List<AnthropicContentBlockDto> = emptyList(),
    val usage: AnthropicUsageDto? = null,
)

@Serializable
data class AnthropicModelDto(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class AnthropicModelListDto(
    val data: List<AnthropicModelDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("last_id") val lastId: String? = null,
)
