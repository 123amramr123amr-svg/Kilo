package com.personalaiagent.core

/**
 * Path rules for the agent workspace.
 *
 * Every path is relative to the folder the user granted. Traversal outside that
 * folder (`..`, absolute paths) is rejected before any storage call happens, so a
 * malicious or confused model cannot escape the workspace.
 */
object WorkspacePaths {

    const val MAX_PATH_LENGTH = 512
    const val MAX_NAME_LENGTH = 128

    /** Returns a normalised relative path, or null when the input is not safe. */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim().replace('\\', '/')
        if (trimmed.length > MAX_PATH_LENGTH) return null
        if (trimmed.any { it.code < 0x20 }) return null

        val segments = mutableListOf<String>()
        for (segment in trimmed.split('/')) {
            when {
                segment.isEmpty() || segment == "." -> Unit
                segment == ".." -> return null // no escaping the workspace
                segment.contains("..") && segment.startsWith("..") -> return null
                segment.length > MAX_NAME_LENGTH -> return null
                else -> segments += segment
            }
        }
        return segments.joinToString("/")
    }

    fun segments(normalized: String): List<String> = normalized.split('/').filter { it.isNotEmpty() }

    fun join(parent: String, name: String): String? {
        val cleanParent = normalize(parent) ?: return null
        val cleanName = normalize(name) ?: return null
        return if (cleanParent.isEmpty()) cleanName else "$cleanParent/$cleanName"
    }

    fun parentOf(normalized: String): String {
        val index = normalized.lastIndexOf('/')
        return if (index <= 0) "" else normalized.substring(0, index)
    }

    fun nameOf(normalized: String): String =
        normalized.substringAfterLast('/').ifEmpty { normalized }

    fun isSafeName(name: String): Boolean {
        val cleaned = name.trim()
        return cleaned.isNotEmpty() &&
            cleaned.length <= MAX_NAME_LENGTH &&
            !cleaned.contains('/') &&
            !cleaned.contains('\\') &&
            cleaned != "." &&
            cleaned != ".." &&
            cleaned.none { it.code < 0x20 }
    }
}
