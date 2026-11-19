package com.kinderman.sdo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.kinderman.sdo.data.local.CampaignDao
import com.kinderman.sdo.data.local.CharacterDao
import com.kinderman.sdo.data.local.CharacterMigrationManager
import com.kinderman.sdo.data.local.OwnerDao
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.sync.SparkFirestoreSyncEngine
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.SheetConflictItem
import com.kinderman.sdo.domain.model.SheetIntent
import com.kinderman.sdo.domain.model.SheetSyncStatus
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.repository.RemoteSyncEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncedCharacterRepository(
    private val dao: CharacterDao,
    private val ownerDao: OwnerDao,
    private val campaignDao: CampaignDao,
    private val migrationManager: CharacterMigrationManager = CharacterMigrationManager(),
    private val syncEngine: SparkFirestoreSyncEngine? = null,
) : CharacterRepository {
    private val syncMutex = Mutex()

    override fun observeSyncStatus(session: UserSession): Flow<SheetSyncStatus> =
        syncEngine?.observeStatus(session) ?: flowOf(SheetSyncStatus())

    override fun observeIntents(session: UserSession): Flow<List<SheetIntent>> =
        syncEngine?.observeIntents(session) ?: emptyFlow()

    override fun observeConflicts(session: UserSession): Flow<List<SheetConflictItem>> =
        syncEngine?.observeConflicts(session) ?: emptyFlow()

    override suspend fun resolveIntentConflict(
        session: UserSession,
        conflictId: String,
        useRemote: Boolean,
        customValueJson: String?,
    ) {
        syncEngine?.resolveIntentConflict(session, conflictId, useRemote, customValueJson)
    }

    override suspend fun retryAccessDeniedDraft(session: UserSession, intentId: String) {
        syncEngine?.retryAccessDeniedDraft(session, intentId)
    }

    override suspend fun discardDraft(session: UserSession, intentId: String) {
        syncEngine?.discardDraft(session, intentId)
    }

    override fun observeOfflineEditableIds(session: UserSession): Flow<Set<String>> =
        syncEngine?.observeOfflineEditableIds(session)
            ?: dao.observe(session.uid).map { records -> records.mapTo(linkedSetOf()) { it.id } }

    override suspend fun supportsOfflineEditing(characterId: String): Boolean {
        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull().orEmpty()
        return if (syncEngine != null && uid.isNotBlank()) {
            syncEngine.supportsOffline(uid, characterId)
        } else {
            dao.get(characterId) != null
        }
    }

    override suspend fun setFieldPresence(
        session: UserSession,
        characterId: String,
        fieldKey: String?,
        active: Boolean,
    ) {
        syncEngine?.setFieldPresence(session, characterId, fieldKey, active)
    }

    override fun observeFieldPresence(session: UserSession, characterId: String): Flow<Map<String, String>> =
        syncEngine?.observeFieldPresence(session, characterId) ?: flowOf(emptyMap())

    override fun observe(session: UserSession): Flow<List<Character>> = flow {
        syncEngine?.ensureAccountProjected(session.uid)
        emitAll(
            (if (session.isAdmin) dao.observeAll() else dao.observe(session.uid))
                .map { records ->
                    records.map { record ->
                        val migrated = migrationManager.migrate(record, markDirty = false)
                        if (migrated != record) dao.upsert(migrated)
                        migrated.toDomain().let { value -> value.copy(editingBase = value) }
                    }
                }
                .distinctUntilChanged()
                .flowOn(Dispatchers.Default),
        )
    }

    override fun observeOne(id: String): Flow<Character?> = flow {
        coroutineScope {
            val listener = launch { syncEngine?.observeOpenSheet(id)?.collect {} }
            try {
                emitAll(dao.observeOne(id).map { record ->
                    record?.let {
                        val migrated = migrationManager.migrate(it, markDirty = false)
                        if (migrated != it) dao.upsert(migrated)
                        migrated.toDomain().let { value -> value.copy(editingBase = value) }
                    }
                }.distinctUntilChanged())
            } finally {
                listener.cancel()
            }
        }
    }.flowOn(Dispatchers.Default)

    override fun observeRemoteChanges(session: UserSession, campaignIds: Set<String>): Flow<RemoteSyncEvent> =
        syncEngine?.observeRemoteChanges(session, campaignIds) ?: emptyFlow()

    override suspend fun create(session: UserSession, ownerId: String, campaignId: String): Character {
        val engine = requireNotNull(syncEngine) { "SparkFirestoreSyncEngine indisponível." }
        return engine.create(session, ownerId, campaignId)
    }

    override suspend fun save(session: UserSession, character: Character) {
        val engine = requireNotNull(syncEngine) { "SparkFirestoreSyncEngine indisponível." }
        engine.save(session, character)
    }

    override suspend fun setPlayerLocked(session: UserSession, character: Character, locked: Boolean) {
        val engine = requireNotNull(syncEngine) { "SparkFirestoreSyncEngine indisponível." }
        engine.setPlayerLocked(session, character, locked)
    }

    override suspend fun setHistorianLocked(session: UserSession, character: Character, locked: Boolean) {
        val engine = requireNotNull(syncEngine) { "SparkFirestoreSyncEngine indisponível." }
        engine.setHistorianLocked(session, character, locked)
    }

    override suspend fun transferOwnership(session: UserSession, character: Character, owner: UserProfile) {
        val engine = requireNotNull(syncEngine) { "SparkFirestoreSyncEngine indisponível." }
        engine.transferOwnership(session, character, owner)
    }

    override suspend fun delete(session: UserSession, character: Character) {
        val engine = requireNotNull(syncEngine) { "SparkFirestoreSyncEngine indisponível." }
        engine.delete(session, character)
    }

    override suspend fun sync(session: UserSession): List<CharacterSyncConflict> = syncMutex.withLock {
        syncEngine?.sync(session) ?: emptyList()
    }

    override suspend fun syncCharacter(session: UserSession, characterId: String): List<CharacterSyncConflict> =
        syncCharacter(session, characterId, pullRemote = true)

    override suspend fun syncCharacter(
        session: UserSession,
        characterId: String,
        pullRemote: Boolean,
    ): List<CharacterSyncConflict> =
        syncMutex.withLock {
            syncEngine?.syncCharacter(session, characterId, pullRemote = pullRemote) ?: emptyList()
        }

    override suspend fun resolveConflict(
        session: UserSession,
        conflict: CharacterSyncConflict,
        remoteFieldIds: Set<String>,
    ) {
        syncMutex.withLock {
            syncEngine?.resolveLegacyConflict(session, conflict, remoteFieldIds)
        }
    }
}
