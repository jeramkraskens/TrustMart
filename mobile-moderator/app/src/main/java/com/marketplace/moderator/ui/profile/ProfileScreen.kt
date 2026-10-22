// Compose screen for account details.
// Read the associated ViewModel for loading, validation and API actions behind the controls.
package com.marketplace.moderator.ui.profile

/** Displays personal moderation statistics and account actions. History links open queues, where Done/Resolved filters show past work. */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marketplace.moderator.ui.theme.*
import kotlinx.coroutines.launch
import com.marketplace.moderator.BuildConfig
import com.marketplace.moderator.ui.RefreshOnResume
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onVerifications: () -> Unit = {},
    onDisputes: () -> Unit = {},
    onNotifications: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    RefreshOnResume { viewModel.load() }
    var dialog by remember { mutableStateOf<String?>(null) }
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    // Every visible account action either performs a request or explains its availability.
    dialog?.let { title ->
        val editable = title == "Edit Profile" || title == "Change Password"
        AlertDialog(onDismissRequest = { if (!saving) dialog = null }, title = { Text(title) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editable) {
                    val password = title == "Change Password"
                    OutlinedTextField(first, { first = it }, label = { Text(if (password) "Current password" else "First name") }, singleLine = true,
                        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
                    OutlinedTextField(second, { second = it }, label = { Text(if (password) "New password" else "Last name") }, singleLine = true,
                        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
                    if (password) OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Confirm new password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                } else Text(when (title) {
                    "Performance Report" -> "${state.verifiedCount} listings approved and ${state.disputesResolvedCount} disputes resolved by you."
                    "Language" -> "The current app supports English. Other translations are not available yet."
                    else -> "TrustMart Moderator ${BuildConfig.VERSION_NAME}"
                })
                error?.let { Text(it, color = Rust) }
            }
        }, confirmButton = { TextButton(enabled = !saving, onClick = {
            if (!editable) dialog = null
            else if (title == "Change Password" && (second != confirmation || second.isBlank())) error = "New passwords must match and cannot be blank."
            else scope.launch {
                saving = true
                error = if (title == "Edit Profile") viewModel.saveProfile(first, second) else viewModel.changePassword(first, second)
                saving = false
                if (error == null) dialog = null
            }
        }) { Text(if (saving) "Saving…" else if (editable) "Save" else "OK") } },
            dismissButton = { if (editable) TextButton(enabled = !saving, onClick = { dialog = null }) { Text("Cancel") } })
    }
    val select: (String) -> Unit = { label ->
        when (label) {
            "Notifications" -> onNotifications()
            "Verification History" -> onVerifications()
            "Resolved Disputes" -> onDisputes()
            else -> {
                first = if (label == "Edit Profile") state.user?.firstName.orEmpty() else ""
                second = if (label == "Edit Profile") state.user?.lastName.orEmpty() else ""
                confirmation = ""; error = null; dialog = label
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(NavyLight, Navy)))
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val name = state.user?.let { "${it.firstName} ${it.lastName}".trim().ifBlank { it.username } } ?: "Moderator"
            val initials = name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()

            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(Teal),
                contentAlignment = Alignment.Center
            ) {
                Text(initials.ifBlank { "M" }, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Content Moderator", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
        }

        // Stats card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("${state.verifiedCount}", "Verified")
                StatDivider()
                StatItem("${state.disputesResolvedCount}", "Disputes")
                StatDivider()
                StatItem(state.user?.trustScore?.toString() ?: "—", "Trust score")
            }
        }

        Column(modifier = Modifier.offset(y = (-8).dp)) {
            state.error?.let { Text(it, color = Rust, modifier = Modifier.padding(20.dp)) }
            TextButton(onClick = { viewModel.load() }) { Text("Refresh profile") }
            MenuSection(title = "ACCOUNT", items = listOf("Edit Profile", "Change Password", "Notifications", "Language"), onClick = select)
            MenuSection(title = "ACTIVITY", items = listOf("Verification History", "Resolved Disputes", "Performance Report"), onClick = select)
            MenuSection(title = "APP", items = listOf("App Version ${BuildConfig.VERSION_NAME}"), onClick = select)

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        viewModel.tokenManager.clear()
                        onLoggedOut()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RustLight, contentColor = Rust)
            ) {
                Text("Log out", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Teal)
        Text(label, fontSize = 11.sp, color = Ink.copy(alpha = 0.5f))
    }
}

@Composable
private fun StatDivider() {
    Box(modifier = Modifier.width(1.dp).height(32.dp).background(Ink.copy(alpha = 0.08f)))
}

@Composable
private fun MenuSection(title: String, items: List<String>, onClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Column {
                items.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClick(label) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontSize = 14.sp, color = Ink.copy(alpha = 0.85f))
                        Text("›", fontSize = 16.sp, color = Ink.copy(alpha = 0.3f))
                    }
                    if (index != items.lastIndex) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.copy(alpha = 0.06f)))
                    }
                }
            }
        }
    }
}
