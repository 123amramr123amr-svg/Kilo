package com.personalaiagent.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.domain.model.AiModel
import com.personalaiagent.domain.model.AppLanguage
import com.personalaiagent.domain.model.AppSettings
import com.personalaiagent.domain.model.ThemeMode
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import com.personalaiagent.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val defaultModel: AiModel? = null,
    val modelCount: Int = 0,
    val providerCount: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val modelRepository: ModelRepository,
    providerRepository: ProviderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                modelRepository.observeEnabledModels(),
                providerRepository.observeProviders(),
            ) { settings, models, providers ->
                Triple(settings, models, providers)
            }.collect { (settings, models, providers) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        settings = settings,
                        defaultModel = models.firstOrNull { model -> model.isDefault } ?: models.firstOrNull(),
                        modelCount = models.size,
                        providerCount = providers.size,
                    )
                }
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTtsEnabled(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticsEnabled(enabled) }
    }

    fun setStreamingEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setStreamingEnabled(enabled) }
    }
}
