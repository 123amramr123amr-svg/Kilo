package com.personalaiagent.ui.screens.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.domain.model.TaskRun
import com.personalaiagent.domain.model.TaskStatus
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.ConfirmDialog
import com.personalaiagent.ui.components.EmptyState
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.StatusPill
import com.personalaiagent.ui.theme.BrandError
import com.personalaiagent.ui.theme.BrandSuccess
import com.personalaiagent.ui.theme.BrandWarning
import com.personalaiagent.ui.util.Formatters
import androidx.compose.ui.graphics.Color

@Composable
fun TaskLogScreen(
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    viewModel: TaskLogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmClear by remember { mutableStateOf(false) }
    val clearedMessage = stringResource(R.string.tasks_cleared)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TaskLogEvent.Cleared -> snackbarHostState.showSnackbar(clearedMessage)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = stringResource(R.string.tasks_title),
            subtitle = "${state.tasks.size}",
            onBack = onBack,
            backContentDescription = stringResource(R.string.cd_back),
            actions = {
                IconButton(
                    onClick = { confirmClear = true },
                    enabled = state.tasks.isNotEmpty(),
                ) {
                    Icon(
                        Icons.Rounded.DeleteSweep,
                        contentDescription = stringResource(R.string.tasks_clear_all),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.tasks_search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(Modifier.height(10.dp))

        when {
            state.isLoading -> LoadingState(stringResource(R.string.state_loading))

            state.tasks.isEmpty() -> EmptyState(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.tasks_empty_title),
                body = stringResource(R.string.tasks_empty_body),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.filtered, key = { it.id }) { task ->
                    TaskCard(task = task, onClick = { onOpenTask(task.id) })
                }
                item { SnackbarHost(hostState = snackbarHostState) }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.tasks_clear_title),
            body = stringResource(R.string.tasks_clear_body),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = {
                confirmClear = false
                viewModel.clearAll()
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun TaskCard(task: TaskRun, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(text = task.status.label(), color = task.status.color())
                Spacer(Modifier.width(8.dp))
                Text(
                    text = Formatters.dateTime(task.startedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                task.durationMillis?.let {
                    Text(
                        text = Formatters.duration(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = task.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!task.error.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = BrandError,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = task.error,
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandError,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = "${task.steps.size} · ${Formatters.tokens(task.tokensIn, task.tokensOut)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TaskStatus.label(): String = when (this) {
    TaskStatus.QUEUED -> stringResource(R.string.tasks_status_queued)
    TaskStatus.RUNNING -> stringResource(R.string.tasks_status_running)
    TaskStatus.SUCCEEDED -> stringResource(R.string.tasks_status_succeeded)
    TaskStatus.FAILED -> stringResource(R.string.tasks_status_failed)
    TaskStatus.CANCELLED -> stringResource(R.string.tasks_status_cancelled)
}

fun TaskStatus.color(): Color = when (this) {
    TaskStatus.QUEUED, TaskStatus.RUNNING -> BrandWarning
    TaskStatus.SUCCEEDED -> BrandSuccess
    TaskStatus.FAILED -> BrandError
    TaskStatus.CANCELLED -> BrandWarning
}
