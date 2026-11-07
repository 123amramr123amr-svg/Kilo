package com.personalaiagent.data.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    private fun sampleBundle() = BackupBundle(
        version = 1,
        exportedAt = 1_700_000_000_000,
        appVersion = "1.0.0",
        settings = BackupSettings(language = "ar", displayName = "Ahmed"),
        providers = listOf(
            BackupProvider(
                id = "p1",
                name = "OpenAI",
                kind = "OPENAI_COMPATIBLE",
                baseUrl = "https://api.openai.com/v1",
                isDefault = true,
                status = "CONNECTED",
                lastCheckedAt = 1_700_000_000_000,
                createdAt = 1_700_000_000_000,
            ),
        ),
        models = listOf(
            BackupModel(
                id = "p1::gpt-4o",
                providerId = "p1",
                remoteId = "gpt-4o",
                displayName = "gpt-4o",
                capabilities = "TEXT,VISION",
                contextWindow = 128_000,
                enabled = true,
                isDefault = true,
                temperature = 0.7f,
                maxTokens = 1024,
                topP = 1f,
                systemPrompt = "",
                streaming = true,
                sortOrder = 0,
            ),
        ),
        conversations = listOf(
            BackupConversation("c1", "Hello", 1, 2, "p1", "p1::gpt-4o"),
        ),
        messages = listOf(
            BackupMessage("m1", "c1", "USER", "مرحبا", 1, "COMPLETE"),
        ),
    )

    @Test
    fun `round trips through json`() {
        val encoded = json.encodeToString(BackupBundle.serializer(), sampleBundle())
        val decoded = json.decodeFromString(BackupBundle.serializer(), encoded)
        assertEquals(sampleBundle(), decoded)
    }

    @Test
    fun `arabic content survives serialisation`() {
        val encoded = json.encodeToString(BackupBundle.serializer(), sampleBundle())
        val decoded = json.decodeFromString(BackupBundle.serializer(), encoded)
        assertEquals("مرحبا", decoded.messages.single().content)
    }

    @Test
    fun `backup never claims to contain keys`() {
        val encoded = json.encodeToString(BackupBundle.serializer(), sampleBundle())
        assertFalse(sampleBundle().keysIncluded)
        assertFalse(encoded.contains("\"apiKey\""))
        assertFalse(encoded.contains("sk-"))
    }

    @Test
    fun `future schema versions are detectable`() {
        val bundle = sampleBundle().copy(version = 99)
        val encoded = json.encodeToString(BackupBundle.serializer(), bundle)
        val decoded = json.decodeFromString(BackupBundle.serializer(), encoded)
        assertTrue(decoded.version > 1)
    }
}
