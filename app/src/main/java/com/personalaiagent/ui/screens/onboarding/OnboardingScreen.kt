package com.personalaiagent.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.domain.model.AppLanguage
import com.personalaiagent.ui.components.AgentOrb
import com.personalaiagent.ui.components.GradientButton
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandPurple
import com.personalaiagent.ui.theme.LocalBrand
import androidx.compose.ui.res.stringResource

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        AgentOrb(size = 150.dp)

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_headline),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_subheadline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.onboarding_language_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LanguageCard(
                title = stringResource(R.string.language_arabic),
                subtitle = stringResource(R.string.language_name_arabic),
                glyph = "ع",
                selected = state.language == AppLanguage.ARABIC,
                onSelect = { viewModel.selectLanguage(AppLanguage.ARABIC) },
                modifier = Modifier.weight(1f),
            )
            LanguageCard(
                title = stringResource(R.string.language_english),
                subtitle = stringResource(R.string.language_name_english),
                glyph = "A",
                selected = state.language == AppLanguage.ENGLISH,
                onSelect = { viewModel.selectLanguage(AppLanguage.ENGLISH) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.onboarding_language_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(26.dp))
        FeatureLine(Icons.Rounded.SmartToy, stringResource(R.string.onboarding_feature_chat))
        FeatureLine(Icons.Rounded.RecordVoiceOver, stringResource(R.string.onboarding_feature_voice))
        FeatureLine(Icons.Rounded.Translate, stringResource(R.string.onboarding_feature_models))
        FeatureLine(Icons.Rounded.Lock, stringResource(R.string.onboarding_privacy_body), isBody = true)

        Spacer(Modifier.height(26.dp))
        GradientButton(
            text = stringResource(R.string.action_start),
            onClick = {
                viewModel.complete()
                onFinished()
            },
            enabled = !state.isSaving,
        )
        TextButton(onClick = {
            viewModel.complete()
            onFinished()
        }) {
            Text(stringResource(R.string.action_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = stringResource(R.string.onboarding_terms),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LanguageCard(
    title: String,
    subtitle: String,
    glyph: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrand.current
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .then(
                if (selected) Modifier.background(palette.accentGradientSoft)
                    .border(1.5.dp, BrandCyan.copy(alpha = 0.7f), MaterialTheme.shapes.large)
                else Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large),
            )
            .selectable(selected = selected, onClick = onSelect)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (selected) BrandPurple.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun FeatureLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isBody: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = if (isBody) Icons.Rounded.Lock else icon,
            contentDescription = null,
            tint = BrandCyan.copy(alpha = 0.9f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = if (isBody) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = if (isBody) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}
