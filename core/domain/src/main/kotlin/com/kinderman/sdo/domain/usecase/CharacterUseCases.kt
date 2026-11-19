package com.kinderman.sdo.domain.usecase

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.repository.CharacterRepository

class SaveCharacter(private val repository: CharacterRepository) {
    suspend operator fun invoke(session: UserSession, character: Character) = repository.save(session, character)
}

class DeleteCharacter(private val repository: CharacterRepository) {
    suspend operator fun invoke(session: UserSession, character: Character) = repository.delete(session, character)
}

class SetPlayerLock(private val repository: CharacterRepository) {
    suspend operator fun invoke(session: UserSession, character: Character, locked: Boolean) =
        repository.setPlayerLocked(session, character, locked)
}

class SetHistorianLock(private val repository: CharacterRepository) {
    suspend operator fun invoke(session: UserSession, character: Character, locked: Boolean) =
        repository.setHistorianLocked(session, character, locked)
}

class TransferCharacterOwner(private val repository: CharacterRepository) {
    suspend operator fun invoke(session: UserSession, character: Character, owner: UserProfile) =
        repository.transferOwnership(session, character, owner)
}
