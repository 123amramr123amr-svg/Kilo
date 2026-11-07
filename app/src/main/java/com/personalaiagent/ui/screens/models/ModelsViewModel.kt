package com.personalaiagent.ui.screens.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.domain.model.AiModel
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ModelFilter { ALL, ENABLED, VISION, TOOLS }

data class ModelsUiState(
    val isLoading: Boolean = true,
    val models: List<AiModel> = emptyList(),
    val query: String = "",
    val filter: ModelFilter = ModelFilter.ALL,
    val hasProviders: Boolean = false,
    val error: String? = null,
) {
    val filtered: List<AiModel>
        get() {
            val normalized = query.trim().lowercase()
            return models.filter { model ->
                val matchesQuery = normalized.isEmpty() ||
                    model.displayName.lowercase().contains(normalized) ||
                    model.remoteId.lowercase().contains(normalized) ||
                    model.providerName.lowercase().contains(normalized)
                val matchesFilter = when (filter) {
                    ModelFilter.ALL -> true
                    ModelFilter.ENABLED -> model.enabled
                    ModelFilter.VISION -> com.personalaiagent.domain.model.ModelCapability.VISION in model.capabilities
                    ModelFilter.TOOLS -> com.personalaiagent.domain.model.ModelCapability.TOOLS in model.capabilities
                }
                matchesQuery && matchesFilter
            }
        }

    val isEmpty: Boolean get() = !isLoading && filtered.isEmpty()
}

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    providerRepository: ProviderRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(ModelFilter.ALL)

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                modelRepository.observeModels(),
                providerRepository.observeProviders(),
                query,
                filter,
            ) { models, providers, search, selectedFilter ->
                ModelsUiState(
                    isLoading = false,
                    models = models,
                    query = search,
                    filter = selectedFilter,
                    hasProviders = providers.isNotEmpty(),
                )
            }.collect { _uiState.value = it }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: ModelFilter) {
        filter.value = value
    }

    fun setEnabled(modelId: String, enabled: Boolean) {
        viewModelScope.launch { modelRepository.setEnabled(modelId, enabled) }
    }

    fun setDefault(modelId: String) {
        viewModelScope.launch { modelRepository.setDefault(modelId) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
