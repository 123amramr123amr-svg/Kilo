package com.personalaiagent.ui.screens.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.core.AppError
import com.personalaiagent.core.AppConstants
import com.personalaiagent.core.Outcome
import com.personalaiagent.core.WorkspacePaths
import com.personalaiagent.domain.model.Workspace
import com.personalaiagent.domain.model.WorkspaceEntry
import com.personalaiagent.domain.repository.SettingsRepository
import com.personalaiagent.domain.repository.WorkspaceRepository
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

data class EditorState(
    val path: String,
    val content: String,
    val isNew: Boolean,
    val isSaving: Boolean = false,
)

data class WorkspaceUiState(
    val isLoading: Boolean = true,
    val workspace: Workspace? = null,
    val entries: List<WorkspaceEntry> = emptyList(),
    val currentPath: String = "",
    val toolsEnabled: Boolean = true,
    val isWorking: Boolean = false,
    val error: AppError? = null,
    val editor: EditorState? = null,
    val showNewFile: Boolean = false,
    val showNewFolder: Boolean = false,
    val deleteTarget: WorkspaceEntry? = null,
) {
    val isEmpty: Boolean get() = !isLoading && workspace != null && entries.isEmpty()
}

sealed interface WorkspaceEvent {
    data object Saved : WorkspaceEvent
    data object Deleted : WorkspaceEvent
    data object Created : WorkspaceEvent
    data object Connected : WorkspaceEvent
    data class Failed(val error: AppError) : WorkspaceEvent
}

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<WorkspaceEvent>(Channel.BUFFERED)
    val events: Flow<WorkspaceEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            var loadedUri: String? = null
            settingsRepository.settings.collect { settings ->
                val workspace = settings.workspaceUri?.let {
                    Workspace(it, settings.workspaceName ?: it)
                }
                val changed = workspace?.uri != loadedUri
                loadedUri = workspace?.uri
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        workspace = workspace,
                        toolsEnabled = settings.agentFileToolsEnabled,
                        currentPath = if (changed) "" else it.currentPath,
                        entries = if (changed) emptyList() else it.entries,
                    )
                }
                if (changed) refresh()
            }
        }
    }

    fun connect(treeUri: String) {
        viewModelScope.launch {
            when (val result = workspaceRepository.connect(treeUri)) {
                is Outcome.Success -> {
                    eventChannel.send(WorkspaceEvent.Connected)
                    refresh()
                }
                is Outcome.Failure -> eventChannel.send(WorkspaceEvent.Failed(result.error))
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            workspaceRepository.disconnect()
            _uiState.update { it.copy(entries = emptyList(), currentPath = "") }
        }
    }

    fun setToolsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAgentFileToolsEnabled(enabled) }
    }

    fun refresh() {
        val state = _uiState.value
        if (state.workspace == null) {
            _uiState.update { it.copy(entries = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null) }
            when (val result = workspaceRepository.list(state.currentPath)) {
                is Outcome.Success -> _uiState.update {
                    it.copy(isWorking = false, entries = result.value)
                }
                is Outcome.Failure -> _uiState.update {
                    it.copy(isWorking = false, entries = emptyList(), error = result.error)
                }
            }
        }
    }

    fun openFolder(entry: WorkspaceEntry) {
        if (!entry.isDirectory) return
        _uiState.update { it.copy(currentPath = entry.path) }
        refresh()
    }

    fun goUp() {
        val parent = WorkspacePaths.parentOf(_uiState.value.currentPath)
        _uiState.update { it.copy(currentPath = parent) }
        refresh()
    }

    fun openFile(entry: WorkspaceEntry) {
        if (entry.isDirectory) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null) }
            when (val result = workspaceRepository.read(entry.path, AppConstants.MAX_EDITOR_BYTES)) {
                is Outcome.Success -> _uiState.update {
                    it.copy(
                        isWorking = false,
                        editor = EditorState(
                            path = result.value.path,
                            content = result.value.content,
                            isNew = false,
                        ),
                    )
                }
                is Outcome.Failure -> _uiState.update {
                    it.copy(isWorking = false, error = result.error)
                }
            }
        }
    }

    // ------------------------------------------------------------ editor

    fun startNewFile() {
        _uiState.update { it.copy(showNewFile = true) }
    }

    fun dismissNewFile() {
        _uiState.update { it.copy(showNewFile = false) }
    }

    fun createFile(nameOrPath: String, content: String) {
        val target = resolveNewPath(nameOrPath) ?: run {
            viewModelScope.launch { eventChannel.send(WorkspaceEvent.Failed(AppError.InvalidInput("path"))) }
            return
        }
        _uiState.update { it.copy(showNewFile = false, isWorking = true) }
        viewModelScope.launch {
            when (val result = workspaceRepository.write(target, content)) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(isWorking = false) }
                    eventChannel.send(WorkspaceEvent.Created)
                    refresh()
                }
                is Outcome.Failure -> {
                    _uiState.update { it.copy(isWorking = false) }
                    eventChannel.send(WorkspaceEvent.Failed(result.error))
                }
            }
        }
    }

    fun startNewFolder() {
        _uiState.update { it.copy(showNewFolder = true) }
    }

    fun dismissNewFolder() {
        _uiState.update { it.copy(showNewFolder = false) }
    }

    fun createFolder(nameOrPath: String) {
        val target = resolveNewPath(nameOrPath) ?: run {
            viewModelScope.launch { eventChannel.send(WorkspaceEvent.Failed(AppError.InvalidInput("path"))) }
            return
        }
        _uiState.update { it.copy(showNewFolder = false, isWorking = true) }
        viewModelScope.launch {
            when (val result = workspaceRepository.createFolder(target)) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(isWorking = false) }
                    eventChannel.send(WorkspaceEvent.Created)
                    refresh()
                }
                is Outcome.Failure -> {
                    _uiState.update { it.copy(isWorking = false) }
                    eventChannel.send(WorkspaceEvent.Failed(result.error))
                }
            }
        }
    }

    fun onEditorChange(value: String) {
        _uiState.update { current ->
            current.editor?.let { current.copy(editor = it.copy(content = value)) } ?: current
        }
    }

    fun saveEditor() {
        val editor = _uiState.value.editor ?: return
        _uiState.update { it.copy(editor = editor.copy(isSaving = true)) }
        viewModelScope.launch {
            when (val result = workspaceRepository.write(editor.path, editor.content)) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(editor = null) }
                    eventChannel.send(WorkspaceEvent.Saved)
                    refresh()
                }
                is Outcome.Failure -> {
                    _uiState.update { it.copy(editor = null) }
                    eventChannel.send(WorkspaceEvent.Failed(result.error))
                }
            }
        }
    }

    fun closeEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun requestDelete(entry: WorkspaceEntry) {
        _uiState.update { it.copy(deleteTarget = entry) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        _uiState.update { it.copy(deleteTarget = null, isWorking = true) }
        viewModelScope.launch {
            when (val result = workspaceRepository.delete(target.path)) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(isWorking = false) }
                    eventChannel.send(WorkspaceEvent.Deleted)
                    refresh()
                }
                is Outcome.Failure -> {
                    _uiState.update { it.copy(isWorking = false) }
                    eventChannel.send(WorkspaceEvent.Failed(result.error))
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Accepts either a bare name (current folder) or a relative path. */
    private fun resolveNewPath(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val combined = if (trimmed.contains('/')) {
            trimmed
        } else {
            val current = _uiState.value.currentPath
            if (current.isEmpty()) trimmed else "$current/$trimmed"
        }
        return WorkspacePaths.normalize(combined)
    }
}
