package com.personalaiagent.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.personalaiagent.R
import com.personalaiagent.domain.model.AppSettings
import com.personalaiagent.ui.components.AppBackdrop
import com.personalaiagent.ui.screens.account.AccountScreen
import com.personalaiagent.ui.screens.account.ApiKeysScreen
import com.personalaiagent.ui.screens.account.BackupScreen
import com.personalaiagent.ui.screens.account.PrivacyScreen
import com.personalaiagent.ui.screens.account.ProfileScreen
import com.personalaiagent.ui.screens.chat.ChatScreen
import com.personalaiagent.ui.screens.chat.ChatViewModel
import com.personalaiagent.ui.screens.history.ConversationHistoryScreen
import com.personalaiagent.ui.screens.models.ModelSettingsScreen
import com.personalaiagent.ui.screens.models.ModelsScreen
import com.personalaiagent.ui.screens.onboarding.OnboardingScreen
import com.personalaiagent.ui.screens.permissions.PermissionsScreen
import com.personalaiagent.ui.screens.providers.ProviderEditScreen
import com.personalaiagent.ui.screens.providers.ProvidersScreen
import com.personalaiagent.ui.screens.settings.SettingsScreen
import com.personalaiagent.ui.screens.tasks.TaskDetailScreen
import com.personalaiagent.ui.screens.tasks.TaskLogScreen
import com.personalaiagent.ui.screens.voice.VoiceScreen
import com.personalaiagent.ui.screens.workspace.WorkspaceScreen
import com.personalaiagent.ui.theme.BrandBlue
import com.personalaiagent.ui.theme.BrandCyan

@Composable
fun AppNavHost(
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.CHAT
    val showBottomBar = currentRoute in Routes.topLevel

    AppBackdrop(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
            ),
            bottomBar = {
                if (showBottomBar) {
                    AppBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Routes.CHAT) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            // Explicit, short transitions: the default cut between screens felt
            // abrupt and made every tap read as a jump.
            NavHost(
                navController = navController,
                startDestination = if (settings.onboardingCompleted) Routes.CHAT else Routes.ONBOARDING,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 12 }
                },
                exitTransition = {
                    fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { -it / 16 }
                },
                popEnterTransition = {
                    fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { -it / 12 }
                },
                popExitTransition = {
                    fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { it / 16 }
                },
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onFinished = {
                            navController.navigate(Routes.CHAT) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Routes.CHAT) { entry ->
                    val chatViewModel: ChatViewModel = hiltViewModel()
                    val voiceTextFlow = entry.savedStateHandle.getStateFlow<String?>("voice_text", null)
                    val voiceText by voiceTextFlow.collectAsStateWithLifecycle()
                    LaunchedEffect(voiceText) {
                        voiceText?.let { recognized ->
                            chatViewModel.onVoiceInput(recognized)
                            entry.savedStateHandle["voice_text"] = null
                        }
                    }
                    // The history screen asks the chat entry to open an old
                    // conversation through this handle, so the route stays simple.
                    val openConversationFlow =
                        entry.savedStateHandle.getStateFlow<String?>("open_conversation", null)
                    val openConversationId by openConversationFlow.collectAsStateWithLifecycle()
                    LaunchedEffect(openConversationId) {
                        openConversationId?.let { id ->
                            // An empty id means "start a new chat" (the + button).
                            if (id.isBlank()) chatViewModel.newChat() else chatViewModel.openConversation(id)
                            entry.savedStateHandle["open_conversation"] = null
                        }
                    }
                    ChatScreen(
                        viewModel = chatViewModel,
                        onOpenProviders = { navController.navigate(Routes.providerEdit(null)) },
                        onOpenModels = {
                            navController.navigate(Routes.MODELS) {
                                launchSingleTop = true
                            }
                        },
                        onOpenVoice = { navController.navigate(Routes.VOICE) },
                        onOpenTaskLog = { navController.navigate(Routes.TASKS) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                        onOpenWorkspace = { navController.navigate(Routes.WORKSPACE) },
                        onOpenHistory = {
                            navController.navigate(Routes.HISTORY) { launchSingleTop = true }
                        },
                    )
                }

                composable(Routes.HISTORY) {
                    ConversationHistoryScreen(
                        onBack = { navController.popBackStack() },
                        onOpenConversation = { conversationId ->
                            runCatching {
                                val chatEntry = navController.getBackStackEntry(Routes.CHAT)
                                chatEntry.savedStateHandle["open_conversation"] = conversationId
                            }
                            navController.popBackStack(Routes.CHAT, inclusive = false)
                        },
                        onNewChat = {
                            runCatching {
                                val chatEntry = navController.getBackStackEntry(Routes.CHAT)
                                chatEntry.savedStateHandle["open_conversation"] = ""
                            }
                            navController.popBackStack(Routes.CHAT, inclusive = false)
                        },
                    )
                }

                composable(Routes.MODELS) {
                    ModelsScreen(
                        onOpenModel = { modelId -> navController.navigate(Routes.modelSettings(modelId)) },
                        onOpenProviders = { navController.navigate(Routes.PROVIDERS) },
                    )
                }

                composable(
                    route = Routes.MODEL_SETTINGS,
                    arguments = listOf(navArgument("modelId") { type = NavType.StringType }),
                ) {
                    ModelSettingsScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.PROVIDERS) {
                    ProvidersScreen(
                        onBack = { navController.popBackStack() },
                        onAddProvider = { navController.navigate(Routes.providerEdit(null)) },
                        onEditProvider = { id -> navController.navigate(Routes.providerEdit(id)) },
                        onOpenModels = { navController.navigate(Routes.MODELS) { launchSingleTop = true } },
                    )
                }

                composable(
                    route = Routes.PROVIDER_EDIT,
                    arguments = listOf(
                        navArgument("providerId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) {
                    ProviderEditScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onOpenProviders = { navController.navigate(Routes.PROVIDERS) },
                        onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                        onOpenTaskLog = { navController.navigate(Routes.TASKS) },
                        onOpenAccount = { navController.navigate(Routes.ACCOUNT) },
                        onOpenModels = { navController.navigate(Routes.MODELS) { launchSingleTop = true } },
                        onOpenWorkspace = { navController.navigate(Routes.WORKSPACE) },
                    )
                }

                composable(Routes.WORKSPACE) {
                    WorkspaceScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.ACCOUNT) {
                    AccountScreen(
                        onBack = { navController.popBackStack() },
                        onOpenProfile = { navController.navigate(Routes.PROFILE) },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                        onOpenApiKeys = { navController.navigate(Routes.API_KEYS) },
                        onOpenBackup = { navController.navigate(Routes.BACKUP) },
                        onSignedOut = {
                            navController.navigate(Routes.ONBOARDING) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.PRIVACY) {
                    PrivacyScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.API_KEYS) {
                    ApiKeysScreen(
                        onBack = { navController.popBackStack() },
                        onOpenProviders = { navController.navigate(Routes.PROVIDERS) },
                    )
                }

                composable(Routes.BACKUP) {
                    BackupScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.PERMISSIONS) {
                    PermissionsScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.TASKS) {
                    TaskLogScreen(
                        onBack = { navController.popBackStack() },
                        onOpenTask = { taskId -> navController.navigate(Routes.taskDetail(taskId)) },
                    )
                }

                composable(
                    route = Routes.TASK_DETAIL,
                    arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
                ) {
                    TaskDetailScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.VOICE) {
                    VoiceScreen(
                        languageTag = settings.language.tag,
                        onBack = { navController.popBackStack() },
                        onResult = { recognized ->
                            navController.previousBackStackEntry?.savedStateHandle
                                ?.set("voice_text", recognized)
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }
}

private data class BottomDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

@Composable
private fun AppBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
) {
    val destinations = listOf(
        BottomDestination(Routes.CHAT, R.string.nav_chat, Icons.Outlined.ChatBubbleOutline),
        BottomDestination(Routes.MODELS, R.string.nav_models, Icons.Outlined.Widgets),
        BottomDestination(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings),
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 0.dp,
    ) {
        destinations.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandCyan,
                    selectedTextColor = BrandCyan,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = BrandBlue.copy(alpha = 0.18f),
                ),
            )
        }
    }
}
