package com.personalaiagent.data.remote

import com.personalaiagent.domain.model.ModelCapability
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.model.RemoteModel

/**
 * Decides which entries of a provider `/models` response are usable chat models
 * and which capabilities they likely support.
 *
 * Provider catalogs contain embeddings, TTS and image models that cannot be used
 * in a chat request; showing them would only produce confusing runtime errors.
 */
object ModelCatalog {

    private val NON_CHAT_HINTS = listOf(
        "embed", "whisper", "tts", "dall-e", "moderation", "audio", "realtime",
        "transcribe", "rerank", "bge", "guard", "image", "vision-preview",
        "speech", "sora", "davinci", "curie", "babbage", "ada",
    )

    fun isChatModel(remoteId: String): Boolean {
        val id = remoteId.lowercase()
        // Unknown ids are kept: custom/self-hosted endpoints often use their own names.
        return NON_CHAT_HINTS.none { id.contains(it) }
    }

    fun filter(models: List<RemoteModel>): List<RemoteModel> {
        val chatOnly = models.filter { isChatModel(it.remoteId) }
        // Never return an empty catalog for a provider that did answer.
        val result = chatOnly.ifEmpty { models }
        return result.distinctBy { it.remoteId }.sortedBy { it.remoteId.lowercase() }
    }

    fun inferCapabilities(remoteId: String): Set<ModelCapability> {
        val id = remoteId.lowercase()
        val caps = mutableSetOf(ModelCapability.TEXT)
        if (listOf("gpt-4o", "gpt-4.1", "gpt-5", "claude-3", "claude-4", "gemini", "vision", "llava", "pixtral", "qwen-vl").any { id.contains(it) }) {
            caps += ModelCapability.VISION
        }
        if (listOf("gpt-4", "gpt-5", "claude", "gemini", "llama-3.3", "llama-4", "mistral-large", "deepseek", "qwen").any { id.contains(it) }) {
            caps += ModelCapability.TOOLS
        }
        if (listOf("o1", "o3", "o4", "reason", "thinking", "r1", "qwq").any { id.contains(it) }) {
            caps += ModelCapability.REASONING
        }
        if (listOf("code", "codestral", "coder", "deepseek-coder").any { id.contains(it) }) {
            caps += ModelCapability.CODE
        }
        return caps
    }

    fun defaultContextWindow(remoteId: String): Int? {
        val id = remoteId.lowercase()
        return when {
            id.contains("gemini") -> 1_000_000
            id.contains("claude") -> 200_000
            id.contains("gpt-4o") || id.contains("gpt-4.1") -> 128_000
            id.contains("gpt-4") -> 128_000
            id.contains("llama") -> 128_000
            id.contains("qwen") -> 32_768
            id.contains("deepseek") -> 64_000
            else -> null
        }
    }

    /**
     * Offline starter catalog, used only when the provider cannot be reached and
     * the provider has no models stored yet, so the UI is never a dead end.
     */
    fun bundled(kind: ProviderKind): List<RemoteModel> = when (kind) {
        ProviderKind.OPENAI_COMPATIBLE -> listOf(
            RemoteModel("gpt-4o-mini", "gpt-4o-mini", 128_000),
            RemoteModel("gpt-4o", "gpt-4o", 128_000),
        )
        ProviderKind.ANTHROPIC -> listOf(
            RemoteModel("claude-3-5-sonnet-latest", "Claude 3.5 Sonnet", 200_000),
            RemoteModel("claude-3-5-haiku-latest", "Claude 3.5 Haiku", 200_000),
        )
    }
}
