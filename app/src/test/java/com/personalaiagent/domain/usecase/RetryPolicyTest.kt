package com.personalaiagent.domain.usecase

import com.personalaiagent.core.AppError
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyTest {

    private val policy = RetryPolicy(random = Random(7))

    @Test
    fun `a rate limit is retried with backoff`() {
        val decision = policy.decide(AppError.RateLimit(), attempt = 1)
        assertTrue(decision.retry)
        assertTrue(decision.delayMillis in 1_500L..2_500L)
        assertEquals("rate_limited", decision.reason)
    }

    @Test
    fun `the provider's Retry-After wins over the backoff`() {
        val decision = policy.decide(AppError.RateLimit(retryAfterSeconds = 12), attempt = 1)
        assertEquals(12_000L, decision.delayMillis)
    }

    @Test
    fun `an absurd Retry-After is capped instead of parking the app`() {
        val decision = policy.decide(AppError.RateLimit(retryAfterSeconds = 600), attempt = 1)
        assertEquals(RetryPolicy.DEFAULT_MAX_RETRY_AFTER_MS, decision.delayMillis)
    }

    @Test
    fun `backoff grows with every attempt`() {
        val first = policy.delayFor(AppError.Network, attempt = 1, retryAfterSeconds = null)
        val second = policy.delayFor(AppError.Network, attempt = 2, retryAfterSeconds = null)
        val third = policy.delayFor(AppError.Network, attempt = 3, retryAfterSeconds = null)
        assertTrue("$first !< $second", first < second)
        assertTrue("$second !< $third", second < third)
        assertTrue(third <= RetryPolicy.DEFAULT_MAX_DELAY_MS + 100L)
    }

    @Test
    fun `it gives up after the last attempt`() {
        val decision = policy.decide(AppError.Timeout, attempt = RetryPolicy.DEFAULT_MAX_ATTEMPTS)
        assertFalse(decision.retry)
    }

    @Test
    fun `a bad key is never retried`() {
        assertFalse(policy.decide(AppError.Auth, attempt = 1).retry)
        assertFalse(policy.decide(AppError.RateLimit(), attempt = 1, partialAnswerShown = true).retry)
    }

    @Test
    fun `an exhausted quota is never retried`() {
        assertFalse(policy.decide(AppError.QuotaExceeded("no credit"), attempt = 1).retry)
    }

    @Test
    fun `a 400 from the provider is not retried but a 503 is`() {
        assertFalse(policy.decide(AppError.Server(400), attempt = 1).retry)
        assertTrue(policy.decide(AppError.Server(503), attempt = 1).retry)
    }

    @Test
    fun `a stream cut in the middle is not resumed automatically`() {
        // Text is already on screen: retrying would duplicate the answer.
        val decision = policy.decide(AppError.Network, attempt = 1, partialAnswerShown = true)
        assertFalse(decision.retry)
    }

    @Test
    fun `a conversation that is too long is shortened then retried once`() {
        val first = policy.decide(AppError.ContextTooLong, attempt = 1)
        assertTrue(first.retry)
        assertTrue(first.trimContext)
        assertEquals(0L, first.delayMillis)

        val second = policy.decide(AppError.ContextTooLong, attempt = 2, trimmedContextAlready = true)
        assertFalse(second.retry)
    }
}
