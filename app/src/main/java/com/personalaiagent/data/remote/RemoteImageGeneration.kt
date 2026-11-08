package com.personalaiagent.data.remote

import android.util.Base64
import com.personalaiagent.core.AppError
import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.model.GeneratedImage
import com.personalaiagent.domain.repository.CredentialStore
import com.personalaiagent.domain.repository.ImageGeneration
import com.personalaiagent.domain.repository.ProviderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Image generation for OpenAI-compatible providers (`POST /images/generations`).
 *
 * Works with `dall-e-3`, `gpt-image-1` and gateways that expose the same shape.
 * Both `b64_json` and `url` responses are supported; the caller stores the bytes
 * inside the user's workspace.
 */
@Singleton
class RemoteImageGeneration @Inject constructor(
    private val http: HttpEngine,
    private val json: Json,
    private val providerRepository: ProviderRepository,
    private val credentialStore: CredentialStore,
) : ImageGeneration {

    override fun isLikelySupported(remoteModelId: String): Boolean {
        val id = remoteModelId.lowercase()
        return IMAGE_MODEL_HINTS.any { id.contains(it) }
    }

    override suspend fun generate(
        providerId: String,
        modelRemoteId: String,
        prompt: String,
        size: String,
    ): Outcome<GeneratedImage> {
        if (prompt.isBlank()) return Outcome.Failure(AppError.InvalidInput("prompt"))
        val provider = providerRepository.getProvider(providerId)
            ?: return Outcome.Failure(AppError.NoProvider)
        val key = credentialStore.read(providerId)
            ?: return Outcome.Failure(AppError.MissingKey(provider.name))

        return try {
            val body = buildJsonObject {
                put("model", modelRemoteId)
                put("prompt", prompt)
                put("n", 1)
                put("size", size)
                put("response_format", "b64_json")
            }
            val raw = http.postJson(
                "${provider.baseUrl.trimEnd('/')}/images/generations",
                mapOf(
                    "Authorization" to "Bearer $key",
                    "Content-Type" to "application/json",
                    "Accept" to "application/json",
                ),
                json.encodeToString(JsonObject.serializer(), body),
            )
            parse(raw)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ProviderException) {
            Outcome.Failure(e.error)
        } catch (e: Exception) {
            Outcome.Failure(e.toAppError())
        }
    }

    private suspend fun parse(raw: String): Outcome<GeneratedImage> {
        val root = runCatching { json.parseToJsonElement(raw) as JsonObject }.getOrNull()
            ?: return Outcome.Failure(AppError.Parse)
        val item = (root["data"] as? kotlinx.serialization.json.JsonArray)
            ?.firstOrNull() as? JsonObject
            ?: return Outcome.Failure(AppError.Parse)

        val base64 = item["b64_json"]?.jsonPrimitive?.content
        if (!base64.isNullOrBlank()) {
            val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull()
                ?: return Outcome.Failure(AppError.Parse)
            return Outcome.Success(GeneratedImage(bytes, mimeType = guessMime(bytes)))
        }

        val url = item["url"]?.jsonPrimitive?.content
        if (!url.isNullOrBlank()) {
            val bytes = runCatching { http.getBytes(url) }.getOrNull()
                ?: return Outcome.Failure(AppError.Network)
            return Outcome.Success(GeneratedImage(bytes, mimeType = guessMime(bytes)))
        }

        return Outcome.Failure(AppError.Parse)
    }

    private fun guessMime(bytes: ByteArray): String = when {
        bytes.size > 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "image/png"
        bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
        else -> "image/png"
    }

    private companion object {
        val IMAGE_MODEL_HINTS = listOf("dall-e", "gpt-image", "imagen", "flux", "sd-", "stable-diffusion")
    }
}
