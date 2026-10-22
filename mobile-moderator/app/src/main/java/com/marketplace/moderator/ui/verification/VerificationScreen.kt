// Compose screen for listing reviews.
// Read the associated ViewModel for loading, validation and API actions behind the controls.
package com.marketplace.moderator.ui.verification

/** Filters live reviews by actual backend status and opens request details. Refresh on resume reflects completed decisions. */

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
import com.marketplace.moderator.data.models.VerificationRequest
import com.marketplace.moderator.ui.theme.*
import com.marketplace.moderator.ui.*
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun VerificationScreen(
    onOpenItem: (Int) -> Unit,
    viewModel: VerificationViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    RefreshOnResume { viewModel.load() }
    var filter by rememberSaveable { mutableStateOf("Pending") }
    // Priority is not stored by the API, so filters reflect real review states.
    val visible = state.items.filter { when (filter) {
        "Pending" -> it.status == "pending"
        "Done" -> it.status != "pending"
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
                    Text("Verification Queue", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Review and approve listings", fontSize = 13.sp, color = Ink.copy(alpha = 0.5f))
                }
                if (state.items.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AmberLight)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("${state.items.count { it.status == "pending" }} left", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        QueueFilters(listOf("All", "Pending", "Done"), filter) { filter = it }
        TextButton(onClick = { viewModel.load() }) { Text("Refresh", color = Teal) }
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Teal)
                visible.isEmpty() -> EmptyState(
                    title = if (state.error == null) "No matching reviews" else "Could not load reviews",
                    subtitle = state.error ?: "Choose another filter or refresh the queue.",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visible, key = { it.id }) { item ->
                        VerificationRow(item = item, onClick = { onOpenItem(item.id) })
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
private fun VerificationRow(item: VerificationRequest, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductPhoto(item.listingDetail?.images?.firstOrNull()?.image,
                modifier = Modifier
                    .size(64.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.listingTitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("by ${item.sellerName}", fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
                Spacer(Modifier.height(4.dp))
                item.listingDetail?.price?.let { Text("Rs. $it", color = Teal, fontWeight = FontWeight.Bold) }
                StatusChip(text = item.listingDetail?.categoryName ?: item.status, bg = TealLight, fg = Teal)
            }
            Text("›", fontSize = 20.sp, color = Ink.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun StatusChip(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 14.sp, color = Ink.copy(alpha = 0.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
