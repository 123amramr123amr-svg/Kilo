package com.personalaiagent.ui.screens.providers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.core.AppError
import com.personalaiagent.core.fold
import com.personalaiagent.domain.model.ProviderDraft
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import com.personalaiagent.domain.usecase.AddProviderUseCase
import com.personalaiagent.domain.usecase.ProviderValidator
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

data class ProviderPreset(
    val labelRes: Int,
    val kind: ProviderKind,
    val name: String,
    val baseUrl: String,
)

data class ProviderFormUiState(
    val isLoading: Boolean = true,
    val providerId: String? = null,
    val name: String = "",
    val kind: ProviderKind = ProviderKind.OPENAI_COMPATIBLE,
    val baseUrl: String = ProviderKind.OPENAI_COMPATIBLE.defaultBaseUrl,
    val apiKey: String = "",
    val hasStoredKey: Boolean = false,
    val isTesting: Boolean = false,
    val isSaving: Boolean = false,
    val testSuccessCount: Int? = null,
    val testError: AppError? = null,
    val formError: AppError? = null,
) {
    val isEditing: Boolean get() = providerId != null
    val requiresKey: Boolean get() = !isEditing || !hasStoredKey
}

sealed interface ProviderFormEvent {
    data object Saved : ProviderFormEvent
}

@HiltViewModel
class ProviderEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
    private val addProvider: AddProviderUseCase,
) : ViewModel() {

    private val providerId: String? = savedStateHandle.get<String>("providerId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(ProviderFormUiState(isLoading = providerId != null, providerId = providerId))
    val uiState: StateFlow<ProviderFormUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<ProviderFormEvent>(Channel.BUFFERED)
    val events: Flow<ProviderFormEvent> = eventChannel.receiveAsFlow()

    init {
        if (providerId != null) {
            viewModelScope.launch {
                val provider = providerRepository.getProvider(providerId)
                _uiState.update { current ->
                    if (provider == null) {
                        current.copy(isLoading = false, formError = AppError.Unknown("provider not found"))
                    } else {
                        current.copy(
                            isLoading = false,
                            name = provider.name,
                            kind = provider.kind,
                            baseUrl = provider.baseUrl,
                            hasStoredKey = provider.hasKey,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, formError = null) }
    }

    fun onBaseUrlChange(value: String) {
        _uiState.update { it.copy(baseUrl = value, formError = null) }
    }

    fun onApiKeyChange(value: String) {
        // The key only ever lives in this transient state; it is not persisted until save.
        _uiState.update { it.copy(apiKey = value, formError = null) }
    }

    fun onKindChange(kind: ProviderKind) {
        _uiState.update { current ->
            val previousDefault = current.kind.defaultBaseUrl
            val nextBaseUrl = if (current.baseUrl.isBlank() || current.baseUrl == previousDefault) {
                kind.defaultBaseUrl
            } else {
                current.baseUrl
            }
            current.copy(kind = kind, baseUrl = nextBaseUrl, testError = null, testSuccessCount = null)
        }
    }

    fun applyPreset(preset: ProviderPreset) {
        _uiState.update { current ->
            current.copy(
                kind = preset.kind,
                baseUrl = preset.baseUrl,
                name = if (current.name.isBlank()) preset.name else current.name,
                testError = null,
                testSuccessCount = null,
            )
        }
    }

    private fun draft(): ProviderDraft = ProviderDraft(
        name = _uiState.value.name.trim(),
        kind = _uiState.value.kind,
        baseUrl = _uiState.value.baseUrl,
    )

    fun testConnection() {
        val state = _uiState.value
        if (state.isTesting) return
        ProviderValidator.validate(draft())?.let {
            _uiState.update { current -> current.copy(formError = it) }
            return
        }
        if (state.requiresKey && state.apiKey.isBlank()) {
            _uiState.update { current -> current.copy(formError = AppError.InvalidInput("key")) }
            return
        }
        _uiState.update { it.copy(isTesting = true, testError = null, testSuccessCount = null, formError = null) }
        viewModelScope.launch {
            val result = providerRepository.testConnection(
                providerId = state.providerId,
                draft = draft(),
                apiKey = state.apiKey.takeIf { it.isNotBlank() },
            )
            _uiState.update { current ->
                current.copy(
                    isTesting = false,
                    testSuccessCount = result.getOrNull()?.modelsFound,
                    testError = (result as? com.personalaiagent.core.Outcome.Failure)?.error,
                )
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        ProviderValidator.validate(draft())?.let {
            _uiState.update { current -> current.copy(formError = it) }
            return
        }
        if (state.requiresKey && state.apiKey.isBlank()) {
            _uiState.update { current -> current.copy(formError = AppError.InvalidInput("key")) }
            return
        }

        _uiState.update { it.copy(isSaving = true, formError = null) }
        viewModelScope.launch {
            if (state.providerId == null) {
                addProvider(draft(), state.apiKey).fold(
                    onSuccess = {
                        _uiState.update { current -> current.copy(isSaving = false, apiKey = "") }
                        eventChannel.send(ProviderFormEvent.Saved)
                    },
                    onFailure = { error ->
                        _uiState.update { current ->
                            current.copy(isSaving = false, formError = error, apiKey = "")
                        }
                    },
                )
            } else {
                providerRepository.updateProvider(
                    id = state.providerId,
                    draft = draft(),
                    apiKey = state.apiKey.takeIf { it.isNotBlank() },
                ).fold(
                    onSuccess = {
                        if (state.apiKey.isNotBlank()) modelRepository.refreshCatalog(state.providerId)
                        _uiState.update { current -> current.copy(isSaving = false, apiKey = "") }
                        eventChannel.send(ProviderFormEvent.Saved)
                    },
                    onFailure = { error ->
                        _uiState.update { current -> current.copy(isSaving = false, formError = error) }
                    },
                )
            }
        }
    }

    fun clearTestResult() {
        _uiState.update { it.copy(testError = null, testSuccessCount = null) }
    }
}
