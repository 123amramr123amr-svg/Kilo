package com.personalaiagent.ui.screens.tasks

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.domain.model.TaskStep
import com.personalaiagent.domain.model.TaskStepName
import com.personalaiagent.domain.model.TaskStepStatus
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.DetailRow
import com.personalaiagent.ui.components.ErrorBanner
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.SectionCard
import com.personalaiagent.ui.components.SectionHeader
import com.personalaiagent.ui.components.StatusPill
import com.personalaiagent.ui.components.Tag
import com.personalaiagent.ui.theme.BrandError
import com.personalaiagent.ui.theme.BrandSuccess
import com.personalaiagent.ui.theme.BrandWarning
import com.personalaiagent.ui.util.Formatters

@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = stringResource(R.string.tasks_detail_title),
            onBack = onBack,
            backContentDescription = stringResource(R.string.cd_back),
        )

        val task = state.task
        when {
            state.isLoading -> LoadingState(stringResource(R.string.state_loading))

            task == null -> LoadingState(stringResource(R.string.tasks_detail_missing))

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(text = task.status.label(), color = task.status.color())
                        Spacer(Modifier.width(8.dp))
                        Tag(
                            text = Formatters.duration(task.durationMillis ?: 0L),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = task.prompt,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                task.error?.let { error ->
                    ErrorBanner(message = error)
                }

                SectionHeader(stringResource(R.string.tasks_detail_title))
                SectionCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                    DetailRow(stringResource(R.string.tasks_started), Formatters.dateTime(task.startedAt))
                    task.finishedAt?.let {
                        DetailRow(stringResource(R.string.tasks_finished), Formatters.dateTime(it))
                    }
                    if (state.providerName.isNotBlank()) {
                        DetailRow(stringResource(R.string.settings_providers), state.providerName)
                    }
                    if (state.modelName.isNotBlank()) {
                        DetailRow(stringResource(R.string.tasks_model), state.modelName)
                    }
                    DetailRow(
                        stringResource(R.string.tasks_tokens),
                        Formatters.tokens(task.tokensIn, task.tokensOut),
                    )
                }

                SectionHeader(stringResource(R.string.tasks_steps))
                SectionCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                    if (task.steps.isEmpty()) {
                        Text(
                            text = stringResource(R.string.state_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        task.steps.forEachIndexed { index, step ->
                            StepRow(step = step, isLast = index == task.steps.lastIndex)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(step: TaskStep, isLast: Boolean) {
    val color = when (step.status) {
        TaskStepStatus.SUCCEEDED -> BrandSuccess
        TaskStepStatus.FAILED -> BrandError
        TaskStepStatus.RUNNING -> BrandWarning
        TaskStepStatus.PENDING, TaskStepStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                when (step.status) {
                    TaskStepStatus.SUCCEEDED -> Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp),
                    )
                    TaskStepStatus.FAILED -> Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp),
                    )
                    else -> Icon(
                        Icons.Rounded.HourglassEmpty,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            if (!isLast) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 10.dp)) {
            Text(
                text = step.name.label(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = buildString {
                    append(Formatters.dateTime(step.startedAt))
                    step.finishedAt?.let {
                        append(" · ")
                        append(Formatters.duration((it - step.startedAt).coerceAtLeast(0)))
                    }
                    step.detail?.let {
                        append(" · ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = step.status.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
fun TaskStepName.label(): String = when (this) {
    TaskStepName.RESOLVE_PROVIDER -> stringResource(R.string.tasks_step_resolve)
    TaskStepName.BUILD_PROMPT -> stringResource(R.string.tasks_step_prompt)
    TaskStepName.SEND_REQUEST -> stringResource(R.string.tasks_step_request)
    TaskStepName.RECEIVE_RESPONSE -> stringResource(R.string.tasks_step_stream)
    TaskStepName.PERSIST_MESSAGE -> stringResource(R.string.tasks_step_persist)
}
