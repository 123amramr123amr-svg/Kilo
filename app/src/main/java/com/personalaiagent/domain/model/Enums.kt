package com.personalaiagent.domain.model

enum class ProviderKind {
    OPENAI_COMPATIBLE,
    ANTHROPIC;

    val defaultBaseUrl: String
        get() = when (this) {
            OPENAI_COMPATIBLE -> "https://api.openai.com/v1"
            ANTHROPIC -> "https://api.anthropic.com/v1"
        }
}

enum class ProviderStatus { UNKNOWN, CONNECTED, ERROR }

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    /** Result of a tool invocation; never shown as a chat bubble. */
    TOOL;

    /** Role name used by OpenAI-compatible endpoints. */
    val wireName: String
        get() = when (this) {
            USER -> "user"
            ASSISTANT -> "assistant"
            SYSTEM -> "system"
            TOOL -> "tool"
        }
}

enum class MessageStatus { COMPLETE, STREAMING, FAILED }

enum class TaskStatus { QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED }

enum class TaskStepStatus { PENDING, RUNNING, SUCCEEDED, FAILED, SKIPPED }

/** Ordered steps recorded for every agent task. */
enum class TaskStepName {
    RESOLVE_PROVIDER,
    BUILD_PROMPT,
    SEND_REQUEST,
    RECEIVE_RESPONSE,
    PERSIST_MESSAGE,
    /** One entry per tool the agent executed against the phone workspace. */
    EXECUTE_TOOL,
}

enum class ModelCapability { TEXT, VISION, TOOLS, REASONING, CODE }

enum class AppLanguage(val tag: String) {
    ARABIC("ar"),
    ENGLISH("en");

    val isRtl: Boolean get() = this == ARABIC

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: ENGLISH
    }
}

/**
 * Visual themes. [LIGHT] is the default modern look (white cards, soft accents);
 * the two dark variants stay available for users who prefer them.
 */
enum class ThemeMode { LIGHT, MIDNIGHT, AMOLED }
