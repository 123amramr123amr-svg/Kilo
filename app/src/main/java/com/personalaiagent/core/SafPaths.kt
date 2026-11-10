package com.personalaiagent.core

import android.net.Uri
import java.io.File

/**
 * Turns a Storage Access Framework tree URI into a real filesystem path.
 *
 * The file tools do not need this (they go through the ContentResolver), but a real
 * shell does: `sh` only understands paths. For the folder the user actually picks
 * (Internal storage / Downloads, which is what the system picker offers by default)
 * the document id is `primary:Documents/Kilo`, so the path is
 * `/storage/emulated/0/Documents/Kilo`. For a cloud provider (Drive, OneDrive) there
 * is no path at all and this returns null — the shell tool then says so instead of
 * pretending.
 */
object SafPaths {

    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val DOWNLOADS_AUTHORITY = "com.android.providers.downloads.documents"

    /** `/storage/emulated/0/…` for a local tree, null for a remote provider. */
    fun localPath(treeUri: String?): String? {
        if (treeUri.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(treeUri) }.getOrNull() ?: return null
        val id = runCatching { uri.lastPathSegment }.getOrNull()
            ?.substringAfterLast('/')
            ?.let { Uri.decode(it) }
        return localPathFor(uri.authority, id)
    }

    /** Pure form, so the mapping can be tested without an Android runtime. */
    fun localPathFor(authority: String?, documentId: String?): String? {
        if (documentId.isNullOrBlank()) return null
        return when (authority) {
            EXTERNAL_STORAGE_AUTHORITY -> fromExternalStorage(documentId)
            DOWNLOADS_AUTHORITY -> fromDownloads(documentId)
            else -> null
        }
    }

    private fun fromExternalStorage(documentId: String): String? {
        val separator = documentId.indexOf(':')
        if (separator <= 0) return null
        val volume = documentId.substring(0, separator)
        val path = documentId.substring(separator + 1).trim('/')
        val root = when {
            volume.equals("primary", ignoreCase = true) -> "/storage/emulated/0"
            // "home:" is the Documents folder on the primary volume.
            volume.equals("home", ignoreCase = true) -> "/storage/emulated/0/Documents"
            // A removable card is addressed by its UUID.
            VOLUME_PATTERN.matches(volume) -> "/storage/$volume"
            else -> return null
        }
        return if (path.isEmpty()) root else "$root/$path"
    }

    private fun fromDownloads(documentId: String): String? {
        // Downloads are either "raw:/storage/emulated/0/Download/x" or a numeric id
        // that only the provider can resolve; the numeric form has no path.
        if (!documentId.startsWith("raw:")) return null
        return documentId.removePrefix("raw:")
    }

    /** True when the derived path really exists and is a readable directory. */
    fun existingDirectory(treeUri: String?): File? {
        val path = localPath(treeUri) ?: return null
        val file = File(path)
        return if (file.isDirectory && file.canRead()) file else null
    }

    private val VOLUME_PATTERN = Regex("^[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}$")
}
