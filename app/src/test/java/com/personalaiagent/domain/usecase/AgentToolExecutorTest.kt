package com.personalaiagent.domain.usecase

import com.personalaiagent.domain.model.ToolCall
import com.personalaiagent.domain.usecase.agent.AgentToolCatalog
import com.personalaiagent.domain.usecase.agent.AgentToolExecutor
import com.personalaiagent.domain.usecase.agent.ToolContext
import com.personalaiagent.domain.usecase.agent.QuestionBroker
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentToolExecutorTest {

    private val workspace = FakeWorkspaceRepository()
    private val executor = testExecutor(workspace = workspace)

    private fun call(name: String, args: String) = ToolCall(id = "call-1", name = name, arguments = args)

    @Test
    fun `write then read returns the file content`() = runTest {
        val write = executor.execute(call(AgentToolCatalog.WRITE_FILE, """{"path":"notes/a.txt","content":"hello"}"""))
        assertTrue(write.ok)
        assertEquals("hello", workspace.files["notes/a.txt"])

        val read = executor.execute(call(AgentToolCatalog.READ_FILE, """{"path":"notes/a.txt"}"""))
        assertTrue(read.ok)
        assertTrue(read.output.contains("hello"))
    }

    @Test
    fun `append adds to the existing content`() = runTest {
        workspace.files["log.txt"] = "line1\n"
        val result = executor.execute(call(AgentToolCatalog.APPEND_FILE, """{"path":"log.txt","content":"line2"}"""))
        assertTrue(result.ok)
        assertEquals("line1\nline2", workspace.files["log.txt"])
    }

    @Test
    fun `list reports files`() = runTest {
        workspace.files["a.txt"] = "1"
        workspace.files["b.txt"] = "22"
        val result = executor.execute(call(AgentToolCatalog.LIST_FILES, "{\"path\":\"\"}"))
        assertTrue(result.ok)
        assertTrue(result.output.contains("a.txt"))
        assertTrue(result.output.contains("b.txt"))
    }

    @Test
    fun `missing file returns an error result instead of throwing`() = runTest {
        val result = executor.execute(call(AgentToolCatalog.READ_FILE, """{"path":"ghost.txt"}"""))
        assertFalse(result.ok)
        assertTrue(result.output.startsWith("ERROR"))
    }

    @Test
    fun `traversal attempt is rejected`() = runTest {
        val result = executor.execute(call(AgentToolCatalog.WRITE_FILE, """{"path":"../escape.txt","content":"x"}"""))
        assertFalse(result.ok)
        assertTrue(workspace.files.keys.none { it.contains("escape") })
    }

    @Test
    fun `unknown tool is rejected`() = runTest {
        val result = executor.execute(call("format_disk", "{}"))
        assertFalse(result.ok)
    }

    @Test
    fun `malformed json does not crash the agent`() = runTest {
        val result = executor.execute(call(AgentToolCatalog.READ_FILE, "{not json"))
        assertFalse(result.ok)
    }

    @Test
    fun `no workspace yields a helpful message`() = runTest {
        val empty = testExecutor(workspace = FakeWorkspaceRepository(workspace = null), json = Json)
        val result = empty.execute(call(AgentToolCatalog.LIST_FILES, "{\"path\":\"\"}"))
        assertFalse(result.ok)
        assertTrue(result.output.contains("workspace", ignoreCase = true))
    }

    @Test
    fun `delete removes the file`() = runTest {
        workspace.files["old.txt"] = "x"
        val result = executor.execute(call(AgentToolCatalog.DELETE_PATH, """{"path":"old.txt"}"""))
        assertTrue(result.ok)
        assertFalse(workspace.files.containsKey("old.txt"))
    }

    @Test
    fun `open_app launches the requested app`() = runTest {
        val device = FakeDeviceControl()
        val executor = testExecutor(
            workspace = workspace,
            device = device,
        )
        val result = executor.execute(call(AgentToolCatalog.OPEN_APP, """{"app":"WhatsApp"}"""))
        assertTrue(result.ok)
        assertEquals(listOf("open_app:com.whatsapp"), device.actions)
    }

    @Test
    fun `unknown app reports an error without crashing`() = runTest {
        val device = FakeDeviceControl()
        val executor = testExecutor(workspace = workspace, json = Json, device = device)
        val result = executor.execute(call(AgentToolCatalog.OPEN_APP, """{"app":"Nope"}"""))
        assertFalse(result.ok)
        assertTrue(device.actions.isEmpty())
    }

    @Test
    fun `web search and url open reach the device layer`() = runTest {
        val device = FakeDeviceControl()
        val executor = testExecutor(workspace = workspace, json = Json, device = device)
        executor.execute(call(AgentToolCatalog.WEB_SEARCH, """{"query":"طقس الغردقة"}"""))
        executor.execute(call(AgentToolCatalog.OPEN_URL, """{"url":"example.com"}"""))
        assertEquals(listOf("web_search:طقس الغردقة", "open_url:example.com"), device.actions)
    }

    @Test
    fun `generate_image saves the picture into the workspace`() = runTest {
        val generator = FakeImageGeneration()
        val executor = testExecutor(workspace = workspace, json = Json, image = generator)
        val result = executor.execute(
            call(AgentToolCatalog.GENERATE_IMAGE, """{"prompt":"قطة زرقاء","file_name":"cat.png"}"""),
            ToolContext(providerId = "p1", modelId = "gpt-image-1"),
        )
        assertTrue(result.ok)
        assertEquals("قطة زرقاء", generator.lastPrompt)
        assertTrue(workspace.files.keys.any { it == "generated/cat.png" })
    }

    @Test
    fun `generate_image without a provider fails clearly`() = runTest {
        val executor = testExecutor(workspace = workspace, json = Json)
        val result = executor.execute(call(AgentToolCatalog.GENERATE_IMAGE, """{"prompt":"x"}"""))
        assertFalse(result.ok)
    }

    @Test
    fun `read_file honours offset and limit`() = runTest {
        workspace.files["long.txt"] = "0123456789"
        val result = executor.execute(
            call(AgentToolCatalog.READ_FILE, """{"path":"long.txt","offset":2,"limit":4}"""),
        )
        assertTrue(result.ok)
        assertTrue(result.output.contains("2345"))
    }

    @Test
    fun `catalog exposes json schemas and a system prompt`() {
        assertEquals(20, AgentToolCatalog.tools.size)
        AgentToolCatalog.tools.forEach { tool ->
            val schema = Json.parseToJsonElement(tool.parametersJson)
            assertTrue(schema.toString().contains("object"))
        }
        assertTrue(AgentToolCatalog.systemPrompt("My files").contains("My files"))
    }

    // ------------------------------------------------------- phase-9 tool surface

    @Test
    fun `edit replaces a unique match and keeps the rest`() = runTest {
        workspace.files["a.txt"] = "one\ntwo\nthree"
        val result = executor.execute(
            call(AgentToolCatalog.EDIT, """{"path":"a.txt","old_string":"two","new_string":"TWO"}"""),
        )
        assertTrue(result.ok)
        assertEquals("one\nTWO\nthree", workspace.files["a.txt"])
    }

    @Test
    fun `edit refuses an ambiguous match instead of guessing`() = runTest {
        workspace.files["a.txt"] = "x\nx"
        val result = executor.execute(
            call(AgentToolCatalog.EDIT, """{"path":"a.txt","old_string":"x","new_string":"y"}"""),
        )
        assertFalse(result.ok)
        assertEquals("x\nx", workspace.files["a.txt"])
    }

    @Test
    fun `edit with replace_all changes every occurrence`() = runTest {
        workspace.files["a.txt"] = "x x x"
        val result = executor.execute(
            call(
                AgentToolCatalog.EDIT,
                """{"path":"a.txt","old_string":"x","new_string":"y","replace_all":true}""",
            ),
        )
        assertTrue(result.ok)
        assertEquals("y y y", workspace.files["a.txt"])
    }

    @Test
    fun `patch applies all edits or none`() = runTest {
        workspace.files["a.txt"] = "alpha\nbeta"
        val ok = executor.execute(
            call(
                AgentToolCatalog.PATCH,
                """{"path":"a.txt","edits":[{"old_string":"alpha","new_string":"A"},{"old_string":"beta","new_string":"B"}]}""",
            ),
        )
        assertTrue(ok.ok)
        assertEquals("A\nB", workspace.files["a.txt"])

        val failed = executor.execute(
            call(
                AgentToolCatalog.PATCH,
                """{"path":"a.txt","edits":[{"old_string":"A","new_string":"Z"},{"old_string":"missing","new_string":"Q"}]}""",
            ),
        )
        assertFalse(failed.ok)
        assertEquals("A\nB", workspace.files["a.txt"])
    }

    @Test
    fun `grep finds the line and reports path and number`() = runTest {
        workspace.files["src/Main.kt"] = "fun main() {}\nval x = 42"
        workspace.files["src/Other.kt"] = "val y = 1"
        val result = executor.execute(call(AgentToolCatalog.GREP, """{"pattern":"42"}"""))
        assertTrue(result.ok)
        assertTrue(result.output.contains("src/Main.kt:2"))
        assertFalse(result.output.contains("Other.kt"))
    }

    @Test
    fun `glob matches root files with a leading double star`() = runTest {
        workspace.files["build.gradle.kts"] = "x"
        workspace.files["app/src/Main.kt"] = "y"
        val result = executor.execute(call(AgentToolCatalog.GLOB, """{"pattern":"**/*.kt"}"""))
        assertTrue(result.ok)
        assertTrue(result.output.contains("app/src/Main.kt"))
        assertFalse(result.output.contains("build.gradle.kts"))
    }

    @Test
    fun `shell runs through the injected executor and reports the workdir`() = runTest {
        val shell = FakeShellExecutor()
        val executor = testExecutor(workspace = workspace, shell = shell)
        val result = executor.execute(call(AgentToolCatalog.SHELL, """{"command":"ls -la","timeout_seconds":5}"""))
        assertTrue(result.ok)
        assertEquals(listOf("ls -la"), shell.commands)
        assertEquals(5_000L, shell.lastTimeout)
    }

    @Test
    fun `shell refuses a destructive command before touching the device`() = runTest {
        val shell = FakeShellExecutor()
        val executor = testExecutor(workspace = workspace, shell = shell)
        val result = executor.execute(call(AgentToolCatalog.SHELL, """{"command":"rm -rf /"}"""))
        assertFalse(result.ok)
        assertTrue(shell.commands.isEmpty())
    }

    @Test
    fun `skill without a name lists what is installed`() = runTest {
        val result = executor.execute(call(AgentToolCatalog.SKILL, "{}"))
        assertTrue(result.ok)
        assertTrue(result.output.contains("code-review"))
    }

    @Test
    fun `skill with a name returns its body`() = runTest {
        val result = executor.execute(call(AgentToolCatalog.SKILL, """{"name":"code-review"}"""))
        assertTrue(result.ok)
        assertTrue(result.output.contains("list real problems"))
    }

    @Test
    fun `skill with an unknown name says so and lists the real ones`() = runTest {
        val result = executor.execute(call(AgentToolCatalog.SKILL, """{"name":"nope"}"""))
        assertFalse(result.ok)
        assertTrue(result.output.contains("code-review"))
    }

    @Test
    fun `diagnostics flags an unclosed bracket`() = runTest {
        workspace.files["Main.kt"] = "fun main() {\n    println(1)\n"
        val result = executor.execute(call(AgentToolCatalog.DIAGNOSTICS, """{"path":"Main.kt"}"""))
        assertTrue(result.output.contains("never closed") || result.output.contains("unclosed"))
    }

    @Test
    fun `question suspends until the user answers`() = runTest {
        val broker = QuestionBroker()
        val executor = testExecutor(workspace = workspace, questions = broker)
        val pending = async { executor.execute(call(AgentToolCatalog.QUESTION, """{"question":"Which file?"}""")) }
        // The broker publishes the question, the "user" answers it.
        val asked = broker.pending.first { it != null }!!
        assertEquals("Which file?", asked.question)
        broker.answer("a.txt")
        val result = pending.await()
        assertTrue(result.ok)
        assertTrue(result.output.contains("a.txt"))
    }
}
