package com.kinderman.sdo.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinderman.sdo.domain.model.CampaignLibraryEntry
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.SessionOperation
import com.kinderman.sdo.domain.repository.CatalogRepository
import com.kinderman.sdo.domain.repository.OperationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class LibraryUiState(
    val catalog: List<CatalogEntry> = emptyList(),
    val campaignLibrary: List<CampaignLibraryEntry> = emptyList(),
    val audit: List<SessionOperation> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    catalogRepository: CatalogRepository,
    operationsRepository: OperationsRepository,
) : ViewModel() {
    private val accessibleCampaignIds = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<LibraryUiState> = combine(
        catalogRepository.observe(),
        accessibleCampaignIds.flatMapLatest(operationsRepository::observeLibrary),
        accessibleCampaignIds.flatMapLatest(operationsRepository::observeAudit),
    ) { catalog, campaignLibrary, audit ->
        LibraryUiState(
            catalog = catalog,
            campaignLibrary = campaignLibrary,
            audit = audit,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun setAccessibleCampaignIds(ids: Set<String>) {
        accessibleCampaignIds.value = ids
    }
}
