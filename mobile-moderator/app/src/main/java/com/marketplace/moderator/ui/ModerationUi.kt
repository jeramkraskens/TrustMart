// Reusable UI elements shared by moderation screens.
package com.marketplace.moderator.ui

/** Shared lifecycle refresh, queue filters, and image rendering keep behavior consistent across moderation screens. */

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import coil.compose.SubcomposeAsyncImage
import com.marketplace.moderator.ui.theme.*
import java.net.URI
import androidx.compose.ui.platform.LocalContext
import com.marketplace.moderator.data.TokenManager

/** Reload on return from a detail screen, including restored bottom-navigation tabs. */
@Composable
fun RefreshOnResume(refresh: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(refresh)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) latest()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

@Composable
fun QueueFilters(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { label ->
            FilterChip(selected = selected == label, onClick = { onSelect(label) },
                label = { Text(label, fontSize = 12.sp) }, shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Teal,
                    selectedLabelColor = androidx.compose.ui.graphics.Color.White))
        }
    }
}

/** Relative media paths use the same server as the API on a physical phone. */
@Composable
fun ProductPhoto(url: String?, modifier: Modifier = Modifier) {
    val server = TokenManager(LocalContext.current).server
    val resolved = remember(url, server) { url?.let { URI(server).resolve(it).toString() } }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(PaperWarm), contentAlignment = Alignment.Center) {
        if (resolved == null) Text("No photo", fontSize = 11.sp, color = Ink.copy(alpha = .45f))
        else SubcomposeAsyncImage(model = resolved, contentDescription = "Listing photo",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), color = Teal) } },
            error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Photo unavailable", fontSize = 11.sp) } })
    }
}
