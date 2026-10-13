package com.amrstudio.ui.projects

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amrstudio.R
import com.amrstudio.data.model.Project
import com.amrstudio.ui.components.AmrStudioAvatar
import com.amrstudio.ui.components.AmrStudioButton
import com.amrstudio.ui.components.AmrStudioCard
import com.amrstudio.ui.components.AmrStudioDivider
import com.amrstudio.ui.components.AmrStudioEmptyState
import com.amrstudio.ui.components.AmrStudioIconButton
import com.amrstudio.ui.components.AmrStudioTextField
import com.amrstudio.ui.components.AmrStudioTopAppBar
import com.amrstudio.ui.theme.AmrStudioTheme

@Composable
fun ProjectsScreen() {
    val viewModel: ProjectsViewModel = hiltViewModel()
    val projects = viewModel.projects.collectAsStateWithLifecycle().value ?: emptyList()
    val currentProject = viewModel.currentProject.collectAsStateWithLifecycle().value
    val projectFiles = viewModel.projectFiles.collectAsStateWithLifecycle().value ?: emptyList()
    val currentFile = viewModel.currentFile.collectAsStateWithLifecycle().value
    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle().value

    val showCreateDialog = remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(Project.ProjectType.HTML) }

    AmrStudioTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                AmrStudioTopAppBar(
                    title = "المشاريع",
                    actions = {
                        IconButton(onClick = { showCreateDialog.value = true }) {
                            Icon(painterResource(R.drawable.ic_add), contentDescription = "New project", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                )

                if (currentProject != null) {
                    // Project editor view
                    ProjectEditorView(viewModel, currentProject, projectFiles, currentFile)
                } else {
                    // Projects list
                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        if (projects.isEmpty()) {
                            AmrStudioEmptyState(
                                icon = { Icon(painterResource(R.drawable.ic_folder), contentDescription = "", modifier = Modifier.size(64.dp)) },
                                title = "لا توجد مشاريع",
                                message = "أنشئ مشروعاً جديداً للبدء في بناء المواقع والتطبيقات",
                                actionText = "إنشاء مشروع",
                                action = { showCreateDialog.value = true }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(projects) { project ->
                                    AmrStudioCard(onClick = { viewModel.selectProject(project) }) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(48.dp)
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painterResource(getProjectTypeIcon(project.type)),
                                                    contentDescription = "",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
                                            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                                                Text(text = project.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                                Text(
                                                    text = getProjectTypeName(project.type),
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            androidx.compose.material3.IconButton(
                                                onClick = { /* Menu */ },
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ) {
                                                Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "Menu")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create project dialog
    if (showCreateDialog.value) {
        AlertDialog(
            onDismissRequest = { showCreateDialog.value = false },
            title = { Text("إنشاء مشروع جديد", fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp).width(300.dp)) {
                    AmrStudioTextField(
                        label = "اسم المشروع",
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        placeholder = "مشروع رائع"
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                    Text(text = "نوع المشروع", fontWeight = FontWeight.Medium)
                    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProjectType.values().forEach { type ->
                            val isSelected = selectedType == type
                            androidx.compose.material3.FilterChip(
                                selected = isSelected,
                                onClick = { selectedType = type },
                                label = { Text(text = getProjectTypeName(type)) },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        if (newProjectName.trim().isNotBlank()) {
                            viewModel.createProject(newProjectName.trim(), selectedType)
                            newProjectName = ""
                            showCreateDialog.value = false
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("إنشاء", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCreateDialog.value = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ProjectEditorView(
    viewModel: ProjectsViewModel,
    project: Project,
    files: List<com.amrstudio.data.model.ProjectFile>,
    currentFile: com.amrstudio.data.model.ProjectFile?
) {
    val showFileTree = remember { mutableStateOf(true) }

    AmrStudioTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            // Project header
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { /* Back to projects */ }) {
                            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                        androidx.compose.foundation.layout.Column {
                            Text(text = project.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(text = getProjectTypeName(project.type), fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    }
                    Row {
                        IconButton(onClick = { /* Save version */ }) {
                            Icon(painterResource(R.drawable.ic_save), contentDescription = "Save version")
                        }
                        IconButton(onClick = { /* Preview */ }) {
                            Icon(painterResource(R.drawable.ic_preview), contentDescription = "Preview")
                        }
                        IconButton(onClick = { /* Export */ }) {
                            Icon(painterResource(R.drawable.ic_download), contentDescription = "Export")
                        }
                    }
                }
            }

            // Editor area
            Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                // File tree
                if (showFileTree.value) {
                    Card(
                        modifier = Modifier.width(280.dp).fillMaxHeight(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                            // File tree header
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "الملفات", fontWeight = FontWeight.Medium)
                                IconButton(onClick = { showFileTree.value = false }) {
                                    Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Hide")
                                }
                            }
                            AmrStudioDivider()
                            // File list
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(files) { file ->
                                    val isSelected = currentFile?.id == file.id
                                    androidx.compose.material3.ListItem(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                        onClick = { viewModel.selectFile(file) },
                                        leadingContent = {
                                            Icon(
                                                painterResource(getFileIcon(file.name)),
                                                contentDescription = "",
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp).padding(start = 8.dp)
                                            )
                                        },
                                        headlineContent = {
                                            Text(
                                                text = file.name,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        colors = androidx.compose.material3.ListItemDefaults.listItemColors(
                                            containerColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                            // Add file button
                            AmrStudioButton(
                                text = "ملف جديد",
                                onClick = { /* Show new file dialog */ },
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary) }
                            )
                        }
                    }
                }

                // Editor
                Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                    if (currentFile != null) {
                        FileEditor(viewModel, currentFile)
                    } else {
                        AmrStudioEmptyState(
                            icon = { Icon(painterResource(R.drawable.ic_file), contentDescription = "", modifier = Modifier.size(64.dp)) },
                            title = "لم يتم تحديد ملف",
                            message = "اختر ملفاً من الشجرة الجانبية لبدء التحرير"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileEditor(viewModel: ProjectsViewModel, file: com.amrstudio.data.model.ProjectFile) {
    var content by remember { mutableStateOf(file.content) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    Column(modifier = Modifier.fillMaxSize()) {
        // File tabs bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = file.name, fontWeight = FontWeight.Medium)
                    Text(text = file.language, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { /* Format */ }) {
                        Icon(painterResource(R.drawable.ic_format), contentDescription = "Format")
                    }
                    IconButton(onClick = { /* Save */ viewModel.updateFileContent(file, content) }) {
                        Icon(painterResource(R.drawable.ic_save), contentDescription = "Save")
                    }
                }
            }
        }

        // Editor
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().weight(1f).padding(16.dp)) {
            androidx.compose.foundation.text.BasicTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = Color.White
                ),
                singleLine = false,
                maxLines = Int.MAX_VALUE,
                decorationBox = { innerTextField ->
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        innerTextField()
                    }
                }
            )
        }

        // Preview button for HTML/CSS/JS
        if (file.language in listOf("html", "css", "javascript")) {
            AmrStudioButton(
                text = "معاينة مباشرة",
                onClick = { /* Open preview */ },
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                icon = { Icon(painterResource(R.drawable.ic_preview), contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary) }
            )
        }
    }
}

fun getProjectTypeIcon(type: Project.ProjectType): Int = when (type) {
    Project.ProjectType.HTML -> R.drawable.ic_file_code
    Project.ProjectType.LANDING_PAGE -> R.drawable.ic_layout
    Project.ProjectType.WEB_APP -> R.drawable.ic_phone_android
    Project.ProjectType.DASHBOARD -> R.drawable.ic_dashboard
    Project.ProjectType.STORE -> R.drawable.ic_store
    Project.ProjectType.API -> R.drawable.ic_api
}

fun getProjectTypeName(type: Project.ProjectType): String = when (type) {
    Project.ProjectType.HTML -> "HTML"
    Project.ProjectType.LANDING_PAGE -> "Landing Page"
    Project.ProjectType.WEB_APP -> "Web App"
    Project.ProjectType.DASHBOARD -> "Dashboard"
    Project.ProjectType.STORE -> "متجر"
    Project.ProjectType.API -> "API"
}

fun getFileIcon(fileName: String): Int {
    val ext = fileName.substringAfterLast(".", "").lowercase()
    return when (ext) {
        "html", "htm" -> R.drawable.ic_file_code
        "css" -> R.drawable.ic_file_css
        "js", "jsx", "ts", "tsx" -> R.drawable.ic_file_js
        "json" -> R.drawable.ic_file_json
        "md" -> R.drawable.ic_file_md
        "py" -> R.drawable.ic_file_py
        "kt" -> R.drawable.ic_file_kt
        "java" -> R.drawable.ic_file_java
        else -> R.drawable.ic_file
    }
}