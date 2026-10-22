package com.marketplace.moderator.data

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit

private val messageSocketClient = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()

/** Runs only while the chat screen is started. REST recovers history after every reconnect. */
suspend fun observeLiveMessages(
    server: String,
    token: suspend () -> String?,
    refresh: suspend () -> Unit,
    status: (String) -> Unit,
    resource: String = "messages"
) {
    var failures = 0
    while (currentCoroutineContext().isActive) {
        val events = Channel<String>(Channel.CONFLATED)
        var socket: WebSocket? = null
        try {
            status("Connecting…")
            refresh() // Existing HTTP authentication renews expired tokens before connecting.
            val access = token() ?: return
            val base = server.toHttpUrl()
            val url = base.newBuilder().encodedPath(base.encodedPath.trimEnd('/').removeSuffix("/api") + "/ws/$resource/").build()
            socket = messageSocketClient.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(Gson().toJson(mapOf("token" to access)))
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    val type = runCatching { JsonParser.parseString(text).asJsonObject["type"]?.asString }.getOrNull()
                    if (type == "ready" || type == "$resource.changed") events.trySend(type)
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { events.trySend("closed") }
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                    events.trySend("closed")
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { events.trySend("closed") }
            })
            var ready = false
            while (currentCoroutineContext().isActive) {
                val event = withTimeoutOrNull(15000) { events.receive() }
                if (event == "closed" || (event == null && !ready)) break
                if (event != null && !ready) { ready = true; failures = 0; status("Live") }
                refresh() // Includes periodic recovery for a missed broadcast.
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep the draft and last loaded history while retrying.
        } finally {
            socket?.cancel()
            events.close()
        }
        status("Reconnecting…")
        delay(minOf(30000L, 1000L * (1L shl minOf(failures++, 5))))
    }
}
