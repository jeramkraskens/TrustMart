// State and API actions for listing reviews.
// The screen observes this state; viewModelScope runs asynchronous work within the ViewModel lifetime.
package com.marketplace.moderator.ui.verification

/** Fetches a single verification request and submits review decisions. Navigation occurs only after server success. */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketplace.moderator.data.RetrofitClient
import com.marketplace.moderator.data.models.ModerationActionRequest
import com.marketplace.moderator.data.models.VerificationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class VerificationDetailUiState(
    val item: VerificationRequest? = null,
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
)

class VerificationDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val api get() = RetrofitClient.getApiService(getApplication<Application>())

    private val _uiState = MutableStateFlow(VerificationDetailUiState())
    val uiState: StateFlow<VerificationDetailUiState> = _uiState.asStateFlow()

    fun load(id: Int) {
        if (_uiState.value.submitting) return
        viewModelScope.launch {
            _uiState.value = VerificationDetailUiState(loading = true)
            try {
                val response = api.getVerification(id)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(item = response.body(), loading = false)
                } else {
                    _uiState.value = _uiState.value.copy(loading = false, error = "Could not load this listing.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = "Network error: ${e.message}")
            }
        }
    }

    fun approve(id: Int, feedback: String) {
        if (_uiState.value.submitting) return
        _uiState.value = _uiState.value.copy(submitting = true, error = null)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true)
            try {
                val response = api.approveVerification(id, ModerationActionRequest(feedback))
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(submitting = false, done = true)
                } else {
                    _uiState.value = _uiState.value.copy(submitting = false, error = "Approval failed. Try again.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(submitting = false, error = "Network error: ${e.message}")
            }
        }
    }

    fun reject(id: Int, feedback: String) {
        if (_uiState.value.submitting) return
        _uiState.value = _uiState.value.copy(submitting = true, error = null)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true)
            try {
                val response = api.rejectVerification(id, ModerationActionRequest(feedback))
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(submitting = false, done = true)
                } else {
                    _uiState.value = _uiState.value.copy(submitting = false, error = "Rejection failed. Try again.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(submitting = false, error = "Network error: ${e.message}")
            }
        }
    }
}
