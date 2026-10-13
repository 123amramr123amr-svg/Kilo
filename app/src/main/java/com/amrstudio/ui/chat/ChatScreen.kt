package com.amrstudio.ui.chat

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amrstudio.R
import com.amrstudio.data.model.Message
import com.amrstudio.ui.components.AmrStudioAvatar
import com.amrstudio.ui.components.AmrStudioButton
import com.amrstudio.ui.components.AmrStudioCard
import com.amrstudio.ui.components.AmrStudioDivider
import com.amrstudio.ui.components.AmrStudioEmptyState
import com.amrstudio.ui.components.AmrStudioIconButton
import com.amrstudio.ui.components.AmrStudioTopAppBar
import com.amrstudio.ui.theme.AmrStudioTheme
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(onNewChat: () -> Unit) {
    val viewModel: ChatViewModel = hiltViewModel()
    val messages = viewModel.messages.collectAsStateWithLifecycle().value ?: emptyList()
    val isStreaming = viewModel.isStreaming.collectAsStateWithLifecycle().value
    val availableModels = viewModel.availableModels.collectAsStateWithLifecycle().value ?: emptyList()
    val selectedModel = viewModel.selectedModel.collectAsStateWithLifecycle().value
    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle().value

    var messageText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val showModelPicker = remember { mutableStateOf(false) }
    val showAttachMenu = remember { mutableStateOf(false) }

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isStreaming) {
        if (!isStreaming) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    AmrStudioTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                AmrStudioTopAppBar(
                    title = viewModel.currentChat.collectAsStateWithLifecycle().value?.title ?: "محادثة جديدة",
                    actions = {
                        IconButton(onClick = { showModelPicker.value = true }) {
                            Icon(painterResource(R.drawable.ic_model), contentDescription = "Select model", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = onNewChat) {
                            Icon(painterResource(R.drawable.ic_add), contentDescription = "New chat", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                )

                // Model selector indicator
                if (selectedModel != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.ic_model), contentDescription = "", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                                Text(
                                    text = selectedModel.name,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = selectedModel.providerId,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Messages list
                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    if (messages.isEmpty() && !isLoading) {
                        AmrStudioEmptyState(
                            icon = { Icon(painterResource(R.drawable.ic_chat), contentDescription = "", modifier = Modifier.size(64.dp)) },
                            title = "لا توجد رسائل",
                            message = "ابدأ محادثة جديدة بإرسال رسالة"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(scrollState),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            reverseLayout = false
                        ) {
                            items(messages) { message ->
                                MessageBubble(message = message)
                            }
                            // Streaming indicator
                            if (isStreaming) {
                                StreamingIndicator()
                            }
                        }
                    }
                }

                // Input area
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    AmrStudioDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attach button
                        AmrStudioIconButton(
                            icon = { Icon(painterResource(R.drawable.ic_attach), contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { showAttachMenu.value = true }
                        )

                        // Text field
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            placeholder = { Text(text = "اكتب رسالتك هنا…") },
                            singleLine = false,
                            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send,
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                            ),
                            keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
                                onDone = { viewModel.sendMessage(messageText); messageText = "" }
                            ),
                            maxLines = 5,
                            colors = androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        )

                        // Send button
                        AmrStudioIconButton(
                            icon = {
                                Icon(
                                    painterResource(if (isStreaming) R.drawable.ic_stop else R.drawable.ic_send),
                                    contentDescription = if (isStreaming) "Cancel" else "Send",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                if (isStreaming) {
                                    viewModel.cancelGeneration()
                                } else if (messageText.trim().isNotBlank()) {
                                    viewModel.sendMessage(messageText.trim())
                                    messageText = ""
                                }
                            },
                            enabled = messageText.trim().isNotBlank() || isStreaming,
                            size = 48.dp
                        )
                    }
                }
            }
        }
    }

    // Model picker dialog
    if (showModelPicker.value) {
        AlertDialog(
            onDismissRequest = { showModelPicker.value = false },
            title = { Text("اختر النموذج", fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                    availableModels.forEach { model ->
                        val isSelected = selectedModel?.id == model.id
                        androidx.compose.material3.ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.selectModel(model)
                                showModelPicker.value = false
                            },
                            leadingContent = {
                                if (isSelected) {
                                    Icon(painterResource(R.drawable.ic_check), contentDescription = "", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                }
                            },
                            headlineContent = { Text(text = model.name) },
                            supportingContent = { Text(text = model.providerId, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = androidx.compose.material3.ListItemDefaults.listItemColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showModelPicker.value = false }) { Text("إلغاء") } }
        )
    }

    // Attach menu
    if (showAttachMenu.value) {
        AlertDialog(
            onDismissRequest = { showAttachMenu.value = false },
            title = { Text("إرفاق ملف", fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                    listOf("صورة", "ملف", "كود", "صوت").forEach { item ->
                        androidx.compose.material3.ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { /* Handle attach */ showAttachMenu.value = false },
                            leadingContent = { Icon(painterResource(R.drawable.ic_attach), contentDescription = "", modifier = Modifier.size(24.dp)) },
                            headlineContent = { Text(text = item) }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAttachMenu.value = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.role == Message.MessageRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            AmrStudioAvatar(name = "AI", modifier = Modifier.size(32.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(vertical = 4.dp),
            crossAxisAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                    if (message.role == Message.MessageRole.ASSISTANT) {
                        MarkdownContent(text = message.content)
                    } else {
                        Text(text = message.content)
                    }

                    // Code blocks handling
                    if (message.content.contains("```")) {
                        CodeBlocksView(content = message.content)
                    }
                }
            }

            // Timestamp
            Text(
                text = formatTime(message.createdAt),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, start = if (isUser) 0.dp else 40.dp, end = if (isUser) 40.dp else 0.dp)
            )
        }

        if (isUser) {
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            AmrStudioAvatar(name = "أنت", modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun MarkdownContent(text: String) {
    // Simple markdown rendering - in production use Markwon or similar
    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
        text.split("\n").forEach { line ->
            when {
                line.startsWith("# ") -> Text(text = line.substring(2), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                line.startsWith("## ") -> Text(text = line.substring(3), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                line.startsWith("### ") -> Text(text = line.substring(4), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                line.startsWith("- ") || line.startsWith("* ") -> Text(text = "• ${line.substring(2)}", modifier = Modifier.fillMaxWidth().padding(start = 16.dp))
                line.startsWith("> ") -> Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) { Text(text = line.substring(2), modifier = Modifier.padding(12.dp), fontStyle = androidx.compose.ui.text.style.FontStyle.Italic) }
                else -> Text(text = line, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun CodeBlocksView(content: String) {
    // Extract code blocks from markdown
    val codeBlocks = extractCodeBlocks(content)
    if (codeBlocks.isNotEmpty()) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            codeBlocks.forEach { block ->
                CodeBlockCard(code = block.code, language = block.language)
            }
        }
    }
}

data class CodeBlock(val language: String, val code: String)

fun extractCodeBlocks(text: String): List<CodeBlock> {
    val blocks = mutableListOf<CodeBlock>()
    val pattern = "```(\\w*)\\n([\\s\\S]*?)```".toRegex()
    pattern.findAll(text).forEach { match ->
        val language = match.groupValues[1].ifBlank { "plaintext" }
        val code = match.groupValues[2].trim()
        blocks.add(CodeBlock(language, code))
    }
    return blocks
}

@Composable
fun CodeBlockCard(code: String, language: String) {
    var showFull by remember { mutableStateOf(false) }
    val maxLines = if (showFull) Int.MAX_VALUE else 20

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = language.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.7f))
                Row {
                    IconButton(onClick = { /* Copy to clipboard */ }) {
                        Icon(painterResource(R.drawable.ic_copy), contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { /* Download */ }) {
                        Icon(painterResource(R.drawable.ic_download), contentDescription = "Download", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { /* Preview */ }) {
                        Icon(painterResource(R.drawable.ic_preview), contentDescription = "Preview", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { /* Open in editor */ }) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Code content
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, bottom = 12.dp)
                    .background(Color(0xFF1E1E1E))
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = code,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .heightIn(min = 100.dp, max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color.White
                    ),
                    readOnly = true,
                    singleLine = false,
                    maxLines = maxLines
                )
            }

            // Show more/less
            if (code.lines().count() > 20) {
                androidx.compose.material3.TextButton(
                    onClick = { showFull = !showFull },
                    contentColor = Color.White.copy(alpha = 0.7f)
                ) {
                    Text(text = if (showFull) "إظهار أقل" else "إظهار المزيد")
                }
            }
        }
    }
}

@Composable
fun StreamingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AmrStudioAvatar(name = "AI", modifier = Modifier.size(32.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
        Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                Text(text = "يفكر…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun formatTime(dateTime: kotlinx.datetime.LocalDateTime): String {
    val hour = dateTime.hour
    val minute = dateTime.minute
    return String.format("%02d:%02d", hour, minute)
}