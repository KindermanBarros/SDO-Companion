package com.kinderman.sdo.data.sync

import android.content.Context
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.kinderman.sdo.SdoApplication
import com.kinderman.sdo.data.local.AppDatabase
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.SheetOutboxRecord
import com.kinderman.sdo.data.local.SheetRouteRecord
import com.kinderman.sdo.data.local.SheetSequenceRecord
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SheetSyncStatus
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.applyCharacterEditKeys
import com.kinderman.sdo.domain.model.applySessionCommand
import com.kinderman.sdo.domain.model.characterEditKeys
import com.kinderman.sdo.domain.model.serverSyncRevision
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/** Room owns intent; RTDB contains only server-confirmed sheet state. */
class HybridSheetSync(
    private val context: Context,
    private val db: AppDatabase,
    private val scope: CoroutineScope,
    private val database: FirebaseDatabase?,
    private val functions: FirebaseFunctions? = runCatching { FirebaseFunctions.getInstance("us-central1") }.getOrNull(),
) {
    private val dao = db.sheetSyncDao()
    private val characters = db.characterDao()
    private val drainMutex = Mutex()
    private val syncing = MutableStateFlow(false)
    private val errors = MutableStateFlow<String?>(null)
    private val connectionId = UUID.randomUUID().toString()

    fun status(session: UserSession): Flow<SheetSyncStatus> = combine(dao.observeOutbox(session.uid), syncing, errors) { rows, active, error ->
        SheetSyncStatus(rows.count { it.status == "PENDING" }, rows.count { it.status == "REJECTED" }, active, error ?: rows.firstOrNull { it.status == "REJECTED" }?.error)
    }
    fun editableIds(session: UserSession): Flow<Set<String>> = dao.observeRoutes(session.uid).map { rows -> rows.filter { it.status == "RTDB_ACTIVE" }.mapTo(mutableSetOf()) { it.sheetId } }
    suspend fun supportsOffline(sheetId: String): Boolean =
        runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()?.let { dao.route(it, sheetId)?.status == "RTDB_ACTIVE" } == true

    suspend fun isRtdb(accountId: String, sheetId: String): Boolean {
        val local = dao.route(accountId, sheetId)
        if (local?.status == "RTDB_ACTIVE") return true
        val firestore = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return false
        val route = firestore.collection("sheetRoutes").document(sheetId).get().await()
        val status = route.getString("status") ?: "FIRESTORE_ACTIVE"
        dao.upsertRoute((local ?: SheetRouteRecord(accountId, sheetId, status)).copy(status = status, accessEpoch = route.getLong("accessEpoch") ?: 1))
        check(status != "MIGRATING") { "Esta ficha está sendo migrada. Tente novamente em instantes." }
        return status == "RTDB_ACTIVE"
    }

    suspend fun save(session: UserSession, candidate: Character) {
        db.withTransaction {
            val previous = characters.get(candidate.id)?.toDomain() ?: candidate
            val base = candidate.editingBase ?: previous
            val projected = applyCharacterEditKeys(previous, candidate, characterEditKeys(base, candidate))
            val changes = SheetWireCodec.changes(previous.toRecord(), projected.toRecord())
            if (changes.isNotEmpty()) enqueue(session.uid, candidate.id, UUID.randomUUID().toString(), "PATCH", mapOf("base" to SheetWireCodec.state(previous.toRecord()), "changes" to changes), projected.toRecord())
        }
        schedule()
    }

    suspend fun session(session: UserSession, character: Character, command: SessionCommand): Boolean {
        var added = false
        db.withTransaction {
            if (dao.operation(command.idempotencyKey) == null) {
                val current = characters.get(character.id)?.toDomain() ?: character
                val optimistic = current.applySessionCommand(command, session.uid).character
                enqueue(session.uid, character.id, command.idempotencyKey, "SESSION", SheetWireCodec.map(command), optimistic.toRecord())
                added = true
            }
        }
        schedule()
        return added
    }

    suspend fun control(session: UserSession, character: Character, type: String, payload: Map<String, Any?>, optimistic: Character = character) {
        db.withTransaction { enqueue(session.uid, character.id, UUID.randomUUID().toString(), type, payload, optimistic.toRecord()) }
        schedule()
    }

    private suspend fun enqueue(uid: String, sheetId: String, operationId: String, type: String, payload: Map<String, Any?>, optimistic: CharacterRecord) {
        val route = requireNotNull(dao.route(uid, sheetId)) { "A ficha ainda não está disponível para sincronização." }
        check(route.status == "RTDB_ACTIVE")
        val sequence = dao.sequence(uid, sheetId) ?: SheetSequenceRecord(uid, sheetId, UUID.randomUUID().toString(), 1)
        check(sequence.nextSequence < Long.MAX_VALUE)
        val envelope = mapOf("protocolVersion" to 1, "sheetId" to sheetId, "operationId" to operationId, "deviceId" to sequence.deviceId, "sequence" to sequence.nextSequence, "baseRevision" to route.revision, "accessEpoch" to route.accessEpoch, "command" to mapOf("type" to type, "payload" to payload))
        dao.upsertSequence(sequence.copy(nextSequence = sequence.nextSequence + 1))
        dao.upsertOperation(SheetOutboxRecord(operationId, uid, sheetId, sequence.deviceId, sequence.nextSequence, SheetWireCodec.json(envelope), SheetWireCodec.encode(optimistic)))
        characters.upsert(optimistic.copy(dirty = false))
    }

    fun schedule() {
        WorkManager.getInstance(context).enqueueUniqueWork("hybrid-sheet-sync", ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<SheetSyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
        scope.launch { runCatching { drain() }.onFailure { if (it is CancellationException) throw it else errors.value = it.message } }
    }

    suspend fun drain(): Boolean = drainMutex.withLock {
        val fn = functions ?: return@withLock true
        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull() ?: return@withLock true
        syncing.value = true
        try {
            for (operation in dao.pending(uid)) {
                if (runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull() != uid) return@withLock true
                try {
                    val result = fn.getHttpsCallable("submitSheetCommand").call(SheetWireCodec.map(operation.envelopeJson)).await().data as? Map<*, *> ?: error("Resposta de sincronização inválida.")
                    val receipt = result["receipt"] as? Map<*, *> ?: error("Confirmação de sincronização ausente.")
                    val accepted = receipt["outcome"] == "COMMITTED"
                    val message = (receipt["error"] as? Map<*, *>)?.get("message")?.toString().orEmpty()
                    db.withTransaction {
                        dao.upsertOperation(operation.copy(status = if (accepted) "COMMITTED" else "REJECTED", error = message))
                        val state = result["state"]
                        if (state != null) accept(uid, operation.sheetId, SheetWireCodec.decode(SheetWireCodec.json(state)), (result["revision"] as? Number)?.toLong())
                    }
                } catch (error: FirebaseFunctionsException) {
                    if (error.code in setOf(FirebaseFunctionsException.Code.INVALID_ARGUMENT, FirebaseFunctionsException.Code.PERMISSION_DENIED, FirebaseFunctionsException.Code.FAILED_PRECONDITION)) {
                        // Keep the original envelope/sequence. A transport rejection has no durable receipt;
                        // later commands must not leap over it and create an unrecoverable sequence gap.
                        errors.value = error.message
                        return@withLock false
                    }
                    throw error
                }
            }
            errors.value = null
            true
        } finally { syncing.value = false }
    }

    private suspend fun accept(uid: String, sheetId: String, record: CharacterRecord, revision: Long? = null) {
        val route = dao.route(uid, sheetId) ?: return
        val incomingRevision = revision ?: record.toDomain().serverSyncRevision()
        if (incomingRevision < route.revision) return
        val confirmed = record.copy(id = sheetId, dirty = false)
        dao.upsertRoute(route.copy(revision = incomingRevision, confirmedJson = SheetWireCodec.encode(confirmed)))
        var projected = confirmed.toDomain()
        for (pending in dao.pendingSheet(uid, sheetId)) {
            val envelope = SheetWireCodec.map(pending.envelopeJson)
            val command = envelope["command"] as? Map<*, *> ?: continue
            val payload = command["payload"] as? Map<*, *> ?: continue
            if (command["type"] == "PATCH") {
                val base = SheetWireCodec.decode(SheetWireCodec.json(payload["base"])).toDomain()
                val local = SheetWireCodec.decode(pending.optimisticJson).toDomain()
                projected = applyCharacterEditKeys(projected, local, characterEditKeys(base, local))
            } else if (command["type"] == "SESSION") {
                projected = try {
                    projected.applySessionCommand(SheetWireCodec.session(payload), uid).character
                } catch (error: IllegalArgumentException) {
                    errors.value = error.message
                    projected
                } catch (error: IllegalStateException) {
                    errors.value = error.message
                    projected
                }
            } else if (command["type"] == "DELETE") {
                projected = projected.copy(deleted = true)
            }
        }
        characters.upsert(projected.copy(dirty = false).toRecord())
    }

    fun observe(session: UserSession): Flow<Unit> = callbackFlow {
        val rtdb = database ?: run { awaitClose {}; return@callbackFlow }
        val registrations = mutableMapOf<String, Pair<com.google.firebase.database.DatabaseReference, ValueEventListener>>()
        val index = rtdb.getReference("userSheets/${session.uid}")
        val indexListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = snapshot.children.mapNotNull { it.key }.toSet()
                registrations.keys.filter { it !in ids }.forEach { id ->
                    registrations.remove(id)?.let { (ref, listener) -> ref.removeEventListener(listener); ref.keepSynced(false) }
                    launch { dao.removeRoute(session.uid, id) }
                }
                ids.filter { it !in registrations }.forEach { id ->
                    val ref = rtdb.getReference("sheetAggregates/$id/state")
                    val listener = object : ValueEventListener {
                        override fun onDataChange(value: DataSnapshot) {
                            if (value.value == null) return
                            launch {
                                try {
                                    if (isRtdb(session.uid, id)) {
                                        db.withTransaction { accept(session.uid, id, SheetWireCodec.decode(SheetWireCodec.json(value.value))) }
                                        trySend(Unit)
                                    }
                                } catch (error: Exception) { if (error is CancellationException) throw error; errors.value = error.message }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) { errors.value = error.message; launch { dao.removeRoute(session.uid, id) } }
                    }
                    registrations[id] = ref to listener
                    ref.addValueEventListener(listener)
                    ref.keepSynced(true)
                }
            }
            override fun onCancelled(error: DatabaseError) { errors.value = error.message }
        }
        index.addValueEventListener(indexListener)
        schedule()
        awaitClose { index.removeEventListener(indexListener); registrations.values.forEach { (ref, listener) -> ref.removeEventListener(listener); ref.keepSynced(false) } }
    }

    suspend fun presence(session: UserSession, sheetId: String, field: String?, active: Boolean) {
        val rtdb = database ?: return
        if (!supportsOffline(sheetId)) return
        val ref = rtdb.getReference("presence/$sheetId/${session.uid}/$connectionId")
        if (!active || field == null) { ref.removeValue(); return }
        if (rtdb.getReference(".info/connected").get().await().getValue(Boolean::class.java) != true) return
        ref.onDisconnect().removeValue().await()
        ref.setValue(mapOf("fieldKey" to field, "timestamp" to ServerValue.TIMESTAMP)).await()
    }
    fun presence(session: UserSession, sheetId: String): Flow<Map<String, String>> = callbackFlow {
        val rtdb = database ?: run { trySend(emptyMap()); awaitClose {}; return@callbackFlow }
        val ref = rtdb.getReference("presence/$sheetId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val values = buildMap { snapshot.children.forEach { user -> user.children.forEach { connection ->
                    if (connection.key != connectionId) {
                        val key = connection.child("fieldKey").getValue(String::class.java)
                        val time = connection.child("timestamp").getValue(Long::class.java) ?: 0
                        if (key != null && System.currentTimeMillis() - time < 120_000) put(key, if (user.key == session.uid) "Outro dispositivo" else "Outro participante")
                    }
                } } }
                trySend(values)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}

class SheetSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        if ((applicationContext as SdoApplication).hybridSheetSync.drain()) Result.success() else Result.retry()
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        Result.retry()
    }
}
