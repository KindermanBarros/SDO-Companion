package com.kinderman.sdo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kinderman.sdo.domain.repository.AuthRepository
import com.kinderman.sdo.domain.repository.CharacterRepository

class AppViewModelFactory(private val repository: CharacterRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repository) as T
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repository) as T
}
