package com.personalaiagent.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val CHAT = "chat"
    const val MODELS = "models"
    const val MODEL_SETTINGS = "model_settings/{modelId}"
    const val PROVIDERS = "providers"
    const val PROVIDER_EDIT = "provider_edit?providerId={providerId}"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val PROFILE = "profile"
    const val PRIVACY = "privacy"
    const val API_KEYS = "api_keys"
    const val BACKUP = "backup"
    const val PERMISSIONS = "permissions"
    const val TASKS = "tasks"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val VOICE = "voice"
    const val WORKSPACE = "workspace"

    fun modelSettings(modelId: String) = "model_settings/${encode(modelId)}"

    /**
     * `null` must mean "add a new provider". Passing the [PROVIDER_EDIT] pattern itself
     * used to fill `providerId` with the literal `{providerId}`, which opened the edit
     * form for a provider that does not exist, so every save failed with
     * "provider not found".
     */
    fun providerEdit(providerId: String?) =
        if (providerId.isNullOrBlank()) "provider_edit" else "provider_edit?providerId=${encode(providerId)}"

    fun taskDetail(taskId: String) = "task_detail/${encode(taskId)}"

    private const val HEX = "0123456789ABCDEF"

    /** Percent-encodes everything except unreserved characters, so ids never break a route. */
    internal fun encode(value: String): String = buildString {
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            val code = byte.toInt() and 0xFF
            val unreserved = code in 0x30..0x39 || code in 0x41..0x5A || code in 0x61..0x7A ||
                code == 0x2D || code == 0x2E || code == 0x5F || code == 0x7E
            if (unreserved) {
                append(code.toChar())
            } else {
                append('%').append(HEX[code shr 4]).append(HEX[code and 0x0F])
            }
        }
    }

    val topLevel = setOf(CHAT, MODELS, SETTINGS)
}
