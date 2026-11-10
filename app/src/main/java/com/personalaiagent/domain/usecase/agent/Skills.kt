package com.personalaiagent.domain.usecase.agent

import android.content.Context
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A skill: a short instruction file the agent loads **only when it is relevant**.
 *
 * The bundled ones live in `assets/skills/<name>/SKILL.md` and are parsed on first
 * use, so they cost nothing at startup. A skill whose file is missing is skipped
 * instead of failing the tool.
 */
data class Skill(
    val name: String,
    val description: String,
    val body: String,
    /** True for skills the user wrote; false for the bundled ones. */
    val custom: Boolean = false,
)

/** What the `skill` tool needs; an interface so it can be faked in tests. */
interface SkillSource {
    fun all(): List<Skill>
    fun byName(name: String): Skill?
    fun catalog(): String

    /** Writes a new skill (same `SKILL.md` format) and returns it. */
    suspend fun add(name: String, description: String, body: String): Result<Skill>

    /** Removes a skill; bundled ones are hidden rather than deleted from assets. */
    suspend fun delete(name: String): Result<Unit>
}

@Singleton
class AssetSkillLibrary @Inject constructor(
    @ApplicationContext private val context: Context,
) : SkillSource {

    @Volatile
    private var cache: List<Skill>? = null

    /** Every bundled skill, sorted by name. Never throws. */
    override fun all(): List<Skill> {
        cache?.let { return it }
        val loaded = runCatching { readAssets() }.getOrDefault(emptyList())
        cache = loaded
        return loaded
    }

    override fun byName(name: String): Skill? =
        all().firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    /**
     * User skills live in `filesDir/skills/<name>/SKILL.md`, the same format as the
     * bundled ones, so the skill tool, the prompt and Settings all read one shape.
     * Deleting a bundled skill writes a tombstone instead of touching the APK.
     */
    override suspend fun add(name: String, description: String, body: String): Result<Skill> =
        runCatching {
            val clean = name.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "-")
            require(clean.isNotBlank()) { "empty name" }
            val dir = File(userRoot(), clean).apply { mkdirs() }
            dir.resolve(FILE_NAME).writeText(
                buildString {
                    append("---\n")
                    append("name: ").append(clean).append('\n')
                    append("description: ").append(description.trim().replace("\n", " ")).append('\n')
                    append("---\n")
                    append(body.trim()).append('\n')
                },
            )
            unhide(clean)
            invalidate()
            Skill(clean, description.trim(), body.trim(), custom = true)
        }

    override suspend fun delete(name: String): Result<Unit> = runCatching {
        val clean = name.trim().lowercase()
        val own = File(userRoot(), clean)
        if (own.isDirectory) own.deleteRecursively()
        hide(clean)
        invalidate()
    }

    /**
     * The one-line list the model sees in its system prompt: name + trigger.
     * Empty when nothing is bundled, so the prompt is never padded with noise.
     */
    override fun catalog(): String = all().joinToString("\n") { skill ->
        "- ${skill.name}: ${skill.description}"
    }

    @Volatile
    private var hiddenCache: Set<String>? = null

    private fun readAssets(): List<Skill> {
        val hidden = hiddenNames()
        val dirs = context.assets.list(ASSET_ROOT)?.toList().orEmpty()
        val bundled = dirs.mapNotNull { dir ->
            if (dir in hidden) return@mapNotNull null
            val path = "$ASSET_ROOT/$dir/SKILL.md"
            runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }
                .getOrNull()
                ?.let { parse(it, fallbackName = dir) }
        }
        val own = userRoot().listFiles().orEmpty()
            .filter { it.isDirectory && it.name !in hidden }
            .mapNotNull { dir ->
                runCatching { dir.resolve(FILE_NAME).readText() }.getOrNull()
                    ?.let { parse(it, fallbackName = dir.name)?.copy(custom = true) }
            }
        return (own + bundled).sortedBy { it.name }
    }

    private fun userRoot(): File = File(context.filesDir, "skills").apply { mkdirs() }

    private fun hiddenFile(): File = File(userRoot(), ".deleted")

    private fun hiddenNames(): Set<String> {
        hiddenCache?.let { return it }
        val loaded = runCatching {
            hiddenFile().readLines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }.getOrDefault(emptySet())
        hiddenCache = loaded
        return loaded
    }

    private fun hide(name: String) {
        val next = hiddenNames() + name
        hiddenFile().writeText(next.joinToString("\n"))
        hiddenCache = next
    }

    private fun unhide(name: String) {
        val next = hiddenNames() - name
        hiddenFile().writeText(next.joinToString("\n"))
        hiddenCache = next
    }

    private fun invalidate() {
        cache = null
    }

    private fun parse(raw: String, fallbackName: String): Skill? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        if (!text.startsWith("---")) {
            return Skill(
                name = fallbackName,
                description = text.lineSequence().firstOrNull { it.isNotBlank() }?.take(160)
                    ?: fallbackName,
                body = text,
            )
        }
        val end = text.indexOf("\n---", startIndex = 3)
        if (end < 0) return null
        val front = text.substring(3, end).trim()
        val body = text.substring(end + 4).trim()
        val fields = front.lineSequence()
            .mapNotNull { line ->
                val index = line.indexOf(':')
                if (index <= 0) null
                else line.substring(0, index).trim() to line.substring(index + 1).trim()
            }
            .toMap()
        val name = fields["name"]?.takeIf { it.isNotBlank() } ?: fallbackName
        return Skill(
            name = name,
            description = fields["description"]?.takeIf { it.isNotBlank() } ?: "",
            body = body.ifBlank { front },
        )
    }

    private companion object {
        const val ASSET_ROOT = "skills"
        const val FILE_NAME = "SKILL.md"
    }
}
