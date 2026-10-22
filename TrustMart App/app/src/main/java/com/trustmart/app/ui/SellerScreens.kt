// Seller screens for dashboard summaries, listing management and new-listing submission.
package com.trustmart.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustmart.app.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Seller metrics are computed from this account's actual listings, transactions and received reviews. */
@Composable fun SellerHomeScreen(vm: AppViewModel, create: () -> Unit, listings: () -> Unit, sales: () -> Unit,
    tracking: (Int) -> Unit, notifications: () -> Unit, settings: () -> Unit) {
    LoadScreen(vm, "seller-home") { vm.loadSellerHome() }
    val me = vm.user ?: return
    val orders = vm.orders.filter { it.sellerId == me.id }
    val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    val pending = vm.mine.count { it.status == "pending_verification" }
    val active = vm.mine.count { it.status == "verified" }
    val held = orders.count { it.escrowStatus == "held" }
    CompositionLocalProvider(LocalAccent provides Orange) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            HomeHero(me.firstName.ifBlank { me.username }, true, notifications, settings, create, vm.unreadNotifications)
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.weight(1f)) { StatTile("Rs. " + money(releasedRevenue(orders, me.id)), "Released earnings", "After platform commission", Teal, Icons.Default.Payments) }
                    Box(Modifier.weight(1f)) { StatTile(active.toString(), "Active listings", "$pending pending", Color(0xFF437EE4), Icons.Default.Inventory2) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.weight(1f)) { StatTile(orders.count { it.createdAt.startsWith(month) }.toString(), "Orders this month", "$held held in escrow", Orange, Icons.Default.ShoppingBag) }
                    Box(Modifier.weight(1f)) { StatTile(averageRating(vm.merchantReviews), "Average rating", vm.merchantReviews.size.toString() + " reviews", Teal, Icons.Default.Star) }
                }
                Text("Needs Your Attention", fontWeight = FontWeight.Bold)
                if (pending > 0) InfoCard("$pending listings awaiting review", "Your listings will go live after moderator approval.", Orange, Icons.Default.PendingActions)
                else if (held > 0) InfoCard("$held orders held in escrow", "Use Chat to coordinate delivery with your buyers.", Orange, Icons.Default.Inventory2)
                else InfoCard("You're all caught up", "Add a new listing to start selling.", Teal, Icons.Default.CheckCircle)
                SectionHeading("Recent Orders", click = sales)
                if (orders.isEmpty() && !vm.busy) Empty("No sales yet.")
                orders.take(4).forEach { order ->
                    Surface(onClick = { tracking(order.id) }, enabled = !vm.busy, color = Color.White, shape = RoundedCornerShape(17.dp), shadowElevation = 2.dp) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(order.buyerName)
                            Column(Modifier.weight(1f)) {
                                Text(order.buyerName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(order.listingTitle, color = Muted, fontSize = 11.sp, maxLines = 1)
                                Text("Rs. " + money(order.amount), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Status(orderLabel(order, vm.disputes))
                        }
                    }
                }
                OutlineAction("Manage all listings", click = listings)
                Link("Refresh dashboard") { vm.runAction { vm.loadSellerHome() } }
            }
        }
    }
}
@Composable fun StatTile(value: String, label: String, note: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), shadowElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = color, modifier = Modifier.background(color.copy(alpha = .12f), RoundedCornerShape(9.dp)).padding(7.dp).size(18.dp))
                Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            }
            Text(label, color = Muted, fontSize = 11.sp)
            Text(note, color = color, fontSize = 10.sp)
        }
    }
}

@Composable fun SellerScreen(vm: AppViewModel, create: () -> Unit, open: (Int) -> Unit, sales: () -> Unit, edit: (Int) -> Unit = {}) {
    LoadScreen(vm, "seller-listings") { vm.loadSeller() }
    CompositionLocalProvider(LocalAccent provides Orange) {
        Page(subtitle = "Manage your listings and verification feedback.") {
            Action("+ Add New Listing", click = create)
            Link("View sales and orders", sales)
            if (vm.mine.isEmpty() && !vm.busy) Empty("You haven't listed anything yet.")
            vm.mine.forEach { item ->
                ProductCard(vm, item, open)
                if (item.canManage) Link("Edit / delete listing") { edit(item.id) }
                vm.verifications.filter { it.listing == item.id }.maxByOrNull { it.id }?.let { review ->
                    if (review.feedback.isNotBlank()) InfoCard("Moderator feedback", review.feedback, Orange)
                }
                if (item.status == "draft") Action("Submit saved draft for verification") { vm.submitDraft(item.id) }
            }
            Link("Refresh listings") { vm.runAction { vm.loadSeller() } }
        }
    }
}

/** Three real steps share one saveable draft; only the final submission creates backend records. */
@Composable fun CreateScreen(vm: AppViewModel, done: () -> Unit) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var condition by rememberSaveable { mutableStateOf("good") }
    var district by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf(vm.user?.phone.orEmpty()) }
    var whatsapp by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }
    var evidence by rememberSaveable { mutableStateOf<String?>(null) }
    var photos by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var draftId by rememberSaveable { mutableStateOf<Int?>(null) }
    fun persist(uri: Uri) {
        try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        catch (_: SecurityException) { /* Provider may only grant access for the current session. */ }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) { uris.take(6).forEach(::persist); photos = uris.take(6).map(Uri::toString) }
        if (uris.size > 6) vm.inform("Selected the first six photos.")
    }
    val evidencePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { persist(it); evidence = it.toString() }
    }
    LoadScreen(vm, "create") { vm.loadSeller() }
    val unlocked = draftId == null
    val validPrice = price.replace(",", "").toBigDecimalOrNull()?.let { it.signum() >= 0 && it.scale() <= 2 } == true
    val stepOneValid = title.isNotBlank() && category.isNotBlank() && validPrice && photos.isNotEmpty()
    val detailsValid = description.isNotBlank() && district in vm.locations && city in vm.locations[district].orEmpty() &&
        com.trustmart.app.data.validPhone(phone) && notes.isNotBlank() && evidence != null
    BackHandler(step > 0 && !vm.busy && unlocked) { step-- }
    CompositionLocalProvider(LocalAccent provides Orange) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                LinearProgressIndicator(progress = { (step + 1) / 3f }, color = Orange, trackColor = Line, modifier = Modifier.fillMaxWidth().height(5.dp))
                Text("Step " + (step + 1) + " of 3 — " + listOf("Product Details", "Description & Evidence", "Review & Submit")[step], fontSize = 11.sp, color = Muted)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                if (draftId != null) InfoCard("Draft #$draftId saved", "Retry verification below. Your product will not be created again.", Orange)
                when (step) {
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Product Photos", fontWeight = FontWeight.Bold)
                            Text("Add up to 6 photos. First photo is your cover image.", fontSize = 11.sp, color = Muted)
                        }
                        Surface(onClick = { picker.launch(arrayOf("image/*")) }, enabled = unlocked && !vm.busy,
                            color = Paper, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Orange)) {
                            if (photos.isNotEmpty()) ProductPhoto(vm, photos.first(), "Cover photo", Modifier.fillMaxWidth().height(160.dp))
                            else Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Muted, modifier = Modifier.size(38.dp))
                                Text("Tap to add cover photo", color = Orange, fontWeight = FontWeight.SemiBold)
                                Text("This helps buyers trust your listing", color = Muted, fontSize = 11.sp)
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(photos, key = { it }) { uri ->
                                Box(Modifier.size(78.dp).clip(RoundedCornerShape(12.dp))) {
                                    ProductPhoto(vm, uri, "Selected product photo", Modifier.fillMaxSize())
                                    IconButton({ photos = photos.filterNot { it == uri } }, enabled = unlocked && !vm.busy, modifier = Modifier.align(Alignment.TopEnd).size(48.dp)) {
                                        Icon(Icons.Default.Close, "Remove photo", modifier = Modifier.background(Color.White, CircleShape).padding(3.dp).size(17.dp), tint = Navy)
                                    }
                                }
                            }
                            if (photos.isEmpty()) items(4) { Box(Modifier.size(72.dp).background(Line.copy(alpha = .45f), RoundedCornerShape(12.dp)).clickable(enabled = !vm.busy) { picker.launch(arrayOf("image/*")) },
                                contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, "Add photo", tint = Muted) } }
                        }
                        if (photos.isNotEmpty()) Link("Replace selected photos (" + photos.size + "/6)") { picker.launch(arrayOf("image/*")) }
                        Field("Product Title", title, { title = it }, enabled = unlocked, placeholder = "e.g. iPhone 14 Pro 256GB Space Black")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f)) { Choice("Category", category, listOf("" to "Select") + vm.categories.map { it.id.toString() to it.name }) { category = it } }
                            Box(Modifier.weight(1f)) { Choice("Condition", condition, conditions) { condition = it } }
                        }
                        Field("Price (Rs.)", price, { price = it }, enabled = unlocked, keyboard = KeyboardType.Decimal, placeholder = "145,000")
                        InfoCard("Your listing will be reviewed", "A moderator checks the listing before it goes live to buyers.", Teal, Icons.Default.Search)
                    }
                    1 -> {
                        Field("Description", description, { description = it }, multiline = true, enabled = unlocked, placeholder = "Condition, included accessories, age and any defects…")
                        Choice("District", district, listOf("" to "Select district") + vm.locations.keys.map { it to it }) { district = it; city = "" }
                        Choice("Town / city", city, listOf("" to "Select town") + vm.locations[district].orEmpty().map { it to it }) { city = it }
                        Field("Contact phone (10 digits)", phone, { phone = it }, keyboard = KeyboardType.Phone, enabled = unlocked)
                        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(whatsapp, { whatsapp = it }, enabled = !vm.busy && unlocked); Text("Allow WhatsApp contact") }
                        Field("Verification notes", notes, { notes = it }, multiline = true, placeholder = "Tell the moderator how authenticity can be checked.")
                        OutlineAction(if (evidence == null) "Attach receipt / warranty" else "Replace verification evidence") { evidencePicker.launch(arrayOf("image/*", "application/pdf")) }
                        Text("Required image or PDF, up to 10 MB. Notes are also required.", color = Muted, fontSize = 11.sp)
                        if (evidence != null) { Status("Evidence attached"); Link("Remove evidence") { evidence = null } }
                    }
                    else -> {
                        ProductPhoto(vm, photos.firstOrNull(), title, Modifier.fillMaxWidth().height(190.dp))
                        Panel {
                            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Rs. " + money(price.replace(",", "")), color = Orange, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text((vm.categories.firstOrNull { it.id.toString() == category }?.name ?: category) + " · " + condition)
                            Text(description, color = Muted)
                            Text("$city, $district · $phone")
                            Text(photos.size.toString() + " photos · " + if (evidence != null) "Evidence attached" else "No evidence attachment", fontSize = 12.sp)
                        }
                        InfoCard("Ready for review", "Check your details before submitting. It will remain pending until a moderator approves it.", Orange)
                    }
                }
                if (step > 0 && unlocked) Link("← Previous step") { step-- }
            }
            BottomActions {
                Action(when { draftId != null -> "Retry Verification Submission"; step == 0 -> "Continue to Description →"; step == 1 -> "Review Listing →"; else -> "Submit for Verification" },
                    when (step) { 0 -> stepOneValid; 1 -> detailsValid; else -> draftId != null || (stepOneValid && detailsValid) }) {
                    if (step < 2) step++
                    else vm.createListing(mapOf("title" to title.trim(), "description" to description.trim(), "price" to price.replace(",", ""),
                        "category" to category, "condition" to condition, "district" to district, "city" to city,
                        "contact_phone" to phone, "whatsapp_enabled" to whatsapp.toString()),
                        photos.map(Uri::parse), notes, evidence?.let(Uri::parse), draftId, { draftId = it }, done)
                }
            }
        }
    }
}
