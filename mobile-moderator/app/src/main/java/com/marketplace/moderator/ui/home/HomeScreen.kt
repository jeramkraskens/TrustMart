// Compose screen for dashboard summaries.
// Read the associated ViewModel for loading, validation and API actions behind the controls.
package com.marketplace.moderator.ui.home

/** Shows live workload totals and actionable records. Unsupported fraud/report metrics are deliberately not fabricated. */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marketplace.moderator.ui.theme.*
import com.marketplace.moderator.ui.RefreshOnResume

@Composable
fun HomeScreen(
    onOpenVerifications: () -> Unit,
    onOpenDisputes: () -> Unit,
    onOpenVerificationItem: (Int) -> Unit,
    onOpenDisputeItem: (Int) -> Unit,
    notificationsAction: @Composable () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    RefreshOnResume { viewModel.load() }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Paper)) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(NavyLight, Navy)))
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Teal.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            state.moderatorName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase().ifBlank { "M" },
                            color = TealLight, fontWeight = FontWeight.Bold, fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Good day", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        Text(
                            state.moderatorName.ifBlank { "Moderator" },
                            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                    }
                    notificationsAction()
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("${state.pendingVerifications}", "Verify", AmberLight, Amber, Modifier.weight(1f).clickable { onOpenVerifications() })
                StatCard("${state.openDisputes}", "Disputes", RustLight, Rust, Modifier.weight(1f).clickable { onOpenDisputes() })
                StatCard("${state.resolvedToday}", "Resolved", SageLight, Sage, Modifier.weight(1f))
            }
        }

        item {
            Text(
                "Needs review",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 0.dp)
            )
            Text(
                "Needs your attention",
                fontSize = 12.sp, color = Ink.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 10.dp)
            )
        }

        item {
            state.error?.let { Text(it, color = Rust, modifier = Modifier.padding(horizontal = 20.dp)) }
            TextButton(onClick = { viewModel.load() }) { Text("Refresh", color = Teal) }
        }
        if (state.loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Teal)
                }
            }
        } else if (state.urgentActions.isEmpty()) {
            item {
                Text(
                    "No pending actions to show.",
                    fontSize = 13.sp, color = Ink.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        } else {
            items(state.urgentActions) { action ->
                when (action) {
                    is UrgentAction.VerificationAction -> UrgentRow(
                        icon = "V", iconBg = AmberLight, iconFg = Amber,
                        title = "Verification needed",
                        subtitle = "${action.item.listingTitle} — ${action.item.sellerName}",
                        onClick = { onOpenVerificationItem(action.item.id) }
                    )
                    is UrgentAction.DisputeAction -> UrgentRow(
                        icon = "D", iconBg = RustLight, iconFg = Rust,
                        title = "Open dispute",
                        subtitle = action.item.listingTitle,
                        onClick = { onOpenDisputeItem(action.item.id) }
                    )
                }
            }
        }

        item {
            Text("Today's Queue", fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(20.dp))
            UrgentRow("V", AmberLight, Amber, "Listing verifications", "${state.pendingVerifications} pending", onOpenVerifications)
            UrgentRow("D", RustLight, Rust, "Dispute cases", "${state.openDisputes} open", onOpenDisputes)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = fg)
            Text(label, fontSize = 11.sp, color = Ink.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun UrgentRow(icon: String, iconBg: Color, iconFg: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, color = iconFg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(subtitle, fontSize = 12.sp, color = Ink.copy(alpha = 0.5f), maxLines = 1)
            }
            Text("›", fontSize = 18.sp, color = Ink.copy(alpha = 0.3f))
        }
    }
}
