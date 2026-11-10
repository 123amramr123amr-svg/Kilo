package com.personalaiagent.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Drive/DocsProvider trees have no filesystem path, so `shell` must know when it
 * cannot act on the workspace. These cases are the difference between a clear
 * message and a wrong path.
 */
class SafPathsTest {

    @Test
    fun `primary storage document becomes a real path`() {
        assertEquals(
            "/storage/emulated/0/Download/report.pdf",
            SafPaths.localPathFor(
                "com.android.externalstorage.documents",
                "primary:Download/report.pdf",
            ),
        )
    }

    @Test
    fun `documents root maps to the Documents folder`() {
        assertEquals(
            "/storage/emulated/0/Documents/notes.txt",
            SafPaths.localPathFor(
                "com.android.externalstorage.documents",
                "home:notes.txt",
            ),
        )
    }

    @Test
    fun `sdcard volume becomes a UUID storage path`() {
        assertEquals(
            "/storage/1A2B-3C4D/projects",
            SafPaths.localPathFor(
                "com.android.externalstorage.documents",
                "1A2B-3C4D:projects",
            ),
        )
    }

    @Test
    fun `downloads provider maps into Download`() {
        assertEquals(
            "/storage/emulated/0/Download",
            SafPaths.localPathFor("com.android.providers.downloads.documents", "raw:/storage/emulated/0/Download"),
        )
    }

    @Test
    fun `cloud providers have no local path`() {
        assertNull(SafPaths.localPathFor("com.google.android.apps.docs.storage", "file-123"))
        assertNull(SafPaths.localPathFor(null, "primary:doc"))
        assertNull(SafPaths.localPathFor("com.android.externalstorage.documents", null))
    }
}
