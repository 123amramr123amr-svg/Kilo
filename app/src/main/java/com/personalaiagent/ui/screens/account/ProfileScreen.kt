package com.personalaiagent.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.ErrorBanner
import com.personalaiagent.ui.components.GradientButton
import com.personalaiagent.ui.components.InfoBanner
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.SectionCard
import com.personalaiagent.ui.theme.BrandCyan

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedMessage = stringResource(R.string.account_profile_saved)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEvent.Saved -> snackbarHostState.showSnackbar(savedMessage)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = stringResource(R.string.account_profile),
            subtitle = stringResource(R.string.account_local_badge),
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                OutlinedTextField(
                    value = state.nameDraft,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.account_display_name)) },
                    placeholder = { Text(stringResource(R.string.account_display_name_hint)) },
                    singleLine = true,
                    isError = state.nameError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                if (state.nameError) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.account_name_required),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.account_display_name_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            InfoBanner(
                icon = Icons.Rounded.Lock,
                title = stringResource(R.string.account_no_cloud_title),
                body = stringResource(R.string.account_no_cloud_body),
                tint = BrandCyan,
            )

            InfoBanner(
                icon = Icons.Rounded.Badge,
                title = stringResource(R.string.account_local_badge),
                body = stringResource(R.string.account_profile_desc),
                tint = BrandCyan,
            )

            GradientButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::save,
                loading = state.isSaving,
                enabled = state.isDirty,
            )
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}
