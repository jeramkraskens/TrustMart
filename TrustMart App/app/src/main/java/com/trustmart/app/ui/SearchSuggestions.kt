// Displays suggested search results while the user enters a marketplace query.
package com.trustmart.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalInspectionMode
import com.trustmart.app.AppViewModel
import com.trustmart.app.data.SearchSuggestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Debounced public suggestions share the website endpoint. Cancelling an old query prevents stale results. */
@Composable fun SearchSuggestions(vm: AppViewModel, query: String, category: String, choose: (SearchSuggestion) -> Unit) {
    var results by remember { mutableStateOf<List<SearchSuggestion>>(emptyList()) }
    val preview = LocalInspectionMode.current
    LaunchedEffect(query, category, vm.server) {
        results = emptyList()
        if (preview || query.trim().length < 2) return@LaunchedEffect
        delay(300)
        try {
            results = vm.repo.get("listings/suggestions/", mapOf("q" to query.trim(), "category" to category))
                .asJsonArray.map { vm.repo.decode(it, SearchSuggestion::class.java) }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { /* Suggestions are optional; explicit Search still reports failures. */ }
    }
    if (results.isNotEmpty()) Panel {
        results.take(8).forEach { suggestion ->
            Link(suggestion.kind + ": " + suggestion.label) { choose(suggestion) }
            Text(suggestion.detail, color = Muted)
        }
    }
}
