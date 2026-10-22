// Buyer/seller Android entry point: creates the Compose UI and connects screen routes.
// AppViewModel owns shared data and API actions; ui/ files describe what each screen displays.
package com.trustmart.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.*
import com.trustmart.app.ui.*

/** The final website uses simulated checkout; never launch a real payment SDK. */
class MainActivity: ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TrustMartTheme { TrustMartApp(vm) } }
    }
}

/** Five role-aware tabs from the references; detail/form routes keep native Back and fixed action bars. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TrustMartApp(vm: AppViewModel) {
    val nav = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current
    val preview = androidx.compose.ui.platform.LocalInspectionMode.current
    val notificationsSnackbar = remember { SnackbarHostState() }
    LaunchedEffect(lifecycleOwner, vm.user?.id, vm.server, preview) {
        if (vm.user != null && !preview) lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.observeNotifications() }
    }
    LaunchedEffect(vm.notificationAlert?.id) {
        val item = vm.notificationAlert ?: return@LaunchedEffect
        if (notificationsSnackbar.showSnackbar(item.content, "View", withDismissAction = true) == SnackbarResult.ActionPerformed) {
            nav.navigate("notifications") { launchSingleTop = true }
        }
        vm.dismissNotificationAlert()
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "welcome"
    val seller = vm.user?.role == "seller"
    val roots = listOf("home", if (seller) "dashboard" else "search", "orders", "messages", "account")
    val hero = route in listOf("welcome", "login", "register", "home", "seller/{id}", "escrow/{id}/{method}", "dispute/{id}")
    val accent = when {
        route == "dispute/{id}" -> Red
        route == "escrow/{id}/{method}" || (seller && route in listOf("home", "dashboard", "create")) -> Orange
        else -> Teal
    }
    val activity = LocalContext.current as? Activity
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = (if (hero) accent else Paper).toArgb()
            window.navigationBarColor = (if (route == "welcome") DeepTeal else Color.White).toArgb()
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !hero
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = route != "welcome"
        }
    }
    fun go(path: String) { nav.navigate(path) { launchSingleTop = true } }
    fun root(path: String) { nav.navigate(path) { popUpTo("home") { inclusive = false }; launchSingleTop = true } }
    fun enter(path: String) { nav.navigate(path) { popUpTo(nav.graph.id) { inclusive = false }; launchSingleTop = true } }
    fun protected(path: String) { if (vm.user == null) go("login") else go(path) }
    fun chat(id: Int, listing: Int = 0) { protected("chat/$id/$listing") }
    // Toolbar Back must honor form-level BackHandler (e.g. previous listing step), just like system Back.
    fun back() { if (!vm.busy) { if (backDispatcher != null) backDispatcher.onBackPressed() else nav.popBackStack() } }
    // Legacy checkout recovery can request navigation from the view model.
    LaunchedEffect(vm.checkoutDestination, vm.ready, entry) {
        // Scaffold subcomposes NavHost: ready data alone does not mean its graph is attached yet.
        if (vm.ready && entry != null && vm.checkoutDestination != null) { root("orders"); vm.consumeCheckoutDestination() }
    }
    CompositionLocalProvider(LocalBusy provides vm.busy, LocalAccent provides accent, LocalBack provides ::back) {
        Scaffold(containerColor = Paper, snackbarHost = { SnackbarHost(notificationsSnackbar) }, topBar = {
            if (!hero) {
                val title = when (route) {
                    "categories" -> "Categories"; "search", "category/{id}" -> "Search Results"
                    "listing/{id}" -> "Product Details"; "checkout/{id}" -> "Checkout"
                    "orders" -> if (seller) "My Sales" else "My Orders"; "tracking/{id}" -> "Track Order"
                    "dashboard" -> "My Listings"; "create" -> "Add New Listing"
                    "messages", "chat/{id}/{listing}" -> "Messages"; "account" -> "My Profile"
                    "wishlist" -> "My Wishlist"; "notifications" -> "Notifications"; "server" -> "Server Settings"
                    else -> "TrustMart"
                }
                CenterAlignedTopAppBar(title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
                    navigationIcon = {
                        if (route !in roots) IconButton(::back, enabled = !vm.busy) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    },
                    actions = {
                        if (vm.user != null && route != "notifications") IconButton({ go("notifications") }) {
                            BadgedBox(badge = { if (vm.unreadNotifications > 0) Badge { Text(vm.unreadNotifications.toString()) } }) {
                                Icon(Icons.Default.NotificationsNone, "Notifications, ${vm.unreadNotifications} unread", tint = Teal)
                            }
                        }
                        if (route == "account" && vm.user?.role == "buyer") IconButton({ go("wishlist") }, enabled = !vm.busy) { Icon(Icons.Default.FavoriteBorder, "Wishlist", tint = Teal) }
                        if (route in roots || route == "server") IconButton({ go("server") }, enabled = !vm.busy) { Icon(Icons.Default.Settings, "Server settings", tint = Muted) }
                    })
            }
        }, bottomBar = {
            if (route in roots || route == "categories" || route == "category/{id}") {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    val labels = listOf("Home", if (seller) "Listings" else "Search", "Orders", "Chat", "Profile")
                    val icons = listOf(Icons.Default.GridView, if (seller) Icons.AutoMirrored.Filled.Assignment else Icons.Default.Search,
                        Icons.Default.Inventory2, Icons.AutoMirrored.Filled.Chat, Icons.Default.Person)
                    roots.forEachIndexed { index, path ->
                        NavigationBarItem(selected = route == path || (path == "search" && route == "category/{id}") || (path == "home" && route == "categories"),
                            enabled = !vm.busy, onClick = { root(path) },
                            icon = { Icon(icons[index], labels[index], Modifier.size(21.dp)) },
                            label = { Text(labels[index], fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = if (seller) Orange else Teal,
                                selectedTextColor = if (seller) Orange else Teal, indicatorColor = if (seller) Cream else Mint,
                                unselectedIconColor = Muted, unselectedTextColor = Muted))
                    }
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
                if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = accent)
                val feedback = vm.error ?: vm.notice
                if (feedback != null) Surface(color = if (vm.error != null) Blush else Mint, shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp)) {
                        Text(feedback, Modifier.weight(1f).heightIn(max = 100.dp).verticalScroll(rememberScrollState()).padding(vertical = 10.dp),
                            color = if (vm.error != null) Red else DeepTeal, fontSize = 12.sp)
                        IconButton({ vm.clearFeedback() }) { Icon(Icons.Default.Close, "Dismiss message", Modifier.size(18.dp)) }
                    }
                }
                if (vm.ready) {
                    val start = remember { if (vm.user == null) "welcome" else "home" }
                    NavHost(nav, startDestination = start, modifier = Modifier.weight(1f)) {
                        composable("welcome") { WelcomeScreen({ go("register") }, { go("login") }, { enter("home") }, { go("server") }) }
                        composable("home") {
                            if (seller) SellerHomeScreen(vm, { go("create") }, { root("dashboard") }, { root("orders") },
                                { go("tracking/$it") }, { go("notifications") }, { go("server") })
                            else HomeScreen(vm, { go("listing/$it") }, { root("search") }, { go("categories") },
                                { go("category/$it") }, { protected("notifications") }, { go("server") })
                        }
                        composable("login") { LoginScreen(vm, { enter("home") }, { go("register") }) }
                        composable("register") { RegisterScreen(vm) { go("login") } }
                        composable("server") { ServerScreen(vm) { enter("welcome") } }
                        composable("categories") { CategoriesScreen(vm) { go("category/$it") } }
                        composable("search") { SearchScreen(vm, { go("listing/$it") }) }
                        composable("category/{id}") { back -> SearchScreen(vm, { go("listing/$it") }, back.arguments!!.getString("id")!!.toInt()) }
                        composable("listing/{id}") { back -> ListingScreen(vm, back.arguments!!.getString("id")!!.toInt(),
                            { chat(it.seller, it.id) }, { go("login") }, { go("checkout/" + it.id) }, { go("seller/$it") }) }
                        composable("seller/{id}") { back -> SellerProfileScreen(vm, back.arguments!!.getString("id")!!.toInt(), { go("listing/$it") }, { chat(it) }) }
                        composable("dashboard") { AuthGate(vm, { go("login") }) {
                            if (seller) SellerScreen(vm, { go("create") }, { go("listing/$it") }, { root("orders") }, { go("edit/$it") })
                            else OrdersScreen(vm, ::chat, { go("tracking/$it") }, { go("dispute/$it") })
                        } }
                        composable("orders") { AuthGate(vm, { go("login") }) { OrdersScreen(vm, ::chat, { go("tracking/$it") }, { go("dispute/$it") }) } }
                        composable("tracking/{id}") { back -> AuthGate(vm, { go("login") }) {
                            TrackingScreen(vm, back.arguments!!.getString("id")!!.toInt(), ::chat, { go("dispute/$it") })
                        } }
                        composable("dispute/{id}") { back -> AuthGate(vm, { go("login") }) {
                            DisputeScreen(vm, back.arguments!!.getString("id")!!.toInt()) { nav.popBackStack() }
                        } }
                        composable("create") { AuthGate(vm, { go("login") }) {
                            if (seller) CreateScreen(vm) { root("dashboard") } else Empty("Only sellers can create listings.")
                        } }
                        composable("wishlist") { AuthGate(vm, { go("login") }) { WishlistScreen(vm) { go("listing/$it") } } }
                        composable("edit/{id}") { back -> AuthGate(vm, { go("login") }) {
                            EditListingScreen(vm, back.arguments!!.getString("id")!!.toInt()) { root("dashboard") }
                        } }
                        composable("messages") { AuthGate(vm, { go("login") }) { InboxScreen(vm) { chat(it) } } }
                        composable("chat/{id}/{listing}") { back -> AuthGate(vm, { go("login") }) {
                            ChatScreen(vm, back.arguments!!.getString("id")!!.toInt(), back.arguments!!.getString("listing")!!.toInt())
                        } }
                        composable("account") { AuthGate(vm, { go("login") }) { AccountScreen(vm) { vm.logout(); enter("welcome") } } }
                        composable("notifications") { AuthGate(vm, { go("login") }) { NotificationsScreen(vm) { item ->
                            val target = when {
                                item.type == "new_message" -> "chat/${android.net.Uri.parse(item.link).getQueryParameter("with")?.toIntOrNull() ?: 0}/0"
                                item.type.startsWith("verification") || item.type.startsWith("listing") -> "dashboard"
                                item.type in listOf("new_sale", "order_created", "delivery_confirmed", "payment_released", "dispute_opened", "dispute_resolved") -> "orders"
                                else -> "account"
                            }
                            go(target)
                        } } }
                        composable("checkout/{id}") { back -> AuthGate(vm, { go("login") }) {
                            val id = back.arguments!!.getString("id")!!.toInt()
                            CheckoutScreen(vm, id) { go("escrow/$id/$it") }
                        } }
                        composable("escrow/{id}/{method}") { back -> AuthGate(vm, { go("login") }) {
                            EscrowScreen(vm, back.arguments!!.getString("id")!!.toInt(), back.arguments!!.getString("method")!!) { root("orders") }
                        } }
                    }
                    // Re-register when busy changes so this guard takes priority over NavHost's callback.
                    key(vm.busy) { BackHandler(vm.busy) { } }
                }
            }
        }
    }
}
@Composable private fun AuthGate(vm: AppViewModel, login: () -> Unit, content: @Composable () -> Unit) {
    if (vm.user == null) Page("Welcome to TrustMart", "Sign in to buy, sell and message securely.") { Action("Sign in", click = login) }
    else content()
}
