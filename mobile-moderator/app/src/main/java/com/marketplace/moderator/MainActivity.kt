// Moderator Android entry point and navigation host.
// Screen composables draw the UI; their ViewModels load records and submit moderator actions.
package com.marketplace.moderator

/** Owns login navigation, bottom tabs, and device-safe content insets. Screens delegate network work to ViewModels. */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marketplace.moderator.ui.notifications.NotificationViewModel
import com.marketplace.moderator.ui.notifications.NotificationsScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.marketplace.moderator.data.TokenManager
import com.marketplace.moderator.ui.chat.ChatScreen
import com.marketplace.moderator.ui.disputes.DisputeDetailScreen
import com.marketplace.moderator.ui.disputes.DisputeScreen
import com.marketplace.moderator.ui.home.HomeScreen
import com.marketplace.moderator.ui.login.LoginScreen
import com.marketplace.moderator.ui.nav.Screen
import com.marketplace.moderator.ui.profile.ProfileScreen
import com.marketplace.moderator.ui.theme.Ink
import com.marketplace.moderator.ui.theme.ModeratorAppTheme
import com.marketplace.moderator.ui.theme.Teal
import com.marketplace.moderator.ui.verification.VerificationDetailScreen
import com.marketplace.moderator.ui.verification.VerificationScreen
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ModeratorAppTheme {
                ModeratorApp()
            }
        }
    }
}

private val TAB_ROUTES = setOf(
    Screen.Home.route, Screen.Verifications.route, Screen.Disputes.route, Screen.Chat.route, Screen.Profile.route
)

@Composable
fun ModeratorApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember(context) { TokenManager(context) }
    val authFlow = remember(tokenManager) { tokenManager.accessTokenFlow.map { true to it } }
    val authState by authFlow.collectAsState(initial = false to null)
    val accessToken = authState.second
    val notifications: NotificationViewModel = viewModel()
    val notificationState by notifications.uiState.collectAsState()
    val owner = LocalLifecycleOwner.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(owner, accessToken != null) {
        if (accessToken != null) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { notifications.observe() }
        else notifications.clear()
    }
    LaunchedEffect(notificationState.alert?.id) {
        val item = notificationState.alert ?: return@LaunchedEffect
        if (snackbar.showSnackbar(item.content, "View", withDismissAction = true) == SnackbarResult.ActionPerformed) {
            navController.navigate("notifications") { launchSingleTop = true }
        }
        notifications.dismiss()
    }

    var startDestination by remember { mutableStateOf<String?>(null) }

    // Run this startup lookup when the composable enters the UI, rather than on every redraw.
    LaunchedEffect(Unit) {
        val token = tokenManager.getAccessToken()
        startDestination = if (token != null) Screen.Home.route else Screen.Login.route
    }

    if (startDestination == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Teal)
        }
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTabRoute = currentRoute in TAB_ROUTES
    LaunchedEffect(authState.first, accessToken == null, currentRoute) {
        if (authState.first && accessToken == null && currentRoute != null && currentRoute != Screen.Login.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if ((isTabRoute && currentRoute != Screen.Home.route) || currentRoute == "notifications") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    NotificationBell(notificationState.unread,
                        onClick = { navController.navigate("notifications") { launchSingleTop = true } })
                }
            }
            when (currentRoute) {
                "verifications/{id}" -> DetailTopBar("Review Listing") { navController.popBackStack() }
                "disputes/{id}" -> DetailTopBar("Dispute Detail") { navController.popBackStack() }
            }
        },
        bottomBar = {
            if (isTabRoute) {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = { navController.navigateToTab(Screen.Home.route) },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home", fontSize = 11.sp) },
                        colors = tabColors()
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Verifications.route,
                        onClick = { navController.navigateToTab(Screen.Verifications.route) },
                        icon = { Icon(Icons.Default.Verified, contentDescription = null) },
                        label = { Text("Queue", fontSize = 11.sp) },
                        colors = tabColors()
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Disputes.route,
                        onClick = { navController.navigateToTab(Screen.Disputes.route) },
                        icon = { Icon(Icons.Default.Gavel, contentDescription = null) },
                        label = { Text("Disputes", fontSize = 11.sp) },
                        colors = tabColors()
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Chat.route,
                        onClick = { navController.navigateToTab(Screen.Chat.route) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                        label = { Text("Chat", fontSize = 11.sp) },
                        colors = tabColors()
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Profile.route,
                        onClick = { navController.navigateToTab(Screen.Profile.route) },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Profile", fontSize = 11.sp) },
                        colors = tabColors()
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination!!,
            // Respect status/navigation bars on physical devices as well as emulators.
            modifier = Modifier.padding(padding).imePadding()
        ) {
            composable(Screen.Login.route) {
                LoginScreen(onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onOpenVerifications = { navController.navigateToTab(Screen.Verifications.route) },
                    onOpenDisputes = { navController.navigateToTab(Screen.Disputes.route) },
                    onOpenVerificationItem = { id -> navController.navigate(Screen.VerificationDetail.build(id)) },
                    onOpenDisputeItem = { id -> navController.navigate(Screen.DisputeDetail.build(id)) },
                    notificationsAction = {
                        NotificationBell(notificationState.unread, tint = Color.White,
                            onClick = { navController.navigate("notifications") { launchSingleTop = true } })
                    }
                )
            }
            composable(Screen.Verifications.route) {
                VerificationScreen(onOpenItem = { id -> navController.navigate(Screen.VerificationDetail.build(id)) })
            }
            composable(
                route = Screen.VerificationDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { backStack ->
                val id = backStack.arguments?.getInt("id") ?: return@composable
                VerificationDetailScreen(listingId = id, onBack = { navController.popBackStack() },
                    onChatSeller = { seller -> navController.navigate("chat/$seller") })
            }
            composable(Screen.Disputes.route) {
                DisputeScreen(onOpenItem = { id -> navController.navigate(Screen.DisputeDetail.build(id)) })
            }
            composable(
                route = Screen.DisputeDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { backStack ->
                val id = backStack.arguments?.getInt("id") ?: return@composable
                DisputeDetailScreen(disputeId = id, onBack = { navController.popBackStack() })
            }
            composable(Screen.Chat.route) {
                ChatScreen()
            }
            composable("notifications") {
                NotificationsScreen(notifications) { item ->
                    val destination = when {
                        item.type == "new_message" -> android.net.Uri.parse(item.link).getQueryParameter("with")?.toIntOrNull()?.let { "chat/$it" } ?: Screen.Chat.route
                        item.type.startsWith("verification") -> Screen.Verifications.route
                        item.type.startsWith("dispute") -> Screen.Disputes.route
                        else -> Screen.Profile.route
                    }
                    navController.navigate(destination) { launchSingleTop = true }
                }
            }
            composable("chat/{receiver}", arguments = listOf(navArgument("receiver") { type = NavType.IntType })) { entry ->
                ChatScreen(initialReceiver = entry.arguments?.getInt("receiver"))
            }
            composable(Screen.Profile.route) {
                ProfileScreen(onVerifications = { navController.navigateToTab(Screen.Verifications.route) },
                    onNotifications = { navController.navigate("notifications") },
                    onDisputes = { navController.navigateToTab(Screen.Disputes.route) }, onLoggedOut = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                })
            }
        }
    }
}

private fun androidx.navigation.NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun NotificationBell(unread: Int, onClick: () -> Unit, tint: Color = LocalContentColor.current) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (unread > 0) Badge { Text(unread.toString()) } }) {
            Icon(Icons.Default.Notifications, "Notifications, $unread unread", tint = tint)
        }
    }
}

@Composable
private fun tabColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Teal, selectedTextColor = Teal, indicatorColor = Color.Transparent
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text("‹ Back", color = Teal, fontWeight = FontWeight.Medium)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Ink
        )
    )
}
