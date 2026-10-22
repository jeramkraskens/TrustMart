// Compose screen for moderator sign-in.
// Read the associated ViewModel for loading, validation and API actions behind the controls.
package com.marketplace.moderator.ui.login

/** Scrollable credential form supports the phone keyboard and displays request progress/errors. The backend authenticates usernames. */

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marketplace.moderator.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var server by remember { mutableStateOf(viewModel.server) }
    var showServer by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()
    var showRecovery by remember { mutableStateOf(false) }
    if (showRecovery) AlertDialog(onDismissRequest = { showRecovery = false },
        title = { Text("Password help") },
        text = { Text("Contact your marketplace administrator to reset your moderator password. Email password recovery is not available yet.") },
        confirmButton = { TextButton(onClick = { showRecovery = false }) { Text("OK") } })

    LaunchedEffect(state) {
        if (state is LoginState.Success) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, NavyLight)))
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Teal),
            contentAlignment = Alignment.Center
        ) {
            Text("V", fontSize = 26.sp, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text("TrustMart", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
        Text("Moderator App", fontSize = 13.sp, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f))

        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Teal.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text("MOD ACCESS ONLY", fontSize = 10.sp, color = TealLight, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(36.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Sign in to continue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Use your moderator credentials", fontSize = 13.sp, color = Ink.copy(alpha = 0.5f))

                Spacer(Modifier.height(20.dp))
                TextButton(onClick = { showServer = !showServer }, enabled = state !is LoginState.Loading) { Text("Server settings") }
                if (showServer) OutlinedTextField(server, { server = it }, label = { Text("Backend URL ending in /api/") },
                    supportingText = { Text("Emulator: http://10.0.2.2:8000/api/ · Phone: use your computer's Wi-Fi address.") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = state !is LoginState.Loading)
                Text("Username", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink.copy(alpha = 0.7f))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Teal,
                        unfocusedBorderColor = Ink.copy(alpha = 0.15f)
                    )
                )

                Spacer(Modifier.height(16.dp))
                Text("Password", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink.copy(alpha = 0.7f))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Enter password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Teal,
                        unfocusedBorderColor = Ink.copy(alpha = 0.15f)
                    )
                )

                if (state is LoginState.Error) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        (state as LoginState.Error).message,
                        color = Rust,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.login(username.trim(), password, server) },
                    enabled = state !is LoginState.Loading && username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = androidx.compose.ui.graphics.Color.White)
                ) {
                    if (state is LoginState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Sign In", fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = { showRecovery = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Forgot password?", color = Teal)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}
