package com.marketplace.moderator.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketplace.moderator.data.*
import com.marketplace.moderator.data.models.Notification
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class NotificationState(
    val items: List<Notification> = emptyList(), val status: String = "Connecting…",
    val error: String? = null, val alert: Notification? = null, val saving: Boolean = false
) { val unread: Int get() = items.count { !it.isRead } }

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val api get() = RetrofitClient.getApiService(getApplication<Application>())
    private val tokens = TokenManager(application)
    private val mutex = Mutex()
    private var highest: Int? = null
    private var generation = 0
    private val state = MutableStateFlow(NotificationState())
    val uiState = state.asStateFlow()

    fun clear() { generation++; highest = null; state.value = NotificationState() }
    fun dismiss() { state.value = state.value.copy(alert = null) }

    private suspend fun refresh() = mutex.withLock {
        val current = generation
        val me = api.getMe()
        if (!me.isSuccessful) throw retrofit2.HttpException(me)
        if (me.body()?.role != "moderator") { tokens.clear(); return@withLock }
        val items = api.allNotifications()
        if (current != generation) return@withLock
        val newest = highest?.let { last -> items.firstOrNull { it.id > last && !it.isRead } }
        highest = maxOf(highest ?: 0, items.maxOfOrNull { it.id } ?: 0)
        state.value = state.value.copy(items = items, alert = newest ?: state.value.alert, error = null)
    }

    suspend fun observe() {
        observeLiveMessages(tokens.server, { tokens.getAccessToken() }, { refresh() },
            { state.value = state.value.copy(status = it) }, resource = "notifications")
    }

    fun load() = viewModelScope.launch {
        try { refresh() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { state.value = state.value.copy(error = "Could not load notifications. Please retry.") }
    }

    fun markRead(id: Int? = null) {
        if (state.value.saving) return
        state.value = state.value.copy(saving = true, error = null)
        val current = generation
        viewModelScope.launch {
            try {
                val response = if (id == null) api.markAllNotificationsRead() else api.markNotificationRead(id)
                if (!response.isSuccessful) throw retrofit2.HttpException(response)
                if (current == generation) {
                    state.value = state.value.copy(items = state.value.items.map {
                        if (id == null || it.id == id) it.copy(isRead = true) else it
                    })
                    refresh()
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (current == generation) state.value = state.value.copy(error = "Could not mark notifications as read. Please retry.")
            } finally { if (current == generation) state.value = state.value.copy(saving = false) }
        }
    }
}
