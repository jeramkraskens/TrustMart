// Compose screens for welcome, login, registration, profile, server settings and notifications.
package com.trustmart.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustmart.app.AppViewModel

@Composable fun BrandMark(size: Int = 72, white: Boolean = false) {
    Box(Modifier.size(size.dp).background(if (white) Color.White else Color.White.copy(alpha = .22f), CircleShape),
        contentAlignment = Alignment.Center) {
        Text("V", color = if (white) Teal else Color.White, fontSize = (size / 2.4).sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Screenshot-inspired onboarding. Claims describe actual capabilities, not an unverified SLA or guarantee. */
@Composable fun WelcomeScreen(register: () -> Unit, login: () -> Unit, browse: () -> Unit, settings: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Teal, DeepTeal)))) {
        val minHeight = this.maxHeight
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = .045f), radius = size.width * .36f, center = androidx.compose.ui.geometry.Offset(size.width * .16f, size.height * .30f))
            drawCircle(Color.White.copy(alpha = .045f), radius = size.width * .16f, center = androidx.compose.ui.geometry.Offset(size.width * .90f, size.height * .15f))
            drawCircle(Color.White.copy(alpha = .07f), radius = size.width * .27f, center = androidx.compose.ui.geometry.Offset(size.width * .85f, size.height * .69f))
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = minHeight).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(settings) { Icon(Icons.Default.Settings, "Server settings", tint = Color.White) }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.background(Color.White.copy(alpha = .17f), CircleShape).padding(12.dp)) { BrandMark(80, true) }
            Spacer(Modifier.height(8.dp))
            Text("TrustMart", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp)
            Text("Buy & Sell Second-Hand Safely", color = Color.White.copy(alpha = .85f), fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))
            listOf("Verified listings", "Protected test checkout", "Moderator dispute support").forEach {
                Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = .13f), RoundedCornerShape(12.dp)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(it, color = Color.White, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(register, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Teal)) { Text("Get Started") }
            OutlinedButton(login, Modifier.fillMaxWidth().heightIn(min = 52.dp), border = BorderStroke(1.dp, Color.White),
                shape = RoundedCornerShape(16.dp)) { Text("I already have an account", color = Color.White) }
            TextButton(browse) { Text("Explore as a guest", color = Color.White.copy(alpha = .9f)) }
        }
    }
}

@Composable private fun AuthHeader(title: String, subtitle: String, mark: Boolean = true) {
    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(DeepTeal, Teal))).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth()) { BackCircle(true) }
        if (mark) BrandMark(60)
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
        if (!mark) LinearProgressIndicator(progress = { .33f }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            color = Color.White, trackColor = Color.White.copy(alpha = .3f))
        Spacer(Modifier.height(12.dp))
    }
}

/** Passwords are transient. Username authentication stays aligned with Django, unlike the email mockup. */
@Composable fun LoginScreen(vm: AppViewModel, done: () -> Unit, register: () -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var help by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AuthHeader("Welcome back!", "Sign in to your TrustMart account")
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Panel {
                Field("Username", username, { username = it }, placeholder = "Your website username")
                Spacer(Modifier.height(2.dp))
                Field("Password", password, { password = it }, secret = true, placeholder = "Enter your password")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Link("Forgot password?") { help = true }
                }
                Action("Sign In", username.isNotBlank() && password.isNotEmpty()) { vm.login(username, password, done) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f)); Text("  Other sign-in options  ", color = Muted, fontSize = 11.sp); HorizontalDivider(Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { OutlineAction("Google", enabled = false) {} }
                    Box(Modifier.weight(1f)) { OutlineAction("Apple", enabled = false) {} }
                }
                Text("Social sign-in is not connected yet.", color = Muted, fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account?", color = Muted, fontSize = 12.sp)
                Link("Sign up free", register)
            }
        }
    }
    if (help) AlertDialog(onDismissRequest = { help = false }, title = { Text("Password help") },
        text = { Text("Email password reset is not available on this backend yet. Ask your administrator for help. If you know your password, you can change it in Profile after signing in.") },
        confirmButton = { TextButton({ help = false }) { Text("Got it") } })
}

@Composable fun RegisterScreen(vm: AppViewModel, done: () -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var first by rememberSaveable { mutableStateOf("") }
    var last by rememberSaveable { mutableStateOf("") }
    var district by rememberSaveable { mutableStateOf("") }
    var nic by rememberSaveable { mutableStateOf("") }
    var suffix by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("buyer") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    LoadScreen(vm, "registration-locations") { vm.loadLocations() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AuthHeader("Create Account", "Join TrustMart — it's free", false)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("I want to", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                listOf("buyer" to "Buy", "seller" to "Sell").forEach { (id, label) ->
                    val selected = role == id
                    Surface(onClick = { role = id }, enabled = !vm.busy, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp,
                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Teal else Line)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(if (id == "buyer") Icons.Default.ShoppingCart else Icons.Default.Storefront, null, tint = Teal)
                            Column { Text(label, fontWeight = FontWeight.Bold); Text(if (id == "buyer") "Find & buy safely" else "List & earn", fontSize = 10.sp, color = Muted) }
                        }
                    }
                }
            }
            Panel {
                Field("First Name", first, { first = it })
                Field("Last Name", last, { last = it })
                Field("Username", username, { username = it }, placeholder = "Choose a sign-in username")
                Field("Email Address", email, { email = it }, keyboard = KeyboardType.Email, placeholder = "you@example.com")
                Field("Phone Number", phone, { phone = it }, keyboard = KeyboardType.Phone, placeholder = "0771234567 (10 digits)")
                Choice("District", district, listOf("" to "Select district") + vm.locations.keys.map { it to it }) { district = it }
                if (role == "seller") {
                    Field("NIC number (digits)", nic, { nic = it }, keyboard = KeyboardType.Number)
                    Choice("NIC suffix (optional)", suffix, listOf("" to "None", "V" to "V", "X" to "X")) { suffix = it }
                }
                Field("Password", password, { password = it }, secret = true, placeholder = "Min. 8 characters")
                Field("Confirm Password", confirmation, { confirmation = it }, secret = true)
                Text("This is a demo marketplace. Payments in this app are test-only.", fontSize = 11.sp, color = Muted)
                Text("Password: 8+ characters with uppercase, lowercase and a number.", fontSize = 11.sp, color = Muted)
                Action("Create Account", first.isNotBlank() && last.isNotBlank() && username.isNotBlank() && email.contains('@') &&
                    com.trustmart.app.data.validPhone(phone) && district in vm.locations && com.trustmart.app.data.validPassword(password) && password == confirmation &&
                    (role != "seller" || Regex("[0-9]+").matches(nic))) {
                    vm.register(mapOf("username" to username.trim(), "email" to email.trim(), "password" to password, "password_confirm" to confirmation,
                        "first_name" to first.trim(), "last_name" to last.trim(), "district" to district,
                        "phone" to phone, "role" to role) + if (role == "seller") mapOf("nic_number" to nic, "nic_suffix" to suffix) else emptyMap(), done)
                }
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Already have an account?", color = Muted, fontSize = 12.sp)
                Link("Sign in", done)
            }
        }
    }
}

@Composable fun ServerScreen(vm: AppViewModel, done: () -> Unit) {
    var url by rememberSaveable { mutableStateOf(vm.server) }
    var confirm by remember { mutableStateOf(false) }
    Page("Connect to your backend", "Use the same Django backend as your website and moderator app.") {
        Field("API URL ending with /api/", url, { url = it }, keyboard = KeyboardType.Uri)
        Panel {
            Text("Physical phone: http://YOUR-PC-IP:8000/api/")
            Text("Android emulator: http://10.0.2.2:8000/api/")
            Text("Phone and PC must use the same Wi-Fi. Run Django on 0.0.0.0:8000, allow port 8000 on your private network and add the PC IP to ALLOWED_HOSTS.")
            Text("Only enter a server you trust. HTTP is allowed for local debug builds only.")
        }
        Action("Save server and sign out", url.isNotBlank() && vm.recovery == null) { confirm = true }
        if (vm.recovery != null) Text("Resolve the checkout in Orders before changing servers.")
    }
    if (confirm) Confirm("Change server?", "This signs you out and clears this device's session. Your backend data is not deleted.",
        { vm.configure(url, done); confirm = false }, { confirm = false })
}

@Composable fun AccountScreen(vm: AppViewModel, logout: () -> Unit) {
    val me = vm.user ?: return
    var first by rememberSaveable(me.id) { mutableStateOf(me.firstName) }
    var last by rememberSaveable(me.id) { mutableStateOf(me.lastName) }
    var email by rememberSaveable(me.id) { mutableStateOf(me.email) }
    var phone by rememberSaveable(me.id) { mutableStateOf(me.phone) }
    var district by rememberSaveable(me.id) { mutableStateOf(me.district) }
    var nic by rememberSaveable(me.id) { mutableStateOf(me.nicNumber) }
    var suffix by rememberSaveable(me.id) { mutableStateOf(me.nicSuffix) }
    LoadScreen(vm, "profile-locations") { vm.loadLocations() }
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }
    Page("Hello, ${me.username}", "${me.role.replaceFirstChar { it.uppercase() }} account • Trust score ${me.trustScore}") {
        Panel {
            Text("Profile information")
            Field("First name", first, { first = it }); Field("Last name", last, { last = it })
            Field("Email", email, { email = it }, keyboard = KeyboardType.Email)
            Field("Phone", phone, { phone = it }, keyboard = KeyboardType.Phone)
            Choice("District", district, listOf("" to "Select district") + vm.locations.keys.map { it to it }) { district = it }
            if (me.role == "seller") {
                Field("NIC number (digits)", nic, { nic = it }, keyboard = KeyboardType.Number)
                Choice("NIC suffix (optional)", suffix, listOf("" to "None", "V" to "V", "X" to "X")) { suffix = it }
            }
            Action("Save profile", first.isNotBlank() && last.isNotBlank() && email.contains('@') && com.trustmart.app.data.validPhone(phone) &&
                district in vm.locations && (me.role != "seller" || Regex("[0-9]+").matches(nic))) {
                vm.profile(mapOf("first_name" to first.trim(), "last_name" to last.trim(), "email" to email.trim(), "phone" to phone, "district" to district) +
                    if (me.role == "seller") mapOf("nic_number" to nic, "nic_suffix" to suffix) else emptyMap())
            }
        }
        Panel {
            Text("Change password")
            Field("Current password", old, { old = it }, secret = true)
            Field("New password", new, { new = it }, secret = true)
            Field("Confirm new password", repeated, { repeated = it }, secret = true)
            Action("Change password", old.isNotEmpty() && com.trustmart.app.data.validPassword(new) && new == repeated) {
                vm.password(old, new) { old = ""; new = ""; repeated = "" }
            }
        }
        if (vm.recovery == null) Link("Sign out", logout)
        else Text("Resolve your pending checkout in Orders before signing out.")
    }
}

@Composable fun NotificationsScreen(vm: AppViewModel, open: (com.trustmart.app.data.Notice) -> Unit = {}) {
    LoadScreen(vm, "notifications") { vm.loadNotifications() }
    Page("Notifications", "Updates about your listings, messages and orders.") {
        Text("${vm.unreadNotifications} unread · ${vm.notificationStatus}")
        Link("Refresh") { vm.runAction { vm.loadNotifications() } }
        if (vm.notifications.any { !it.isRead }) Action("Mark all as read") { vm.readNotifications() }
        if (vm.notifications.isEmpty() && !vm.busy) Empty("No notifications yet.")
        vm.notifications.forEach { item -> Panel {
            if (!item.isRead) Status("Unread")
            Text(item.type.replace('_', ' '), style = MaterialTheme.typography.labelSmall)
            Text(item.content)
            Text(item.createdAt.take(16).replace('T', ' '), style = MaterialTheme.typography.labelSmall)
            if (!item.isRead) Link("Mark as read") { vm.readNotification(item.id) }
            if (item.link.isNotBlank()) Link("View update") { open(item) }
        } }
    }
}
