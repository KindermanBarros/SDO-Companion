package com.kinderman.sdo.domain.repository

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun observe(session: UserSession): Flow<List<Character>>
    fun observeOne(id: String): Flow<Character?>
    suspend fun create(session: UserSession): Character
    suspend fun save(session: UserSession, character: Character)
    suspend fun setPlayerLocked(session: UserSession, character: Character, locked: Boolean)
    suspend fun setHistorianLocked(session: UserSession, character: Character, locked: Boolean)
    suspend fun delete(session: UserSession, character: Character)
    suspend fun sync(session: UserSession)
}
