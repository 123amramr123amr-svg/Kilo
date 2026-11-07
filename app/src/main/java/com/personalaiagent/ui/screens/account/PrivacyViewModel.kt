package com.personalaiagent.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.domain.repository.ChatRepository
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

data class PrivacyUiState(val isWorking: Boolean = false)

sealed interface PrivacyEvent {
    data object ConversationsDeleted : PrivacyEvent
    data object LogsDeleted : PrivacyEvent
}

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<PrivacyEvent>(Channel.BUFFERED)
    val events: Flow<PrivacyEvent> = eventChannel.receiveAsFlow()

    fun clearConversations() {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            chatRepository.clearAll()
            _uiState.update { it.copy(isWorking = false) }
            eventChannel.send(PrivacyEvent.ConversationsDeleted)
        }
    }

    fun clearTaskLogs() {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            taskRepository.clearAll()
            _uiState.update { it.copy(isWorking = false) }
            eventChannel.send(PrivacyEvent.LogsDeleted)
        }
    }
}
