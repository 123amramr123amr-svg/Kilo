package com.personalaiagent.domain.usecase

import com.personalaiagent.core.AppError
import com.personalaiagent.domain.model.ProviderDraft

/**
 * Validation shared by the provider form and by tests.
 *
 * Security note: plain HTTP is accepted only for loopback / private-network
 * hosts so an API key is never sent unencrypted over the public internet.
 */
object ProviderValidator {

    fun normalizeBaseUrl(raw: String): String = raw.trim().trimEnd('/')

    fun validate(draft: ProviderDraft): AppError? {
        if (draft.name.isBlank()) return AppError.InvalidInput("name")
        val url = normalizeBaseUrl(draft.baseUrl)
        if (url.isBlank()) return AppError.InvalidInput("url")
        val uri = runCatching { java.net.URI(url) }.getOrNull()
            ?: return AppError.InvalidInput("url")
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return AppError.InvalidInput("url")
        val host = uri.host
        if (host.isNullOrBlank()) return AppError.InvalidInput("url")
        if (scheme == "http" && !isLocalHost(host)) return AppError.InvalidInput("insecure_url")
        return null
    }

    fun isLocalHost(host: String): Boolean {
        val h = host.lowercase().trim('[', ']')
        if (h == "localhost" || h == "::1" || h == "10.0.2.2") return true
        if (h.startsWith("127.")) return true
        val parts = h.split('.')
        if (parts.size == 4 && parts.all { it.toIntOrNull() != null }) {
            val first = parts[0].toInt()
            val second = parts[1].toInt()
            if (first == 10) return true
            if (first == 192 && second == 168) return true
            if (first == 172 && second in 16..31) return true
        }
        return false
    }
}
