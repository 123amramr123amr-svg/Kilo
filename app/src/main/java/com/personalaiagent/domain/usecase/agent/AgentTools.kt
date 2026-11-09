package com.personalaiagent.domain.usecase.agent

import com.personalaiagent.core.AppConstants
import com.personalaiagent.core.AppError
import com.personalaiagent.core.Outcome
import com.personalaiagent.core.WorkspacePaths
import com.personalaiagent.domain.model.AgentTool
import com.personalaiagent.domain.model.ToolCall
import com.personalaiagent.domain.model.ToolResult
import com.personalaiagent.domain.model.WorkspaceEntry
import com.personalaiagent.domain.repository.DeviceControl
import com.personalaiagent.domain.repository.ImageGeneration
import com.personalaiagent.domain.repository.WorkspaceRepository
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
    fun systemPrompt(workspaceName: String): String = """
        You are an on-device agent running inside an Android app. The user's phone is
        your workspace: the folder "$workspaceName" is open to you through tools.

        Rules:
        - Use the file tools to actually perform file work. Never claim a file was
          created, read or edited unless the tool result confirms it.
        - Paths are relative to the workspace root. Do not use absolute paths or "..".
        - Read a file before rewriting it when the user asks for a change.
        - Prefer small, complete file contents; do not truncate with placeholders.
        - Answer in the language the user writes in.
        - When the task is done, summarise briefly what changed and list the paths.

        Phone actions you may take (use them only when asked):
        - list_apps / open_app: open an installed app by name.
        - web_search / open_url: start a search or open a link on the device.
        - generate_image: draw a picture with an image model and save it in the workspace.
        You can open apps and links, but you cannot tap inside another app's screens.
    """.trimIndent()
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
