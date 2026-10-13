package com.amrstudio.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesKeys
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import androidx.datastore.rxjava3.RxDataStore
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.tasks.await

object PreferencesKeys {
    // Theme
    val THEME_MODE = stringPreferencesKey("theme_mode") // "light", "dark", "system"
    val COLOR_SCHEME = stringPreferencesKey("color_scheme") // "ocean", "violet", "forest", "light"

    // Language
    val LANGUAGE = stringPreferencesKey("language") // "ar", "en"

    // User session
    val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
    val IS_LOGGED_IN = stringPreferencesKey("is_logged_in", "false")
    val DEVELOPER_MODE = stringPreferencesKey("developer_mode", "false")

    // Notifications
    val NOTIFICATIONS_ENABLED = stringPreferencesKey("notifications_enabled", "true")
    val CHAT_COMPLETE_NOTIFICATION = stringPreferencesKey("chat_complete_notification", "true")
    val TASK_COMPLETE_NOTIFICATION = stringPreferencesKey("task_complete_notification", "true")
    val DOWNLOAD_COMPLETE_NOTIFICATION = stringPreferencesKey("download_complete_notification", "true")

    // Offline mode
    val OFFLINE_MODE = stringPreferencesKey("offline_mode", "false")

    // Analytics
    val ANALYTICS_ENABLED = stringPreferencesKey("analytics_enabled", "false")

    // Onboarding
    val ONBOARDING_COMPLETED = stringPreferencesKey("onboarding_completed", "false")

    // Chat settings
    val AUTO_SCROLL = stringPreferencesKey("auto_scroll", "true")
    val SHOW_TIMESTAMPS = stringPreferencesKey("show_timestamps", "true")
    val CODE_THEME = stringPreferencesKey("code_theme", "github-dark")

    // Network
    val REQUEST_TIMEOUT = longPreferencesKey("request_timeout", 30000L)
    val MAX_RETRIES = intPreferencesKey("max_retries", 3)
}

class PreferenceManager(private val dataStore: RxDataStore<Preferences>) {
    companion object {
        fun create(context: Context): PreferenceManager {
            val dataStore = RxPreferenceDataStoreBuilder(context, "amr_studio_preferences").build()
            return PreferenceManager(dataStore)
        }
    }

    // Theme
    val themeMode: Flowable<String> = dataStore.data
        .map { it[PreferencesKeys.THEME_MODE] ?: "system" }
        .distinctUntilChanged()

    val colorScheme: Flowable<String> = dataStore.data
        .map { it[PreferencesKeys.COLOR_SCHEME] ?: "ocean" }
        .distinctUntilChanged()

    // Language
    val language: Flowable<String> = dataStore.data
        .map { it[PreferencesKeys.LANGUAGE] ?: "ar" }
        .distinctUntilChanged()

    // User session
    val currentUserId: Flowable<String?> = dataStore.data
        .map { it[PreferencesKeys.CURRENT_USER_ID] }
        .distinctUntilChanged()

    val isLoggedIn: Flowable<Boolean> = dataStore.data
        .map { it[PreferencesKeys.IS_LOGGED_IN] == "true" }
        .distinctUntilChanged()

    val developerMode: Flowable<Boolean> = dataStore.data
        .map { it[PreferencesKeys.DEVELOPER_MODE] == "true" }
        .distinctUntilChanged()

    // Notifications
    val notificationsEnabled: Flowable<Boolean> = dataStore.data
        .map { it[PreferencesKeys.NOTIFICATIONS_ENABLED] == "true" }
        .distinctUntilChanged()

    // Offline mode
    val offlineMode: Flowable<Boolean> = dataStore.data
        .map { it[PreferencesKeys.OFFLINE_MODE] == "true" }
        .distinctUntilChanged()

    // Analytics
    val analyticsEnabled: Flowable<Boolean> = dataStore.data
        .map { it[PreferencesKeys.ANALYTICS_ENABLED] == "true" }
        .distinctUntilChanged()

    // Onboarding
    val onboardingCompleted: Flowable<Boolean> = dataStore.data
        .map { it[PreferencesKeys.ONBOARDING_COMPLETED] == "true" }
        .distinctUntilChanged()

    // Chat settings
    val autoScroll: Flowable<Boolean> = dataStore.data
        .map { it[PreferencesKeys.AUTO_SCROLL] == "true" }
        .distinctUntilChanged()

    val showTimestamps: Flowable<Boolean> = dataStore.data
        .map { it[PreferencesKeys.SHOW_TIMESTAMPS] == "true" }
        .distinctUntilChanged()

    val codeTheme: Flowable<String> = dataStore.data
        .map { it[PreferencesKeys.CODE_THEME] ?: "github-dark" }
        .distinctUntilChanged()

    // Network
    val requestTimeout: Flowable<Long> = dataStore.data
        .map { it[PreferencesKeys.REQUEST_TIMEOUT] ?: 30000L }
        .distinctUntilChanged()

    val maxRetries: Flowable<Int> = dataStore.data
        .map { it[PreferencesKeys.MAX_RETRIES] ?: 3 }
        .distinctUntilChanged()

    // Setters
    suspend fun setThemeMode(mode: String) = dataStore.updateData { it[PreferencesKeys.THEME_MODE] = mode }.await()
    suspend fun setColorScheme(scheme: String) = dataStore.updateData { it[PreferencesKeys.COLOR_SCHEME] = scheme }.await()
    suspend fun setLanguage(lang: String) = dataStore.updateData { it[PreferencesKeys.LANGUAGE] = lang }.await()
    suspend fun setCurrentUserId(userId: String?) = dataStore.updateData { it[PreferencesKeys.CURRENT_USER_ID] = userId }.await()
    suspend fun setLoggedIn(loggedIn: Boolean) = dataStore.updateData { it[PreferencesKeys.IS_LOGGED_IN] = loggedIn.toString() }.await()
    suspend fun setDeveloperMode(enabled: Boolean) = dataStore.updateData { it[PreferencesKeys.DEVELOPER_MODE] = enabled.toString() }.await()
    suspend fun setNotificationsEnabled(enabled: Boolean) = dataStore.updateData { it[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled.toString() }.await()
    suspend fun setOfflineMode(enabled: Boolean) = dataStore.updateData { it[PreferencesKeys.OFFLINE_MODE] = enabled.toString() }.await()
    suspend fun setAnalyticsEnabled(enabled: Boolean) = dataStore.updateData { it[PreferencesKeys.ANALYTICS_ENABLED] = enabled.toString() }.await()
    suspend fun setOnboardingCompleted(completed: Boolean) = dataStore.updateData { it[PreferencesKeys.ONBOARDING_COMPLETED] = completed.toString() }.await()
    suspend fun setAutoScroll(enabled: Boolean) = dataStore.updateData { it[PreferencesKeys.AUTO_SCROLL] = enabled.toString() }.await()
    suspend fun setShowTimestamps(enabled: Boolean) = dataStore.updateData { it[PreferencesKeys.SHOW_TIMESTAMPS] = enabled.toString() }.await()
    suspend fun setCodeTheme(theme: String) = dataStore.updateData { it[PreferencesKeys.CODE_THEME] = theme }.await()
    suspend fun setRequestTimeout(timeout: Long) = dataStore.updateData { it[PreferencesKeys.REQUEST_TIMEOUT] = timeout }.await()
    suspend fun setMaxRetries(retries: Int) = dataStore.updateData { it[PreferencesKeys.MAX_RETRIES] = retries }.await()
}