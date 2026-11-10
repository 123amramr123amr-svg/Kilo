package com.personalaiagent.domain.usecase.agent

import com.personalaiagent.domain.model.Workspace
import com.personalaiagent.domain.usecase.FakeWorkspaceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSearchTest {

    private val workspace = FakeWorkspaceRepository(Workspace("content://t/tree", "Test"))
    private val search = WorkspaceSearch(workspace)

    @Test
    fun `glob star does not cross folders`() {
        val regex = Regex(globToRegex("*.kt"))

        assertTrue(regex.matches("Main.kt"))
        assertFalse(regex.matches("ui/Main.kt"))
    }

    @Test
    fun `double star matches zero or more folders`() {
        val regex = Regex(globToRegex("**/*.kt"))

        assertTrue(regex.matches("Main.kt"))
        assertTrue(regex.matches("ui/screen/Main.kt"))
    }

    @Test
    fun `single star inside a pattern matches any name part`() {
        val regex = Regex(globToRegex("src/*.md"))

        assertTrue(regex.matches("src/notes.md"))
        assertFalse(regex.matches("src/deep/notes.md"))
    }

    @Test
    fun `question mark matches exactly one character`() {
        val regex = Regex(globToRegex("a?.txt"))

        assertTrue(regex.matches("ab.txt"))
        assertFalse(regex.matches("abc.txt"))
    }

    @Test
    fun `grep returns path and line number and skips binary files`() = runTest {
        workspace.files["src/A.kt"] = "val a = 1\nval b = TODO"
        workspace.files["bin/blob.png"] = "TODO in a fake binary"
        val result = grep("TODO")

        assertTrue(result.hits.any { it.path == "src/A.kt" && it.line == 2 })
        assertFalse(result.hits.any { it.path.endsWith(".png") })
    }

    @Test
    fun `grep is case-insensitive and regex based`() = runTest {
        workspace.files["a.md"] = "Hello World"
        assertEquals(1, grep("hello\\s+world").hits.size)
        assertEquals(0, grep("^world").hits.size)
    }

    private suspend fun grep(pattern: String): GrepResult =
        search.grep(pattern, search.walk())

    @Test
    fun `walk lists files under a folder only`() = runTest {
        workspace.files["src/A.kt"] = "a"
        workspace.files["docs/B.md"] = "b"
        val files = search.walk("src")

        assertEquals(listOf("src/A.kt"), files.map { it.path })
    }
}
