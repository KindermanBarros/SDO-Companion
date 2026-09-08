package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.data.local.CharacterDao
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.CampaignDao
import com.kinderman.sdo.data.local.OwnerDao
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.domain.model.Character
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

class OfflineFirstCharacterRepository(
    private val dao: CharacterDao,
    private val ownerDao: OwnerDao,
    private val campaignDao: CampaignDao,
) : CharacterRepository {
    private val syncMutex = Mutex()

    override fun observe(session: UserSession): Flow<List<Character>> =
        (if (session.isAdmin) dao.observeAll() else dao.observe(session.uid))
            .map { records -> records.map(CharacterRecord::toDomain) }

    override fun observeOne(id: String): Flow<Character?> = dao.observeOne(id).map { it?.toDomain() }

    override suspend fun create(session: UserSession): Character =
        Character(ownerId = session.uid, campaignId = "").also { dao.upsert(it.toRecord()) }

    override suspend fun save(session: UserSession, character: Character) {
        check(CharacterAccessPolicy.canEdit(session, character, isCampaignHistorian(session, character))) {
            "Você não pode editar esta ficha."
        }
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
        check(CharacterAccessPolicy.canDelete(session, character, isCampaignHistorian(session, character))) {
            "Esta ficha não pode ser removida."
        }
        dao.upsert(character.copy(deleted = true, updatedAt = System.currentTimeMillis(), dirty = true).toRecord())
    }

    override suspend fun sync(session: UserSession): List<CharacterSyncConflict> = syncMutex.withLock {
        val store = runCatching { Firebase.firestore }.getOrNull() ?: return@withLock emptyList()
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

        var campaignScopeComplete = true
        val membershipDocuments = if (session.isAdmin) emptyList() else runCatching {
            members.whereEqualTo("userId", session.uid).get().await().documents
        }.getOrElse {
            campaignScopeComplete = false
            emptyList()
        }
        val memberCampaignIds = membershipDocuments.mapNotNull { document ->
                val campaignId = document.getString("campaignId").orEmpty()
                val state = document.getString("state").orEmpty()
                campaignId.takeIf { it.isNotBlank() && state == "ACTIVE" }
            }
            .toMutableSet()
        val historianCampaignIds = membershipDocuments.mapNotNullTo(mutableSetOf()) { document ->
            val role = document.getString("role").orEmpty()
            document.getString("campaignId")?.takeIf {
                document.getString("state") == "ACTIVE" && (role == "HISTORIAN" || role == "MASTER")
            }
        }
        val ownedCampaignIds = if (session.isAdmin) mutableSetOf() else runCatching {
            campaigns.whereEqualTo("ownerId", session.uid).get().await().documents.mapTo(mutableSetOf()) { it.id }
        }.getOrElse {
            campaignScopeComplete = false
            mutableSetOf()
        }
        val authorizedCampaignIds = (memberCampaignIds + ownedCampaignIds).toSet()
        authorizedCampaignIds.forEach { campaignId ->
            runCatching {
                collection.whereEqualTo("campaignId", campaignId).get().await().documents
            }.onSuccess { documents ->
                documents.forEach { document ->
                    document.toObject(CharacterRecord::class.java)?.copy(id = document.id)?.let { remoteById[it.id] = it }
                }
            }.onFailure {
                campaignScopeComplete = false
            }
        }
        val remoteRecords = remoteById.values.toList()
        val remoteIds = remoteById.keys

        val dirtyRecords = dao.dirty().filter { record ->
            record.ownerId == session.uid ||
                normalizeCampaignId(record.campaignId) in ownedCampaignIds ||
                normalizeCampaignId(record.campaignId) in historianCampaignIds ||
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
                !it.dirty && it.id !in remoteIds && it.id !in dirtyIds &&
                    (it.ownerId == session.uid || campaignScopeComplete)
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
        if (campaignDao.campaign(campaignId)?.ownerId == session.uid) return true
        val member = campaignDao.member(campaignId, session.uid) ?: return false
        val role = if (member.role == "MASTER") CampaignRole.HISTORIAN.name else member.role
        return member.state == CampaignMemberState.ACTIVE.name && role == CampaignRole.HISTORIAN.name
    }

    private suspend fun isCampaignResponsible(session: UserSession, character: Character): Boolean {
        if (session.isAdmin) return true
        val campaignId = normalizeCampaignId(character.campaignId)
        return campaignId.isNotBlank() && campaignDao.campaign(campaignId)?.ownerId == session.uid
    }
}
