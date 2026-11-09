package com.personalaiagent.domain.model

data class AiModel(
    val id: String,
    val providerId: String,
    val providerName: String,
    val remoteId: String,
    val displayName: String,
    val capabilities: Set<ModelCapability>,
    val contextWindow: Int?,
    val enabled: Boolean,
    val isDefault: Boolean,
    val params: ModelParams,
)

data class ModelParams(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val topP: Float = 1f,
    val systemPrompt: String = "",
    val streaming: Boolean = true,
) {
    companion object {
        const val MIN_TEMPERATURE = 0f
        const val MAX_TEMPERATURE = 2f
        const val MIN_MAX_TOKENS = 64
        const val MAX_MAX_TOKENS = 8192
        const val DEFAULT_MAX_TOKENS = 1024
        const val MAX_SYSTEM_PROMPT_LENGTH = 4000
    }
}

/** A model as returned by a provider's /models endpoint. */
data class RemoteModel(
    val remoteId: String,
    val displayName: String,
    val contextWindow: Int?,
)
