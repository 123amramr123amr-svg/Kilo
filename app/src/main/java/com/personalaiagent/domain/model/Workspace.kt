package com.personalaiagent.domain.model

/** Where the agent is allowed to work: a folder the user picked via SAF. */
data class Workspace(
    val uri: String,
    val displayName: String,
)

data class WorkspaceEntry(
    val name: String,
    /** Path relative to the workspace root, e.g. `notes/today.txt`. */
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
)

data class WorkspaceFile(
    val path: String,
    val content: String,
    val size: Long,
    val truncated: Boolean,
)

/** Result of one tool invocation, fed back to the model. */
data class ToolResult(
    val callId: String,
    val name: String,
    val ok: Boolean,
    val output: String,
)

/** A tool the agent may call, described with a JSON schema. */
data class AgentTool(
    val name: String,
    val description: String,
    val parametersJson: String,
)
