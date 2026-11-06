package com.personalaiagent.data.remote

import com.personalaiagent.core.AppError
import java.io.IOException
import java.io.InterruptedIOException

data class SseEvent(val event: String?, val data: String)

/**
 * Minimal, allocation-light Server-Sent-Events parser.
 *
 * Implements the subset used by OpenAI-compatible and Anthropic streaming APIs:
 * `event:` and `data:` fields, one event per blank line, `:` comments ignored.
 */
class SseStreamParser {

    private var eventName: String? = null
    private val data = StringBuilder()

    /** Feeds one line (without line terminator). Returns an event when it is complete. */
    fun consume(line: String): SseEvent? {
        if (line.isEmpty()) return flush()
        if (line.startsWith(":")) return null

        val colon = line.indexOf(':')
        val field = if (colon >= 0) line.substring(0, colon) else line
        var value = if (colon >= 0) line.substring(colon + 1) else ""
        if (value.startsWith(" ")) value = value.substring(1)

        when (field) {
            "event" -> eventName = value
            "data" -> {
                if (data.isNotEmpty()) data.append('\n')
                data.append(value)
            }
            // "id" and "retry" are irrelevant for these APIs.
            else -> Unit
        }
        return null
    }

    /** Flushes a pending event (used when the stream ends without a blank line). */
    fun flush(): SseEvent? {
        if (eventName == null && data.isEmpty()) return null
        val result = SseEvent(eventName, data.toString())
        eventName = null
        data.clear()
        return result
    }

    companion object {
        const val DONE = "[DONE]"
    }
}

/** Maps transport failures onto user-facing [AppError]s. */
fun Throwable.toAppError(): AppError = when (this) {
    is ProviderException -> error
    is InterruptedIOException -> AppError.Timeout
    is IOException -> AppError.Network
    is kotlinx.coroutines.CancellationException -> AppError.Cancelled
    else -> AppError.Unknown(message)
}

fun httpStatusToAppError(code: Int): AppError = when {
    code == 401 || code == 403 -> AppError.Auth
    code == 429 -> AppError.RateLimit
    code >= 500 -> AppError.Server(code)
    code == 408 || code == 504 -> AppError.Timeout
    else -> AppError.Unknown("HTTP $code")
}
