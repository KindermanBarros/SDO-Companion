package com.kinderman.sdo.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinderman.sdo.BuildConfig
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.repository.AuthRepository
import com.kinderman.sdo.presentation.login.GoogleIdTokenFetcher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val initializing: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val currentSession = MutableStateFlow<UserSession?>(null)
    val session = currentSession.asStateFlow()
    private val _state = MutableStateFlow(AuthUiState(initializing = repository.configured))
    val state = _state.asStateFlow()
    val configured: Boolean get() = repository.configured

    init {
        viewModelScope.launch {
            repository.session.collect { session ->
                currentSession.value = session
                _state.value = _state.value.copy(initializing = false, loading = false)
            }
        }
    }

    fun login(activity: Activity) = viewModelScope.launch {
        if (_state.value.loading) return@launch
        _state.value = AuthUiState(initializing = false, loading = true)
        _state.value = runCatching {
            val idToken = GoogleIdTokenFetcher.fetch(activity)
            repository.loginWithGoogleIdToken(idToken)
        }.fold(
            onSuccess = { session ->
                if (session != null) currentSession.value = session
                AuthUiState(initializing = false)
            },
            onFailure = {
                AuthUiState(
                    initializing = false,
                    error = it.localizedMessage ?: "Falha no login com Google",
                )
            },
        )
    }

    fun loginWithEmailPassword(email: String, password: String) = viewModelScope.launch {
        if (!BuildConfig.IS_DEV_ENV || _state.value.loading) return@launch
        _state.value = AuthUiState(initializing = false, loading = true)
        _state.value = runCatching {
            repository.loginWithEmailPassword(email, password)
        }.fold(
            onSuccess = { session ->
                if (session != null) currentSession.value = session
                AuthUiState(initializing = false)
            },
            onFailure = {
                AuthUiState(
                    initializing = false,
                    error = it.localizedMessage ?: "Falha no login com email e senha",
                )
            },
        )
    }

    fun logout() = repository.logout()
}
