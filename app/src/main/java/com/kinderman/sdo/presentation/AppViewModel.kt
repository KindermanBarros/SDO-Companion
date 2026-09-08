package com.kinderman.sdo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignInvitePreview
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.repository.CampaignRepository
import com.kinderman.sdo.domain.repository.CatalogRepository
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.repository.OwnerRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val localSaveMutex = Mutex()

    val session = currentSession.asStateFlow()
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
    val owners = currentSession.flatMapLatest { session ->
        if (session?.isMaster == true) ownerRepository.observe() else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun setDemoSession() = setSession(UserSession("demo-player", "", "Modo local", UserRole.PLAYER))

    fun clearSession() {
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

    fun addToCampaign(campaign: Campaign) = runAction("Ficha criada na campanha") { session ->
        val created = repository.create(session)
        val linked = campaignRepository.linkCharacter(session, created, campaign)
        repository.save(session, linked)
    }

    fun save(character: Character) = saveLocally(character, notify = true)
    fun autosave(character: Character) = saveLocally(character, notify = false)

    fun createCampaign(name: String, description: String) = runAction("Campanha criada") { session ->
        campaignRepository.create(session, name, description)
    }

    fun archiveCampaign(campaign: Campaign, archived: Boolean) = runAction(
        if (archived) "Campanha arquivada" else "Campanha restaurada",
    ) { session ->
        campaignRepository.archive(session, campaign, archived)
    }

    fun leaveCampaign(campaign: Campaign) = runAction("Você saiu da campanha; suas fichas foram preservadas") { session ->
        campaignRepository.leave(session, campaign)
    }

    fun removeCampaignMember(campaign: Campaign, userId: String) = runAction("Participante removido; fichas preservadas") { session ->
        campaignRepository.removeMember(session, campaign, userId)
    }

    fun transferCampaignOwner(campaign: Campaign, newOwnerId: String) = runAction("Responsabilidade transferida") { session ->
        campaignRepository.transferOwnership(session, campaign, newOwnerId)
    }

    fun createCampaignInvite(campaign: Campaign) {
        val session = currentSession.value ?: return
        viewModelScope.launch {
            runCatching { campaignRepository.createInvite(session, campaign) }
                .onSuccess { invite ->
                    _message.value = "Convite ${invite.code} criado"
                    startSync(initial = false)
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

    fun joinCampaign(code: String) = runAction("Entrada na campanha concluída") { session ->
        campaignRepository.joinByCode(session, code)
        _invitePreview.value = null
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
        val session = currentSession.value ?: return
        viewModelScope.launch {
            runCatching { localSaveMutex.withLock { saveCharacter(session, character) } }
                .onSuccess { if (notify) _message.value = "Ficha salva localmente" }
                .onFailure { _message.value = userMessage(it, "Falha ao salvar ficha localmente") }
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
                    startSync(initial = false)
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
                ownerRepository.sync(session)
                campaignRepository.sync(session)
                _conflicts.value = repository.sync(session)
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
}
