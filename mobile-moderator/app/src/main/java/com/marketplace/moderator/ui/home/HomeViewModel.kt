// State and API actions for dashboard summaries.
// The screen observes this state; viewModelScope runs asynchronous work within the ViewModel lifetime.
package com.marketplace.moderator.ui.home

/** Loads full moderation queues and computes dashboard totals. Errors remain visible rather than masquerading as an empty workload. */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketplace.moderator.data.RetrofitClient
import com.marketplace.moderator.data.allVerifications
import com.marketplace.moderator.data.allDisputes
import com.marketplace.moderator.data.models.Dispute
import com.marketplace.moderator.data.models.VerificationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

sealed class UrgentAction {
    data class VerificationAction(val item: VerificationRequest) : UrgentAction()
    data class DisputeAction(val item: Dispute) : UrgentAction()
}

data class HomeUiState(
    val moderatorName: String = "",
    val pendingVerifications: Int = 0,
    val openDisputes: Int = 0,
    val resolvedToday: Int = 0,
    val urgentActions: List<UrgentAction> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val api get() = RetrofitClient.getApiService(getApplication<Application>())

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            try {
                val meResponse = api.getMe()
                if (!meResponse.isSuccessful) throw retrofit2.HttpException(meResponse)
                val verifications = api.allVerifications()
                val disputes = api.allDisputes()

                val me = meResponse.body()
                val pending = verifications.filter { it.status == "pending" }
                val open = disputes.filter { it.status == "open" }

                val actions = (pending.take(2).map { UrgentAction.VerificationAction(it) } +
                        open.take(2).map { UrgentAction.DisputeAction(it) }).take(3)

                _uiState.value = HomeUiState(
                    moderatorName = me?.let { "${it.firstName} ${it.lastName}".trim().ifBlank { it.username } } ?: "",
                    pendingVerifications = pending.size,
                    openDisputes = open.size,
                    // API timestamps use UTC; compare against the same calendar boundary.
                    resolvedToday = disputes.count { it.status == "resolved" && it.resolvedAt?.take(10) == java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date()) },
                    urgentActions = actions,
                    loading = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = "Could not refresh dashboard. Check your connection and try again.")
            }
        }
    }
}
