package com.personalaiagent.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {

    @Test
    fun `a new provider route carries no provider id`() {
        // Regression: the chat setup card navigated to Routes.PROVIDER_EDIT itself, so
        // providerId arrived as the literal "{providerId}" and every save answered
        // "provider not found" instead of creating a provider.
        assertEquals("provider_edit", Routes.providerEdit(null))
        assertFalse(Routes.providerEdit(null).contains("?"))
        assertEquals("provider_edit", Routes.providerEdit(""))
        assertEquals("provider_edit", Routes.providerEdit("  "))
    }

    @Test
    fun `an existing provider id is url encoded`() {
        val route = Routes.providerEdit("a b/c?d&e=f")
        assertTrue(route.startsWith("provider_edit?providerId="))
        assertEquals("provider_edit?providerId=a%20b%2Fc%3Fd%26e%3Df", route)
    }

    @Test
    fun `plain ids are left readable`() {
        val uuid = "3f2b8c1e-9a4d-4c77-8f0e-123456789abc"
        assertEquals("provider_edit?providerId=$uuid", Routes.providerEdit(uuid))
        assertEquals("model_settings/$uuid", Routes.modelSettings(uuid))
        assertEquals("task_detail/$uuid", Routes.taskDetail(uuid))
    }

    @Test
    fun `path arguments are encoded and non ascii stays escaped`() {
        assertEquals("model_settings/a%2Fb", Routes.modelSettings("a/b"))
        assertEquals("task_detail/a%2Fb", Routes.taskDetail("a/b"))
        assertEquals("%D9%85", Routes.encode("م"))
    }
}
