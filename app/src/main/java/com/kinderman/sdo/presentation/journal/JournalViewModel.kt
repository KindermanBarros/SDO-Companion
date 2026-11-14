package com.kinderman.sdo.presentation.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignJournalDraft
import com.kinderman.sdo.domain.model.CampaignJournalEntry
import com.kinderman.sdo.domain.model.CampaignJournalPolicy
import com.kinderman.sdo.domain.model.JournalSaveResult
import com.kinderman.sdo.domain.model.JournalSyncState
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.repository.CampaignJournalRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class JournalMode { LIST, DETAIL, EDITOR, CONFLICT }

data class JournalUiState(
    val campaign: Campaign? = null,
    val session: UserSession? = null,
    val entries: List<CampaignJournalEntry> = emptyList(),
    val syncState: JournalSyncState = JournalSyncState.LOADING,
    val mode: JournalMode = JournalMode.LIST,
    val selectedEntryId: String? = null,
    val draft: CampaignJournalDraft? = null,
    val editingBaseline: CampaignJournalEntry? = null,
    val conflictServerEntry: CampaignJournalEntry? = null,
    val saving: Boolean = false,
    val uncertainSave: Boolean = false,
    val disconnectedDuringEdit: Boolean = false,
    val validationError: String? = null,
    val statusMessage: String? = null,
    val deleteConfirmEntry: CampaignJournalEntry? = null,
    val discardConfirmVisible: Boolean = false,
    val replaceConfirmVisible: Boolean = false,
    val isActiveMember: Boolean = false,
) {
    val selectedEntry: CampaignJournalEntry?
        get() = selectedEntryId?.let { id -> entries.firstOrNull { it.id == id } }

    val isOnline: Boolean
        get() = syncState == JournalSyncState.LIVE || syncState == JournalSyncState.ARCHIVED

    val canCreate: Boolean
        get() = CampaignJournalPolicy.canCreate(campaign, session, isActiveMember, isOnline)

    val canMutateSelected: Boolean
        get() = selectedEntry?.let { entry ->
            CampaignJournalPolicy.canMutateEntry(entry, campaign, session, isActiveMember, isOnline)
        } ?: false
}

class JournalViewModel(
    private val repository: CampaignJournalRepository,
    private val externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope
    private val _state = MutableStateFlow(JournalUiState())
    val state: StateFlow<JournalUiState> = _state.asStateFlow()

    private var observerJob: Job? = null
    private var boundUid: String? = null
    private var boundCampaignId: String? = null

    fun bind(session: UserSession?, campaign: Campaign?, isActiveMember: Boolean) {
        if (session == null || campaign == null) {
            clearContext()
            return
        }
        if (boundUid == session.uid && boundCampaignId == campaign.id) {
            _state.update { it.copy(session = session, campaign = campaign, isActiveMember = isActiveMember) }
            return
        }
        boundUid?.takeIf { it != session.uid }?.let(repository::clearAccountCache)
        observerJob?.cancel()
        boundUid = session.uid
        boundCampaignId = campaign.id
        _state.value = JournalUiState(
            campaign = campaign,
            session = session,
            syncState = JournalSyncState.LOADING,
            isActiveMember = isActiveMember,
        )
        startObserving(session, campaign)
    }

    fun retryObserve() {
        val current = _state.value
        val session = current.session ?: return
        val campaign = current.campaign ?: return
        observerJob?.cancel()
        _state.update { it.copy(syncState = JournalSyncState.LOADING, statusMessage = null) }
        startObserving(session, campaign)
    }

    private fun startObserving(session: UserSession, campaign: Campaign) {
        observerJob = scope.launch {
            repository.observe(session, campaign).collect { snapshot ->
                _state.update { current ->
                    if (snapshot.syncState == JournalSyncState.ACCESS_REVOKED) {
                        current.copy(
                            entries = emptyList(),
                            syncState = JournalSyncState.ACCESS_REVOKED,
                            mode = JournalMode.LIST,
                            selectedEntryId = null,
                            draft = null,
                            editingBaseline = null,
                            conflictServerEntry = null,
                            deleteConfirmEntry = null,
                            discardConfirmVisible = false,
                            replaceConfirmVisible = false,
                            statusMessage = "Você não tem mais acesso a este diário.",
                        )
                    } else {
                        val updatedSelected = current.selectedEntryId?.let { id ->
                            snapshot.entries.firstOrNull { it.id == id }
                        }
                        val nextMode = if (current.mode == JournalMode.DETAIL && updatedSelected == null) {
                            JournalMode.LIST
                        } else {
                            current.mode
                        }
                        current.copy(
                            entries = snapshot.entries,
                            syncState = snapshot.syncState,
                            mode = nextMode,
                            selectedEntryId = if (nextMode == JournalMode.DETAIL) current.selectedEntryId else null,
                            disconnectedDuringEdit = current.disconnectedDuringEdit &&
                                snapshot.syncState != JournalSyncState.LIVE,
                            statusMessage = snapshot.errorMessage ?: current.statusMessage,
                        )
                    }
                }
            }
        }
    }

    fun clearContext() {
        observerJob?.cancel()
        observerJob = null
        boundUid?.let(repository::clearAccountCache)
        boundUid = null
        boundCampaignId = null
        _state.value = JournalUiState()
    }

    fun openEntry(entryId: String) {
        _state.update { it.copy(mode = JournalMode.DETAIL, selectedEntryId = entryId, statusMessage = null) }
    }

    fun startNewEntry() {
        if (!_state.value.canCreate) return
        _state.update {
            it.copy(
                mode = JournalMode.EDITOR,
                editingBaseline = null,
                draft = CampaignJournalDraft(isNew = true),
                validationError = null,
                uncertainSave = false,
                disconnectedDuringEdit = false,
                statusMessage = null,
            )
        }
    }

    fun startEditEntry(entry: CampaignJournalEntry) {
        val current = _state.value
        if (!CampaignJournalPolicy.canMutateEntry(entry, current.campaign, current.session, current.isActiveMember, current.isOnline)) {
            return
        }
        _state.update {
            it.copy(
                mode = JournalMode.EDITOR,
                editingBaseline = entry,
                draft = CampaignJournalDraft(
                    entryId = entry.id,
                    title = entry.title,
                    body = entry.body,
                    isNew = false,
                ),
                validationError = null,
                uncertainSave = false,
                disconnectedDuringEdit = false,
                statusMessage = null,
            )
        }
    }

    fun updateDraft(title: String, body: String) {
        _state.update { current ->
            val active = current.draft ?: return@update current
            current.copy(
                draft = active.copy(
                    title = title.take(CampaignJournalPolicy.TITLE_MAX_LENGTH),
                    body = body.take(CampaignJournalPolicy.BODY_MAX_LENGTH),
                ),
                validationError = null,
            )
        }
    }

    fun saveDraft() {
        val current = _state.value
        val session = current.session ?: return
        val campaign = current.campaign ?: return
        val draft = current.draft ?: return
        val validation = CampaignJournalPolicy.validateDraft(draft.title, draft.body)
        if (validation != null) {
            _state.update { it.copy(validationError = validation) }
            return
        }
        scope.launch {
            _state.update { it.copy(saving = true, validationError = null, statusMessage = null) }
            if (current.uncertainSave) {
                val reconciled = repository.verifyOperation(session, campaign.id, draft.entryId, draft.operationId)
                if (reconciled != null) {
                    _state.update {
                        it.copy(
                            saving = false,
                            uncertainSave = false,
                            disconnectedDuringEdit = false,
                            mode = JournalMode.LIST,
                            draft = null,
                            editingBaseline = null,
                            selectedEntryId = null,
                            conflictServerEntry = null,
                            replaceConfirmVisible = false,
                        )
                    }
                    return@launch
                }
            }
            when (val result = repository.save(session, campaign, draft, current.editingBaseline?.revision)) {
                is JournalSaveResult.Confirmed -> _state.update {
                    it.copy(
                        saving = false,
                        uncertainSave = false,
                        disconnectedDuringEdit = false,
                        mode = JournalMode.LIST,
                        draft = null,
                        editingBaseline = null,
                        selectedEntryId = null,
                        conflictServerEntry = null,
                        replaceConfirmVisible = false,
                        statusMessage = null,
                    )
                }

                is JournalSaveResult.Conflict -> _state.update {
                    it.copy(
                        saving = false,
                        mode = JournalMode.CONFLICT,
                        conflictServerEntry = result.serverEntry,
                        replaceConfirmVisible = false,
                        statusMessage = "Este registro foi alterado por outra pessoa.",
                    )
                }

                is JournalSaveResult.Offline -> _state.update {
                    it.copy(
                        saving = false,
                        disconnectedDuringEdit = true,
                        statusMessage = result.message,
                    )
                }

                is JournalSaveResult.Uncertain -> _state.update {
                    it.copy(
                        saving = false,
                        uncertainSave = true,
                        statusMessage = result.message,
                    )
                }

                is JournalSaveResult.PermissionDenied -> _state.update {
                    it.copy(
                        saving = false,
                        syncState = JournalSyncState.ACCESS_REVOKED,
                        entries = emptyList(),
                        mode = JournalMode.LIST,
                        draft = null,
                        editingBaseline = null,
                        selectedEntryId = null,
                        statusMessage = result.message,
                    )
                }

                is JournalSaveResult.ValidationError -> _state.update {
                    it.copy(saving = false, validationError = result.message)
                }
            }
        }
    }

    fun reloadConflict() {
        _state.update { current ->
            val server = current.conflictServerEntry ?: return@update current
            current.copy(
                mode = JournalMode.EDITOR,
                editingBaseline = server,
                draft = CampaignJournalDraft(
                    entryId = server.id,
                    title = server.title,
                    body = server.body,
                    isNew = false,
                ),
                conflictServerEntry = null,
                replaceConfirmVisible = false,
                statusMessage = null,
            )
        }
    }

    fun requestReplaceConflict() {
        _state.update { it.copy(replaceConfirmVisible = true) }
    }

    fun dismissReplaceConflict() {
        _state.update { it.copy(replaceConfirmVisible = false) }
    }

    fun confirmReplaceConflict() {
        _state.update { current ->
            val server = current.conflictServerEntry ?: return@update current
            current.copy(
                replaceConfirmVisible = false,
                editingBaseline = server,
                draft = current.draft?.copy(operationId = UUID.randomUUID().toString()),
            )
        }
        saveDraft()
    }

    fun requestDelete(entry: CampaignJournalEntry) {
        _state.update { it.copy(deleteConfirmEntry = entry) }
    }

    fun dismissDelete() {
        _state.update { it.copy(deleteConfirmEntry = null) }
    }

    fun confirmDelete() {
        val current = _state.value
        val session = current.session ?: return
        val campaign = current.campaign ?: return
        val entry = current.deleteConfirmEntry ?: return
        scope.launch {
            _state.update { it.copy(saving = true, deleteConfirmEntry = null) }
            when (val result = repository.delete(session, campaign, entry, UUID.randomUUID().toString())) {
                is JournalSaveResult.Confirmed -> _state.update {
                    it.copy(saving = false, mode = JournalMode.LIST, selectedEntryId = null, statusMessage = null)
                }

                is JournalSaveResult.Offline -> _state.update {
                    it.copy(saving = false, statusMessage = result.message)
                }

                is JournalSaveResult.PermissionDenied -> _state.update {
                    it.copy(
                        saving = false,
                        syncState = JournalSyncState.ACCESS_REVOKED,
                        entries = emptyList(),
                        mode = JournalMode.LIST,
                        selectedEntryId = null,
                        statusMessage = result.message,
                    )
                }

                is JournalSaveResult.Uncertain -> _state.update {
                    it.copy(saving = false, statusMessage = result.message)
                }

                is JournalSaveResult.Conflict, is JournalSaveResult.ValidationError -> _state.update {
                    it.copy(saving = false)
                }
            }
        }
    }

    fun handleBack(onExitToCampaign: () -> Unit) {
        val current = _state.value
        when (current.mode) {
            JournalMode.LIST -> onExitToCampaign()
            JournalMode.DETAIL -> _state.update { it.copy(mode = JournalMode.LIST, selectedEntryId = null) }
            JournalMode.CONFLICT -> _state.update { it.copy(mode = JournalMode.EDITOR, replaceConfirmVisible = false) }
            JournalMode.EDITOR -> {
                if (hasUnsavedChanges(current)) {
                    _state.update { it.copy(discardConfirmVisible = true) }
                } else {
                    _state.update {
                        it.copy(
                            mode = if (it.editingBaseline != null) JournalMode.DETAIL else JournalMode.LIST,
                            draft = null,
                            validationError = null,
                        )
                    }
                }
            }
        }
    }

    fun dismissDiscard() {
        _state.update { it.copy(discardConfirmVisible = false) }
    }

    fun confirmDiscard() {
        _state.update {
            it.copy(
                discardConfirmVisible = false,
                mode = if (it.editingBaseline != null) JournalMode.DETAIL else JournalMode.LIST,
                draft = null,
                editingBaseline = null,
                conflictServerEntry = null,
                validationError = null,
                uncertainSave = false,
                disconnectedDuringEdit = false,
                statusMessage = null,
            )
        }
    }

    private fun hasUnsavedChanges(current: JournalUiState): Boolean {
        val draft = current.draft ?: return false
        val base = current.editingBaseline
        return if (base == null) {
            draft.title.isNotBlank() || draft.body.isNotBlank()
        } else {
            draft.title != base.title || draft.body != base.body
        }
    }
}
