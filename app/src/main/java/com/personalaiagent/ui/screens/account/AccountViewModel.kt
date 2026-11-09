package com.personalaiagent.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.domain.model.AppSettings
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import com.personalaiagent.domain.repository.SettingsRepository
import com.personalaiagent.domain.usecase.ClearLocalDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val providerCount: Int = 0,
    val modelCount: Int = 0,
    val isSigningOut: Boolean = false,
)

sealed interface AccountEvent {
    data object SignedOut : AccountEvent
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    providerRepository: ProviderRepository,
    modelRepository: ModelRepository,
    private val clearLocalData: ClearLocalDataUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<AccountEvent>(Channel.BUFFERED)
    val events: Flow<AccountEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                providerRepository.observeProviders(),
                modelRepository.observeModels(),
            ) { settings, providers, models ->
                Triple(settings, providers, models)
            }.collect { (settings, providers, models) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        settings = settings,
                        providerCount = providers.size,
                        modelCount = models.size,
                    )
                }
            }
        }
    }

    fun signOut() {
        if (_uiState.value.isSigningOut) return
        _uiState.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            clearLocalData()
            _uiState.update { it.copy(isSigningOut = false) }
            eventChannel.send(AccountEvent.SignedOut)
        }
    }
}
