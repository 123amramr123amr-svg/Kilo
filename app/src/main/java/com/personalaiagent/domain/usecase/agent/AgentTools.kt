package com.personalaiagent.domain.usecase.agent

import com.personalaiagent.core.AppConstants
import com.personalaiagent.core.AppError
import com.personalaiagent.core.Outcome
import com.personalaiagent.core.SafPaths
import com.personalaiagent.core.WorkspacePaths
import com.personalaiagent.domain.model.AgentTool
import com.personalaiagent.domain.model.ToolCall
import com.personalaiagent.domain.model.ToolResult
import com.personalaiagent.domain.model.WorkspaceEntry
import com.personalaiagent.domain.repository.DeviceControl
import com.personalaiagent.domain.repository.ImageGeneration
import com.personalaiagent.domain.repository.SettingsRepository
import com.personalaiagent.domain.repository.WorkspaceRepository
import com.personalaiagent.platform.ShellExecutor
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * The tools the agent may call. They all operate inside the user's workspace
 * folder; nothing else on the device is reachable.
 */
object AgentToolCatalog {

    const val LIST_FILES = "list_files"
    const val READ_FILE = "read_file"
    const val WRITE_FILE = "write_file"
    const val APPEND_FILE = "append_file"
    const val CREATE_FOLDER = "create_folder"
    const val DELETE_PATH = "delete_path"
    const val LIST_APPS = "list_apps"
    const val OPEN_APP = "open_app"
    const val WEB_SEARCH = "web_search"
    const val OPEN_URL = "open_url"
    const val GENERATE_IMAGE = "generate_image"

    // Code-agent tools. The old file tools stay accepted; these are the ones the
    // model is told about, because they cover the workflow of a real agent:
    // find → read → change a region → check.
    const val READ = "read"
    const val EDIT = "edit"
    const val PATCH = "patch"
    const val GREP = "grep"
    const val GLOB = "glob"
    const val SHELL = "shell"
    const val DIAGNOSTICS = "diagnostics"
    const val QUESTION = "question"
    const val SKILL = "skill"

    val tools: List<AgentTool> = listOf(
        AgentTool(
            name = LIST_FILES,
            description = "List the files and folders inside the user's workspace. " +
                "Use \"\" for the workspace root or a relative folder such as \"documents\".",
            parametersJson = """
                {"type":"object","properties":{
                  "path":{"type":"string","description":"Relative folder path, empty for the root"}
                },"required":["path"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = READ_FILE,
            description = "Read a UTF-8 text file from the workspace. Returns the content, " +
                "or an error when the file does not exist.",
            parametersJson = """
                {"type":"object","properties":{
                  "path":{"type":"string","description":"Relative file path"}
                },"required":["path"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = WRITE_FILE,
            description = "Create a text file in the workspace or overwrite an existing one. " +
                "Parent folders are created automatically.",
            parametersJson = """
                {"type":"object","properties":{
                  "path":{"type":"string","description":"Relative file path"},
                  "content":{"type":"string","description":"Full file content to write"}
                },"required":["path","content"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = APPEND_FILE,
            description = "Append text to a file in the workspace, creating it if missing.",
            parametersJson = """
                {"type":"object","properties":{
                  "path":{"type":"string","description":"Relative file path"},
                  "content":{"type":"string","description":"Text to append"}
                },"required":["path","content"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = CREATE_FOLDER,
            description = "Create a folder (and any missing parents) in the workspace.",
            parametersJson = """
                {"type":"object","properties":{
                  "path":{"type":"string","description":"Relative folder path"}
                },"required":["path"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = GENERATE_IMAGE,
            description = "Create a picture from a text prompt with an image-capable model and " +
                "save it into the workspace. Use it when the user asks for an image to be drawn.",
            parametersJson = """
                {"type":"object","properties":{
                  "prompt":{"type":"string","description":"Detailed description of the image"},
                  "file_name":{"type":"string","description":"Optional file name, e.g. cat.png"},
                  "size":{"type":"string","description":"1024x1024, 1024x1536 or 1536x1024","default":"1024x1024"}
                },"required":["prompt"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = LIST_APPS,
            description = "List the apps installed on the phone that can be opened by name.",
            parametersJson = """{"type":"object","properties":{}}""",
        ),
        AgentTool(
            name = OPEN_APP,
            description = "Open an app on the phone by its name or package, e.g. \"WhatsApp\".",
            parametersJson = """
                {"type":"object","properties":{
                  "app":{"type":"string","description":"App name or package name"}
                },"required":["app"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = WEB_SEARCH,
            description = "Search the web on the user's device, e.g. searching inside the " +
                "browser for a query the user asked about.",
            parametersJson = """
                {"type":"object","properties":{
                  "query":{"type":"string","description":"Search terms"}
                },"required":["query"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = OPEN_URL,
            description = "Open a web link in the phone's browser.",
            parametersJson = """
                {"type":"object","properties":{
                  "url":{"type":"string","description":"http or https address"}
                },"required":["url"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = READ,
            description = "Read a text file with line numbers. Page a long file with " +
                "offset (first line, 1-based) and limit (number of lines, default 400).",
            parametersJson = """
                {"type":"object","properties":{
                  "path":{"type":"string","description":"Relative file path"},
                  "offset":{"type":"integer","description":"First line to show, 1-based"},
                  "limit":{"type":"integer","description":"How many lines, default 400"}
                },"required":["path"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = EDIT,
            description = "Replace one exact piece of text inside a file. old_string must " +
                "appear exactly once (or set replace_all). Read the file first.",
            parametersJson = """
                {"type":"object","properties":{
                  "path":{"type":"string","description":"Relative file path"},
                  "old_string":{"type":"string","description":"Exact text to replace, including indentation"},
                  "new_string":{"type":"string","description":"Replacement text"},
                  "replace_all":{"type":"boolean","description":"Replace every occurrence instead of one"}
                },"required":["path","old_string","new_string"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = PATCH,
            description = "Apply several exact replacements to one file at once, all or " +
                "nothing. Use it for a change that touches more than one place.",
            parametersJson = """
                {"type":"object","properties":{
                  "path":{"type":"string","description":"Relative file path"},
                  "edits":{"type":"array","description":"Ordered list of replacements","items":{
                    "type":"object","properties":{
                      "old_string":{"type":"string"},
                      "new_string":{"type":"string"}
                    },"required":["old_string","new_string"]}}
                },"required":["path","edits"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = GREP,
            description = "Search file contents in the workspace (regular expression). " +
                "Returns file:line:text. Filter with glob, e.g. \"**/*.kt\".",
            parametersJson = """
                {"type":"object","properties":{
                  "pattern":{"type":"string","description":"Regular expression"},
                  "path":{"type":"string","description":"Folder to search, empty for the whole workspace"},
                  "glob":{"type":"string","description":"Only search files matching this pattern"},
                  "max_results":{"type":"integer","description":"Default 60"}
                },"required":["pattern"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = GLOB,
            description = "Find files by name pattern, e.g. \"**/*.kt\" or \"notes/*.md\". " +
                "Returns paths, newest first.",
            parametersJson = """
                {"type":"object","properties":{
                  "pattern":{"type":"string","description":"Glob pattern"},
                  "path":{"type":"string","description":"Folder to start from, empty for the workspace root"}
                },"required":["pattern"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = SHELL,
            description = "Run a real shell command on the phone (Android sh: ls, cat, grep, " +
                "sed, find, wc, head, tail, cp, mv, mkdir, rm, du, sort, uniq…). It runs inside " +
                "the workspace folder when that folder is on this device's storage. There is no " +
                "python and no root.",
            parametersJson = """
                {"type":"object","properties":{
                  "command":{"type":"string","description":"Shell command line"},
                  "timeout_seconds":{"type":"integer","description":"Default 20, max 60"}
                },"required":["command"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = DIAGNOSTICS,
            description = "Check a file for structural problems before telling the user it is " +
                "done: JSON validity, unbalanced brackets or XML tags, unterminated strings, " +
                "duplicate string resources. Not a compiler.",
            parametersJson = """
                {"type":"object","properties":{
                  "path":{"type":"string","description":"Relative file path"}
                },"required":["path"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = QUESTION,
            description = "Ask the user a question and wait for the answer. Use it only when " +
                "the request cannot be completed correctly without a decision (a path, a " +
                "choice between two options). Never use it to confirm something obvious.",
            parametersJson = """
                {"type":"object","properties":{
                  "question":{"type":"string","description":"One clear question"},
                  "options":{"type":"array","items":{"type":"string"},"description":"Up to 4 suggested answers"}
                },"required":["question"]}
            """.trimIndent(),
        ),
        AgentTool(
            name = SKILL,
            description = "Load one of the installed skills, or list them. Call it with no " +
                "name to see what is available, then with the name to read its instructions.",
            parametersJson = """
                {"type":"object","properties":{
                  "name":{"type":"string","description":"Skill name, e.g. code-review"}
                }}
            """.trimIndent(),
        ),
        AgentTool(
            name = DELETE_PATH,
            description = "Delete a file or an empty folder from the workspace. " +
                "Only use it when the user explicitly asks to delete something.",
            parametersJson = """
                {"type":"object","properties":{
                  "path":{"type":"string","description":"Relative path to delete"}
                },"required":["path"]}
            """.trimIndent(),
        ),
    )

    /** System prompt fragment that explains the workspace to the model. */
    fun systemPrompt(workspaceName: String, skills: List<String> = emptyList()): String = """
        You are an on-device agent running inside an Android app. The user's phone is
        your workspace: the folder "$workspaceName" is open to you through tools.

        Rules:
        - Use the file tools to actually perform file work. Never claim a file was
          created, read or edited unless the tool result confirms it.
        - Work like an engineer: find with glob/grep, read before changing, change
          with edit/patch (never rewrite a whole file for a small fix), then run
          diagnostics on what you changed and report the result.
        - shell runs a real Android shell inside the workspace folder. Use it for
          listing, counting and quick checks; it has no python, no compiler and no root.
        - Paths are relative to the workspace root. Do not use absolute paths or "..".
        - Read a file before rewriting it when the user asks for a change.
        - Prefer small, complete file contents; do not truncate with placeholders.
        - Answer in the language the user writes in.
        - When the task is done, summarise briefly what changed and list the paths.

        Phone actions you may take (use them only when asked):
        - list_apps / open_app: open an installed app by name.
        - web_search / open_url: start a search or open a link on the device.
        - generate_image: draw a picture with an image model and save it in the workspace.
        - question: ask the user and wait for the answer when a decision is missing.
        - skill: load the instructions of one of the installed skills when it fits.
        You can open apps and links, but you cannot tap inside another app's screens.
    """.trimIndent() + if (skills.isEmpty()) {
        ""
    } else {
        "\n\nInstalled skills (call skill with the name to read its instructions " +
            "before doing that kind of work):\n" + skills.joinToString("\n") { "  - $it" }
    }
}

/** Per-run information the tools need (which provider/model to use). */
data class ToolContext(
    val providerId: String? = null,
    val modelId: String? = null,
)

class AgentToolExecutor @Inject constructor(
    private val workspace: WorkspaceRepository,
    private val json: Json,
    private val device: DeviceControl,
    private val imageGeneration: ImageGeneration,
    private val skills: SkillSource,
    private val shell: ShellExecutor,
    private val search: WorkspaceSearch,
    private val questions: QuestionBroker,
    private val settings: SettingsRepository,
) {

    /** Executes one tool call and returns a compact, model-friendly result. */
    suspend fun execute(call: ToolCall, context: ToolContext = ToolContext()): ToolResult {
        val args = parseArguments(call.arguments)
        val outcome: Outcome<String> = when (call.name) {
            AgentToolCatalog.LIST_FILES -> withPath(argumentString(args, "path")) { listFiles(it) }
            AgentToolCatalog.READ_FILE -> withPath(argumentString(args, "path")) {
                readFile(
                    path = it,
                    offset = ((args["offset"] as? JsonPrimitive)?.intOrNull ?: 0).coerceAtLeast(0),
                    limit = ((args["limit"] as? JsonPrimitive)?.intOrNull ?: 20_000).coerceIn(1, 120_000),
                )
            }
            AgentToolCatalog.WRITE_FILE -> withPath(argumentString(args, "path")) { safe ->
                writeFile(path = safe, content = argumentString(args, "content"), append = false)
            }
            AgentToolCatalog.APPEND_FILE -> withPath(argumentString(args, "path")) { safe ->
                writeFile(path = safe, content = argumentString(args, "content"), append = true)
            }
            AgentToolCatalog.CREATE_FOLDER -> withPath(argumentString(args, "path")) { createFolder(it) }
            AgentToolCatalog.DELETE_PATH -> withPath(argumentString(args, "path")) { deletePath(it) }
            AgentToolCatalog.READ -> withPath(argumentString(args, "path")) { safe ->
                readLines(
                    path = safe,
                    offset = argumentInt(args, "offset", 1).coerceAtLeast(1),
                    limit = argumentInt(args, "limit", 400).coerceIn(1, 4_000),
                )
            }
            AgentToolCatalog.EDIT -> withPath(argumentString(args, "path")) { safe ->
                editFile(
                    path = safe,
                    oldString = argumentString(args, "old_string"),
                    newString = argumentString(args, "new_string"),
                    replaceAll = argumentBoolean(args, "replace_all"),
                )
            }
            AgentToolCatalog.PATCH -> withPath(argumentString(args, "path")) { safe ->
                patchFile(safe, parseEdits(args))
            }
            AgentToolCatalog.GREP -> grepWorkspace(args)
            AgentToolCatalog.GLOB -> globWorkspace(args)
            AgentToolCatalog.SHELL -> runShell(args)
            AgentToolCatalog.DIAGNOSTICS -> withPath(argumentString(args, "path")) { diagnosticsOf(it) }
            AgentToolCatalog.QUESTION -> askUser(args)
            AgentToolCatalog.SKILL -> loadSkill(args)
            AgentToolCatalog.LIST_APPS -> listApps()
            AgentToolCatalog.OPEN_APP -> device.openApp(argumentString(args, "app"))
            AgentToolCatalog.WEB_SEARCH -> device.webSearch(argumentString(args, "query"))
            AgentToolCatalog.OPEN_URL -> device.openUrl(argumentString(args, "url"))
            AgentToolCatalog.GENERATE_IMAGE -> generateImage(args, context)
            else -> Outcome.Failure(com.personalaiagent.core.AppError.InvalidInput("unknown_tool"))
        }

        return when (outcome) {
            is Outcome.Success -> ToolResult(call.id, call.name, ok = true, output = outcome.value.take(AppConstants.MAX_TOOL_OUTPUT_CHARS))
            is Outcome.Failure -> ToolResult(
                callId = call.id,
                name = call.name,
                ok = false,
                output = "ERROR: ${failureText(outcome.error)}",
            )
        }
    }

    /**
     * Normalises a model-supplied path and refuses anything that would escape the
     * workspace (`..`, control characters, oversized names) before touching storage.
     */
    private suspend fun withPath(raw: String, block: suspend (String) -> Outcome<String>): Outcome<String> {
        val normalized = WorkspacePaths.normalize(raw)
            ?: return Outcome.Failure(AppError.InvalidInput("unsafe_path"))
        return block(normalized)
    }

    private suspend fun listFiles(path: String): Outcome<String> =
        when (val result = workspace.list(path)) {
            is Outcome.Failure -> result
            is Outcome.Success -> {
                if (result.value.isEmpty()) {
                    Outcome.Success("The folder is empty.")
                } else {
                    val lines = result.value.map { entry -> describe(entry) }
                    Outcome.Success(
                        "Contents of \"${path.ifBlank { "/" }}\" (${result.value.size}):\n" +
                            lines.joinToString("\n"),
                    )
                }
            }
        }

    private suspend fun readFile(path: String, offset: Int, limit: Int): Outcome<String> =
        when (val result = workspace.read(path)) {
            is Outcome.Failure -> result
            is Outcome.Success -> {
                val file = result.value
                val start = offset.coerceIn(0, file.content.length)
                val end = (start + limit).coerceAtMost(file.content.length)
                buildString {
                    append("File: ${file.path} (${file.size} bytes")
                    if (file.truncated) append(", truncated to ${file.content.length} chars")
                    append(")\n")
                    if (start > 0 || end < file.content.length) {
                        append("Showing characters $start..$end of ${file.content.length}\n")
                    }
                    append("-----\n")
                    append(file.content.substring(start, end))
                    if (end < file.content.length) {
                        append("\n----\n(more content: call read_file with offset=$end)")
                    }
                }.let { Outcome.Success(it) }
            }
        }

    private suspend fun writeFile(path: String, content: String, append: Boolean): Outcome<String> =
        when (val result = workspace.write(path, content, append)) {
            is Outcome.Failure -> result
            is Outcome.Success ->
                Outcome.Success(
                    (if (append) "Appended to " else "Wrote ") + result.value.path +
                        " (${result.value.size} bytes).",
                )
        }

    private suspend fun createFolder(path: String): Outcome<String> =
        when (val result = workspace.createFolder(path)) {
            is Outcome.Failure -> result
            is Outcome.Success -> Outcome.Success("Created folder ${result.value.path}.")
        }

    private suspend fun deletePath(path: String): Outcome<String> =
        when (val result = workspace.delete(path)) {
            is Outcome.Failure -> result
            is Outcome.Success -> Outcome.Success("Deleted $path.")
        }

    // ---------------------------------------------------------- code tools

    /** `read`: line numbers, so the model can quote and edit precisely. */
    private suspend fun readLines(path: String, offset: Int, limit: Int): Outcome<String> =
        when (val result = workspace.read(path, AppConstants.MAX_TOOL_READ_BYTES)) {
            is Outcome.Failure -> result
            is Outcome.Success -> {
                val file = result.value
                val lines = file.content.split('\n')
                val from = (offset - 1).coerceIn(0, maxOf(lines.size - 1, 0))
                val to = (from + limit).coerceAtMost(lines.size)
                buildString {
                    append("File: ${file.path} · ${lines.size} lines · ${file.size} bytes")
                    if (file.truncated) append(" (read truncated at ${file.content.length} chars)")
                    append('\n')
                    for (index in from until to) {
                        append((index + 1).toString().padStart(5))
                        append("│ ")
                        append(lines[index])
                        append('\n')
                    }
                    if (to < lines.size) {
                        append("… ${lines.size - to} more lines: call read with offset=${to + 1}")
                    }
                }.let { Outcome.Success(it) }
            }
        }

    private suspend fun editFile(
        path: String,
        oldString: String,
        newString: String,
        replaceAll: Boolean,
    ): Outcome<String> {
        if (oldString.isEmpty()) return Outcome.Failure(AppError.InvalidInput("empty_old_string"))
        val file = (workspace.read(path, AppConstants.MAX_EDITOR_BYTES) as? Outcome.Success)?.value
            ?: return Outcome.Failure(AppError.Unknown("not_found: $path"))
        val occurrences = countOccurrences(file.content, oldString)
        if (occurrences == 0) {
            return Outcome.Failure(
                AppError.Unknown(
                    "old_string not found in $path — read the file and copy the exact lines, " +
                        "including indentation",
                ),
            )
        }
        if (occurrences > 1 && !replaceAll) {
            return Outcome.Failure(
                AppError.Unknown(
                    "old_string appears $occurrences times in $path — add more context, or set replace_all",
                ),
            )
        }
        val updated = if (replaceAll) {
            file.content.replace(oldString, newString)
        } else {
            file.content.replaceFirst(oldString, newString)
        }
        return when (val written = workspace.write(path, updated)) {
            is Outcome.Failure -> written
            is Outcome.Success ->
                Outcome.Success("Edited ${written.value.path}: $occurrences replacement(s), ${updated.length} chars now.")
        }
    }

    private suspend fun patchFile(path: String, edits: List<Pair<String, String>>): Outcome<String> {
        if (edits.isEmpty()) return Outcome.Failure(AppError.InvalidInput("no_edits"))
        val file = (workspace.read(path, AppConstants.MAX_EDITOR_BYTES) as? Outcome.Success)?.value
            ?: return Outcome.Failure(AppError.Unknown("not_found: $path"))
        var content = file.content
        edits.forEachIndexed { index, (oldString, newString) ->
            if (oldString.isEmpty()) {
                return Outcome.Failure(AppError.Unknown("edit ${index + 1}: old_string is empty"))
            }
            val occurrences = countOccurrences(content, oldString)
            when {
                occurrences == 0 -> return Outcome.Failure(
                    AppError.Unknown("edit ${index + 1}: old_string not found — nothing was changed"),
                )
                occurrences > 1 -> return Outcome.Failure(
                    AppError.Unknown("edit ${index + 1}: old_string appears $occurrences times — nothing was changed"),
                )
                else -> content = content.replaceFirst(oldString, newString)
            }
        }
        return when (val written = workspace.write(path, content)) {
            is Outcome.Failure -> written
            is Outcome.Success ->
                Outcome.Success("Patched ${written.value.path}: ${edits.size} edit(s), ${content.length} chars now.")
        }
    }

    private suspend fun grepWorkspace(args: JsonObject): Outcome<String> {
        val pattern = argumentString(args, "pattern")
        if (pattern.isBlank()) return Outcome.Failure(AppError.InvalidInput("empty_pattern"))
        val folder = WorkspacePaths.normalize(argumentString(args, "path"))
            ?: return Outcome.Failure(AppError.InvalidInput("unsafe_path"))
        val files = search.walk(folder)
        val filtered = argumentString(args, "glob").takeIf { it.isNotBlank() }
            ?.let { pattern2 -> search.glob(pattern2, files) }
            ?: files
        val max = argumentInt(args, "max_results", 60).coerceIn(1, 200)
        val result = search.grep(pattern, filtered, maxHits = max)
        if (result.hits.isEmpty()) {
            return Outcome.Success(
                "No matches for \"$pattern\" in ${result.filesScanned} file(s).",
            )
        }
        return Outcome.Success(
            buildString {
                append("${result.hits.size} match(es) in ${result.filesMatched} file(s):\n")
                result.hits.forEach { hit ->
                    append(hit.path).append(':').append(hit.line).append(": ").append(hit.text).append('\n')
                }
                if (result.truncated) append("(stopped at the limit — narrow the pattern or the folder)")
            }.trim(),
        )
    }

    private suspend fun globWorkspace(args: JsonObject): Outcome<String> {
        val raw = argumentString(args, "pattern")
        if (raw.isBlank()) return Outcome.Failure(AppError.InvalidInput("empty_pattern"))
        val folder = WorkspacePaths.normalize(argumentString(args, "path"))
            ?: return Outcome.Failure(AppError.InvalidInput("unsafe_path"))
        val matches = search.glob(raw, search.walk(folder))
        if (matches.isEmpty()) return Outcome.Success("No files match \"$raw\".")
        return Outcome.Success(
            buildString {
                append("${matches.size} file(s) matching \"$raw\"${if (folder.isNotEmpty()) " in $folder" else ""}:\n")
                matches.take(200).forEach { entry -> append(entry.path).append('\n') }
                if (matches.size > 200) append("… ${matches.size - 200} more")
            }.trim(),
        )
    }

    private suspend fun runShell(args: JsonObject): Outcome<String> {
        val command = argumentString(args, "command")
        if (command.isBlank()) return Outcome.Failure(AppError.InvalidInput("empty_command"))
        com.personalaiagent.platform.ShellPolicy.denyReason(command)?.let { reason ->
            return Outcome.Failure(AppError.InvalidInput(reason))
        }
        val workspaceUri = settings.current().workspaceUri
        val localDir = SafPaths.existingDirectory(workspaceUri)
        val workdir = localDir ?: shell.homeDirectory()
        val timeout = argumentInt(args, "timeout_seconds", 20).coerceIn(1, 60) * 1_000L
        val result = shell.run(command, workdir, timeoutMillis = timeout)
        val header = buildString {
            append("$ ").append(command).append('\n')
            append("(cwd: ").append(result.workdir).append(", exit ").append(result.exitCode).append(")\n")
            if (localDir == null) {
                append(
                    "(note: the workspace folder is not on this phone's storage, so the command ran " +
                        "in the app's private home folder; use glob/grep/read for workspace files)\n",
                )
            }
        }
        return Outcome.Success(header + result.output)
    }

    private suspend fun diagnosticsOf(path: String): Outcome<String> =
        when (val result = workspace.read(path, AppConstants.MAX_EDITOR_BYTES)) {
            is Outcome.Failure -> result
            is Outcome.Success -> Outcome.Success(
                Diagnostics.check(path, result.value.content).render(),
            )
        }

    private suspend fun askUser(args: JsonObject): Outcome<String> {
        val question = argumentString(args, "question")
        if (question.isBlank()) return Outcome.Failure(AppError.InvalidInput("empty_question"))
        val options = (args["options"] as? kotlinx.serialization.json.JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .orEmpty()
        val answer = questions.ask(question, options)
        return Outcome.Success("The user answered: $answer")
    }

    private fun loadSkill(args: JsonObject): Outcome<String> {
        val all = skills.all()
        if (all.isEmpty()) return Outcome.Success("No skills are installed.")
        val requested = argumentString(args, "name")
        if (requested.isBlank()) {
            return Outcome.Success(
                "Installed skills (call skill with the name to load one):\n" +
                    all.joinToString("\n") { "- ${it.name}: ${it.description}" },
            )
        }
        val skill = skills.byName(requested)
            ?: return Outcome.Failure(
                AppError.Unknown(
                    "unknown skill \"$requested\" — available: " + all.joinToString { it.name },
                ),
            )
        return Outcome.Success("# Skill: ${skill.name}\n${skill.body}")
    }

    private fun countOccurrences(haystack: String, needle: String): Int {
        if (needle.isEmpty()) return 0
        var count = 0
        var index = haystack.indexOf(needle)
        while (index >= 0) {
            count++
            index = haystack.indexOf(needle, index + needle.length)
        }
        return count
    }

    private fun parseEdits(args: JsonObject): List<Pair<String, String>> =
        (args["edits"] as? kotlinx.serialization.json.JsonArray)
            ?.mapNotNull { element ->
                val object0 = element as? JsonObject ?: return@mapNotNull null
                val old = (object0["old_string"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                val new = (object0["new_string"] as? JsonPrimitive)?.contentOrNull ?: ""
                old to new
            }
            .orEmpty()

    private fun argumentInt(args: JsonObject, key: String, fallback: Int): Int =
        (args[key] as? JsonPrimitive)?.intOrNull ?: fallback

    private fun argumentBoolean(args: JsonObject, key: String): Boolean =
        (args[key] as? JsonPrimitive)?.let { it.contentOrNull.equals("true", ignoreCase = true) } ?: false

    private suspend fun listApps(): Outcome<String> {
        val apps = device.launchableApps()
        return if (apps.isEmpty()) {
            Outcome.Success("No launchable apps are visible to the app.")
        } else {
            Outcome.Success(
                "Installed apps (${apps.size}):\n" + apps.joinToString("\n") { "- ${it.label} (${it.packageName})" },
            )
        }
    }

    private suspend fun generateImage(args: JsonObject, context: ToolContext): Outcome<String> {
        val generator = imageGeneration
        val prompt = argumentString(args, "prompt")
        if (prompt.isBlank()) return Outcome.Failure(AppError.InvalidInput("prompt"))
        val provider = context.providerId ?: return Outcome.Failure(AppError.NoProvider)
        val model = context.modelId ?: return Outcome.Failure(AppError.NoModel)
        val size = argumentString(args, "size").ifBlank { "1024x1024" }
        val name = argumentString(args, "file_name").ifBlank { "image-${System.currentTimeMillis()}.png" }

        return when (val result = generator.generate(provider, model, prompt, size)) {
            is Outcome.Failure -> result
            is Outcome.Success -> {
                val extension = if (result.value.mimeType == "image/jpeg") "jpg" else "png"
                val fileName = if (name.contains('.')) name else "$name.$extension"
                val path = WorkspacePaths.join("generated", fileName)
                    ?: return Outcome.Failure(AppError.InvalidInput("name"))
                when (val saved = workspace.writeBytes(path, result.value.bytes, result.value.mimeType)) {
                    is Outcome.Failure -> saved
                    is Outcome.Success -> Outcome.Success(
                        "Generated an image and saved it to ${saved.value.path} (${saved.value.size} bytes).",
                    )
                }
            }
        }
    }


    private fun describe(entry: WorkspaceEntry): String = buildString {
        append(if (entry.isDirectory) "[dir] " else "[file] ")
        append(entry.path)
        if (!entry.isDirectory) append(" (${entry.size} bytes)")
    }

    private fun failureText(error: com.personalaiagent.core.AppError): String = when (error) {
        com.personalaiagent.core.AppError.NoWorkspace ->
            "No workspace folder is connected. Ask the user to pick one in Settings > Workspace."
        is com.personalaiagent.core.AppError.InvalidInput -> "Invalid path (${error.reason})."
        is com.personalaiagent.core.AppError.Unknown ->
            if (error.message == "not_found") "Path not found." else (error.message ?: "Unknown error.")
        else -> "Operation failed."
    }

    private fun parseArguments(raw: String): JsonObject = try {
        json.parseToJsonElement(raw.ifBlank { "{}" }) as? JsonObject ?: JsonObject(emptyMap())
    } catch (e: Exception) {
        JsonObject(emptyMap())
    }

    private fun argumentString(args: JsonObject, key: String): String =
        (args[key] as? JsonPrimitive)?.contentOrNull ?: ""

    @Suppress("unused")
    private fun argumentInt(args: JsonObject, key: String): Int? =
        (args[key] as? JsonPrimitive)?.intOrNull

    @Suppress("unused")
    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.jsonPrimitive?.contentOrNull
}
