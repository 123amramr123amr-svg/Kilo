package com.personalaiagent.domain.usecase.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsTest {

    @Test
    fun `valid json has no issues`() {
        val report = Diagnostics.check("data.json", "{\"a\":1,\"b\":[2,3]}\n")

        assertTrue(report.issues.isEmpty())
        assertTrue(report.render().contains("No structural problems"))
    }

    @Test
    fun `broken json is reported with the note that this is not a compiler`() {
        val report = Diagnostics.check("data.json", """{"a":1,""")

        assertFalse(report.issues.isEmpty())
        assertTrue(report.render().contains("not a compiler"))
    }

    @Test
    fun `duplicate string resources are found`() {
        val xml = """
            <resources>
                <string name="app_name">A</string>
                <string name="app_name">B</string>
            </resources>
        """.trimIndent()
        val report = Diagnostics.check("strings.xml", xml)

        assertTrue(report.issues.any { it.message.contains("app_name") })
    }

    @Test
    fun `an unclosed brace is found with its line`() {
        val report = Diagnostics.check("Main.kt", "fun main() {\n    println(1)\n")

        assertTrue(report.issues.any { it.message.contains("never closed") })
        assertTrue(report.issues.first().line >= 1)
    }

    @Test
    fun `braces inside strings and comments are ignored`() {
        val kotlin = """
            fun main() {
                val text = "unclosed ( bracket in a string {"
                // a comment with a stray )
                println(text)
            }
        """.trimIndent()
        val report = Diagnostics.check("Main.kt", kotlin)

        assertTrue(report.render(), report.issues.isEmpty())
    }

    @Test
    fun `empty file is reported instead of pretending everything is fine`() {
        val report = Diagnostics.check("empty.txt", "")

        assertTrue(report.issues.any { it.message.contains("empty", ignoreCase = true) })
    }
}
