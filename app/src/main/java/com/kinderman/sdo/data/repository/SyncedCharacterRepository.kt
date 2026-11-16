package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.TransactionOptions
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.data.local.CharacterDao
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.CampaignDao
import com.kinderman.sdo.data.local.OwnerDao
import com.kinderman.sdo.data.local.firestoreCharacterPatch
import com.kinderman.sdo.data.local.hasRedundantLegacyProjections
import com.kinderman.sdo.data.local.optimizedForFirestore
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.data.local.CharacterMigrationManager
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.needsLegacySyncReview
import com.kinderman.sdo.domain.model.resolveConcurrentEditKeys
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
import com.kinderman.sdo.domain.repository.RemoteSyncEvent
import kotlinx.coroutines.launch
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
    private val hybrid: com.kinderman.sdo.data.sync.HybridSheetSync? = null,
) : CharacterRepository {
    private val syncMutex = Mutex()
    private val selfCommittedVersions = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Long>>()
    override fun observeSyncStatus(session: UserSession): Flow<com.kinderman.sdo.domain.model.SheetSyncStatus> {
        val baseFlow = hybrid?.status(session) ?: kotlinx.coroutines.flow.flowOf(com.kinderman.sdo.domain.model.SheetSyncStatus())
        val localFlow = (if (session.isAdmin) dao.observeAll() else dao.observe(session.uid))
        return kotlinx.coroutines.flow.combine(baseFlow, localFlow) { status, records ->
            val dirtyCount = records.count { it.dirty }
            status.copy(pendingCount = status.pendingCount + dirtyCount)
        }
    }
    override fun observeOfflineEditableIds(session: UserSession): Flow<Set<String>> {
        val routeFlow = hybrid?.editableIds(session) ?: kotlinx.coroutines.flow.flowOf(emptySet())
        val localFlow = (if (session.isAdmin) dao.observeAll() else dao.observe(session.uid))
        return kotlinx.coroutines.flow.combine(routeFlow, localFlow) { routeIds, records ->
            routeIds + records.mapTo(mutableSetOf()) { it.id }
        }
    }
    override suspend fun supportsOfflineEditing(characterId: String) =
        dao.get(characterId) != null || hybrid?.supportsOffline(characterId) == true
    override suspend fun setFieldPresence(session: UserSession, characterId: String, fieldKey: String?, active: Boolean) { hybrid?.presence(session, characterId, fieldKey, active) }
    override fun observeFieldPresence(session: UserSession, characterId: String) = hybrid?.presence(session, characterId) ?: kotlinx.coroutines.flow.flowOf(emptyMap())

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

    override fun observeRemoteChanges(session: UserSession, campaignIds: Set<String>): Flow<RemoteSyncEvent> = callbackFlow {
        val store = Firebase.firestore
        val registrations = mutableListOf<ListenerRegistration>()
        val realtimeJob = hybrid?.let { engine ->
            launch {
                engine.observe(session).collect { sheetId ->
                    trySend(RemoteSyncEvent(fromCloud = true, characterIds = setOf(sheetId), fullSync = false))
                }
            }
        }

        fun listenCharacters(query: Query) {
            val initialized = AtomicBoolean(false)
            registrations += query.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (snapshot.metadata.hasPendingWrites() || snapshot.metadata.isFromCache) {
                    initialized.compareAndSet(false, true)
                    return@addSnapshotListener
                }
                if (!initialized.compareAndSet(false, true)) {
                    val changes = snapshot.documentChanges
                    if (changes.isEmpty()) return@addSnapshotListener
                    launch {
                        val cloudIds = mutableSetOf<String>()
                        for (change in changes) {
                            val doc = change.document
                            val docId = doc.id
                            if (change.type == DocumentChange.Type.REMOVED) {
                                val local = dao.get(docId)
                                if (local != null && !local.deleted) cloudIds += docId
                                continue
                            }
                            val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
                            val remoteFieldMeta = doc.get("fieldUpdatedAt") as? Map<*, *>
                            val remoteRev = (remoteFieldMeta?.get("@revision") as? Number)?.toLong() ?: 0L
                            if (selfCommittedVersions[docId] == (remoteUpdatedAt to remoteRev)) continue
                            val local = dao.get(docId)
                            if (local == null) {
                                if (doc.getBoolean("deleted") != true) cloudIds += docId
                            } else if (!local.dirty) {
                                val localRev = local.fieldUpdatedAt["@revision"] ?: 0L
                                if (remoteRev > localRev || (remoteRev == localRev && remoteUpdatedAt > local.updatedAt)) {
                                    cloudIds += docId
                                }
                            }
                        }
                        if (cloudIds.isNotEmpty()) {
                            trySend(RemoteSyncEvent(fromCloud = true, characterIds = cloudIds, fullSync = false))
                        }
                    }
                }
            }
        }

        fun listenCollection(query: Query) {
            val initialized = AtomicBoolean(false)
            registrations += query.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (snapshot.metadata.hasPendingWrites() || snapshot.metadata.isFromCache) {
                    initialized.compareAndSet(false, true)
                    return@addSnapshotListener
                }
                if (!initialized.compareAndSet(false, true) && snapshot.documentChanges.isNotEmpty()) {
                    trySend(RemoteSyncEvent(fromCloud = true, fullSync = true))
                }
            }
        }

        val characters = store.collection(FirestoreCollections.CHARACTERS)
        listenCharacters(if (session.isAdmin) characters else characters.whereEqualTo("ownerId", session.uid))
        listenCollection(store.collection(FirestoreCollections.CAMPAIGN_MEMBERS).whereEqualTo("userId", session.uid))
        listenCollection(store.collection(FirestoreCollections.CAMPAIGN_DELIVERIES).whereEqualTo("recipientId", session.uid))
        campaignIds.filter(String::isNotBlank).distinct().forEach { campaignId ->
            listenCharacters(characters.whereEqualTo("campaignId", campaignId))
            listenCollection(store.collection(FirestoreCollections.CAMPAIGN_LIBRARY).whereEqualTo("campaignId", campaignId))
            listenCollection(store.collection(FirestoreCollections.CAMPAIGN_AUDIT).whereEqualTo("campaignId", campaignId))
        }

        awaitClose { realtimeJob?.cancel(); registrations.forEach(ListenerRegistration::remove) }
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
        if (hybrid?.isRtdb(session.uid, character.id) == true) { hybrid.save(session, character); return }
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
        dao.get(character.id)?.let { saved ->
            val cid = normalizeCampaignId(saved.campaignId)
            val campaignOwner = if (cid.isBlank()) null else campaignDao.campaign(cid)?.ownerId
            hybrid?.mirrorCharacter(saved, campaignOwner)
        }
    }

    override suspend fun setPlayerLocked(session: UserSession, character: Character, locked: Boolean) {
        if (hybrid?.isRtdb(session.uid, character.id) == true) { hybrid.control(session, character, "SET_LOCK", mapOf("lockType" to if (locked) "PLAYER" else "NONE")); return }
        check(CharacterAccessPolicy.canChangePlayerLock(session, character, isCampaignHistorian(session, character))) {
            "O bloqueio do jogador só pode ser alterado pelo dono e não substitui o bloqueio do historiador."
        }
        saveLock(character, if (locked) CharacterLock.PLAYER else CharacterLock.NONE, session.uid)
    }

    override suspend fun setHistorianLocked(session: UserSession, character: Character, locked: Boolean) {
        if (hybrid?.isRtdb(session.uid, character.id) == true) { hybrid.control(session, character, "SET_LOCK", mapOf("lockType" to if (locked) "HISTORIAN" else "NONE")); return }
        check(CharacterAccessPolicy.canChangeHistorianLock(session, isCampaignHistorian(session, character))) {
            "Somente o historiador pode alterar o bloqueio real."
        }
        saveLock(character, if (locked) CharacterLock.HISTORIAN else CharacterLock.NONE, session.uid)
    }

    override suspend fun transferOwnership(session: UserSession, character: Character, owner: UserProfile) {
        if (hybrid?.isRtdb(session.uid, character.id) == true) { hybrid.control(session, character, "TRANSFER_OWNERSHIP", mapOf("ownerId" to owner.uid)); return }
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
        if (hybrid?.isRtdb(session.uid, character.id) == true) { hybrid.control(session, character, "DELETE", emptyMap(), character.copy(deleted = true)); return }
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
        val realtimeIds = remoteById.keys.filter { hybrid?.isRtdb(session.uid, it) == true }.toSet()
        val remoteRecords = remoteById.values.filter { it.id !in realtimeIds }.map { remote ->
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

        val dirtyRecords = dao.dirty().filter { hybrid?.isRtdb(session.uid, it.id) != true }.map { record ->
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
                !it.dirty && it.id !in remoteIds && it.id !in dirtyIds && hybrid?.supportsOffline(it.id) != true
            }
            .forEach {
                val cid = normalizeCampaignId(it.campaignId)
                val campaignOwner = if (cid.isBlank()) null else campaignDao.campaign(cid)?.ownerId
                hybrid?.removeCharacterMirror(it.id, it.ownerId, campaignOwner)
                dao.purge(it.id)
            }

        var firstDirtyError: Throwable? = null
        dirtyRecords.forEach { record ->
            runCatching {
                pushDirtyRecord(
                    session = session,
                    record = record,
                    remoteExistsHint = remoteById[record.id] != null,
                    collection = collection,
                    store = store,
                )
            }.onSuccess { conflict ->
                if (conflict != null) conflicts += conflict
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                android.util.Log.e("SDO_SYNC", "Failed syncing character ${record.id}", error)
                if (firstDirtyError == null) firstDirtyError = error
            }
        }
        firstDirtyError?.let { if (conflicts.isEmpty()) throw it }
        conflicts
    }

    override suspend fun syncCharacter(session: UserSession, characterId: String): List<CharacterSyncConflict> = syncMutex.withLock {
        if (characterId.isBlank() || hybrid?.isRtdb(session.uid, characterId) == true) return@withLock emptyList()
        val store = Firebase.firestore
        val collection = store.collection(FirestoreCollections.CHARACTERS)
        val local = dao.get(characterId) ?: return@withLock emptyList()
        if (local.dirty) {
            val migrated = migrationManager.migrate(local, markDirty = true)
            if (migrated != local) dao.upsertTracked(migrated)
            val conflict = pushDirtyRecord(
                session = session,
                record = migrated,
                remoteExistsHint = null,
                collection = collection,
                store = store,
            )
            return@withLock listOfNotNull(conflict)
        }
        val snapshot = collection.document(characterId).get().await()
        val remote = snapshot.toObject(CharacterRecord::class.java)?.copy(id = snapshot.id)
        if (remote == null) {
            val cid = normalizeCampaignId(local.campaignId)
            val campaignOwner = if (cid.isBlank()) null else campaignDao.campaign(cid)?.ownerId
            hybrid?.removeCharacterMirror(local.id, local.ownerId, campaignOwner)
            dao.purge(local.id)
            return@withLock emptyList()
        }
        val migrated = migrationManager.migrate(remote, markDirty = false)
        if (migrated.updatedAt > local.updatedAt) {
            dao.upsertTracked(
                migrated.copy(
                    campaignId = normalizeCampaignId(migrated.campaignId),
                    dirty = false,
                    lastSyncedAt = migrated.updatedAt,
                    deleted = false,
                ),
            )
        }
        emptyList()
    }

    private suspend fun pushDirtyRecord(
        session: UserSession,
        record: CharacterRecord,
        remoteExistsHint: Boolean?,
        collection: com.google.firebase.firestore.CollectionReference,
        store: com.google.firebase.firestore.FirebaseFirestore,
    ): CharacterSyncConflict? {
        val cid = normalizeCampaignId(record.campaignId)
        val campaignOwner = if (cid.isBlank()) null else campaignDao.campaign(cid)?.ownerId
        if (record.deleted) {
            val shouldDeleteRemote = remoteExistsHint ?: runCatching {
                collection.document(record.id).get().await().exists()
            }.getOrDefault(true)
            if (shouldDeleteRemote) {
                try {
                    collection.document(record.id).delete().await()
                } catch (error: FirebaseFirestoreException) {
                    if (error.code != FirebaseFirestoreException.Code.NOT_FOUND) throw error
                }
            }
            hybrid?.removeCharacterMirror(record.id, record.ownerId, campaignOwner)
            dao.purge(record.id)
            return null
        }

        val normalizedRecord = record.copy(campaignId = cid)
        val reference = collection.document(record.id)
        val txOptions = TransactionOptions.Builder().setMaxAttempts(2).build()
        val result = try {
            store.runTransaction(txOptions) { transaction ->
                val snapshot = transaction.get(reference)
                val rawRemote = snapshot.toObject(CharacterRecord::class.java)?.copy(id = reference.id)
                val remote = rawRemote?.let { migrationManager.migrate(it, markDirty = false) }
                if (remote != null) {
                    val localCharacter = normalizedRecord.toDomain()
                    val remoteCharacter = remote.toDomain()
                    if (needsLegacySyncReview(localCharacter, remoteCharacter)) {
                        return@runTransaction CharacterSyncWrite.LegacyReview(
                            CharacterSyncConflict(localCharacter, remoteCharacter, remote.updatedAt),
                        )
                    }
                    val keys = pendingCharacterEditKeys(localCharacter, remoteCharacter)
                    if (keys.isEmpty() && !rawRemote.hasRedundantLegacyProjections()) {
                        return@runTransaction CharacterSyncWrite.Accepted(remote)
                    }
                    val effectiveKeys = resolveConcurrentEditKeys(localCharacter, remoteCharacter, keys)
                    if (effectiveKeys.isEmpty() && !rawRemote.hasRedundantLegacyProjections()) {
                        return@runTransaction CharacterSyncWrite.Accepted(remote)
                    }
                    val merged = commitCharacterEditKeys(remoteCharacter, localCharacter, effectiveKeys)
                    val mergedRecord = merged.toRecord()
                    val optimized = mergedRecord.optimizedForFirestore(migrationManager)
                    val patch = firestoreCharacterPatch(
                        before = rawRemote,
                        after = optimized,
                        existingDocumentKeys = snapshot.data.orEmpty().keys,
                        deleteValue = FieldValue.delete(),
                    )
                    if (patch.isNotEmpty()) {
                        transaction.update(reference, patch)
                    }
                    return@runTransaction CharacterSyncWrite.Accepted(mergedRecord)
                }
                check(record.lastSyncedAt == 0L) { "The remote sheet was deleted; stale edits cannot recreate it." }
                val local = normalizedRecord.toDomain()
                val empty = Character(id = local.id, updatedAt = 0, dirty = false)
                val initial = commitCharacterEditKeys(empty, local, characterEditKeys(empty, local)).toRecord()
                transaction.set(reference, initial.optimizedForFirestore(migrationManager))
                CharacterSyncWrite.Accepted(initial)
            }.await()
        } catch (error: FirebaseFirestoreException) {
            if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                error.code == FirebaseFirestoreException.Code.ABORTED
            ) {
                val latestRemote = runCatching {
                    reference.get().await().toObject(CharacterRecord::class.java)?.copy(id = reference.id)
                }.getOrNull()?.let { migrationManager.migrate(it, markDirty = false) }
                if (latestRemote != null) {
                    val localCharacter = normalizedRecord.toDomain()
                    val remoteCharacter = latestRemote.toDomain()
                    if (characterConflictFields(localCharacter, remoteCharacter).isNotEmpty()) {
                        return CharacterSyncConflict(localCharacter, remoteCharacter, latestRemote.updatedAt)
                    }
                }
            }
            throw error
        }

        return when (result) {
            is CharacterSyncWrite.Accepted -> {
                selfCommittedVersions[record.id] = result.record.updatedAt to (result.record.fieldUpdatedAt["@revision"] ?: 0L)
                dao.acceptSnapshot(result.record, record.updatedAt, record)
                val resultCid = normalizeCampaignId(result.record.campaignId)
                val resultCampaignOwner = if (resultCid.isBlank()) null else campaignDao.campaign(resultCid)?.ownerId
                hybrid?.mirrorCharacter(result.record, resultCampaignOwner)
                null
            }
            is CharacterSyncWrite.LegacyReview -> result.conflict
        }
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
