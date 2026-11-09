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

    fun modelSettings(modelId: String) = "model_settings/$modelId"
    fun providerEdit(providerId: String?) =
        if (providerId == null) "provider_edit" else "provider_edit?providerId=$providerId"
    fun taskDetail(taskId: String) = "task_detail/$taskId"

    val topLevel = setOf(CHAT, MODELS, SETTINGS)
}
