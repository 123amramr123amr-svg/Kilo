package com.personalaiagent.ui.screens.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.domain.model.TaskRun
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import com.personalaiagent.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val isLoading: Boolean = true,
    val task: TaskRun? = null,
    val providerName: String = "",
    val modelName: String = "",
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    taskRepository: TaskRepository,
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private val taskId: String = savedStateHandle.get<String>("taskId").orEmpty()

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            taskRepository.observeTask(taskId).collect { task ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        task = task,
                        providerName = current.providerName.ifBlank {
                            task?.providerId?.let { providerRepository.getProvider(it)?.name } ?: ""
                        },
                        modelName = current.modelName.ifBlank {
                            task?.modelId?.let { modelRepository.getModel(it)?.displayName } ?: ""
                        },
                    )
                }
            }
        }
    }
}
