package com.kinderman.sdo.presentation

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinderman.sdo.domain.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(val loading: Boolean = false, val error: String? = null)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    val session = repository.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    var state by mutableStateOf(AuthUiState())
        private set
    val configured: Boolean get() = repository.configured

    fun login(activity: Activity) = viewModelScope.launch {
        state = AuthUiState(loading = true)
        state = runCatching { repository.loginWithGoogle(activity) }.fold(
            onSuccess = { AuthUiState() },
            onFailure = { AuthUiState(error = it.localizedMessage ?: "Falha no login com Google") },
        )
    }

    fun logout() = repository.logout()
}
