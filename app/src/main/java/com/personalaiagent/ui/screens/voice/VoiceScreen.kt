package com.personalaiagent.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.ui.components.AgentOrb
import com.personalaiagent.ui.components.ErrorBanner
import com.personalaiagent.ui.components.GhostButton
import com.personalaiagent.ui.components.GradientButton
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandTextSecondary
import com.personalaiagent.ui.theme.LocalBrand

@Composable
fun VoiceScreen(
    languageTag: String,
    onBack: () -> Unit,
    onResult: (String) -> Unit,
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.start(languageTag) else viewModel.onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.start(languageTag) else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.voice_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                state.isListening -> stringResource(R.string.voice_listening)
                state.isProcessing -> stringResource(R.string.voice_processing)
                state.transcript.isNotBlank() -> stringResource(R.string.voice_result_hint)
                else -> stringResource(R.string.voice_hint)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))
        AgentOrb(size = 190.dp, listening = state.isListening)
        Spacer(Modifier.height(6.dp))
        Waveform(active = state.isListening)
        Spacer(Modifier.height(24.dp))

        Text(
            text = state.transcript.ifBlank { "…" },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        state.error?.let { error ->
            Spacer(Modifier.height(16.dp))
            ErrorBanner(
                message = stringResource(
                    when (error) {
                        VoiceError.PERMISSION_REQUIRED -> R.string.voice_permission_required
                        VoiceError.UNAVAILABLE -> R.string.voice_unavailable
                        VoiceError.NO_MATCH, VoiceError.FAILED -> R.string.voice_error
                    },
                ),
            )
        }

        Spacer(Modifier.weight(1f))

        GradientButton(
            text = stringResource(R.string.action_send),
            onClick = { onResult(state.transcript) },
            enabled = state.canSend,
            icon = Icons.Rounded.Send,
        )
        Spacer(Modifier.height(10.dp))
        GhostButton(
            text = if (state.isListening) {
                stringResource(R.string.voice_stop)
            } else {
                stringResource(R.string.voice_retry)
            },
            onClick = {
                if (state.isListening) {
                    viewModel.stopListening()
                } else {
                    viewModel.reset()
                    viewModel.start(languageTag)
                }
            },
            icon = Icons.Rounded.Mic,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Waveform(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val bars = 21
    Row(
        modifier = Modifier.height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(bars) { index ->
            val phase = (index % 7) / 7f
            val height by transition.animateFloat(
                initialValue = 6f + phase * 26f,
                targetValue = 38f - phase * 18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420 + index * 30, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar$index",
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((if (active) height else 6f).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (active) BrandCyan else MaterialTheme.colorScheme.outline),
            )
        }
    }
}
