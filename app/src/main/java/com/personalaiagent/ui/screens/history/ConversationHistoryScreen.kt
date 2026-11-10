package com.personalaiagent.ui.screens.history

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.R
import com.personalaiagent.domain.model.ConversationSummary
import com.personalaiagent.ui.components.BrandTopBar
import com.personalaiagent.ui.components.EmptyState
import com.personalaiagent.ui.components.LoadingState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The conversation list: one card per chat, newest (and pinned) first.
 *
 * This is the "go back to an old conversation" the chat screen was missing: the
 * title is the first thing you asked, the preview is the last thing that was
 * said, and a long press on the chips renames, pins or deletes.
 */
@Composable
fun ConversationHistoryScreen(
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            title = stringResource(R.string.history_title),
            subtitle = stringResource(R.string.history_subtitle),
            onBack = onBack,
            backContentDescription = stringResource(R.string.cd_back),
            actions = {
                IconButton(onClick = onNewChat) {
                    Icon(
                        Icons.Rounded.AddComment,
                        contentDescription = stringResource(R.string.history_new_chat),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )

        SearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
        )

        when {
            state.loading -> LoadingState(
                text = stringResource(R.string.state_loading),
                modifier = Modifier.fillMaxSize(),
            )

            state.isEmpty -> EmptyState(
                icon = Icons.Rounded.AddComment,
                title = if (state.isSearching) {
                    stringResource(R.string.history_empty_search, state.query)
                } else {
                    stringResource(R.string.history_empty_title)
                },
                body = stringResource(R.string.history_empty_body),
                actionLabel = if (state.isSearching) null else stringResource(R.string.history_new_chat),
                onAction = if (state.isSearching) null else onNewChat,
                modifier = Modifier.fillMaxSize(),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.pinned.isNotEmpty()) {
                    item(key = "pinned-header") {
                        SectionLabel(stringResource(R.string.history_pinned))
                    }
                    items(state.pinned, key = { "pin-${it.id}" }) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            pinned = true,
                            onOpen = { onOpenConversation(conversation.id) },
                            onTogglePin = { viewModel.togglePin(conversation) },
                            onRename = { viewModel.requestRename(conversation) },
                            onDelete = { viewModel.requestDelete(conversation) },
                        )
                    }
                }
                if (state.others.isNotEmpty()) {
                    item(key = "others-header") {
                        SectionLabel(
                            stringResource(R.string.history_recent),
                        )
                    }
                    items(state.others, key = { "conv-${it.id}" }) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            pinned = false,
                            onOpen = { onOpenConversation(conversation.id) },
                            onTogglePin = { viewModel.togglePin(conversation) },
                            onRename = { viewModel.requestRename(conversation) },
                            onDelete = { viewModel.requestDelete(conversation) },
                        )
                    }
                }
            }
        }
    }

    state.renaming?.let { conversation ->
        RenameDialog(
            initial = conversation.title,
            onDismiss = viewModel::dismissRename,
            onConfirm = viewModel::commitRename,
        )
    }

    state.deleting?.let { conversation ->
        DeleteDialog(
            title = conversation.title,
            onDismiss = viewModel::dismissDelete,
            onConfirm = viewModel::confirmDelete,
        )
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp)),
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.history_search_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ConversationCard(
    conversation: ConversationSummary,
    pinned: Boolean,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pinned) {
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = stringResource(R.string.history_pinned),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        text = conversation.title.ifBlank {
                            stringResource(R.string.chat_untitled_conversation)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (conversation.preview.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = conversation.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = relativeTime(conversation.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (conversation.messageCount == 1) {
                            stringResource(R.string.history_message_count_one)
                        } else {
                            stringResource(R.string.history_message_count, conversation.messageCount)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.a11y_more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (pinned) R.string.history_unpin else R.string.history_pin,
                                ),
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                        onClick = {
                            menu = false
                            onTogglePin()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_rename)) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menu = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_delete)) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menu = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_rename_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(80) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.history_rename_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun DeleteDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_delete_title)) },
        text = { Text(stringResource(R.string.history_delete_body, title)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.history_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** "Just now" / "12 min ago" / "3 h ago" / "5 d ago" / "12 Mar". */
@Composable
private fun relativeTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> stringResource(R.string.history_now)
        diff < 3_600_000L -> stringResource(R.string.history_minutes_ago, (diff / 60_000L).toInt())
        diff < 86_400_000L -> stringResource(R.string.history_hours_ago, (diff / 3_600_000L).toInt())
        diff < 7L * 86_400_000L -> stringResource(R.string.history_days_ago, (diff / 86_400_000L).toInt())
        else -> remember(timestamp) {
            SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
