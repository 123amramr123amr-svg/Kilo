package com.personalaiagent.domain.model

data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val theme: ThemeMode = ThemeMode.MIDNIGHT,
    val ttsEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val streamingEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val displayName: String = "",
    val createdAt: Long = 0L,
)
