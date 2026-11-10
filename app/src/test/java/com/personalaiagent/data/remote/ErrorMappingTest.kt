package com.personalaiagent.data.remote

import com.personalaiagent.core.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorMappingTest {

    @Test
    fun `auth and rate limit keep their own cases`() {
        assertEquals(AppError.Auth, httpStatusToAppError(401))
        assertEquals(AppError.Auth, httpStatusToAppError(403))
        assertEquals(AppError.RateLimit(), httpStatusToAppError(429))
    }

    @Test
    fun `404 becomes the actionable endpoint error`() {
        assertEquals(AppError.HttpNotFound, httpStatusToAppError(404))
    }

    @Test
    fun `other client errors carry the status code`() {
        assertEquals(AppError.Server(400), httpStatusToAppError(400))
        assertEquals(AppError.Server(422), httpStatusToAppError(422))
        assertEquals(AppError.Server(405), httpStatusToAppError(405))
    }

    @Test
    fun `server and timeout codes are unchanged`() {
        assertEquals(AppError.Server(503), httpStatusToAppError(503))
        assertEquals(AppError.Timeout, httpStatusToAppError(504))
        assertEquals(AppError.Timeout, httpStatusToAppError(408))
    }

    @Test
    fun `no 4xx or 5xx falls back to the generic unknown error`() {
        (400..599).forEach { code ->
            val error = httpStatusToAppError(code)
            assertTrue("status $code leaked as $error", error !is AppError.Unknown)
        }
    }
}
