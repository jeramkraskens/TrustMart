// Fetches complete moderation collections across API pages.
// This prevents screen filters and totals from considering only the first page.
package com.marketplace.moderator.data

/** Collects paginated moderation records before deriving filters and totals. HTTP failures are propagated, not treated as empty queues. */

import com.marketplace.moderator.data.models.*
import retrofit2.HttpException
import retrofit2.Response

/** Read every API page, so filters and dashboard totals include the whole queue. */
private suspend fun <T> allPages(fetch: suspend (Int) -> Response<PaginatedResponse<T>>): List<T> {
    val result = mutableListOf<T>()
    var page = 1
    do {
        check(page <= 1000) { "Too many pages. Narrow the request." }
        val response = fetch(page++)
        if (!response.isSuccessful) throw HttpException(response)
        val body = response.body() ?: error("The server returned an empty response.")
        check(body.results.isNotEmpty() || body.next == null) { "The server returned an empty page with more pages. Please retry." }
        result.addAll(body.results)
    } while (body.next != null)
    return result
}

suspend fun ApiService.allVerifications(): List<VerificationRequest> = allPages { getVerifications(page = it) }
suspend fun ApiService.allDisputes(): List<Dispute> = allPages { getDisputes(page = it) }
suspend fun ApiService.allMessages(): List<Message> = allPages { getMessages(page = it) }
suspend fun ApiService.allNotifications(): List<Notification> = allPages { getNotifications(page = it) }
