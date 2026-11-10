package com.personalaiagent.data.remote

import com.personalaiagent.core.AppError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Turns a provider's HTTP failure into an [AppError] that says *what* happened.
 *
 * Before this existed every non-2xx response became a bare code ("the provider
 * returned an error (429)"), which is why the app kept showing "an error
 * happened" with no way to tell a rate limit from an exhausted quota from a
 * prompt that is simply too long. Everything here is pure so it can be tested
 * without a network.
 */
object ProviderErrors {

    /** Bodies are decoration: never read more than a few KB of an error page. */
    const val MAX_DETAIL_CHARS = 400

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val contextHints = listOf(
        "context length",
        "context_length",
        "maximum context",
        "context window",
        "prompt is too long",
        "too many tokens",
        "token limit",
        "max_tokens",
        "reduce the length",
        "input is too long",
    )

    private val quotaHints = listOf(
        "insufficient_quota",
        "quota exceeded",
        "exceeded your current quota",
        "billing",
        "credit balance",
        "payment required",
        "out of credit",
        "no credit",
    )

    /**
     * The human sentence inside a provider error body.
     *
     * OpenAI, Anthropic, Gemini-compat and most gateways put it at
     * `error.message`; a few use `message`, `error` or a plain string.
     */
    fun detailOf(body: String?): String? {
        val raw = body?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val parsed = runCatching { json.parseToJsonElement(raw) }.getOrNull()
        val fromJson = when (parsed) {
            is JsonObject -> {
                val error = parsed["error"]
                when (error) {
                    is JsonObject -> error["message"]?.stringOrNull()
                        ?: error["detail"]?.stringOrNull()
                        ?: error["code"]?.stringOrNull()
                    is JsonPrimitive -> error.stringOrNull()
                    else -> parsed["message"]?.stringOrNull()
                        ?: parsed["detail"]?.stringOrNull()
                }
            }
            is JsonPrimitive -> parsed.stringOrNull()
            else -> null
        }
        val text = (fromJson ?: raw).replace(Regex("\\s+"), " ").trim()
        return text.take(MAX_DETAIL_CHARS).ifBlank { null }
    }

    /**
     * Maps an HTTP failure onto the error the UI understands.
     *
     * `retryAfterSeconds` comes from the `Retry-After` header (or the
     * rate-limit reset headers some gateways use).
     */
    fun toAppError(
        code: Int,
        body: String? = null,
        retryAfterSeconds: Int? = null,
    ): AppError {
        val detail = detailOf(body)
        val haystack = (body ?: "").lowercase()

        if (code == 429 || code == 503) {
            if (quotaHints.any { haystack.contains(it) } || code == 402) {
                return AppError.QuotaExceeded(detail)
            }
            if (code == 429) return AppError.RateLimit(retryAfterSeconds, detail)
        }
        if (code == 402) return AppError.QuotaExceeded(detail)
        if (contextHints.any { haystack.contains(it) } && code in 400..499) {
            return AppError.ContextTooLong
        }
        return when (code) {
            401, 403 -> AppError.Auth
            404 -> AppError.HttpNotFound
            408, 504 -> AppError.Timeout
            in 500..599 -> AppError.Server(code, detail)
            // 400/405/415/422 …: a real provider rejection. Keep the code and the
            // sentence instead of replacing both with "something went wrong".
            in 400..499 -> AppError.Server(code, detail)
            else -> AppError.Unknown(if (detail != null) "HTTP $code · $detail" else "HTTP $code")
        }
    }

    /**
     * `Retry-After` is either delta-seconds ("12") or an HTTP date.
     * Returns null when absent/unparseable; the retry policy then uses backoff.
     */
    fun retryAfterSeconds(header: String?, nowMillis: Long = System.currentTimeMillis()): Int? {
        val raw = header?.trim().orEmpty()
        if (raw.isEmpty()) return null
        raw.toIntOrNull()?.let { return it.coerceIn(0, 600) }
        val date = runCatching {
            java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
                .parse(raw, java.time.ZonedDateTime::from)
        }.getOrNull() ?: return null
        val seconds = (date.toInstant().toEpochMilli() - nowMillis) / 1000
        return seconds.coerceIn(0, 600).toInt()
    }

    private fun kotlinx.serialization.json.JsonElement.stringOrNull(): String? =
        (this as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    /** Escapes a provider sentence for one-line display in a log row. */
    fun summarize(error: AppError): String = when (error) {
        is AppError.RateLimit -> error.detail?.take(80) ?: "rate limited"
        is AppError.QuotaExceeded -> error.detail?.take(80) ?: "quota exhausted"
        is AppError.Server -> error.detail?.take(80) ?: "http ${error.code}"
        AppError.ContextTooLong -> "context too long"
        else -> error.toString()
    }
}
