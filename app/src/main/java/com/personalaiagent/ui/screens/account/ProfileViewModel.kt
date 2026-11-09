package com.personalaiagent.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.domain.model.AppSettings
import com.personalaiagent.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val nameDraft: String = "",
    val isSaving: Boolean = false,
    val nameError: Boolean = false,
) {
    val isDirty: Boolean get() = nameDraft.trim() != settings.displayName
}

sealed interface ProfileEvent {
    data object Saved : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<ProfileEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        settings = settings,
                        nameDraft = if (current.isDirty) current.nameDraft else settings.displayName,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(nameDraft = value, nameError = false) }
    }

    fun save() {
        val state = _uiState.value
        if (state.nameDraft.isBlank()) {
            _uiState.update { it.copy(nameError = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            settingsRepository.setDisplayName(state.nameDraft)
            _uiState.update { it.copy(isSaving = false) }
            eventChannel.send(ProfileEvent.Saved)
        }
    }
}
