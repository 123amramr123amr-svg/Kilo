package com.personalaiagent.ui.screens.account

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
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.GhostButton
import com.personalaiagent.ui.components.GradientButton
import com.personalaiagent.ui.components.InfoBanner
import com.personalaiagent.ui.components.SectionCard
import com.personalaiagent.ui.components.SettingsRow
import com.personalaiagent.ui.theme.BrandWarning
import com.personalaiagent.ui.util.appErrorText

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportedMessage = stringResource(R.string.backup_export_success)
    val exportFailedMessage = stringResource(R.string.backup_export_failed)
    val importedMessage = stringResource(R.string.backup_import_success)

    var pendingError by remember { androidx.compose.runtime.mutableStateOf<com.personalaiagent.core.AppError?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                BackupEvent.Exported -> snackbarHostState.showSnackbar(exportedMessage)
                BackupEvent.ExportFailed -> snackbarHostState.showSnackbar(exportFailedMessage)
                BackupEvent.Imported -> snackbarHostState.showSnackbar(importedMessage)
                is BackupEvent.ImportFailed -> pendingError = event.error
            }
        }
    }

    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::export) }

    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::import) }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = stringResource(R.string.backup_title),
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
                icon = Icons.Rounded.Lock,
                title = stringResource(R.string.backup_keys_excluded),
                body = stringResource(R.string.backup_body),
                tint = BrandWarning,
            )

            SectionCard {
                SettingsRow(
                    icon = Icons.Rounded.FileDownload,
                    title = stringResource(R.string.backup_include_logs),
                    subtitle = stringResource(R.string.settings_task_log_desc),
                    onClick = null,
                    trailing = {
                        Switch(
                            checked = state.includeTaskLogs,
                            onCheckedChange = viewModel::setIncludeTaskLogs,
                        )
                    },
                )
            }

            GradientButton(
                text = stringResource(R.string.backup_export_title),
                onClick = { createDocument.launch("personal-ai-agent-backup.json") },
                loading = state.isWorking,
                icon = Icons.Rounded.FileDownload,
            )
            GhostButton(
                text = stringResource(R.string.backup_import_title),
                onClick = {
                    openDocument.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                enabled = !state.isWorking,
                icon = Icons.Rounded.FileUpload,
            )

            Text(
                text = stringResource(R.string.backup_restore_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            pendingError?.let { error ->
                Text(
                    text = appErrorText(error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(4.dp))
            }

            SnackbarHost(hostState = snackbarHostState)
        }
    }
}
