package com.kinderman.sdo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kinderman.sdo.domain.repository.AlwaysOnlineConnectivityMonitor
import com.kinderman.sdo.domain.repository.AuthRepository
import com.kinderman.sdo.domain.repository.CampaignJournalRepository
import com.kinderman.sdo.domain.repository.CampaignRepository
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.repository.CatalogRepository
import com.kinderman.sdo.domain.repository.ConnectivityMonitor
import com.kinderman.sdo.domain.repository.OwnerRepository
import com.kinderman.sdo.domain.repository.OperationsRepository
import com.kinderman.sdo.presentation.journal.JournalViewModel

class AppViewModelFactory(
    private val repository: CharacterRepository,
    private val ownerRepository: OwnerRepository,
    private val catalogRepository: CatalogRepository,
    private val campaignRepository: CampaignRepository,
    private val operationsRepository: OperationsRepository,
    private val connectivityMonitor: ConnectivityMonitor = AlwaysOnlineConnectivityMonitor,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AppViewModel(
            repository,
            ownerRepository,
            catalogRepository,
            campaignRepository,
            operationsRepository,
            connectivityMonitor,
        ) as T
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repository) as T
}

class JournalViewModelFactory(private val repository: CampaignJournalRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = JournalViewModel(repository) as T
}
