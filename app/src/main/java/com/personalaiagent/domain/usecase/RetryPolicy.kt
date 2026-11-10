package com.personalaiagent.domain.usecase

import com.personalaiagent.core.AppError
import kotlin.math.min
import kotlin.random.Random

/**
 * What to do after a failed attempt.
 *
 * [delayMillis] is how long to wait before the next attempt; it is 0 when
 * [retry] is false. [trimContext] asks the caller to shrink the prompt before
 * retrying (the provider said the conversation is too long).
 */
data class RetryDecision(
    val retry: Boolean,
    val delayMillis: Long = 0L,
    val trimContext: Boolean = false,
    /** Short, non-sensitive reason for the execution log. */
    val reason: String = "",
) {
    companion object {
        val GiveUp = RetryDecision(retry = false)
    }
}

/**
 * The retry policy for a single model request.
 *
 * The old code retried **once after a flat 1.2 s** and only for three error
 * kinds, which is why a busy provider (HTTP 429) or a cold model (5xx) surfaced
 * to the user as "the provider failed". This policy:
 *
 *  * retries network blips, timeouts, 429 and 5xx, plus a stream that closed
 *    without its `[DONE]` terminator;
 *  * waits **exactly** what `Retry-After` said when the provider sent one,
 *    otherwise backs off exponentially (2 s → 4 s → 8 s, capped) with jitter;
 *  * never retries a bad key, an exhausted quota, a missing model or a prompt
 *    that is too long (that last one is *trimmed* instead — see [trimContext]).
 *
 * Pure and deterministic when [random] is seeded, so the behaviour is tested.
 */
class RetryPolicy(
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val baseDelayMillis: Long = DEFAULT_BASE_DELAY_MS,
    private val maxDelayMillis: Long = DEFAULT_MAX_DELAY_MS,
    private val maxRetryAfterMillis: Long = DEFAULT_MAX_RETRY_AFTER_MS,
    private val random: Random = Random.Default,
) {

    /**
     * @param attempt the attempt that just failed (1 for the first try).
     * @param retryAfterSeconds the provider's own hint, when it sent one.
     * @param partialAnswerShown true when text was already rendered: retrying
     *   then would duplicate the answer, so the policy refuses.
     * @param trimmedContextAlready true when the context was already shortened
     *   once for this request (never trim twice).
     */
    fun decide(
        error: AppError,
        attempt: Int,
        retryAfterSeconds: Int? = null,
        partialAnswerShown: Boolean = false,
        trimmedContextAlready: Boolean = false,
    ): RetryDecision {
        if (error == AppError.ContextTooLong && !trimmedContextAlready) {
            // The prompt itself is the problem: send a shorter one.
            return RetryDecision(retry = true, delayMillis = 0L, trimContext = true, reason = "context_too_long")
        }
        if (partialAnswerShown) return RetryDecision.GiveUp
        if (!error.isRetryable()) return RetryDecision.GiveUp
        if (attempt >= maxAttempts) return RetryDecision.GiveUp
        return RetryDecision(
            retry = true,
            delayMillis = delayFor(error, attempt, retryAfterSeconds),
            reason = error.retryReason(),
        )
    }

    /** The wait before attempt `attempt + 1`. */
    fun delayFor(error: AppError, attempt: Int, retryAfterSeconds: Int?): Long {
        val providerHint = retryAfterSeconds?.let { it.coerceAtLeast(0) * 1_000L }
        if (providerHint != null && providerHint > 0L) {
            return min(providerHint, maxRetryAfterMillis)
        }
        // 2 s, 4 s, 8 s … capped, ±20 % jitter so many devices do not stampede.
        val exponential = baseDelayMillis shl (attempt - 1).coerceIn(0, 6)
        val capped = min(exponential, maxDelayMillis)
        val jitter = (capped * 0.2).toLong().coerceAtLeast(1L)
        val offset = random.nextLong(-jitter, jitter + 1)
        return (capped + offset).coerceIn(200L, maxDelayMillis + jitter)
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 4
        const val DEFAULT_BASE_DELAY_MS = 2_000L
        const val DEFAULT_MAX_DELAY_MS = 20_000L
        const val DEFAULT_MAX_RETRY_AFTER_MS = 60_000L
    }
}

/** Transient enough that trying again is reasonable. */
internal fun AppError.isRetryable(): Boolean = when (this) {
    AppError.Network, AppError.Timeout -> true
    is AppError.RateLimit -> true
    is AppError.Server -> code >= 500 || code == 408 || code == 409
    // Never: a wrong key, an empty account, a missing model, a bad prompt.
    else -> false
}

/** Stable reason for the execution log / activity panel. */
internal fun AppError.retryReason(): String = when (this) {
    is AppError.RateLimit -> "rate_limited"
    is AppError.Server -> "http_$code"
    AppError.Timeout -> "timeout"
    AppError.Network -> "network"
    AppError.ContextTooLong -> "context_too_long"
    else -> "retry"
}
