package com.personalaiagent.data.remote

import com.personalaiagent.domain.model.AgentTool
import com.personalaiagent.domain.model.ChatTurn
import com.personalaiagent.domain.model.ModelParams
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.model.ToolCall

/** Everything a client needs to perform one completion. */
data class ChatRequest(
    val kind: ProviderKind,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val messages: List<ChatTurn>,
    val params: ModelParams,
    /** Empty when the workspace is not connected or tools are disabled. */
    val tools: List<AgentTool> = emptyList(),
)

/** Incremental output of a completion. */
sealed interface ChatChunk {
    data class Delta(val text: String) : ChatChunk

    /** A slice of the model's visible reasoning, when the provider exposes it. */
    data class Reasoning(val text: String) : ChatChunk
    data class Usage(val tokenIn: Int?, val tokenOut: Int?) : ChatChunk

    /** Complete tool call, emitted once the provider finished streaming it. */
    data class ToolCallChunk(val call: ToolCall) : ChatChunk
    data object Completed : ChatChunk
}

/**
 * A failure raised by a provider call.
 *
 * [retryAfterSeconds] and [httpCode] come from the response itself, so the retry
 * loop can wait exactly as long as the provider asked instead of guessing.
 */
class ProviderException(
    val error: com.personalaiagent.core.AppError,
    cause: Throwable? = null,
    val retryAfterSeconds: Int? = null,
    val httpCode: Int? = null,
) : Exception(cause?.message ?: error.toString(), cause)
