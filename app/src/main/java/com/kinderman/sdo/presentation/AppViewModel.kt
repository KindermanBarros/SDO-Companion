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
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.repository.CampaignRepository
import com.kinderman.sdo.domain.repository.CatalogRepository
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.repository.OwnerRepository
import com.kinderman.sdo.domain.repository.OperationsRepository
import com.kinderman.sdo.domain.usecase.DeleteCharacter
import com.kinderman.sdo.domain.usecase.SaveCharacter
import com.kinderman.sdo.domain.usecase.SetHistorianLock
import com.kinderman.sdo.domain.usecase.SetPlayerLock
import com.kinderman.sdo.domain.usecase.TransferCharacterOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CharacterLoadState(
    val initialLoading: Boolean = false,
    val syncing: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val repository: CharacterRepository,
    private val ownerRepository: OwnerRepository,
    catalogRepository: CatalogRepository,
    private val campaignRepository: CampaignRepository,
    private val operationsRepository: OperationsRepository,
) : ViewModel() {
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
    private val localSaveMutex = Mutex()
    private val autosaveJobs = mutableMapOf<String, Job>()

    val session = currentSession.asStateFlow()
    private val _saveErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val saveErrors = _saveErrors.asStateFlow()
    val message = _message.asStateFlow()
    val loadState = _loadState.asStateFlow()
    val conflicts = _conflicts.asStateFlow()
    val invitePreview = _invitePreview.asStateFlow()
    val catalog = catalogRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val campaigns = currentSession.flatMapLatest { session ->
        session?.let(campaignRepository::observe) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val characters = currentSession.flatMapLatest { session ->
        session?.let(repository::observe) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val memberships = currentSession.flatMapLatest { session ->
        session?.let(campaignRepository::observeMemberships) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<CampaignMember>())
    val campaignInvites = campaignRepository.observeAllInvites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val owners = currentSession.flatMapLatest { session ->
        if (session?.isAdmin == true) ownerRepository.observe() else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val campaignMembers = campaigns.flatMapLatest { values ->
        if (values.isEmpty()) flowOf(emptyList())
        else combine(values.map { campaignRepository.observeMembers(it.id) }) { groups -> groups.flatMap { it } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<CampaignMember>())
    private val manageableCampaignIds = combine(currentSession, campaigns) { session, values ->
        if (session == null) emptySet() else values.filter { campaign ->
            session.isAdmin || campaign.ownerId == session.uid
        }.mapTo(linkedSetOf(), Campaign::id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val audit = manageableCampaignIds.flatMapLatest(operationsRepository::observeAudit)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val library = manageableCampaignIds.flatMapLatest(operationsRepository::observeLibrary)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val deliveries = combine(currentSession, manageableCampaignIds) { session, ids -> session to ids }
        .flatMapLatest { (session, ids) -> session?.let { operationsRepository.observeDeliveries(it, ids) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val alertSettings = manageableCampaignIds.flatMapLatest(operationsRepository::observeAlertSettings)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSession(session: UserSession) {
        if (currentSession.value == session) return
        syncJob?.cancel()
        syncJob = null
        syncRequested = false
        _conflicts.value = emptyList()
        _invitePreview.value = null
        currentSession.value = session
        startSync(initial = true)
    }

    fun setDemoSession() = setSession(UserSession("demo-player", "", "Modo local", UserRole.USER))
    fun setAutomaticSync(enabled: Boolean) { automaticSync = enabled }

    fun clearSession() {
        autosaveJobs.values.forEach(Job::cancel)
        autosaveJobs.clear()
        syncJob?.cancel()
        syncJob = null
        syncRequested = false
        currentSession.value = null
        _message.value = null
        _conflicts.value = emptyList()
        _invitePreview.value = null
        _loadState.value = CharacterLoadState()
    }

    fun add() = runAction { session -> repository.create(session) }

    fun addToCampaign(campaign: Campaign, ownerId: String) = runAction("Ficha criada na campanha") { session ->
        repository.create(session, ownerId = ownerId, campaignId = campaign.id)
    }

    fun save(character: Character) {
        autosaveJobs.remove(character.id)?.cancel()
        saveLocally(character, notify = true)
    }

    fun autosave(character: Character) {
        autosaveJobs.remove(character.id)?.cancel()
        val job = viewModelScope.launch {
            delay(350)
            persistLocally(character, notify = false)
        }
        autosaveJobs[character.id] = job
        job.invokeOnCompletion { if (autosaveJobs[character.id] === job) autosaveJobs.remove(character.id) }
    }

    fun applySessionCommand(character: Character, command: SessionCommand) {
        val session = currentSession.value ?: return
        viewModelScope.launch {
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
    fun saveAlertSettings(settings: CampaignAlertSettings) = viewModelScope.launch { operationsRepository.saveAlertSettings(settings) }

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
        val session = currentSession.value ?: return
        viewModelScope.launch {
            runCatching { campaignRepository.createInvite(session, campaign) }
                .onSuccess { invite ->
                    _message.value = "Código de convite: ${invite.code}"
                    if (automaticSync) startSync(initial = false)
                }
                .onFailure { _message.value = userMessage(it, "Falha ao criar convite") }
        }
    }

    fun previewCampaignInvite(code: String) {
        val session = currentSession.value ?: return
        viewModelScope.launch {
            runCatching { campaignRepository.previewInvite(session, code) }
                .onSuccess { preview ->
                    _invitePreview.value = preview
                    if (preview == null) _message.value = "Convite inválido, revogado ou expirado"
                }
                .onFailure { _message.value = userMessage(it, "Falha ao consultar convite") }
        }
    }

    fun acceptCampaignInvite(code: String, character: Character?, createNewCharacter: Boolean) {
        val session = currentSession.value ?: return
        viewModelScope.launch {
            runCatching {
                val campaign = campaignRepository.joinByCode(session, code)
                val selectedCharacter = when {
                    createNewCharacter -> repository.create(session)
                    character != null -> character
                    else -> null
                }
                if (selectedCharacter != null) {
                    val linked = campaignRepository.linkCharacter(session, selectedCharacter, campaign)
                    repository.save(session, linked)
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
        val linked = campaignRepository.linkCharacter(session, character, campaign)
        repository.save(session, linked)
    }

    fun unlinkCharacter(character: Character) = runAction("Ficha removida da campanha") { session ->
        val unlinked = campaignRepository.unlinkCharacter(session, character)
        repository.save(session, unlinked)
    }

    fun setPlayerLocked(character: Character, locked: Boolean) = runAction(
        if (locked) "Bloqueio pessoal ativado" else "Bloqueio pessoal removido",
    ) { session -> setPlayerLock(session, character, locked) }

    fun setHistorianLocked(character: Character, locked: Boolean) = runAction(
        if (locked) "Bloqueio do historiador ativado" else "Bloqueio do historiador removido",
    ) { session -> setHistorianLock(session, character, locked) }

    fun transferOwner(character: Character, owner: UserProfile) = runAction(
        "Owner transferido para ${owner.firstName}",
    ) { session -> transferCharacterOwner(session, character, owner) }

    fun delete(character: Character) = runAction("Personagem removido") { session -> deleteCharacter(session, character) }

    fun dismissMessage() { _message.value = null }
    fun sync() = startSync(initial = false)

    private fun saveLocally(character: Character, notify: Boolean) {
        viewModelScope.launch {
            persistLocally(character, notify)
        }
    }

    private suspend fun persistLocally(character: Character, notify: Boolean) {
        val session = currentSession.value ?: return
        runCatching { localSaveMutex.withLock { saveCharacter(session, character) } }
            .onSuccess {
                _saveErrors.value = _saveErrors.value - character.id
                if (notify) _message.value = "Ficha salva localmente"
            }
            .onFailure {
                val error = userMessage(it, "Falha ao salvar ficha localmente")
                _saveErrors.value = _saveErrors.value + (character.id to error)
                _message.value = error
            }
    }

    fun resolveConflict(conflict: CharacterSyncConflict, remoteFieldIds: Set<String>) {
        val session = currentSession.value ?: return
        viewModelScope.launch {
            runCatching { repository.resolveConflict(session, conflict, remoteFieldIds) }
                .onSuccess {
                    _conflicts.value = _conflicts.value.filterNot {
                        it.local.id == conflict.local.id && it.remoteUpdatedAt == conflict.remoteUpdatedAt
                    }
                    _message.value = "Conflito resolvido"
                    if (_conflicts.value.isEmpty()) startSync(initial = false)
                }
                .onFailure { _message.value = userMessage(it, "Falha ao resolver conflito") }
        }
    }

    private fun runAction(success: String? = null, action: suspend (UserSession) -> Unit) {
        val session = currentSession.value ?: return
        viewModelScope.launch {
            runCatching { action(session) }
                .onSuccess {
                    if (success != null) _message.value = success
                    if (automaticSync) startSync(initial = false)
                }
                .onFailure { _message.value = userMessage(it, "Falha na operação") }
        }
    }

    private fun startSync(initial: Boolean) {
        val session = currentSession.value ?: return
        if (syncJob?.isActive == true) {
            syncRequested = true
            return
        }
        syncJob = viewModelScope.launch {
            _loadState.value = CharacterLoadState(initialLoading = initial, syncing = true)
            try {
                val failures = mutableListOf<Pair<String, Throwable>>()
                runCatching { campaignRepository.sync(session) }
                    .onFailure { failures += "campanhas" to it }
                // Delivery acceptance claims and applies remote content atomically before the
                // general character sync can upload an offline draft of the same sheet.
                runCatching { operationsRepository.sync(session, manageableCampaignIds.value) }
                    .onFailure { failures += "operações" to it }
                // Personal sync always runs, even if campaign authorization or queries fail.
                // Keeping it after campaigns also lets a freshly accepted invite create its
                // membership before the selected character is linked remotely.
                runCatching { _conflicts.value = repository.sync(session) }
                    .onFailure { failures += "fichas pessoais" to it }
                runCatching { ownerRepository.sync(session) }
                    .onFailure { failures += "administração" to it }
                if (failures.isNotEmpty()) _message.value = syncFailureMessage(failures)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _message.value = userMessage(error, "Falha ao sincronizar dados")
            } finally {
                if (syncJob === currentCoroutineContext()[Job]) {
                    _loadState.value = CharacterLoadState()
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
}
