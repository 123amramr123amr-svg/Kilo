package com.personalaiagent.ui.screens.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.GhostButton
import com.personalaiagent.ui.components.InfoBanner
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.SectionCard
import com.personalaiagent.ui.components.SectionHeader
import com.personalaiagent.ui.components.SettingsRow
import com.personalaiagent.ui.components.StatusPill
import com.personalaiagent.ui.components.Tag
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandError
import com.personalaiagent.ui.theme.BrandSuccess
import com.personalaiagent.ui.theme.BrandWarning

@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            // After a denial the system may stop showing the dialog; send the user there.
            viewModel.refresh()
        } else {
            viewModel.refresh()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = stringResource(R.string.permissions_title),
            subtitle = stringResource(R.string.permissions_summary, state.grantedCount, state.total),
            onBack = onBack,
            backContentDescription = stringResource(R.string.cd_back),
            actions = {
                TextButton(onClick = viewModel::refresh) {
                    Text(stringResource(R.string.action_refresh))
                }
            },
        )

        if (state.isLoading) {
            LoadingState(stringResource(R.string.state_loading))
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InfoBanner(
                icon = Icons.Rounded.Security,
                title = stringResource(R.string.permissions_note_title),
                body = stringResource(R.string.permissions_note_body),
                tint = BrandSuccess,
            )
            Text(
                text = stringResource(R.string.permissions_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.permissions_workspace_summary,
                    state.workspaceName ?: stringResource(R.string.permissions_workspace_none),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = BrandCyan,
            )
            Text(
                text = stringResource(R.string.permissions_request_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHeader(stringResource(R.string.permissions_title))
            SectionCard {
                AppPermission.entries.forEach { permission ->
                    val status = state.statuses[permission] ?: PermissionState.DENIED
                    SettingsRow(
                        icon = permission.icon(),
                        title = stringResource(
                            when (permission) {
                                AppPermission.MICROPHONE -> R.string.perm_mic_name
                                AppPermission.NOTIFICATIONS -> R.string.perm_notif_name
                                AppPermission.STORAGE -> R.string.perm_storage_name
                                AppPermission.INTERNET -> R.string.perm_network_name
                            },
                        ),
                        subtitle = stringResource(
                            when (permission) {
                                AppPermission.MICROPHONE -> R.string.perm_mic_body
                                AppPermission.NOTIFICATIONS -> R.string.perm_notif_body
                                AppPermission.STORAGE -> R.string.perm_storage_body
                                AppPermission.INTERNET -> R.string.perm_network_body
                            },
                        ),
                        onClick = {
                            val canAsk = permission.minSdk <= android.os.Build.VERSION.SDK_INT &&
                                permission.maxSdk >= android.os.Build.VERSION.SDK_INT &&
                                permission != AppPermission.INTERNET &&
                                status != PermissionState.GRANTED
                            if (canAsk) launcher.launch(permission.permission)
                        },
                        trailing = {
                            when (status) {
                                PermissionState.GRANTED -> StatusPill(
                                    text = stringResource(R.string.permissions_granted),
                                    color = BrandSuccess,
                                )
                                PermissionState.NOT_APPLICABLE -> Tag(
                                    text = stringResource(R.string.permissions_rationale),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                PermissionState.BLOCKED -> StatusPill(
                                    text = stringResource(R.string.permissions_blocked),
                                    color = BrandWarning,
                                )
                                PermissionState.DENIED -> StatusPill(
                                    text = stringResource(R.string.permissions_denied),
                                    color = BrandError,
                                )
                            }
                        },
                    )
                    if (permission != AppPermission.entries.last()) {
                        com.personalaiagent.ui.components.Divider()
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            GhostButton(
                text = stringResource(R.string.action_open_settings),
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                },
                icon = Icons.Rounded.Warning,
            )
            GhostButton(
                text = stringResource(R.string.permissions_grant_all),
                onClick = {
                    // The system shows one dialog at a time, so ask for the first missing one.
                    val next = viewModel.runtimePermissions()
                        .firstOrNull { state.statuses[it] != PermissionState.GRANTED }
                    if (next != null) launcher.launch(next.permission)
                },
                enabled = state.statuses.any {
                    it.key != AppPermission.INTERNET &&
                        it.value != PermissionState.GRANTED &&
                        it.value != PermissionState.NOT_APPLICABLE &&
                        it.key.minSdk <= android.os.Build.VERSION.SDK_INT
                },
                icon = Icons.Rounded.Mic,
            )
        }
    }
}

private fun AppPermission.icon(): ImageVector = when (this) {
    AppPermission.MICROPHONE -> Icons.Rounded.Mic
    AppPermission.NOTIFICATIONS -> Icons.Rounded.Notifications
    AppPermission.STORAGE -> Icons.Rounded.FolderOpen
    AppPermission.INTERNET -> Icons.Rounded.Public
}
