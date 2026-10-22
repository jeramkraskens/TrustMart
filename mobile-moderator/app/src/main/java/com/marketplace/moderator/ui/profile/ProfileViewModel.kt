// State and API actions for account details.
// The screen observes this state; viewModelScope runs asynchronous work within the ViewModel lifetime.
package com.marketplace.moderator.ui.profile

/** Computes statistics for the current moderator and calls existing profile/password endpoints without changing roles. */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketplace.moderator.data.RetrofitClient
import com.marketplace.moderator.data.allVerifications
import com.marketplace.moderator.data.allDisputes
import com.marketplace.moderator.data.TokenManager
import com.marketplace.moderator.data.passwordChangeFields
import kotlinx.coroutines.CancellationException
import com.marketplace.moderator.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val verifiedCount: Int = 0,
    val disputesResolvedCount: Int = 0,
    val loading: Boolean = true,
    val error: String? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val api get() = RetrofitClient.getApiService(getApplication<Application>())
    val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /** Save only editable identity fields; role and moderation scores are server-owned. */
    suspend fun saveProfile(first: String, last: String): String? = try {
        val response = api.updateProfile(mapOf("first_name" to first.trim(), "last_name" to last.trim()))
        if (response.isSuccessful) { load(); null } else "Profile could not be saved. Please try again."
    } catch (e: CancellationException) { throw e } catch (e: Exception) { "Connection failed. Please try again." }

    suspend fun changePassword(old: String, new: String): String? = try {
        require(new.length >= 8 && new.any { it in 'A'..'Z' } && new.any { it in 'a'..'z' } && new.any { it in '0'..'9' }) {
            "Use 8+ characters including uppercase, lowercase and a number."
        }
        val response = api.changePassword(passwordChangeFields(old, new))
        if (response.isSuccessful) null else "Check your current password and use a stronger new password."
    } catch (e: CancellationException) { throw e } catch (e: IllegalArgumentException) { e.message } catch (e: Exception) { "Connection failed. Please try again." }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            try {
                val meResponse = api.getMe()
                if (!meResponse.isSuccessful) throw retrofit2.HttpException(meResponse)
                val user = meResponse.body()

                var verifiedCount = 0
                var disputesResolvedCount = 0
                if (user != null) {
                    // Personal statistics must exclude work performed by other moderators.
                    verifiedCount = api.allVerifications().count { it.status == "approved" && it.moderator == user.id }
                    disputesResolvedCount = api.allDisputes().count { it.status == "resolved" && it.moderator == user.id }
                }

                _uiState.value = ProfileUiState(
                    user = user,
                    verifiedCount = verifiedCount,
                    disputesResolvedCount = disputesResolvedCount,
                    loading = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = "Could not load your profile. Please refresh.")
            }
        }
    }
}
