package com.kinderman.sdo.domain.repository

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.SheetConflictItem
import com.kinderman.sdo.domain.model.SheetIntent
import com.kinderman.sdo.domain.model.SheetSyncStatus
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

data class RemoteSyncEvent(
    val fromCloud: Boolean = false,
    val characterIds: Set<String> = emptySet(),
    val fullSync: Boolean = true,
)

interface CharacterRepository {
    fun observeSyncStatus(session: UserSession): Flow<SheetSyncStatus> = flowOf(SheetSyncStatus())
    fun observeIntents(session: UserSession): Flow<List<SheetIntent>> = emptyFlow()
    fun observeConflicts(session: UserSession): Flow<List<SheetConflictItem>> = emptyFlow()
    suspend fun resolveIntentConflict(
        session: UserSession,
        conflictId: String,
        useRemote: Boolean,
        customValueJson: String? = null,
    ) = Unit
    suspend fun retryAccessDeniedDraft(session: UserSession, intentId: String) = Unit
    suspend fun discardDraft(session: UserSession, intentId: String) = Unit
    fun observeOfflineEditableIds(session: UserSession): Flow<Set<String>> = flowOf(emptySet())
    suspend fun supportsOfflineEditing(characterId: String): Boolean = false
    suspend fun setFieldPresence(session: UserSession, characterId: String, fieldKey: String?, active: Boolean) = Unit
    fun observeFieldPresence(session: UserSession, characterId: String): Flow<Map<String, String>> = flowOf(emptyMap())
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
