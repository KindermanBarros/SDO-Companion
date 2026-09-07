package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.data.local.CharacterDao
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.policy.CharacterAccessPolicy
import com.kinderman.sdo.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OfflineFirstCharacterRepository(
    private val dao: CharacterDao,
) : CharacterRepository {
    override fun observe(session: UserSession): Flow<List<Character>> =
        dao.observe(session.uid, session.isMaster).map { records -> records.map(CharacterRecord::toDomain) }

    override fun observeOne(id: String): Flow<Character?> = dao.observeOne(id).map { it?.toDomain() }

    override suspend fun create(session: UserSession): Character =
        Character(ownerId = session.uid).also { dao.upsert(it.toRecord()) }

    override suspend fun save(session: UserSession, character: Character) {
        check(CharacterAccessPolicy.canEdit(session, character)) { "Você não pode editar esta ficha." }
        dao.upsert(character.copy(updatedAt = System.currentTimeMillis(), dirty = true).toRecord())
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

    override suspend fun sync(session: UserSession) {
        val store = runCatching { Firebase.firestore }.getOrNull() ?: return
        val collection = store.collection("characters")

        dao.dirty()
            .filter { session.isMaster || it.ownerId == session.uid }
            .forEach { record ->
                if (record.deleted) {
                    collection.document(record.id).delete().await()
                    dao.purge(record.id)
                } else {
                    collection.document(record.id).set(record.copy(dirty = false)).await()
                    dao.markSynced(record.id)
                }
            }

        val snapshot = if (session.isMaster) {
            collection.get().await()
        } else {
            collection.whereEqualTo("ownerId", session.uid).get().await()
        }
        val remoteRecords = snapshot.documents.mapNotNull { it.toObject(CharacterRecord::class.java) }
        val remoteIds = remoteRecords.mapTo(mutableSetOf()) { it.id }
        remoteRecords.forEach { dao.upsert(it.copy(dirty = false, deleted = false)) }
        dao.visible(session.uid, session.isMaster)
            .filter { !it.dirty && it.id !in remoteIds }
            .forEach { dao.purge(it.id) }
    }
}
