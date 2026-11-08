package com.personalaiagent.domain.usecase

import com.personalaiagent.domain.model.ToolCall
import com.personalaiagent.domain.usecase.agent.AgentToolCatalog
import com.personalaiagent.domain.usecase.agent.AgentToolExecutor
import com.personalaiagent.domain.usecase.agent.ToolContext
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentToolExecutorTest {

    private val workspace = FakeWorkspaceRepository()
    private val executor = AgentToolExecutor(workspace, Json { ignoreUnknownKeys = true }, FakeDeviceControl(), FakeImageGeneration())

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
        val empty = AgentToolExecutor(FakeWorkspaceRepository(workspace = null), Json, FakeDeviceControl(), FakeImageGeneration())
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
        val executor = AgentToolExecutor(
            workspace,
            Json { ignoreUnknownKeys = true },
            device,
            FakeImageGeneration(),
        )
        val result = executor.execute(call(AgentToolCatalog.OPEN_APP, """{"app":"WhatsApp"}"""))
        assertTrue(result.ok)
        assertEquals(listOf("open_app:com.whatsapp"), device.actions)
    }

    @Test
    fun `unknown app reports an error without crashing`() = runTest {
        val device = FakeDeviceControl()
        val executor = AgentToolExecutor(workspace, Json, device, FakeImageGeneration())
        val result = executor.execute(call(AgentToolCatalog.OPEN_APP, """{"app":"Nope"}"""))
        assertFalse(result.ok)
        assertTrue(device.actions.isEmpty())
    }

    @Test
    fun `web search and url open reach the device layer`() = runTest {
        val device = FakeDeviceControl()
        val executor = AgentToolExecutor(workspace, Json, device, FakeImageGeneration())
        executor.execute(call(AgentToolCatalog.WEB_SEARCH, """{"query":"طقس الغردقة"}"""))
        executor.execute(call(AgentToolCatalog.OPEN_URL, """{"url":"example.com"}"""))
        assertEquals(listOf("web_search:طقس الغردقة", "open_url:example.com"), device.actions)
    }

    @Test
    fun `generate_image saves the picture into the workspace`() = runTest {
        val generator = FakeImageGeneration()
        val executor = AgentToolExecutor(workspace, Json, FakeDeviceControl(), generator)
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
        val executor = AgentToolExecutor(workspace, Json, FakeDeviceControl(), FakeImageGeneration())
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
        assertEquals(11, AgentToolCatalog.tools.size)
        AgentToolCatalog.tools.forEach { tool ->
            val schema = Json.parseToJsonElement(tool.parametersJson)
            assertTrue(schema.toString().contains("object"))
        }
        assertTrue(AgentToolCatalog.systemPrompt("My files").contains("My files"))
    }
}
