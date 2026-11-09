package com.personalaiagent.ui.screens.providers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.domain.model.ProviderKind
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.ChoiceChip
import com.personalaiagent.ui.components.ErrorBanner
import com.personalaiagent.ui.components.GhostButton
import com.personalaiagent.ui.components.GradientButton
import com.personalaiagent.ui.components.InfoBanner
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.SectionCard
import com.personalaiagent.ui.theme.BrandSuccess
import com.personalaiagent.ui.util.appErrorText

@Composable
fun ProviderEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ProviderEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.providers_saved)
    var showKey by remember { mutableStateOf(false) }

    val modelsSavedTemplate = stringResource(R.string.providers_models_saved)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProviderFormEvent.Saved -> {
                    snackbarHostState.showSnackbar(savedMessage)
                    onSaved()
                }
                is ProviderFormEvent.ModelsApplied ->
                    snackbarHostState.showSnackbar("$modelsSavedTemplate (${event.count})")
            }
        }
    }

    val presets = listOf(
        ProviderPreset(
            labelRes = R.string.providers_preset_openai,
            kind = ProviderKind.OPENAI_COMPATIBLE,
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
        ),
        ProviderPreset(
            labelRes = R.string.providers_preset_anthropic,
            kind = ProviderKind.ANTHROPIC,
            name = "Anthropic",
            baseUrl = "https://api.anthropic.com/v1",
        ),
        ProviderPreset(
            labelRes = R.string.providers_preset_gemini,
            kind = ProviderKind.OPENAI_COMPATIBLE,
            name = "Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        ),
        ProviderPreset(
            labelRes = R.string.providers_preset_groq,
            kind = ProviderKind.OPENAI_COMPATIBLE,
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
        ),
        ProviderPreset(
            labelRes = R.string.providers_preset_deepseek,
            kind = ProviderKind.OPENAI_COMPATIBLE,
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1",
        ),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = stringResource(
                if (state.isEditing) R.string.providers_edit_title else R.string.providers_add_title,
            ),
            onBack = onBack,
            backContentDescription = stringResource(R.string.cd_back),
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
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.formError?.let { error ->
                ErrorBanner(
                    message = appErrorText(error),
                    onDismiss = viewModel::clearFormError,
                )
            }

            if (!state.isEditing) {
                Text(
                    text = stringResource(R.string.providers_presets),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.take(3).forEach { preset ->
                        ChoiceChip(
                            label = stringResource(preset.labelRes),
                            selected = state.baseUrl == preset.baseUrl,
                            onClick = { viewModel.applyPreset(preset) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.drop(3).forEach { preset ->
                        ChoiceChip(
                            label = stringResource(preset.labelRes),
                            selected = state.baseUrl == preset.baseUrl,
                            onClick = { viewModel.applyPreset(preset) },
                        )
                    }
                }
            }

            SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.providers_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.providers_kind),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(
                        label = stringResource(R.string.providers_kind_openai),
                        selected = state.kind == ProviderKind.OPENAI_COMPATIBLE,
                        onClick = { viewModel.onKindChange(ProviderKind.OPENAI_COMPATIBLE) },
                    )
                    ChoiceChip(
                        label = stringResource(R.string.providers_kind_anthropic),
                        selected = state.kind == ProviderKind.ANTHROPIC,
                        onClick = { viewModel.onKindChange(ProviderKind.ANTHROPIC) },
                    )
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = viewModel::onBaseUrlChange,
                    label = { Text(stringResource(R.string.providers_base_url)) },
                    placeholder = { Text(stringResource(R.string.providers_base_url_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.apiKey,
                    onValueChange = viewModel::onApiKeyChange,
                    label = { Text(stringResource(R.string.providers_api_key)) },
                    placeholder = {
                        Text(
                            if (state.hasStoredKey) {
                                stringResource(R.string.providers_api_key_keep)
                            } else {
                                stringResource(R.string.providers_api_key_hint)
                            },
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                )

                Spacer(Modifier.height(12.dp))
                InfoBanner(
                    icon = Icons.Rounded.Lock,
                    title = stringResource(R.string.providers_key_stored),
                    body = stringResource(R.string.api_keys_body),
                )
            }

            InfoBanner(
                icon = Icons.Rounded.CloudDownload,
                title = stringResource(R.string.providers_fetch_models),
                body = stringResource(R.string.providers_fetch_hint),
            )

            GhostButton(
                text = if (state.isFetchingModels) {
                    stringResource(R.string.providers_fetching_models)
                } else {
                    stringResource(R.string.providers_fetch_models)
                },
                onClick = viewModel::fetchModels,
                enabled = !state.isFetchingModels,
                icon = Icons.Rounded.CloudDownload,
            )

            state.fetchError?.let { error ->
                ErrorBanner(
                    message = appErrorText(error),
                    onDismiss = viewModel::clearFetchError,
                )
            }

            if (state.fetchedModels.isNotEmpty()) {
                SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    Text(
                        text = stringResource(R.string.providers_models_shown),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.providers_fetch_success, state.fetchedModels.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.providers_models_enabled_count,
                            state.selectedModelIds.size,
                            state.fetchedModels.size,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    state.fetchedModels.take(12).forEach { model ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (model.remoteId in state.selectedModelIds) "✓" else "•",
                                color = if (model.remoteId in state.selectedModelIds) BrandSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if (state.fetchedModels.size > 12) {
                        Text(
                            text = "+${state.fetchedModels.size - 12}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.showModelsDialogAction() },
                        enabled = !state.isFetchingModels,
                    ) {
                        Text(stringResource(R.string.providers_models_apply))
                    }
                }
            }

            state.appliedModels?.let { count ->
                InfoBanner(
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.providers_models_saved_title),
                    body = stringResource(R.string.providers_models_saved_body, count),
                    tint = BrandSuccess,
                )
            }

            state.testSuccessCount?.let { count ->
                InfoBanner(
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.providers_test_success, count),
                    body = stringResource(R.string.providers_status_connected),
                    tint = BrandSuccess,
                )
            }
            state.testError?.let { error ->
                ErrorBanner(message = stringResource(R.string.providers_test_failure, appErrorText(error)))
            }

            GhostButton(
                text = stringResource(R.string.action_test),
                onClick = viewModel::testConnection,
                enabled = !state.isTesting,
                icon = Icons.Rounded.Wifi,
            )
            GradientButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::save,
                loading = state.isSaving,
            )

            SnackbarHost(hostState = snackbarHostState)
        }
    }

    if (state.showModelsDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissModelsDialog,
            title = {
                Text(stringResource(R.string.providers_models_dialog_title, state.fetchedModels.size))
            },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { viewModel.selectAllModels(true) }) {
                            Text(stringResource(R.string.providers_models_select_all))
                        }
                        TextButton(onClick = { viewModel.selectAllModels(false) }) {
                            Text(stringResource(R.string.action_clear))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.providers_models_selected, state.selectedModelIds.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(state.fetchedModels, key = { it.remoteId }) { model ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = model.remoteId in state.selectedModelIds,
                                    onCheckedChange = { viewModel.toggleModelSelection(model.remoteId) },
                                )
                                Column {
                                    Text(
                                        text = model.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    if (model.displayName != model.remoteId) {
                                        Text(
                                            text = model.remoteId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (!state.isEditing) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.providers_models_pending),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::applyModelSelection, enabled = !state.isFetchingModels) {
                    Text(
                        stringResource(
                            if (state.isFetchingModels) R.string.providers_saving_models
                            else if (state.isEditing) R.string.providers_models_apply_save
                            else R.string.providers_models_apply
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissModelsDialog) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }
}
