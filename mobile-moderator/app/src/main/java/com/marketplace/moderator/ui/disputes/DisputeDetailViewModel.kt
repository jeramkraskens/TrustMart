// State and API actions for order disputes.
// The screen observes this state; viewModelScope runs asynchronous work within the ViewModel lifetime.
package com.marketplace.moderator.ui.disputes

/** Retrieves case data and submits a resolution. Failed requests remain on screen for correction or retry. */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketplace.moderator.data.RetrofitClient
import com.marketplace.moderator.data.models.Dispute
import com.marketplace.moderator.data.models.ResolveDisputeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class DisputeDetailUiState(
    val item: Dispute? = null,
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
)

class DisputeDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val api get() = RetrofitClient.getApiService(getApplication<Application>())

    private val _uiState = MutableStateFlow(DisputeDetailUiState())
    val uiState: StateFlow<DisputeDetailUiState> = _uiState.asStateFlow()

    fun load(id: Int) {
        if (_uiState.value.submitting) return
        viewModelScope.launch {
            _uiState.value = DisputeDetailUiState(loading = true)
            try {
                val response = api.getDispute(id)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(item = response.body(), loading = false)
                } else {
                    _uiState.value = _uiState.value.copy(loading = false, error = "Could not load this dispute.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = "Network error: ${e.message}")
            }
        }
    }

    fun resolve(id: Int, resolution: String, notes: String) {
        if (_uiState.value.submitting) return
        _uiState.value = _uiState.value.copy(submitting = true, error = null)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true)
            try {
                val response = api.resolveDispute(id, ResolveDisputeRequest(resolution, notes))
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(submitting = false, done = true)
                } else {
                    _uiState.value = _uiState.value.copy(submitting = false, error = "Action failed. Try again.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(submitting = false, error = "Network error: ${e.message}")
            }
        }
    }
}
