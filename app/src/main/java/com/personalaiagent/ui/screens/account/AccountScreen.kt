package com.personalaiagent.ui.screens.account

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.personalaiagent.BuildConfig
import com.personalaiagent.R
import com.personalaiagent.domain.model.AppSettings
import com.personalaiagent.ui.components.AvatarBadge
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.ConfirmDialog
import com.personalaiagent.ui.components.DetailRow
import com.personalaiagent.ui.components.GhostButton
import com.personalaiagent.ui.components.InfoBanner
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.SectionCard
import com.personalaiagent.ui.components.SectionHeader
import com.personalaiagent.ui.components.SettingsRow
import com.personalaiagent.ui.components.Tag
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandSuccess
import com.personalaiagent.ui.theme.LocalBrand
import com.personalaiagent.ui.util.Formatters

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenApiKeys: () -> Unit,
    onOpenBackup: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AccountEvent.SignedOut -> onSignedOut()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = stringResource(R.string.account_title),
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
                .padding(bottom = 28.dp),
        ) {
            SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarBadge(
                        initials = Formatters.initials(
                            state.settings.displayName.ifBlank { stringResource(R.string.account_local_badge) },
                        ),
                        size = 56.dp,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = state.settings.displayName.ifBlank {
                                stringResource(R.string.account_local_badge)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Tag(text = stringResource(R.string.account_local_badge), color = BrandCyan)
                    }
                }
                Spacer(Modifier.height(14.dp))
                DetailRow(
                    label = stringResource(R.string.tasks_model),
                    value = "${state.modelCount} · ${state.providerCount}",
                )
                DetailRow(
                    label = stringResource(R.string.account_version, ""),
                    value = BuildConfig.VERSION_NAME,
                )
                if (state.settings.createdAt > 0) {
                    DetailRow(
                        label = stringResource(R.string.account_since, ""),
                        value = Formatters.date(state.settings.createdAt),
                    )
                }
            }

            SectionHeader(stringResource(R.string.settings_account))
            SectionCard {
                SettingsRow(
                    icon = Icons.Rounded.Person,
                    title = stringResource(R.string.account_profile),
                    subtitle = stringResource(R.string.account_profile_desc),
                    onClick = onOpenProfile,
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.account_privacy),
                    subtitle = stringResource(R.string.account_privacy_desc),
                    onClick = onOpenPrivacy,
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.Key,
                    title = stringResource(R.string.account_api_keys),
                    subtitle = stringResource(R.string.account_api_keys_desc),
                    onClick = onOpenApiKeys,
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.Backup,
                    title = stringResource(R.string.account_backup),
                    subtitle = stringResource(R.string.account_backup_desc),
                    onClick = onOpenBackup,
                )
                com.personalaiagent.ui.components.Divider()
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.account_about),
                    subtitle = stringResource(R.string.settings_version_row) + " " + BuildConfig.VERSION_NAME,
                    onClick = null,
                    iconTint = BrandSuccess,
                )
            }

            Spacer(Modifier.height(14.dp))
            InfoBanner(
                icon = Icons.Rounded.Security,
                title = stringResource(R.string.account_no_cloud_title),
                body = stringResource(R.string.account_no_cloud_body),
            )

            Spacer(Modifier.height(18.dp))
            GhostButton(
                text = stringResource(R.string.account_sign_out),
                onClick = { confirmSignOut = true },
                icon = Icons.Rounded.Logout,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.account_sign_out_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    if (confirmSignOut) {
        ConfirmDialog(
            title = stringResource(R.string.account_sign_out_title),
            body = stringResource(R.string.account_sign_out_body),
            confirmLabel = stringResource(R.string.account_sign_out_confirm),
            dismissLabel = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = {
                confirmSignOut = false
                viewModel.signOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
}
