// Compose screen for order disputes.
// Read the associated ViewModel for loading, validation and API actions behind the controls.
package com.marketplace.moderator.ui.disputes

/** Shows parties and case details, requires a ruling note, and confirms the decision before calling the escrow endpoint. */

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marketplace.moderator.ui.theme.*
import com.marketplace.moderator.ui.ProductPhoto
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun DisputeDetailScreen(
    disputeId: Int,
    onBack: () -> Unit,
    viewModel: DisputeDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var notes by rememberSaveable(disputeId) { mutableStateOf("") }
    var decision by remember { mutableStateOf<String?>(null) }
    var showEscalateNotice by remember { mutableStateOf(false) }

    LaunchedEffect(disputeId) { viewModel.load(disputeId) }
    LaunchedEffect(state.done) { if (state.done) onBack() }

    // Confirm escrow decisions to prevent accidental taps on a small screen.
    decision?.let { resolution ->
        AlertDialog(onDismissRequest = { decision = null }, title = { Text("Confirm decision") },
            text = { Text(if (resolution == "buyer_favor") "Resolve this dispute in the buyer's favor?" else "Resolve this dispute in the seller's favor?") },
            confirmButton = { TextButton(onClick = { decision = null; viewModel.resolve(disputeId, resolution, notes.trim()) }) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { decision = null }) { Text("Cancel") } })
    }

    Box(modifier = Modifier.fillMaxSize().background(Paper)) {
        when {
            state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Teal)
            state.item == null -> Text(
                state.error ?: "Not found",
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                color = Ink.copy(alpha = 0.5f)
            )
            else -> {
                val item = state.item!!
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {

                        // Status banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(RustLight)
                                .padding(14.dp)
                        ) {
                            Column {
                                Text("#D-${item.id.toString().padStart(4, '0')} · ${item.status}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Rust)
                                Spacer(Modifier.height(2.dp))
                                Text(item.reason, fontSize = 12.sp, color = Rust.copy(alpha = 0.85f))
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        Text("Parties involved", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PartyCard(
                                label = "BUYER",
                                name = item.buyerName,
                                bg = Color(0xFFDCEAFE),
                                fg = Color(0xFF1D4ED8),
                                modifier = Modifier.weight(1f)
                            )
                            PartyCard(
                                label = "SELLER",
                                name = item.sellerName,
                                bg = AmberLight,
                                fg = Amber,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                        Text("Item in dispute", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                ProductPhoto(item.listingDetail?.images?.firstOrNull()?.image, Modifier.size(44.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.listingTitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                    Text("Rs. ${item.amount} · transaction amount", fontSize = 12.sp, color = Teal, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        if (item.status == "resolved") Text("${item.resolution}: ${item.resolutionNotes}", color = Teal)
                        Text("Evidence attachments are not available yet.", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                        Text("Moderator note", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Add ruling notes here...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal)
                        )

                        state.error?.let {
                            Spacer(Modifier.height(10.dp))
                            Text(it, color = Rust, fontSize = 12.sp)
                        }
                        if (showEscalateNotice) {
                            Spacer(Modifier.height(10.dp))
                            Text("Escalation to admin isn't available yet in this app.", color = Amber, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(100.dp))
                    }

                    Surface(shadowElevation = 8.dp, color = Color.White) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(if (state.submitting) "Saving decision…" else "Add a ruling note before resolving.", color = Ink.copy(alpha = .6f), fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { decision = "buyer_favor" },
                                    enabled = !state.submitting && item.status == "open" && notes.isNotBlank(),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SageLight, contentColor = Sage)
                                ) {
                                    Text("Refund Buyer", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                Button(
                                    onClick = { decision = "seller_favor" },
                                    enabled = !state.submitting && item.status == "open" && notes.isNotBlank(),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberLight, contentColor = Amber)
                                ) {
                                    Text("Release to Seller", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showEscalateNotice = true },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Rust),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RustLight)
                            ) {
                                Text("Escalate to Admin", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyCard(label: String, name: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(14.dp)
    ) {
        Column {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(fg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.take(2).uppercase(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 10.sp, color = fg.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
            Text(name, fontSize = 13.sp, color = fg, fontWeight = FontWeight.SemiBold)
        }
    }
}
