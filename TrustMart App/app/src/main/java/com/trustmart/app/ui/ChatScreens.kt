// Inbox and conversation UI. AppViewModel supplies messages and handles send actions.
package com.trustmart.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.trustmart.app.AppViewModel
import com.trustmart.app.data.conversations
import com.trustmart.app.data.conversationMessages
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

@Composable private fun liveChat(vm: AppViewModel): String {
    if (androidx.compose.ui.platform.LocalInspectionMode.current) return "Preview"
    val owner = LocalLifecycleOwner.current
    var status by remember { mutableStateOf("Connecting…") }
    LaunchedEffect(owner, vm, vm.user?.id) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.observeMessages { status = it }
        }
    }
    return status
}

@Composable fun InboxScreen(vm: AppViewModel, open: (Int) -> Unit) {
    val status = liveChat(vm)
    val me = vm.user ?: return
    val threads = conversations(vm.messages, me.id)
    Page("Your conversations", status) {
        Link("Refresh now") { vm.runAction { vm.loadMessages() } }
        if (threads.isEmpty() && !vm.busy) Empty("No conversations yet. Open a product and tap Message seller, or message a participant from an order.")
        threads.forEach { thread -> Panel {
            Text(thread.name); Text(thread.last.content, maxLines = 2); Text(thread.last.sentAt.take(16).replace('T', ' '))
            Link("Open conversation") { open(thread.userId) }
        } }
    }
}

/** Sending only clears the draft after server acknowledgement. No fake delivery/read indicators. */
@Composable fun ChatScreen(vm: AppViewModel, other: Int, listing: Int) {
    val status = liveChat(vm)
    var draft by rememberSaveable(other) { mutableStateOf("") }
    val thread = conversationMessages(vm.messages, vm.user?.id ?: return, other)
    val name = thread.lastOrNull()?.let { if (it.sender == other) it.senderName else it.receiverName } ?: "User #$other"
    val list = rememberLazyListState()
    LaunchedEffect(thread.size) { if (thread.isNotEmpty()) list.animateScrollToItem(thread.lastIndex) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(name, style = MaterialTheme.typography.titleLarge)
        Text(status, style = MaterialTheme.typography.labelSmall)
        if (listing != 0) Text("Regarding listing #$listing")
        Link("Refresh messages") { vm.runAction { vm.loadMessages() } }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (thread.isEmpty()) item { Text("Start this conversation with a message.") }
            items(thread, key = { it.id }) { message ->
                val mine = message.sender == vm.user?.id
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Card(Modifier.widthIn(max = 300.dp), colors = CardDefaults.cardColors(containerColor = if (mine) Color(0xFFDFF3EC) else Color.White)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(message.senderName, style = MaterialTheme.typography.labelSmall)
                            Text(message.content)
                            Text(message.sentAt.take(16).replace('T', ' '), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        // Polling must not steal keyboard focus, and a successful send must not erase a newer draft.
        Field("Message", draft, { draft = it }, lockWhileBusy = false)
        Action("Send", draft.isNotBlank() && other != vm.user?.id) {
            val sentText = draft
            vm.send(other, sentText, listing.takeIf { it != 0 }) { if (draft == sentText) draft = "" }
        }
    }
}
