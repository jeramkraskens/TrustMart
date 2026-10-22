// Order, checkout, tracking and dispute screens.
// Checkout in this project is a simulation; screen labels do not imply a real payment transfer.
package com.trustmart.app.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustmart.app.AppViewModel
import com.trustmart.app.BuildConfig
import com.trustmart.app.data.*

/** Only display states supplied or unambiguously derivable from the backend. */
@Composable fun OrderItem(vm: AppViewModel, order: Order) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val product = vm.mine.firstOrNull { it.id == order.listing } ?: vm.listings.firstOrNull { it.id == order.listing }
            ?: vm.detail?.takeIf { it.id == order.listing }
        ProductPhoto(vm, product?.images?.firstOrNull()?.image, order.listingTitle, Modifier.size(66.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Order #TM-" + order.id, fontSize = 10.sp, color = Muted)
            Text(order.listingTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("by " + order.sellerName, color = Muted, fontSize = 11.sp)
        }
        Text("Rs. " + money(order.amount), color = Teal, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
@Composable fun OrdersScreen(vm: AppViewModel, chat: (Int, Int) -> Unit, tracking: (Int) -> Unit, dispute: (Int) -> Unit) {
    LoadScreen(vm, "orders") { vm.loadOrders() }
    var filter by rememberSaveable { mutableStateOf("All") }
    var reviewOrder by remember { mutableStateOf<Order?>(null) }
    var reviewText by rememberSaveable { mutableStateOf("") }
    var rating by rememberSaveable { mutableStateOf("5") }
    var reset by remember { mutableStateOf(false) }
    val me = vm.user ?: return
    val selected = vm.orders.filter { filter == "All" || orderLabel(it, vm.disputes) == filter }
    Column(Modifier.fillMaxSize()) {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("All", "In escrow", "Completed", "Refunded", "Dispute")) { label ->
                FilterChip(selected = filter == label, onClick = { filter = label }, label = {
                    Text(label + " (" + vm.orders.count { label == "All" || orderLabel(it, vm.disputes) == label } + ")", fontSize = 11.sp)
                }, shape = RoundedCornerShape(30.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Teal, selectedLabelColor = Color.White))
            }
        }
        Page {
            if (vm.recovery != null) Panel(color = Cream) {
                Text("Finish your pending test checkout", fontWeight = FontWeight.Bold)
                Text("Check for an existing order before starting another authorization.", fontSize = 12.sp)
                Action("Recover / finish checkout") { vm.completeCard { } }
                Link("Reset checkout recovery") { reset = true }
            }
            if (selected.isEmpty() && !vm.busy) Empty("No orders in this view yet.")
            selected.forEach { order ->
                Panel {
                    Text(order.createdAt.take(10), color = Muted, fontSize = 10.sp)
                    OrderItem(vm, order)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Status(orderLabel(order, vm.disputes))
                        Spacer(Modifier.weight(1f))
                        Link("Track Order") { tracking(order.id) }
                    }
                    if (canDispute(order, me, vm.disputes)) Link("Report a problem") { dispute(order.id) }
                    if (canReview(order, me, vm.myReviews)) {
                        Link("Leave a review") { reviewOrder = order; reviewText = ""; rating = "5" }
                    }
                    if (reviewOrder?.id == order.id) {
                        Choice("Rating", rating, (1..5).map { it.toString() to ("★".repeat(it)) }) { rating = it }
                        Field("Your review", reviewText, { reviewText = it }, multiline = true)
                        Action("Submit Review") { vm.review(order, rating.toInt(), reviewText) { reviewOrder = null; reviewText = "" } }
                        Link("Cancel") { reviewOrder = null }
                    }
                    Link(if (me.id == order.buyer) "Message seller" else "Message buyer") {
                        chat(if (me.id == order.buyer) order.sellerId else order.buyer, order.listing)
                    }
                }
            }
            Link("Refresh orders") { vm.runAction { vm.loadOrders() } }
        }
    }
    if (reset) Confirm("Reset checkout recovery?", "This does not cancel a Stripe authorization. If authorization completed but no order exists, recover it or ask your administrator to reconcile it first.",
        { vm.cancelCard(); reset = false }, { reset = false })
}

@Composable fun CheckoutSteps(active: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf("Review", "Payment", "Confirm").forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(28.dp).background(if (index <= active) Teal else Line, CircleShape), contentAlignment = Alignment.Center) {
                    Text(if (index < active) "✓" else (index + 1).toString(), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(label, fontSize = 10.sp, color = if (index <= active) Teal else Muted)
            }
        }
    }
}
@Composable fun PaymentChoice(title: String, subtitle: String, selected: Boolean, available: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.CreditCard, click: () -> Unit) {
    val enabled = available && !LocalBusy.current
    Surface(Modifier.fillMaxWidth().selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = click),
        shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp,
        border = BorderStroke(if (selected) 2.dp else 0.dp, if (selected) LocalAccent.current else Color.Transparent)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RadioButton(selected, onClick = null, enabled = enabled, colors = RadioButtonDefaults.colors(selectedColor = LocalAccent.current))
            Icon(icon, null, tint = if (available) Teal else Muted, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = if (available) Navy else Muted)
                Text(subtitle, fontSize = 11.sp, color = Muted)
            }
        }
    }
}
@Composable fun CheckoutProduct(vm: AppViewModel, item: Listing) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProductPhoto(vm, item.images.firstOrNull()?.image, item.title, Modifier.size(64.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(item.sellerName, color = Muted, fontSize = 11.sp); Status(item.status)
            }
            Text("Rs. " + money(item.price), color = Teal, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}
@Composable fun Invoice(item: Listing) {
    Panel {
        Row { Text("Item Price", Modifier.weight(1f), color = Muted); Text("Rs. " + money(item.price)) }
        Row { Text("Buyer Platform Fee", Modifier.weight(1f), color = Muted); Text("Rs. 0") }
        Text("The platform commission is deducted from the seller's payout.", color = Muted, fontSize = 11.sp)
        HorizontalDivider(color = Line)
        Row { Text("Total", Modifier.weight(1f), fontWeight = FontWeight.Bold); Text("Rs. " + money(item.price), fontWeight = FontWeight.Bold) }
    }
}

/** Review is separate from authorization; it never changes the order or reserves inventory. */
@Composable fun CheckoutScreen(vm: AppViewModel, id: Int, next: (String) -> Unit) {
    LoadScreen(vm, "checkout-$id") { vm.loadDetail(id) }
    var method by rememberSaveable(id) { mutableStateOf("card") }
    val item = vm.detail?.takeIf { it.id == id }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CheckoutSteps(0)
            if (item != null) {
                CheckoutProduct(vm, item)
                Text("Delivery", fontWeight = FontWeight.Bold)
                InfoCard("Arrange delivery with the seller", "Address saving and courier booking are not available yet. Agree delivery details in Chat.", Teal, Icons.Default.LocationOn)
                Text("Payment Method", fontWeight = FontWeight.Bold)
                PaymentChoice("Visa / Mastercard", "Demo simulation · no money collected", method == "card", BuildConfig.DEBUG) { method = "card" }
                if (BuildConfig.DEBUG) PaymentChoice("PayHere", "Demo simulation · no money collected", method == "payhere", icon = Icons.Default.AccountBalance) { method = "payhere" }
                PaymentChoice("Cash on Delivery", "Not supported by this backend", false, false, Icons.Default.LocalShipping) {}
                Invoice(item)
            } else if (!vm.busy) Empty("Could not load checkout. Return to the product and retry.")
        }
        if (item != null) BottomActions { Action("Proceed to Escrow Payment", canBuy(item, vm.user)) { next(method) } }
    }
}

@Composable fun EscrowScreen(vm: AppViewModel, id: Int, initialMethod: String, done: () -> Unit) {
    LoadScreen(vm, "escrow-$id") { vm.loadDetail(id) }
    var method by rememberSaveable(id) { mutableStateOf(if (initialMethod == "payhere" && BuildConfig.DEBUG) "payhere" else "card") }
    var demoConfirm by remember { mutableStateOf(false) }
    val item = vm.detail?.takeIf { it.id == id }
    CompositionLocalProvider(LocalAccent provides Orange) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                GradientHero("Escrow Payment", "Development checkout — test payments only", Orange, Icons.Default.Lock)
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (item != null) {
                        Panel(color = Cream) {
                            Text("Total for test checkout", color = Orange, fontSize = 12.sp)
                            Text("Rs. " + money(item.price), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text("No extra buyer platform fee", color = Muted, fontSize = 12.sp)
                        }
                        Text("Pay with", fontWeight = FontWeight.Bold)
                        PaymentChoice("Visa / Mastercard", "Demo simulation", method == "card", BuildConfig.DEBUG) { method = "card" }
                        if (BuildConfig.DEBUG) PaymentChoice("PayHere", "Simulated payment", method == "payhere", icon = Icons.Default.AccountBalance) { method = "payhere" }
                        if (method == "card") Panel {
                            Text("Demo card checkout", fontWeight = FontWeight.Bold)
                            Text("Matches the website's simulated checkout. No card number is needed, no money is collected and no Stripe authorization is created.", color = Muted, fontSize = 12.sp)
                        } else InfoCard("PayHere is a simulation", "Creates a demo order and marks this item sold. No money is collected.", Orange)
                        if (vm.recovery != null) InfoCard("Checkout needs recovery", "Open Orders to finish the previous checkout before paying again.", Orange)
                        Link("View my orders", done)
                    }
                }
            }
            if (item != null) BottomActions {
                Action("Create Demo Purchase", BuildConfig.DEBUG && canBuy(item, vm.user) && vm.recovery == null) { demoConfirm = true }
            }
        }
    }
    if (demoConfirm && item != null) Confirm("Create demo purchase?", item.title + " will be marked sold. No real payment is collected.",
        { vm.demoPurchase(item, method, done); demoConfirm = false }, { demoConfirm = false })
}

/** Courier milestones are explicitly unavailable until the backend exposes shipment data. */
@Composable fun TrackingScreen(vm: AppViewModel, id: Int, chat: (Int, Int) -> Unit, dispute: (Int) -> Unit) {
    LoadScreen(vm, "order-$id") { vm.loadOrders() }
    var confirm by remember { mutableStateOf(false) }
    val order = vm.orders.firstOrNull { it.id == id }
    val me = vm.user ?: return
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (order != null) {
                Panel { OrderItem(vm, order) }
                Surface(color = Teal, shape = RoundedCornerShape(17.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Default.Inventory2, null, tint = Color.White)
                        Column { Text(orderLabel(order, vm.disputes), color = Color.White, fontWeight = FontWeight.Bold); Text("Latest recorded order status", color = Color.White.copy(alpha = .8f), fontSize = 12.sp) }
                    }
                }
                Panel {
                    Text("Order Progress", fontWeight = FontWeight.Bold)
                    TimelineStep("Order confirmed", order.createdAt.take(16).replace('T', ' '), true)
                    TimelineStep("Payment " + order.escrowStatus, if (order.paymentMethod == "payhere") "Demo record; no real funds collected" else "Stripe test payment", true)
                    TimelineStep("Shipping updates", "Courier, tracking number and delivery estimate are not available.", false)
                    TimelineStep("Delivery confirmed", if (order.deliveryConfirmed) "Receipt confirmed by buyer" else "Confirm only after you receive and inspect the item.", order.deliveryConfirmed)
                }
                vm.disputes.filter { it.transaction == id }.forEach { case -> Panel(color = Blush) {
                    Text("Dispute #" + case.id + " · " + case.status, fontWeight = FontWeight.Bold)
                    Text(case.reason)
                    if (case.resolutionNotes.isNotBlank()) Text("Moderator: " + case.resolutionNotes)
                } }
                Action(if (order.buyer == me.id) "Message Seller" else "Message Buyer") {
                    chat(if (order.buyer == me.id) order.sellerId else order.buyer, order.listing)
                }
                if (canDispute(order, me, vm.disputes)) Link("Report a problem") { dispute(id) }
                Link("Refresh order") { vm.runAction { vm.loadOrders() } }
            } else if (!vm.busy) Empty("Order not found or unavailable to this account.")
        }
        if (order != null && canConfirm(order, me, vm.disputes.any { it.transaction == id })) BottomActions {
            Action("Confirm Delivery & Release Funds") { confirm = true }
        }
    }
    if (confirm && order != null) Confirm("Confirm receipt?", "Only confirm after receiving and inspecting " + order.listingTitle + ". This releases the test payment authorization.",
        { vm.confirmDelivery(order); confirm = false }, { confirm = false })
}
@Composable fun TimelineStep(title: String, detail: String, complete: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(if (complete) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null,
            tint = if (complete) Teal else Line, modifier = Modifier.size(21.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (complete) Navy else Muted)
            Text(detail, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable fun DisputeScreen(vm: AppViewModel, id: Int, done: () -> Unit) {
    LoadScreen(vm, "dispute-$id") { vm.loadOrders() }
    var reason by rememberSaveable(id) { mutableStateOf("Item not as described") }
    var detail by rememberSaveable(id) { mutableStateOf("") }
    val order = vm.orders.firstOrNull { it.id == id }
    val me = vm.user ?: return
    val allowed = order != null && canDispute(order, me, vm.disputes)
    CompositionLocalProvider(LocalAccent provides Red) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                GradientHero("Raise a Dispute", "A moderator will review your case", Red)
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (order != null) {
                        Panel { OrderItem(vm, order) }
                        Text("What went wrong?", fontWeight = FontWeight.Bold)
                        listOf("Item not as described", "Item not received", "Wrong item sent", "Item damaged").chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { choice ->
                                    Surface(onClick = { reason = choice }, enabled = allowed && !vm.busy, modifier = Modifier.weight(1f),
                                        color = if (reason == choice) Blush else Color.White, shape = RoundedCornerShape(13.dp),
                                        border = BorderStroke(if (reason == choice) 2.dp else 1.dp, if (reason == choice) Red else Line)) {
                                        Text(choice, Modifier.padding(horizontal = 14.dp, vertical = 20.dp), color = if (reason == choice) Red else Navy, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Field("Describe the issue", detail, { detail = it }, multiline = true, placeholder = "Explain what happened and what you expected…", enabled = allowed)
                        Text("Evidence", fontWeight = FontWeight.Bold)
                        InfoCard("File uploads are not supported yet", "Include the relevant details in your description. This backend does not accept dispute photos or attachments.", Red, Icons.Default.AttachFile)
                        if (!allowed && !vm.busy) Text("This order already has a case or is not eligible for a new dispute.", color = Red)
                    } else if (!vm.busy) Empty("Order not found.")
                }
            }
            BottomActions { Action("Submit Dispute", allowed && detail.isNotBlank()) {
                order?.let { vm.dispute(it, reason + "\n\n" + detail.trim(), done) }
            } }
        }
    }
}
