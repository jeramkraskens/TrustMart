// State and API actions for conversations.
// The screen observes this state; viewModelScope runs asynchronous work within the ViewModel lifetime.
package com.marketplace.moderator.ui.chat

/** Loads all message pages, groups conversations by the other participant, and submits replies through Django. */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketplace.moderator.data.RetrofitClient
import com.marketplace.moderator.data.allMessages
import com.marketplace.moderator.data.models.SendMessageRequest
import com.marketplace.moderator.data.TokenManager
import com.marketplace.moderator.data.models.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.marketplace.moderator.data.observeLiveMessages

data class Conversation(
    val otherUserId: Int,
    val otherUserName: String,
    val lastMessage: String,
    val sentAt: String
)

data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val loading: Boolean = true,
    val messages: List<Message> = emptyList(),
    val sending: Boolean = false,
    val error: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val api get() = RetrofitClient.getApiService(getApplication<Application>())
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val refreshMutex = Mutex()

    suspend fun observe(status: (String) -> Unit) {
        observeLiveMessages(tokenManager.server, { tokenManager.getAccessToken() }, { refresh() }, status)
    }

    private suspend fun refresh() = refreshMutex.withLock {
        refreshMessages()
    }

    fun load() {
        viewModelScope.launch {
            try { refresh() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { }
        }
    }

    private suspend fun refreshMessages() {
        _uiState.value = _uiState.value.copy(loading = _uiState.value.messages.isEmpty())
        try {
            val response = api.getMe()
            if (!response.isSuccessful) throw retrofit2.HttpException(response)
            val me = response.body()
            val messages = api.allMessages()

            val conversations = messages
                .groupBy { if (it.sender == me?.id) it.receiver to it.receiverName else it.sender to it.senderName }
                .map { (key, msgs) ->
                    val latest = msgs.maxByOrNull { it.sentAt }!!
                    Conversation(
                        otherUserId = key.first,
                        otherUserName = key.second,
                        lastMessage = latest.content,
                        sentAt = latest.sentAt
                    )
                }
                .sortedByDescending { it.sentAt }

            // Refresh must not unlock Send while its request is still in flight.
            _uiState.value = _uiState.value.copy(conversations = conversations, messages = messages, loading = false, error = null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(loading = false, error = "Could not update messages. Reconnecting…")
            throw e
        }
    }

    /** Keep the draft until the server confirms receipt, so failed sends can be retried. */
    fun send(receiver: Int, content: String, onSent: () -> Unit) {
        if (content.isBlank() || _uiState.value.sending) return
        _uiState.value = _uiState.value.copy(sending = true, error = null)
        viewModelScope.launch {
            try {
                val response = api.sendMessage(SendMessageRequest(receiver, content.trim()))
                if (!response.isSuccessful) throw retrofit2.HttpException(response)
                onSent()
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Message was not sent. Please try again.")
            } finally { _uiState.value = _uiState.value.copy(sending = false) }
        }
    }
}
