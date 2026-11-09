package com.personalaiagent.ui.screens.providers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.core.AppError
import com.personalaiagent.core.fold
import com.personalaiagent.domain.model.ProviderDraft
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.model.RemoteModel
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
    val isFetchingModels: Boolean = false,
    val fetchedModels: List<RemoteModel> = emptyList(),
    val selectedModelIds: Set<String> = emptySet(),
    val showModelsDialog: Boolean = false,
    val fetchError: AppError? = null,
    val appliedModels: Int? = null,
    /** Enabled models after the last apply, shown as a confirmation banner. */
    val enabledCount: Int = 0,
) {
    val isEditing: Boolean get() = providerId != null
    val requiresKey: Boolean get() = !isEditing || !hasStoredKey
}

sealed interface ProviderFormEvent {
    data object Saved : ProviderFormEvent
    data class ModelsApplied(val count: Int) : ProviderFormEvent
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
                        current.copy(isLoading = false, formError = AppError.NotFound)
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
                // Known models from the fetch button are stored as-is: no second
                // network round trip that could fail after a successful fetch.
                addProvider(draft(), state.apiKey, state.fetchedModels).fold(
                    onSuccess = { created ->
                        if (state.fetchedModels.isNotEmpty()) {
                            modelRepository.storeCatalog(
                                created.providerId,
                                state.fetchedModels,
                                state.selectedModelIds.ifEmpty {
                                    state.fetchedModels.map { it.remoteId }.toSet()
                                },
                            )
                        }
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
                        if (state.fetchedModels.isNotEmpty()) {
                            modelRepository.storeCatalog(
                                state.providerId,
                                state.fetchedModels,
                                state.selectedModelIds.ifEmpty {
                                    state.fetchedModels.map { it.remoteId }.toSet()
                                },
                            )
                        } else if (state.apiKey.isNotBlank()) {
                            // Credentials changed but no catalog on screen: ask the provider.
                            val sync = modelRepository.refreshCatalog(state.providerId)
                            if (sync is com.personalaiagent.core.Outcome.Failure) {
                                _uiState.update { current -> current.copy(fetchError = sync.error) }
                            }
                        }
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
        _uiState.update { it.copy(testError = null, testSuccessCount = null, fetchError = null) }
    }

    // ------------------------------------------------------------ fetch models

    /** Loads the model names from the provider and opens the selection dialog. */
    fun fetchModels() {
        val state = _uiState.value
        if (state.isFetchingModels) return

        ProviderValidator.validate(draft())?.let { error ->
            _uiState.update { it.copy(formError = error, fetchError = null) }
            return
        }
        if (state.requiresKey && state.apiKey.isBlank()) {
            _uiState.update { it.copy(formError = AppError.InvalidInput("key")) }
            return
        }

        _uiState.update {
            it.copy(isFetchingModels = true, fetchError = null, formError = null, appliedModels = null)
        }
        viewModelScope.launch {
            val result = providerRepository.fetchAvailableModels(
                providerId = state.providerId,
                draft = draft(),
                apiKey = state.apiKey.takeIf { it.isNotBlank() },
            )
            _uiState.update { current ->
                when (result) {
                    is com.personalaiagent.core.Outcome.Success -> current.copy(
                        isFetchingModels = false,
                        fetchedModels = result.value,
                        // Keep what the user already ticked, but only for ids that
                        // this provider actually returned; default to everything.
                        selectedModelIds = result.value.map { it.remoteId }.toSet()
                            .let { available ->
                                current.selectedModelIds.intersect(available).ifEmpty { available }
                            },
                        showModelsDialog = result.value.isNotEmpty(),
                        fetchError = if (result.value.isEmpty()) AppError.NoModel else null,
                        testSuccessCount = result.value.size,
                    )

                    is com.personalaiagent.core.Outcome.Failure -> current.copy(
                        isFetchingModels = false,
                        fetchError = result.error,
                    )
                }
            }
        }
    }

    fun toggleModelSelection(remoteId: String) {
        _uiState.update { current ->
            val selected = current.selectedModelIds.toMutableSet()
            if (!selected.add(remoteId)) selected.remove(remoteId)
            current.copy(selectedModelIds = selected)
        }
    }

    fun selectAllModels(selectAll: Boolean) {
        _uiState.update { current ->
            current.copy(
                selectedModelIds = if (selectAll) current.fetchedModels.map { it.remoteId }.toSet() else emptySet(),
            )
        }
    }

    fun dismissModelsDialog() {
        _uiState.update { it.copy(showModelsDialog = false) }
    }

    fun clearFetchError() {
        _uiState.update { it.copy(fetchError = null) }
    }

    fun clearFormError() {
        _uiState.update { it.copy(formError = null) }
    }

    /** Re-opens the selection list without hitting the network again. */
    fun showModelsDialogAction() {
        _uiState.update { current ->
            if (current.fetchedModels.isEmpty()) current else current.copy(showModelsDialog = true)
        }
    }

    /**
     * Applies the ticked models right away for an existing provider. For a new
     * provider the selection is kept and applied when the provider is saved.
     */
    fun applyModelSelection() {
        val state = _uiState.value
        if (state.fetchedModels.isEmpty() || state.isFetchingModels) return

        // Validate what is on screen: these exact values are what we are about to save.
        ProviderValidator.validate(draft())?.let { error ->
            _uiState.update { it.copy(formError = error) }
            return
        }
        if (state.requiresKey && state.apiKey.isBlank()) {
            _uiState.update { it.copy(formError = AppError.InvalidInput("key")) }
            return
        }

        val selection = state.selectedModelIds.ifEmpty {
            state.fetchedModels.map { it.remoteId }.toSet()
        }
        val providerId = state.providerId
        if (providerId == null) {
            // New provider: keep the ticks, they are applied when the provider is saved.
            _uiState.update {
                it.copy(
                    showModelsDialog = false,
                    appliedModels = selection.size,
                    enabledCount = selection.size,
                )
            }
            return
        }

        _uiState.update { it.copy(isFetchingModels = true, fetchError = null, formError = null) }
        viewModelScope.launch {
            // 1. Persist the form first (URL / kind / key). Without this the catalog
            //    would be attached to an endpoint the app cannot actually call.
            val saved = providerRepository.updateProvider(
                id = providerId,
                draft = draft(),
                apiKey = state.apiKey.takeIf { it.isNotBlank() },
            )
            if (saved is com.personalaiagent.core.Outcome.Failure) {
                _uiState.update {
                    it.copy(isFetchingModels = false, appliedModels = null, formError = saved.error)
                }
                return@launch
            }

            // 2. Store the catalog that is already on screen — no second request.
            when (val stored = modelRepository.storeCatalog(providerId, state.fetchedModels, selection)) {
                is com.personalaiagent.core.Outcome.Failure -> _uiState.update {
                    it.copy(isFetchingModels = false, appliedModels = null, fetchError = stored.error)
                }

                is com.personalaiagent.core.Outcome.Success -> {
                    _uiState.update {
                        it.copy(
                            isFetchingModels = false,
                            showModelsDialog = false,
                            apiKey = "",
                            hasStoredKey = true,
                            appliedModels = selection.size,
                            enabledCount = selection.size,
                            testSuccessCount = state.fetchedModels.size,
                        )
                    }
                    eventChannel.send(ProviderFormEvent.ModelsApplied(selection.size))
                }
            }
        }
    }
}
