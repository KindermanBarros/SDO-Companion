package com.kinderman.sdo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.UserProfile
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

data class CharacterLoadState(
    val initialLoading: Boolean = false,
    val syncing: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val repository: CharacterRepository,
    private val ownerRepository: OwnerRepository,
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
    private var syncJob: Job? = null
    private var syncRequested = false

    val session = currentSession.asStateFlow()
    val message = _message.asStateFlow()
    val loadState = _loadState.asStateFlow()
    val conflicts = _conflicts.asStateFlow()
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
        _loadState.value = CharacterLoadState()
    }

    fun add() = runAction { session -> repository.create(session) }

    fun save(character: Character) = runAction("Ficha salva") { session -> saveCharacter(session, character) }

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
                _conflicts.value = repository.sync(session)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _message.value = userMessage(error, "Falha ao sincronizar personagens")
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
