// Compose screen for conversations.
// Read the associated ViewModel for loading, validation and API actions behind the controls.
package com.marketplace.moderator.ui.chat

/** Shows live conversations and retains drafts when a send fails. */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marketplace.moderator.ui.theme.*
import com.marketplace.moderator.ui.verification.EmptyState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel(), initialReceiver: Int? = null) {
    val state by viewModel.uiState.collectAsState()
    val owner = LocalLifecycleOwner.current
    var connection by remember { mutableStateOf("Connecting…") }
    LaunchedEffect(owner, viewModel) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.observe { connection = it }
        }
    }
    var selectedId by rememberSaveable(initialReceiver) { mutableStateOf(initialReceiver) }
    var selectedName by rememberSaveable { mutableStateOf("") }
    var draft by rememberSaveable(selectedId) { mutableStateOf("") }
    BackHandler(selectedId != null) { selectedId = null }

    selectedId?.let { receiverId ->
        Column(Modifier.fillMaxSize().background(Paper).padding(16.dp)) {
            TextButton(onClick = { selectedId = null }) { Text("‹ All conversations") }
            Text(selectedName.ifBlank { state.conversations.find { it.otherUserId == receiverId }?.otherUserName ?: "Seller #$receiverId" }, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(connection, fontSize = 12.sp)
            TextButton(onClick = { viewModel.load() }) { Text("Refresh messages") }
            val thread = state.messages.filter { it.sender == receiverId || it.receiver == receiverId }.sortedBy { it.sentAt }
            val listState = rememberLazyListState()
            LaunchedEffect(receiverId, thread.size) {
                if (thread.isNotEmpty()) listState.animateScrollToItem(thread.lastIndex)
            }
            LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(thread, key = { it.id }) { message ->
                    Card(colors = CardDefaults.cardColors(containerColor = if (message.sender == receiverId) Color.White else TealLight)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(message.senderName, fontSize = 11.sp, color = TealDark)
                            Text(message.content, color = Ink)
                        }
                    }
                }
            }
            state.error?.let { Text(it, color = Rust) }
            OutlinedTextField(draft, { draft = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
            Button(onClick = {
                val sentDraft = draft
                viewModel.send(receiverId, sentDraft) {
                    if (selectedId == receiverId && draft == sentDraft) draft = ""
                }
            }, enabled = draft.isNotBlank() && !state.sending) { Text(if (state.sending) "Sending…" else "Send") }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Paper)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("Chat", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(connection, fontSize = 12.sp, color = Teal)
            Text("Messages with buyers and sellers", fontSize = 13.sp, color = Ink.copy(alpha = 0.5f))
            state.error?.let { Text(it, color = Rust) }
            TextButton(onClick = { viewModel.load() }) { Text("Refresh") }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Teal)
                state.conversations.isEmpty() -> EmptyState(
                    title = "No conversations yet",
                    subtitle = "Messages from buyers and sellers will show up here.",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.conversations, key = { it.otherUserId }) { convo ->
                        Card(
                            onClick = { selectedId = convo.otherUserId; selectedName = convo.otherUserName },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(TealLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        convo.otherUserName.take(2).uppercase(),
                                        color = TealDark, fontWeight = FontWeight.Bold, fontSize = 13.sp
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(convo.otherUserName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                    Text(convo.lastMessage, fontSize = 12.sp, color = Ink.copy(alpha = 0.5f), maxLines = 1)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}
