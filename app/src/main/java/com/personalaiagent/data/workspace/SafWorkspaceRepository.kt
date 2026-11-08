package com.personalaiagent.data.workspace

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import com.personalaiagent.core.AppError
import com.personalaiagent.core.WorkspacePaths
import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.model.Workspace
import com.personalaiagent.domain.model.WorkspaceEntry
import com.personalaiagent.domain.model.WorkspaceFile
import com.personalaiagent.domain.repository.SettingsRepository
import com.personalaiagent.domain.repository.WorkspaceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Workspace backed by a Storage Access Framework tree.
 *
 * The user grants one folder; the app keeps a persistable read/write grant for it.
 * No `MANAGE_EXTERNAL_STORAGE` and no broad storage permission are involved, which
 * keeps the app installable from Play while still letting the agent create, read and
 * edit real files in the folder the user chose.
 */
class SafWorkspaceRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val settingsRepository: SettingsRepository,
) : WorkspaceRepository {

    override fun observeWorkspace(): Flow<Workspace?> =
        settingsRepository.settings.map { settings ->
            val uri = settings.workspaceUri
            if (uri.isNullOrBlank()) null else Workspace(uri, settings.workspaceName ?: uri)
        }

    override suspend fun connect(treeUri: String): Outcome<Workspace> {
        val uri = runCatching { Uri.parse(treeUri) }.getOrNull()
            ?: return Outcome.Failure(AppError.InvalidInput("workspace_uri"))
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
            return Outcome.Failure(AppError.InvalidInput("workspace_uri"))
        }
        return try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            val name = queryDisplayName(rootDocumentUri(uri)) ?: uri.lastPathSegment ?: "Workspace"
            settingsRepository.setWorkspace(uri.toString(), name)
            Outcome.Success(Workspace(uri.toString(), name))
        } catch (e: SecurityException) {
            Outcome.Failure(AppError.Unknown("workspace_permission_denied"))
        } catch (e: Exception) {
            Outcome.Failure(AppError.Unknown(e.message))
        }
    }

    override suspend fun disconnect() {
        val current = settingsRepository.current().workspaceUri
        if (current != null) {
            runCatching {
                contentResolver.releasePersistableUriPermission(
                    Uri.parse(current),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        settingsRepository.setWorkspace(null, null)
    }

    override suspend fun list(path: String): Outcome<List<WorkspaceEntry>> {
        val normalized = WorkspacePaths.normalize(path)
            ?: return Outcome.Failure(AppError.InvalidInput("path"))
        return runCatching {
            val tree = currentTree() ?: return Outcome.Failure(AppError.NoWorkspace)
            val directory = resolveDocument(tree, normalized)
                ?: return Outcome.Failure(AppError.Unknown("not_found"))
            val entries = mutableListOf<WorkspaceEntry>()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                tree,
                DocumentsContract.getDocumentId(directory),
            )
            contentResolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                if (nameIndex < 0) return@use
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex) ?: continue
                    val isDirectory = mimeIndex >= 0 &&
                        cursor.getString(mimeIndex) == DocumentsContract.Document.MIME_TYPE_DIR
                    entries += WorkspaceEntry(
                        name = name,
                        path = if (normalized.isEmpty()) name else "$normalized/$name",
                        isDirectory = isDirectory,
                        size = cursor.longOrZero(DocumentsContract.Document.COLUMN_SIZE),
                        lastModified = cursor.longOrZero(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                    )
                }
            }
            Outcome.Success(entries.sortedWith(compareByDescending<WorkspaceEntry> { it.isDirectory }.thenBy { it.name.lowercase() }))
        }.getOrElse { Outcome.Failure(AppError.Unknown(it.message)) }
    }

    override suspend fun read(path: String, maxBytes: Int): Outcome<WorkspaceFile> {
        val normalized = WorkspacePaths.normalize(path)
            ?: return Outcome.Failure(AppError.InvalidInput("path"))
        if (normalized.isEmpty()) return Outcome.Failure(AppError.InvalidInput("path"))
        return runCatching {
            val tree = currentTree() ?: return Outcome.Failure(AppError.NoWorkspace)
            val document = resolveDocument(tree, normalized)
                ?: return Outcome.Failure(AppError.Unknown("not_found"))
            val size = querySize(document)
            val bytes = contentResolver.openInputStream(document)?.use { input ->
                val buffer = ByteArray(minOf(maxBytes.toLong(), if (size > 0) size else maxBytes.toLong()).toInt().coerceAtLeast(1))
                var read = 0
                while (read < buffer.size) {
                    val chunk = input.read(buffer, read, buffer.size - read)
                    if (chunk <= 0) break
                    read += chunk
                }
                buffer.copyOf(read)
            } ?: return Outcome.Failure(AppError.Unknown("unreadable"))
            Outcome.Success(
                WorkspaceFile(
                    path = normalized,
                    content = String(bytes, Charsets.UTF_8),
                    size = if (size >= 0) size else bytes.size.toLong(),
                    truncated = size > bytes.size,
                ),
            )
        }.getOrElse { Outcome.Failure(AppError.Unknown(it.message)) }
    }

    override suspend fun write(path: String, content: String, append: Boolean): Outcome<WorkspaceEntry> {
        val normalized = WorkspacePaths.normalize(path)
            ?: return Outcome.Failure(AppError.InvalidInput("path"))
        if (normalized.isEmpty()) return Outcome.Failure(AppError.InvalidInput("path"))
        return runCatching {
            val tree = currentTree() ?: return Outcome.Failure(AppError.NoWorkspace)
            val name = WorkspacePaths.nameOf(normalized)
            if (!WorkspacePaths.isSafeName(name)) return Outcome.Failure(AppError.InvalidInput("name"))
            val parentPath = WorkspacePaths.parentOf(normalized)
            val parent = ensureDirectory(tree, parentPath) ?: return Outcome.Failure(AppError.Unknown("parent"))
            val existing = findChild(tree, DocumentsContract.getDocumentId(parent), name)
            val bytes = content.toByteArray(Charsets.UTF_8)

            val documentUri = if (existing == null) {
                DocumentsContract.createDocument(contentResolver, parent, mimeTypeFor(name), name)
                    ?: return Outcome.Failure(AppError.Unknown("create_failed"))
            } else {
                existing
            }
            // "wa" appends, "wt" truncates. A few providers reject the explicit mode,
            // so fall back to the default stream before giving up.
            val mode = if (append) "wa" else "wt"
            val stream = runCatching { contentResolver.openOutputStream(documentUri, mode) }.getOrNull()
                ?: runCatching { contentResolver.openOutputStream(documentUri, if (append) "wa" else "w") }.getOrNull()
                ?: return Outcome.Failure(AppError.Unknown("write_failed"))
            stream.use {
                it.write(bytes)
                it.flush()
            }

            val size = querySize(documentUri)
            Outcome.Success(
                WorkspaceEntry(
                    name = name,
                    path = normalized,
                    isDirectory = false,
                    size = if (size >= 0) size else bytes.size.toLong(),
                    lastModified = System.currentTimeMillis(),
                ),
            )
        }.getOrElse { Outcome.Failure(AppError.Unknown(it.message)) }
    }

    override suspend fun writeBytes(path: String, bytes: ByteArray, mimeType: String): Outcome<WorkspaceEntry> {
        val normalized = WorkspacePaths.normalize(path)
            ?: return Outcome.Failure(AppError.InvalidInput("path"))
        if (normalized.isEmpty()) return Outcome.Failure(AppError.InvalidInput("path"))
        return runCatching {
            val tree = currentTree() ?: return Outcome.Failure(AppError.NoWorkspace)
            val name = WorkspacePaths.nameOf(normalized)
            if (!WorkspacePaths.isSafeName(name)) return Outcome.Failure(AppError.InvalidInput("name"))
            val parent = ensureDirectory(tree, WorkspacePaths.parentOf(normalized))
                ?: return Outcome.Failure(AppError.Unknown("parent"))
            val existing = findChild(tree, DocumentsContract.getDocumentId(parent), name)
            val documentUri = existing ?: DocumentsContract.createDocument(
                contentResolver,
                parent,
                mimeType,
                name,
            ) ?: return Outcome.Failure(AppError.Unknown("create_failed"))

            val stream = runCatching { contentResolver.openOutputStream(documentUri, "wt") }.getOrNull()
                ?: runCatching { contentResolver.openOutputStream(documentUri, "w") }.getOrNull()
                ?: return Outcome.Failure(AppError.Unknown("write_failed"))
            stream.use {
                it.write(bytes)
                it.flush()
            }
            Outcome.Success(
                WorkspaceEntry(
                    name = name,
                    path = normalized,
                    isDirectory = false,
                    size = bytes.size.toLong(),
                    lastModified = System.currentTimeMillis(),
                ),
            )
        }.getOrElse { Outcome.Failure(AppError.Unknown(it.message)) }
    }

    override suspend fun createFolder(path: String): Outcome<WorkspaceEntry> {
        val normalized = WorkspacePaths.normalize(path)
            ?: return Outcome.Failure(AppError.InvalidInput("path"))
        if (normalized.isEmpty()) return Outcome.Failure(AppError.InvalidInput("path"))
        return runCatching {
            val tree = currentTree() ?: return Outcome.Failure(AppError.NoWorkspace)
            val name = WorkspacePaths.nameOf(normalized)
            if (!WorkspacePaths.isSafeName(name)) return Outcome.Failure(AppError.InvalidInput("name"))
            val parent = ensureDirectory(tree, WorkspacePaths.parentOf(normalized))
                ?: return Outcome.Failure(AppError.Unknown("parent"))
            val created = DocumentsContract.createDocument(
                contentResolver,
                parent,
                DocumentsContract.Document.MIME_TYPE_DIR,
                name,
            ) ?: return Outcome.Failure(AppError.Unknown("create_failed"))
            Outcome.Success(
                WorkspaceEntry(
                    name = name,
                    path = normalized,
                    isDirectory = true,
                    size = 0,
                    lastModified = System.currentTimeMillis(),
                ),
            )
        }.getOrElse { Outcome.Failure(AppError.Unknown(it.message)) }
    }

    override suspend fun delete(path: String): Outcome<Unit> {
        val normalized = WorkspacePaths.normalize(path)
            ?: return Outcome.Failure(AppError.InvalidInput("path"))
        if (normalized.isEmpty()) return Outcome.Failure(AppError.InvalidInput("path"))
        return runCatching {
            val tree = currentTree() ?: return Outcome.Failure(AppError.NoWorkspace)
            val document = resolveDocument(tree, normalized)
                ?: return Outcome.Failure(AppError.Unknown("not_found"))
            if (DocumentsContract.deleteDocument(contentResolver, document)) {
                Outcome.Success(Unit)
            } else {
                Outcome.Failure(AppError.Unknown("delete_failed"))
            }
        }.getOrElse { Outcome.Failure(AppError.Unknown(it.message)) }
    }

    // ---------------------------------------------------------------- helpers

    private suspend fun currentTree(): Uri? =
        settingsRepository.current().workspaceUri?.let { runCatching { Uri.parse(it) }.getOrNull() }

    private fun rootDocumentUri(tree: Uri): Uri =
        DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

    /** Walks `a/b/c` from the tree root, creating nothing. */
    private fun resolveDocument(tree: Uri, path: String): Uri? {
        var current = rootDocumentUri(tree)
        for (segment in WorkspacePaths.segments(path)) {
            current = findChild(tree, DocumentsContract.getDocumentId(current), segment) ?: return null
        }
        return current
    }

    /** Like [resolveDocument] but creates missing directories. */
    private fun ensureDirectory(tree: Uri, path: String): Uri? {
        var current = rootDocumentUri(tree)
        for (segment in WorkspacePaths.segments(path)) {
            val id = DocumentsContract.getDocumentId(current)
            val existing = findChild(tree, id, segment)
            current = if (existing != null) {
                existing
            } else {
                DocumentsContract.createDocument(
                    contentResolver,
                    current,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    segment,
                ) ?: return null
            }
        }
        return current
    }

    private fun findChild(tree: Uri, parentDocumentId: String, name: String): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentDocumentId)
        contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val childName = cursor.getString(1) ?: continue
                if (childName == name) {
                    val childId = cursor.getString(0)
                    return DocumentsContract.buildDocumentUriUsingTree(tree, childId)
                }
            }
        }
        return null
    }

    private fun queryDisplayName(documentUri: Uri): String? = try {
        contentResolver.query(
            documentUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    } catch (e: Exception) {
        null
    }

    private fun querySize(documentUri: Uri): Long = try {
        contentResolver.query(
            documentUri,
            arrayOf(DocumentsContract.Document.COLUMN_SIZE),
            null,
            null,
            null,
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.longOrZero(DocumentsContract.Document.COLUMN_SIZE) else -1L }
            ?: -1L
    } catch (e: Exception) {
        -1L
    }

    private fun Cursor.longOrZero(columnName: String): Long {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getLong(index) else 0L
    }

    private fun mimeTypeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "txt", "md", "log" -> "text/plain"
        "json" -> "application/json"
        "csv" -> "text/csv"
        "html", "htm" -> "text/html"
        "xml" -> "text/xml"
        "kt", "java", "py", "js", "ts", "sh", "yaml", "yml" -> "text/plain"
        else -> "application/octet-stream"
    }

    private companion object {
        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    }
}
