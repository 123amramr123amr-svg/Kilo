package com.personalaiagent.data.remote

import com.personalaiagent.domain.model.ChatTurn
import com.personalaiagent.domain.model.ModelParams
import com.personalaiagent.domain.model.ProviderKind

/** Everything a client needs to perform one completion. */
data class ChatRequest(
    val kind: ProviderKind,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val messages: List<ChatTurn>,
    val params: ModelParams,
)

/** Incremental output of a completion. */
sealed interface ChatChunk {
    data class Delta(val text: String) : ChatChunk
    data class Usage(val tokenIn: Int?, val tokenOut: Int?) : ChatChunk
    data object Completed : ChatChunk
}

class ProviderException(val error: com.personalaiagent.core.AppError, cause: Throwable? = null) :
    Exception(cause?.message ?: error.toString(), cause)
