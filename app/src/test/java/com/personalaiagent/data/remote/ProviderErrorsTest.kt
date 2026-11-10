package com.personalaiagent.data.remote

import com.personalaiagent.core.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderErrorsTest {

    @Test
    fun `the provider's own sentence is kept for the user`() {
        val body = """{"error":{"message":"Rate limit reached for gpt-4o on requests per min","type":"requests"}}"""
        assertEquals(
            "Rate limit reached for gpt-4o on requests per min",
            ProviderErrors.detailOf(body),
        )
    }

    @Test
    fun `a plain text error page is still readable`() {
        assertEquals("upstream connect error", ProviderErrors.detailOf("upstream connect error"))
        assertNull(ProviderErrors.detailOf("   "))
        assertNull(ProviderErrors.detailOf(null))
    }

    @Test
    fun `429 becomes a rate limit carrying the provider detail`() {
        val error = ProviderErrors.toAppError(
            429,
            """{"error":{"message":"Too many requests"}}""",
            retryAfterSeconds = 5,
        )
        assertTrue(error is AppError.RateLimit)
        error as AppError.RateLimit
        assertEquals(5, error.retryAfterSeconds)
        assertEquals("Too many requests", error.detail)
    }

    @Test
    fun `429 with a quota message is not retried as a rate limit`() {
        val error = ProviderErrors.toAppError(
            429,
            """{"error":{"message":"You exceeded your current quota, please check your plan"}}""",
        )
        assertTrue(error is AppError.QuotaExceeded)
    }

    @Test
    fun `402 is an empty account`() {
        assertTrue(ProviderErrors.toAppError(402, null) is AppError.QuotaExceeded)
    }

    @Test
    fun `a context length rejection is recognised`() {
        val error = ProviderErrors.toAppError(
            400,
            """{"error":{"message":"This model's maximum context length is 8192 tokens"}}""",
        )
        assertEquals(AppError.ContextTooLong, error)
    }

    @Test
    fun `a normal 400 keeps its code and sentence`() {
        val error = ProviderErrors.toAppError(400, """{"error":{"message":"model is required"}}""")
        assertEquals(AppError.Server(400, "model is required"), error)
    }

    @Test
    fun `Retry-After is read as seconds`() {
        assertEquals(12, ProviderErrors.retryAfterSeconds("12"))
        assertEquals(0, ProviderErrors.retryAfterSeconds("0"))
        assertNull(ProviderErrors.retryAfterSeconds(null))
        assertNull(ProviderErrors.retryAfterSeconds("soon"))
    }

    @Test
    fun `Retry-After is also read as an HTTP date`() {
        val now = 0L
        // 1970-01-01T00:00:30Z in RFC 1123 form.
        assertEquals(30, ProviderErrors.retryAfterSeconds("Thu, 01 Jan 1970 00:00:30 GMT", now))
    }

    @Test
    fun `the status-only mapping never changes behaviour`() {
        assertEquals(AppError.Auth, httpStatusToAppError(401))
        assertEquals(AppError.HttpNotFound, httpStatusToAppError(404))
        assertEquals(AppError.Timeout, httpStatusToAppError(504))
        assertEquals(AppError.Server(503), httpStatusToAppError(503))
    }
}
