package com.personalaiagent.ui.screens.account

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.core.AppError
import com.personalaiagent.core.Outcome
import com.personalaiagent.data.backup.BackupManager
import com.personalaiagent.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(
    val includeTaskLogs: Boolean = true,
    val isWorking: Boolean = false,
)

sealed interface BackupEvent {
    data object Exported : BackupEvent
    data object ExportFailed : BackupEvent
    data class Imported(val conversations: Int) : BackupEvent
    data class ImportFailed(val error: AppError) : BackupEvent
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<BackupEvent>(Channel.BUFFERED)
    val events: Flow<BackupEvent> = eventChannel.receiveAsFlow()

    fun setIncludeTaskLogs(enabled: Boolean) {
        _uiState.update { it.copy(includeTaskLogs = enabled) }
    }

    fun export(uri: Uri) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val ok = withContext(io) {
                runCatching {
                    val payload = backupManager.exportJson(_uiState.value.includeTaskLogs)
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(payload.toByteArray(Charsets.UTF_8))
                    } ?: false
                    true
                }.getOrDefault(false)
            }
            _uiState.update { it.copy(isWorking = false) }
            eventChannel.send(if (ok) BackupEvent.Exported else BackupEvent.ExportFailed)
        }
    }

    fun import(uri: Uri) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val raw = withContext(io) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (raw == null) {
                _uiState.update { it.copy(isWorking = false) }
                eventChannel.send(BackupEvent.ImportFailed(AppError.InvalidInput("malformed_backup")))
                return@launch
            }
            val result = withContext(io) { backupManager.importJson(raw) }
            _uiState.update { it.copy(isWorking = false) }
            when (result) {
                is Outcome.Success -> eventChannel.send(BackupEvent.Imported(result.value))
                is Outcome.Failure -> eventChannel.send(BackupEvent.ImportFailed(result.error))
            }
        }
    }
}
