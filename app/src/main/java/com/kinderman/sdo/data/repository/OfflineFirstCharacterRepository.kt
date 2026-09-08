package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.data.local.CharacterDao
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.OwnerDao
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserSession
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

class OfflineFirstCharacterRepository(
    private val dao: CharacterDao,
    private val ownerDao: OwnerDao,
) : CharacterRepository {
    private val syncMutex = Mutex()

    override fun observe(session: UserSession): Flow<List<Character>> =
        dao.observe(session.uid).map { records -> records.map(CharacterRecord::toDomain) }

    override fun observeOne(id: String): Flow<Character?> = dao.observeOne(id).map { it?.toDomain() }

    override suspend fun create(session: UserSession): Character =
        Character(ownerId = session.uid, campaignId = "").also { dao.upsert(it.toRecord()) }

    override suspend fun save(session: UserSession, character: Character) {
        check(CharacterAccessPolicy.canEdit(session, character)) { "Você não pode editar esta ficha." }
        dao.upsert(
            character.copy(
                campaignId = normalizeCampaignId(character.campaignId),
                updatedAt = System.currentTimeMillis(),
                dirty = true,
            ).toRecord(),
        )
    }

    override suspend fun setPlayerLocked(session: UserSession, character: Character, locked: Boolean) {
        check(CharacterAccessPolicy.canChangePlayerLock(session, character)) {
            "O bloqueio do jogador só pode ser alterado pelo dono e não substitui o bloqueio do historiador."
        }
        saveLock(character, if (locked) CharacterLock.PLAYER else CharacterLock.NONE, session.uid)
    }

    override suspend fun setHistorianLocked(session: UserSession, character: Character, locked: Boolean) {
        check(CharacterAccessPolicy.canChangeHistorianLock(session)) {
            "Somente o historiador pode alterar o bloqueio real."
        }
        saveLock(character, if (locked) CharacterLock.HISTORIAN else CharacterLock.NONE, session.uid)
    }

    override suspend fun transferOwnership(session: UserSession, character: Character, owner: UserProfile) {
        check(CharacterAccessPolicy.canTransferOwnership(session)) {
            "Somente o historiador pode transferir uma ficha."
        }
        check(owner.uid.isNotBlank()) { "O novo owner é inválido." }
        dao.upsert(
            character.copy(
                ownerId = owner.uid,
                updatedAt = System.currentTimeMillis(),
                dirty = true,
            ).toRecord(),
        )
    }

    private suspend fun saveLock(character: Character, lockType: CharacterLock, actorId: String) {
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
        check(CharacterAccessPolicy.canDelete(session, character)) { "Esta ficha não pode ser removida." }
        dao.upsert(character.copy(deleted = true, updatedAt = System.currentTimeMillis(), dirty = true).toRecord())
    }

    override suspend fun sync(session: UserSession): List<CharacterSyncConflict> = syncMutex.withLock {
        val store = runCatching { Firebase.firestore }.getOrNull() ?: return@withLock emptyList()
        val collection = store.collection("characters")
        val campaigns = store.collection("campaigns")
        val members = store.collection("campaignMembers")
        val conflicts = mutableListOf<CharacterSyncConflict>()

        val memberCampaignIds = members.whereEqualTo("userId", session.uid).get().await().documents
            .mapNotNull { document ->
                val campaignId = document.getString("campaignId").orEmpty()
                val state = document.getString("state").orEmpty()
                campaignId.takeIf { it.isNotBlank() && state == "ACTIVE" }
            }
            .toMutableSet()
        val ownedCampaignIds = campaigns.whereEqualTo("ownerId", session.uid).get().await().documents
            .mapTo(mutableSetOf()) { it.id }
        val authorizedCampaignIds = (memberCampaignIds + ownedCampaignIds).toSet()

        val remoteById = linkedMapOf<String, CharacterRecord>()
        collection.whereEqualTo("ownerId", session.uid).get().await().documents.forEach { document ->
            document.toObject(CharacterRecord::class.java)?.copy(id = document.id)?.let { remoteById[it.id] = it }
        }
        authorizedCampaignIds.forEach { campaignId ->
            collection.whereEqualTo("campaignId", campaignId).get().await().documents.forEach { document ->
                document.toObject(CharacterRecord::class.java)?.copy(id = document.id)?.let { remoteById[it.id] = it }
            }
        }
        val remoteRecords = remoteById.values.toList()
        val remoteIds = remoteById.keys

        val dirtyRecords = dao.dirty().filter { record ->
            record.ownerId == session.uid || normalizeCampaignId(record.campaignId) in ownedCampaignIds
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
        dao.visible(session.uid)
            .filter { !it.dirty && it.id !in remoteIds && it.id !in dirtyIds }
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
                        dao.upsert(
                            remote.copy(
                                campaignId = normalizeCampaignId(remote.campaignId),
                                dirty = false,
                                lastSyncedAt = remote.updatedAt,
                                deleted = false,
                            ),
                        )
                        throw error
                    }
                }
            } else {
                val normalizedRecord = record.copy(campaignId = normalizeCampaignId(record.campaignId))
                val remote = remoteById[record.id]
                if (remote != null && remote.updatedAt > record.lastSyncedAt) {
                    val localCharacter = normalizedRecord.toDomain()
                    val remoteCharacter = remote.toDomain()
                    val fields = characterConflictFields(localCharacter, remoteCharacter)
                    if (fields.isNotEmpty()) {
                        conflicts += CharacterSyncConflict(
                            local = localCharacter,
                            remote = remoteCharacter,
                            remoteUpdatedAt = remote.updatedAt,
                            fields = fields,
                        )
                        return@forEach
                    }
                }
                collection.document(record.id).set(normalizedRecord.copy(dirty = false)).await()
                dao.markSynced(record.id, normalizedRecord.updatedAt)
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

        check(CharacterAccessPolicy.canEdit(session, conflict.remote)) {
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
}
