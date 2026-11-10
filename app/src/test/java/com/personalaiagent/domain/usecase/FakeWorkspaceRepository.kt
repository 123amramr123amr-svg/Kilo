package com.personalaiagent.domain.usecase

import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.model.Workspace
import com.personalaiagent.domain.model.WorkspaceEntry
import com.personalaiagent.domain.model.WorkspaceFile
import com.personalaiagent.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** In-memory workspace used by JVM tests; mirrors the SAF repository semantics. */
class FakeWorkspaceRepository(
    private var workspace: Workspace? = Workspace("content://test/tree", "Test"),
) : WorkspaceRepository {

    val files = mutableMapOf<String, String>()

    override fun observeWorkspace(): Flow<Workspace?> = flowOf(workspace)

    override suspend fun connect(treeUri: String): Outcome<Workspace> {
        workspace = Workspace(treeUri, "Test")
        return Outcome.Success(workspace!!)
    }

    override suspend fun disconnect() {
        workspace = null
    }

    override suspend fun list(path: String): Outcome<List<WorkspaceEntry>> {
        if (workspace == null) return Outcome.Failure(com.personalaiagent.core.AppError.NoWorkspace)
        val prefix = if (path.isBlank()) "" else "$path/"
        val entries = files.keys
            .filter { it.startsWith(prefix) && it.removePrefix(prefix).isNotEmpty() }
            .map { full ->
                WorkspaceEntry(
                    name = full.substringAfterLast('/'),
                    path = full,
                    isDirectory = false,
                    size = files[full]?.length?.toLong() ?: 0L,
                    lastModified = 0L,
                )
            }
            .distinctBy { it.path }
        return Outcome.Success(entries)
    }

    override suspend fun read(path: String, maxBytes: Int): Outcome<WorkspaceFile> {
        val content = files[path] ?: return Outcome.Failure(com.personalaiagent.core.AppError.Unknown("not_found"))
        return Outcome.Success(WorkspaceFile(path, content, content.length.toLong(), truncated = false))
    }

    override suspend fun write(path: String, content: String, append: Boolean): Outcome<WorkspaceEntry> {
        if (workspace == null) return Outcome.Failure(com.personalaiagent.core.AppError.NoWorkspace)
        val next = if (append) (files[path] ?: "") + content else content
        files[path] = next
        return Outcome.Success(
            WorkspaceEntry(
                name = path.substringAfterLast('/'),
                path = path,
                isDirectory = false,
                size = next.length.toLong(),
                lastModified = 0L,
            ),
        )
    }

    override suspend fun writeBytes(path: String, bytes: ByteArray, mimeType: String): Outcome<WorkspaceEntry> {
        if (workspace == null) return Outcome.Failure(com.personalaiagent.core.AppError.NoWorkspace)
        files[path] = "<binary ${bytes.size} bytes>"
        return Outcome.Success(
            WorkspaceEntry(path.substringAfterLast('/'), path, isDirectory = false, size = bytes.size.toLong(), lastModified = 0),
        )
    }

    override suspend fun createFolder(path: String): Outcome<WorkspaceEntry> {
        if (workspace == null) return Outcome.Failure(com.personalaiagent.core.AppError.NoWorkspace)
        val marker = "$path/.keep"
        if (!files.containsKey(marker)) files[marker] = ""
        return Outcome.Success(WorkspaceEntry(path, path, isDirectory = true, size = 0, lastModified = 0))
    }

    override suspend fun delete(path: String): Outcome<Unit> {
        files.remove(path)
        return Outcome.Success(Unit)
    }
}
