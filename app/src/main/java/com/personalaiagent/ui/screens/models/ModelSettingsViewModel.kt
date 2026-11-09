package com.personalaiagent.ui.screens.models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.domain.model.AiModel
import com.personalaiagent.domain.model.ModelParams
import com.personalaiagent.domain.repository.ModelRepository
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

data class ModelSettingsUiState(
    val isLoading: Boolean = true,
    val model: AiModel? = null,
    val params: ModelParams = ModelParams(),
    val enabled: Boolean = true,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
)

sealed interface ModelSettingsEvent {
    data object Saved : ModelSettingsEvent
}

@HiltViewModel
class ModelSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private val modelId: String = savedStateHandle.get<String>("modelId").orEmpty()

    private val _uiState = MutableStateFlow(ModelSettingsUiState())
    val uiState: StateFlow<ModelSettingsUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<ModelSettingsEvent>(Channel.BUFFERED)
    val events: Flow<ModelSettingsEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            modelRepository.observeModel(modelId).collect { model ->
                _uiState.update { current ->
                    if (current.model == null) {
                        current.copy(
                            isLoading = false,
                            model = model,
                            params = model?.params ?: ModelParams(),
                            enabled = model?.enabled ?: false,
                        )
                    } else {
                        current.copy(isLoading = false, model = model)
                    }
                }
            }
        }
    }

    fun setTemperature(value: Float) = updateParams {
        it.copy(temperature = value.coerceIn(ModelParams.MIN_TEMPERATURE, ModelParams.MAX_TEMPERATURE))
    }

    fun setMaxTokens(value: Int) = updateParams {
        it.copy(maxTokens = value.coerceIn(ModelParams.MIN_MAX_TOKENS, ModelParams.MAX_MAX_TOKENS))
    }

    fun setTopP(value: Float) = updateParams { it.copy(topP = value.coerceIn(0f, 1f)) }

    fun setSystemPrompt(value: String) = updateParams {
        it.copy(systemPrompt = value.take(ModelParams.MAX_SYSTEM_PROMPT_LENGTH))
    }

    fun setStreaming(value: Boolean) = updateParams { it.copy(streaming = value) }

    fun setEnabled(value: Boolean) {
        _uiState.update { it.copy(enabled = value, isDirty = true) }
    }

    private fun updateParams(transform: (ModelParams) -> ModelParams) {
        _uiState.update { it.copy(params = transform(it.params), isDirty = true) }
    }

    fun resetToDefaults() {
        _uiState.update { it.copy(params = ModelParams(), isDirty = true) }
    }

    fun setAsDefault() {
        viewModelScope.launch { modelRepository.setDefault(modelId) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.model == null) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            modelRepository.updateParams(modelId, state.params)
            modelRepository.setEnabled(modelId, state.enabled)
            _uiState.update { it.copy(isSaving = false, isDirty = false) }
            eventChannel.send(ModelSettingsEvent.Saved)
        }
    }
}
