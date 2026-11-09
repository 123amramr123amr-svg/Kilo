package com.personalaiagent.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.ConfirmDialog
import com.personalaiagent.ui.components.EmptyState
import com.personalaiagent.ui.components.ErrorBanner
import com.personalaiagent.ui.components.InfoBanner
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.StatusPill
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandError
import com.personalaiagent.ui.util.appErrorText

@Composable
fun ApiKeysScreen(
    onBack: () -> Unit,
    onOpenProviders: () -> Unit,
    viewModel: ApiKeysViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.api_keys_saved)
    val deletedMessage = stringResource(R.string.providers_key_deleted)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ApiKeysEvent.KeySaved -> snackbarHostState.showSnackbar(savedMessage)
                ApiKeysEvent.KeyDeleted -> snackbarHostState.showSnackbar(deletedMessage)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = stringResource(R.string.api_keys_title),
            onBack = onBack,
            backContentDescription = stringResource(R.string.cd_back),
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            state.error?.let { error ->
                ErrorBanner(message = appErrorText(error), onDismiss = viewModel::clearError)
                Spacer(Modifier.height(8.dp))
            }
            InfoBanner(
                icon = Icons.Rounded.Lock,
                title = stringResource(R.string.providers_key_stored),
                body = stringResource(R.string.api_keys_body),
            )
            Spacer(Modifier.height(12.dp))
        }

        when {
            state.isLoading -> LoadingState(stringResource(R.string.state_loading))

            state.providers.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Key,
                title = stringResource(R.string.api_keys_empty_title),
                body = stringResource(R.string.api_keys_empty_body),
                actionLabel = stringResource(R.string.providers_add),
                onAction = onOpenProviders,
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.providers, key = { it.id }) { provider ->
                    ApiKeyRow(
                        provider = provider,
                        onAddOrUpdate = { viewModel.openDialog(provider) },
                        onDelete = { viewModel.requestDelete(provider) },
                    )
                }
                item { SnackbarHost(hostState = snackbarHostState) }
            }
        }
    }

    state.dialogProvider?.let { provider ->
        AlertDialog(
            onDismissRequest = viewModel::closeDialog,
            title = { Text(stringResource(R.string.api_keys_dialog_title, provider.name)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.keyDraft,
                        onValueChange = viewModel::onKeyChange,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = { Text(stringResource(R.string.providers_api_key_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.api_keys_body),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::saveKey,
                    enabled = state.keyDraft.isNotBlank() && !state.isSaving,
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeDialog) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }

    if (state.deleteTarget != null) {
        ConfirmDialog(
            title = stringResource(R.string.api_keys_delete_title),
            body = stringResource(R.string.api_keys_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete,
        )
    }
}

@Composable
private fun ApiKeyRow(
    provider: Provider,
    onAddOrUpdate: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                StatusPill(
                    text = if (provider.hasKey) {
                        stringResource(R.string.api_keys_set)
                    } else {
                        stringResource(R.string.api_keys_not_set)
                    },
                    color = if (provider.hasKey) BrandCyan else BrandError,
                )
            }
            TextButton(onClick = onAddOrUpdate) {
                Text(
                    text = if (provider.hasKey) {
                        stringResource(R.string.api_keys_update)
                    } else {
                        stringResource(R.string.api_keys_add)
                    },
                )
            }
            IconButton(onClick = onDelete, enabled = provider.hasKey) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.api_keys_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
