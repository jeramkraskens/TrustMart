// State and API actions for listing reviews.
// The screen observes this state; viewModelScope runs asynchronous work within the ViewModel lifetime.
package com.marketplace.moderator.ui.verification

/** Loads all verification pages and exposes immutable UI state; network errors leave previously loaded data available. */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketplace.moderator.data.RetrofitClient
import com.marketplace.moderator.data.allVerifications
import com.marketplace.moderator.data.models.VerificationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class VerificationUiState(
    val items: List<VerificationRequest> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class VerificationViewModel(application: Application) : AndroidViewModel(application) {

    private val api get() = RetrofitClient.getApiService(getApplication<Application>())

    private val _uiState = MutableStateFlow(VerificationUiState(loading = true))
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                _uiState.value = VerificationUiState(items = api.allVerifications(), loading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = "Network error: ${e.message}")
            }
        }
    }
}
