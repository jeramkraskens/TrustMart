// Compose screen for order disputes.
// Read the associated ViewModel for loading, validation and API actions behind the controls.
package com.marketplace.moderator.ui.disputes

/** Lists all accessible dispute pages with real open/resolved filters; priority is not part of the current backend model. */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.marketplace.moderator.data.models.Dispute
import com.marketplace.moderator.ui.theme.*
import com.marketplace.moderator.ui.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.marketplace.moderator.ui.verification.EmptyState
import com.marketplace.moderator.ui.verification.StatusChip

@Composable
fun DisputeScreen(
    onOpenItem: (Int) -> Unit,
    viewModel: DisputeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    RefreshOnResume { viewModel.load() }
    var filter by rememberSaveable { mutableStateOf("Pending") }
    val visible = state.items.filter { when (filter) {
        "Pending" -> it.status == "open"
        "Resolved" -> it.status == "resolved"
        else -> true
    } }

    Column(modifier = Modifier.fillMaxSize().background(Paper)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Disputes", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Active dispute cases", fontSize = 13.sp, color = Ink.copy(alpha = 0.5f))
                }
                if (state.items.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(RustLight)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("${state.items.size}", fontSize = 12.sp, color = Rust, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        QueueFilters(listOf("All", "Pending", "Resolved"), filter) { filter = it }
        TextButton(onClick = { viewModel.load() }) { Text("Refresh", color = Teal) }
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Teal)
                visible.isEmpty() -> EmptyState(
                    title = if (state.error == null) "No matching disputes" else "Could not load disputes",
                    subtitle = state.error ?: "Choose another filter or refresh the cases.",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visible, key = { it.id }) { item ->
                        DisputeRow(item = item, onClick = { onOpenItem(item.id) })
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }

            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = RustLight,
                    contentColor = Rust
                ) { Text(error) }
            }
        }
    }
}

@Composable
private fun DisputeRow(item: Dispute, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Rust)
            )
            Column(modifier = Modifier.weight(1f).padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "#D-${item.id.toString().padStart(4, '0')}",
                            fontSize = 12.sp,
                            color = Ink.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(item.listingTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("Rs. ${item.amount}", fontSize = 13.sp, color = Teal, fontWeight = FontWeight.SemiBold)
                        Text("${item.buyerName} vs ${item.sellerName}", fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
                    }
                    StatusChip(text = item.status, bg = if (item.status == "resolved") SageLight else RustLight, fg = if (item.status == "resolved") Sage else Rust)
                }
            }
        }
    }
}
