package com.personalaiagent.domain.usecase

import com.personalaiagent.core.AppError
import com.personalaiagent.domain.model.ProviderDraft
import com.personalaiagent.domain.model.ProviderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderValidatorTest {

    private fun draft(name: String = "My provider", url: String = "https://api.example.com/v1") =
        ProviderDraft(name = name, kind = ProviderKind.OPENAI_COMPATIBLE, baseUrl = url)

    @Test
    fun `accepts https url`() {
        assertNull(ProviderValidator.validate(draft()))
    }

    @Test
    fun `rejects blank name`() {
        val error = ProviderValidator.validate(draft(name = "   "))
        assertTrue(error is AppError.InvalidInput)
        assertEquals("name", (error as AppError.InvalidInput).reason)
    }

    @Test
    fun `rejects malformed url`() {
        val error = ProviderValidator.validate(draft(url = "not a url"))
        assertEquals("url", (error as AppError.InvalidInput).reason)
    }

    @Test
    fun `rejects unsupported scheme`() {
        val error = ProviderValidator.validate(draft(url = "ftp://example.com/v1"))
        assertEquals("url", (error as AppError.InvalidInput).reason)
    }

    @Test
    fun `rejects plain http for public hosts`() {
        val error = ProviderValidator.validate(draft(url = "http://api.example.com/v1"))
        assertEquals("insecure_url", (error as AppError.InvalidInput).reason)
    }

    @Test
    fun `allows plain http for local endpoints`() {
        assertNull(ProviderValidator.validate(draft(url = "http://localhost:1234/v1")))
        assertNull(ProviderValidator.validate(draft(url = "http://10.0.2.2:8080/v1")))
        assertNull(ProviderValidator.validate(draft(url = "http://192.168.1.20:8080/v1")))
    }

    @Test
    fun `normalises trailing slashes and whitespace`() {
        assertEquals(
            "https://api.example.com/v1",
            ProviderValidator.normalizeBaseUrl("  https://api.example.com/v1///  "),
        )
    }

    @Test
    fun `classifies private hosts`() {
        assertTrue(ProviderValidator.isLocalHost("127.0.0.1"))
        assertTrue(ProviderValidator.isLocalHost("172.16.5.4"))
        assertTrue(ProviderValidator.isLocalHost("[::1]"))
        assertTrue(!ProviderValidator.isLocalHost("8.8.8.8"))
    }
}
