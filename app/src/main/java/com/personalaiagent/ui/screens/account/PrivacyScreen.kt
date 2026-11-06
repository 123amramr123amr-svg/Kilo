package com.personalaiagent.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.ConfirmDialog
import com.personalaiagent.ui.components.InfoBanner
import com.personalaiagent.ui.components.SectionCard
import com.personalaiagent.ui.components.SectionHeader
import com.personalaiagent.ui.components.SettingsRow
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandError

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    viewModel: PrivacyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmChats by remember { mutableStateOf(false) }
    var confirmLogs by remember { mutableStateOf(false) }

    val chatsDeleted = stringResource(R.string.privacy_clear_chat_done)
    val logsDeleted = stringResource(R.string.privacy_clear_logs_done)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PrivacyEvent.ConversationsDeleted -> snackbarHostState.showSnackbar(chatsDeleted)
                PrivacyEvent.LogsDeleted -> snackbarHostState.showSnackbar(logsDeleted)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = stringResource(R.string.privacy_title),
            onBack = onBack,
            backContentDescription = stringResource(R.string.cd_back),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoBanner(
                icon = Icons.Rounded.CloudOff,
                title = stringResource(R.string.privacy_network_title),
                body = stringResource(R.string.privacy_network_body),
            )
            InfoBanner(
                icon = Icons.Rounded.Lock,
                title = stringResource(R.string.privacy_storage_title),
                body = stringResource(R.string.privacy_storage_body),
                tint = BrandCyan,
            )
            InfoBanner(
                icon = Icons.Rounded.Storage,
                title = stringResource(R.string.privacy_backup_title),
                body = stringResource(R.string.privacy_backup_body),
            )
            InfoBanner(
                icon = Icons.Rounded.TextSnippet,
                title = stringResource(R.string.privacy_logs_title),
                body = stringResource(R.string.privacy_logs_body),
            )

            SectionHeader(stringResource(R.string.settings_data))
            SectionCard {
                SettingsRow(
                    icon = Icons.Rounded.DeleteOutline,
                    title = stringResource(R.string.privacy_clear_chat),
                    subtitle = stringResource(R.string.privacy_clear_chat_body),
                    onClick = { confirmChats = true },
                    iconTint = BrandError,
                    enabled = !state.isWorking,
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.privacy_clear_logs),
                    subtitle = stringResource(R.string.tasks_clear_body),
                    onClick = { confirmLogs = true },
                    iconTint = BrandError,
                    enabled = !state.isWorking,
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.account_no_cloud_body),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SnackbarHost(hostState = snackbarHostState)
        }
    }

    if (confirmChats) {
        ConfirmDialog(
            title = stringResource(R.string.privacy_clear_chat),
            body = stringResource(R.string.privacy_clear_chat_body),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = {
                confirmChats = false
                viewModel.clearConversations()
            },
            onDismiss = { confirmChats = false },
        )
    }

    if (confirmLogs) {
        ConfirmDialog(
            title = stringResource(R.string.tasks_clear_title),
            body = stringResource(R.string.tasks_clear_body),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = {
                confirmLogs = false
                viewModel.clearTaskLogs()
            },
            onDismiss = { confirmLogs = false },
        )
    }
}
