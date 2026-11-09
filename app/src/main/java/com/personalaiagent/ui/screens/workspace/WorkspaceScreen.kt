package com.personalaiagent.ui.screens.workspace

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.domain.model.WorkspaceEntry
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.ConfirmDialog
import com.personalaiagent.ui.components.EmptyState
import com.personalaiagent.ui.components.ErrorBanner
import com.personalaiagent.ui.components.GradientButton
import com.personalaiagent.ui.components.InfoBanner
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.SectionCard
import com.personalaiagent.ui.components.SettingsRow
import com.personalaiagent.ui.components.Tag
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandSuccess
import com.personalaiagent.ui.theme.BrandWarning
import com.personalaiagent.ui.theme.LocalBrand
import com.personalaiagent.ui.util.appErrorText
import com.personalaiagent.ui.util.Formatters

@Composable
fun WorkspaceScreen(
    onBack: () -> Unit,
    viewModel: WorkspaceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var newFileName by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val savedMessage = stringResource(R.string.workspace_saved)
    val deletedMessage = stringResource(R.string.workspace_deleted)
    val createdMessage = stringResource(R.string.workspace_created)
    val connectedMessage = stringResource(R.string.providers_status_connected)

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let { viewModel.connect(it.toString()) } }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                WorkspaceEvent.Saved -> snackbarHostState.showSnackbar(savedMessage)
                WorkspaceEvent.Deleted -> snackbarHostState.showSnackbar(deletedMessage)
                WorkspaceEvent.Created -> snackbarHostState.showSnackbar(createdMessage)
                WorkspaceEvent.Connected -> snackbarHostState.showSnackbar(connectedMessage)
                // The failure is rendered by the error banner from view-model state,
                // so the collector only needs to keep the channel drained.
                is WorkspaceEvent.Failed -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            BrandTopBar(
                title = stringResource(R.string.workspace_title),
                subtitle = state.workspace?.displayName ?: stringResource(R.string.workspace_subtitle),
                onBack = onBack,
                backContentDescription = stringResource(R.string.cd_back),
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = state.workspace != null) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.a11y_more),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.workspace_change)) },
                                leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    picker.launch(null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.workspace_disconnect)) },
                                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                                enabled = state.workspace != null,
                                onClick = {
                                    showMenu = false
                                    viewModel.disconnect()
                                },
                            )
                        }
                    }
                },
            )

            state.error?.let { error ->
                ErrorBanner(
                    message = appErrorText(error),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    onDismiss = viewModel::clearError,
                )
            }

            when {
                state.isLoading -> LoadingState(stringResource(R.string.state_loading))

                state.workspace == null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EmptyState(
                        icon = Icons.Rounded.Workspaces,
                        title = stringResource(R.string.workspace_not_connected_title),
                        body = stringResource(R.string.workspace_not_connected_body),
                    )
                    GradientButton(
                        text = stringResource(R.string.workspace_choose),
                        onClick = { picker.launch(null) },
                        icon = Icons.Rounded.FolderOpen,
                    )
                    Spacer(Modifier.height(14.dp))
                    InfoBanner(
                        icon = Icons.Rounded.Workspaces,
                        title = stringResource(R.string.workspace_title),
                        body = stringResource(R.string.workspace_saf_note),
                        tint = BrandCyan,
                    )
                }

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    SectionCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        SettingsRow(
                            icon = Icons.Rounded.Workspaces,
                            title = stringResource(R.string.workspace_allow_tools),
                            subtitle = stringResource(R.string.workspace_allow_tools_body),
                            onClick = null,
                            trailing = {
                                Switch(
                                    checked = state.toolsEnabled,
                                    onCheckedChange = viewModel::setToolsEnabled,
                                )
                            },
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Tag(
                                text = stringResource(
                                    if (state.toolsEnabled) R.string.workspace_tools_on else R.string.workspace_tools_off,
                                ),
                                color = if (state.toolsEnabled) BrandSuccess else BrandWarning,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = state.workspace?.displayName.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.currentPath.isNotEmpty()) {
                            IconButton(onClick = viewModel::goUp) {
                                Icon(
                                    Icons.Rounded.ArrowUpward,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = BrandCyan,
                                )
                            }
                        }
                        Text(
                            text = state.currentPath.ifEmpty { stringResource(R.string.workspace_root) },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(onClick = viewModel::startNewFile) {
                            Icon(Icons.Rounded.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.workspace_new_file))
                        }
                        TextButton(onClick = viewModel::startNewFolder) {
                            Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (state.isEmpty) {
                        EmptyState(
                            icon = Icons.Rounded.Folder,
                            title = stringResource(R.string.workspace_empty),
                            body = stringResource(R.string.workspace_not_connected_body),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(state.entries, key = { it.path }) { entry ->
                                EntryRow(
                                    entry = entry,
                                    onOpen = {
                                        if (entry.isDirectory) viewModel.openFolder(entry) else viewModel.openFile(entry)
                                    },
                                    onDelete = { viewModel.requestDelete(entry) },
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        )
    }

    if (state.showNewFile) {
        AlertDialog(
            onDismissRequest = viewModel::dismissNewFile,
            title = { Text(stringResource(R.string.workspace_new_file)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text(stringResource(R.string.workspace_file_name)) },
                        placeholder = { Text(stringResource(R.string.workspace_path_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createFile(newFileName, "")
                        newFileName = ""
                    },
                    enabled = newFileName.isNotBlank(),
                ) { Text(stringResource(R.string.workspace_create)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissNewFile) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }

    if (state.showNewFolder) {
        AlertDialog(
            onDismissRequest = viewModel::dismissNewFolder,
            title = { Text(stringResource(R.string.workspace_new_folder)) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text(stringResource(R.string.workspace_folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createFolder(newFolderName)
                        newFolderName = ""
                    },
                    enabled = newFolderName.isNotBlank(),
                ) { Text(stringResource(R.string.workspace_create)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissNewFolder) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }

    state.editor?.let { editor ->
        AlertDialog(
            onDismissRequest = viewModel::closeEditor,
            title = {
                Column {
                    Text(stringResource(R.string.workspace_editor_title))
                    Text(
                        text = editor.path,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            text = {
                OutlinedTextField(
                    value = editor.content,
                    onValueChange = viewModel::onEditorChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    placeholder = { Text(stringResource(R.string.workspace_content_hint)) },
                    shape = MaterialTheme.shapes.medium,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveEditor, enabled = !editor.isSaving) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeEditor) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }

    if (state.deleteTarget != null) {
        ConfirmDialog(
            title = stringResource(R.string.workspace_delete_title),
            body = stringResource(R.string.workspace_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete,
        )
    }
}

@Composable
private fun EntryRow(
    entry: WorkspaceEntry,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(LocalBrand.current.accentGradientSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Rounded.Folder else Icons.Rounded.Description,
                contentDescription = null,
                tint = BrandCyan,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (entry.isDirectory) {
                    stringResource(R.string.workspace_folder_name)
                } else {
                    Formatters.bytes(entry.size) + " · " + Formatters.date(entry.lastModified)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
