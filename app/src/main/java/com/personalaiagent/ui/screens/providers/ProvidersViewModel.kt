package com.personalaiagent.ui.screens.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.core.AppError
import com.personalaiagent.core.fold
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.repository.ModelRepository
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

data class ProvidersUiState(
    val isLoading: Boolean = true,
    val providers: List<Provider> = emptyList(),
    val busyProviderId: String? = null,
)

sealed interface ProvidersEvent {
    data object ProviderDeleted : ProvidersEvent
    data object DefaultChanged : ProvidersEvent
    data object CatalogSynced : ProvidersEvent
    data class Error(val error: AppError) : ProvidersEvent
}

@HiltViewModel
class ProvidersViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProvidersUiState())
    val uiState: StateFlow<ProvidersUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<ProvidersEvent>(Channel.BUFFERED)
    val events: Flow<ProvidersEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            providerRepository.observeProviders().collect { providers ->
                _uiState.update {
                    it.copy(isLoading = false, providers = providers)
                }
            }
        }
    }

    fun setDefault(providerId: String) {
        viewModelScope.launch {
            providerRepository.setDefaultProvider(providerId).fold(
                onSuccess = { eventChannel.send(ProvidersEvent.DefaultChanged) },
                onFailure = { eventChannel.send(ProvidersEvent.Error(it)) },
            )
        }
    }

    fun refreshCatalog(providerId: String) {
        if (_uiState.value.busyProviderId != null) return
        _uiState.update { it.copy(busyProviderId = providerId) }
        viewModelScope.launch {
            val result = modelRepository.refreshCatalog(providerId)
            _uiState.update { it.copy(busyProviderId = null) }
            result.fold(
                onSuccess = { eventChannel.send(ProvidersEvent.CatalogSynced) },
                onFailure = { eventChannel.send(ProvidersEvent.Error(it)) },
            )
        }
    }

    fun deleteProvider(providerId: String) {
        viewModelScope.launch {
            providerRepository.deleteProvider(providerId).fold(
                onSuccess = { eventChannel.send(ProvidersEvent.ProviderDeleted) },
                onFailure = { eventChannel.send(ProvidersEvent.Error(it)) },
            )
        }
    }
}
