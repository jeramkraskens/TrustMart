// Compose screen for listing reviews.
// Read the associated ViewModel for loading, validation and API actions behind the controls.
package com.marketplace.moderator.ui.verification

/** Renders product metadata and photos, retains review inputs across rotation, and requires checks before approval. */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.saveable.rememberSaveable

private val CHECKLIST_ITEMS = listOf(
    "Photos match description",
    "Price is reasonable",
    "Seller notes are consistent",
    "No duplicate listing"
)

@Composable
fun VerificationDetailScreen(
    listingId: Int,
    onBack: () -> Unit,
    onChatSeller: (Int) -> Unit = {},
    viewModel: VerificationDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var feedback by rememberSaveable(listingId) { mutableStateOf("") }
    var checks by rememberSaveable(listingId) { mutableStateOf(listOf(false, false, false, false)) }

    LaunchedEffect(listingId) { viewModel.load(listingId) }
    LaunchedEffect(state.done) { if (state.done) onBack() }

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
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        // Swipe through actual listing photos, without inventing evidence.
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val photos = item.listingDetail?.images.orEmpty()
                            if (photos.isEmpty()) ProductPhoto(null, Modifier.width(340.dp).height(210.dp))
                            photos.forEach { ProductPhoto(it.image, Modifier.width(340.dp).height(210.dp)) }
                        }

                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(item.listingTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                            item.listingDetail?.price?.let { Text("Rs. $it", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Teal) }
                            item.listingDetail?.categoryName?.let { InfoRow("Category", it) }
                            item.listingDetail?.condition?.let { InfoRow("Condition", it) }
                            // Same structured location/contact fields used by the final website.
                            item.listingDetail?.district?.takeIf { it.isNotBlank() }?.let { InfoRow("District", it) }
                            item.listingDetail?.city?.takeIf { it.isNotBlank() }?.let { InfoRow("Town", it) }
                            item.listingDetail?.contactPhone?.takeIf { it.isNotBlank() }?.let { InfoRow("Seller contact", it) }
                            Spacer(Modifier.height(4.dp))

                            Spacer(Modifier.height(12.dp))
                            InfoRow("Seller", item.sellerName)
                            item.listingDetail?.seller?.let { seller ->
                                TextButton(onClick = { onChatSeller(seller) }) { Text("Chat with seller") }
                            }
                            InfoRow("Status", item.status.replaceFirstChar { it.uppercase() })
                            InfoRow("Submitted", item.createdAt.take(10))

                            Spacer(Modifier.height(20.dp))
                            Text("Seller notes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = 0.6f))
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PaperWarm)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    item.notes.ifBlank { "No notes provided by seller." },
                                    fontSize = 14.sp,
                                    color = Ink.copy(alpha = 0.8f)
                                )
                            }

                            Spacer(Modifier.height(20.dp))
                            Text("Verification checklist", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = 0.6f))
                            Spacer(Modifier.height(4.dp))
                            CHECKLIST_ITEMS.forEachIndexed { index, label ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = item.status == "pending" && !state.submitting) { checks = checks.mapIndexed { i, value -> if (i == index) !value else value } }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Icon(
                                        if (checks[index]) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = if (checks[index]) "Checked" else "Not checked",
                                        tint = if (checks[index]) Teal else Ink.copy(alpha = 0.3f)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(label, fontSize = 14.sp, color = Ink.copy(alpha = 0.8f))
                                }
                            }

                            Spacer(Modifier.height(20.dp))
                            Text(if (item.status == "pending") "Complete all checks to approve. Add a reason to reject." else "This review is ${item.status}. ${item.feedback}", fontSize = 12.sp, color = Teal)
                            Text("Feedback for seller", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = 0.6f))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = feedback,
                                onValueChange = { feedback = it },
                                placeholder = { Text("Add a note or request more info...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal)
                            )

                            state.error?.let {
                                Spacer(Modifier.height(10.dp))
                                Text(it, color = Rust, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(90.dp))
                        }
                    }

                    // Sticky action bar
                    Surface(shadowElevation = 8.dp, color = Color.White) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.reject(item.id, feedback) },
                                enabled = !state.submitting && item.status == "pending" && feedback.isNotBlank(),
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Rust),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RustLight)
                            ) {
                                Text("Reject", fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { viewModel.approve(item.id, feedback) },
                                enabled = !state.submitting && item.status == "pending" && checks.all { it },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White)
                            ) {
                                if (state.submitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("Verify & Approve", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Ink.copy(alpha = 0.5f))
        Spacer(Modifier.width(12.dp))
        Text(value, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink)
    }
}
