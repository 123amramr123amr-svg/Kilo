package com.personalaiagent.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }
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
                    subtitle = if (state.settings.theme == ThemeMode.AMOLED) {
                        stringResource(R.string.settings_theme_amoled)
                    } else {
                        stringResource(R.string.settings_theme_midnight)
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
                    icon = androidx.compose.material.icons.Icons.Rounded.Cloud,
                    title = stringResource(R.string.settings_providers),
                    subtitle = stringResource(R.string.settings_providers_desc),
                    onClick = onOpenProviders,
                    trailing = {
                        Tag(text = "${state.providerCount}", color = BrandCyan)
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
                                text = if (theme == ThemeMode.AMOLED) {
                                    stringResource(R.string.settings_theme_amoled)
                                } else {
                                    stringResource(R.string.settings_theme_midnight)
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
