package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.data.local.CharacterDao
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.CampaignDao
import com.kinderman.sdo.data.local.OwnerDao
import com.kinderman.sdo.data.local.hasRedundantLegacyProjections
import com.kinderman.sdo.data.local.optimizedForFirestore
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.data.local.CharacterMigrationManager
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.needsLegacySyncReview
import com.kinderman.sdo.domain.model.withPendingEditKeys
import com.kinderman.sdo.domain.model.commitCharacterEditKeys
import com.kinderman.sdo.domain.model.pendingCharacterEditKeys
import com.kinderman.sdo.domain.model.characterEditKeys
import com.kinderman.sdo.domain.model.CURRENT_CHARACTER_SCHEMA_VERSION
import com.kinderman.sdo.domain.model.DomainError
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.CampaignMemberState
import com.kinderman.sdo.domain.model.CampaignRole
import com.kinderman.sdo.domain.model.characterConflictFields
import com.kinderman.sdo.domain.model.mergeCharacterConflict
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.domain.policy.CharacterAccessPolicy
import com.kinderman.sdo.domain.repository.CharacterRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean


private sealed interface CharacterSyncWrite {
    data class Accepted(val record: CharacterRecord) : CharacterSyncWrite
    data class LegacyReview(val conflict: CharacterSyncConflict) : CharacterSyncWrite
}

class SyncedCharacterRepository(
    private val dao: CharacterDao,
    private val ownerDao: OwnerDao,
    private val campaignDao: CampaignDao,
    private val migrationManager: CharacterMigrationManager = CharacterMigrationManager(),
) : CharacterRepository {
    private val syncMutex = Mutex()

    override fun observe(session: UserSession): Flow<List<Character>> =
        (if (session.isAdmin) dao.observeAll() else dao.observe(session.uid))
            .map { records ->
                records.map { record ->
                    val migrated = migrationManager.migrate(record, markDirty = true)
                    if (migrated != record) dao.upsertTracked(migrated)
                    migrated.toDomain().let { value -> value.copy(editingBase = value) }
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    override fun observeOne(id: String): Flow<Character?> = dao.observeOne(id).map { record ->
        record?.let {
            val migrated = migrationManager.migrate(it, markDirty = true)
            if (migrated != it) dao.upsertTracked(migrated)
            migrated.toDomain().let { value -> value.copy(editingBase = value) }
        }
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    override fun observeRemoteChanges(session: UserSession, campaignIds: Set<String>): Flow<Unit> = callbackFlow {
        val store = Firebase.firestore
        val registrations = mutableListOf<ListenerRegistration>()

        fun listen(query: Query) {
            val initialized = AtomicBoolean(false)
            registrations += query.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (!initialized.compareAndSet(false, true)) {
                    if (!snapshot.metadata.hasPendingWrites()) {
                        trySend(Unit)
                    }
                }
            }
        }

        val characters = store.collection(FirestoreCollections.CHARACTERS)
        listen(if (session.isAdmin) characters else characters.whereEqualTo("ownerId", session.uid))
        listen(store.collection(FirestoreCollections.CAMPAIGN_MEMBERS).whereEqualTo("userId", session.uid))
        listen(store.collection(FirestoreCollections.CAMPAIGN_DELIVERIES).whereEqualTo("recipientId", session.uid))
        campaignIds.filter(String::isNotBlank).distinct().forEach { campaignId ->
            listen(characters.whereEqualTo("campaignId", campaignId))
            listen(store.collection(FirestoreCollections.CAMPAIGN_LIBRARY).whereEqualTo("campaignId", campaignId))
            listen(store.collection(FirestoreCollections.CAMPAIGN_AUDIT).whereEqualTo("campaignId", campaignId))
        }

        awaitClose { registrations.forEach(ListenerRegistration::remove) }
    }

    override suspend fun create(session: UserSession, ownerId: String, campaignId: String): Character {
        val normalizedCampaignId = normalizeCampaignId(campaignId)
        if (ownerId != session.uid) {
            check(normalizedCampaignId.isNotBlank()) { "A Mestre só pode atribuir uma ficha dentro da campanha." }
            check(campaignDao.campaign(normalizedCampaignId)?.ownerId == session.uid || session.isAdmin) {
                "Somente a Mestre da campanha pode atribuir fichas."
            }
            check(campaignDao.member(normalizedCampaignId, ownerId)?.state == CampaignMemberState.ACTIVE.name) {
                "A ficha só pode ser atribuída a um jogador ativo da campanha."
            }
        }
        val character = Character(ownerId = ownerId, campaignId = normalizedCampaignId, lastEditedBy = session.uid)
        dao.upsertTracked(character.toRecord())
        if (normalizedCampaignId.isNotBlank()) {
            campaignDao.member(normalizedCampaignId, ownerId)?.let { membership ->
                campaignDao.upsertMember(
                    membership.copy(
                        role = CampaignRole.PLAYER.name,
                        characterIds = (membership.characterIds + character.id).distinct(),
                        updatedAt = System.currentTimeMillis(),
                        dirty = true,
                    ),
                )
            }
        }
        return character
    }

    override suspend fun save(session: UserSession, character: Character) {
        val latest = dao.get(character.id)?.toDomain() ?: character
        check(CharacterAccessPolicy.canEdit(session, latest, isCampaignHistorian(session, latest))) {
            "Você não pode editar esta ficha."
        }
        if (character.canonicalSchemaVersion != CURRENT_CHARACTER_SCHEMA_VERSION) throw DomainError.LegacyWriteRejected()
        dao.saveEditorChange(
            character.copy(
                campaignId = normalizeCampaignId(character.campaignId),
                updatedAt = System.currentTimeMillis(),
                lastEditedBy = session.uid,
                dirty = true,
            ),
        )
    }

    override suspend fun setPlayerLocked(session: UserSession, character: Character, locked: Boolean) {
        check(CharacterAccessPolicy.canChangePlayerLock(session, character, isCampaignHistorian(session, character))) {
            "O bloqueio do jogador só pode ser alterado pelo dono e não substitui o bloqueio do historiador."
        }
        saveLock(character, if (locked) CharacterLock.PLAYER else CharacterLock.NONE, session.uid)
    }

    override suspend fun setHistorianLocked(session: UserSession, character: Character, locked: Boolean) {
        check(CharacterAccessPolicy.canChangeHistorianLock(session, isCampaignHistorian(session, character))) {
            "Somente o historiador pode alterar o bloqueio real."
        }
        saveLock(character, if (locked) CharacterLock.HISTORIAN else CharacterLock.NONE, session.uid)
    }

    override suspend fun transferOwnership(session: UserSession, character: Character, owner: UserProfile) {
        check(CharacterAccessPolicy.canTransferOwnership(session, isCampaignResponsible(session, character))) {
            "Somente a responsável principal pode transferir uma ficha."
        }
        check(owner.uid.isNotBlank()) { "O novo owner é inválido." }
        if (character.canonicalSchemaVersion != CURRENT_CHARACTER_SCHEMA_VERSION) throw DomainError.LegacyWriteRejected()
        dao.upsertTracked(
            character.copy(
                ownerId = owner.uid,
                updatedAt = System.currentTimeMillis(),
                lastEditedBy = session.uid,
                dirty = true,
            ).toRecord(),
        )
    }

    private suspend fun saveLock(character: Character, lockType: CharacterLock, actorId: String) {
        if (character.canonicalSchemaVersion != CURRENT_CHARACTER_SCHEMA_VERSION) throw DomainError.LegacyWriteRejected()
        val locked = lockType != CharacterLock.NONE
        dao.upsertTracked(
            character.copy(
                lockType = lockType,
                lockedBy = if (locked) actorId else "",
                lockedAt = if (locked) System.currentTimeMillis() else null,
                updatedAt = System.currentTimeMillis(),
                lastEditedBy = actorId,
                dirty = true,
            ).toRecord(),
        )
    }

    override suspend fun delete(session: UserSession, character: Character) {
        check(CharacterAccessPolicy.canDelete(session, character, isCampaignHistorian(session, character))) {
            "Esta ficha não pode ser removida."
        }
        dao.upsertTracked(character.copy(deleted = true, updatedAt = System.currentTimeMillis(), lastEditedBy = session.uid, dirty = true).toRecord())
    }

    override suspend fun sync(session: UserSession): List<CharacterSyncConflict> = syncMutex.withLock {
        val store = Firebase.firestore
        val collection = store.collection(FirestoreCollections.CHARACTERS)
        val campaigns = store.collection(FirestoreCollections.CAMPAIGNS)
        val members = store.collection(FirestoreCollections.CAMPAIGN_MEMBERS)
        val conflicts = mutableListOf<CharacterSyncConflict>()

        val remoteById = linkedMapOf<String, CharacterRecord>()
        val visibleSnapshot = if (session.isAdmin) {
            collection.get().await()
        } else {
            collection.whereEqualTo("ownerId", session.uid).get().await()
        }
        visibleSnapshot.documents.forEach { document ->
            document.toObject(CharacterRecord::class.java)?.copy(id = document.id)?.let { remoteById[it.id] = it }
        }

        val (membershipDocuments, ownedCampaignIds) = if (session.isAdmin) {
            emptyList<com.google.firebase.firestore.DocumentSnapshot>() to mutableSetOf<String>()
        } else {
            coroutineScope {
                val membershipsJob = async { members.whereEqualTo("userId", session.uid).get().await().documents }
                val ownedJob = async {
                    campaigns.whereEqualTo("ownerId", session.uid).get().await().documents.mapTo(mutableSetOf()) { it.id }
                }
                membershipsJob.await() to ownedJob.await()
            }
        }
        val memberCampaignIds = membershipDocuments.mapNotNull { document ->
                val campaignId = document.getString("campaignId").orEmpty()
                val state = document.getString("state").orEmpty()
                campaignId.takeIf { it.isNotBlank() && state == "ACTIVE" }
            }
            .toMutableSet()
        val authorizedCampaignIds = (memberCampaignIds + ownedCampaignIds).toSet()
        val campaignCharacterDocuments = coroutineScope {
            authorizedCampaignIds.map { campaignId ->
                async { collection.whereEqualTo("campaignId", campaignId).get().await().documents }
            }.awaitAll().flatten()
        }
        campaignCharacterDocuments.forEach { document ->
            document.toObject(CharacterRecord::class.java)?.copy(id = document.id)?.let { remoteById[it.id] = it }
        }
        val remoteRecords = remoteById.values.map { remote ->
            val migrated = migrationManager.migrate(remote, markDirty = false)
            val canWriteRemote = session.isAdmin || remote.ownerId == session.uid ||
                normalizeCampaignId(remote.campaignId) in ownedCampaignIds
            if ((migrated != remote || remote.hasRedundantLegacyProjections()) && canWriteRemote) {
                val reference = collection.document(remote.id)
                return@map store.runTransaction { transaction ->
                    val snapshot = transaction.get(reference)
                    val latest = snapshot.toObject(CharacterRecord::class.java)?.copy(id = reference.id)
                        ?: return@runTransaction migrated
                    val optimized = latest.optimizedForFirestore(migrationManager)
                    val comparable = latest.copy(
                        dirty = false, lastSyncedAt = 0,
                        protections = optimized.protections, notes = optimized.notes,
                        bodyRegions = optimized.bodyRegions, organs = optimized.organs, conditions = optimized.conditions,
                    )
                    val removable = buildSet {
                        add("dirty")
                        if (optimized.protections.isEmpty()) add("protections")
                        if (optimized.notes.isBlank()) add("notes")
                        if (optimized.bodyRegions.isEmpty()) add("bodyRegions")
                        if (optimized.organs.isEmpty()) add("organs")
                        if (optimized.conditions.isEmpty()) add("conditions")
                    }
                    val hasRemovable = snapshot.data.orEmpty().keys.any { it in removable }
                    if (optimized == comparable && !hasRemovable) return@runTransaction latest
                    val before = latest.toDomain()
                    val after = optimized.toDomain()
                    val committed = commitCharacterEditKeys(before, after, characterEditKeys(before, after)).toRecord()
                    transaction.set(reference, committed.optimizedForFirestore(migrationManager))
                    committed
                }.await()
            }
            migrated
        }
        val remoteIds = remoteById.keys

        val dirtyRecords = dao.dirty().map { record ->
            val migrated = migrationManager.migrate(record, markDirty = true)
            if (migrated != record) dao.upsertTracked(migrated)
            migrated
        }.filter { record ->
            record.ownerId == session.uid ||
                normalizeCampaignId(record.campaignId) in ownedCampaignIds ||
                session.isAdmin
        }
        val dirtyIds = dirtyRecords.mapTo(mutableSetOf(), CharacterRecord::id)

        remoteRecords.filter { it.id !in dirtyIds }.forEach { remote ->
            dao.upsertTracked(
                remote.copy(
                    campaignId = normalizeCampaignId(remote.campaignId),
                    dirty = false,
                    lastSyncedAt = remote.updatedAt,
                    deleted = false,
                ),
            )
        }
        (if (session.isAdmin) dao.all() else dao.visible(session.uid))
            .filter {
                !it.dirty && it.id !in remoteIds && it.id !in dirtyIds
            }
            .forEach { dao.purge(it.id) }

        dirtyRecords.forEach { record ->
            if (record.deleted) {
                val remote = remoteById[record.id]
                if (remote == null) {
                    dao.purge(record.id)
                } else {
                    try {
                        collection.document(record.id).delete().await()
                        dao.purge(record.id)
                    } catch (error: Throwable) {
                        // Keep the pending local deletion. Losing permission must never discard
                        // a user change that can be retried or resolved explicitly later.
                        throw error
                    }
                }
            } else {
                val normalizedRecord = record.copy(campaignId = normalizeCampaignId(record.campaignId))
                val reference = collection.document(record.id)
                val result = store.runTransaction<CharacterSyncWrite> { transaction ->
                    val snapshot = transaction.get(reference)
                    val rawRemote = snapshot.toObject(CharacterRecord::class.java)?.copy(id = reference.id)
                    val remote = rawRemote?.let { migrationManager.migrate(it, markDirty = false) }
                    if (remote != null) {
                        val localCharacter = normalizedRecord.toDomain()
                        val remoteCharacter = remote.toDomain()
                        if (needsLegacySyncReview(localCharacter, remoteCharacter)) {
                            return@runTransaction CharacterSyncWrite.LegacyReview(CharacterSyncConflict(localCharacter, remoteCharacter, remote.updatedAt))
                        }
                        val keys = pendingCharacterEditKeys(localCharacter, remoteCharacter)
                        if (keys.isEmpty() && !rawRemote.hasRedundantLegacyProjections()) {
                            return@runTransaction CharacterSyncWrite.Accepted(remote)
                        }
                        val merged = commitCharacterEditKeys(remoteCharacter, localCharacter, keys)
                        val mergedRecord = merged.toRecord()
                        transaction.set(reference, mergedRecord.optimizedForFirestore(migrationManager))
                        return@runTransaction CharacterSyncWrite.Accepted(mergedRecord)

                    }
                    check(record.lastSyncedAt == 0L) { "The remote sheet was deleted; stale edits cannot recreate it." }
                    val local = normalizedRecord.toDomain()
                    val empty = Character(id = local.id, updatedAt = 0, dirty = false)
                    val initial = commitCharacterEditKeys(empty, local, characterEditKeys(empty, local)).toRecord()
                    transaction.set(reference, initial.optimizedForFirestore(migrationManager))
                    CharacterSyncWrite.Accepted(initial)
                }.await()
                when (result) {
                    is CharacterSyncWrite.Accepted -> dao.acceptSnapshot(result.record, record.updatedAt, record)
                    is CharacterSyncWrite.LegacyReview -> conflicts += result.conflict
                }
            }
        }
        conflicts
    }

    override suspend fun resolveConflict(
        session: UserSession,
        conflict: CharacterSyncConflict,
        remoteFieldIds: Set<String>,
    ) = syncMutex.withLock {
        val merged = mergeCharacterConflict(conflict.local, conflict.remote, remoteFieldIds)
        if (characterConflictFields(merged, conflict.remote).isEmpty()) {
            dao.acceptSnapshot(
                conflict.remote.copy(
                    campaignId = normalizeCampaignId(conflict.remote.campaignId),
                    dirty = false,
                    lastSyncedAt = conflict.remoteUpdatedAt,
                    deleted = false,
                ).toRecord(),
                conflict.local.updatedAt,
                conflict.local.toRecord(),
            )
            return@withLock
        }

        check(CharacterAccessPolicy.canEdit(session, conflict.remote, isCampaignHistorian(session, conflict.remote))) {
            "A versão online está bloqueada. Para continuar, escolha todos os valores online."
        }
        dao.acceptSnapshot(conflict.remote.toRecord(), conflict.local.updatedAt, conflict.local.toRecord())
        dao.saveEditorChange(
            merged.withPendingEditKeys(characterEditKeys(conflict.remote, merged)).copy(
                campaignId = normalizeCampaignId(merged.campaignId),
                updatedAt = System.currentTimeMillis(),
                lastEditedBy = session.uid,
                dirty = true,
                lastSyncedAt = conflict.remoteUpdatedAt,
                deleted = false,
                editingBase = conflict.remote,
            ),
        )
    }

    private suspend fun isCampaignHistorian(session: UserSession, character: Character): Boolean {
        if (session.isAdmin) return true
        val campaignId = normalizeCampaignId(character.campaignId)
        if (campaignId.isBlank()) return false
        return campaignDao.campaign(campaignId)?.ownerId == session.uid
    }

    private suspend fun isCampaignResponsible(session: UserSession, character: Character): Boolean {
        if (session.isAdmin) return true
        val campaignId = normalizeCampaignId(character.campaignId)
        return campaignId.isNotBlank() && campaignDao.campaign(campaignId)?.ownerId == session.uid
    }
}
