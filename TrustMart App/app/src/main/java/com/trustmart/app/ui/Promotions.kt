// Compose UI for displaying marketplace promotions.
package com.trustmart.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.trustmart.app.AppViewModel

/** Active dates and offer calculations come from the same advertisement API as the website. */
@Composable fun Promotions(vm: AppViewModel) {
    val context = LocalContext.current
    vm.advertisements.forEach { ad ->
        Panel(color = Cream) {
            Text("Sponsored · " + ad.title)
            ad.image?.let { ProductPhoto(vm, it, ad.title, Modifier.fillMaxWidth().height(130.dp)) }
            Text(ad.description)
            ad.originalPrice?.let { Text("Original: Rs. " + money(it)) }
            ad.effectiveOfferPrice?.let { Text("Offer: Rs. " + money(it)) }
            ad.effectiveDiscountPercent?.let { Text("$it% off") }
            ad.linkUrl?.let { url ->
                val uri = Uri.parse(url)
                if (uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank()) Link("View offer") {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    catch (_: android.content.ActivityNotFoundException) { vm.inform("No browser is installed.") }
                }
            }
        }
    }
}
