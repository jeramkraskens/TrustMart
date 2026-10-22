// State and API actions for order disputes.
// The screen observes this state; viewModelScope runs asynchronous work within the ViewModel lifetime.
package com.marketplace.moderator.ui.disputes

/** Loads disputes and exposes loading/error state separately from the records shown by the screen. */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketplace.moderator.data.RetrofitClient
import com.marketplace.moderator.data.allDisputes
import com.marketplace.moderator.data.models.Dispute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class DisputeUiState(
    val items: List<Dispute> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class DisputeViewModel(application: Application) : AndroidViewModel(application) {

    private val api get() = RetrofitClient.getApiService(getApplication<Application>())

    private val _uiState = MutableStateFlow(DisputeUiState(loading = true))
    val uiState: StateFlow<DisputeUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                _uiState.value = DisputeUiState(items = api.allDisputes(), loading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = "Network error: ${e.message}")
            }
        }
    }
}
