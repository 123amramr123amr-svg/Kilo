package com.personalaiagent.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.domain.model.TaskRun
import com.personalaiagent.domain.repository.TaskRepository
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

data class TaskLogUiState(
    val isLoading: Boolean = true,
    val tasks: List<TaskRun> = emptyList(),
    val query: String = "",
) {
    val filtered: List<TaskRun>
        get() {
            val normalized = query.trim().lowercase()
            if (normalized.isEmpty()) return tasks
            return tasks.filter {
                it.prompt.lowercase().contains(normalized) ||
                    it.status.name.lowercase().contains(normalized) ||
                    (it.error?.lowercase()?.contains(normalized) ?: false)
            }
        }
}

sealed interface TaskLogEvent {
    data object Cleared : TaskLogEvent
}

@HiltViewModel
class TaskLogViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskLogUiState())
    val uiState: StateFlow<TaskLogUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<TaskLogEvent>(Channel.BUFFERED)
    val events: Flow<TaskLogEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            taskRepository.observeTasks().collect { tasks ->
                _uiState.update { it.copy(isLoading = false, tasks = tasks) }
            }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun clearAll() {
        viewModelScope.launch {
            taskRepository.clearAll()
            eventChannel.send(TaskLogEvent.Cleared)
        }
    }
}
