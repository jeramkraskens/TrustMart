package com.marketplace.moderator.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marketplace.moderator.data.models.Notification

@Composable
fun NotificationsScreen(viewModel: NotificationViewModel, open: (Notification) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Notifications", style = MaterialTheme.typography.headlineSmall)
            Text("${state.unread} unread · ${state.status}")
            Row {
                TextButton(onClick = { viewModel.load() }) { Text("Refresh") }
                TextButton(onClick = { viewModel.markRead() }, enabled = state.unread > 0 && !state.saving) { Text("Mark all as read") }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        if (state.items.isEmpty()) item { Text("No notifications yet. Updates will appear automatically.") }
        items(state.items, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!item.isRead) Text("Unread", style = MaterialTheme.typography.labelSmall)
                    Text(item.type.replace('_', ' '), style = MaterialTheme.typography.labelMedium)
                    Text(item.content)
                    Text(item.createdAt.take(16).replace('T', ' '), style = MaterialTheme.typography.labelSmall)
                    Row {
                        if (!item.isRead) TextButton(onClick = { viewModel.markRead(item.id) }, enabled = !state.saving) { Text("Mark as read") }
                        if (item.link.isNotBlank()) TextButton(onClick = { open(item) }) { Text("View update") }
                    }
                }
            }
        }
    }
}
