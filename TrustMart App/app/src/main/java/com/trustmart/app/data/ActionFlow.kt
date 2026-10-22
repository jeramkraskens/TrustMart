// Separates a successful write from the refresh that follows it.
// A failed refresh must not tell the user to repeat a write that already succeeded.
package com.trustmart.app.data

import kotlinx.coroutines.CancellationException

/** A successful write stays successful when the following GET fails. Clear drafts before refreshing. */
suspend fun acknowledgeThenRefresh(acknowledge: () -> Unit, refresh: suspend () -> Unit,
    refreshFailed: (Exception) -> Unit) {
    acknowledge()
    try { refresh() }
    catch (e: CancellationException) { throw e }
    catch (e: Exception) { refreshFailed(e) }
}
