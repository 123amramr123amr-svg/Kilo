package com.personalaiagent.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspacePathsTest {

    @Test
    fun `normalises separators and dots`() {
        assertEquals("notes/today.txt", WorkspacePaths.normalize("  notes/./today.txt "))
        assertEquals("a/b", WorkspacePaths.normalize("a//b/"))
        assertEquals("a/b", WorkspacePaths.normalize("a\\b"))
    }

    @Test
    fun `rejects traversal outside the workspace`() {
        assertNull(WorkspacePaths.normalize("../secret.txt"))
        assertNull(WorkspacePaths.normalize("notes/../../etc/passwd"))
        assertNull(WorkspacePaths.normalize(".."))
    }

    @Test
    fun `rejects control characters`() {
        assertNull(WorkspacePaths.normalize("bad\u0000name.txt"))
    }

    @Test
    fun `rejects overly long paths and names`() {
        assertNull(WorkspacePaths.normalize("a".repeat(WorkspacePaths.MAX_NAME_LENGTH + 1)))
        assertNull(WorkspacePaths.normalize("x".repeat(WorkspacePaths.MAX_PATH_LENGTH + 1)))
    }

    @Test
    fun `parent and name helpers`() {
        assertEquals("notes", WorkspacePaths.parentOf("notes/today.txt"))
        assertEquals("", WorkspacePaths.parentOf("today.txt"))
        assertEquals("today.txt", WorkspacePaths.nameOf("notes/today.txt"))
    }

    @Test
    fun `join keeps paths inside the root`() {
        assertEquals("notes/a.txt", WorkspacePaths.join("notes", "a.txt"))
        assertEquals("a.txt", WorkspacePaths.join("", "a.txt"))
        assertNull(WorkspacePaths.join("notes", "../escape"))
    }

    @Test
    fun `safe names reject separators and dot entries`() {
        assertTrue(WorkspacePaths.isSafeName("report.txt"))
        assertTrue(WorkspacePaths.isSafeName("تقرير.txt"))
        assertFalse(WorkspacePaths.isSafeName("../x"))
        assertFalse(WorkspacePaths.isSafeName("a/b"))
        assertFalse(WorkspacePaths.isSafeName("."))
        assertFalse(WorkspacePaths.isSafeName(""))
    }
}
