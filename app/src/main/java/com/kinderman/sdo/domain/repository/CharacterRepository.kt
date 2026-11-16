package com.kinderman.sdo.domain.repository

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class RemoteSyncEvent(
    val fromCloud: Boolean = false,
    val characterIds: Set<String> = emptySet(),
    val fullSync: Boolean = true,
)

interface CharacterRepository {
    fun observeSyncStatus(session: UserSession): Flow<com.kinderman.sdo.domain.model.SheetSyncStatus> =
        kotlinx.coroutines.flow.flowOf(com.kinderman.sdo.domain.model.SheetSyncStatus())
    fun observeOfflineEditableIds(session: UserSession): Flow<Set<String>> = kotlinx.coroutines.flow.flowOf(emptySet())
    suspend fun supportsOfflineEditing(characterId: String): Boolean = false
    suspend fun setFieldPresence(session: UserSession, characterId: String, fieldKey: String?, active: Boolean) = Unit
    fun observeFieldPresence(session: UserSession, characterId: String): Flow<Map<String, String>> =
        kotlinx.coroutines.flow.flowOf(emptyMap())
    fun observe(session: UserSession): Flow<List<Character>>
    fun observeOne(id: String): Flow<Character?>
    fun observeRemoteChanges(session: UserSession, campaignIds: Set<String>): Flow<RemoteSyncEvent> = emptyFlow()
    suspend fun create(session: UserSession, ownerId: String = session.uid, campaignId: String = ""): Character
    suspend fun save(session: UserSession, character: Character)
    suspend fun setPlayerLocked(session: UserSession, character: Character, locked: Boolean)
    suspend fun setHistorianLocked(session: UserSession, character: Character, locked: Boolean)
    suspend fun transferOwnership(session: UserSession, character: Character, owner: UserProfile)
    suspend fun delete(session: UserSession, character: Character)
    suspend fun sync(session: UserSession): List<CharacterSyncConflict>
    suspend fun syncCharacter(session: UserSession, characterId: String): List<CharacterSyncConflict> = sync(session)
    suspend fun resolveConflict(
        session: UserSession,
        conflict: CharacterSyncConflict,
        remoteFieldIds: Set<String>,
    )
}
