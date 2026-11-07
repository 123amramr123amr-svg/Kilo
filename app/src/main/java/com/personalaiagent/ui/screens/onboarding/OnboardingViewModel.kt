package com.personalaiagent.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.domain.model.AppLanguage
import com.personalaiagent.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val isSaving: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(language = settings.language) }
            }
        }
    }

    fun selectLanguage(language: AppLanguage) {
        if (_uiState.value.language == language) return
        _uiState.update { it.copy(language = language) }
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun complete() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
