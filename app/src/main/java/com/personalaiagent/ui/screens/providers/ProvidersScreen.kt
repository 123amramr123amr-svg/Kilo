package com.personalaiagent.ui.screens.providers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.domain.model.ProviderStatus
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.ConfirmDialog
import com.personalaiagent.ui.components.EmptyState
import com.personalaiagent.ui.components.GradientButton
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.StatusPill
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandError
import com.personalaiagent.ui.theme.BrandSuccess
import com.personalaiagent.ui.theme.LocalBrand
import com.personalaiagent.ui.util.appErrorText

@Composable
fun ProvidersScreen(
    onBack: () -> Unit,
    onAddProvider: () -> Unit,
    onEditProvider: (String) -> Unit,
    onOpenModels: () -> Unit,
    viewModel: ProvidersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<Provider?>(null) }
    var error by remember { mutableStateOf<com.personalaiagent.core.AppError?>(null) }

    val deletedMessage = stringResource(R.string.providers_deleted)
    val defaultMessage = stringResource(R.string.settings_applied)
    val syncedMessage = stringResource(R.string.providers_saved)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProvidersEvent.ProviderDeleted -> snackbarHostState.showSnackbar(deletedMessage)
                ProvidersEvent.DefaultChanged -> snackbarHostState.showSnackbar(defaultMessage)
                ProvidersEvent.CatalogSynced -> snackbarHostState.showSnackbar(syncedMessage)
                is ProvidersEvent.Error -> error = event.error
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            BrandTopBar(
                title = stringResource(R.string.providers_title),
                onBack = onBack,
                backContentDescription = stringResource(R.string.cd_back),
                actions = {
                    IconButton(onClick = onAddProvider) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.providers_add),
                            tint = BrandCyan,
                        )
                    }
                },
            )

            error?.let { current ->
                com.personalaiagent.ui.components.ErrorBanner(
                    message = appErrorText(current),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    onDismiss = { error = null },
                )
            }

            when {
                state.isLoading -> LoadingState(stringResource(R.string.state_loading))

                state.providers.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.Cloud,
                    title = stringResource(R.string.providers_empty_title),
                    body = stringResource(R.string.providers_empty_body),
                    actionLabel = stringResource(R.string.providers_add),
                    onAction = onAddProvider,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.providers, key = { it.id }) { provider ->
                        ProviderCard(
                            provider = provider,
                            busy = state.busyProviderId == provider.id,
                            onSetDefault = { viewModel.setDefault(provider.id) },
                            onRefresh = { viewModel.refreshCatalog(provider.id) },
                            onEdit = { onEditProvider(provider.id) },
                            onDelete = { pendingDelete = provider },
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            if (state.providers.isNotEmpty()) {
                GradientButton(
                    text = stringResource(R.string.providers_add),
                    onClick = onAddProvider,
                    icon = Icons.Rounded.Add,
                )
            }
            TextButton(onClick = onOpenModels, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_providers_desc))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp),
        )
    }

    pendingDelete?.let { provider ->
        ConfirmDialog(
            title = stringResource(R.string.providers_delete_title),
            body = stringResource(R.string.providers_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = {
                pendingDelete = null
                viewModel.deleteProvider(provider.id)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ProviderCard(
    provider: Provider,
    busy: Boolean,
    onSetDefault: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (provider.isDefault) BrandCyan.copy(alpha = 0.5f) else LocalBrand.current.cardBorder,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LocalBrand.current.accentGradientSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = BrandCyan,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = provider.baseUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.a11y_more),
                            )
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit)) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    onEdit()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.providers_set_default)) },
                                leadingIcon = {
                                    Icon(
                                        if (provider.isDefault) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menu = false
                                    onSetDefault()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_refresh)) },
                                leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    onRefresh()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    onDelete()
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (provider.status) {
                    ProviderStatus.CONNECTED -> StatusPill(
                        text = stringResource(R.string.providers_status_connected),
                        color = BrandSuccess,
                    )
                    ProviderStatus.ERROR -> StatusPill(
                        text = stringResource(R.string.providers_status_error),
                        color = BrandError,
                    )
                    ProviderStatus.UNKNOWN -> StatusPill(
                        text = stringResource(R.string.providers_status_unknown),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPill(
                    text = if (provider.hasKey) {
                        stringResource(R.string.providers_key_stored)
                    } else {
                        stringResource(R.string.providers_key_missing)
                    },
                    color = if (provider.hasKey) BrandCyan else BrandError,
                )
                Spacer(Modifier.weight(1f))
                if (provider.isDefault) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = BrandSuccess,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.providers_default),
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandSuccess,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = if (provider.kind == ProviderKind.ANTHROPIC) {
                    stringResource(R.string.providers_kind_anthropic)
                } else {
                    stringResource(R.string.providers_kind_openai)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
