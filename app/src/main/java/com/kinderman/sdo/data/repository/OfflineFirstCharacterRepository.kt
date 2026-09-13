package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.data.local.CharacterDao
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.CampaignDao
import com.kinderman.sdo.data.local.OwnerDao
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.data.local.migratedStructuredRecord
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CANONICAL_SCHEMA_VERSION
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

private sealed interface CharacterSyncWrite {
    data class Saved(val updatedAt: Long) : CharacterSyncWrite
    data class Conflict(val value: CharacterSyncConflict) : CharacterSyncWrite
}

class OfflineFirstCharacterRepository(
    private val dao: CharacterDao,
    private val ownerDao: OwnerDao,
    private val campaignDao: CampaignDao,
) : CharacterRepository {
    private val syncMutex = Mutex()

    override fun observe(session: UserSession): Flow<List<Character>> =
        (if (session.isAdmin) dao.observeAll() else dao.observe(session.uid))
            .map { records ->
                records.map { record ->
                    val migrated = record.migratedStructuredRecord(markDirty = true)
                    if (migrated != record) dao.upsert(migrated)
                    migrated.toDomain()
                }
            }

    override fun observeOne(id: String): Flow<Character?> = dao.observeOne(id).map { record ->
        record?.let {
            val migrated = it.migratedStructuredRecord(markDirty = true)
            if (migrated != it) dao.upsert(migrated)
            migrated.toDomain()
        }
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
        val character = Character(ownerId = ownerId, campaignId = normalizedCampaignId)
        dao.upsert(character.toRecord())
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
        check(CharacterAccessPolicy.canEdit(session, character, isCampaignHistorian(session, character))) {
            "Você não pode editar esta ficha."
        }
        if (character.canonicalSchemaVersion != CANONICAL_SCHEMA_VERSION) throw DomainError.LegacyWriteRejected()
        dao.upsert(
            character.copy(
                campaignId = normalizeCampaignId(character.campaignId),
                updatedAt = System.currentTimeMillis(),
                dirty = true,
            ).toRecord(),
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
        if (character.canonicalSchemaVersion != CANONICAL_SCHEMA_VERSION) throw DomainError.LegacyWriteRejected()
        dao.upsert(
            character.copy(
                ownerId = owner.uid,
                updatedAt = System.currentTimeMillis(),
                dirty = true,
            ).toRecord(),
        )
    }

    private suspend fun saveLock(character: Character, lockType: CharacterLock, actorId: String) {
        if (character.canonicalSchemaVersion != CANONICAL_SCHEMA_VERSION) throw DomainError.LegacyWriteRejected()
        val locked = lockType != CharacterLock.NONE
        dao.upsert(
            character.copy(
                lockType = lockType,
                lockedBy = if (locked) actorId else "",
                lockedAt = if (locked) System.currentTimeMillis() else null,
                updatedAt = System.currentTimeMillis(),
                dirty = true,
            ).toRecord(),
        )
    }

    override suspend fun delete(session: UserSession, character: Character) {
        check(CharacterAccessPolicy.canDelete(session, character, isCampaignHistorian(session, character))) {
            "Esta ficha não pode ser removida."
        }
        dao.upsert(character.copy(deleted = true, updatedAt = System.currentTimeMillis(), dirty = true).toRecord())
    }

    override suspend fun sync(session: UserSession): List<CharacterSyncConflict> = syncMutex.withLock {
        val store = Firebase.firestore
        val collection = store.collection("characters")
        val campaigns = store.collection("campaigns")
        val members = store.collection("campaignMembers")
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

        val membershipDocuments = if (session.isAdmin) emptyList()
            else members.whereEqualTo("userId", session.uid).get().await().documents
        val memberCampaignIds = membershipDocuments.mapNotNull { document ->
                val campaignId = document.getString("campaignId").orEmpty()
                val state = document.getString("state").orEmpty()
                campaignId.takeIf { it.isNotBlank() && state == "ACTIVE" }
            }
            .toMutableSet()
        val ownedCampaignIds = if (session.isAdmin) mutableSetOf()
            else campaigns.whereEqualTo("ownerId", session.uid).get().await().documents.mapTo(mutableSetOf()) { it.id }
        val authorizedCampaignIds = (memberCampaignIds + ownedCampaignIds).toSet()
        authorizedCampaignIds.forEach { campaignId ->
            collection.whereEqualTo("campaignId", campaignId).get().await().documents.forEach { document ->
                document.toObject(CharacterRecord::class.java)?.copy(id = document.id)?.let { remoteById[it.id] = it }
            }
        }
        val remoteRecords = remoteById.values.map { remote ->
            val migrated = remote.migratedStructuredRecord(markDirty = false)
            val canWriteRemote = session.isAdmin || remote.ownerId == session.uid ||
                normalizeCampaignId(remote.campaignId) in ownedCampaignIds
            if (migrated != remote && canWriteRemote) {
                val reference = collection.document(remote.id)
                store.runTransaction { transaction ->
                    val latest = transaction.get(reference).toObject(CharacterRecord::class.java)
                        ?.copy(id = reference.id)
                        ?: return@runTransaction
                    val latestMigrated = latest.migratedStructuredRecord(markDirty = false)
                    if (latestMigrated != latest) transaction.set(reference, latestMigrated.copy(dirty = false))
                    Unit
                }.await()
            }
            migrated
        }
        val remoteIds = remoteById.keys

        val dirtyRecords = dao.dirty().map { record ->
            val migrated = record.migratedStructuredRecord(markDirty = true)
            if (migrated != record) dao.upsert(migrated)
            migrated
        }.filter { record ->
            record.ownerId == session.uid ||
                normalizeCampaignId(record.campaignId) in ownedCampaignIds ||
                session.isAdmin
        }
        val dirtyIds = dirtyRecords.mapTo(mutableSetOf(), CharacterRecord::id)

        remoteRecords.filter { it.id !in dirtyIds }.forEach { remote ->
            dao.upsert(
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
                val result = store.runTransaction { transaction ->
                    val snapshot = transaction.get(reference)
                    val remote = snapshot.toObject(CharacterRecord::class.java)?.copy(id = reference.id)
                        ?.migratedStructuredRecord(markDirty = false)
                    if (remote != null && remote.updatedAt > normalizedRecord.lastSyncedAt) {
                        val localCharacter = normalizedRecord.toDomain()
                        val remoteCharacter = remote.toDomain()
                        val fields = characterConflictFields(localCharacter, remoteCharacter)
                        if (fields.isNotEmpty()) {
                            return@runTransaction CharacterSyncWrite.Conflict(
                                CharacterSyncConflict(
                                    local = localCharacter,
                                    remote = remoteCharacter,
                                    remoteUpdatedAt = remote.updatedAt,
                                    fields = fields,
                                ),
                            )
                        }
                    }
                    val writeTimestamp = maxOf(
                        System.currentTimeMillis(),
                        normalizedRecord.updatedAt,
                        normalizedRecord.lastSyncedAt + 1,
                    )
                    transaction.set(reference, normalizedRecord.copy(updatedAt = writeTimestamp, dirty = false))
                    CharacterSyncWrite.Saved(writeTimestamp)
                }.await()
                when (result) {
                    is CharacterSyncWrite.Conflict -> conflicts += result.value
                    is CharacterSyncWrite.Saved -> dao.markSynced(record.id, result.updatedAt)
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
            dao.upsert(
                conflict.remote.copy(
                    campaignId = normalizeCampaignId(conflict.remote.campaignId),
                    dirty = false,
                    lastSyncedAt = conflict.remoteUpdatedAt,
                    deleted = false,
                ).toRecord(),
            )
            return@withLock
        }

        check(CharacterAccessPolicy.canEdit(session, conflict.remote, isCampaignHistorian(session, conflict.remote))) {
            "A versão online está bloqueada. Para continuar, escolha todos os valores online."
        }
        dao.upsert(
            merged.copy(
                campaignId = normalizeCampaignId(merged.campaignId),
                updatedAt = System.currentTimeMillis(),
                dirty = true,
                lastSyncedAt = conflict.remoteUpdatedAt,
                deleted = false,
            ).toRecord(),
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
