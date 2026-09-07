package com.kinderman.sdo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kinderman.sdo.domain.repository.AuthRepository
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.repository.CatalogRepository
import com.kinderman.sdo.domain.repository.OwnerRepository

class AppViewModelFactory(
    private val repository: CharacterRepository,
    private val ownerRepository: OwnerRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AppViewModel(repository, ownerRepository, catalogRepository) as T
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repository) as T
}
