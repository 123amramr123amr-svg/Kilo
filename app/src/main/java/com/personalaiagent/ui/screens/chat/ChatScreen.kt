package com.personalaiagent.ui.screens.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.domain.model.Attachment
import com.personalaiagent.domain.model.ChatMessage
import com.personalaiagent.domain.model.DraftReview
import com.personalaiagent.domain.model.MessageRole
import com.personalaiagent.domain.model.MessageStatus
import com.personalaiagent.platform.Speaker
import com.personalaiagent.ui.components.ConfirmDialog
import com.personalaiagent.ui.components.GhostButton
import com.personalaiagent.ui.components.GradientButton
import com.personalaiagent.ui.components.RichText
import com.personalaiagent.ui.components.SectionCard
import com.personalaiagent.ui.components.Tag
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.LoadingState
import com.personalaiagent.ui.components.EmptyState
import com.personalaiagent.ui.components.ErrorBanner
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandSuccess
import com.personalaiagent.ui.theme.BrandWarning
import com.personalaiagent.ui.theme.LocalBrand
import com.personalaiagent.ui.util.appErrorText
import com.personalaiagent.domain.usecase.agent.ToolActivityUi
import com.personalaiagent.domain.model.AiModel
import kotlinx.coroutines.launch

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

    // Images and PDFs: both are rasterised on device and sent to vision models.
    val attachLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::attach) }

    // Follow the conversation without fighting the user. A new message scrolls
    // smoothly; streamed text only jumps when the user is already at the bottom,
    // and never restarts an animation on every token (that was the stutter).
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }
    LaunchedEffect(state.streamingText) {
        if (state.streamingText.isEmpty() || state.messages.isEmpty()) return@LaunchedEffect
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (lastVisible >= state.messages.lastIndex - 1) {
            listState.scrollToItem(state.messages.lastIndex)
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
                                viewModel.requestNewChat()
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

        // Always reachable while a run is active — even if the user scrolled away.
        AnimatedVisibility(visible = state.isGenerating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                    .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.chat_running_background),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::stopGenerating) {
                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.chat_stop_generating))
                }
            }
        }

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

                state.messages.isEmpty() -> EmptyChatState(
                    modifier = Modifier.align(Alignment.Center),
                    onPickModel = viewModel::openModelPicker,
                    onManageModels = onOpenModels,
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.messages.size, key = { state.messages[it].id }) { index ->
                        val message = state.messages[index]
                        val streaming = message.id == state.streamingMessageId
                        Box(modifier = Modifier.animateItem()) {
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
        }

        if (state.plan.isNotEmpty()) {
            PlanCard(plan = state.plan, running = state.isGenerating)
        }

        // The arrow the user asked for: one tap shows what the agent is doing
        // (searching, reading, writing) and where it is doing it.
        AgentActivityPanel(
            activities = state.toolActivity,
            plan = state.plan,
            running = state.isGenerating,
        )

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
            onAttach = {
                attachLauncher.launch(arrayOf("image/*", "application/pdf"))
            },
            onRemoveAttachment = viewModel::removeAttachment,
            onToggleReview = viewModel::toggleReviewFirst,
        )
    }

    if (state.showModelPicker) {
        ModelPickerDialog(
            models = state.models,
            selectedId = state.activeModel?.id,
            startingNewChat = state.pickerStartsNewChat,
            onPick = viewModel::chooseModel,
            onDismiss = viewModel::dismissModelPicker,
        )
    }

    state.review?.let { review ->
        ReviewDialog(
            review = review,
            input = state.input,
            answers = state.reviewAnswers,
            onAnswer = viewModel::answerQuestion,
            onConfirm = viewModel::confirmReview,
            onSendAsIs = viewModel::sendWithoutReview,
            onDismiss = viewModel::dismissReview,
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

// --------------------------------------------------------------------- pieces

@Composable
private fun SetupProvidersCard(onOpenProviders: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
private fun EmptyChatState(
    modifier: Modifier = Modifier,
    onPickModel: () -> Unit,
    onManageModels: () -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = Icons.Rounded.Lightbulb,
            title = stringResource(R.string.chat_empty_title),
            body = stringResource(R.string.chat_empty_body),
            actionLabel = stringResource(R.string.chat_empty_choose_model),
            onAction = onPickModel,
        )
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onManageModels) {
            Text(stringResource(R.string.chat_model_picker), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * "What is the agent doing?" — a single compact row with an arrow. Collapsed it
 * shows the current step; tapping it reveals the plan and every step taken, with
 * the file, query or app it touched.
 */
@Composable
private fun AgentActivityPanel(
    activities: List<ToolActivityUi>,
    plan: List<String>,
    running: Boolean,
) {
    if (activities.isEmpty() && plan.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }

    // Open by itself when real work starts, so the user sees activity without
    // having to look for it; they can collapse it at any time.
    LaunchedEffect(running, activities.size) {
        if (running && activities.isNotEmpty()) expanded = true
    }
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "agent-arrow")
    val latest = activities.lastOrNull()
    val latestLabel = if (latest == null) {
        stringResource(R.string.agent_activity_empty)
    } else {
        val label = toolLabel(latest.toolName)
        if (latest.detail.isBlank()) label else "$label · ${latest.detail}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (running) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(15.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    Icons.Rounded.TaskAlt,
                    contentDescription = null,
                    tint = BrandSuccess,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (running) R.string.agent_activity_running else R.string.agent_activity_done,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = latestLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (activities.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.agent_activity_steps, activities.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = stringResource(
                    if (expanded) R.string.agent_activity_hide else R.string.agent_activity_show,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).rotate(arrow),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (plan.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.agent_activity_plan),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    plan.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp, top = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                activities.forEach { activity ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = if (activity.ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                            contentDescription = stringResource(
                                if (activity.ok) R.string.chat_tool_done else R.string.chat_tool_failed,
                            ),
                            tint = if (activity.ok) BrandSuccess else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = toolLabel(activity.toolName),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (activity.detail.isNotBlank()) {
                                Text(
                                    text = activity.detail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Human label for an agent tool: "Reading a file", not "read_file". */
@Composable
private fun toolLabel(toolName: String): String = stringResource(
    when (toolName) {
        "list_files" -> R.string.tool_list_files
        "read_file" -> R.string.tool_read_file
        "write_file" -> R.string.tool_write_file
        "append_file" -> R.string.tool_append_file
        "create_folder" -> R.string.tool_create_folder
        "delete_path" -> R.string.tool_delete_path
        "list_apps" -> R.string.tool_list_apps
        "open_app" -> R.string.tool_open_app
        "web_search" -> R.string.tool_web_search
        "open_url" -> R.string.tool_open_url
        "generate_image" -> R.string.tool_generate_image
        else -> R.string.tool_generic
    },
)

/** The "which model?" sheet: shown before a new chat and when switching mid-chat. */
@Composable
private fun ModelPickerDialog(
    models: List<AiModel>,
    selectedId: String?,
    startingNewChat: Boolean,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_model_pick_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(
                        if (startingNewChat) R.string.chat_model_pick_new_body else R.string.chat_model_pick_switch_body,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(models, key = { it.id }) { model ->
                        val active = model.id == selectedId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                    else Color.Transparent,
                                )
                                .clickable { onPick(model.id) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = model.providerName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (model.isDefault) {
                                Tag(text = stringResource(R.string.chat_model_default_tag), color = BrandCyan)
                            } else if (active) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = stringResource(R.string.models_active),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
    )
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
        Column(
            modifier = Modifier.widthIn(max = 340.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (isUser) Color.Transparent else palette.bubbleAssistant,
                modifier = Modifier.then(
                    if (isUser) Modifier.background(palette.bubbleUser, MaterialTheme.shapes.large) else Modifier,
                ),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (displayText.isEmpty() && streaming) {
                        TypingIndicator()
                    } else {
                        SelectionContainer {
                            if (isUser) {
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )
                            } else {
                                RichText(text = displayText, color = MaterialTheme.colorScheme.onSurface)
                            }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun AttachmentChip(attachment: Attachment, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.AttachFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = attachment.sourceLabel ?: attachment.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.widthIn(max = 160.dp),
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.action_remove),
                modifier = Modifier.size(14.dp),
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
    onAttach: () -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    onToggleReview: () -> Unit,
) {
    var modelMenu by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Every connected model, one tap away — right above the input.
            if (state.models.isNotEmpty()) {
                val selectedLabel = stringResource(R.string.chat_models_connected)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    item {
                        Text(
                            text = selectedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 2.dp),
                        )
                    }
                    items(state.models, key = { it.id }) { model ->
                        val active = model.id == state.activeModel?.id
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { onSelectModel(model.id) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (active) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            if (state.attachments.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.attachments.size) { index ->
                        AttachmentChip(
                            attachment = state.attachments[index],
                            onRemove = { onRemoveAttachment(index) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAttach, enabled = !state.isAttaching) {
                    if (state.isAttaching) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(
                            Icons.Rounded.AttachFile,
                            contentDescription = stringResource(R.string.chat_attach),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (state.reviewFirst) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .clickable(onClick = onToggleReview)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = if (state.reviewFirst) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.chat_review_toggle),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.reviewFirst) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { modelMenu = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.activeModel?.displayName ?: stringResource(R.string.chat_no_models),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 110.dp),
                        )
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
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.models_title)) },
                            leadingIcon = { Icon(Icons.Rounded.Memory, contentDescription = null) },
                            onClick = {
                                modelMenu = false
                                onOpenModels()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
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
                    maxLines = 6,
                    shape = MaterialTheme.shapes.large,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.16f)),
                    ) {
                        Icon(
                            Icons.Rounded.Stop,
                            contentDescription = stringResource(R.string.chat_stop_generating),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                } else if (state.isReviewing) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
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
                                else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant),
                            ),
                    ) {
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

/**
 * The "before we start" sheet: how clear the request is, what the agent plans to
 * do, and up to three questions whose answers make the result better.
 */
@Composable
private fun ReviewDialog(
    review: DraftReview,
    input: String,
    answers: Map<Int, String>,
    onAnswer: (Int, String) -> Unit,
    onConfirm: () -> Unit,
    onSendAsIs: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = BrandWarning)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.review_title))
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.review_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (review.plan.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.chat_plan_title),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    review.plan.forEachIndexed { index, step ->
                        Row(modifier = Modifier.padding(top = 2.dp)) {
                            Text("${index + 1}.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(step, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (review.questions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.review_questions_title),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    review.questions.forEachIndexed { index, question ->
                        Spacer(Modifier.height(8.dp))
                        Text(question.question, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(question.options) { option ->
                                val selected = answers[index] == option
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                        .clickable { onAnswer(index, option) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }

                if (review.improvedPrompt.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.review_improved_title),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = review.improvedPrompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (review.improvedPrompt.isBlank() && input.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(input, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.review_start))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSendAsIs) {
                    Text(stringResource(R.string.review_send_as_is))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    )
}
