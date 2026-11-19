package com.kinderman.sdo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignAlertSettings
import com.kinderman.sdo.domain.model.CampaignDelivery
import com.kinderman.sdo.domain.model.CampaignLibraryEntry
import com.kinderman.sdo.domain.model.CampaignInvitePreview
import com.kinderman.sdo.domain.model.CampaignMember
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SheetConflictItem
import com.kinderman.sdo.domain.model.SheetIntent
import com.kinderman.sdo.domain.model.SheetSyncStatus
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.InventoryState
import com.kinderman.sdo.domain.model.validInventoryStates
import com.kinderman.sdo.domain.model.withItemInventoryState
import com.kinderman.sdo.domain.repository.AlwaysOnlineConnectivityMonitor
import com.kinderman.sdo.domain.repository.CampaignRepository
import com.kinderman.sdo.domain.repository.CatalogRepository
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.repository.ConnectivityMonitor
import com.kinderman.sdo.domain.repository.OwnerRepository
import com.kinderman.sdo.domain.repository.OperationsRepository
import com.kinderman.sdo.domain.repository.RemoteSyncEvent
import com.kinderman.sdo.domain.usecase.DeleteCharacter
import com.kinderman.sdo.domain.usecase.SaveCharacter
import com.kinderman.sdo.domain.usecase.SetHistorianLock
import com.kinderman.sdo.domain.usecase.SetPlayerLock
import com.kinderman.sdo.domain.usecase.TransferCharacterOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CharacterLoadState(
    val initialLoading: Boolean = false,
    val syncing: Boolean = false,
    val cloudSyncing: Boolean = false,
)

internal enum class RemoteSyncTrigger { MANUAL, EDITING_IDLE, APP_FOREGROUND, APP_BACKGROUND, SHEET_CLOSED }

internal fun shouldRunRemoteSync(automaticSync: Boolean, trigger: RemoteSyncTrigger): Boolean =
    trigger == RemoteSyncTrigger.MANUAL || automaticSync

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class AppViewModel(
    private val repository: CharacterRepository,
    private val ownerRepository: OwnerRepository,
    catalogRepository: CatalogRepository,
    private val campaignRepository: CampaignRepository,
    private val operationsRepository: OperationsRepository,
    private val connectivityMonitor: ConnectivityMonitor = AlwaysOnlineConnectivityMonitor,
    private val externalScope: CoroutineScope? = null,
    private val cloudSyncPulseMs: Long = 800L,
) : ViewModel() {
    @Inject
    constructor(
        repository: CharacterRepository,
        ownerRepository: OwnerRepository,
        catalogRepository: CatalogRepository,
        campaignRepository: CampaignRepository,
        operationsRepository: OperationsRepository,
        connectivityMonitor: ConnectivityMonitor,
    ) : this(
        repository = repository,
        ownerRepository = ownerRepository,
        catalogRepository = catalogRepository,
        campaignRepository = campaignRepository,
        operationsRepository = operationsRepository,
        connectivityMonitor = connectivityMonitor,
        externalScope = null,
        cloudSyncPulseMs = 800L,
    )
    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope
    private val saveCharacter = SaveCharacter(repository)
    private val deleteCharacter = DeleteCharacter(repository)
    private val setPlayerLock = SetPlayerLock(repository)
    private val setHistorianLock = SetHistorianLock(repository)
    private val transferCharacterOwner = TransferCharacterOwner(repository)
    private val currentSession = MutableStateFlow<UserSession?>(null)
    private val _message = MutableStateFlow<String?>(null)
    private val _loadState = MutableStateFlow(CharacterLoadState())
    private val _conflicts = MutableStateFlow<List<CharacterSyncConflict>>(emptyList())
    private val _invitePreview = MutableStateFlow<CampaignInvitePreview?>(null)
    private var syncJob: Job? = null
    private var syncRequested = false
    private var automaticSync = true
    private val appVisibility = AppVisibility()
    private var lastSyncFailure: String? = null
    private val localSaveMutex = Mutex()
    private val autosaveJobs = mutableMapOf<String, Job>()
    private val pendingAutosaves = mutableMapOf<String, Character>()
    private var remoteSyncDebounceJob: Job? = null
    private var cloudSyncPulseJob: Job? = null
    private var isSheetSurfaceActive = false
    private var activePresenceCharacterId: String? = null

    val session = currentSession.asStateFlow()
    val isOnline = connectivityMonitor.isOnline
    val isOfflineReadOnly = combine(currentSession, connectivityMonitor.isOnline) { session, online ->
        session != null && session.uid != DEMO_USER_ID && !online
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)
    private val _syncFailure = MutableStateFlow<String?>(null)
    val syncFailure = _syncFailure.asStateFlow()
    val sheetSyncStatus = currentSession.flatMapLatest { session ->
        session?.let(repository::observeSyncStatus) ?: flowOf(SheetSyncStatus())
    }.catch { emit(SheetSyncStatus()) }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), SheetSyncStatus())
    val sheetIntents = currentSession.flatMapLatest { session ->
        session?.let(repository::observeIntents) ?: flowOf(emptyList())
    }.catch { emit(emptyList()) }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sheetConflicts = currentSession.flatMapLatest { session ->
        session?.let(repository::observeConflicts) ?: flowOf(emptyList())
    }.catch { emit(emptyList()) }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val offlineEditableIds = currentSession.flatMapLatest { session ->
        session?.let(repository::observeOfflineEditableIds) ?: flowOf(emptySet())
    }.catch { emit(emptySet()) }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun observeFieldPresence(characterId: String) =
        if (characterId.isBlank()) {
            flowOf(emptyMap())
        } else {
            currentSession.flatMapLatest { session ->
                session?.let { repository.observeFieldPresence(it, characterId) } ?: flowOf(emptyMap())
            }.catch { emit(emptyMap()) }
        }

    fun setSheetPresenceActive(active: Boolean, characterId: String? = null) {
        val nextId = if (active) characterId?.takeIf(String::isNotBlank) else null
        val previousId = activePresenceCharacterId
        if (previousId != null && (!active || previousId != nextId)) {
            clearFieldPresenceInternal(previousId)
        }
        isSheetSurfaceActive = active
        activePresenceCharacterId = nextId
    }

    fun setFieldPresence(characterId: String, fieldKey: String?, active: Boolean) {
        if (characterId.isBlank()) return
        if (active && (!isSheetSurfaceActive || (activePresenceCharacterId != null && activePresenceCharacterId != characterId))) {
            return
        }
        val session = currentSession.value ?: return
        if (active) {
            activePresenceCharacterId = characterId
        } else if (activePresenceCharacterId == characterId && fieldKey == null) {
            activePresenceCharacterId = null
        }
        scope.launch {
            try {
                repository.setFieldPresence(session, characterId, fieldKey, active)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Presence is advisory; a disconnected editor still retains its durable edits.
            }
        }
    }

    private fun clearFieldPresenceInternal(characterId: String) {
        val session = currentSession.value ?: return
        if (characterId.isBlank()) return
        scope.launch {
            try {
                repository.setFieldPresence(session, characterId, null, false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Presence is advisory; a disconnected editor still retains its durable edits.
            }
        }
    }
    private val _saveErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val saveErrors = _saveErrors.asStateFlow()
    val message = _message.asStateFlow()
    val loadState = _loadState.asStateFlow()
    val conflicts = _conflicts.asStateFlow()
    val invitePreview = _invitePreview.asStateFlow()
    val catalog = catalogRepository.observe()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val campaigns = currentSession.flatMapLatest { session ->
        session?.let(campaignRepository::observe) ?: flowOf(emptyList())
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val characters = currentSession.flatMapLatest { session ->
        session?.let(repository::observe) ?: flowOf(emptyList())
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val memberships = currentSession.flatMapLatest { session ->
        session?.let(campaignRepository::observeMemberships) ?: flowOf(emptyList())
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList<CampaignMember>())
    val campaignInvites = campaignRepository.observeAllInvites()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val owners = currentSession.flatMapLatest { session ->
        if (session?.isAdmin == true) ownerRepository.observe() else flowOf(emptyList())
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val campaignMembers = campaigns.flatMapLatest { values ->
        if (values.isEmpty()) flowOf(emptyList())
        else combine(values.map { campaignRepository.observeMembers(it.id) }) { groups -> groups.flatMap { it } }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList<CampaignMember>())
    private val accessibleCampaignIds = campaigns.map { values ->
        values.filterNot(Campaign::isDeleted).mapTo(linkedSetOf(), Campaign::id)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptySet())
    private val manageableCampaignIds = combine(currentSession, campaigns) { session, values ->
        if (session == null) emptySet() else values.filter { campaign ->
            session.isAdmin || campaign.ownerId == session.uid
        }.mapTo(linkedSetOf(), Campaign::id)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val audit = accessibleCampaignIds.flatMapLatest(operationsRepository::observeAudit)
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val library = accessibleCampaignIds.flatMapLatest(operationsRepository::observeLibrary)
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val deliveries = combine(currentSession, manageableCampaignIds) { session, ids -> session to ids }
        .flatMapLatest { (session, ids) -> session?.let { operationsRepository.observeDeliveries(it, ids) } ?: flowOf(emptyList()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val alertSettings = manageableCampaignIds.flatMapLatest(operationsRepository::observeAlertSettings)
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        var wasOnline = connectivityMonitor.isOnline.value
        connectivityMonitor.isOnline
            .onEach { online ->
                if (!wasOnline && online && currentSession.value != null && currentSession.value?.uid != DEMO_USER_ID) {
                    _syncFailure.value = null
                    lastSyncFailure = null
                    startSync(initial = false)
                } else if (!online) {
                    remoteSyncDebounceJob?.cancel()
                    remoteSyncDebounceJob = null
                }
                wasOnline = online
            }
            .launchIn(scope)

        combine(currentSession, accessibleCampaignIds, connectivityMonitor.isOnline) { session, ids, online ->
            Triple(session, ids, online)
        }
            .flatMapLatest { (session, ids, online) ->
                if (session == null || session.uid == DEMO_USER_ID || !online) emptyFlow()
                else repository.observeRemoteChanges(session, ids).catch { /* Ignore listener errors */ }
            }
            .onEach { event -> handleRemoteSyncEvent(event) }
            .launchIn(scope)
    }

    fun setSession(session: UserSession) {
        if (currentSession.value == session) return
        autosaveJobs.values.forEach(Job::cancel)
        autosaveJobs.clear()
        pendingAutosaves.clear()
        remoteSyncDebounceJob?.cancel()
        remoteSyncDebounceJob = null
        cloudSyncPulseJob?.cancel()
        cloudSyncPulseJob = null
        syncJob?.cancel()
        syncJob = null
        syncRequested = false
        isSheetSurfaceActive = false
        activePresenceCharacterId = null
        _conflicts.value = emptyList()
        _invitePreview.value = null
        lastSyncFailure = null
        _syncFailure.value = null
        _saveErrors.value = emptyMap()
        currentSession.value = session
        if (shouldRunRemoteSync(automaticSync, RemoteSyncTrigger.APP_FOREGROUND)) startSync(initial = true)
    }

    fun setDemoSession() = setSession(UserSession(DEMO_USER_ID, "", "Modo local", UserRole.USER))

    fun setAutomaticSync(enabled: Boolean) {
        val shouldSyncPendingChanges = enabled && !automaticSync
        automaticSync = enabled
        if (shouldSyncPendingChanges) scope.launch {
            flushAllPendingAutosaves()
            startSync(initial = false)
        }
    }

    fun clearSession() {
        autosaveJobs.values.forEach(Job::cancel)
        autosaveJobs.clear()
        pendingAutosaves.clear()
        remoteSyncDebounceJob?.cancel()
        remoteSyncDebounceJob = null
        cloudSyncPulseJob?.cancel()
        cloudSyncPulseJob = null
        syncJob?.cancel()
        syncJob = null
        syncRequested = false
        isSheetSurfaceActive = false
        activePresenceCharacterId = null
        currentSession.value = null
        _message.value = null
        _conflicts.value = emptyList()
        _invitePreview.value = null
        lastSyncFailure = null
        _syncFailure.value = null
        _saveErrors.value = emptyMap()
        _loadState.value = CharacterLoadState()
    }

    fun add() = runCharacterAction { session -> repository.create(session).id }

    fun addToCampaign(campaign: Campaign, ownerId: String) = runCharacterAction("Ficha criada na campanha") { session ->
        repository.create(session, ownerId = ownerId, campaignId = campaign.id).id
    }

    fun save(character: Character) {
        if (!ensureSheetMutation(character)) return
        autosaveJobs.remove(character.id)?.cancel()
        pendingAutosaves.remove(character.id)
        saveLocally(character, notify = true)
    }

    fun autosave(character: Character) {
        if (!ensureSheetMutation(character)) return
        // Persist every edit immediately; debounce network delivery in the durable sync layer.
        saveLocally(character, notify = false)
    }
    fun changeItemState(character: Character, itemId: String, state: InventoryState) {
        if (!ensureSheetMutation(character)) return
        val item = character.inventory.firstOrNull { it.id == itemId } ?: return
        if (state !in character.validInventoryStates(item)) return
        save(character.withItemInventoryState(itemId, state))
    }

    fun applySessionCommand(character: Character, command: SessionCommand) {
        if (!ensureSheetMutation(character)) return
        val session = currentSession.value ?: return
        scope.launch {
            runCatching { operationsRepository.apply(session, character, command) }
                .onSuccess { if (it && automaticSync) startSync(initial = false) }
                .onFailure { _message.value = userMessage(it, "Falha ao aplicar ação") }
        }
    }

    fun saveLibrary(entry: CampaignLibraryEntry) = runAction("Biblioteca atualizada") { operationsRepository.saveLibrary(it, entry) }
    fun duplicateLibrary(entry: CampaignLibraryEntry) = runAction("Modelo duplicado") { operationsRepository.duplicateLibrary(it, entry) }
    fun archiveLibrary(entry: CampaignLibraryEntry, archived: Boolean) = runAction(if (archived) "Modelo arquivado" else "Modelo restaurado") { operationsRepository.archiveLibrary(it, entry, archived) }
    fun deliverLibrary(entry: CampaignLibraryEntry, recipients: List<Character>, mappings: Map<String, String>) = runAction("Entrega enviada") { operationsRepository.deliver(it, entry, recipients, mappings) }
    fun respondDelivery(delivery: CampaignDelivery, accept: Boolean) = runAction(if (accept) "Conteúdo aceito" else "Conteúdo recusado") { operationsRepository.respondToDelivery(it, delivery, accept) }
    fun saveAlertSettings(settings: CampaignAlertSettings) {
        if (!ensureOnlineForMutation()) return
        scope.launch { operationsRepository.saveAlertSettings(settings) }
    }

    fun createCampaign(name: String, description: String) = runAction("Campanha criada") { session ->
        campaignRepository.create(session, name, description)
    }

    fun archiveCampaign(campaign: Campaign, archived: Boolean) = runAction(
        if (archived) "Campanha arquivada" else "Campanha restaurada",
    ) { session -> campaignRepository.archive(session, campaign, archived) }

    fun deleteCampaign(campaign: Campaign) = runAction("Campanha excluída; fichas preservadas") { session ->
        campaignRepository.delete(session, campaign)
    }

    fun leaveCampaign(campaign: Campaign) = runAction("Você saiu da campanha; suas fichas foram preservadas") { session ->
        campaignRepository.leave(session, campaign)
    }

    fun removeCampaignMember(campaign: Campaign, userId: String) = runAction("Participante removido; fichas preservadas") { session ->
        campaignRepository.removeMember(session, campaign, userId)
    }

    fun createCampaignInvite(campaign: Campaign) {
        if (!ensureOnlineForMutation()) return
        val session = currentSession.value ?: return
        scope.launch {
            runCatching { campaignRepository.createInvite(session, campaign) }
                .onSuccess { invite ->
                    _message.value = "Código de convite: ${invite.code}"
                    if (automaticSync) startSync(initial = false)
                }
                .onFailure { _message.value = userMessage(it, "Falha ao criar convite") }
        }
    }

    fun previewCampaignInvite(code: String) {
        if (!ensureOnlineForMutation()) return
        val session = currentSession.value ?: return
        scope.launch {
            runCatching { campaignRepository.previewInvite(session, code) }
                .onSuccess { preview ->
                    _invitePreview.value = preview
                    if (preview == null) _message.value = "Convite inválido, revogado ou expirado"
                }
                .onFailure { _message.value = userMessage(it, "Falha ao consultar convite") }
        }
    }

    fun acceptCampaignInvite(code: String, character: Character?, createNewCharacter: Boolean) {
        if (!ensureOnlineForMutation()) return
        val session = currentSession.value ?: return
        scope.launch {
            runCatching {
                val campaign = campaignRepository.joinByCode(session, code)
                val selectedCharacter = when {
                    createNewCharacter -> repository.create(session)
                    character != null -> character
                    else -> null
                }
                if (selectedCharacter != null) {
                    campaignRepository.linkCharacter(session, selectedCharacter, campaign)
                }
            }.onSuccess {
                _invitePreview.value = null
                _message.value = "Entrada na campanha concluída"
                if (automaticSync) startSync(initial = false)
            }.onFailure { error ->
                _message.value = userMessage(error, "Falha ao entrar na campanha")
            }
        }
    }

    fun dismissInvitePreview() { _invitePreview.value = null }

    fun linkCharacter(character: Character, campaign: Campaign) = runAction("Ficha vinculada à campanha") { session ->
        campaignRepository.linkCharacter(session, character, campaign)
    }

    fun unlinkCharacter(character: Character) = runAction("Ficha removida da campanha") { session ->
        campaignRepository.unlinkCharacter(session, character)
    }

    fun setPlayerLocked(character: Character, locked: Boolean) = runCharacterAction(
        if (locked) "Bloqueio pessoal ativado" else "Bloqueio pessoal removido",
    ) { session ->
        setPlayerLock(session, character, locked)
        character.id
    }

    fun setHistorianLocked(character: Character, locked: Boolean) = runCharacterAction(
        if (locked) "Bloqueio do historiador ativado" else "Bloqueio do historiador removido",
    ) { session ->
        setHistorianLock(session, character, locked)
        character.id
    }

    fun transferOwner(character: Character, owner: UserProfile) = runCharacterAction(
        "Owner transferido para ${owner.firstName}",
    ) { session ->
        transferCharacterOwner(session, character, owner)
        character.id
    }

    fun delete(character: Character) {
        autosaveJobs.remove(character.id)?.cancel()
        pendingAutosaves.remove(character.id)
        remoteSyncDebounceJob?.cancel()
        remoteSyncDebounceJob = null
        _conflicts.value = _conflicts.value.filterNot { it.local.id == character.id }
        _saveErrors.value = _saveErrors.value - character.id
        runCharacterAction("Personagem removido") { session ->
            deleteCharacter(session, character)
            character.id
        }
    }

    fun dismissMessage() { _message.value = null }
    fun sync() {
        scope.launch {
            flushAllPendingAutosaves()
            startSync(initial = false)
        }
    }

    fun onAppForegrounded() {
        if (!appVisibility.started()) return
        if (shouldRunRemoteSync(automaticSync, RemoteSyncTrigger.APP_FOREGROUND)) startSync(initial = false)
    }

    fun onAppBackgrounded(changingConfiguration: Boolean = false) {
        if (!appVisibility.stopped(changingConfiguration)) return
        activePresenceCharacterId?.let { characterId ->
            clearFieldPresenceInternal(characterId)
            activePresenceCharacterId = null
        }
        scope.launch {
            flushAllPendingAutosaves()
            if (shouldRunRemoteSync(automaticSync, RemoteSyncTrigger.APP_BACKGROUND)) startSync(initial = false)
        }
    }

    fun closeCharacter(character: Character) {
        if (isMutationBlockedOffline(currentSession.value) && character.id !in offlineEditableIds.value) {
            autosaveJobs.remove(character.id)?.cancel()
            pendingAutosaves.remove(character.id)
            return
        }
        pendingAutosaves[character.id] = character
        autosaveJobs.remove(character.id)?.cancel()
        scope.launch {
            flushPendingAutosave(character.id, scheduleRemote = false)
            if (shouldRunRemoteSync(automaticSync, RemoteSyncTrigger.SHEET_CLOSED)) startCharacterSync(character.id)
        }
    }

    private fun saveLocally(character: Character, notify: Boolean) {
        val session = currentSession.value ?: return
        scope.launch {
            persistLocally(character, notify, session)
        }
    }

    private suspend fun persistLocally(
        character: Character,
        notify: Boolean,
        capturedSession: UserSession? = currentSession.value,
    ) {
        val session = capturedSession ?: return
        try {
            localSaveMutex.withLock { saveCharacter(session, character) }
            _saveErrors.value = _saveErrors.value - character.id
            if (automaticSync) scheduleRemoteSync(character.id)
        } catch (cancelled: CancellationException) {
            // Superseding an autosave is normal while the player is editing.
            throw cancelled
        } catch (error: Throwable) {
            val message = userMessage(error, "Falha ao salvar ficha localmente")
            _saveErrors.value = _saveErrors.value + (character.id to message)
            _message.value = message
        }
    }

    private suspend fun flushPendingAutosave(characterId: String, scheduleRemote: Boolean = true) {
        // This function is normally called by the autosave job itself. Removing its
        // reference is enough; cancelling it here interrupts the save and surfaces a
        // misleading "was canceled" snackbar to the user.
        autosaveJobs.remove(characterId)
        val character = pendingAutosaves.remove(characterId) ?: return
        persistLocally(character, notify = false)
        if (!scheduleRemote) remoteSyncDebounceJob?.cancel()
    }

    private suspend fun flushAllPendingAutosaves() {
        val pending = pendingAutosaves.values.toList()
        autosaveJobs.values.forEach(Job::cancel)
        autosaveJobs.clear()
        pendingAutosaves.clear()
        pending.forEach { persistLocally(it, notify = false) }
        remoteSyncDebounceJob?.cancel()
        remoteSyncDebounceJob = null
    }

    private fun scheduleRemoteSync(characterId: String) {
        if (!shouldRunRemoteSync(automaticSync, RemoteSyncTrigger.EDITING_IDLE)) return
        remoteSyncDebounceJob?.cancel()
        remoteSyncDebounceJob = scope.launch {
            delay(350)
            startCharacterSync(characterId)
        }
    }

    fun resolveIntentConflict(
        conflictId: String,
        useRemote: Boolean,
        customValueJson: String? = null,
    ) {
        val session = currentSession.value ?: return
        scope.launch {
            runCatching {
                repository.resolveIntentConflict(
                    session = session,
                    conflictId = conflictId,
                    useRemote = useRemote,
                    customValueJson = customValueJson,
                )
            }.onSuccess {
                _message.value = "Conflito resolvido"
                if (automaticSync) startSync(initial = false)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _message.value = userMessage(error, "Falha ao resolver conflito")
            }
        }
    }

    fun retryAccessDeniedDraft(intentId: String) {
        val session = currentSession.value ?: return
        scope.launch {
            runCatching { repository.retryAccessDeniedDraft(session, intentId) }
                .onSuccess {
                    _message.value = "Reenvio do rascunho agendado"
                    if (automaticSync) startSync(initial = false)
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _message.value = userMessage(error, "Falha ao reenviar rascunho")
                }
        }
    }

    fun discardDraft(intentId: String) {
        val session = currentSession.value ?: return
        scope.launch {
            runCatching { repository.discardDraft(session, intentId) }
                .onSuccess { _message.value = "Rascunho descartado" }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _message.value = userMessage(error, "Falha ao descartar rascunho")
                }
        }
    }

    fun resolveConflict(conflict: CharacterSyncConflict, remoteFieldIds: Set<String>) {
        if (!ensureOnlineForMutation()) return
        val session = currentSession.value ?: return
        scope.launch {
            runCatching { repository.resolveConflict(session, conflict, remoteFieldIds) }
                .onSuccess {
                    _conflicts.value = _conflicts.value.filterNot {
                        it.local.id == conflict.local.id && it.remoteUpdatedAt == conflict.remoteUpdatedAt
                    }
                    _message.value = "Conflito resolvido"
                    if (_conflicts.value.isEmpty() && automaticSync) startCharacterSync(conflict.local.id)
                }
                .onFailure { _message.value = userMessage(it, "Falha ao resolver conflito") }
        }
    }

    private fun runCharacterAction(success: String? = null, action: suspend (UserSession) -> String) {
        if (!ensureOnlineForMutation()) return
        val session = currentSession.value ?: return
        scope.launch {
            runCatching { action(session) }
                .onSuccess { characterId ->
                    if (success != null) _message.value = success
                    if (automaticSync) startCharacterSync(characterId)
                }
                .onFailure { _message.value = userMessage(it, "Falha na operação") }
        }
    }

    private fun runAction(success: String? = null, action: suspend (UserSession) -> Unit) {
        if (!ensureOnlineForMutation()) return
        val session = currentSession.value ?: return
        scope.launch {
            runCatching { action(session) }
                .onSuccess {
                    if (success != null) _message.value = success
                    if (automaticSync) startSync(initial = false)
                }
                .onFailure { _message.value = userMessage(it, "Falha na operação") }
        }
    }

    private fun handleRemoteSyncEvent(event: RemoteSyncEvent) {
        if (!event.fromCloud) return
        val session = currentSession.value ?: return
        if (session.uid == DEMO_USER_ID || !connectivityMonitor.isOnline.value) return
        if (cloudSyncPulseMs > 0L) {
            cloudSyncPulseJob?.cancel()
            _loadState.value = _loadState.value.copy(cloudSyncing = true)
            cloudSyncPulseJob = scope.launch {
                delay(cloudSyncPulseMs)
                if (syncJob?.isActive != true) {
                    _loadState.value = _loadState.value.copy(cloudSyncing = false)
                }
            }
        }
        if (event.fullSync) {
            startSync(initial = false, fromCloud = true)
        } else {
            event.characterIds.forEach { characterId ->
                startCharacterSync(characterId, fromCloud = true)
            }
        }
    }

    private fun startCharacterSync(characterId: String, fromCloud: Boolean = false) {
        val session = currentSession.value ?: return
        if (session.uid == DEMO_USER_ID || characterId.isBlank()) return
        if (!connectivityMonitor.isOnline.value) {
            _loadState.value = CharacterLoadState()
            return
        }
        if (syncJob?.isActive == true) {
            syncRequested = true
            return
        }
        scope.launch {
            _loadState.value = _loadState.value.copy(
                syncing = true,
                cloudSyncing = _loadState.value.cloudSyncing || fromCloud,
            )
            try {
                runCatching { repository.syncCharacter(session, characterId) }
                    .onSuccess { charConflicts ->
                        _conflicts.value = _conflicts.value.filterNot { it.local.id == characterId } + charConflicts
                    }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        android.util.Log.e("SDO_SYNC", "syncCharacter($characterId) failed", error)
                    }
            } finally {
                val keepCloudSyncing = fromCloud && (cloudSyncPulseMs > 0L && cloudSyncPulseJob?.isActive == true)
                _loadState.value = _loadState.value.copy(
                    syncing = false,
                    cloudSyncing = if (fromCloud && !keepCloudSyncing) false else _loadState.value.cloudSyncing,
                )
            }
        }
    }

    private fun isMutationBlockedOffline(session: UserSession?): Boolean =
        session != null && session.uid != DEMO_USER_ID && !connectivityMonitor.isOnline.value

    private fun ensureSheetMutation(character: Character): Boolean {
        val session = currentSession.value ?: return false
        if (!isMutationBlockedOffline(session) || character.id in offlineEditableIds.value) return true
        _message.value = "Conecte uma vez para preparar esta ficha para edição offline."
        return false
    }

    private fun ensureOnlineForMutation(): Boolean {
        val session = currentSession.value ?: return false
        if (isMutationBlockedOffline(session)) {
            _message.value = OFFLINE_READ_ONLY_MESSAGE
            return false
        }
        return true
    }

    private fun startSync(initial: Boolean, fromCloud: Boolean = false) {
        val session = currentSession.value ?: return
        if (session.uid == DEMO_USER_ID) return
        if (!connectivityMonitor.isOnline.value) {
            _loadState.value = CharacterLoadState()
            return
        }
        if (syncJob?.isActive == true) {
            syncRequested = true
            return
        }
        syncJob = scope.launch {
            _loadState.value = _loadState.value.copy(
                initialLoading = initial,
                syncing = true,
                cloudSyncing = _loadState.value.cloudSyncing || fromCloud,
            )
            try {
                val failures = mutableListOf<Pair<String, Throwable>>()
                coroutineScope {
                    val campaignJob = async {
                        runCatching { campaignRepository.sync(session) }
                    }
                    val ownerJob = async {
                        runCatching { ownerRepository.sync(session) }
                    }
                    campaignJob.await().onFailure {
                        android.util.Log.e("SDO_SYNC", "campaignRepository.sync failed", it)
                        failures += "campanhas" to it
                    }
                    ownerJob.await().onFailure {
                        android.util.Log.e("SDO_SYNC", "ownerRepository.sync failed", it)
                        failures += "administração" to it
                    }
                }
                // Delivery acceptance claims and applies remote content atomically before the
                // general character sync can upload an offline draft of the same sheet.
                runCatching {
                    operationsRepository.sync(
                        session = session,
                        readableCampaignIds = accessibleCampaignIds.value,
                        writableCampaignIds = manageableCampaignIds.value,
                    )
                }
                    .onFailure {
                        android.util.Log.e("SDO_SYNC", "operationsRepository.sync failed", it)
                        failures += "operações" to it
                    }
                // Personal sync always runs, even if campaign authorization or queries fail.
                // Keeping it after campaigns also lets a freshly accepted invite create its
                // membership before the selected character is linked remotely.
                runCatching { _conflicts.value = repository.sync(session) }
                    .onFailure {
                        android.util.Log.e("SDO_SYNC", "characterRepository.sync failed", it)
                        failures += "fichas pessoais" to it
                    }
                if (failures.isEmpty()) {
                    lastSyncFailure = null
                    _syncFailure.value = null
                } else {
                    val failureMessage = syncFailureMessage(failures)
                    _syncFailure.value = failureMessage
                    // Automatic saves can trigger several sync cycles while the user moves
                    // through the sheet. Report one persistent failure once, instead of
                    // showing the same global snackbar on every screen.
                    if (failureMessage != lastSyncFailure) _message.value = failureMessage
                    lastSyncFailure = failureMessage
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                val failure = userMessage(error, "Falha ao sincronizar dados")
                _syncFailure.value = failure
                _message.value = failure
            } finally {
                if (syncJob === currentCoroutineContext()[Job]) {
                    val keepCloudSyncing = cloudSyncPulseJob?.isActive == true
                    _loadState.value = CharacterLoadState(cloudSyncing = keepCloudSyncing)
                    syncJob = null
                    if (syncRequested) {
                        syncRequested = false
                        startSync(initial = false)
                    }
                }
            }
        }
    }

    private fun userMessage(error: Throwable, fallback: String): String {
        val detail = error.localizedMessage.orEmpty()
        return if (detail.contains("PERMISSION_DENIED", ignoreCase = true)) {
            "A sessão não pôde acessar o Firebase. Entre novamente se o problema continuar."
        } else {
            detail.ifBlank { fallback }
        }
    }

    private fun syncFailureMessage(failures: List<Pair<String, Throwable>>): String {
        val areas = failures.joinToString { it.first }
        val details = failures.joinToString(" ") { it.second.message.orEmpty() }
        return when {
            details.contains("UNAUTHENTICATED", ignoreCase = true) ->
                "Sua autenticação expirou. Entre novamente. Alterações locais foram preservadas."
            details.contains("UNAVAILABLE", ignoreCase = true) ||
                details.contains("network", ignoreCase = true) ||
                details.contains("offline", ignoreCase = true) ->
                "Sem conexão para sincronizar $areas. Alterações locais foram preservadas."
            details.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Sem permissão para sincronizar $areas. As demais áreas continuam disponíveis e as alterações locais foram preservadas."
            else -> "Sincronização parcial em $areas. Alterações locais foram preservadas."
        }
    }

    companion object {
        const val DEMO_USER_ID = "demo-player"
        const val OFFLINE_READ_ONLY_MESSAGE =
            "Sem conexão. O aplicativo está em modo somente leitura; reconecte-se para editar."
    }
}
