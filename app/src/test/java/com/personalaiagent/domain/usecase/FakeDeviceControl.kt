package com.personalaiagent.domain.usecase

import com.personalaiagent.core.AppError
import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.model.GeneratedImage
import com.personalaiagent.domain.repository.AppInfo
import com.personalaiagent.domain.repository.DeviceControl
import com.personalaiagent.domain.repository.ImageGeneration

/** Records the device actions a tool would have started. */
class FakeDeviceControl(
    private val apps: List<AppInfo> = listOf(
        AppInfo("WhatsApp", "com.whatsapp"),
        AppInfo("Chrome", "com.android.chrome"),
    ),
) : DeviceControl {

    val actions = mutableListOf<String>()

    override suspend fun launchableApps(limit: Int): List<AppInfo> = apps.take(limit)

    override suspend fun openApp(query: String): Outcome<String> {
        val match = apps.firstOrNull { it.label.contains(query, ignoreCase = true) }
            ?: return Outcome.Failure(AppError.Unknown("app_not_found"))
        actions += "open_app:${match.packageName}"
        return Outcome.Success("Opened ${match.label}")
    }

    override suspend fun openUrl(url: String): Outcome<String> {
        actions += "open_url:$url"
        return Outcome.Success("Opened $url")
    }

    override suspend fun webSearch(query: String): Outcome<String> {
        actions += "web_search:$query"
        return Outcome.Success("Searched for \"$query\"")
    }

    override suspend fun shareText(text: String): Outcome<String> {
        actions += "share:$text"
        return Outcome.Success("Shared")
    }
}

class FakeImageGeneration(
    private val supported: Boolean = true,
) : ImageGeneration {

    var lastPrompt: String? = null

    override fun isLikelySupported(remoteModelId: String): Boolean = supported

    override suspend fun generate(
        providerId: String,
        modelRemoteId: String,
        prompt: String,
        size: String,
    ): Outcome<GeneratedImage> {
        lastPrompt = prompt
        return if (supported) {
            Outcome.Success(GeneratedImage(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47), "image/png"))
        } else {
            Outcome.Failure(AppError.Unknown("image_unsupported"))
        }
    }
}
