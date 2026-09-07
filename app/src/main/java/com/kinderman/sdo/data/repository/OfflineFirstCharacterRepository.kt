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
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.UserProfile
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

    override suspend fun transferOwnership(
        session: UserSession,
        character: Character,
        owner: UserProfile,
    ) {
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

    override suspend fun sync(session: UserSession) = syncMutex.withLock {
        val store = runCatching { Firebase.firestore }.getOrNull() ?: return@withLock
        val collection = store.collection("characters")

        val snapshot = if (session.isMaster) {
            collection.get().await()
        } else {
            collection.whereEqualTo("ownerId", session.uid).get().await()
        }
        val remoteRecords = snapshot.documents.mapNotNull { document ->
            document.toObject(CharacterRecord::class.java)?.copy(id = document.id)
        }
        val remoteById = remoteRecords.associateBy(CharacterRecord::id)
        val remoteIds = remoteRecords.mapTo(mutableSetOf()) { it.id }
        val registeredOwnerIds = ownerDao.ids().toSet()
        val dirtyRecords = dao.dirty().filter { record ->
            record.ownerId == session.uid || (
                session.isMaster && (
                    record.id in remoteIds || record.deleted || record.ownerId in registeredOwnerIds
                )
            )
        }
        val dirtyIds = dirtyRecords.mapTo(mutableSetOf(), CharacterRecord::id)

        remoteRecords
            .filter { it.id !in dirtyIds }
            .forEach { dao.upsert(it.copy(dirty = false, deleted = false)) }
        dao.visible(session.uid, session.isMaster)
            .filter { !it.dirty && it.id !in remoteIds && it.id !in dirtyIds }
            .forEach { dao.purge(it.id) }

        dirtyRecords.forEach { record ->
            if (record.deleted) {
                val remote = remoteById[record.id]
                if (remote == null) {
                    // A exclusão já foi sincronizada por outro dispositivo. Não tente apagar
                    // novamente: as regras não conseguem validar a posse de um documento ausente.
                    dao.purge(record.id)
                } else {
                    try {
                        collection.document(record.id).delete().await()
                        dao.purge(record.id)
                    } catch (error: Throwable) {
                        // Restaura o estado autoritativo (por exemplo, um lock aplicado remotamente)
                        // para que a mesma exclusão rejeitada não seja repetida a cada abertura.
                        dao.upsert(remote.copy(dirty = false, deleted = false))
                        throw error
                    }
                }
            } else {
                collection.document(record.id).set(record.copy(dirty = false)).await()
                dao.markSynced(record.id)
            }
        }
    }
}
