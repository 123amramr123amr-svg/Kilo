package com.amrstudio.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amrstudio.data.preferences.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    application: Application
) : ViewModel() {

    val themeMode = preferenceManager.themeMode
    val colorScheme = preferenceManager.colorScheme
    val language = preferenceManager.language
    val notificationsEnabled = preferenceManager.notificationsEnabled
    val offlineMode = preferenceManager.offlineMode
    val analyticsEnabled = preferenceManager.analyticsEnabled
    val autoScroll = preferenceManager.autoScroll
    val showTimestamps = preferenceManager.showTimestamps
    val codeTheme = preferenceManager.codeTheme

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading.asStateFlow()

    fun setThemeMode(mode: String) {
        viewModelScope.launch { preferenceManager.setThemeMode(mode) }
    }

    fun setColorScheme(scheme: String) {
        viewModelScope.launch { preferenceManager.setColorScheme(scheme) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { preferenceManager.setLanguage(lang) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.setNotificationsEnabled(enabled) }
    }

    fun setOfflineMode(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.setOfflineMode(enabled) }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.setAnalyticsEnabled(enabled) }
    }

    fun setAutoScroll(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.setAutoScroll(enabled) }
    }

    fun setShowTimestamps(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.setShowTimestamps(enabled) }
    }

    fun setCodeTheme(theme: String) {
        viewModelScope.launch { preferenceManager.setCodeTheme(theme) }
    }

    fun exportData(): String {
        // Placeholder for data export
        return "{}"
    }

    fun importData(json: String): Boolean {
        // Placeholder for data import
        return true
    }

    fun clearAllData() {
        // Placeholder for clearing all local data
    }
}