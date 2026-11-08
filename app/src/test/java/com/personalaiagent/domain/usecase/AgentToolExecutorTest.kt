package com.personalaiagent.domain.usecase

import com.personalaiagent.domain.model.ToolCall
import com.personalaiagent.domain.usecase.agent.AgentToolCatalog
import com.personalaiagent.domain.usecase.agent.AgentToolExecutor
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentToolExecutorTest {

    private val workspace = FakeWorkspaceRepository()
    private val executor = AgentToolExecutor(workspace, Json { ignoreUnknownKeys = true })

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
        val result = executor.execute(call(AgentToolCatalog.LIST_FILES, """{"path":""""}"""))
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
        val empty = AgentToolExecutor(FakeWorkspaceRepository(workspace = null), Json)
        val result = empty.execute(call(AgentToolCatalog.LIST_FILES, """{"path":""""}"""))
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
    fun `catalog exposes json schemas and a system prompt`() {
        assertEquals(6, AgentToolCatalog.tools.size)
        AgentToolCatalog.tools.forEach { tool ->
            val schema = Json.parseToJsonElement(tool.parametersJson)
            assertTrue(schema.toString().contains("object"))
        }
        assertTrue(AgentToolCatalog.systemPrompt("My files").contains("My files"))
    }
}
