package com.personalaiagent.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.domain.model.ChatMessage
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.MessageStatus
import com.personalaiagent.platform.Speaker
import com.personalaiagent.ui.components.AgentOrb
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.ConfirmDialog
import com.personalaiagent.ui.components.EmptyState
import com.personalaiagent.ui.components.ErrorBanner
import com.personalaiagent.ui.components.GradientButton
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.SectionCard
import com.personalaiagent.ui.components.Tag
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandSuccess
import com.personalaiagent.ui.theme.BrandTextSecondary
import com.personalaiagent.ui.theme.LocalBrand
import com.personalaiagent.ui.util.appErrorText

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenProviders: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenTaskLog: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWorkspace: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    val ttsEnabled by rememberUpdatedState(state.ttsEnabled)

    DisposableEffect(Unit) {
        speaker.initialize()
        onDispose { speaker.release() }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.SpeakAnswer -> if (ttsEnabled) speaker.speak(event.text)
            }
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.streamingText.length) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        BrandTopBar(
            title = stringResource(R.string.app_name),
            subtitle = if (state.hasProviders) {
                stringResource(R.string.chat_subtitle_ready)
            } else {
                stringResource(R.string.chat_subtitle_setup)
            },
            actions = {
                IconButton(onClick = onOpenModels) {
                    Icon(
                        Icons.Rounded.Memory,
                        contentDescription = stringResource(R.string.chat_model_picker),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.a11y_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_new)) },
                            leadingIcon = { Icon(Icons.Rounded.AddComment, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.newChat()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_workspace)) },
                            leadingIcon = { Icon(Icons.Rounded.Workspaces, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenWorkspace()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_task_log)) },
                            leadingIcon = { Icon(Icons.Rounded.History, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenTaskLog()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_title)) },
                            leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenSettings()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_delete_conversation)) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                            enabled = state.conversationId != null,
                            onClick = {
                                showMenu = false
                                confirmDelete = true
                            },
                        )
                    }
                }
            },
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> LoadingState(
                    text = stringResource(R.string.state_loading),
                    modifier = Modifier.align(Alignment.Center),
                )

                !state.hasProviders -> SetupProvidersCard(
                    onOpenProviders = onOpenProviders,
                    modifier = Modifier.align(Alignment.Center),
                )

                state.messages.isEmpty() -> EmptyChatState(modifier = Modifier.align(Alignment.Center))

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.messages.size, key = { state.messages[it].id }) { index ->
                        val message = state.messages[index]
                        val streaming = message.id == state.streamingMessageId
                        MessageBubble(
                            message = message,
                            streaming = streaming,
                            streamingText = state.streamingText,
                            onRetry = viewModel::retryLastMessage,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.workspaceName == null && state.messages.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Workspaces,
                    contentDescription = null,
                    tint = BrandCyan,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.chat_workspace_prompt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpenWorkspace) {
                    Text(stringResource(R.string.workspace_choose))
                }
            }
        }

        AnimatedVisibility(visible = state.toolsEnabled && state.isGenerating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.TaskAlt,
                    contentDescription = null,
                    tint = BrandCyan,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${stringResource(R.string.chat_tools_hint)} · ${state.workspaceName.orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandCyan,
                    maxLines = 1,
                )
            }
        }

        AnimatedVisibility(visible = state.toolActivity.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            ) {
                Text(
                    text = stringResource(R.string.chat_tools_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.toolActivity.takeLast(4).forEach { activity ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (activity.ok) Icons.Rounded.TaskAlt else Icons.Rounded.Error,
                            contentDescription = null,
                            tint = if (activity.ok) BrandSuccess else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${activity.toolName} ${activity.detail}".trim(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.error != null) {
            state.error?.let { error ->
                ErrorBanner(
                    message = appErrorText(error),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    onRetry = if (state.messages.any { it.role == MessageRole.USER }) {
                        viewModel::retryLastMessage
                    } else {
                        null
                    },
                    retryLabel = stringResource(R.string.action_retry),
                    onDismiss = viewModel::clearError,
                )
            }
        }

        Composer(
            state = state,
            onInputChange = viewModel::onInputChange,
            onSend = viewModel::send,
            onStop = viewModel::stopGenerating,
            onMic = onOpenVoice,
            onSelectModel = viewModel::selectModel,
            onOpenModels = onOpenModels,
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.chat_delete_conversation),
            body = stringResource(R.string.privacy_clear_chat_body),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = {
                confirmDelete = false
                viewModel.deleteCurrentConversation()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun SetupProvidersCard(onOpenProviders: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AgentOrb(size = 120.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.chat_setup_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.chat_setup_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        GradientButton(
            text = stringResource(R.string.chat_setup_action),
            onClick = onOpenProviders,
            modifier = Modifier.widthIn(max = 320.dp),
        )
    }
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AgentOrb(size = 116.dp)
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.chat_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.chat_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    streaming: Boolean,
    streamingText: String,
    onRetry: () -> Unit,
) {
    val isUser = message.role == MessageRole.USER
    val palette = LocalBrand.current
    val displayText = when {
        streaming && streamingText.isNotEmpty() -> streamingText
        else -> message.content
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            AgentOrb(size = 30.dp, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.width(10.dp))
        }
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (isUser) Color.Transparent else palette.bubbleAssistant,
                modifier = Modifier
                    .then(if (isUser) Modifier.background(palette.bubbleUser, MaterialTheme.shapes.large) else Modifier),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (!isUser) {
                        Text(
                            text = stringResource(R.string.chat_assistant),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandCyan,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    if (displayText.isEmpty() && streaming) {
                        TypingIndicator()
                    } else {
                        SelectionContainer {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (streaming) {
                            Spacer(Modifier.height(2.dp))
                            TypingIndicator(compact = true)
                        }
                    }
                }
            }

            if (message.status == MessageStatus.FAILED) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.chat_message_failed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (!streaming) {
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.chat_retry_failed), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator(compact: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "typing-alpha",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!compact) {
            Text(
                text = stringResource(R.string.chat_thinking),
                style = MaterialTheme.typography.bodySmall,
                color = BrandTextSecondary,
            )
        } else {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(BrandCyan.copy(alpha = alpha)),
            )
        }
    }
}

@Composable
private fun Composer(
    state: ChatUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onMic: () -> Unit,
    onSelectModel: (String) -> Unit,
    onOpenModels: () -> Unit,
) {
    var modelMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .clickable { modelMenu = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = BrandCyan,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = state.activeModel?.displayName
                                ?: stringResource(R.string.chat_no_models),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Rounded.ArrowDropDown,
                            contentDescription = stringResource(R.string.a11y_current_model),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                        state.models.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(model.displayName, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            model.providerName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (model.id == state.activeModel?.id) {
                                        Tag(text = stringResource(R.string.models_active))
                                    }
                                },
                                onClick = {
                                    modelMenu = false
                                    onSelectModel(model.id)
                                },
                            )
                        }
                        if (state.models.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.models_empty_title)) },
                                onClick = {
                                    modelMenu = false
                                    onOpenModels()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_providers)) },
                            leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                            onClick = {
                                modelMenu = false
                                onOpenModels()
                            },
                        )
                    }
                }

                Spacer(Modifier.weight(1f))
                state.activeModel?.let { model ->
                    Text(
                        text = model.providerName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.activeModel == null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onOpenModels() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.chat_no_models),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                IconButton(onClick = onMic) {
                    Icon(
                        Icons.Rounded.Mic,
                        contentDescription = stringResource(R.string.a11y_mic),
                        tint = BrandCyan,
                    )
                }
                TextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.large),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.chat_input_hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    maxLines = 5,
                    shape = MaterialTheme.shapes.large,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                if (state.isGenerating) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                    ) {
                        Icon(
                            Icons.Rounded.Stop,
                            contentDescription = stringResource(R.string.chat_stop_generating),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSend,
                        enabled = state.canSend,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.canSend) LocalBrand.current.accentGradient
                                else androidx.compose.ui.graphics.SolidColor(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ),
                    ) {
                        if (state.isGenerating) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(R.string.a11y_send),
                                tint = if (state.canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
