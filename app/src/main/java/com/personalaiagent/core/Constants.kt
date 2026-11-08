package com.personalaiagent.core

object AppConstants {
    const val DATABASE_NAME = "personal_ai_agent.db"
    const val SECURE_PREFS_NAME = "pai_secure_keys"
    const val DATASTORE_NAME = "pai_settings"

    /** Maximum number of previous turns sent back to the model. */
    const val MAX_CONTEXT_MESSAGES = 30

    /** Backups produced by this app version. */
    const val BACKUP_SCHEMA_VERSION = 1

    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 90L
    const val WRITE_TIMEOUT_SECONDS = 30L

    const val ANTHROPIC_VERSION = "2023-06-01"

    /** Maximum model → tool → model round trips for a single user message. */
    const val MAX_AGENT_ROUNDS = 6

    /** Largest file the agent may read in one call. */
    const val MAX_TOOL_READ_BYTES = 60_000

    /** Largest tool output forwarded back to the model. */
    const val MAX_TOOL_OUTPUT_CHARS = 8_000

    /** Largest file the workspace screen will display. */
    const val MAX_EDITOR_BYTES = 400_000
}
