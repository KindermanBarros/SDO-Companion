package com.kinderman.sdo.presentation

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class AuthUiState(
    val initializing: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val currentSession = MutableStateFlow<UserSession?>(null)
    val session = currentSession.asStateFlow()
    var state by mutableStateOf(AuthUiState(initializing = repository.configured))
        private set
    val configured: Boolean get() = repository.configured

    init {
        viewModelScope.launch {
            repository.session.collect { session ->
                currentSession.value = session
                state = state.copy(initializing = false, loading = false)
            }
        }
    }

    fun login(activity: Activity) = viewModelScope.launch {
        if (state.loading) return@launch
        state = AuthUiState(initializing = false, loading = true)
        state = runCatching { repository.loginWithGoogle(activity) }.fold(
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

    fun logout() = repository.logout()
}
