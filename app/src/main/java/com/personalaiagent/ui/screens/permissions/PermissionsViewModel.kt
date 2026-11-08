package com.personalaiagent.ui.screens.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalaiagent.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PermissionState { GRANTED, DENIED, BLOCKED, NOT_APPLICABLE }

enum class AppPermission(val permission: String, val minSdk: Int, val maxSdk: Int = Int.MAX_VALUE) {
    MICROPHONE(android.Manifest.permission.RECORD_AUDIO, 1),
    NOTIFICATIONS("android.permission.POST_NOTIFICATIONS", Build.VERSION_CODES.TIRAMISU),

    /**
     * Legacy storage permission, only meaningful up to Android 12L where the
     * folder picker needs it to grant a workspace tree. Android 13+ uses SAF only.
     */
    STORAGE(android.Manifest.permission.READ_EXTERNAL_STORAGE, 1, Build.VERSION_CODES.S_V2),
    INTERNET(android.Manifest.permission.INTERNET, 1),
}

data class PermissionsUiState(
    val statuses: Map<AppPermission, PermissionState> = emptyMap(),
    val workspaceName: String? = null,
    val isLoading: Boolean = true,
) {
    val grantedCount: Int get() = statuses.count { it.value == PermissionState.GRANTED || it.value == PermissionState.NOT_APPLICABLE }
    val total: Int get() = statuses.size
}

/**
 * Reads permission state. Requesting is intentionally done from the UI layer,
 * where ActivityResultContracts can show the system dialog.
 */
@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    workspaceName = settings.workspaceName ?: settings.workspaceUri,
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val statuses = AppPermission.entries.associateWith { permission ->
                when {
                    permission.minSdk > Build.VERSION.SDK_INT -> PermissionState.NOT_APPLICABLE
                    permission.maxSdk < Build.VERSION.SDK_INT -> PermissionState.NOT_APPLICABLE
                    permission == AppPermission.INTERNET -> PermissionState.GRANTED
                    context.checkSelfPermission(permission.permission) == PackageManager.PERMISSION_GRANTED ->
                        PermissionState.GRANTED
                    else -> PermissionState.DENIED
                }
            }
            _uiState.value = _uiState.value.copy(statuses = statuses, isLoading = false)
        }
    }

    /** Marks a permission as permanently blocked (system no longer shows a dialog). */
    fun markBlocked(permission: AppPermission) {
        val current = _uiState.value
        _uiState.value = current.copy(statuses = current.statuses + (permission to PermissionState.BLOCKED))
    }

    fun runtimePermissions(): List<AppPermission> = AppPermission.entries.filter {
        it != AppPermission.INTERNET &&
            it.minSdk <= Build.VERSION.SDK_INT &&
            it.maxSdk >= Build.VERSION.SDK_INT
    }
}
