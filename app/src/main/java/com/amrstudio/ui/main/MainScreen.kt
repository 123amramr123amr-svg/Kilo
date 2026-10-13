package com.amrstudio.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amrstudio.R
import com.amrstudio.ui.auth.AuthScreen
import com.amrstudio.ui.chat.ChatScreen
import com.amrstudio.ui.components.AmrStudioAvatar
import com.amrstudio.ui.components.AmrStudioButton
import com.amrstudio.ui.components.AmrStudioCard
import com.amrstudio.ui.components.AmrStudioDivider
import com.amrstudio.ui.components.AmrStudioEmptyState
import com.amrstudio.ui.components.AmrStudioIconButton
import com.amrstudio.ui.components.AmrStudioTopAppBar
import com.amrstudio.ui.developer.DeveloperScreen
import com.amrstudio.ui.models.ModelsScreen
import com.amrstudio.ui.projects.ProjectsScreen
import com.amrstudio.ui.settings.SettingsScreen
import com.amrstudio.ui.theme.AmrStudioTheme
import com.amrstudio.ui.theme.ColorSchemeType

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val currentUser = mainViewModel.currentUser.collectAsStateWithLifecycle().value
            val isLoggedIn = currentUser != null

            AmrStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLoggedIn) {
                        MainScreen(navController, mainViewModel)
                    } else {
                        AuthScreen(
                            onLoginSuccess = { userId ->
                                mainViewModel.setCurrentUser(
                                    mainViewModel.userRepository.getUser(userId)
                                )
                            },
                            onShowDeveloper = { /* Navigate to developer auth */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    navController: androidx.navigation.NavController,
    viewModel: MainViewModel
) {
    var selectedTab by remember { mutableStateOf(MainTab.CHATS) }
    val scaffoldState = remember { androidx.compose.material3.ScaffoldState() }
    val scope = rememberCoroutineScope()

    androidx.compose.material3.Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            AmrStudioTopAppBar(
                title = selectedTab.title,
                navigationIcon = {
                    IconButton(onClick = { scaffoldState.drawerState.open() }) {
                        Icon(painterResource(R.drawable.ic_menu), contentDescription = "Menu")
                    }
                },
                actions = {
                    when (selectedTab) {
                        MainTab.CHATS -> {
                            IconButton(onClick = { /* New chat */ }) {
                                Icon(painterResource(R.drawable.ic_add), contentDescription = "New chat")
                            }
                        }
                        MainTab.PROJECTS -> {
                            IconButton(onClick = { /* New project */ }) {
                                Icon(painterResource(R.drawable.ic_add), contentDescription = "New project")
                            }
                        }
                        MainTab.MODELS -> {
                            IconButton(onClick = { /* Refresh models */ }) {
                                Icon(painterResource(R.drawable.ic_refresh), contentDescription = "Refresh")
                            }
                        }
                        else -> {}
                    }
                    // Developer access via logo long press
                    IconButton(onClick = { /* Long press handler */ }) {
                        Icon(painterResource(R.drawable.ic_launcher_foreground), contentDescription = "Amr Studio")
                    }
                }
            )
        },
        drawerContent = { DrawerContent(viewModel, navController, selectedTab) },
        drawerGesturesEnabled = true,
        bottomBar = {
            NavigationBar {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(painterResource(tab.icon), contentDescription = "") },
                        label = { Text(text = tab.title, fontSize = 12.sp) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        colors = androidx.compose.material3.NavigationBarDefaults.navigationBarItemColors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            unselectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                MainTab.CHATS -> ChatScreen(onNewChat = { /* Handle new chat */ })
                MainTab.PROJECTS -> ProjectsScreen()
                MainTab.MODELS -> ModelsScreen()
                MainTab.CONNECTORS -> ConnectorsScreen()
                MainTab.AGENT -> AgentScreen()
                MainTab.SETTINGS -> SettingsScreen()
            }
        }
    }
}

enum class MainTab(val icon: Int, val title: String) {
    CHATS(R.drawable.ic_chat, "المحادثات"),
    PROJECTS(R.drawable.ic_folder, "المشاريع"),
    MODELS(R.drawable.ic_model, "النماذج"),
    CONNECTORS(R.drawable.ic_link, "الوصلات"),
    AGENT(R.drawable.ic_robot, "الوكيل"),
    SETTINGS(R.drawable.ic_settings, "الإعدادات")
}

@Composable
fun DrawerContent(
    viewModel: MainViewModel,
    navController: androidx.navigation.NavController,
    selectedTab: MainTab
) {
    val user = viewModel.currentUser.collectAsStateWithLifecycle().value
    val remainingTime = viewModel.remainingTime.collectAsStateWithLifecycle().value
    val isExpired = viewModel.isExpired.collectAsStateWithLifecycle().value

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // User header
        if (user != null) {
            AmrStudioCard(onClick = { /* Profile */ }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AmrStudioAvatar(name = user.name)
                    androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
                    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        Text(text = user.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        Text(text = user.email, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (isExpired) {
                            Text(text = "انتهت صلاحية الحساب", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(text = "المتبقي: $remainingTime", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            AmrStudioDivider()
        }

        // Navigation items
        MainTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            androidx.compose.material3.Text(
                text = tab.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }

        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))

        // Logout
        AmrStudioOutlinedButton(
            text = "تسجيل الخروج",
            onClick = { viewModel.logout() },
            icon = { Icon(painterResource(R.drawable.ic_logout), contentDescription = "", tint = MaterialTheme.colorScheme.error) }
        )
    }
}

@Composable
fun ConnectorsScreen() {
    val viewModel: ConnectorsViewModel = hiltViewModel()
    val connectors = viewModel.connectors.collectAsStateWithLifecycle().value ?: emptyList()

    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "الوصلات", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

        if (connectors.isEmpty()) {
            AmrStudioEmptyState(
                icon = { Icon(painterResource(R.drawable.ic_link), contentDescription = "", modifier = Modifier.size(64.dp)) },
                title = "لا توجد وصلات",
                message = "أضف وصلات للربط مع الخدمات الخارجية",
                actionText = "إضافة وصلة",
                action = { /* Add connector */ }
            )
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(connectors) { connector ->
                    AmrStudioCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                                Text(text = connector.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                Text(
                                    text = if (connector.isConnected) "متصل" else "غير متصل",
                                    fontSize = 12.sp,
                                    color = if (connector.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.testConnection(connector) },
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(if (connector.isConnected) "اختبار" else "اتصال")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgentScreen() {
    val viewModel: AgentViewModel = hiltViewModel()
    val tasks = viewModel.tasks.collectAsStateWithLifecycle().value ?: emptyList()

    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "مساحة الوكيل", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            AmrStudioButton(
                text = "مهمة جديدة",
                onClick = { /* Show create task dialog */ },
                modifier = Modifier.width(140.dp)
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            AmrStudioEmptyState(
                icon = { Icon(painterResource(R.drawable.ic_robot), contentDescription = "", modifier = Modifier.size(64.dp)) },
                title = "لا توجد مهام",
                message = "أنشئ مهمة جديدة ليقوم الوكيل بتنفيذها تلقائياً",
                actionText = "إنشاء مهمة",
                action = { /* Show create task dialog */ }
            )
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks) { task ->
                    AmrStudioCard(onClick = { viewModel.selectTask(task) }) {
                        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = task.title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                Text(
                                    text = task.status.name,
                                    fontSize = 12.sp,
                                    color = when (task.status) {
                                        AgentTask.TaskStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                                        AgentTask.TaskStatus.FAILED -> MaterialTheme.colorScheme.error
                                        AgentTask.TaskStatus.EXECUTING -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                            Text(text = task.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = (task.currentStep.toFloat() / 5).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val themeMode = viewModel.themeMode.collectAsStateWithLifecycle().value ?: "system"
    val colorScheme = viewModel.colorScheme.collectAsStateWithLifecycle().value ?: "ocean"
    val language = viewModel.language.collectAsStateWithLifecycle().value ?: "ar"

    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "الإعدادات", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))

        // Theme
        AmrStudioCard {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "المظهر", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "السمة")
                    androidx.compose.material3.DropdownMenuButton(
                        onClick = { /* Show dropdown */ },
                        trailingIcon = { Icon(painterResource(R.drawable.ic_expand_more), contentDescription = "") }
                    ) {
                        Text(text = when (themeMode) { "light" -> "فاتح"; "dark" -> "داكن"; else -> "حسب النظام" })
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "مخطط الألوان")
                    androidx.compose.material3.DropdownMenuButton(
                        onClick = { /* Show dropdown */ },
                        trailingIcon = { Icon(painterResource(R.drawable.ic_expand_more), contentDescription = "") }
                    ) {
                        Text(text = ColorSchemeType.values().first { it.key == colorScheme }.name)
                    }
                }
            }
        }
        AmrStudioDivider()

        // Language
        AmrStudioCard {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "اللغة", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "لغة الواجهة")
                    androidx.compose.material3.DropdownMenuButton(
                        onClick = { /* Show dropdown */ },
                        trailingIcon = { Icon(painterResource(R.drawable.ic_expand_more), contentDescription = "") }
                    ) {
                        Text(text = when (language) { "ar" -> "العربية"; else -> "English" })
                    }
                }
            }
        }
        AmrStudioDivider()

        // Notifications
        AmrStudioCard {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "الإشعارات", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "تفعيل الإشعارات")
                    androidx.compose.material3.Switch(
                        checked = viewModel.notificationsEnabled.collectAsStateWithLifecycle().value ?: true,
                        onCheckedChange = viewModel::setNotificationsEnabled,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
        AmrStudioDivider()

        // Offline mode
        AmrStudioCard {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "وضع عدم الاتصال", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "العمل بدون إنترنت")
                    androidx.compose.material3.Switch(
                        checked = viewModel.offlineMode.collectAsStateWithLifecycle().value ?: false,
                        onCheckedChange = viewModel::setOfflineMode,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
        AmrStudioDivider()

        // Data management
        AmrStudioCard {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "البيانات", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                AmrStudioOutlinedButton(
                    text = "تصدير البيانات",
                    onClick = { /* Export */ }
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                AmrStudioOutlinedButton(
                    text = "استيراد البيانات",
                    onClick = { /* Import */ }
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                AmrStudioOutlinedButton(
                    text = "مسح جميع البيانات",
                    onClick = { /* Confirm and clear */ }
                )
            }
        }
        AmrStudioDivider()

        // Legal
        AmrStudioCard {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "قانوني", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                AmrStudioOutlinedButton(text = "سياسة الخصوصية", onClick = { /* Show privacy */ })
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                AmrStudioOutlinedButton(text = "شروط الاستخدام", onClick = { /* Show terms */ })
            }
        }
        AmrStudioDivider()

        // About
        AmrStudioCard {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "حول التطبيق", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                Text(text = "مساعدون عمرو - Amr Studio", fontWeight = FontWeight.Medium)
                androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                Text(text = "الإصدار 1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                Text(text = "منصة ذكاء اصطناعي للمحادثة والبرمجة وإنشاء المواقع", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}