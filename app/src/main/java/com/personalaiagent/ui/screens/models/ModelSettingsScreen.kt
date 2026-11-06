package com.personalaiagent.ui.screens.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.domain.model.ModelParams
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.DetailRow
import com.personalaiagent.ui.components.GhostButton
import com.personalaiagent.ui.components.GradientButton
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.SectionCard
import com.personalaiagent.ui.components.SettingsRow
import com.personalaiagent.ui.components.Tag
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.util.Formatters
import kotlin.math.roundToInt

@Composable
fun ModelSettingsScreen(
    onBack: () -> Unit,
    viewModel: ModelSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.models_saved)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ModelSettingsEvent.Saved -> snackbarHostState.showSnackbar(savedMessage)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = state.model?.displayName ?: stringResource(R.string.models_settings_title),
            subtitle = state.model?.providerName,
            onBack = onBack,
            backContentDescription = stringResource(R.string.cd_back),
        )

        if (state.isLoading) {
            LoadingState(stringResource(R.string.state_loading))
            return@Column
        }

        val model = state.model
        if (model == null) {
            LoadingState(stringResource(R.string.models_provider_missing))
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard {
                DetailRow(stringResource(R.string.tasks_model), model.remoteId)
                DetailRow(
                    stringResource(R.string.models_context_window),
                    Formatters.compactNumber(model.contextWindow),
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    model.capabilities.forEach { capability -> Tag(text = capability.name) }
                }
            }

            SectionCard {
                SettingsRow(
                    icon = Icons.Rounded.Star,
                    title = stringResource(R.string.models_set_default),
                    subtitle = if (model.isDefault) stringResource(R.string.models_default) else null,
                    onClick = viewModel::setAsDefault,
                    trailing = {
                        if (model.isDefault) {
                            Tag(text = stringResource(R.string.models_active), color = BrandCyan)
                        }
                    },
                )
                SettingsRow(
                    icon = Icons.Rounded.Memory,
                    title = stringResource(R.string.models_enable),
                    subtitle = stringResource(R.string.models_enable_body),
                    onClick = null,
                    trailing = {
                        Switch(checked = state.enabled, onCheckedChange = viewModel::setEnabled)
                    },
                )
            }

            SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                Text(
                    text = stringResource(R.string.models_temperature),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.models_temperature_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SliderRow(
                    value = state.params.temperature,
                    valueText = String.format(java.util.Locale.getDefault(), "%.2f", state.params.temperature),
                    onValueChange = viewModel::setTemperature,
                    range = ModelParams.MIN_TEMPERATURE..ModelParams.MAX_TEMPERATURE,
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.models_max_tokens),
                    style = MaterialTheme.typography.titleMedium,
                )
                SliderRow(
                    value = state.params.maxTokens.toFloat(),
                    valueText = state.params.maxTokens.toString(),
                    onValueChange = { viewModel.setMaxTokens(it.roundToInt()) },
                    range = ModelParams.MIN_MAX_TOKENS.toFloat()..ModelParams.MAX_MAX_TOKENS.toFloat(),
                    steps = 24,
                )

                Spacer(Modifier.height(8.dp))
                Text(text = stringResource(R.string.models_top_p), style = MaterialTheme.typography.titleMedium)
                SliderRow(
                    value = state.params.topP,
                    valueText = String.format(java.util.Locale.getDefault(), "%.2f", state.params.topP),
                    onValueChange = viewModel::setTopP,
                    range = 0f..1f,
                )
            }

            SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                Text(
                    text = stringResource(R.string.models_system_prompt),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.params.systemPrompt,
                    onValueChange = viewModel::setSystemPrompt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    placeholder = { Text(stringResource(R.string.models_system_prompt_hint)) },
                    shape = MaterialTheme.shapes.medium,
                )
            }

            SectionCard {
                SettingsRow(
                    icon = Icons.Rounded.Bolt,
                    title = stringResource(R.string.models_streaming),
                    subtitle = stringResource(R.string.models_streaming_body),
                    onClick = null,
                    trailing = {
                        Switch(checked = state.params.streaming, onCheckedChange = viewModel::setStreaming)
                    },
                )
            }

            GradientButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::save,
                loading = state.isSaving,
                enabled = state.isDirty,
            )
            GhostButton(
                text = stringResource(R.string.models_reset),
                onClick = viewModel::resetToDefaults,
                icon = Icons.Rounded.Refresh,
            )
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

@Composable
private fun SliderRow(
    value: Float,
    valueText: String,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelLarge,
            color = BrandCyan,
        )
    }
}
