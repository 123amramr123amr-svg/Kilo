package com.personalaiagent.data.remote

import com.personalaiagent.domain.model.ModelCapability
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.model.RemoteModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun `filters out embedding and audio models`() {
        val models = listOf(
            RemoteModel("text-embedding-3-large", "text-embedding-3-large", null),
            RemoteModel("whisper-1", "whisper-1", null),
            RemoteModel("tts-1", "tts-1", null),
            RemoteModel("gpt-4o", "gpt-4o", 128_000),
        )
        val filtered = ModelCatalog.filter(models)
        assertEquals(listOf("gpt-4o"), filtered.map { it.remoteId })
    }

    @Test
    fun `keeps unknown custom model ids`() {
        val models = listOf(RemoteModel("my-own-model", "my-own-model", null))
        assertEquals(1, ModelCatalog.filter(models).size)
    }

    @Test
    fun `never returns an empty list when the provider answered`() {
        val models = listOf(RemoteModel("text-embedding-ada-002", "embed", null))
        assertEquals(1, ModelCatalog.filter(models).size)
    }

    @Test
    fun `deduplicates model ids`() {
        val models = listOf(
            RemoteModel("gpt-4o", "gpt-4o", null),
            RemoteModel("gpt-4o", "GPT-4o", null),
        )
        assertEquals(1, ModelCatalog.filter(models).size)
    }

    @Test
    fun `infers vision capability for multimodal ids`() {
        assertTrue(ModelCapability.VISION in ModelCatalog.inferCapabilities("gpt-4o"))
        assertTrue(ModelCapability.VISION in ModelCatalog.inferCapabilities("claude-3-5-sonnet"))
        assertFalse(ModelCapability.VISION in ModelCatalog.inferCapabilities("deepseek-chat"))
    }

    @Test
    fun `infers reasoning and code capabilities`() {
        assertTrue(ModelCapability.REASONING in ModelCatalog.inferCapabilities("o1-preview"))
        assertTrue(ModelCapability.CODE in ModelCatalog.inferCapabilities("codestral-latest"))
        assertTrue(ModelCapability.TEXT in ModelCatalog.inferCapabilities("anything"))
    }

    @Test
    fun `matches known context windows`() {
        assertEquals(1_000_000, ModelCatalog.defaultContextWindow("gemini-1.5-pro"))
        assertEquals(200_000, ModelCatalog.defaultContextWindow("claude-3-5-sonnet"))
        assertEquals(null, ModelCatalog.defaultContextWindow("unknown-model"))
    }

    @Test
    fun `bundled catalog is available for both provider kinds`() {
        assertTrue(ModelCatalog.bundled(ProviderKind.OPENAI_COMPATIBLE).isNotEmpty())
        assertTrue(ModelCatalog.bundled(ProviderKind.ANTHROPIC).isNotEmpty())
    }
}
