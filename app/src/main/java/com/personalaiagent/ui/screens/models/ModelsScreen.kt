package com.personalaiagent.ui.screens.models

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.personalaiagent.R
import com.personalaiagent.domain.model.AiModel
import com.personalaiagent.domain.model.ModelCapability
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.ChoiceChip
import com.personalaiagent.ui.components.EmptyState
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.Tag
import com.personalaiagent.ui.theme.BrandBlue
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandPurple
import com.personalaiagent.ui.theme.BrandSuccess
import com.personalaiagent.ui.theme.LocalBrand
import com.personalaiagent.ui.util.Formatters

@Composable
fun ModelsScreen(
    onOpenModel: (String) -> Unit,
    onOpenProviders: () -> Unit,
    viewModel: ModelsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = stringResource(R.string.models_title),
            subtitle = "${state.models.size} · ${state.models.count { it.enabled }} ${stringResource(R.string.models_enabled)}",
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.models_search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { }),
        )

        Spacer(Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val filters = listOf(
                ModelFilter.ALL to stringResource(R.string.models_filter_all),
                ModelFilter.ENABLED to stringResource(R.string.models_enabled),
                ModelFilter.VISION to stringResource(R.string.models_cap_vision),
                ModelFilter.TOOLS to stringResource(R.string.models_cap_tools),
            )
            items(filters) { (filter, label) ->
                ChoiceChip(
                    label = label,
                    selected = state.filter == filter,
                    onClick = { viewModel.onFilterChange(filter) },
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        when {
            state.isLoading -> LoadingState(stringResource(R.string.state_loading))

            state.isEmpty && !state.hasProviders -> EmptyState(
                icon = Icons.Rounded.Memory,
                title = stringResource(R.string.models_empty_title),
                body = stringResource(R.string.models_empty_body),
                actionLabel = stringResource(R.string.providers_add),
                onAction = onOpenProviders,
            )

            state.isEmpty -> EmptyState(
                icon = Icons.Rounded.Search,
                title = stringResource(R.string.state_empty),
                body = stringResource(R.string.models_empty_body),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.filtered, key = { it.id }) { model ->
                    ModelCard(
                        model = model,
                        onClick = { onOpenModel(model.id) },
                        onToggle = { enabled -> viewModel.setEnabled(model.id, enabled) },
                        onSetDefault = { viewModel.setDefault(model.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: AiModel,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onSetDefault: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (model.isDefault) BrandCyan.copy(alpha = 0.55f) else LocalBrand.current.cardBorder,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (model.enabled) LocalBrand.current.accentGradientSoft
                            else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = if (model.enabled) BrandCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = model.providerName.ifBlank { stringResource(R.string.models_provider_missing) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onSetDefault) {
                    Icon(
                        imageVector = if (model.isDefault) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = stringResource(R.string.models_set_default),
                        tint = if (model.isDefault) BrandCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = model.enabled,
                    onCheckedChange = onToggle,
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (model.isDefault) {
                    Tag(text = stringResource(R.string.models_default), color = BrandCyan)
                }
                model.capabilities.forEach { capability ->
                    Tag(
                        text = capability.label(),
                        color = when (capability) {
                            ModelCapability.VISION -> BrandPurple
                            ModelCapability.TOOLS -> BrandBlue
                            ModelCapability.REASONING -> BrandSuccess
                            ModelCapability.CODE -> BrandCyan
                            ModelCapability.TEXT -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                model.contextWindow?.let {
                    Text(
                        text = stringResource(R.string.models_context, Formatters.compactNumber(it)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelCapability.label(): String = when (this) {
    ModelCapability.TEXT -> stringResource(R.string.models_cap_text)
    ModelCapability.VISION -> stringResource(R.string.models_cap_vision)
    ModelCapability.TOOLS -> stringResource(R.string.models_cap_tools)
    ModelCapability.REASONING -> stringResource(R.string.models_cap_reasoning)
    ModelCapability.CODE -> stringResource(R.string.models_cap_code)
}
