package com.personalaiagent.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.core.AppError
import com.personalaiagent.data.secure.SecureStorageException
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.repository.CredentialStore
import com.personalaiagent.domain.repository.ProviderRepository
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

data class ApiKeysUiState(
    val isLoading: Boolean = true,
    val providers: List<Provider> = emptyList(),
    val dialogProvider: Provider? = null,
    val keyDraft: String = "",
    val isSaving: Boolean = false,
    val deleteTarget: Provider? = null,
    val error: AppError? = null,
)

sealed interface ApiKeysEvent {
    data object KeySaved : ApiKeysEvent
    data object KeyDeleted : ApiKeysEvent
}

@HiltViewModel
class ApiKeysViewModel @Inject constructor(
    providerRepository: ProviderRepository,
    private val credentialStore: CredentialStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiKeysUiState())
    val uiState: StateFlow<ApiKeysUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<ApiKeysEvent>(Channel.BUFFERED)
    val events: Flow<ApiKeysEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            providerRepository.observeProviders().collect { providers ->
                _uiState.update { it.copy(isLoading = false, providers = providers) }
            }
        }
    }

    fun openDialog(provider: Provider) {
        _uiState.update { it.copy(dialogProvider = provider, keyDraft = "", error = null) }
    }

    fun closeDialog() {
        _uiState.update { it.copy(dialogProvider = null, keyDraft = "") }
    }

    fun onKeyChange(value: String) {
        _uiState.update { it.copy(keyDraft = value) }
    }

    fun saveKey() {
        val state = _uiState.value
        val provider = state.dialogProvider ?: return
        if (state.keyDraft.isBlank()) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                credentialStore.save(provider.id, state.keyDraft.trim())
                _uiState.update {
                    it.copy(isSaving = false, dialogProvider = null, keyDraft = "")
                }
                eventChannel.send(ApiKeysEvent.KeySaved)
            } catch (_: SecureStorageException) {
                _uiState.update { it.copy(isSaving = false, error = AppError.SecureStorage) }
            }
        }
    }

    fun requestDelete(provider: Provider) {
        _uiState.update { it.copy(deleteTarget = provider) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val provider = _uiState.value.deleteTarget ?: return
        _uiState.update { it.copy(deleteTarget = null) }
        viewModelScope.launch {
            credentialStore.delete(provider.id)
            eventChannel.send(ApiKeysEvent.KeyDeleted)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
