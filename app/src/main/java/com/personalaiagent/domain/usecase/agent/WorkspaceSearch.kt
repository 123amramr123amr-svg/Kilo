package com.personalaiagent.domain.usecase.agent

import com.personalaiagent.core.Outcome
import com.personalaiagent.domain.model.WorkspaceEntry
import com.personalaiagent.domain.repository.WorkspaceRepository
import javax.inject.Inject

/** One matching line of a `grep`. */
data class GrepHit(
    val path: String,
    val line: Int,
    val text: String,
)

data class GrepResult(
    val hits: List<GrepHit>,
    val filesScanned: Int,
    val filesMatched: Int,
    val truncated: Boolean,
)

/**
 * `glob` and `grep` over the user's workspace.
 *
 * The workspace is a SAF tree, so there is no `java.io.File` to walk: it is walked
 * through [WorkspaceRepository.list], and a hard cap on files and bytes keeps a
 * search from running for a minute on a huge folder.
 */
class WorkspaceSearch @Inject constructor(
    private val workspace: WorkspaceRepository,
) {

    /** Every file under [root], depth-first, capped at [maxFiles]. */
    suspend fun walk(root: String = "", maxFiles: Int = MAX_FILES, maxDepth: Int = MAX_DEPTH): List<WorkspaceEntry> {
        val found = mutableListOf<WorkspaceEntry>()
        suspend fun visit(path: String, depth: Int) {
            if (depth > maxDepth || found.size >= maxFiles) return
            val listing = (workspace.list(path) as? Outcome.Success)?.value ?: return
            for (entry in listing) {
                if (found.size >= maxFiles) return
                if (entry.isDirectory) {
                    visit(entry.path, depth + 1)
                } else {
                    found += entry
                }
            }
        }
        visit(root.trim('/'), 0)
        return found
    }

    /**
     * Shell-style glob over relative paths: `**` crosses folders, `*` stays inside
     * one, `?` is a single character. A pattern without `/` matches a file name
     * anywhere in the tree, which is what people expect from `*.kt`.
     */
    fun glob(pattern: String, files: List<WorkspaceEntry>): List<WorkspaceEntry> {
        val cleaned = pattern.trim().trimStart('/')
        if (cleaned.isEmpty()) return files
        val anchored = cleaned.contains('/') || cleaned.startsWith("**")
        val regex = Regex(globToRegex(if (anchored) cleaned else "**/$cleaned"))
        return files.filter { file -> regex.matches(file.path) }
    }

    /** Case-insensitive regex search, one hit per line. */
    suspend fun grep(
        pattern: String,
        files: List<WorkspaceEntry>,
        maxHits: Int = MAX_HITS,
        maxFiles: Int = MAX_FILES,
    ): GrepResult {
        val regex = runCatching {
            Regex(pattern, setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
        }.getOrNull()
            ?: return GrepResult(emptyList(), 0, 0, truncated = false)
        val hits = mutableListOf<GrepHit>()
        var scanned = 0
        var matched = 0
        for (file in files) {
            if (scanned >= maxFiles || hits.size >= maxHits) break
            if (!looksTextual(file)) continue
            scanned++
            val content = (workspace.read(file.path, MAX_GREP_FILE_BYTES) as? Outcome.Success)
                ?.value?.content ?: continue
            var fileMatched = false
            content.lineSequence().forEachIndexed { index, line ->
                if (hits.size < maxHits && regex.containsMatchIn(line)) {
                    hits += GrepHit(file.path, index + 1, line.trim().take(240))
                    fileMatched = true
                }
            }
            if (fileMatched) matched++
        }
        return GrepResult(
            hits = hits,
            filesScanned = scanned,
            filesMatched = matched,
            truncated = hits.size >= maxHits,
        )
    }

    fun looksTextual(entry: WorkspaceEntry): Boolean {
        val name = entry.name.lowercase()
        if (BINARY_EXTENSIONS.any { name.endsWith(it) }) return false
        return true
    }


    companion object {
        const val MAX_FILES = 1_500
        const val MAX_DEPTH = 8
        const val MAX_HITS = 120
        const val MAX_GREP_FILE_BYTES = 512_000

        private val BINARY_EXTENSIONS = listOf(
            ".png", ".jpg", ".jpeg", ".webp", ".gif", ".heic", ".pdf", ".zip", ".rar",
            ".mp3", ".mp4", ".m4a", ".ogg", ".wav", ".apk", ".so", ".ttf", ".otf", ".db",
        )
    }
}

/** Translates a glob into a regular expression anchored at both ends. */
internal fun globToRegex(glob: String): String {
    val out = StringBuilder("^")
    var index = 0
    while (index < glob.length) {
        val char = glob[index]
        when {
            char == '*' && index + 1 < glob.length && glob[index + 1] == '*' -> {
                // "**/" also matches zero folders: "**/*.kt" must find "a.kt".
                if (index + 2 < glob.length && glob[index + 2] == '/') {
                    out.append("(?:.*/)?")
                    index += 3
                } else {
                    out.append(".*")
                    index += 2
                }
            }
            char == '*' -> {
                out.append("[^/]*")
                index++
            }
            char == '?' -> {
                out.append("[^/]")
                index++
            }
            char in ".()[]{}+^$|\\" -> {
                out.append('\\').append(char)
                index++
            }
            else -> {
                out.append(char)
                index++
            }
        }
    }
    out.append('$')
    return out.toString()
}
