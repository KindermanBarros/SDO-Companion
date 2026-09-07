package com.kinderman.sdo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.usecase.DeleteCharacter
import com.kinderman.sdo.domain.usecase.SaveCharacter
import com.kinderman.sdo.domain.usecase.SetCharacterLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val repository: CharacterRepository) : ViewModel() {
    private val saveCharacter = SaveCharacter(repository)
    private val deleteCharacter = DeleteCharacter(repository)
    private val setCharacterLock = SetCharacterLock(repository)
    private val currentSession = MutableStateFlow<UserSession?>(null)
    private val _message = MutableStateFlow<String?>(null)

    val session = currentSession.asStateFlow()
    val message = _message.asStateFlow()
    val characters = currentSession.flatMapLatest { session ->
        session?.let(repository::observe) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSession(session: UserSession) {
        currentSession.value = session
        sync()
    }

    fun setDemoSession() = setSession(UserSession("demo-player", "", "Modo local", UserRole.PLAYER))

    fun clearSession() {
        currentSession.value = null
        _message.value = null
    }

    fun add() = runAction { session -> repository.create(session) }

    fun save(character: Character) = runAction("Ficha salva") { session -> saveCharacter(session, character) }

    fun setLocked(character: Character, locked: Boolean) = runAction(
        if (locked) "Ficha trancada" else "Ficha destrancada",
    ) { session -> setCharacterLock(session, character, locked) }

    fun delete(character: Character) = runAction("Personagem removido") { session -> deleteCharacter(session, character) }

    fun dismissMessage() { _message.value = null }

    fun sync() = runAction { session -> repository.sync(session) }

    private fun runAction(success: String? = null, action: suspend (UserSession) -> Unit) {
        val session = currentSession.value ?: return
        viewModelScope.launch {
            runCatching { action(session) }
                .onSuccess {
                    if (success != null) _message.value = success
                    runCatching { repository.sync(session) }
                }
                .onFailure { _message.value = it.localizedMessage ?: "Falha na operação" }
        }
    }
}
