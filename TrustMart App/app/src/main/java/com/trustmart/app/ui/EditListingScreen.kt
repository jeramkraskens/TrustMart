// Loads an existing listing into an edit form and sends changes through AppViewModel.
package com.trustmart.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trustmart.app.AppViewModel
import com.trustmart.app.data.Listing
import com.trustmart.app.data.validPhone
import com.trustmart.app.data.selectedPhotos

/** The server's can_manage flag protects order history. A fresh check also runs before each write. */
@Composable fun EditListingScreen(vm: AppViewModel, id: Int, done: () -> Unit) {
    LoadScreen(vm, "edit-$id") { vm.loadSeller(); vm.loadDetail(id) }
    // Resuming after a document picker reloads detail. Keep the editor mounted during
    // that refresh, otherwise rememberSaveable fields and selected photos are discarded.
    var cached by remember(id, vm.user?.id) { mutableStateOf<Listing?>(null) }
    val fresh = vm.detail?.takeIf { it.id == id && it.seller == vm.user?.id }
    val item = fresh ?: cached
    SideEffect { if (fresh != null) cached = fresh }
    if (item == null) { Page { Empty("Loading your listing. Return to Listings and retry if it is unavailable.") }; return }
    if (!item.canManage) { Page { Empty("Sold listings and listings with orders cannot be edited or deleted.") }; return }
    key(id) { ListingEditor(vm, item, done) }
}

@Composable private fun ListingEditor(vm: AppViewModel, item: Listing, done: () -> Unit) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(item.title) }
    var description by rememberSaveable { mutableStateOf(item.description) }
    var price by rememberSaveable { mutableStateOf(item.price) }
    var category by rememberSaveable { mutableStateOf(item.category.toString()) }
    var condition by rememberSaveable { mutableStateOf(item.condition) }
    var district by rememberSaveable { mutableStateOf(item.district.orEmpty()) }
    var city by rememberSaveable { mutableStateOf(item.city.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(item.contactPhone.orEmpty()) }
    var whatsapp by rememberSaveable { mutableStateOf(item.whatsappEnabled) }
    var removed by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var photos by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { selected ->
        val accepted = selectedPhotos(photos, selected.map(Uri::toString), 6 - item.images.size + removed.size)
        if (selected.isNotEmpty()) accepted.forEach { value ->
            try { context.contentResolver.takePersistableUriPermission(Uri.parse(value), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: SecurityException) { /* Some document providers offer only temporary permission. */ }
        }
        photos = accepted
    }
    val remaining = item.images.count { it.id !in removed } + photos.size
    Page("Edit Listing", "Saving resubmits this item for moderator review. Existing evidence is retained.") {
        Field("Product Title", title, { title = it })
        Field("Description", description, { description = it }, multiline = true)
        Field("Price (Rs.)", price, { price = it }, keyboard = KeyboardType.Decimal)
        Choice("Category", category, vm.categories.map { it.id.toString() to it.name }) { category = it }
        Choice("Condition", condition, conditions) { condition = it }
        Choice("District", district, listOf("" to "Select district") + vm.locations.keys.map { it to it }) { district = it; city = "" }
        Choice("Town / city", city, listOf("" to "Select town") + vm.locations[district].orEmpty().map { it to it }) { city = it }
        Field("Contact phone (10 digits)", phone, { phone = it }, keyboard = KeyboardType.Phone)
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(whatsapp, { whatsapp = it }, enabled = !vm.busy); Text("Allow WhatsApp contact") }
        item.images.forEach { image ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductPhoto(vm, image.image, "Existing product photo", Modifier.size(70.dp))
                Checkbox(image.id !in removed, { keep -> removed = if (keep) removed - image.id else removed + image.id }, enabled = !vm.busy)
                Text("Keep photo")
            }
        }
        OutlineAction("Select additional photos (${photos.size})") { picker.launch(arrayOf("image/*")) }
        if (photos.isNotEmpty()) Link("Clear new photos") { photos = emptyList() }
        Action("Save and resubmit", title.isNotBlank() && description.isNotBlank() &&
            price.toBigDecimalOrNull()?.let { it.signum() >= 0 && it.scale() <= 2 } == true &&
            validPhone(phone) && city in vm.locations[district].orEmpty() && remaining in 1..6) {
            val fields = mapOf("title" to title.trim(), "description" to description.trim(), "price" to price,
                "category" to category, "condition" to condition, "district" to district, "city" to city,
                "contact_phone" to phone, "whatsapp_enabled" to whatsapp.toString()) +
                removed.mapIndexed { index, photo -> "removed_image_ids[$index]" to photo.toString() }
            vm.editListing(item.id, fields, photos.map(Uri::parse), done)
        }
        OutlineAction("Delete listing") { confirmDelete = true }
    }
    if (confirmDelete) Confirm("Delete listing?", "This permanently removes this listing and its saved-list entries. This cannot be undone from the app.",
        { confirmDelete = false; vm.deleteListing(item.id, done) }, { confirmDelete = false })
}
