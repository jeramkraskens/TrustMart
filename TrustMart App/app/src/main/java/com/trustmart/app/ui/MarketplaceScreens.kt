// Compose screens for browsing the marketplace and inspecting product and seller information.
package com.trustmart.app.ui

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.trustmart.app.AppViewModel
import com.trustmart.app.data.*

val conditions = listOf("new" to "Like New", "excellent" to "Excellent", "good" to "Good", "fair" to "Fair")


@Composable fun ProductPhoto(vm: AppViewModel, path: String?, title: String, modifier: Modifier = Modifier) {
    val canvas = modifier.background(Color(0xFFF0F0F2))
    if (path == null) Box(canvas, contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Image, "No product photo", tint = Color(0xFFB9BEC3), modifier = Modifier.size(34.dp))
    } else SubcomposeAsyncImage(model = vm.repo.mediaUrl(path), contentDescription = title, modifier = canvas,
        contentScale = ContentScale.Fit, loading = { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) } },
        error = { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.BrokenImage, "Photo unavailable", tint = Muted) } })
}

@Composable fun ProductCard(vm: AppViewModel, item: Listing, open: (Int) -> Unit) {
    Card(onClick = { if (!vm.busy) open(item.id) }, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Box {
            ProductPhoto(vm, item.images.firstOrNull()?.image, item.title, Modifier.fillMaxWidth().height(116.dp))
            Box(Modifier.align(Alignment.BottomStart).padding(8.dp)) { Status(item.status) }
            if (vm.user?.role == "buyer") IconButton({ vm.toggleWishlist(item.id) }, enabled = !vm.busy,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(48.dp)) {
                Icon(if (vm.wishlist.any { it.listing == item.id }) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Toggle wishlist for " + item.title, tint = Teal, modifier = Modifier.background(Color.White, CircleShape).padding(7.dp).size(18.dp))
            }
        }
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Rs. " + money(item.price), color = Teal, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(item.location.ifBlank { item.categoryName.orEmpty().ifBlank { "Uncategorised" } }, color = Muted, fontSize = 10.sp, maxLines = 1)
            Button({ open(item.id) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp), shape = RoundedCornerShape(9.dp)) {
                Text(if (canBuy(item, vm.user)) "Buy Safely" else "View", fontSize = 12.sp)
            }
        }
    }
}

@Composable fun HomeHero(name: String, seller: Boolean, notifications: () -> Unit, settings: () -> Unit, action: () -> Unit, unread: Int = 0) {
    val color = if (seller) Orange else Teal
    Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(if (seller) Color(0xFFDC850B) else Color(0xFF10756C), color)))
        .padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Welcome back,", color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
                Text(name + "!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(notifications, enabled = !LocalBusy.current) {
                BadgedBox(badge = { if (unread > 0) Badge { Text(unread.toString()) } }) {
                    Icon(Icons.Default.NotificationsNone, "Notifications, $unread unread", tint = Color.White)
                }
            }
            IconButton(settings, enabled = !LocalBusy.current) { Icon(Icons.Default.Settings, "Server settings", tint = Color.White) }
        }
        Surface(onClick = action, enabled = !LocalBusy.current, shape = RoundedCornerShape(15.dp), color = Color.White) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(if (seller) Icons.Default.Add else Icons.Default.Search, null, tint = color, modifier = Modifier.size(20.dp))
                Text(if (seller) "Add New Listing" else "Search verified listings…", color = if (seller) Orange else Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable fun HomeScreen(vm: AppViewModel, open: (Int) -> Unit, search: () -> Unit,
    categories: () -> Unit, category: (Int) -> Unit, notifications: () -> Unit, settings: () -> Unit) {
    LoadScreen(vm, "home-" + vm.user?.id + vm.server) { vm.loadHome() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HomeHero(vm.user?.firstName?.ifBlank { vm.user?.username.orEmpty() } ?: "TrustMart", false, notifications, settings, search, vm.unreadNotifications)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Surface(color = Teal, shape = RoundedCornerShape(17.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Shop with confidence.", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Verified listings. Moderator support.", color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                        TextButton({ vm.inform("Listings are reviewed before going live. Both Card and PayHere are demo simulations. No real payment is collected.") },
                            colors = ButtonDefaults.textButtonColors(containerColor = Color.White), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                            Text("Learn more →", fontSize = 11.sp)
                        }
                    }
                    Icon(Icons.Default.Lock, null, tint = Cream, modifier = Modifier.size(42.dp))
                }
            }
            SectionHeading("Categories", click = categories)
            Promotions(vm)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(vm.categories, key = { it.id }) { item ->
                    Surface(onClick = { category(item.id) }, enabled = !vm.busy, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
                        Column(Modifier.width(70.dp).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            CategoryIcon(categoryDesign(item.name))
                            Text(item.name, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            SectionHeading(if (vm.recommended.isEmpty()) "Verified Picks for You" else "Recommended for You", click = search)
            val picks = vm.recommended.ifEmpty { vm.listings }.take(4)
            if (picks.isEmpty() && !vm.busy) Empty("No verified products yet. Try Search or refresh after moderator approval.")
            ProductRows(vm, picks, open)
            SectionHeading("Latest Finds", click = search)
            vm.listings.take(4).forEach { item ->
                Surface(onClick = { open(item.id) }, enabled = !vm.busy, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Circle, null, tint = Teal, modifier = Modifier.size(8.dp))
                        Text(item.title, modifier = Modifier.weight(1f).padding(horizontal = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                        Text("Rs. " + money(item.price), fontSize = 12.sp, color = Teal)
                        Icon(Icons.Default.ChevronRight, null, tint = Muted)
                    }
                }
            }
            Link("Refresh marketplace") { vm.runAction { vm.loadHome() } }
        }
    }
}
@Composable fun ProductRows(vm: AppViewModel, items: List<Listing>, open: (Int) -> Unit) {
    items.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            pair.forEach { Box(Modifier.weight(1f)) { ProductCard(vm, it, open) } }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable fun CategoriesScreen(vm: AppViewModel, open: (Int) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    LoadScreen(vm, "categories") { vm.loadHome() }
    LazyVerticalGrid(GridCells.Adaptive(155.dp), contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { Field("", query, { query = it }, placeholder = "Search categories…") }
        items(vm.categories.filter { it.name.contains(query, true) }, key = { it.id }) { item ->
            val design = categoryDesign(item.name)
            Surface(onClick = { open(item.id) }, enabled = !vm.busy, shape = RoundedCornerShape(17.dp), color = Color.White, shadowElevation = 2.dp) {
                Row(Modifier.heightIn(min = 84.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryIcon(design)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(vm.listings.count { it.category == item.id }.toString() + " verified", color = design.accent, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable fun SearchScreen(vm: AppViewModel, open: (Int) -> Unit, initialCategory: Int = 0) {
    var search by rememberSaveable(initialCategory) { mutableStateOf("") }
    var category by rememberSaveable(initialCategory) { mutableStateOf(if (initialCategory == 0) "" else initialCategory.toString()) }
    var condition by rememberSaveable(initialCategory) { mutableStateOf("") }
    var order by rememberSaveable(initialCategory) { mutableStateOf("-created_at") }
    var budget by rememberSaveable(initialCategory) { mutableStateOf("") }
    var district by rememberSaveable(initialCategory) { mutableStateOf("") }
    var city by rememberSaveable(initialCategory) { mutableStateOf("") }
    var filters by rememberSaveable { mutableStateOf(false) }
    LoadScreen(vm, "search-$initialCategory") { vm.loadHome(search, category, condition, order, district, city) }
    val results = belowBudget(vm.listings, budget)
    LazyVerticalGrid(GridCells.Adaptive(155.dp), contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("", search, { search = it }, placeholder = "Search verified listings…")
                SearchSuggestions(vm, search, category) { suggestion ->
                    val target = android.net.Uri.parse(suggestion.url)
                    val productId = target.pathSegments.takeIf { it.firstOrNull() == "listings" }?.getOrNull(1)?.toIntOrNull()
                    if (productId != null) open(productId)
                    else {
                        category = target.getQueryParameter("category").orEmpty()
                        district = target.getQueryParameter("district").orEmpty()
                        city = target.getQueryParameter("city").orEmpty()
                        search = ""
                        vm.runAction { vm.loadHome(search, category, condition, order, district, city) }
                    }
                }
                Text(results.size.toString() + " verified listings found", color = Muted, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Status("Verified only")
                    Spacer(Modifier.weight(1f))
                    Link(if (filters) "Hide filters" else "Filter & sort") { filters = !filters }
                }
                if (filters) {
                    Choice("Category", category, listOf("" to "All categories") + vm.categories.map { it.id.toString() to it.name }) { category = it }
                    Choice("Condition", condition, listOf("" to "All conditions") + conditions) { condition = it }
                    Choice("District", district, listOf("" to "All districts") + vm.locations.keys.map { it to it }) { district = it; city = "" }
                    Choice("Town / city", city, listOf("" to "All towns") + vm.locations[district].orEmpty().map { it to it }) { city = it }
                    Field("Maximum price (Rs.)", budget, { budget = it }, keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    Choice("Sort", order, listOf("-created_at" to "Newest first", "price" to "Price: low to high", "-price" to "Price: high to low")) { order = it }
                    Link("Clear filters") { category = ""; condition = ""; budget = ""; district = ""; city = ""; order = "-created_at"; vm.runAction { vm.loadHome(search) } }
                }
                Action("Search", budget.isBlank() || budget.toBigDecimalOrNull()?.signum()?.let { it >= 0 } == true) {
                    vm.runAction { vm.loadHome(search, category, condition, order, district, city) }
                }
            }
        }
        if (results.isEmpty() && !vm.busy) item(span = { GridItemSpan(maxLineSpan) }) { Empty("No matches. Try another keyword or clear your filters.") }
        items(results, key = { it.id }) { ProductCard(vm, it, open) }
    }
}

@Composable fun ListingScreen(vm: AppViewModel, id: Int, message: (Listing) -> Unit, login: () -> Unit,
    checkout: (Listing) -> Unit, seller: (Int) -> Unit) {
    LoadScreen(vm, id) { vm.loadDetail(id) }
    val item = vm.detail?.takeIf { it.id == id }
    var selected by rememberSaveable(id) { mutableIntStateOf(0) }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (item != null) {
                Box {
                    ProductPhoto(vm, item.images.getOrNull(selected)?.image ?: item.images.firstOrNull()?.image, item.title,
                        Modifier.fillMaxWidth().height(255.dp))
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
                        if (vm.user?.role == "buyer") {
                            val saved = vm.wishlist.any { it.listing == id }
                            RoundIcon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                if (saved) "Remove from wishlist" else "Save to wishlist") { vm.toggleWishlist(id) }
                        }
                        Spacer(Modifier.width(8.dp))
                        RoundIcon(Icons.Default.Share, "Share listing") {
                            val text = item.title + "\nRs. " + money(item.price) + "\nSeller: " + item.sellerName + "\nTrustMart listing #" + item.id
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share listing"))
                        }
                    }
                    Row(Modifier.align(Alignment.BottomStart).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Status(item.status); Status("Test escrow")
                    }
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(item.images.indices.toList()) { index ->
                            Box(Modifier.size(70.dp).clip(RoundedCornerShape(12.dp))
                                .border(if (selected == index) 2.dp else 0.dp, if (selected == index) Teal else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { selected = index }) {
                                ProductPhoto(vm, item.images[index].image, "View photo " + (index + 1), Modifier.fillMaxSize())
                            }
                        }
                    }
                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("★ " + averageRating(vm.reviews) + "  ·  " + vm.reviews.size + " seller reviews", color = Orange, fontSize = 12.sp)
                    Text("Rs. " + money(item.price), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Status(conditions.firstOrNull { it.first == item.condition }?.second ?: item.condition)
                    HorizontalDivider(color = Line)
                    Surface(onClick = { seller(item.seller) }, enabled = !vm.busy, color = Color.Transparent) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Avatar(item.sellerName)
                            Column(Modifier.weight(1f)) { Text(item.sellerName, fontWeight = FontWeight.SemiBold); Text("Trust score " + item.sellerTrustScore, color = Muted, fontSize = 11.sp) }
                            Icon(Icons.Default.ChevronRight, null, tint = Muted)
                        }
                    }
                    InfoCard("Protected test checkout", "Inspect your item before confirming receipt. This app uses test payments.", Orange, Icons.Default.Lock)
                    Text("About this item", fontWeight = FontWeight.Bold)
                    Text(item.description, color = Muted)
                    Text(item.categoryName.orEmpty().ifBlank { "Uncategorised" } + " · " + item.location, fontSize = 12.sp)
                    if (!item.contactPhone.isNullOrBlank()) {
                        Text("Contact: " + item.contactPhone)
                        Link("Call seller") {
                            val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + item.contactPhone))
                            try { context.startActivity(intent) } catch (_: android.content.ActivityNotFoundException) { vm.inform("No dialer is installed.") }
                        }
                        if (item.whatsappEnabled) Link("WhatsApp seller") {
                            val phone = item.contactPhone.let { if (it.startsWith("0")) "94" + it.drop(1) else it }
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/$phone"))) }
                            catch (_: android.content.ActivityNotFoundException) { vm.inform("No app can open this link.") }
                        }
                    }
                    Link("Refresh product") { vm.runAction { vm.loadDetail(id) } }
                }
            } else {
                // The parent already scrolls. Nesting Page here adds a second vertical scroller
                // with unbounded height and crashes before loading starts or after a request fails.
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (vm.busy) Text("Loading product…", color = Muted)
                    else {
                        Empty("Unable to load this listing.")
                        Link("Retry") { vm.runAction { vm.loadDetail(id) } }
                    }
                }
            }
        }
        if (item != null) BottomActions {
            if (vm.user == null) Action("Sign in to buy or message", click = login)
            else {
                if (vm.user?.id != item.seller) Box(Modifier.weight(1f)) { OutlineAction("Chat") { message(item) } }
                Box(Modifier.weight(1.4f)) { Action(if (canBuy(item, vm.user)) "Buy Safely" else item.status.replace('_', ' '), canBuy(item, vm.user)) { checkout(item) } }
            }
        }
    }
}

@Composable fun SellerProfileScreen(vm: AppViewModel, id: Int, open: (Int) -> Unit, message: (Int) -> Unit) {
    LoadScreen(vm, "seller-profile-$id") { vm.loadMerchant(id) }
    val first = vm.merchantListings.firstOrNull() ?: vm.detail?.takeIf { it.seller == id }
    val name = first?.sellerName ?: "Seller #$id"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(DeepTeal, Teal))).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth()) { BackCircle(true) }
            Avatar(name, 72)
            Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("TrustMart seller", color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Metric(averageRating(vm.merchantReviews), "Rating")
                    Metric(vm.merchantListings.size.toString(), "Active listings")
                    Metric(first?.sellerTrustScore?.toString() ?: "—", "Trust score")
                }
            }
            if (vm.user?.id != id) Action("Message Seller") { message(id) }
            SectionHeading("Active Listings (" + vm.merchantListings.size + ")")
            ProductRows(vm, vm.merchantListings, open)
            if (vm.merchantListings.isEmpty() && !vm.busy) Empty("No active verified listings.")
            Text("Recent Reviews", fontWeight = FontWeight.Bold)
            if (vm.merchantReviews.isEmpty() && !vm.busy) Empty("No reviews yet.")
            vm.merchantReviews.forEach { review -> Panel {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(review.reviewerName); Column { Text(review.reviewerName, fontWeight = FontWeight.SemiBold); Text("★".repeat(review.rating.coerceIn(0, 5)), color = Orange) }
                }
                Text(review.comment, color = Muted, fontSize = 12.sp)
            } }
        }
    }
}
@Composable fun Metric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp); Text(label, color = Muted, fontSize = 10.sp)
    }
}

@Composable fun WishlistScreen(vm: AppViewModel, open: (Int) -> Unit) {
    LoadScreen(vm, "wishlist") { vm.loadWishlist() }
    Page(subtitle = "Save a find and come back when you're ready.") {
        Link("Refresh") { vm.runAction { vm.loadWishlist() } }
        if (vm.wishlist.isEmpty() && !vm.busy) Empty("No saved listings yet.")
        ProductRows(vm, vm.wishlist.map { it.listingDetail }, open)
    }
}
