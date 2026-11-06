package com.personalaiagent.data.remote

import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.model.RemoteModel
import kotlinx.coroutines.flow.Flow

interface ProviderClient {
    fun stream(request: ChatRequest): Flow<ChatChunk>
    suspend fun fetchModels(provider: Provider, apiKey: String, baseUrlOverride: String? = null): Outcome<List<RemoteModel>>
}

class ProviderClientRouter(
    private val openAi: ProviderClient,
    private val anthropic: ProviderClient,
) {
    fun forKind(kind: ProviderKind): ProviderClient = when (kind) {
        ProviderKind.OPENAI_COMPATIBLE -> openAi
        ProviderKind.ANTHROPIC -> anthropic
    }
}
