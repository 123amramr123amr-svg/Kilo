package com.personalaiagent.core

object AppConstants {
    const val DATABASE_NAME = "personal_ai_agent.db"
    const val SECURE_PREFS_NAME = "pai_secure_keys"
    const val DATASTORE_NAME = "pai_settings"

    /** Maximum number of previous turns sent back to the model. */
    const val MAX_CONTEXT_MESSAGES = 30

    /** Soft cap on the characters of history sent in one request (~6k tokens). */
    const val MAX_CONTEXT_CHARS = 24_000

    /** How many earlier conversations are summarised into the system prompt. */
    const val MEMORY_CONVERSATIONS = 3

    /** Characters of each remembered message. */
    const val MEMORY_MESSAGE_CHARS = 180

    /** Total characters of the cross-conversation memory block. */
    const val MEMORY_TOTAL_CHARS = 900

    /** A run with no event for this long is aborted instead of hanging forever. */
    const val RUN_STALL_TIMEOUT_MS = 120_000L

    /** Backups produced by this app version. */
    const val BACKUP_SCHEMA_VERSION = 1

    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 90L
    const val WRITE_TIMEOUT_SECONDS = 30L

    const val ANTHROPIC_VERSION = "2023-06-01"

    /** Maximum model → tool → model round trips for a single user message. */
    const val MAX_AGENT_ROUNDS = 6

    /** Largest file the agent may read in one call (chunked via offset/limit). */
    const val MAX_TOOL_READ_BYTES = 240_000

    /** Largest tool output forwarded back to the model. */
    const val MAX_TOOL_OUTPUT_CHARS = 24_000

    /** Largest file the workspace screen will display/edit. */
    const val MAX_EDITOR_BYTES = 1_200_000

    /** Rounds of the agent loop when the model keeps calling tools. */
    const val DEFAULT_AGENT_MAX_ROUNDS = 8
}
