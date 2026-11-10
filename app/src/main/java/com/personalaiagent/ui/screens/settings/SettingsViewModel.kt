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
import com.personalaiagent.domain.usecase.agent.Skill
import com.personalaiagent.domain.usecase.agent.SkillSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val defaultModel: AiModel? = null,
    val modelCount: Int = 0,
    val providerCount: Int = 0,
    /** Bundled + user skills; the agent loads them on demand. */
    val skills: List<Skill> = emptyList(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val modelRepository: ModelRepository,
    providerRepository: ProviderRepository,
    private val skills: SkillSource,
) : ViewModel() {

    private fun reloadSkills() {
        viewModelScope.launch {
            val installed = runCatching { withContext(Dispatchers.IO) { skills.all() } }
                .getOrDefault(emptyList())
            _uiState.update { it.copy(skills = installed) }
        }
    }

    fun addSkill(name: String, description: String, body: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { skills.add(name, description, body) }
            if (result.isSuccess) reloadSkills()
        }
    }

    fun deleteSkill(name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { skills.delete(name) }
            reloadSkills()
        }
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        reloadSkills()
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

    fun setAgentFileToolsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAgentFileToolsEnabled(enabled) }
    }

    fun setReviewRequests(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReviewRequests(enabled) }
    }

    fun setShowAgentActivity(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowAgentActivity(enabled) }
    }
}
