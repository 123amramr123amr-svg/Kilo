package com.personalaiagent.domain.repository

import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.model.GeneratedImage

/**
 * Image generation through an OpenAI-compatible `/images/generations` endpoint
 * (DALL·E 3, gpt-image-1, and compatible gateways).
 */
interface ImageGeneration {
    fun isLikelySupported(remoteModelId: String): Boolean

    suspend fun generate(
        providerId: String,
        modelRemoteId: String,
        prompt: String,
        size: String,
    ): Outcome<GeneratedImage>
}
