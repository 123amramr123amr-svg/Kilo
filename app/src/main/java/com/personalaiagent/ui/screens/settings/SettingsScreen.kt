package com.personalaiagent.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.BuildConfig
import com.personalaiagent.R
import com.personalaiagent.domain.model.AppLanguage
import com.personalaiagent.domain.model.ThemeMode
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.SectionCard
import com.personalaiagent.ui.components.SectionHeader
import com.personalaiagent.ui.components.SettingsRow
import com.personalaiagent.ui.components.Tag
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandPurple
import com.personalaiagent.ui.theme.BrandSuccess

@Composable
fun SettingsScreen(
    onOpenProviders: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenTaskLog: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenWorkspace: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSkillDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.account_version, BuildConfig.VERSION_NAME),
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
        ) {
            SectionHeader(stringResource(R.string.settings_general))
            SectionCard {
                SettingsRow(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = if (state.settings.language == AppLanguage.ARABIC) {
                        stringResource(R.string.language_name_arabic)
                    } else {
                        stringResource(R.string.language_name_english)
                    },
                    onClick = { showLanguageDialog = true },
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.DarkMode,
                    title = stringResource(R.string.settings_theme),
                    subtitle = when (state.settings.theme) {
                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        ThemeMode.MIDNIGHT -> stringResource(R.string.settings_theme_midnight)
                        ThemeMode.AMOLED -> stringResource(R.string.settings_theme_amoled)
                    },
                    onClick = { showThemeDialog = true },
                )
            }

            SectionHeader(stringResource(R.string.settings_ai))
            SectionCard {
                SettingsRow(
                    icon = Icons.Rounded.Memory,
                    title = stringResource(R.string.settings_default_model),
                    subtitle = state.defaultModel?.displayName
                        ?: stringResource(R.string.chat_no_models),
                    onClick = onOpenModels,
                    trailing = {
                        Tag(
                            text = "${state.modelCount}",
                            color = BrandPurple,
                        )
                    },
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.Cloud,
                    title = stringResource(R.string.settings_providers),
                    subtitle = stringResource(R.string.settings_providers_desc),
                    onClick = onOpenProviders,
                    trailing = {
                        Tag(text = "${state.providerCount}", color = BrandCyan)
                    },
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.Workspaces,
                    title = stringResource(R.string.settings_workspace),
                    subtitle = state.settings.workspaceName
                        ?: stringResource(R.string.settings_workspace_desc),
                    onClick = onOpenWorkspace,
                    trailing = {
                        Switch(
                            checked = state.settings.agentFileToolsEnabled,
                            onCheckedChange = viewModel::setAgentFileToolsEnabled,
                        )
                    },
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.settings_review),
                    subtitle = stringResource(R.string.settings_review_body),
                    onClick = null,
                    trailing = {
                        Switch(
                            checked = state.settings.reviewRequests,
                            onCheckedChange = viewModel::setReviewRequests,
                        )
                    },
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.Visibility,
                    title = stringResource(R.string.settings_agent_activity),
                    subtitle = stringResource(R.string.settings_agent_activity_body),
                    onClick = null,
                    trailing = {
                        Switch(
                            checked = state.settings.showAgentActivity,
                            onCheckedChange = viewModel::setShowAgentActivity,
                        )
                    },
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.Bolt,
                    title = stringResource(R.string.settings_streaming),
                    subtitle = stringResource(R.string.settings_streaming_body),
                    onClick = null,
                    trailing = {
                        Switch(
                            checked = state.settings.streamingEnabled,
                            onCheckedChange = viewModel::setStreamingEnabled,
                        )
                    },
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.RecordVoiceOver,
                    title = stringResource(R.string.settings_tts),
                    subtitle = stringResource(R.string.settings_tts_body),
                    onClick = null,
                    trailing = {
                        Switch(
                            checked = state.settings.ttsEnabled,
                            onCheckedChange = viewModel::setTtsEnabled,
                        )
                    },
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.Vibration,
                    title = stringResource(R.string.settings_haptics),
                    subtitle = stringResource(R.string.settings_haptics_body),
                    onClick = null,
                    trailing = {
                        Switch(
                            checked = state.settings.hapticsEnabled,
                            onCheckedChange = viewModel::setHapticsEnabled,
                        )
                    },
                )
            }

            SectionHeader(stringResource(R.string.settings_security))
            SectionCard {
                SettingsRow(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.settings_permissions),
                    subtitle = stringResource(R.string.settings_permissions_desc),
                    onClick = onOpenPermissions,
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.settings_task_log),
                    subtitle = stringResource(R.string.settings_task_log_desc),
                    onClick = onOpenTaskLog,
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.AccountCircle,
                    title = stringResource(R.string.settings_account),
                    subtitle = stringResource(R.string.settings_account_desc),
                    onClick = onOpenAccount,
                )
            }

            SectionHeader(stringResource(R.string.settings_skills))
            SectionCard {
                state.skills.forEachIndexed { index, skill ->
                    if (index > 0) com.personalaiagent.ui.components.Divider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = skill.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (skill.description.isNotBlank()) {
                                Text(
                                    text = skill.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.deleteSkill(skill.name) }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                if (state.skills.isNotEmpty()) com.personalaiagent.ui.components.Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSkillDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_skill_add),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_skills_body),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp),
            )

            SectionHeader(stringResource(R.string.settings_about))
            SectionCard {
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_version_row),
                    subtitle = BuildConfig.VERSION_NAME,
                    onClick = null,
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.account_no_cloud_title),
                    subtitle = stringResource(R.string.account_no_cloud_body),
                    onClick = null,
                    iconTint = BrandSuccess,
                )
            }
        }
    }

    if (showSkillDialog) {
        AddSkillDialog(
            onDismiss = { showSkillDialog = false },
            onSave = { name, description, body ->
                showSkillDialog = false
                viewModel.addSkill(name, description, body)
            },
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language_dialog)) },
            text = {
                Column {
                    AppLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            RadioButton(
                                selected = state.settings.language == language,
                                onClick = {
                                    showLanguageDialog = false
                                    viewModel.setLanguage(language)
                                },
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.ARABIC) {
                                    stringResource(R.string.language_name_arabic)
                                } else {
                                    stringResource(R.string.language_name_english)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme_dialog)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = state.settings.theme == theme,
                                onClick = {
                                    showThemeDialog = false
                                    viewModel.setTheme(theme)
                                },
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = when (theme) {
                                    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                    ThemeMode.MIDNIGHT -> stringResource(R.string.settings_theme_midnight)
                                    ThemeMode.AMOLED -> stringResource(R.string.settings_theme_amoled)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }
}

/** Writes a real `filesDir/skills/<name>/SKILL.md`, the same shape as the bundled ones. */
@Composable
private fun AddSkillDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, body: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_skill_add)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.settings_skill_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.settings_skill_description)) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(stringResource(R.string.settings_skill_body)) },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, description, body) },
                enabled = name.isNotBlank() && body.isNotBlank(),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
