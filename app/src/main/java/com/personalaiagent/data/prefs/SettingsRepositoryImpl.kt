package com.personalaiagent.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.personalaiagent.domain.model.AppLanguage
import com.personalaiagent.domain.model.AppSettings
import com.personalaiagent.domain.model.ThemeMode
import com.personalaiagent.domain.repository.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val localeMirror: com.personalaiagent.platform.LocaleMirror,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { prefs -> prefs.toSettings() }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setLanguage(language: AppLanguage) {
        localeMirror.write(language.tag)
        dataStore.edit { it[KEY_LANGUAGE] = language.tag }
    }

    override suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = theme.name }
    }

    override suspend fun setTtsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_TTS] = enabled }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_HAPTICS] = enabled }
    }

    override suspend fun setStreamingEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_STREAMING] = enabled }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING] = completed
            if (prefs[KEY_CREATED_AT] == null) prefs[KEY_CREATED_AT] = System.currentTimeMillis()
        }
    }

    override suspend fun setDisplayName(name: String) {
        dataStore.edit { it[KEY_DISPLAY_NAME] = name.trim() }
    }

    override suspend fun setWorkspace(uri: String?, displayName: String?) {
        dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(KEY_WORKSPACE_URI)
                prefs.remove(KEY_WORKSPACE_NAME)
            } else {
                prefs[KEY_WORKSPACE_URI] = uri
                prefs[KEY_WORKSPACE_NAME] = displayName ?: ""
            }
        }
    }

    override suspend fun setAgentFileToolsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AGENT_TOOLS] = enabled }
    }

    override suspend fun clearAll() {
        localeMirror.clear()
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        // Before the user picks a language, follow the device locale so the very
        // first launch is already in Arabic on an Arabic phone.
        language = AppLanguage.fromTag(this[KEY_LANGUAGE] ?: systemLanguageTag()),
        theme = runCatching { ThemeMode.valueOf(this[KEY_THEME] ?: ThemeMode.MIDNIGHT.name) }
            .getOrDefault(ThemeMode.MIDNIGHT),
        ttsEnabled = this[KEY_TTS] ?: false,
        hapticsEnabled = this[KEY_HAPTICS] ?: true,
        streamingEnabled = this[KEY_STREAMING] ?: true,
        onboardingCompleted = this[KEY_ONBOARDING] ?: false,
        displayName = this[KEY_DISPLAY_NAME] ?: "",
        createdAt = this[KEY_CREATED_AT] ?: 0L,
        workspaceUri = this[KEY_WORKSPACE_URI],
        workspaceName = this[KEY_WORKSPACE_NAME],
        agentFileToolsEnabled = this[KEY_AGENT_TOOLS] ?: true,
    )

    private fun systemLanguageTag(): String =
        java.util.Locale.getDefault().language.ifBlank { AppLanguage.ENGLISH.tag }

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_TTS = booleanPreferencesKey("tts_enabled")
        val KEY_HAPTICS = booleanPreferencesKey("haptics_enabled")
        val KEY_STREAMING = booleanPreferencesKey("streaming_enabled")
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        val KEY_CREATED_AT = longPreferencesKey("profile_created_at")
        val KEY_WORKSPACE_URI = stringPreferencesKey("workspace_uri")
        val KEY_WORKSPACE_NAME = stringPreferencesKey("workspace_name")
        val KEY_AGENT_TOOLS = booleanPreferencesKey("agent_file_tools")
    }
}
