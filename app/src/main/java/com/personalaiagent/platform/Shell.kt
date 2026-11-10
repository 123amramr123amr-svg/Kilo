package com.personalaiagent.platform

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ShellResult(
    val exitCode: Int,
    val output: String,
    val timedOut: Boolean,
    /** The directory the command really ran in. */
    val workdir: String,
)

/**
 * Runs **real** shell commands with the Android system shell (`/system/bin/sh`).
 *
 * This is not an emulator and not a list of canned replies: the process runs with the
 * app's own UID, in a real directory, with the toybox userland Android ships (`ls`,
 * `cat`, `grep`, `sed`, `find`, `wc`, `head`, `tail`, `cp`, `mv`, `mkdir`, `rm`, `du`,
 * `sort`, `uniq`, `date`, `printf`) — and nothing else. There is no `apt`, no `python`,
 * no root, and files outside the app's sandbox are unreadable by the OS itself.
 *
 * Output is captured (bounded), the process is killed after [timeoutMillis], and a
 * short deny-list stops the handful of commands that would damage the user's folder.
 */
/** The real shell, behind an interface so the tool can be tested without Android. */
interface ShellExecutor {
    suspend fun run(
        command: String,
        workdir: File,
        timeoutMillis: Long = 20_000L,
        maxOutputChars: Int = 20_000,
    ): ShellResult

    fun homeDirectory(): File
}

/**
 * A handful of commands that can destroy the folder they run in — or the phone.
 *
 * Checked twice on purpose: in the tool layer (so the model gets a clear refusal
 * without spawning a process) and in the runner (so nothing bypasses it).
 */
object ShellPolicy {

    fun denyReason(command: String): String? {
        val lower = command.lowercase()
        return BLOCKED.firstOrNull { (pattern, _) -> pattern.containsMatchIn(lower) }?.second
    }

    /** Regexes, not substrings: `rm -rf /` must not block `rm -rf /storage/x`. */
    private val BLOCKED: List<Pair<Regex, String>> = listOf(
        Regex("""rm\s+-[a-z]*r[a-z]*f[a-z]*\s+/\*?(\s|$)""") to
            "refusing to delete from the filesystem root",
        Regex("""rm\s+-[a-z]*f[a-z]*r[a-z]*\s+/\*?(\s|$)""") to
            "refusing to delete from the filesystem root",
        Regex("""\bmkfs(\s|$)""") to "refusing to format a filesystem",
        Regex("""\bdd\s+if=""") to "refusing raw device writes",
        Regex("""\berase\s+/dev/block""") to "refusing raw device writes",
        Regex("""\breboot(\s|$)""") to "the app cannot reboot the phone",
        Regex("""\bshutdown(\s|$)""") to "the app cannot power off the phone",
        Regex("""\bsu\s+-c""") to "root is not available to this app",
        Regex("""chmod\s+-r\s+777\s+/\s*$""") to "refusing to change permissions on the whole device",
    )
}

@Singleton
class AndroidShellRunner @Inject constructor(
    @ApplicationContext private val context: Context,
) : ShellExecutor {

    override suspend fun run(
        command: String,
        workdir: File,
        timeoutMillis: Long,
        maxOutputChars: Int,
    ): ShellResult = withContext(Dispatchers.IO) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return@withContext ShellResult(2, "ERROR: empty command", timedOut = false, workdir = workdir.path)
        }
        deniedReason(trimmed)?.let { reason ->
            return@withContext ShellResult(126, "ERROR: $reason", timedOut = false, workdir = workdir.path)
        }
        if (!workdir.isDirectory) {
            workdir.mkdirs()
        }

        val process = runCatching {
            ProcessBuilder(SHELL, "-c", trimmed)
                .directory(workdir)
                .redirectErrorStream(true)
                .apply {
                    environment()["PATH"] = "$SHELL_BIN:/system/xbin:/system/bin"
                    environment()["HOME"] = workdir.absolutePath
                    environment()["TMPDIR"] = context.cacheDir.absolutePath
                }
                .start()
        }.getOrElse { error ->
            return@withContext ShellResult(
                exitCode = 127,
                output = "ERROR: could not start a shell (${error.message ?: error.javaClass.simpleName})",
                timedOut = false,
                workdir = workdir.path,
            )
        }

        val output = StringBuilder()
        val reader = Thread {
            runCatching {
                process.inputStream.bufferedReader().use { stream ->
                    val buffer = CharArray(4_096)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read <= 0) break
                        if (output.length < maxOutputChars) {
                            output.append(buffer, 0, minOf(read, maxOutputChars - output.length))
                        }
                    }
                }
            }
        }.apply { isDaemon = true; start() }

        val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
        if (!finished) {
            runCatching { process.destroyForcibly() }
        }
        reader.join(1_000)

        val exit = if (finished) runCatching { process.exitValue() }.getOrDefault(-1) else -1
        val text = output.toString().trimEnd()
        when {
            !finished -> ShellResult(
                exitCode = 124,
                output = buildString {
                    if (text.isNotEmpty()) append(text).append('\n')
                    append("ERROR: the command was still running after ${timeoutMillis / 1000}s and was stopped.")
                },
                timedOut = true,
                workdir = workdir.path,
            )
            text.isEmpty() -> ShellResult(
                exitCode = exit,
                output = "(no output · exit code $exit)",
                timedOut = false,
                workdir = workdir.path,
            )
            else -> ShellResult(exit, text, timedOut = false, workdir = workdir.path)
        }
    }

    private fun deniedReason(command: String): String? = ShellPolicy.denyReason(command)

    /** Where `shell` runs when no workspace path is available: a real, private folder. */
    override fun homeDirectory(): File = File(context.filesDir, "agent-home").apply { mkdirs() }

    private companion object {
        const val SHELL = "/system/bin/sh"
        const val SHELL_BIN = "/system/bin"
        const val DEFAULT_TIMEOUT_MS = 20_000L
        const val MAX_OUTPUT_CHARS = 20_000
    }
}
