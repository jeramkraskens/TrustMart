// State and API actions for moderator sign-in.
// The screen observes this state; viewModelScope runs asynchronous work within the ViewModel lifetime.
package com.marketplace.moderator.ui.login

/** Validates credentials, accepts moderator accounts only, and persists tokens after a successful login. */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marketplace.moderator.data.RetrofitClient
import com.marketplace.moderator.data.TokenManager
import com.marketplace.moderator.data.models.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.marketplace.moderator.BuildConfig
import com.marketplace.moderator.data.normalizedServer

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val api get() = RetrofitClient.getApiService(getApplication<Application>())

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    val server: String get() = tokenManager.server

    fun login(username: String, password: String, server: String = tokenManager.server) {
        if (_state.value is LoginState.Loading) return
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("Please enter both username and password.")
            return
        }
        _state.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val normalized = normalizedServer(server, BuildConfig.DEBUG)
                if (normalized != tokenManager.server) {
                    tokenManager.clear()
                    tokenManager.server = normalized
                }
                val response = api.login(LoginRequest(username.trim(), password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.user.role != "moderator") {
                        tokenManager.clear()
                        _state.value = LoginState.Error("This app is for moderator accounts only.")
                        return@launch
                    }
                    tokenManager.saveTokens(body.access, body.refresh)
                    _state.value = LoginState.Success
                } else {
                    _state.value = LoginState.Error("Invalid username or password.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = LoginState.Error("Could not connect to server: ${e.message}")
            }
        }
    }
}
