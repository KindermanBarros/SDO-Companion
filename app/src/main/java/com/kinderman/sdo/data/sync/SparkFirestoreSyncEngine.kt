package com.kinderman.sdo.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.firestore
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kinderman.sdo.SdoApplication
import com.kinderman.sdo.data.local.AccountSheetRecord
import com.kinderman.sdo.data.local.AppDatabase
import com.kinderman.sdo.data.local.CampaignMemberRecord
import com.kinderman.sdo.data.local.CampaignRecord
import com.kinderman.sdo.data.local.CharacterConverters
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.SessionOperationRecord
import com.kinderman.sdo.data.local.SheetConflictRecord
import com.kinderman.sdo.data.local.SheetIntentRecord
import com.kinderman.sdo.data.local.SheetOutboxRecord
import com.kinderman.sdo.data.local.SheetRouteRecord
import com.kinderman.sdo.data.local.normalizedForSparkV2
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.data.repository.FirestoreCollections
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AccountSheetSnapshot
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.CampaignMemberState
import com.kinderman.sdo.domain.model.CampaignRole
import com.kinderman.sdo.domain.model.CampaignState
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.ConditionInstance
import com.kinderman.sdo.domain.model.ConflictResolutionStatus
import com.kinderman.sdo.domain.model.CURRENT_CHARACTER_SCHEMA_VERSION
import com.kinderman.sdo.domain.model.DomainError
import com.kinderman.sdo.domain.model.IdempotencyMatchResult
import com.kinderman.sdo.domain.model.ItemState
import com.kinderman.sdo.domain.model.OperationReceipt
import com.kinderman.sdo.domain.model.OperationType
import com.kinderman.sdo.domain.model.ProgressionRecord
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SessionResource
import com.kinderman.sdo.domain.model.SheetConflictItem
import com.kinderman.sdo.domain.model.SheetIntent
import com.kinderman.sdo.domain.model.SheetIntentStatus
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.SheetSyncStatus
import com.kinderman.sdo.domain.model.SparkTargetSpec
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.TargetKind
import com.kinderman.sdo.domain.model.TargetOp
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.allCanonicalAbilitiesSafely
import com.kinderman.sdo.domain.model.applyCharacterEditKeys
import com.kinderman.sdo.domain.model.applyPendingIntentsOnConfirmed
import com.kinderman.sdo.domain.model.applySessionCommand
import com.kinderman.sdo.domain.model.buildConflictResolutionIntent
import com.kinderman.sdo.domain.model.characterEditKeys
import com.kinderman.sdo.domain.model.computeCanonicalPayloadHash
import com.kinderman.sdo.domain.model.decodeWireElementId
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.domain.model.resourceMaximum
import com.kinderman.sdo.domain.model.resourceValue
import com.kinderman.sdo.domain.model.toSyncStatus
import com.kinderman.sdo.domain.model.validateAtomicSparkActionBudget
import com.kinderman.sdo.domain.policy.CharacterAccessPolicy
import com.kinderman.sdo.domain.repository.RemoteSyncEvent
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

private const val DEMO_ACCOUNT_ID = "demo-player"

interface SparkClock {
    fun nowMillis(): Long
}

object SystemSparkClock : SparkClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

interface SparkWorkScheduler {
    fun schedule(accountId: String)
}

class WorkManagerSparkScheduler(private val context: Context) : SparkWorkScheduler {
    override fun schedule(accountId: String) {
        if (accountId.isBlank() || accountId == DEMO_ACCOUNT_ID) return
        runCatching {
            val request = OneTimeWorkRequestBuilder<SparkSheetSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "spark-sheet-sync",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request,
            )
        }
    }
}

interface SparkTransactionContext {
    fun getRoot(sheetId: String): Map<String, Any?>?
    fun getField(sheetId: String, fieldKey: String): Map<String, Any?>?
    fun getItem(sheetId: String, itemId: String): Map<String, Any?>?
    fun getItemField(sheetId: String, itemId: String, fieldKey: String): Map<String, Any?>?
    fun getOperation(sheetId: String, operationId: String): Map<String, Any?>?
    fun getCampaign(campaignId: String): Map<String, Any?>?
    fun getMember(campaignId: String, uid: String): Map<String, Any?>?
    fun getDelivery(deliveryId: String): Map<String, Any?>?

    fun setRoot(sheetId: String, data: Map<String, Any?>)
    fun setField(sheetId: String, fieldKey: String, data: Map<String, Any?>)
    fun setItem(sheetId: String, itemId: String, data: Map<String, Any?>)
    fun setItemField(sheetId: String, itemId: String, fieldKey: String, data: Map<String, Any?>)
    fun setOperation(sheetId: String, operationId: String, data: Map<String, Any?>)
    fun setMember(campaignId: String, uid: String, data: Map<String, Any?>)
    fun setDelivery(deliveryId: String, data: Map<String, Any?>)

    fun serverTimestampToken(): Any
    fun currentTimestampMillis(): Long
}

/** Cheap change-detector for a sheet: root revision plus the creation time of the latest receipt. */
data class SparkSheetHead(
    val rootRevision: Long,
    val deleted: Boolean,
    val latestOperationAtMs: Long,
)

private const val ROOT_QUERY_LIMIT = 100L
private const val LAST_OPERATION_MARKER = "@lastOperationAt"

interface SparkRemoteGateway {
    suspend fun <T> runTransaction(sheetId: String, block: (SparkTransactionContext) -> T): T
    suspend fun fetchSheetDocuments(sheetId: String): SparkSheetDocuments?

    /** Root document + latest receipt only (two small reads) so unchanged sheets are never re-downloaded. */
    suspend fun fetchSheetHead(sheetId: String): SparkSheetHead?
    suspend fun fetchAccessibleSheetIds(
        uid: String,
        ownedActiveCampaignIds: Set<String>,
        isAdmin: Boolean = false,
    ): Set<String>
    suspend fun writeMediaAvatar(sheetId: String, mediaDoc: Map<String, Any?>)
    suspend fun deleteMediaAvatar(sheetId: String)
    suspend fun writePresence(sheetId: String, presenceId: String, data: Map<String, Any?>)
    suspend fun deletePresence(sheetId: String, presenceId: String)
    fun observePresence(sheetId: String, ownPresenceId: String, ownUid: String): Flow<Map<String, String>>

    /** Bounded root-level listeners only (own sheets + owned active campaigns); never per-sheet. */
    fun observeRemoteChanges(
        uid: String,
        ownedActiveCampaignIds: Set<String>,
        isAdmin: Boolean = false,
    ): Flow<RemoteSyncEvent>

    /** Receipt listener for exactly one OPEN sheet. */
    fun observeSheetReceipts(sheetId: String, ownUid: String): Flow<RemoteSyncEvent>
}

interface SparkLocalStore {
    suspend fun <T> withTransaction(block: suspend () -> T): T
    suspend fun getAccountSheet(accountId: String, sheetId: String): AccountSheetRecord?
    suspend fun listAccountSheets(accountId: String): List<AccountSheetRecord>
    fun observeAccountSheets(accountId: String): Flow<List<AccountSheetRecord>>
    suspend fun upsertAccountSheet(record: AccountSheetRecord)
    suspend fun deleteAccountSheet(accountId: String, sheetId: String)

    suspend fun listIntentsForSheet(accountId: String, sheetId: String): List<SheetIntentRecord>
    suspend fun listActiveIntents(accountId: String): List<SheetIntentRecord>
    fun observeIntents(accountId: String): Flow<List<SheetIntentRecord>>
    suspend fun getIntent(accountId: String, operationId: String): SheetIntentRecord?
    suspend fun upsertIntent(intent: SheetIntentRecord)
    suspend fun upsertIntents(intents: List<SheetIntentRecord>)
    suspend fun deleteIntent(accountId: String, operationId: String)

    fun observeOpenConflicts(accountId: String): Flow<List<SheetConflictRecord>>
    suspend fun getConflict(accountId: String, id: String): SheetConflictRecord?
    suspend fun upsertConflict(conflict: SheetConflictRecord)
    suspend fun upsertConflicts(conflicts: List<SheetConflictRecord>)
    suspend fun deleteConflict(accountId: String, id: String)

    suspend fun getCharacter(sheetId: String): CharacterRecord?
    suspend fun allCharacters(): List<CharacterRecord>
    suspend fun upsertCharacter(record: CharacterRecord)
    suspend fun purgeCharacter(sheetId: String)

    suspend fun getCampaign(campaignId: String): CampaignRecord?
    suspend fun listOwnedActiveCampaignIds(uid: String): Set<String>
    suspend fun getMember(campaignId: String, uid: String): CampaignMemberRecord?
    suspend fun upsertMember(member: CampaignMemberRecord)

    suspend fun getOperationByIdempotencyKey(key: String): SessionOperationRecord?
    suspend fun upsertOperation(operation: SessionOperationRecord)

    suspend fun listLegacyOutbox(accountId: String): List<SheetOutboxRecord>
    suspend fun markLegacyOutboxStatus(operationId: String, status: String)
    suspend fun listLegacyRoutes(accountId: String): List<SheetRouteRecord>
    suspend fun removeLegacyRoute(accountId: String, sheetId: String)
}

class SparkPermissionDeniedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class SparkTransientNetworkException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class RoomSparkLocalStore(private val db: AppDatabase) : SparkLocalStore {
    private val accountSheetDao = db.accountSheetDao()
    private val intentDao = db.sheetIntentDao()
    private val conflictDao = db.sheetConflictDao()
    private val characterDao = db.characterDao()
    private val campaignDao = db.campaignDao()
    private val operationsDao = db.operationsDao()
    private val legacyDao = db.sheetSyncDao()

    override suspend fun <T> withTransaction(block: suspend () -> T): T = db.withTransaction(block)

    override suspend fun getAccountSheet(accountId: String, sheetId: String): AccountSheetRecord? =
        accountSheetDao.get(accountId, sheetId)

    override suspend fun listAccountSheets(accountId: String): List<AccountSheetRecord> =
        db.openHelper.readableDatabase.query(
            "SELECT accountId, sheetId, sheetRevision, fieldRevisionsJson, confirmedJson, localPhotoUri, updatedAt " +
                "FROM account_sheets WHERE accountId = ?",
            arrayOf(accountId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        AccountSheetRecord(
                            accountId = cursor.getString(0),
                            sheetId = cursor.getString(1),
                            sheetRevision = cursor.getLong(2),
                            fieldRevisionsJson = cursor.getString(3),
                            confirmedJson = cursor.getString(4),
                            localPhotoUri = cursor.getString(5),
                            updatedAt = cursor.getLong(6),
                        ),
                    )
                }
            }
        }

    override fun observeAccountSheets(accountId: String): Flow<List<AccountSheetRecord>> =
        accountSheetDao.observeAll(accountId)

    override suspend fun upsertAccountSheet(record: AccountSheetRecord) = accountSheetDao.upsert(record)

    override suspend fun deleteAccountSheet(accountId: String, sheetId: String) =
        accountSheetDao.delete(accountId, sheetId)

    override suspend fun listIntentsForSheet(accountId: String, sheetId: String): List<SheetIntentRecord> =
        intentDao.listForSheet(accountId, sheetId)

    override suspend fun listActiveIntents(accountId: String): List<SheetIntentRecord> =
        intentDao.listActive(accountId)

    override fun observeIntents(accountId: String): Flow<List<SheetIntentRecord>> =
        intentDao.observeForAccount(accountId)

    override suspend fun getIntent(accountId: String, operationId: String): SheetIntentRecord? =
        intentDao.get(accountId, operationId)

    override suspend fun upsertIntent(intent: SheetIntentRecord) = intentDao.upsert(intent)

    override suspend fun upsertIntents(intents: List<SheetIntentRecord>) {
        if (intents.isNotEmpty()) intentDao.upsertAll(intents)
    }

    override suspend fun deleteIntent(accountId: String, operationId: String) =
        intentDao.delete(accountId, operationId)

    override fun observeOpenConflicts(accountId: String): Flow<List<SheetConflictRecord>> =
        conflictDao.observeOpen(accountId)

    override suspend fun getConflict(accountId: String, id: String): SheetConflictRecord? =
        conflictDao.get(accountId, id)

    override suspend fun upsertConflict(conflict: SheetConflictRecord) = conflictDao.upsert(conflict)

    override suspend fun upsertConflicts(conflicts: List<SheetConflictRecord>) {
        if (conflicts.isNotEmpty()) conflictDao.upsertAll(conflicts)
    }

    override suspend fun deleteConflict(accountId: String, id: String) = conflictDao.delete(accountId, id)

    override suspend fun getCharacter(sheetId: String): CharacterRecord? = characterDao.get(sheetId)

    override suspend fun allCharacters(): List<CharacterRecord> = characterDao.all()

    override suspend fun upsertCharacter(record: CharacterRecord) = characterDao.upsert(record)

    override suspend fun purgeCharacter(sheetId: String) = characterDao.purge(sheetId)

    override suspend fun getCampaign(campaignId: String): CampaignRecord? = campaignDao.campaign(campaignId)

    override suspend fun listOwnedActiveCampaignIds(uid: String): Set<String> =
        campaignDao.ownedCampaigns(uid)
            .filter { it.state == CampaignState.ACTIVE.name }
            .mapTo(linkedSetOf(), CampaignRecord::id)

    override suspend fun getMember(campaignId: String, uid: String): CampaignMemberRecord? =
        campaignDao.member(campaignId, uid)

    override suspend fun upsertMember(member: CampaignMemberRecord) = campaignDao.upsertMember(member)

    override suspend fun getOperationByIdempotencyKey(key: String): SessionOperationRecord? =
        operationsDao.operationByIdempotencyKey(key)

    override suspend fun upsertOperation(operation: SessionOperationRecord) = operationsDao.upsertOperation(operation)

    override suspend fun listLegacyOutbox(accountId: String): List<SheetOutboxRecord> =
        legacyDao.pending(accountId)

    override suspend fun markLegacyOutboxStatus(operationId: String, status: String) {
        val existing = legacyDao.operation(operationId) ?: return
        legacyDao.upsertOperation(existing.copy(status = status))
    }

    override suspend fun listLegacyRoutes(accountId: String): List<SheetRouteRecord> =
        db.openHelper.readableDatabase.query(
            "SELECT accountId, sheetId, status, accessEpoch, revision, confirmedJson FROM sheet_routes WHERE accountId = ?",
            arrayOf(accountId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        SheetRouteRecord(
                            accountId = cursor.getString(0),
                            sheetId = cursor.getString(1),
                            status = cursor.getString(2),
                            accessEpoch = cursor.getLong(3),
                            revision = cursor.getLong(4),
                            confirmedJson = cursor.getString(5),
                        ),
                    )
                }
            }
        }

    override suspend fun removeLegacyRoute(accountId: String, sheetId: String) =
        legacyDao.removeRoute(accountId, sheetId)
}

class FirestoreSparkRemoteGateway(
    private val firestoreProvider: () -> FirebaseFirestore = { Firebase.firestore },
    private val clock: SparkClock = SystemSparkClock,
) : SparkRemoteGateway {

    override suspend fun <T> runTransaction(sheetId: String, block: (SparkTransactionContext) -> T): T {
        val store = firestoreProvider()
        return try {
            store.runTransaction { tx ->
                block(FirestoreTransactionContext(store, tx, clock))
            }.await()
        } catch (error: FirebaseFirestoreException) {
            when (error.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    throw SparkPermissionDeniedException(error.message ?: "Permissão negada.", error)
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                FirebaseFirestoreException.Code.ABORTED,
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                    throw SparkTransientNetworkException(error.message ?: "Falha temporária de rede.", error)
                else -> throw error
            }
        }
    }

    override suspend fun fetchSheetDocuments(sheetId: String): SparkSheetDocuments? {
        if (sheetId.isBlank()) return null
        val store = firestoreProvider()
        val charRef = store.collection(FirestoreCollections.CHARACTERS).document(sheetId)
        val rootSnap = try {
            charRef.get().await()
        } catch (error: FirebaseFirestoreException) {
            if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                error.code == FirebaseFirestoreException.Code.NOT_FOUND
            ) {
                return null
            }
            throw error
        }
        if (!rootSnap.exists()) return null
        val rootData = normalizeFirestoreMap(rootSnap.data.orEmpty())
        val (fieldsSnap, itemsSnap, avatarSnap) = coroutineScope {
            val fieldsJob = async { charRef.collection(FirestoreCollections.CHARACTER_FIELDS).get().await() }
            val itemsJob = async { charRef.collection(FirestoreCollections.CHARACTER_ITEMS).get().await() }
            val avatarJob = async {
                runCatching {
                    charRef.collection(FirestoreCollections.CHARACTER_MEDIA)
                        .document(FirestoreCollections.AVATAR_DOCUMENT)
                        .get()
                        .await()
                }.getOrNull()
            }
            Triple(fieldsJob.await(), itemsJob.await(), avatarJob.await())
        }

        val fields = linkedMapOf<String, Map<String, Any?>>()
        fieldsSnap.documents.forEach { doc ->
            fields[doc.id] = normalizeFirestoreMap(doc.data.orEmpty())
        }

        val items = linkedMapOf<String, SparkItemBundle>()
        val itemFieldMaps = coroutineScope {
            itemsSnap.documents.map { itemDoc ->
                async {
                    val itemData = normalizeFirestoreMap(itemDoc.data.orEmpty())
                    val itemFieldsSnap = itemDoc.reference
                        .collection(FirestoreCollections.CHARACTER_FIELDS)
                        .get()
                        .await()
                    val itemFields = linkedMapOf<String, Map<String, Any?>>()
                    itemFieldsSnap.documents.forEach { f ->
                        itemFields[f.id] = normalizeFirestoreMap(f.data.orEmpty())
                    }
                    itemDoc.id to SparkItemBundle(document = itemData, fields = itemFields)
                }
            }.awaitAll()
        }
        itemFieldMaps.forEach { (itemId, bundle) ->
            items[itemId] = bundle
        }

        val mediaAvatar = avatarSnap?.takeIf { it.exists() }?.data?.let(::normalizeFirestoreMap)
        return SparkSheetDocuments(
            root = rootData,
            fields = fields,
            items = items,
            mediaAvatar = mediaAvatar,
        )
    }

    private suspend fun pagedSheetIds(query: Query): Set<String> {
        val ids = linkedSetOf<String>()
        var cursor: DocumentSnapshot? = null
        do {
            val page = (cursor?.let { query.startAfter(it) } ?: query)
                .limit(ROOT_QUERY_LIMIT).get().await().documents
            ids += page.map(DocumentSnapshot::getId)
            cursor = page.lastOrNull()
        } while (page.size == ROOT_QUERY_LIMIT.toInt())
        return ids
    }

    override suspend fun fetchAccessibleSheetIds(
        uid: String,
        ownedActiveCampaignIds: Set<String>,
        isAdmin: Boolean,
    ): Set<String> {
        if (uid.isBlank() || uid == DEMO_ACCOUNT_ID) return emptySet()
        val store = firestoreProvider()
        val characters = store.collection(FirestoreCollections.CHARACTERS)
        if (isAdmin) {
            return runCatching { pagedSheetIds(characters) }.getOrDefault(emptySet())
        }
        val campaigns = store.collection(FirestoreCollections.CAMPAIGNS)
        val remoteOwnedCampaignIds = runCatching {
            campaigns.whereEqualTo("ownerId", uid).get().await().documents
                .filter { it.getString("state") == CampaignState.ACTIVE.name }
                .mapTo(linkedSetOf(), DocumentSnapshot::getId)
        }.getOrDefault(emptySet())
        val effectiveCampaignIds = (ownedActiveCampaignIds + remoteOwnedCampaignIds)
            .filter(String::isNotBlank)
            .toSet()

        return coroutineScope {
            val ownJob = async {
                pagedSheetIds(characters.whereEqualTo("ownerId", uid))
            }
            val gmJobs = effectiveCampaignIds.map { cid ->
                async {
                    runCatching {
                        pagedSheetIds(characters.whereEqualTo("campaignId", cid))
                    }.getOrDefault(emptySet())
                }
            }
            val result = linkedSetOf<String>()
            result += ownJob.await()
            gmJobs.awaitAll().forEach { result += it }
            result
        }
    }

    override suspend fun writeMediaAvatar(sheetId: String, mediaDoc: Map<String, Any?>) {
        val store = firestoreProvider()
        val payload = linkedMapOf<String, Any?>().apply {
            putAll(mediaDoc)
            put("updatedAt", FieldValue.serverTimestamp())
        }
        store.collection(FirestoreCollections.CHARACTERS)
            .document(sheetId)
            .collection(FirestoreCollections.CHARACTER_MEDIA)
            .document(FirestoreCollections.AVATAR_DOCUMENT)
            .set(payload)
            .await()
    }

    override suspend fun deleteMediaAvatar(sheetId: String) {
        val store = firestoreProvider()
        runCatching {
            store.collection(FirestoreCollections.CHARACTERS)
                .document(sheetId)
                .collection(FirestoreCollections.CHARACTER_MEDIA)
                .document(FirestoreCollections.AVATAR_DOCUMENT)
                .delete()
                .await()
        }
    }

    override suspend fun writePresence(sheetId: String, presenceId: String, data: Map<String, Any?>) {
        val store = firestoreProvider()
        val payload = linkedMapOf<String, Any?>().apply {
            putAll(data)
            put("updatedAt", FieldValue.serverTimestamp())
        }
        store.collection(FirestoreCollections.CHARACTERS)
            .document(sheetId)
            .collection(FirestoreCollections.CHARACTER_PRESENCE)
            .document(presenceId)
            .set(payload)
            .await()
    }

    override suspend fun deletePresence(sheetId: String, presenceId: String) {
        val store = firestoreProvider()
        runCatching {
            store.collection(FirestoreCollections.CHARACTERS)
                .document(sheetId)
                .collection(FirestoreCollections.CHARACTER_PRESENCE)
                .document(presenceId)
                .delete()
                .await()
        }
    }

    override fun observePresence(
        sheetId: String,
        ownPresenceId: String,
        ownUid: String,
    ): Flow<Map<String, String>> = callbackFlow {
        if (sheetId.isBlank() || ownUid.isBlank() || ownUid == DEMO_ACCOUNT_ID) {
            trySend(emptyMap())
            awaitClose {}
            return@callbackFlow
        }
        val store = runCatching { firestoreProvider() }.getOrNull() ?: run {
            trySend(emptyMap())
            awaitClose {}
            return@callbackFlow
        }
        val registration = store.collection(FirestoreCollections.CHARACTERS)
            .document(sheetId)
            .collection(FirestoreCollections.CHARACTER_PRESENCE)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyMap())
                    return@addSnapshotListener
                }
                val now = clock.nowMillis()
                val active = linkedMapOf<String, String>()
                snapshot.documents.forEach { doc ->
                    if (doc.id == ownPresenceId) return@forEach
                    val expiresAtMs = doc.getTimestamp("expiresAt")?.toDate()?.time
                        ?: doc.getLong("expiresAt")
                        ?: 0L
                    if (expiresAtMs <= now) return@forEach
                    val fieldKey = doc.getString("fieldKey").orEmpty()
                    if (fieldKey.isBlank()) return@forEach
                    val uid = doc.getString("uid").orEmpty()
                    active[fieldKey] = if (uid == ownUid) "Outro dispositivo" else "Outro participante"
                }
                trySend(active)
            }
        awaitClose { registration.remove() }
    }

    override fun observeRemoteChanges(
        uid: String,
        ownedActiveCampaignIds: Set<String>,
        isAdmin: Boolean,
    ): Flow<RemoteSyncEvent> = callbackFlow {
        if (uid.isBlank() || uid == DEMO_ACCOUNT_ID) {
            awaitClose {}
            return@callbackFlow
        }
        val store = runCatching { firestoreProvider() }.getOrNull() ?: run {
            awaitClose {}
            return@callbackFlow
        }
        val registrations = mutableListOf<ListenerRegistration>()

        // Dashboard listeners are bounded, root-level only. Per-sheet receipt listeners are attached
        // exclusively for OPEN sheets through [observeSheetReceipts].
        fun listenCharactersQuery(query: Query) {
            val initialized = AtomicBoolean(false)
            registrations += query.limit(ROOT_QUERY_LIMIT).addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (snapshot.metadata.hasPendingWrites() || snapshot.metadata.isFromCache) {
                    initialized.compareAndSet(false, true)
                    return@addSnapshotListener
                }
                if (!initialized.compareAndSet(false, true)) {
                    val changedIds = snapshot.documentChanges.mapTo(linkedSetOf()) { it.document.id }
                    if (changedIds.isNotEmpty()) {
                        trySend(
                            RemoteSyncEvent(
                                fromCloud = true,
                                characterIds = changedIds,
                                fullSync = false,
                            ),
                        )
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
        if (isAdmin) {
            listenCharactersQuery(characters)
        } else {
            // Strict Spark v2 ACL: listen ONLY to own sheets + active campaigns owned as GM.
            listenCharactersQuery(characters.whereEqualTo("ownerId", uid))
            ownedActiveCampaignIds.filter(String::isNotBlank).distinct().forEach { campaignId ->
                listenCharactersQuery(characters.whereEqualTo("campaignId", campaignId))
            }
        }
        ownedActiveCampaignIds.filter(String::isNotBlank).distinct().forEach { campaignId ->
            listenCollection(store.collection(FirestoreCollections.CAMPAIGN_LIBRARY).whereEqualTo("campaignId", campaignId))
            listenCollection(store.collection(FirestoreCollections.CAMPAIGN_AUDIT).whereEqualTo("campaignId", campaignId))
        }
        listenCollection(
            store.collectionGroup(FirestoreCollections.CAMPAIGN_MEMBERS_SUBCOLLECTION)
                .whereEqualTo("userId", uid),
        )
        listenCollection(
            store.collection(FirestoreCollections.CAMPAIGN_DELIVERIES)
                .whereEqualTo("recipientId", uid),
        )

        awaitClose {
            registrations.forEach(ListenerRegistration::remove)
        }
    }

    override suspend fun fetchSheetHead(sheetId: String): SparkSheetHead? {
        if (sheetId.isBlank()) return null
        val charRef = firestoreProvider().collection(FirestoreCollections.CHARACTERS).document(sheetId)
        val root = try {
            charRef.get().await()
        } catch (error: FirebaseFirestoreException) {
            if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                error.code == FirebaseFirestoreException.Code.NOT_FOUND
            ) {
                return null
            }
            throw error
        }
        if (!root.exists()) return null
        val latest = charRef.collection(FirestoreCollections.CHARACTER_OPERATIONS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
        val latestMs = asLong(normalizeFirestoreValue(latest?.get("createdAt"))) ?: 0L
        return SparkSheetHead(
            rootRevision = root.getLong("revision") ?: 0L,
            deleted = root.getBoolean("deleted") == true,
            latestOperationAtMs = latestMs,
        )
    }

    override fun observeSheetReceipts(sheetId: String, ownUid: String): Flow<RemoteSyncEvent> = callbackFlow {
        val store = runCatching { firestoreProvider() }.getOrNull()
        if (store == null || sheetId.isBlank() || ownUid.isBlank() || ownUid == DEMO_ACCOUNT_ID) {
            awaitClose {}
            return@callbackFlow
        }
        val initialized = AtomicBoolean(false)
        val registration = store.collection(FirestoreCollections.CHARACTERS)
            .document(sheetId)
            .collection(FirestoreCollections.CHARACTER_OPERATIONS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (snapshot.metadata.hasPendingWrites() || snapshot.metadata.isFromCache) {
                    initialized.compareAndSet(false, true)
                    return@addSnapshotListener
                }
                if (!initialized.compareAndSet(false, true) &&
                    snapshot.documentChanges.isNotEmpty()
                ) {
                    trySend(RemoteSyncEvent(fromCloud = true, characterIds = setOf(sheetId), fullSync = false))
                }
            }
        awaitClose { registration.remove() }
    }

    private class FirestoreTransactionContext(
        private val store: FirebaseFirestore,
        private val tx: Transaction,
        private val clock: SparkClock,
    ) : SparkTransactionContext {
        private fun charRef(sheetId: String) = store.collection(FirestoreCollections.CHARACTERS).document(sheetId)

        override fun getRoot(sheetId: String): Map<String, Any?>? =
            tx.get(charRef(sheetId)).takeIf { it.exists() }?.data?.let(::normalizeFirestoreMap)

        override fun getField(sheetId: String, fieldKey: String): Map<String, Any?>? =
            tx.get(charRef(sheetId).collection(FirestoreCollections.CHARACTER_FIELDS).document(fieldKey))
                .takeIf { it.exists() }?.data?.let(::normalizeFirestoreMap)

        override fun getItem(sheetId: String, itemId: String): Map<String, Any?>? =
            tx.get(charRef(sheetId).collection(FirestoreCollections.CHARACTER_ITEMS).document(itemId))
                .takeIf { it.exists() }?.data?.let(::normalizeFirestoreMap)

        override fun getItemField(sheetId: String, itemId: String, fieldKey: String): Map<String, Any?>? =
            tx.get(
                charRef(sheetId)
                    .collection(FirestoreCollections.CHARACTER_ITEMS)
                    .document(itemId)
                    .collection(FirestoreCollections.CHARACTER_FIELDS)
                    .document(fieldKey),
            ).takeIf { it.exists() }?.data?.let(::normalizeFirestoreMap)

        override fun getOperation(sheetId: String, operationId: String): Map<String, Any?>? =
            tx.get(charRef(sheetId).collection(FirestoreCollections.CHARACTER_OPERATIONS).document(operationId))
                .takeIf { it.exists() }?.data?.let(::normalizeFirestoreMap)

        override fun getCampaign(campaignId: String): Map<String, Any?>? =
            tx.get(store.collection(FirestoreCollections.CAMPAIGNS).document(campaignId))
                .takeIf { it.exists() }?.data?.let(::normalizeFirestoreMap)

        override fun getMember(campaignId: String, uid: String): Map<String, Any?>? =
            tx.get(
                store.collection(FirestoreCollections.CAMPAIGNS)
                    .document(campaignId)
                    .collection(FirestoreCollections.CAMPAIGN_MEMBERS_SUBCOLLECTION)
                    .document(uid),
            ).takeIf { it.exists() }?.data?.let(::normalizeFirestoreMap)

        override fun getDelivery(deliveryId: String): Map<String, Any?>? =
            tx.get(store.collection(FirestoreCollections.CAMPAIGN_DELIVERIES).document(deliveryId))
                .takeIf { it.exists() }?.data?.let(::normalizeFirestoreMap)

        override fun setRoot(sheetId: String, data: Map<String, Any?>) {
            tx.set(charRef(sheetId), data)
        }

        override fun setField(sheetId: String, fieldKey: String, data: Map<String, Any?>) {
            tx.set(charRef(sheetId).collection(FirestoreCollections.CHARACTER_FIELDS).document(fieldKey), data)
        }

        override fun setItem(sheetId: String, itemId: String, data: Map<String, Any?>) {
            tx.set(charRef(sheetId).collection(FirestoreCollections.CHARACTER_ITEMS).document(itemId), data)
        }

        override fun setItemField(sheetId: String, itemId: String, fieldKey: String, data: Map<String, Any?>) {
            tx.set(
                charRef(sheetId)
                    .collection(FirestoreCollections.CHARACTER_ITEMS)
                    .document(itemId)
                    .collection(FirestoreCollections.CHARACTER_FIELDS)
                    .document(fieldKey),
                data,
            )
        }

        override fun setOperation(sheetId: String, operationId: String, data: Map<String, Any?>) {
            tx.set(charRef(sheetId).collection(FirestoreCollections.CHARACTER_OPERATIONS).document(operationId), data)
        }

        override fun setMember(campaignId: String, uid: String, data: Map<String, Any?>) {
            tx.set(
                store.collection(FirestoreCollections.CAMPAIGNS)
                    .document(campaignId)
                    .collection(FirestoreCollections.CAMPAIGN_MEMBERS_SUBCOLLECTION)
                    .document(uid),
                data,
            )
        }

        override fun setDelivery(deliveryId: String, data: Map<String, Any?>) {
            tx.set(store.collection(FirestoreCollections.CAMPAIGN_DELIVERIES).document(deliveryId), data)
        }

        override fun serverTimestampToken(): Any = FieldValue.serverTimestamp()

        override fun currentTimestampMillis(): Long = clock.nowMillis()
    }
}

private sealed interface DrainTxOutcome {
    data class Committed(
        val resolvedTargets: List<SparkTargetSpec>,
        val serverTimestamp: Long,
        val appliedDeliveryId: String? = null,
        val updatedRoot: Map<String, Any?>? = null,
    ) : DrainTxOutcome

    data class AlreadyCommitted(val receipt: OperationReceipt) : DrainTxOutcome
    data class IdempotencyMismatch(val reason: String) : DrainTxOutcome
    data class Tombstoned(val reason: String, val isSheet: Boolean, val itemId: String = "") : DrainTxOutcome
    data class Rejected(val reason: String) : DrainTxOutcome
    data class RevisionConflict(
        val conflicts: List<SheetConflictItem>,
        val remoteRoot: Map<String, Any?>?,
    ) : DrainTxOutcome
}

/**
 * Single Spark Firestore Protocol v2 synchronization engine.
 * Owns durable account-scoped intents (`sheet_intents`), confirmed caches (`account_sheets`),
 * structured conflicts (`sheet_conflicts`), WorkManager drain, ephemeral Firestore presence,
 * and <= 64 KiB avatar thumbnail media sync.
 */
class SparkFirestoreSyncEngine(
    private val localStore: SparkLocalStore,
    private val remoteGateway: SparkRemoteGateway,
    private val scope: CoroutineScope,
    private val workScheduler: SparkWorkScheduler,
    private val clock: SparkClock = SystemSparkClock,
    private val currentUidProvider: () -> String? = { runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull() },
) {
    constructor(
        context: Context,
        db: AppDatabase,
        scope: CoroutineScope,
        clock: SparkClock = SystemSparkClock,
    ) : this(
        localStore = RoomSparkLocalStore(db),
        remoteGateway = FirestoreSparkRemoteGateway(clock = clock),
        scope = scope,
        workScheduler = WorkManagerSparkScheduler(context),
        clock = clock,
    )

    private val drainMutex = Mutex()
    private val accountMutex = Mutex()
    private val syncing = MutableStateFlow(false)
    private val errors = MutableStateFlow<String?>(null)
    private val connectionId = UUID.randomUUID().toString()
    private val lastPresenceState = ConcurrentHashMap<String, Pair<String?, Long>>()
    private val lastUploadedAvatarHash = ConcurrentHashMap<String, String>()

    @Volatile
    private var activeAccountId: String? = null

    /** Bumped on every account activation/logout so in-flight work can detect a session change. */
    private val sessionGeneration = java.util.concurrent.atomic.AtomicLong(0L)

    /**
     * True only when [accountId] is the signed-in account (demo sessions never use Firestore auth).
     * Every shared-state touch (projection into `characters`, transactions, post-transaction
     * writes of acknowledgements) is gated on this so account A's queue is never driven with
     * account B's credentials.
     */
    private fun isAccountCurrent(accountId: String): Boolean =
        accountId == DEMO_ACCOUNT_ID || currentUidProvider() == accountId

    private fun isStillCurrent(accountId: String, generation: Long): Boolean =
        sessionGeneration.get() == generation && isAccountCurrent(accountId)

    fun observeStatus(session: UserSession): Flow<SheetSyncStatus> = combine(
        localStore.observeIntents(session.uid),
        syncing,
        errors,
    ) { records, isSyncing, lastErr ->
        records.map(SheetIntentRecord::toDomain).toSyncStatus(isSyncing = isSyncing, lastError = lastErr)
    }

    fun observeIntents(session: UserSession): Flow<List<SheetIntent>> =
        localStore.observeIntents(session.uid).map { rows -> rows.map(SheetIntentRecord::toDomain) }

    fun observeConflicts(session: UserSession): Flow<List<SheetConflictItem>> =
        localStore.observeOpenConflicts(session.uid).map { rows -> rows.map(SheetConflictRecord::toDomain) }

    fun observeOfflineEditableIds(session: UserSession): Flow<Set<String>> =
        localStore.observeAccountSheets(session.uid).map { rows ->
            rows.mapTo(linkedSetOf(), AccountSheetRecord::sheetId)
        }

    suspend fun supportsOffline(accountId: String, sheetId: String): Boolean {
        if (accountId.isBlank() || sheetId.isBlank()) return false
        return localStore.getAccountSheet(accountId, sheetId) != null || localStore.getCharacter(sheetId) != null
    }

    suspend fun activateAccount(accountId: String) {
        if (accountId.isBlank()) return
        if (!isAccountCurrent(accountId)) return
        accountMutex.withLock {
            if (!isAccountCurrent(accountId)) return@withLock
            sessionGeneration.incrementAndGet()
            activeAccountId = accountId
            reconcileLegacyData(accountId)
            projectAccountToCharactersLocked(accountId)
        }
    }

    /**
     * Leaves durable `account_sheets`, `sheet_intents`, and `sheet_conflicts` untouched on logout
     * while clearing active session state and the `characters` table projection.
     */
    suspend fun onLogout() {
        accountMutex.withLock {
            sessionGeneration.incrementAndGet()
            activeAccountId = null
            lastPresenceState.clear()
            localStore.withTransaction {
                localStore.allCharacters().forEach { localStore.purgeCharacter(it.id) }
            }
        }
        errors.value = null
    }

    suspend fun ensureAccountProjected(accountId: String) {
        if (accountId.isBlank()) return
        if (activeAccountId == accountId) return
        activateAccount(accountId)
    }

    suspend fun create(session: UserSession, ownerId: String = session.uid, campaignId: String = ""): Character {
        val accountId = session.uid
        ensureAccountProjected(accountId)
        val normalizedCampaignId = normalizeCampaignId(campaignId)
        if (ownerId != accountId) {
            check(normalizedCampaignId.isNotBlank()) { "A Mestre só pode atribuir uma ficha dentro da campanha." }
            check(localStore.getCampaign(normalizedCampaignId)?.ownerId == accountId || session.isAdmin) {
                "Somente a Mestre da campanha pode atribuir fichas."
            }
            check(localStore.getMember(normalizedCampaignId, ownerId)?.state == CampaignMemberState.ACTIVE.name) {
                "A ficha só pode ser atribuída a um jogador ativo da campanha."
            }
        }
        val now = clock.nowMillis()
        val character = Character(
            ownerId = ownerId,
            campaignId = normalizedCampaignId,
            canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
            updatedAt = now,
            lastEditedBy = accountId,
            dirty = false,
        )
        val opPrefix = UUID.randomUUID().toString()
        val batches = SparkWireCodec.buildOperationBatches(
            before = null,
            after = character.toRecord(),
            operationIdPrefix = opPrefix,
            actorUid = accountId,
            existingRevisions = emptyMap(),
            sheetRevision = 0L,
            updatedAt = now,
            skipDefaultFieldsOnCreate = true,
        )
        val intents = batchesToIntents(
            batches = batches,
            accountId = accountId,
            sheetId = character.id,
            initialDependency = null,
            createdAt = now,
        )
        val initialSnapshot = AccountSheetSnapshot(
            accountId = accountId,
            sheetId = character.id,
            sheetRevision = 0L,
            fieldRevisions = emptyMap(),
            confirmedCharacter = Character(id = character.id, ownerId = ownerId, campaignId = normalizedCampaignId, dirty = false),
            localPhotoUri = SparkWireCodec.toSparkDocuments(character.toRecord()).localPhotoUri,
            updatedAt = now,
        )
        localStore.withTransaction {
            localStore.upsertAccountSheet(initialSnapshot.toRecord())
            localStore.upsertIntents(intents.map(SheetIntent::toRecord))
            if (normalizedCampaignId.isNotBlank()) {
                localStore.getMember(normalizedCampaignId, ownerId)?.let { membership ->
                    localStore.upsertMember(
                        membership.copy(
                            role = CampaignRole.PLAYER.name,
                            characterIds = (membership.characterIds + character.id).distinct(),
                            updatedAt = now,
                            dirty = true,
                        ),
                    )
                }
            }
            reprojectSheetLocked(accountId, character.id)
        }
        if (accountId != DEMO_ACCOUNT_ID) {
            schedule(accountId)
        }
        return (localStore.getCharacter(character.id)?.toDomain() ?: character).let { it.copy(editingBase = it) }
    }

    suspend fun save(session: UserSession, candidate: Character) {
        val accountId = session.uid
        ensureAccountProjected(accountId)
        if (candidate.canonicalSchemaVersion != CURRENT_CHARACTER_SCHEMA_VERSION) {
            throw DomainError.LegacyWriteRejected()
        }
        val now = clock.nowMillis()
        var shouldSchedule = false
        var mediaToUpload: Map<String, Any?>? = null

        localStore.withTransaction {
            val existingAccountSheet = localStore.getAccountSheet(accountId, candidate.id)
            val snapshot = existingAccountSheet?.toSnapshot()
                ?: localStore.getCharacter(candidate.id)?.toDomain()?.let {
                    AccountSheetSnapshot(
                        accountId = accountId,
                        sheetId = candidate.id,
                        sheetRevision = 0L,
                        confirmedCharacter = it.copy(dirty = false),
                        localPhotoUri = if (SparkWireCodec.isLocalOnlyPhotoUri(it.photoUri)) it.photoUri else "",
                        updatedAt = now,
                    )
                }
            val existingIntents = localStore.listIntentsForSheet(accountId, candidate.id)
                .map(SheetIntentRecord::toDomain)
            val currentProjected = if (snapshot != null) {
                projectReplay(snapshot, existingIntents).projectedCharacter
            } else {
                candidate.copy(dirty = false)
            }
            check(CharacterAccessPolicy.canEdit(session, currentProjected, isCampaignHistorian(session, currentProjected))) {
                "Você não pode editar esta ficha."
            }

            val base = candidate.editingBase ?: currentProjected
            val projected = applyCharacterEditKeys(currentProjected, candidate, characterEditKeys(base, candidate))
                .copy(
                    campaignId = normalizeCampaignId(candidate.campaignId),
                    updatedAt = now,
                    lastEditedBy = accountId,
                    dirty = false,
                )

            val workingState = computeWorkingRevisionsAndHeads(snapshot, existingIntents)
            val afterRecord = projected.toRecord()
            val afterDocs = SparkWireCodec.toSparkDocuments(afterRecord, updatedBy = accountId, updatedAt = now)
            mediaToUpload = afterDocs.mediaAvatar
            val updatedLocalPhotoUri = when {
                projected.photoUri.isBlank() || SparkWireCodec.isPresetAvatar(projected.photoUri) -> ""
                else -> afterDocs.localPhotoUri
            }
            if (snapshot != null && snapshot.localPhotoUri != updatedLocalPhotoUri) {
                localStore.upsertAccountSheet(snapshot.copy(localPhotoUri = updatedLocalPhotoUri, updatedAt = now).toRecord())
            } else if (existingAccountSheet == null) {
                localStore.upsertAccountSheet(
                    AccountSheetSnapshot(
                        accountId = accountId,
                        sheetId = candidate.id,
                        sheetRevision = 0L,
                        confirmedCharacter = currentProjected,
                        localPhotoUri = updatedLocalPhotoUri,
                        updatedAt = now,
                    ).toRecord(),
                )
            }

            val batches = SparkWireCodec.buildOperationBatches(
                before = if (snapshot == null || workingState.sheetRevision == 0L && existingIntents.none { it.type == OperationType.CREATE_SHEET }) {
                    if (snapshot == null) null else currentProjected.toRecord()
                } else {
                    currentProjected.toRecord()
                },
                after = afterRecord,
                operationIdPrefix = UUID.randomUUID().toString(),
                actorUid = accountId,
                existingRevisions = workingState.fieldRevisions,
                sheetRevision = workingState.sheetRevision,
                updatedAt = now,
            )

            if (batches.isNotEmpty()) {
                val newIntents = buildChainedIntentsFromBatches(
                    batches = batches,
                    accountId = accountId,
                    sheetId = candidate.id,
                    workingHeads = workingState.targetHeads,
                    createOpId = workingState.pendingCreateOpId,
                    createdAt = now,
                )
                localStore.upsertIntents(newIntents.map(SheetIntent::toRecord))
                shouldSchedule = true
            }
            reprojectSheetLocked(accountId, candidate.id)
        }

        if (accountId != DEMO_ACCOUNT_ID) {
            mediaToUpload?.let { uploadAvatarIfChanged(candidate.id, it) }
            if (shouldSchedule) schedule(accountId)
        }
    }

    suspend fun setPlayerLocked(session: UserSession, character: Character, locked: Boolean) {
        ensureAccountProjected(session.uid)
        check(CharacterAccessPolicy.canChangePlayerLock(session, character, isCampaignHistorian(session, character))) {
            "O bloqueio do jogador só pode ser alterado pelo dono e não substitui o bloqueio do historiador."
        }
        enqueueLockChange(session.uid, character, if (locked) CharacterLock.PLAYER else CharacterLock.NONE)
    }

    suspend fun setHistorianLocked(session: UserSession, character: Character, locked: Boolean) {
        ensureAccountProjected(session.uid)
        check(CharacterAccessPolicy.canChangeHistorianLock(session, isCampaignHistorian(session, character))) {
            "Somente o historiador pode alterar o bloqueio real."
        }
        enqueueLockChange(session.uid, character, if (locked) CharacterLock.HISTORIAN else CharacterLock.NONE)
    }

    private suspend fun enqueueLockChange(accountId: String, character: Character, lockType: CharacterLock) {
        if (character.canonicalSchemaVersion != CURRENT_CHARACTER_SCHEMA_VERSION) {
            throw DomainError.LegacyWriteRejected()
        }
        val now = clock.nowMillis()
        val locked = lockType != CharacterLock.NONE
        val desiredJson = plainGson.toJson(
            linkedMapOf(
                "lockType" to lockType.name,
                "isLocked" to locked,
                "lockedBy" to if (locked) accountId else "",
                "lockedAt" to if (locked) now else null,
            ),
        )
        localStore.withTransaction {
            val snapshot = ensureAccountSheetSnapshotLocked(accountId, character, now)
            val existing = localStore.listIntentsForSheet(accountId, character.id).map(SheetIntentRecord::toDomain)
            val working = computeWorkingRevisionsAndHeads(snapshot, existing)
            val target = SparkTargetSpec(
                kind = TargetKind.SHEET,
                op = TargetOp.LOCK,
                expectedRevision = working.sheetRevision,
                nextRevision = working.sheetRevision + 1L,
                baseValueJson = plainGson.toJson(
                    linkedMapOf(
                        "lockType" to character.lockType.name,
                        "isLocked" to character.isLocked,
                        "lockedBy" to character.lockedBy,
                    ),
                ),
                valueJson = desiredJson,
            )
            val intent = SheetIntent(
                operationId = UUID.randomUUID().toString(),
                accountId = accountId,
                sheetId = character.id,
                type = OperationType.SET_LOCK,
                targetKey = target.canonicalTargetKey(),
                targets = listOf(target),
                dependsOnOperationId = working.targetHeads["sheet"] ?: working.pendingCreateOpId,
                baseRevision = working.sheetRevision,
                baseValueJson = target.baseValueJson.orEmpty(),
                desiredPayloadJson = desiredJson,
                status = SheetIntentStatus.PENDING,
                createdAt = now,
                updatedAt = now,
            )
            localStore.upsertIntent(intent.toRecord())
            reprojectSheetLocked(accountId, character.id)
        }
        if (accountId != DEMO_ACCOUNT_ID) schedule(accountId)
    }

    suspend fun transferOwnership(session: UserSession, character: Character, owner: UserProfile) {
        val accountId = session.uid
        ensureAccountProjected(accountId)
        check(CharacterAccessPolicy.canTransferOwnership(session, isCampaignResponsible(session, character))) {
            "Somente a responsável principal pode transferir uma ficha."
        }
        check(owner.uid.isNotBlank()) { "O novo owner é inválido." }
        if (character.canonicalSchemaVersion != CURRENT_CHARACTER_SCHEMA_VERSION) {
            throw DomainError.LegacyWriteRejected()
        }
        val now = clock.nowMillis()
        val desiredJson = plainGson.toJson(mapOf("ownerId" to owner.uid))
        localStore.withTransaction {
            val snapshot = ensureAccountSheetSnapshotLocked(accountId, character, now)
            val existing = localStore.listIntentsForSheet(accountId, character.id).map(SheetIntentRecord::toDomain)
            val working = computeWorkingRevisionsAndHeads(snapshot, existing)
            val target = SparkTargetSpec(
                kind = TargetKind.SHEET,
                op = TargetOp.TRANSFER,
                expectedRevision = working.sheetRevision,
                nextRevision = working.sheetRevision + 1L,
                baseValueJson = plainGson.toJson(mapOf("ownerId" to character.ownerId)),
                valueJson = desiredJson,
            )
            val intent = SheetIntent(
                operationId = UUID.randomUUID().toString(),
                accountId = accountId,
                sheetId = character.id,
                type = OperationType.TRANSFER_OWNERSHIP,
                targetKey = target.canonicalTargetKey(),
                targets = listOf(target),
                dependsOnOperationId = working.targetHeads["sheet"] ?: working.pendingCreateOpId,
                baseRevision = working.sheetRevision,
                baseValueJson = target.baseValueJson.orEmpty(),
                desiredPayloadJson = desiredJson,
                status = SheetIntentStatus.PENDING,
                createdAt = now,
                updatedAt = now,
            )
            localStore.upsertIntent(intent.toRecord())
            reprojectSheetLocked(accountId, character.id)
        }
        if (accountId != DEMO_ACCOUNT_ID) schedule(accountId)
    }

    suspend fun delete(session: UserSession, character: Character) {
        val accountId = session.uid
        ensureAccountProjected(accountId)
        check(CharacterAccessPolicy.canDelete(session, character, isCampaignHistorian(session, character))) {
            "Esta ficha não pode ser removida."
        }
        if (drainMutex.withLock { cancelUnpublishedSheet(accountId, character.id) }) return
        if (!isAccountCurrent(accountId)) return
        val now = clock.nowMillis()
        localStore.withTransaction {
            val snapshot = ensureAccountSheetSnapshotLocked(accountId, character, now)
            val existing = localStore.listIntentsForSheet(accountId, character.id).map(SheetIntentRecord::toDomain)
            val working = computeWorkingRevisionsAndHeads(snapshot, existing)
            val target = SparkTargetSpec(
                kind = TargetKind.SHEET,
                op = TargetOp.TOMBSTONE,
                expectedRevision = working.sheetRevision,
                nextRevision = working.sheetRevision + 1L,
                baseValueJson = "{\"deleted\":false}",
                valueJson = "{\"deleted\":true}",
            )
            val intent = SheetIntent(
                operationId = UUID.randomUUID().toString(),
                accountId = accountId,
                sheetId = character.id,
                type = OperationType.DELETE_SHEET,
                targetKey = target.canonicalTargetKey(),
                targets = listOf(target),
                dependsOnOperationId = working.targetHeads["sheet"] ?: working.pendingCreateOpId,
                baseRevision = working.sheetRevision,
                baseValueJson = "{\"deleted\":false}",
                desiredPayloadJson = "{\"deleted\":true}",
                status = SheetIntentStatus.PENDING,
                createdAt = now,
                updatedAt = now,
            )
            localStore.upsertIntent(intent.toRecord())
            reprojectSheetLocked(accountId, character.id)
        }
        if (accountId != DEMO_ACCOUNT_ID) schedule(accountId)
    }

    // Called with drainMutex held so an in-flight creation settles before checking remote absence.
    private suspend fun cancelUnpublishedSheet(accountId: String, sheetId: String): Boolean {
        val snapshot = localStore.getAccountSheet(accountId, sheetId)?.toSnapshot() ?: return false
        if (snapshot.sheetRevision != 0L) return false
        val intents = localStore.listIntentsForSheet(accountId, sheetId)
        if (intents.any { it.status == SheetIntentStatus.CONFIRMED.name }) return false
        val absent = accountId == DEMO_ACCOUNT_ID || try {
            remoteGateway.runTransaction(sheetId) { it.getRoot(sheetId) == null }
        } catch (_: SparkPermissionDeniedException) {
            return false
        } catch (_: SparkTransientNetworkException) {
            return false
        }
        if (!absent || !isAccountCurrent(accountId)) return false
        return localStore.withTransaction {
            val current = localStore.getAccountSheet(accountId, sheetId)?.toSnapshot()
                ?: return@withTransaction true
            val currentIntents = localStore.listIntentsForSheet(accountId, sheetId)
            if (!isAccountCurrent(accountId) || current.sheetRevision != 0L ||
                currentIntents.any { it.status == SheetIntentStatus.CONFIRMED.name }
            ) return@withTransaction false
            currentIntents.forEach { record ->
                record.toDomain().effectiveTargets().forEach { target ->
                    localStore.deleteConflict(accountId, "${record.operationId}::${target.canonicalTargetKey()}")
                }
                localStore.deleteIntent(accountId, record.operationId)
            }
            val character = current.confirmedCharacter
            localStore.getMember(character.campaignId, character.ownerId)?.let { member ->
                localStore.upsertMember(member.copy(characterIds = member.characterIds - sheetId))
            }
            localStore.listLegacyOutbox(accountId).filter { it.sheetId == sheetId }.forEach {
                localStore.markLegacyOutboxStatus(it.operationId, "MIGRATED_V33")
            }
            localStore.removeLegacyRoute(accountId, sheetId)
            localStore.deleteAccountSheet(accountId, sheetId)
            if (activeAccountId == accountId) localStore.purgeCharacter(sheetId)
            true
        }
    }

    /**
     * Validates Spark's single-transaction budget BEFORE any optimistic mutation on multi-effect
     * actions (`ITEM_CONSUME`, `ABILITY_USE`, `EFFECT_OPERATION`, etc.).
     */
    suspend fun applySessionCommand(
        session: UserSession,
        character: Character,
        command: SessionCommand,
    ): Boolean {
        val accountId = session.uid
        ensureAccountProjected(accountId)
        require(command.idempotencyKey.isNotBlank()) { "A ação precisa de uma chave de operação." }
        if (localStore.getIntent(accountId, command.idempotencyKey) != null ||
            localStore.getOperationByIdempotencyKey(command.idempotencyKey) != null
        ) {
            return false
        }

        val now = clock.nowMillis()
        var enqueued = false
        localStore.withTransaction {
            val snapshot = ensureAccountSheetSnapshotLocked(accountId, character, now)
            val existingIntents = localStore.listIntentsForSheet(accountId, character.id)
                .map(SheetIntentRecord::toDomain)
            val currentProjected = projectReplay(snapshot, existingIntents).projectedCharacter
            check(!currentProjected.deleted) { "Esta ficha foi excluída." }
            if (currentProjected.canonicalSchemaVersion != CURRENT_CHARACTER_SCHEMA_VERSION) {
                throw DomainError.LegacyWriteRejected()
            }

            val consume = command.effectOperation as? com.kinderman.sdo.domain.model.EffectOperation.ConsumeItemState
            if (consume != null) {
                require(currentProjected.inventory.firstOrNull { it.id == consume.itemInstanceId.value }
                    ?.mechanicalEffects.orEmpty().isEmpty()) {
                    "Efeitos de consumo exigem um ajuste explícito da Mestre."
                }
            }
            if (command.type == SessionOperationType.ABILITY_USE) {
                val cost = currentProjected.allCanonicalAbilitiesSafely()
                    .firstOrNull { it.id == command.targetId }?.cost
                require(cost !is com.kinderman.sdo.domain.model.AbilityCost.AshCost) {
                    "Este custo exige um ajuste explícito da Mestre."
                }
            }
            val activeGm = currentProjected.campaignId.takeIf(String::isNotBlank)?.let {
                localStore.getCampaign(it)?.ownerId == accountId
            } == true
            require(activeGm || command.type in setOf(
                SessionOperationType.MONEY, SessionOperationType.RESOURCE,
                SessionOperationType.DESTINY, SessionOperationType.HEAL,
                SessionOperationType.NOTE, SessionOperationType.CONDITION_ADD,
                SessionOperationType.CONDITION_REMOVE, SessionOperationType.ABILITY_USE,
            ) || consume != null) {
                "Esta ação exige um ajuste explícito da Mestre."
            }
            // Compute mutation purely before persisting anything!
            val mutation = currentProjected.applySessionCommand(command, accountId)
            val working = computeWorkingRevisionsAndHeads(snapshot, existingIntents)
            val builtIntent = buildSessionIntent(
                accountId = accountId,
                before = currentProjected,
                after = mutation.character,
                command = command,
                working = working,
                createdAt = now,
            )

            // Reject multi-effect actions exceeding Spark's Rules budget BEFORE optimistic mutation.
            if (builtIntent != null) {
                builtIntent.targets.filter {
                    it.kind == TargetKind.FIELD && it.op == TargetOp.DELTA && it.key != "money"
                }.forEach { target ->
                    val beforeValue = jsonToPlainValue(target.baseValueJson)
                    require(computeFieldDelta(target.key, beforeValue, target.delta ?: 0,
                        target.minValue, target.maxValue).isSuccess) {
                        "Este recurso exige um ajuste explícito da Mestre."
                    }
                }
                validateAtomicSparkActionBudget(
                    targets = builtIntent.targets,
                    actionLabel = command.label.ifBlank { command.type.name },
                )
                localStore.upsertIntent(builtIntent.toRecord())
            }
            val opRecord = mutation.operation.copy(createdAt = now).toRecord().copy(
                dirty = mutation.operation.campaignId.isNotBlank(),
                lastSyncedAt = 0L,
            )
            localStore.upsertOperation(opRecord)
            reprojectSheetLocked(accountId, character.id)
            enqueued = true
        }
        if (enqueued && accountId != DEMO_ACCOUNT_ID) {
            schedule(accountId)
        }
        return enqueued
    }

    suspend fun enqueueDeliveryAcceptance(
        session: UserSession,
        character: Character,
        deliveryId: String,
        appliedCharacter: Character,
    ) {
        val accountId = session.uid
        ensureAccountProjected(accountId)
        val now = clock.nowMillis()
        localStore.withTransaction {
            val snapshot = ensureAccountSheetSnapshotLocked(accountId, character, now)
            val existingIntents = localStore.listIntentsForSheet(accountId, character.id)
                .map(SheetIntentRecord::toDomain)
            val working = computeWorkingRevisionsAndHeads(snapshot, existingIntents)
            val currentProjected = projectReplay(snapshot, existingIntents).projectedCharacter
            val after = appliedCharacter.copy(
                appliedDeliveryIds = (currentProjected.appliedDeliveryIds + deliveryId).distinct(),
                updatedAt = now,
                lastEditedBy = accountId,
                dirty = false,
            )
            val batches = SparkWireCodec.buildOperationBatches(
                before = currentProjected.toRecord(),
                after = after.toRecord(),
                operationIdPrefix = "delivery_${deliveryId}_${UUID.randomUUID().toString().take(8)}",
                actorUid = accountId,
                existingRevisions = working.fieldRevisions,
                sheetRevision = working.sheetRevision,
                updatedAt = now,
            )
            val allTargets = batches.flatMap { it.targets }
            require(allTargets.size <= 5) {
                "Entrega excede o limite atômico. Solicite um ajuste explícito à Mestre."
            }
            validateAtomicSparkActionBudget(allTargets, "Entrega")
            val root = allTargets.singleOrNull { it.kind == TargetKind.SHEET }
                ?: error("Entrega exige a confirmação da ficha na mesma transação.")
            val targets = listOf(root.copy(
                op = TargetOp.DELIVERY,
                valueJson = plainGson.toJson(mapOf("deliveryId" to deliveryId)),
            )) + allTargets.filter { it.kind != TargetKind.SHEET }
            localStore.upsertIntent(SheetIntent(
                operationId = UUID.randomUUID().toString(),
                accountId = accountId,
                sheetId = character.id,
                type = OperationType.ACCEPT_DELIVERY,
                targetKey = "sheet",
                targets = targets,
                dependsOnOperationId = working.targetHeads["sheet"] ?: working.pendingCreateOpId,
                baseRevision = working.sheetRevision,
                baseValueJson = deliveryId,
                desiredPayloadJson = "",
                status = SheetIntentStatus.PENDING,
                createdAt = now,
                updatedAt = now,
            ).toRecord())
            reprojectSheetLocked(accountId, character.id)
        }
        if (accountId != DEMO_ACCOUNT_ID) {
            schedule(accountId)
        }
    }

    suspend fun resolveIntentConflict(
        session: UserSession,
        conflictId: String,
        useRemote: Boolean,
        customValueJson: String? = null,
    ) {
        val accountId = session.uid
        ensureAccountProjected(accountId)
        val now = clock.nowMillis()
        var shouldSchedule = false
        localStore.withTransaction {
            val conflictRecord = localStore.getConflict(accountId, conflictId) ?: return@withTransaction
            val conflict = conflictRecord.toDomain()
            val resolutionStatus = when {
                useRemote -> ConflictResolutionStatus.RESOLVED_REMOTE
                customValueJson != null -> ConflictResolutionStatus.RESOLVED_CUSTOM
                else -> ConflictResolutionStatus.RESOLVED_LOCAL
            }
            localStore.upsertConflict(conflictRecord.copy(resolutionStatus = resolutionStatus.name))

            val sheetIntents = localStore.listIntentsForSheet(accountId, conflict.sheetId)
                .map(SheetIntentRecord::toDomain)
            val newOpId = UUID.randomUUID().toString()
            val fieldResolution = buildConflictResolutionIntent(
                conflict = conflict,
                newOperationId = newOpId,
                useRemote = useRemote,
                customValueJson = customValueJson,
                createdAt = now,
            )

            val original = sheetIntents.firstOrNull { it.operationId == conflict.operationId }
            val remaining = original?.effectiveTargets().orEmpty()
                .filterNot { it.canonicalTargetKey() == conflict.targetKey }
            val replacementTargets = remaining + fieldResolution?.effectiveTargets().orEmpty()
            val resolutionIntent = if (replacementTargets.isEmpty()) null else {
                (fieldResolution ?: requireNotNull(original)).copy(
                    operationId = newOpId,
                    targets = replacementTargets,
                    type = original?.type ?: OperationType.PATCH_FIELDS,
                    desiredPayloadJson = original?.desiredPayloadJson.orEmpty(),
                    payloadHash = computeCanonicalPayloadHash(
                        conflict.sheetId, original?.type ?: OperationType.PATCH_FIELDS,
                        replacementTargets.first().canonicalTargetKey(), replacementTargets,
                        original?.desiredPayloadJson.orEmpty(),
                    ),
                    targetKey = replacementTargets.first().canonicalTargetKey(),
                    dependsOnOperationId = original?.dependsOnOperationId,
                    status = SheetIntentStatus.PENDING,
                    error = "",
                    createdAt = now,
                    updatedAt = now,
                )
            }

            // Supersede the conflicted intent so it no longer blocks the queue.
            localStore.deleteIntent(accountId, conflict.operationId)

            // Unpause dependents: re-anchor to new resolution intent (if local/custom) or clear dependency.
            sheetIntents.filter { it.dependsOnOperationId == conflict.operationId }.forEach { dependent ->
                val updatedDep = dependent.copy(
                    dependsOnOperationId = resolutionIntent?.operationId,
                    status = if (dependent.status == SheetIntentStatus.PAUSED_DEPENDENCY) {
                        SheetIntentStatus.PENDING
                    } else {
                        dependent.status
                    },
                    error = "",
                    updatedAt = now,
                )
                localStore.upsertIntent(updatedDep.toRecord())
            }

            if (resolutionIntent != null) {
                localStore.upsertIntent(resolutionIntent.toRecord())
                shouldSchedule = true
            }
            reprojectSheetLocked(accountId, conflict.sheetId)
        }
        if (shouldSchedule && accountId != DEMO_ACCOUNT_ID) {
            schedule(accountId)
        }
    }

    suspend fun resolveLegacyConflict(
        session: UserSession,
        conflict: CharacterSyncConflict,
        remoteFieldIds: Set<String>,
    ) {
        val accountId = session.uid
        ensureAccountProjected(accountId)
        val openConflicts = localStore.listIntentsForSheet(accountId, conflict.local.id)
            .filter { it.status == SheetIntentStatus.CONFLICT.name }
        if (openConflicts.isEmpty()) return
        openConflicts.forEach { intent ->
            val conflictItemId = "${intent.operationId}::${intent.targetKey}"
            val useRemote = intent.targetKey.removePrefix("field:") in remoteFieldIds ||
                intent.targetKey in remoteFieldIds
            resolveIntentConflict(session, conflictItemId, useRemote = useRemote)
        }
    }

    suspend fun retryAccessDeniedDraft(session: UserSession, intentId: String) {
        val accountId = session.uid
        ensureAccountProjected(accountId)
        val now = clock.nowMillis()
        var sheetId = ""
        localStore.withTransaction {
            val record = localStore.getIntent(accountId, intentId) ?: return@withTransaction
            sheetId = record.sheetId
            if (record.status !in setOf(
                    SheetIntentStatus.ACCESS_DENIED.name,
                    SheetIntentStatus.REJECTED.name,
                    SheetIntentStatus.DRAFT.name,
                )
            ) {
                return@withTransaction
            }
            localStore.upsertIntent(
                record.copy(
                    status = SheetIntentStatus.PENDING.name,
                    error = "",
                    updatedAt = now,
                ),
            )
            // Unpause dependents so they wait cleanly on the retried draft.
            localStore.listIntentsForSheet(accountId, sheetId)
                .filter { it.dependsOnOperationId == intentId && it.status == SheetIntentStatus.PAUSED_DEPENDENCY.name }
                .forEach { dep ->
                    localStore.upsertIntent(
                        dep.copy(
                            status = SheetIntentStatus.PENDING.name,
                            error = "",
                            updatedAt = now,
                        ),
                    )
                }
            reprojectSheetLocked(accountId, sheetId)
        }
        if (sheetId.isNotBlank() && accountId != DEMO_ACCOUNT_ID) {
            schedule(accountId)
        }
    }

    suspend fun discardDraft(session: UserSession, intentId: String) {
        val accountId = session.uid
        ensureAccountProjected(accountId)
        val now = clock.nowMillis()
        localStore.withTransaction {
            val record = localStore.getIntent(accountId, intentId) ?: return@withTransaction
            val sheetId = record.sheetId
            localStore.deleteIntent(accountId, intentId)
            val targetId = "${intentId}::${record.targetKey}"
            localStore.deleteConflict(accountId, targetId)
            localStore.listIntentsForSheet(accountId, sheetId)
                .filter { it.dependsOnOperationId == intentId }
                .forEach { dep ->
                    localStore.upsertIntent(
                        dep.copy(
                            dependsOnOperationId = null,
                            status = if (dep.status == SheetIntentStatus.PAUSED_DEPENDENCY.name) {
                                SheetIntentStatus.PENDING.name
                            } else {
                                dep.status
                            },
                            error = "",
                            updatedAt = now,
                        ),
                    )
                }
            reprojectSheetLocked(accountId, sheetId)
        }
    }

    suspend fun recordConfirmedSheetLinkUpdate(
        accountId: String,
        sheetId: String,
        campaignId: String,
        newRevision: Long,
        updatedCharacter: Character? = null,
    ) {
        val now = clock.nowMillis()
        localStore.withTransaction {
            val existing = localStore.getAccountSheet(accountId, sheetId)?.toSnapshot()
            val baseChar = updatedCharacter ?: existing?.confirmedCharacter ?: localStore.getCharacter(sheetId)?.toDomain()
            if (baseChar != null) {
                val updatedSnap = (existing ?: AccountSheetSnapshot(accountId = accountId, sheetId = sheetId)).copy(
                    sheetRevision = newRevision,
                    confirmedCharacter = baseChar.copy(campaignId = normalizeCampaignId(campaignId), dirty = false),
                    updatedAt = now,
                )
                localStore.upsertAccountSheet(updatedSnap.toRecord())
                reprojectSheetLocked(accountId, sheetId)
            }
        }
    }

    suspend fun purgeSheetForNonOwner(accountId: String, sheetId: String) {
        localStore.withTransaction {
            localStore.deleteAccountSheet(accountId, sheetId)
            if (activeAccountId == accountId) {
                localStore.purgeCharacter(sheetId)
            }
        }
    }

    fun schedule(accountId: String? = activeAccountId ?: currentUidProvider()) {
        val uid = accountId ?: return
        if (uid.isBlank() || uid == DEMO_ACCOUNT_ID) return
        workScheduler.schedule(uid)
        scope.launch {
            runCatching { drain(uid) }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    errors.value = error.message
                }
        }
    }

    suspend fun drain(accountIdOverride: String? = null, targetSheetId: String? = null): Boolean = drainMutex.withLock {
        val uid = accountIdOverride ?: currentUidProvider() ?: return@withLock true
        if (uid.isBlank() || uid == DEMO_ACCOUNT_ID) return@withLock true
        // Unconditional auth gate (also for scheduled/WorkManager drains with an explicit uid):
        // account A's durable queue is never touched or driven unless A is the signed-in account.
        if (!isAccountCurrent(uid)) return@withLock true
        ensureAccountProjected(uid)
        if (!isAccountCurrent(uid)) return@withLock true
        val generation = sessionGeneration.get()
        syncing.value = true
        var allTransientSucceeded = true
        try {
            if (targetSheetId == null) {
                reconcileLegacyData(uid)
            }
            localStore.listAccountSheets(uid)
                .filter { it.sheetRevision == 0L && (targetSheetId == null || it.sheetId == targetSheetId) }
                .forEach { sheet ->
                    val intents = localStore.listIntentsForSheet(uid, sheet.sheetId)
                    if (intents.any {
                            it.type == OperationType.DELETE_SHEET.name && it.status != SheetIntentStatus.CONFIRMED.name
                        }
                    ) {
                        cancelUnpublishedSheet(uid, sheet.sheetId)
                    }
                }
            val rawIntents = if (targetSheetId != null) {
                localStore.listIntentsForSheet(uid, targetSheetId)
                    .filter { it.status == SheetIntentStatus.PENDING.name || it.status == SheetIntentStatus.SENDING.name }
            } else {
                localStore.listActiveIntents(uid)
            }
            val pending = orderIntentsTopologically(
                rawIntents.map(SheetIntentRecord::toDomain),
            )
            val blockedOps = mutableSetOf<String>()
            // Seed blocked ops from existing CONFLICT/REJECTED/ACCESS_DENIED intents.
            for (sheetId in pending.map(SheetIntent::sheetId).distinct()) {
                localStore.listIntentsForSheet(uid, sheetId)
                    .map(SheetIntentRecord::toDomain)
                    .filter {
                        it.status in setOf(
                            SheetIntentStatus.CONFLICT,
                            SheetIntentStatus.REJECTED,
                            SheetIntentStatus.ACCESS_DENIED,
                        )
                    }
                    .forEach { blockedOps += it.operationId }
            }

            for (queuedIntent in pending) {
                val intent = localStore.getIntent(uid, queuedIntent.operationId)?.toDomain() ?: continue
                if (!isStillCurrent(uid, generation)) return@withLock true
                // The client never sends MIGRATE_V2 receipts; only the admin migration tool may.
                if (intent.type == OperationType.MIGRATE_V2) {
                    blockedOps += intent.operationId
                    localStore.withTransaction {
                        localStore.upsertIntent(
                            intent.copy(
                                status = SheetIntentStatus.REJECTED,
                                error = "MIGRATE_V2 não é enviado pelo aplicativo.",
                                updatedAt = clock.nowMillis(),
                            ).toRecord(),
                        )
                        pauseDependentsLocked(uid, intent.sheetId, intent.operationId, blockedOps)
                    }
                    continue
                }
                val depId = intent.dependsOnOperationId
                if (!depId.isNullOrBlank()) {
                    val dep = localStore.getIntent(uid, depId)
                    val depStatus = dep?.status
                    val depFailed = depId in blockedOps || depStatus in setOf(
                        SheetIntentStatus.CONFLICT.name,
                        SheetIntentStatus.REJECTED.name,
                        SheetIntentStatus.ACCESS_DENIED.name,
                        SheetIntentStatus.PAUSED_DEPENDENCY.name,
                        SheetIntentStatus.DRAFT.name,
                    )
                    if (depFailed) {
                        blockedOps += intent.operationId
                        localStore.withTransaction {
                            localStore.upsertIntent(
                                intent.copy(
                                    status = SheetIntentStatus.PAUSED_DEPENDENCY,
                                    updatedAt = clock.nowMillis(),
                                ).toRecord(),
                            )
                            reprojectSheetLocked(uid, intent.sheetId)
                        }
                        continue
                    }
                    // Dependents run only after a CONFIRMED prerequisite receipt; a prerequisite that is
                    // still PENDING/SENDING (e.g. transient network failure) keeps this intent waiting
                    // while independent intents continue.
                    if (depStatus != SheetIntentStatus.CONFIRMED.name) {
                        continue
                    }
                }

                localStore.upsertIntent(
                    intent.copy(status = SheetIntentStatus.SENDING, updatedAt = clock.nowMillis()).toRecord(),
                )

                val outcome = try {
                    executeIntentTransaction(uid, intent)
                } catch (denied: SparkPermissionDeniedException) {
                    if (!isStillCurrent(uid, generation)) {
                        // Credentials changed underneath us: never classify A's draft with B's identity.
                        localStore.upsertIntent(intent.copy(status = SheetIntentStatus.PENDING).toRecord())
                        return@withLock true
                    }
                    blockedOps += intent.operationId
                    localStore.withTransaction {
                        localStore.upsertIntent(
                            intent.copy(
                                status = SheetIntentStatus.ACCESS_DENIED,
                                error = denied.message ?: "Sem permissão para sincronizar esta alteração.",
                                updatedAt = clock.nowMillis(),
                            ).toRecord(),
                        )
                        pauseDependentsLocked(uid, intent.sheetId, intent.operationId, blockedOps)
                        reprojectSheetLocked(uid, intent.sheetId)
                    }
                    continue
                } catch (transient: SparkTransientNetworkException) {
                    allTransientSucceeded = false
                    localStore.upsertIntent(
                        intent.copy(
                            status = SheetIntentStatus.PENDING,
                            error = transient.message.orEmpty(),
                            updatedAt = clock.nowMillis(),
                        ).toRecord(),
                    )
                    errors.value = transient.message
                    continue
                }

                if (!isStillCurrent(uid, generation) &&
                    outcome !is DrainTxOutcome.Committed &&
                    outcome !is DrainTxOutcome.AlreadyCommitted
                ) {
                    localStore.upsertIntent(intent.copy(status = SheetIntentStatus.PENDING).toRecord())
                    return@withLock true
                }

                when (outcome) {
                    is DrainTxOutcome.Committed -> {
                        localStore.withTransaction {
                            localStore.upsertIntent(
                                intent.copy(
                                    status = SheetIntentStatus.CONFIRMED,
                                    error = "",
                                    updatedAt = outcome.serverTimestamp,
                                ).toRecord(),
                            )
                            applyCommittedTargetsToAccountSheetLocked(
                                accountId = uid,
                                intent = intent,
                                resolvedTargets = outcome.resolvedTargets,
                                serverTimestamp = outcome.serverTimestamp,
                            )
                            rebindDependentsLocked(intent, outcome.resolvedTargets)
                            reprojectSheetLocked(uid, intent.sheetId)
                        }
                        if (intent.type == OperationType.CREATE_SHEET) {
                            localStore.getAccountSheet(uid, intent.sheetId)?.let { createdSnap ->
                                if (createdSnap.localPhotoUri.isNotBlank()) {
                                    val docs = SparkWireCodec.toSparkDocuments(
                                        record = createdSnap.toSnapshot().confirmedCharacter.toRecord()
                                            .copy(photoUri = createdSnap.localPhotoUri),
                                        updatedBy = uid,
                                        updatedAt = outcome.serverTimestamp,
                                    )
                                    docs.mediaAvatar?.let { uploadAvatarIfChanged(intent.sheetId, it) }
                                }
                            }
                        }
                    }
                    is DrainTxOutcome.AlreadyCommitted -> {
                        when (outcome.receipt.verifyAgainst(intent)) {
                            IdempotencyMatchResult.MATCHED_COMMITTED -> {
                                // Lost acknowledgement: the commit happened but we never saw it. Pull the
                                // committed remote state (values + revisions) into the confirmed snapshot
                                // BEFORE acknowledging, otherwise replay would roll the value back and
                                // later dependents would raise false conflicts.
                                val fetched = try {
                                    fetchRemoteSnapshot(uid, intent.sheetId)
                                } catch (error: Exception) {
                                    if (error is CancellationException) throw error
                                    null
                                }
                                if (fetched == null) {
                                    allTransientSucceeded = false
                                    localStore.upsertIntent(
                                        intent.copy(
                                            status = SheetIntentStatus.PENDING,
                                            updatedAt = clock.nowMillis(),
                                        ).toRecord(),
                                    )
                                } else {
                                    localStore.withTransaction {
                                        localStore.upsertAccountSheet(fetched.toRecord())
                                        localStore.upsertIntent(
                                            intent.copy(
                                                status = SheetIntentStatus.CONFIRMED,
                                                error = "",
                                                updatedAt = clock.nowMillis(),
                                            ).toRecord(),
                                        )
                                        rebindDependentsLocked(intent, outcome.receipt.targets)
                                        reprojectSheetLocked(uid, intent.sheetId)
                                    }
                                }
                            }
                            IdempotencyMatchResult.HASH_MISMATCH,
                            IdempotencyMatchResult.OUTCOME_NOT_COMMITTED -> {
                                blockedOps += intent.operationId
                                localStore.withTransaction {
                                    localStore.upsertIntent(
                                        intent.copy(
                                            status = SheetIntentStatus.REJECTED,
                                            error = "Idempotency mismatch: operationId reused with different payload.",
                                            updatedAt = clock.nowMillis(),
                                        ).toRecord(),
                                    )
                                    pauseDependentsLocked(uid, intent.sheetId, intent.operationId, blockedOps)
                                    reprojectSheetLocked(uid, intent.sheetId)
                                }
                            }
                        }
                    }
                    is DrainTxOutcome.IdempotencyMismatch -> {
                        blockedOps += intent.operationId
                        localStore.withTransaction {
                            localStore.upsertIntent(
                                intent.copy(
                                    status = SheetIntentStatus.REJECTED,
                                    error = outcome.reason,
                                    updatedAt = clock.nowMillis(),
                                ).toRecord(),
                            )
                            pauseDependentsLocked(uid, intent.sheetId, intent.operationId, blockedOps)
                            reprojectSheetLocked(uid, intent.sheetId)
                        }
                    }
                    is DrainTxOutcome.Tombstoned -> {
                        blockedOps += intent.operationId
                        localStore.withTransaction {
                            localStore.upsertIntent(
                                intent.copy(
                                    status = SheetIntentStatus.REJECTED,
                                    error = outcome.reason,
                                    updatedAt = clock.nowMillis(),
                                ).toRecord(),
                            )
                            val snap = localStore.getAccountSheet(uid, intent.sheetId)?.toSnapshot()
                            if (snap != null) {
                                val updatedSnap = if (outcome.isSheet) {
                                    snap.copy(
                                        sheetDeleted = true,
                                        confirmedCharacter = snap.confirmedCharacter.copy(deleted = true),
                                        updatedAt = clock.nowMillis(),
                                    )
                                } else {
                                    snap.copy(
                                        tombstonedItemIds = snap.tombstonedItemIds + outcome.itemId,
                                        updatedAt = clock.nowMillis(),
                                    )
                                }
                                localStore.upsertAccountSheet(updatedSnap.toRecord())
                            }
                            pauseDependentsLocked(uid, intent.sheetId, intent.operationId, blockedOps)
                            reprojectSheetLocked(uid, intent.sheetId)
                        }
                    }
                    is DrainTxOutcome.Rejected -> {
                        blockedOps += intent.operationId
                        localStore.withTransaction {
                            localStore.upsertIntent(
                                intent.copy(
                                    status = SheetIntentStatus.REJECTED,
                                    error = outcome.reason,
                                    updatedAt = clock.nowMillis(),
                                ).toRecord(),
                            )
                            pauseDependentsLocked(uid, intent.sheetId, intent.operationId, blockedOps)
                            reprojectSheetLocked(uid, intent.sheetId)
                        }
                    }
                    is DrainTxOutcome.RevisionConflict -> {
                        blockedOps += intent.operationId
                        localStore.withTransaction {
                            localStore.upsertConflicts(outcome.conflicts.map(SheetConflictItem::toRecord))
                            localStore.upsertIntent(
                                intent.copy(
                                    status = SheetIntentStatus.CONFLICT,
                                    error = outcome.conflicts.firstOrNull()?.let {
                                        "Conflito de revisão em ${it.targetKey} (base ${it.baseRevision}, remota ${it.remoteRevision})."
                                    } ?: "Conflito de revisão.",
                                    updatedAt = clock.nowMillis(),
                                ).toRecord(),
                            )
                            pauseDependentsLocked(uid, intent.sheetId, intent.operationId, blockedOps)
                            reprojectSheetLocked(uid, intent.sheetId)
                        }
                        // Refresh full remote state in background / right away so UI shows latest remote.
                        runCatching { refreshSheetFromRemote(uid, intent.sheetId) }
                    }
                }
            }
            if (allTransientSucceeded) errors.value = null
            allTransientSucceeded
        } finally {
            syncing.value = false
        }
    }

    private suspend fun executeIntentTransaction(uid: String, intent: SheetIntent): DrainTxOutcome {
        if (!isAccountCurrent(uid)) return DrainTxOutcome.Rejected("A conta ativa mudou.")
        val effectiveTargets = intent.effectiveTargets()
        return remoteGateway.runTransaction(intent.sheetId) { ctx ->
            if (!isAccountCurrent(uid)) return@runTransaction DrainTxOutcome.Rejected("A conta ativa mudou.")
            // 1. Check operations/{opId} FIRST for idempotency!
            val existingOp = ctx.getOperation(intent.sheetId, intent.operationId)
            if (existingOp != null) {
                val receipt = parseOperationReceipt(existingOp)
                return@runTransaction DrainTxOutcome.AlreadyCommitted(receipt)
            }

            // 2. Read all required target docs BEFORE any writes (Firestore transaction requirement).
            val remoteRoot = ctx.getRoot(intent.sheetId)
            val remoteFields = mutableMapOf<String, Map<String, Any?>?>()
            val remoteItems = mutableMapOf<String, Map<String, Any?>?>()
            val remoteItemFields = mutableMapOf<Pair<String, String>, Map<String, Any?>?>()

            // A new root has no readable children yet; Rules deny those pre-creation reads.
            val readableTargets = if (remoteRoot == null && intent.type == OperationType.CREATE_SHEET) {
                emptyList()
            } else {
                effectiveTargets
            }
            readableTargets.forEach { target ->
                when (target.kind) {
                    TargetKind.SHEET -> Unit
                    TargetKind.FIELD -> remoteFields[target.key] = ctx.getField(intent.sheetId, target.key)
                    TargetKind.ITEM -> remoteItems[target.itemId] = ctx.getItem(intent.sheetId, target.itemId)
                    TargetKind.ITEM_FIELD -> {
                        if (target.itemId !in remoteItems) {
                            remoteItems[target.itemId] = ctx.getItem(intent.sheetId, target.itemId)
                        }
                        remoteItemFields[target.itemId to target.key] =
                            ctx.getItemField(intent.sheetId, target.itemId, target.key)
                    }
                }
            }

            val deliveryId = if (intent.type == OperationType.ACCEPT_DELIVERY) {
                intent.baseValueJson.ifBlank {
                    val sheetTarget = effectiveTargets.firstOrNull { it.kind == TargetKind.SHEET }
                    (jsonToPlainValue(sheetTarget?.valueJson.orEmpty()) as? Map<*, *>)?.get("deliveryId")?.toString().orEmpty()
                }
            } else {
                ""
            }
            val remoteDelivery = if (deliveryId.isNotBlank()) ctx.getDelivery(deliveryId) else null

            // 3. Validate tombstones, revisions, and DELTA bounds!
            val isSheetDeleted = (remoteRoot?.get("deleted") as? Boolean) == true
            val isCreateSheet = intent.type == OperationType.CREATE_SHEET
            if (!isCreateSheet && remoteRoot == null) {
                return@runTransaction DrainTxOutcome.Rejected("Ficha remota não encontrada.")
            }

            val resolvedTargets = mutableListOf<SparkTargetSpec>()
            val pendingWrites = mutableListOf<() -> Unit>()
            val serverTs = ctx.serverTimestampToken()
            val nowMs = ctx.currentTimestampMillis()

            for (target in effectiveTargets) {
                if (isSheetDeleted && target.op != TargetOp.RESTORE) {
                    return@runTransaction DrainTxOutcome.Tombstoned(
                        reason = "A ficha foi excluída remotamente.",
                        isSheet = true,
                    )
                }

                when (target.kind) {
                    TargetKind.SHEET -> {
                        val remoteRev = asLong(remoteRoot?.get("revision")) ?: 0L
                        if (target.op != TargetOp.CREATE && target.expectedRevision != remoteRev) {
                            return@runTransaction DrainTxOutcome.RevisionConflict(
                                conflicts = listOf(
                                    buildConflict(
                                        intent = intent,
                                        target = target,
                                        remoteRev = remoteRev,
                                        remoteValue = remoteRoot.orEmpty(),
                                    ),
                                ),
                                remoteRoot = remoteRoot,
                            )
                        }
                        val nextRev = remoteRev + 1L
                        val updatedRoot = buildUpdatedRootDoc(
                            sheetId = intent.sheetId,
                            actorUid = uid,
                            remoteRoot = remoteRoot,
                            target = target,
                            operationId = intent.operationId,
                            nextRevision = nextRev,
                            serverTimestampToken = serverTs,
                            deliveryId = deliveryId,
                        )
                        val resolved = target.copy(expectedRevision = remoteRev, nextRevision = nextRev)
                        resolvedTargets += resolved
                        pendingWrites += { ctx.setRoot(intent.sheetId, updatedRoot) }
                    }
                    TargetKind.FIELD -> {
                        val remoteDoc = remoteFields[target.key]
                        val remoteRev = asLong(remoteDoc?.get("revision")) ?: 0L
                        if (target.op == TargetOp.DELTA) {
                            if (remoteDoc == null) {
                                return@runTransaction DrainTxOutcome.Rejected("Campo ${target.key} ausente no servidor.")
                            }
                            val delta = target.delta ?: 0
                            val prevVal = remoteDoc["value"]
                            val nextValResult = computeFieldDelta(target.key, prevVal, delta, target.minValue, target.maxValue)
                            if (nextValResult.isFailure) {
                                return@runTransaction DrainTxOutcome.Rejected(
                                    nextValResult.exceptionOrNull()?.message ?: "Saldo insuficiente em ${target.key}.",
                                )
                            }
                            val nextVal = nextValResult.getOrThrow()
                            val nextRev = remoteRev + 1L
                            val resolved = target.copy(
                                expectedRevision = remoteRev,
                                nextRevision = nextRev,
                                baseValueJson = plainGson.toJson(prevVal),
                                valueJson = plainGson.toJson(nextVal),
                            )
                            resolvedTargets += resolved
                            pendingWrites += {
                                ctx.setField(
                                    intent.sheetId,
                                    target.key,
                                    linkedMapOf(
                                        "key" to target.key,
                                        "value" to nextVal,
                                        "revision" to nextRev,
                                        "lastOperationId" to intent.operationId,
                                        "updatedBy" to uid,
                                        "updatedAt" to serverTs,
                                    ),
                                )
                            }
                        } else {
                            if (target.expectedRevision != remoteRev) {
                                return@runTransaction DrainTxOutcome.RevisionConflict(
                                    conflicts = listOf(
                                        buildConflict(
                                            intent = intent,
                                            target = target,
                                            remoteRev = remoteRev,
                                            remoteValue = remoteDoc?.get("value"),
                                        ),
                                    ),
                                    remoteRoot = remoteRoot,
                                )
                            }
                            val nextRev = remoteRev + 1L
                            val desiredPlain = jsonToPlainValue(target.valueJson ?: intent.desiredPayloadJson)
                            val resolved = target.copy(
                                expectedRevision = remoteRev,
                                nextRevision = nextRev,
                                baseValueJson = if (remoteDoc == null) null else plainGson.toJson(remoteDoc["value"]),
                            )
                            resolvedTargets += resolved
                            pendingWrites += {
                                ctx.setField(
                                    intent.sheetId,
                                    target.key,
                                    linkedMapOf(
                                        "key" to target.key,
                                        "value" to desiredPlain,
                                        "revision" to nextRev,
                                        "lastOperationId" to intent.operationId,
                                        "updatedBy" to uid,
                                        "updatedAt" to serverTs,
                                    ),
                                )
                            }
                        }
                    }
                    TargetKind.ITEM -> {
                        val remoteItem = remoteItems[target.itemId]
                        val itemDeleted = (remoteItem?.get("deleted") as? Boolean) == true
                        if (itemDeleted && target.op != TargetOp.RESTORE) {
                            return@runTransaction DrainTxOutcome.Tombstoned(
                                reason = "Item ${target.itemId} foi excluído remotamente.",
                                isSheet = false,
                                itemId = target.itemId,
                            )
                        }
                        val remoteRev = asLong(remoteItem?.get("revision")) ?: 0L
                        if (target.expectedRevision != remoteRev) {
                            return@runTransaction DrainTxOutcome.RevisionConflict(
                                conflicts = listOf(
                                    buildConflict(
                                        intent = intent,
                                        target = target,
                                        remoteRev = remoteRev,
                                        remoteValue = remoteItem ?: mapOf("deleted" to itemDeleted),
                                    ),
                                ),
                                remoteRoot = remoteRoot,
                            )
                        }
                        val nextRev = remoteRev + 1L
                        val kind = expectedItemKindFromId(target.itemId)
                        val deletedFlag = target.op == TargetOp.TOMBSTONE
                        val resolved = target.copy(expectedRevision = remoteRev, nextRevision = nextRev)
                        resolvedTargets += resolved
                        pendingWrites += {
                            ctx.setItem(
                                intent.sheetId,
                                target.itemId,
                                linkedMapOf(
                                    "id" to target.itemId,
                                    "kind" to kind,
                                    "deleted" to deletedFlag,
                                    "revision" to nextRev,
                                    "lastOperationId" to intent.operationId,
                                    "updatedBy" to uid,
                                    "updatedAt" to serverTs,
                                ),
                            )
                        }
                    }
                    TargetKind.ITEM_FIELD -> {
                        val remoteItem = remoteItems[target.itemId]
                        if ((remoteItem?.get("deleted") as? Boolean) == true && target.op != TargetOp.RESTORE) {
                            return@runTransaction DrainTxOutcome.Tombstoned(
                                reason = "Item ${target.itemId} foi excluído remotamente.",
                                isSheet = false,
                                itemId = target.itemId,
                            )
                        }
                        val remoteDoc = remoteItemFields[target.itemId to target.key]
                        val remoteRev = asLong(remoteDoc?.get("revision")) ?: 0L
                        if (target.op == TargetOp.DELTA) {
                            if (remoteDoc == null) {
                                return@runTransaction DrainTxOutcome.Rejected(
                                    "Campo ${target.key} de ${target.itemId} ausente no servidor.",
                                )
                            }
                            val delta = target.delta ?: 0
                            val rawPrev = remoteDoc["value"]
                            val prevMap = (rawPrev as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value }
                            val prevVal = if (prevMap != null) {
                                asLong(prevMap["quantity"])
                            } else {
                                asLong(rawPrev)
                            } ?: return@runTransaction DrainTxOutcome.Rejected("Valor numérico inválido.")
                            val minVal = (target.minValue ?: 0).toLong()
                            val maxVal = (target.maxValue ?: Int.MAX_VALUE).toLong()
                            val nextVal = prevVal + delta
                            if (nextVal < minVal || nextVal > maxVal) {
                                return@runTransaction DrainTxOutcome.Rejected(
                                    "Quantidade insuficiente para ${target.itemId} (atual=$prevVal, delta=$delta).",
                                )
                            }
                            val nextFieldVal: Any = if (prevMap != null) prevMap + ("quantity" to nextVal) else nextVal
                            val nextRev = remoteRev + 1L
                            val resolved = target.copy(
                                expectedRevision = remoteRev,
                                nextRevision = nextRev,
                                baseValueJson = plainGson.toJson(rawPrev),
                                valueJson = plainGson.toJson(nextFieldVal),
                            )
                            resolvedTargets += resolved
                            pendingWrites += {
                                ctx.setItemField(
                                    intent.sheetId,
                                    target.itemId,
                                    target.key,
                                    linkedMapOf(
                                        "key" to target.key,
                                        "value" to nextFieldVal,
                                        "revision" to nextRev,
                                        "lastOperationId" to intent.operationId,
                                        "updatedBy" to uid,
                                        "updatedAt" to serverTs,
                                    ),
                                )
                            }
                        } else {
                            if (target.expectedRevision != remoteRev) {
                                return@runTransaction DrainTxOutcome.RevisionConflict(
                                    conflicts = listOf(
                                        buildConflict(
                                            intent = intent,
                                            target = target,
                                            remoteRev = remoteRev,
                                            remoteValue = remoteDoc?.get("value"),
                                        ),
                                    ),
                                    remoteRoot = remoteRoot,
                                )
                            }
                            val nextRev = remoteRev + 1L
                            val desiredPlain = jsonToPlainValue(target.valueJson ?: intent.desiredPayloadJson)
                            val resolved = target.copy(
                                expectedRevision = remoteRev,
                                nextRevision = nextRev,
                                baseValueJson = if (remoteDoc == null) null else plainGson.toJson(remoteDoc["value"]),
                            )
                            resolvedTargets += resolved
                            pendingWrites += {
                                ctx.setItemField(
                                    intent.sheetId,
                                    target.itemId,
                                    target.key,
                                    linkedMapOf(
                                        "key" to target.key,
                                        "value" to desiredPlain,
                                        "revision" to nextRev,
                                        "lastOperationId" to intent.operationId,
                                        "updatedBy" to uid,
                                        "updatedAt" to serverTs,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            // 4. Write immutable operations/{opId} + targets + optional delivery state atomically!
            val preconditions = linkedMapOf<String, Any?>("payloadHash" to intent.payloadHash)
            if (intent.type == OperationType.SESSION_ACTION) {
                val metadata = jsonToPlainValue(intent.desiredPayloadJson) as? Map<*, *>
                listOf("actionType", "abilityItemId", "costResource").forEach { key ->
                    (metadata?.get(key) as? String)?.let { preconditions[key] = it }
                }
            }
            if (deliveryId.isNotBlank()) {
                preconditions["deliveryId"] = deliveryId
                if (remoteDelivery != null && remoteDelivery["state"] != "ACCEPTED") {
                    val acceptedDelivery = LinkedHashMap<String, Any?>(remoteDelivery).apply {
                        put("state", "ACCEPTED")
                        put("updatedAt", nowMs)
                    }
                    ctx.setDelivery(deliveryId, acceptedDelivery)
                }
            }
            val receiptTargets = resolvedTargets.map { target ->
                linkedMapOf<String, Any?>().apply {
                    put("kind", target.kind.name)
                    put("itemId", target.itemId)
                    put("key", target.key)
                    put("op", target.op.name)
                    put("expectedRevision", target.expectedRevision)
                    put("nextRevision", target.nextRevision)
                    if (target.op != TargetOp.CREATE && target.baseValueJson != null) {
                        put("baseValue", jsonToPlainValue(target.baseValueJson))
                    }
                    if (target.delta != null) {
                        put("delta", target.delta)
                    }
                    if (target.valueJson != null) {
                        put("desiredValue", jsonToPlainValue(target.valueJson))
                    }
                }
            }
            val opDoc = linkedMapOf<String, Any?>(
                "id" to intent.operationId,
                "actorUid" to uid,
                "type" to intent.type.name,
                "outcome" to "COMMITTED",
                "schemaVersion" to SparkWireCodec.SCHEMA_VERSION,
                "createdAt" to serverTs,
                "targets" to receiptTargets,
                "preconditions" to preconditions,
            )
            ctx.setOperation(intent.sheetId, intent.operationId, opDoc)
            pendingWrites.forEach { it.invoke() }

            DrainTxOutcome.Committed(
                resolvedTargets = resolvedTargets,
                serverTimestamp = nowMs,
                appliedDeliveryId = deliveryId.takeIf(String::isNotBlank),
            )
        }
    }

    suspend fun sync(session: UserSession): List<CharacterSyncConflict> {
        val uid = session.uid
        if (uid.isBlank() || uid == DEMO_ACCOUNT_ID) return emptyList()
        if (!isAccountCurrent(uid)) return emptyList()
        ensureAccountProjected(uid)
        drain(uid)

        val ownedCampaignIds = localStore.listOwnedActiveCampaignIds(uid)
        val accessibleIds = remoteGateway.fetchAccessibleSheetIds(uid, ownedCampaignIds, session.isAdmin)

        // Bounded cloud reads: a sheet is only re-downloaded in full when it is new locally or its
        // head (root revision / latest receipt) moved; unchanged sheets cost two small reads.
        coroutineScope {
            accessibleIds.map { sheetId ->
                async { runCatching { refreshDiscoveredSheet(uid, sheetId) } }
            }.awaitAll()
        }

        // Remove clean sheets that are no longer accessible (e.g. unlinked by player).
        if (!isAccountCurrent(uid)) return emptyList()
        val existingSheets = localStore.listAccountSheets(uid)
        for (sheet in existingSheets) {
            if (sheet.sheetId !in accessibleIds) {
                val hasPending = localStore.listIntentsForSheet(uid, sheet.sheetId).any {
                    it.status != SheetIntentStatus.CONFIRMED.name
                }
                if (!hasPending) {
                    localStore.deleteAccountSheet(uid, sheet.sheetId)
                    if (activeAccountId == uid) localStore.purgeCharacter(sheet.sheetId)
                }
            }
        }
        return emptyList()
    }

    suspend fun syncCharacter(session: UserSession, characterId: String): List<CharacterSyncConflict> {
        val uid = session.uid
        if (uid.isBlank() || uid == DEMO_ACCOUNT_ID || characterId.isBlank()) return emptyList()
        if (!isAccountCurrent(uid)) return emptyList()
        ensureAccountProjected(uid)
        drain(uid, targetSheetId = characterId)
        refreshDiscoveredSheet(uid, characterId)
        return emptyList()
    }

    private suspend fun refreshDiscoveredSheet(accountId: String, sheetId: String) {
        val existing = localStore.getAccountSheet(accountId, sheetId)?.toSnapshot()
        if (existing != null && LAST_OPERATION_MARKER in existing.fieldRevisions) {
            val head = remoteGateway.fetchSheetHead(sheetId)
            if (head != null && existing.sheetRevision == head.rootRevision &&
                existing.sheetDeleted == head.deleted &&
                existing.fieldRevisions[LAST_OPERATION_MARKER] == head.latestOperationAtMs
            ) {
                return
            }
        }
        refreshSheetFromRemote(accountId, sheetId)
    }

    /**
     * Downloads the complete committed state of [sheetId] as a confirmed snapshot for [accountId].
     * Returns null when the sheet no longer exists remotely. Throws if [accountId] is not the
     * signed-in account.
     */
    private suspend fun fetchRemoteSnapshot(accountId: String, sheetId: String): AccountSheetSnapshot? {
        check(isAccountCurrent(accountId)) { "A conta ativa mudou durante a sincronização." }
        val head = remoteGateway.fetchSheetHead(sheetId)
        val docs = remoteGateway.fetchSheetDocuments(sheetId) ?: return null

        val existing = localStore.getAccountSheet(accountId, sheetId)
        val preservedLocalUri = existing?.localPhotoUri.orEmpty()
        val decodedRecord = SparkWireCodec.fromSparkDocuments(
            docs.copy(localPhotoUri = preservedLocalUri),
        ).copy(id = sheetId, dirty = false)
        val decodedDomain = decodedRecord.toDomain()

        val fieldRevisions = linkedMapOf<String, Long>()
        docs.fields.forEach { (key, doc) ->
            asLong(doc["revision"])?.let { fieldRevisions["field:$key"] = it }
        }
        val tombstones = linkedSetOf<String>()
        docs.items.forEach { (itemId, bundle) ->
            asLong(bundle.document["revision"])?.let { fieldRevisions["item:$itemId"] = it }
            if ((bundle.document["deleted"] as? Boolean) == true) {
                tombstones += itemId
            }
            bundle.fields.forEach { (fieldKey, fDoc) ->
                asLong(fDoc["revision"])?.let { fieldRevisions["item:$itemId/field:$fieldKey"] = it }
            }
        }
        head?.let { fieldRevisions[LAST_OPERATION_MARKER] = it.latestOperationAtMs }
        val sheetRev = asLong(docs.root["revision"]) ?: 1L
        return AccountSheetSnapshot(
            accountId = accountId,
            sheetId = sheetId,
            sheetRevision = sheetRev,
            fieldRevisions = fieldRevisions,
            confirmedCharacter = decodedDomain,
            tombstonedItemIds = tombstones,
            sheetDeleted = decodedDomain.deleted,
            localPhotoUri = preservedLocalUri,
            updatedAt = clock.nowMillis(),
        )
    }

    suspend fun refreshSheetFromRemote(accountId: String, sheetId: String): Boolean {
        if (!isAccountCurrent(accountId)) return false
        val snapshot = fetchRemoteSnapshot(accountId, sheetId)
        if (snapshot == null) {
            localStore.withTransaction {
                val hasPending = localStore.listIntentsForSheet(accountId, sheetId)
                    .any { it.status != SheetIntentStatus.CONFIRMED.name }
                if (!hasPending) {
                    localStore.deleteAccountSheet(accountId, sheetId)
                    if (activeAccountId == accountId) localStore.purgeCharacter(sheetId)
                }
            }
            return false
        }
        if (!isAccountCurrent(accountId)) return false
        localStore.withTransaction {
            localStore.upsertAccountSheet(snapshot.toRecord())
            reprojectSheetLocked(accountId, sheetId)
        }
        return true
    }

    fun observeOpenSheet(sheetId: String): Flow<Unit> = kotlinx.coroutines.flow.flow {
        val uid = currentUidProvider() ?: return@flow
        if (!isAccountCurrent(uid) || uid == DEMO_ACCOUNT_ID) return@flow
        suspend fun refresh() {
            try {
                if (isAccountCurrent(uid)) refreshSheetFromRemote(uid, sheetId)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                errors.value = error.message
            }
        }
        refresh()
        remoteGateway.observeSheetReceipts(sheetId, uid).collect {
            refresh()
            emit(Unit)
        }
    }

    fun observeRemoteChanges(session: UserSession, campaignIds: Set<String>): Flow<RemoteSyncEvent> {
        if (session.uid.isBlank() || session.uid == DEMO_ACCOUNT_ID) return flowOf(RemoteSyncEvent())
        return kotlinx.coroutines.flow.flow {
            val ownedCampaignIds = localStore.listOwnedActiveCampaignIds(session.uid)
                .filter { campaignIds.isEmpty() || it in campaignIds }
                .toSet()
            remoteGateway.observeRemoteChanges(session.uid, ownedCampaignIds, session.isAdmin).collect { emit(it) }
        }
    }

    suspend fun setFieldPresence(
        session: UserSession,
        characterId: String,
        fieldKey: String?,
        active: Boolean,
    ) {
        val uid = session.uid
        if (characterId.isBlank() || uid.isBlank() || uid == DEMO_ACCOUNT_ID) return
        val presenceId = "${uid}_$connectionId"
        if (!active || fieldKey.isNullOrBlank()) {
            lastPresenceState.remove(characterId)
            remoteGateway.deletePresence(characterId, presenceId)
            return
        }
        val now = clock.nowMillis()
        val previous = lastPresenceState[characterId]
        if (previous != null && previous.first == fieldKey && now - previous.second < PRESENCE_HEARTBEAT_THROTTLE_MS) {
            return
        }
        lastPresenceState[characterId] = fieldKey to now
        val payload = linkedMapOf<String, Any?>(
            "uid" to uid,
            "connectionId" to connectionId,
            "fieldKey" to fieldKey.take(200),
            "label" to fieldKey.take(100),
            "updatedAt" to now,
            "expiresAt" to Timestamp(Date(now + PRESENCE_TTL_MS)),
        )
        runCatching { remoteGateway.writePresence(characterId, presenceId, payload) }
    }

    fun observeFieldPresence(session: UserSession, characterId: String): Flow<Map<String, String>> {
        val uid = session.uid
        if (characterId.isBlank() || uid.isBlank() || uid == DEMO_ACCOUNT_ID) return flowOf(emptyMap())
        val ownPresenceId = "${uid}_$connectionId"
        return remoteGateway.observePresence(characterId, ownPresenceId, uid)
    }

    private suspend fun uploadAvatarIfChanged(sheetId: String, mediaDoc: Map<String, Any?>) {
        val payloadFingerprint = mediaDoc["dataUri"]?.toString() ?: mediaDoc["bytesBase64"]?.toString() ?: return
        val byteLen = payloadFingerprint.toByteArray(Charsets.UTF_8).size
        if (byteLen > SparkWireCodec.MAX_AVATAR_THUMBNAIL_BYTES) return
        if (lastUploadedAvatarHash[sheetId] == payloadFingerprint) return
        runCatching {
            remoteGateway.writeMediaAvatar(sheetId, mediaDoc)
            lastUploadedAvatarHash[sheetId] = payloadFingerprint
        }
    }

    private suspend fun reconcileLegacyData(accountId: String) {
        val snapshotBySheet = localStore.listAccountSheets(accountId).associateBy { it.sheetId }

        // 1. Reconcile legacy MIGRATION_32_33 envelopes in sheet_intents (targetKey == "sheet" or JSON envelope).
        for (sheetId in (snapshotBySheet.keys + localStore.listActiveIntents(accountId).map { it.sheetId }).distinct()) {
            val intents = localStore.listIntentsForSheet(accountId, sheetId).map(SheetIntentRecord::toDomain)
            val snapshot = snapshotBySheet[sheetId]?.toSnapshot()
            for (intent in intents) {
                if (isLegacyMigratedIntent(intent)) {
                    val reconciled = reconcileLegacyIntent(intent, snapshot)
                    localStore.withTransaction {
                        if (reconciled.isEmpty()) {
                            localStore.upsertIntent(
                                intent.copy(
                                    status = SheetIntentStatus.REJECTED,
                                    error = "Envelope legado v32 inválido para reconciliação automática.",
                                    updatedAt = clock.nowMillis(),
                                ).toRecord(),
                            )
                        } else {
                            localStore.upsertIntents(reconciled.map(SheetIntent::toRecord))
                        }
                        reprojectSheetLocked(accountId, sheetId)
                    }
                }
            }
        }

        // 2. Reconcile any un-migrated v32 sheet_outbox rows if present.
        val legacyOutbox = localStore.listLegacyOutbox(accountId)
        for (row in legacyOutbox) {
            if (localStore.getIntent(accountId, row.operationId) == null) {
                val type = if (row.envelopeJson.contains("\"SESSION\"")) {
                    OperationType.SESSION_ACTION
                } else {
                    OperationType.PATCH_FIELDS
                }
                val syntheticIntent = SheetIntent(
                    operationId = row.operationId,
                    accountId = row.accountId,
                    sheetId = row.sheetId,
                    type = type,
                    targetKey = "sheet",
                    baseValueJson = row.envelopeJson,
                    desiredPayloadJson = row.optimisticJson.ifBlank { row.envelopeJson },
                    status = SheetIntentStatus.PENDING,
                    error = row.error,
                    createdAt = row.sequence,
                    updatedAt = row.sequence,
                )
                val snapshot = snapshotBySheet[row.sheetId]?.toSnapshot()
                val reconciled = reconcileLegacyIntent(syntheticIntent, snapshot)
                localStore.withTransaction {
                    if (reconciled.isNotEmpty()) {
                        localStore.upsertIntents(reconciled.map(SheetIntent::toRecord))
                    } else {
                        localStore.upsertIntent(
                            syntheticIntent.copy(
                                status = SheetIntentStatus.REJECTED,
                                error = "Envelope legado v32 inválido.",
                            ).toRecord(),
                        )
                    }
                    localStore.markLegacyOutboxStatus(row.operationId, "MIGRATED_V33")
                    reprojectSheetLocked(accountId, row.sheetId)
                }
            }
        }

        // 3. Reconcile any legacy dirty characters owned by accountId that have no intents yet.
        val allChars = localStore.allCharacters()
        for (charRecord in allChars) {
            if (charRecord.dirty && charRecord.ownerId == accountId) {
                val existingIntents = localStore.listIntentsForSheet(accountId, charRecord.id)
                if (existingIntents.isEmpty()) {
                    val snapshot = snapshotBySheet[charRecord.id]?.toSnapshot()
                    val now = clock.nowMillis()
                    val batches = SparkWireCodec.buildOperationBatches(
                        before = snapshot?.confirmedCharacter?.toRecord(),
                        after = charRecord.copy(dirty = false),
                        operationIdPrefix = UUID.randomUUID().toString(),
                        actorUid = accountId,
                        existingRevisions = snapshot?.fieldRevisions.orEmpty(),
                        sheetRevision = snapshot?.sheetRevision ?: 0L,
                        updatedAt = now,
                    )
                    if (batches.isNotEmpty()) {
                        val intents = batchesToIntents(batches, accountId, charRecord.id, null, now)
                        localStore.withTransaction {
                            if (snapshot == null) {
                                localStore.upsertAccountSheet(
                                    AccountSheetSnapshot(
                                        accountId = accountId,
                                        sheetId = charRecord.id,
                                        sheetRevision = 0L,
                                        confirmedCharacter = Character(
                                            id = charRecord.id,
                                            ownerId = charRecord.ownerId,
                                            campaignId = normalizeCampaignId(charRecord.campaignId),
                                            dirty = false,
                                        ),
                                        updatedAt = now,
                                    ).toRecord(),
                                )
                            }
                            localStore.upsertIntents(intents.map(SheetIntent::toRecord))
                            reprojectSheetLocked(accountId, charRecord.id)
                        }
                    }
                }
            }
        }
    }

    private fun isLegacyMigratedIntent(intent: SheetIntent): Boolean {
        if (intent.targets.isNotEmpty()) return false
        val trimmed = intent.desiredPayloadJson.trimStart()
        val hasEnvelope = intent.baseValueJson.contains("\"protocolVersion\":1") ||
            intent.baseValueJson.contains("\"command\"")
        return (intent.targetKey == "sheet" && !trimmed.startsWith("[")) || hasEnvelope
    }

    private fun reconcileLegacyIntent(
        intent: SheetIntent,
        snapshot: AccountSheetSnapshot?,
    ): List<SheetIntent> {
        val envelope = runCatching { plainGson.fromJson(intent.baseValueJson, Map::class.java) }.getOrNull()
        val commandMap = envelope?.get("command") as? Map<*, *>
        val commandType = commandMap?.get("type")?.toString() ?: ""
        val payload = commandMap?.get("payload") as? Map<*, *>
        val now = clock.nowMillis()

        when (commandType) {
            "DELETE" -> {
                val rev = snapshot?.sheetRevision ?: asLong(envelope?.get("baseRevision")) ?: 0L
                val target = SparkTargetSpec(
                    kind = TargetKind.SHEET,
                    op = TargetOp.TOMBSTONE,
                    expectedRevision = rev,
                    nextRevision = rev + 1L,
                    baseValueJson = "{\"deleted\":false}",
                    valueJson = "{\"deleted\":true}",
                )
                return listOf(
                    intent.copy(
                        type = OperationType.DELETE_SHEET,
                        targetKey = target.canonicalTargetKey(),
                        targets = listOf(target),
                        baseRevision = rev,
                        baseValueJson = "{\"deleted\":false}",
                        desiredPayloadJson = "{\"deleted\":true}",
                        payloadHash = computeCanonicalPayloadHash(
                            sheetId = intent.sheetId,
                            type = OperationType.DELETE_SHEET,
                            targetKey = target.canonicalTargetKey(),
                            targets = listOf(target),
                        ),
                        updatedAt = now,
                    ),
                )
            }
            "SET_LOCK" -> {
                val rev = snapshot?.sheetRevision ?: asLong(envelope?.get("baseRevision")) ?: 0L
                val lockType = payload?.get("lockType")?.toString() ?: "NONE"
                val locked = lockType != "NONE"
                val desiredJson = plainGson.toJson(
                    linkedMapOf(
                        "lockType" to lockType,
                        "isLocked" to locked,
                        "lockedBy" to if (locked) intent.accountId else "",
                        "lockedAt" to if (locked) now else null,
                    ),
                )
                val target = SparkTargetSpec(
                    kind = TargetKind.SHEET,
                    op = TargetOp.LOCK,
                    expectedRevision = rev,
                    nextRevision = rev + 1L,
                    valueJson = desiredJson,
                )
                return listOf(
                    intent.copy(
                        type = OperationType.SET_LOCK,
                        targetKey = target.canonicalTargetKey(),
                        targets = listOf(target),
                        baseRevision = rev,
                        desiredPayloadJson = desiredJson,
                        payloadHash = computeCanonicalPayloadHash(
                            sheetId = intent.sheetId,
                            type = OperationType.SET_LOCK,
                            targetKey = target.canonicalTargetKey(),
                            targets = listOf(target),
                            desiredPayloadJson = desiredJson,
                        ),
                        updatedAt = now,
                    ),
                )
            }
            "TRANSFER_OWNERSHIP" -> {
                val rev = snapshot?.sheetRevision ?: asLong(envelope?.get("baseRevision")) ?: 0L
                val desiredJson = plainGson.toJson(mapOf("ownerId" to payload?.get("ownerId")?.toString().orEmpty()))
                val target = SparkTargetSpec(
                    kind = TargetKind.SHEET,
                    op = TargetOp.TRANSFER,
                    expectedRevision = rev,
                    nextRevision = rev + 1L,
                    valueJson = desiredJson,
                )
                return listOf(
                    intent.copy(
                        type = OperationType.TRANSFER_OWNERSHIP,
                        targetKey = target.canonicalTargetKey(),
                        targets = listOf(target),
                        baseRevision = rev,
                        desiredPayloadJson = desiredJson,
                        payloadHash = computeCanonicalPayloadHash(
                            sheetId = intent.sheetId,
                            type = OperationType.TRANSFER_OWNERSHIP,
                            targetKey = target.canonicalTargetKey(),
                            targets = listOf(target),
                            desiredPayloadJson = desiredJson,
                        ),
                        updatedAt = now,
                    ),
                )
            }
        }

        val afterRecord = decodeLegacyCharacterRecord(intent.desiredPayloadJson)
            ?: return emptyList()
        val beforeRecord = (payload?.get("base")?.let { decodeLegacyCharacterRecord(plainGson.toJson(it)) })
            ?: snapshot?.confirmedCharacter?.toRecord()
        val baseRev = snapshot?.sheetRevision ?: asLong(envelope?.get("baseRevision")) ?: 0L
        val batches = SparkWireCodec.buildOperationBatches(
            before = beforeRecord,
            after = afterRecord.copy(id = intent.sheetId, dirty = false),
            operationIdPrefix = intent.operationId,
            actorUid = intent.accountId,
            existingRevisions = snapshot?.fieldRevisions.orEmpty(),
            sheetRevision = baseRev,
            updatedAt = now,
        )
        if (batches.isEmpty()) return emptyList()
        return batches.mapIndexed { idx, batch ->
            val targets = batch.targets
            val opType = if (intent.type == OperationType.SESSION_ACTION && idx == 0) {
                OperationType.SESSION_ACTION
            } else {
                batch.type
            }
            SheetIntent(
                operationId = batch.operationId,
                accountId = intent.accountId,
                sheetId = intent.sheetId,
                type = opType,
                targetKey = targets.first().canonicalTargetKey(),
                targets = targets,
                dependsOnOperationId = if (idx == 0) intent.dependsOnOperationId else batches[idx - 1].operationId,
                baseRevision = baseRev,
                baseValueJson = "",
                desiredPayloadJson = "",
                status = intent.status,
                error = intent.error,
                createdAt = intent.createdAt,
                updatedAt = now,
            )
        }
    }

    private suspend fun projectAccountToCharactersLocked(accountId: String) {
        localStore.withTransaction {
            val accountSheets = localStore.listAccountSheets(accountId)
            val sheetIds = accountSheets.mapTo(linkedSetOf(), AccountSheetRecord::sheetId)
            val existingChars = localStore.allCharacters()

            // Remove characters belonging to other accounts' projections.
            existingChars.forEach { record ->
                if (record.id !in sheetIds) {
                    if (record.ownerId == accountId) {
                        // Legacy local character owned by this account not yet in account_sheets: preserve it.
                        val now = clock.nowMillis()
                        val snap = AccountSheetSnapshot(
                            accountId = accountId,
                            sheetId = record.id,
                            sheetRevision = 0L,
                            confirmedCharacter = record.copy(dirty = false).toDomain(),
                            localPhotoUri = if (SparkWireCodec.isLocalOnlyPhotoUri(record.photoUri)) record.photoUri else "",
                            updatedAt = now,
                        )
                        localStore.upsertAccountSheet(snap.toRecord())
                    } else {
                        localStore.purgeCharacter(record.id)
                    }
                }
            }

            accountSheets.forEach { sheet ->
                reprojectSheetLocked(accountId, sheet.sheetId)
            }
        }
    }

    private suspend fun reprojectSheetLocked(accountId: String, sheetId: String) {
        val record = localStore.getAccountSheet(accountId, sheetId) ?: return
        val snapshot = record.toSnapshot()
        val intents = localStore.listIntentsForSheet(accountId, sheetId).map(SheetIntentRecord::toDomain)
        if (!isAccountCurrent(accountId) || activeAccountId != accountId) return
        val replay = projectReplay(snapshot, intents)

        // Persist any new conflicts or PAUSED_DEPENDENCY status transitions detected during replay.
        if (replay.conflicts.isNotEmpty()) {
            localStore.upsertConflicts(replay.conflicts.map(SheetConflictItem::toRecord))
        }
        replay.intents.forEachIndexed { idx, updated ->
            val original = intents.getOrNull(idx)
            if (original != null && original.status != updated.status) {
                localStore.upsertIntent(updated.toRecord())
            }
        }

        // Only the signed-in, currently active account may write the shared `characters` projection.
        if (activeAccountId == accountId && isAccountCurrent(accountId)) {
            var projected = replay.projectedCharacter
            // Overlay non-blocked PENDING/SENDING targets not yet mirrored by SparkSyncModels.applyTargetToCharacter.
            // DELTA targets are already replayed against the latest confirmed balance; never re-apply
            // their stale absolute valueJson on top.
            replay.intents
                .filter { it.status == SheetIntentStatus.PENDING || it.status == SheetIntentStatus.SENDING }
                .forEach { pending ->
                    pending.effectiveTargets().forEach { target ->
                        projected = applyExtendedTargetProjection(projected, target, absoluteForDelta = false)
                    }
                }
            if (snapshot.localPhotoUri.isNotBlank() && projected.photoUri.isBlank()) {
                projected = projected.copy(photoUri = snapshot.localPhotoUri)
            }
            localStore.upsertCharacter(projected.copy(dirty = false).toRecord())
        }
    }

    private fun orderIntentsTopologically(intents: List<SheetIntent>): List<SheetIntent> {
        if (intents.size <= 1) return intents
        val byId = intents.associateBy { it.operationId }
        val visited = HashSet<String>(intents.size)
        val ordered = ArrayList<SheetIntent>(intents.size)
        fun visit(intent: SheetIntent) {
            if (!visited.add(intent.operationId)) return
            intent.dependsOnOperationId?.let { depId -> byId[depId]?.let(::visit) }
            ordered += intent
        }
        intents.forEach(::visit)
        return ordered
    }

    private fun projectReplay(
        snapshot: AccountSheetSnapshot,
        intents: List<SheetIntent>,
    ) = applyPendingIntentsOnConfirmed(snapshot, intents)

    private suspend fun pauseDependentsLocked(
        accountId: String,
        sheetId: String,
        failedOpId: String,
        blockedOps: MutableSet<String>,
    ) {
        val all = localStore.listIntentsForSheet(accountId, sheetId).map(SheetIntentRecord::toDomain)
        var changed = true
        while (changed) {
            changed = false
            for (item in all) {
                if (item.operationId !in blockedOps &&
                    item.dependsOnOperationId != null &&
                    (item.dependsOnOperationId == failedOpId || item.dependsOnOperationId in blockedOps) &&
                    item.status in setOf(SheetIntentStatus.PENDING, SheetIntentStatus.SENDING)
                ) {
                    blockedOps += item.operationId
                    localStore.upsertIntent(
                        item.copy(
                            status = SheetIntentStatus.PAUSED_DEPENDENCY,
                            updatedAt = clock.nowMillis(),
                        ).toRecord(),
                    )
                    changed = true
                }
            }
        }
    }

    private suspend fun rebindDependentsLocked(parent: SheetIntent, committed: List<SparkTargetSpec>) {
        val prior = parent.effectiveTargets().associateBy { it.canonicalTargetKey() }
        val confirmed = committed.associateBy { it.canonicalTargetKey() }
        localStore.listIntentsForSheet(parent.accountId, parent.sheetId).map { it.toDomain() }
            .filter { it.dependsOnOperationId == parent.operationId &&
                it.status in setOf(SheetIntentStatus.PENDING, SheetIntentStatus.PAUSED_DEPENDENCY) }
            .forEach { dependent ->
                val targets = dependent.effectiveTargets().map { target ->
                    val key = target.canonicalTargetKey()
                    val previous = prior[key]
                    val resolved = confirmed[key]
                    if (previous != null && resolved != null && target.expectedRevision == previous.nextRevision) {
                        target.copy(expectedRevision = resolved.nextRevision,
                            nextRevision = resolved.nextRevision + 1,
                            baseValueJson = resolved.valueJson ?: target.baseValueJson)
                    } else target
                }
                localStore.upsertIntent(dependent.copy(targets = targets,
                    payloadHash = computeCanonicalPayloadHash(dependent.sheetId, dependent.type,
                        dependent.targetKey, targets, dependent.desiredPayloadJson),
                    status = SheetIntentStatus.PENDING, error = "").toRecord())
            }
    }

    private suspend fun applyCommittedTargetsToAccountSheetLocked(
        accountId: String,
        intent: SheetIntent,
        resolvedTargets: List<SparkTargetSpec>,
        serverTimestamp: Long,
    ) {
        val existing = localStore.getAccountSheet(accountId, intent.sheetId)?.toSnapshot()
            ?: AccountSheetSnapshot(accountId = accountId, sheetId = intent.sheetId)
        val preRevs = existing.fieldRevisions.toMutableMap()
        var preSheetRev = existing.sheetRevision
        resolvedTargets.forEach { target ->
            if (target.kind == TargetKind.SHEET) {
                preSheetRev = target.expectedRevision
            } else {
                preRevs[target.canonicalTargetKey()] = target.expectedRevision
            }
        }
        val preSnapshot = existing.copy(sheetRevision = preSheetRev, fieldRevisions = preRevs)
        var updatedChar = applyPendingIntentsOnConfirmed(
            confirmed = preSnapshot,
            intents = listOf(intent.copy(targets = resolvedTargets, status = SheetIntentStatus.PENDING)),
        ).projectedCharacter

        var sheetRev = existing.sheetRevision
        val fieldRevs = existing.fieldRevisions.toMutableMap()
        val tombstones = existing.tombstonedItemIds.toMutableSet()
        var sheetDeleted = existing.sheetDeleted

        resolvedTargets.forEach { target ->
            updatedChar = applyExtendedTargetProjection(updatedChar, target)
            when (target.kind) {
                TargetKind.SHEET -> {
                    sheetRev = maxOf(sheetRev, target.nextRevision)
                    if (target.op == TargetOp.TOMBSTONE) {
                        sheetDeleted = true
                        updatedChar = updatedChar.copy(deleted = true)
                    } else if (target.op == TargetOp.RESTORE) {
                        sheetDeleted = false
                        updatedChar = updatedChar.copy(deleted = false)
                    }
                }
                TargetKind.FIELD -> {
                    fieldRevs[target.canonicalTargetKey()] = target.nextRevision
                }
                TargetKind.ITEM -> {
                    fieldRevs[target.canonicalTargetKey()] = target.nextRevision
                    if (target.op == TargetOp.TOMBSTONE) {
                        tombstones += target.itemId
                    } else if (target.op == TargetOp.RESTORE) {
                        tombstones -= target.itemId
                    }
                }
                TargetKind.ITEM_FIELD -> {
                    fieldRevs[target.canonicalTargetKey()] = target.nextRevision
                }
            }
        }

        val nextSnap = existing.copy(
            sheetRevision = sheetRev,
            fieldRevisions = fieldRevs,
            confirmedCharacter = updatedChar.copy(dirty = false),
            tombstonedItemIds = tombstones,
            sheetDeleted = sheetDeleted,
            updatedAt = serverTimestamp,
        )
        localStore.upsertAccountSheet(nextSnap.toRecord())
    }

    private suspend fun ensureAccountSheetSnapshotLocked(
        accountId: String,
        character: Character,
        now: Long,
    ): AccountSheetSnapshot {
        localStore.getAccountSheet(accountId, character.id)?.let { return it.toSnapshot() }
        val existingChar = localStore.getCharacter(character.id)?.toDomain() ?: character.copy(dirty = false)
        val snap = AccountSheetSnapshot(
            accountId = accountId,
            sheetId = character.id,
            sheetRevision = 0L,
            confirmedCharacter = existingChar.copy(dirty = false),
            localPhotoUri = if (SparkWireCodec.isLocalOnlyPhotoUri(existingChar.photoUri)) existingChar.photoUri else "",
            updatedAt = now,
        )
        localStore.upsertAccountSheet(snap.toRecord())
        return snap
    }

    private data class WorkingRevisionState(
        val sheetRevision: Long,
        val fieldRevisions: Map<String, Long>,
        val targetHeads: Map<String, String>,
        val pendingCreateOpId: String?,
    )

    private fun computeWorkingRevisionsAndHeads(
        snapshot: AccountSheetSnapshot?,
        intents: List<SheetIntent>,
    ): WorkingRevisionState {
        var sheetRev = snapshot?.sheetRevision ?: 0L
        val fieldRevs = snapshot?.fieldRevisions?.toMutableMap() ?: mutableMapOf()
        val heads = mutableMapOf<String, String>()
        var createOpId: String? = null

        intents.filter {
            it.status in setOf(
                SheetIntentStatus.PENDING,
                SheetIntentStatus.SENDING,
                SheetIntentStatus.PAUSED_DEPENDENCY,
                SheetIntentStatus.CONFLICT,
                SheetIntentStatus.ACCESS_DENIED,
                SheetIntentStatus.DRAFT,
            )
        }.forEach { intent ->
            if (intent.type == OperationType.CREATE_SHEET) createOpId = intent.operationId
            intent.effectiveTargets().forEach { target ->
                val key = target.canonicalTargetKey()
                heads[key] = intent.operationId
                if (target.kind == TargetKind.SHEET) {
                    sheetRev = maxOf(sheetRev, target.nextRevision)
                } else {
                    fieldRevs[key] = maxOf(fieldRevs[key] ?: 0L, target.nextRevision)
                }
            }
        }
        return WorkingRevisionState(sheetRev, fieldRevs, heads, createOpId)
    }

    private fun buildChainedIntentsFromBatches(
        batches: List<SparkOperationBatch>,
        accountId: String,
        sheetId: String,
        workingHeads: Map<String, String>,
        createOpId: String?,
        createdAt: Long,
    ): List<SheetIntent> {
        val localHeads = workingHeads.toMutableMap()
        return batches.mapIndexed { idx, batch ->
            val dep = batch.targets.firstNotNullOfOrNull { localHeads[it.canonicalTargetKey()] }
                ?: if (idx > 0) batches[idx - 1].operationId else createOpId
            val intent = SheetIntent(
                operationId = batch.operationId,
                accountId = accountId,
                sheetId = sheetId,
                type = batch.type,
                targetKey = batch.targets.first().canonicalTargetKey(),
                targets = batch.targets,
                dependsOnOperationId = dep,
                baseRevision = batch.targets.first().expectedRevision,
                baseValueJson = batch.targets.first().baseValueJson.orEmpty(),
                desiredPayloadJson = "",
                status = SheetIntentStatus.PENDING,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
            batch.targets.forEach { localHeads[it.canonicalTargetKey()] = intent.operationId }
            intent
        }
    }

    private fun batchesToIntents(
        batches: List<SparkOperationBatch>,
        accountId: String,
        sheetId: String,
        initialDependency: String?,
        createdAt: Long,
    ): List<SheetIntent> = batches.mapIndexed { idx, batch ->
        SheetIntent(
            operationId = batch.operationId,
            accountId = accountId,
            sheetId = sheetId,
            type = batch.type,
            targetKey = batch.targets.first().canonicalTargetKey(),
            targets = batch.targets,
            dependsOnOperationId = if (idx == 0) initialDependency else batches[idx - 1].operationId,
            baseRevision = batch.targets.first().expectedRevision,
            baseValueJson = batch.targets.first().baseValueJson.orEmpty(),
            desiredPayloadJson = "",
            status = SheetIntentStatus.PENDING,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }

    private fun buildSessionIntent(
        accountId: String,
        before: Character,
        after: Character,
        command: SessionCommand,
        working: WorkingRevisionState,
        createdAt: Long,
    ): SheetIntent? {
        // Fast path for single-field resource/money DELTA operations (commute cleanly).
        if (command.type in setOf(
                SessionOperationType.MONEY,
                SessionOperationType.HEAL,
                SessionOperationType.RESOURCE,
                SessionOperationType.DESTINY,
            )
        ) {
            val deltaTarget = buildSingleDeltaTarget(before, after, command, working)
            if (deltaTarget != null) {
                val key = deltaTarget.canonicalTargetKey()
                return SheetIntent(
                    operationId = command.idempotencyKey,
                    accountId = accountId,
                    sheetId = before.id,
                    type = OperationType.ADJUST_RESOURCE,
                    targetKey = key,
                    targets = listOf(deltaTarget),
                    dependsOnOperationId = working.targetHeads[key] ?: working.pendingCreateOpId,
                    baseRevision = deltaTarget.expectedRevision,
                    baseValueJson = deltaTarget.baseValueJson.orEmpty(),
                    desiredPayloadJson = "",
                    status = SheetIntentStatus.PENDING,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                )
            }
        }

        val batches = SparkWireCodec.buildOperationBatches(
            before = before.toRecord(),
            after = after.toRecord(),
            operationIdPrefix = command.idempotencyKey,
            actorUid = accountId,
            existingRevisions = working.fieldRevisions,
            sheetRevision = working.sheetRevision,
            updatedAt = createdAt,
        )
        if (batches.isEmpty()) return null
        val allTargets = batches.flatMap { it.targets }
        // Atomic multi-effect session actions must fit within a single Spark transaction budget!
        validateAtomicSparkActionBudget(allTargets, command.label.ifBlank { command.type.name })
        val isItemConsume = command.type == SessionOperationType.ITEM_CONSUME ||
            command.effectOperation is com.kinderman.sdo.domain.model.EffectOperation.ConsumeItemState
        val upgraded = allTargets.map { target ->
            when {
                target.kind == TargetKind.ITEM_FIELD &&
                    (target.key == "quantity" || target.key == "stack") &&
                    isItemConsume -> {
                    val prevMap = jsonToPlainValue(target.baseValueJson) as? Map<*, *>
                    val nextMap = jsonToPlainValue(target.valueJson) as? Map<*, *>
                    val prevQty = if (prevMap != null) asInt(prevMap["quantity"]) else asInt(jsonToPlainValue(target.baseValueJson))
                    val nextQty = if (nextMap != null) asInt(nextMap["quantity"]) else asInt(jsonToPlainValue(target.valueJson))
                    if (prevQty != null && nextQty != null && nextQty < prevQty) {
                        target.copy(
                            op = TargetOp.DELTA,
                            delta = nextQty - prevQty,
                            minValue = 0,
                        )
                    } else {
                        target
                    }
                }
                target.kind == TargetKind.FIELD && target.key in setOf(
                    "life", "sanity", "arcane", "energy", "destiny", "exhaustion", "corruption", "money",
                ) -> {
                    if (target.key == "money") {
                        val prevVal = asInt(jsonToPlainValue(target.baseValueJson))
                        val nextVal = asInt(jsonToPlainValue(target.valueJson))
                        if (prevVal != null && nextVal != null) {
                            target.copy(op = TargetOp.DELTA, delta = nextVal - prevVal, minValue = 0, maxValue = Int.MAX_VALUE)
                        } else target
                    } else {
                        val prevMap = jsonToPlainValue(target.baseValueJson) as? Map<*, *>
                        val nextMap = jsonToPlainValue(target.valueJson) as? Map<*, *>
                        val prevCur = asInt(prevMap?.get("current"))
                        val nextCur = asInt(nextMap?.get("current"))
                        if (prevCur != null && nextCur != null) {
                            target.copy(
                                op = TargetOp.DELTA,
                                delta = nextCur - prevCur,
                                minValue = 0,
                                maxValue = if (target.key == "corruption") 100 else asInt(nextMap?.get("maximum")) ?: Int.MAX_VALUE,
                            )
                        } else target
                    }
                }
                else -> target
            }
        }
        val orderedTargets = if (isItemConsume) {
            val consumed = upgraded.filter {
                it.kind == TargetKind.ITEM_FIELD &&
                    (it.key == "quantity" || it.key == "stack") &&
                    it.op == TargetOp.DELTA &&
                    (it.delta ?: 0) < 0
            }
            val rest = upgraded.filterNot { it in consumed }
            consumed + rest
        } else {
            upgraded
        }
        val dep = orderedTargets.firstNotNullOfOrNull { working.targetHeads[it.canonicalTargetKey()] }
            ?: working.pendingCreateOpId
        val opType = if (isItemConsume) OperationType.ITEM_CONSUME else OperationType.SESSION_ACTION
        return SheetIntent(
            operationId = command.idempotencyKey,
            accountId = accountId,
            sheetId = before.id,
            type = opType,
            targetKey = orderedTargets.first().canonicalTargetKey(),
            targets = orderedTargets,
            dependsOnOperationId = dep,
            baseRevision = orderedTargets.first().expectedRevision,
            baseValueJson = orderedTargets.first().baseValueJson.orEmpty(),
            desiredPayloadJson = plainGson.toJson(mapOf(
                "actionType" to command.type.name,
                "abilityItemId" to if (command.type == SessionOperationType.ABILITY_USE) {
                    val id = command.targetId
                    when {
                        before.powers.any { it.id == id } -> "pow_" + id
                        before.mysticAbilities.any { it.id == id } -> "mys_" + id
                        else -> "abil_" + id
                    }
                } else "",
                "costResource" to command.resource?.name?.lowercase().orEmpty(),
            )),
            status = SheetIntentStatus.PENDING,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }

    private fun buildSingleDeltaTarget(
        before: Character,
        after: Character,
        command: SessionCommand,
        working: WorkingRevisionState,
    ): SparkTargetSpec? {
        if (command.type == SessionOperationType.MONEY) {
            val delta = after.money - before.money
            if (delta == 0) return null
            val rev = working.fieldRevisions["field:money"] ?: 0L
            return SparkTargetSpec(
                kind = TargetKind.FIELD,
                key = "money",
                op = TargetOp.DELTA,
                expectedRevision = rev,
                nextRevision = rev + 1L,
                delta = delta,
                baseValueJson = before.money.toString(),
                minValue = 0,
                maxValue = Int.MAX_VALUE,
            )
        }
        val resource = command.resource ?: if (command.type == SessionOperationType.DESTINY) SessionResource.DESTINY else return null
        val key = resource.name.lowercase()
        val beforeRes = before.resourceValue(resource)
        val afterRes = after.resourceValue(resource)
        val delta = afterRes.current - beforeRes.current
        if (delta == 0) return null
        val rev = working.fieldRevisions["field:$key"] ?: 0L
        return SparkTargetSpec(
            kind = TargetKind.FIELD,
            key = key,
            op = TargetOp.DELTA,
            expectedRevision = rev,
            nextRevision = rev + 1L,
            delta = delta,
            baseValueJson = plainGson.toJson(
                mapOf(
                    "current" to beforeRes.current,
                    "maximum" to beforeRes.maximum,
                    "adjustment" to beforeRes.adjustment,
                ),
            ),
            minValue = 0,
            maxValue = before.resourceMaximum(resource).coerceAtLeast(afterRes.maximum),
        )
    }

    private suspend fun isCampaignHistorian(session: UserSession, character: Character): Boolean {
        if (session.isAdmin) return true
        val cid = normalizeCampaignId(character.campaignId)
        if (cid.isBlank()) return false
        return localStore.getCampaign(cid)?.ownerId == session.uid
    }

    private suspend fun isCampaignResponsible(session: UserSession, character: Character): Boolean =
        isCampaignHistorian(session, character)

    private fun buildConflict(
        intent: SheetIntent,
        target: SparkTargetSpec,
        remoteRev: Long,
        remoteValue: Any?,
    ): SheetConflictItem {
        val targetKey = target.canonicalTargetKey()
        val remoteJson = plainGson.toJson(remoteValue)
        val localJson = target.valueJson ?: intent.desiredPayloadJson
        val baseJson = target.baseValueJson ?: intent.baseValueJson
        return SheetConflictItem(
            id = "${intent.operationId}::$targetKey",
            accountId = intent.accountId,
            sheetId = intent.sheetId,
            operationId = intent.operationId,
            targetKey = targetKey,
            label = target.key.ifBlank { target.itemId.ifBlank { "Ficha" } },
            baseRevision = target.expectedRevision,
            remoteRevision = remoteRev,
            baseSummary = summarizePlainJson(baseJson),
            localSummary = summarizePlainJson(localJson),
            remoteSummary = summarizePlainJson(remoteJson),
            basePayloadJson = baseJson,
            localPayloadJson = localJson,
            remotePayloadJson = remoteJson,
            resolutionStatus = ConflictResolutionStatus.OPEN,
            createdAt = intent.createdAt,
        )
    }

    companion object {
        const val DEMO_ACCOUNT_ID = "demo-player"
        private const val PRESENCE_HEARTBEAT_THROTTLE_MS = 25_000L
        private const val PRESENCE_TTL_MS = 90_000L

        private val plainGson = GsonBuilder().create()
        private val converters = CharacterConverters()
        private val revMapType = object : TypeToken<Map<String, Long>>() {}.type
        private val canonicalJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        private fun AccountSheetRecord.toSnapshot(): AccountSheetSnapshot {
            val rawRevs: Map<String, Long> = runCatching {
                plainGson.fromJson<Map<String, Long>>(fieldRevisionsJson, revMapType)
            }.getOrNull().orEmpty()
            val tombstones = linkedSetOf<String>()
            val cleanRevs = linkedMapOf<String, Long>()
            rawRevs.forEach { (k, v) ->
                if (k.startsWith("@tombstone:")) {
                    tombstones += k.removePrefix("@tombstone:")
                } else {
                    cleanRevs[k] = v
                }
            }
            val confirmedChar = if (confirmedJson.isBlank()) {
                Character(id = sheetId, dirty = false)
            } else {
                runCatching {
                    plainGson.fromJson(confirmedJson, CharacterRecord::class.java)
                        ?.copy(id = sheetId, dirty = false)
                        ?.toDomain()
                }.getOrNull() ?: Character(id = sheetId, dirty = false)
            }
            return AccountSheetSnapshot(
                accountId = accountId,
                sheetId = sheetId,
                sheetRevision = sheetRevision,
                fieldRevisions = cleanRevs,
                confirmedCharacter = confirmedChar,
                tombstonedItemIds = tombstones,
                sheetDeleted = confirmedChar.deleted,
                localPhotoUri = localPhotoUri,
                updatedAt = updatedAt,
            )
        }

        private fun AccountSheetSnapshot.toRecord(): AccountSheetRecord {
            val mergedRevs = LinkedHashMap<String, Long>(fieldRevisions)
            tombstonedItemIds.forEach { itemId ->
                mergedRevs["@tombstone:$itemId"] = fieldRevisions["item:$itemId"] ?: 1L
            }
            val charRecord = confirmedCharacter.copy(
                id = sheetId,
                deleted = sheetDeleted || confirmedCharacter.deleted,
                dirty = false,
            ).toRecord()
            return AccountSheetRecord(
                accountId = accountId,
                sheetId = sheetId,
                sheetRevision = sheetRevision,
                fieldRevisionsJson = plainGson.toJson(mergedRevs),
                confirmedJson = plainGson.toJson(charRecord),
                localPhotoUri = localPhotoUri,
                updatedAt = updatedAt,
            )
        }
    }
}

class SparkSheetSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val app = applicationContext as SdoApplication
        if (app.sparkSyncEngine.drain()) Result.success() else Result.retry()
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        Log.e("SDO_SYNC", "SparkSheetSyncWorker failed", error)
        Result.retry()
    }
}

private val sharedPlainGson = GsonBuilder().create()
private val sharedConverters = CharacterConverters()
private val sharedJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal fun normalizeFirestoreMap(input: Map<String, Any?>): Map<String, Any?> =
    input.mapValues { (_, v) -> normalizeFirestoreValue(v) }

internal fun normalizeFirestoreValue(value: Any?): Any? = when (value) {
    is Timestamp -> value.toDate().time
    is Map<*, *> -> value.entries.associate { it.key.toString() to normalizeFirestoreValue(it.value) }
    is List<*> -> value.map(::normalizeFirestoreValue)
    else -> value
}

internal fun jsonToPlainValue(raw: String?): Any? {
    if (raw.isNullOrBlank()) return ""
    val parsed = runCatching { JsonParser.parseString(raw) }.getOrNull() ?: return raw
    return gsonElementToPlain(parsed)
}

private fun gsonElementToPlain(element: JsonElement?): Any? = when {
    element == null || element.isJsonNull -> null
    element.isJsonObject -> element.asJsonObject.entrySet().associate { it.key to gsonElementToPlain(it.value) }
    element.isJsonArray -> element.asJsonArray.map(::gsonElementToPlain)
    element.asJsonPrimitive.isBoolean -> element.asBoolean
    element.asJsonPrimitive.isNumber -> element.asString.toLongOrNull() ?: element.asDouble
    else -> element.asString
}

private fun asLong(raw: Any?): Long? = when (raw) {
    is Long -> raw
    is Int -> raw.toLong()
    is Number -> raw.toLong()
    is String -> raw.toLongOrNull()
    else -> null
}

private fun asInt(raw: Any?): Int? = asLong(raw)?.toInt()

private fun summarizePlainJson(raw: String): String {
    if (raw.isBlank()) return "—"
    val parsed = runCatching { JsonParser.parseString(raw) }.getOrNull() ?: return raw.take(120)
    return when {
        parsed.isJsonPrimitive -> parsed.asJsonPrimitive.asString.take(120)
        parsed.isJsonObject -> {
            val obj = parsed.asJsonObject
            if (obj.has("current") && obj.has("maximum")) {
                "${obj.get("current").asString}/${obj.get("maximum").asString}"
            } else {
                raw.take(120)
            }
        }
        else -> raw.take(120)
    }
}

private fun decodeLegacyCharacterRecord(raw: String): CharacterRecord? {
    if (raw.isBlank() || !raw.trimStart().startsWith("{")) return null
    return runCatching {
        val defaults = sharedPlainGson.toJsonTree(CharacterRecord()).asJsonObject
        val incoming = JsonParser.parseString(raw).asJsonObject
        incoming.entrySet().forEach { (key, value) ->
            if (!value.isJsonNull || defaults.get(key)?.isJsonNull != false) {
                defaults.add(key, value)
            }
        }
        sharedPlainGson.fromJson(defaults, CharacterRecord::class.java)?.normalizedForSparkV2()
    }.getOrNull()
}

private fun parseOperationReceipt(data: Map<String, Any?>): OperationReceipt {
    val opType = runCatching { OperationType.valueOf(data["type"]?.toString().orEmpty()) }
        .getOrDefault(OperationType.PATCH_FIELDS)
    val preconditions = data["preconditions"] as? Map<*, *>
    val payloadHash = preconditions?.get("payloadHash")?.toString().orEmpty()
    val rawTargets = (data["targets"] as? List<*>).orEmpty().mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        val kind = runCatching { TargetKind.valueOf(map["kind"]?.toString().orEmpty()) }.getOrNull()
            ?: return@mapNotNull null
        val op = runCatching { TargetOp.valueOf(map["op"]?.toString().orEmpty()) }.getOrNull()
            ?: return@mapNotNull null
        SparkTargetSpec(
            kind = kind,
            itemId = map["itemId"]?.toString().orEmpty(),
            key = map["key"]?.toString().orEmpty(),
            op = op,
            expectedRevision = asLong(map["expectedRevision"]) ?: 0L,
            nextRevision = asLong(map["nextRevision"]) ?: 1L,
            delta = asInt(map["delta"]),
            baseValueJson = map["baseValue"]?.let(sharedPlainGson::toJson),
            valueJson = map["desiredValue"]?.let(sharedPlainGson::toJson),
        )
    }
    return OperationReceipt(
        id = data["id"]?.toString().orEmpty(),
        actorUid = data["actorUid"]?.toString().orEmpty(),
        type = opType,
        outcome = data["outcome"]?.toString().orEmpty(),
        schemaVersion = asInt(data["schemaVersion"]) ?: 2,
        createdAt = asLong(data["createdAt"]) ?: 0L,
        targets = rawTargets,
        payloadHash = payloadHash,
        campaignId = preconditions?.get("campaignId")?.toString(),
        lockType = preconditions?.get("lockType")?.toString(),
    )
}

private fun expectedItemKindFromId(itemId: String): String = when {
    itemId.startsWith("inv_") -> "INVENTORY"
    itemId.startsWith("istate_") -> "ITEM_STATE"
    itemId.startsWith("abil_") -> "ABILITY"
    itemId.startsWith("pow_") -> "POWER"
    itemId.startsWith("mys_") -> "MYSTIC"
    itemId.startsWith("cond_") -> "CONDITION"
    itemId.startsWith("lcond_") -> "LEGACY_CONDITION"
    itemId.startsWith("note_") -> "NOTE"
    itemId.startsWith("attr_") -> "ATTRIBUTE"
    else -> ""
}

private fun computeFieldDelta(
    key: String,
    prevValue: Any?,
    delta: Int,
    minValue: Int?,
    maxValue: Int?,
): Result<Any> {
    if (key == "money") {
        val current = asLong(prevValue)
            ?: return Result.failure(DomainError.ValidationError("Saldo monetário inválido."))
        val min = (minValue ?: 0).toLong()
        val max = (maxValue ?: Int.MAX_VALUE).toLong()
        val next = current + delta
        return if (next < min || next > max) {
            Result.failure(DomainError.InsufficientResource("Dinheiro insuficiente (atual=$current, delta=$delta)."))
        } else {
            Result.success(next)
        }
    }
    val map = (prevValue as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value }?.toMutableMap()
        ?: return Result.failure(DomainError.ValidationError("Recurso $key inválido."))
    val current = asLong(map["current"]) ?: 0L
    val min = (minValue ?: 0).toLong()
    val maxBound = minOf(if (delta > 0) maxValue?.toLong() ?: Int.MAX_VALUE.toLong() else Int.MAX_VALUE.toLong(),
        if (delta > 0) asLong(map["maximum"]) ?: 0L else Int.MAX_VALUE.toLong(),
        if (key == "corruption") 100L else Int.MAX_VALUE.toLong())
    val next = current + delta
    if (next < min || next > maxBound) {
        return Result.failure(DomainError.InsufficientResource("Recurso $key insuficiente (atual=$current, delta=$delta)."))
    }
    map["current"] = next
    return Result.success(map)
}

private fun buildUpdatedRootDoc(
    sheetId: String,
    actorUid: String,
    remoteRoot: Map<String, Any?>?,
    target: SparkTargetSpec,
    operationId: String,
    nextRevision: Long,
    serverTimestampToken: Any,
    deliveryId: String,
): Map<String, Any?> {
    val desired = jsonToPlainValue(target.valueJson) as? Map<*, *> ?: emptyMap<String, Any?>()
    if (target.op == TargetOp.CREATE || remoteRoot == null) {
        val lockType = desired["lockType"]?.toString() ?: "NONE"
        val isLocked = lockType != "NONE"
        val deliveries = (desired["appliedDeliveryIds"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
        return linkedMapOf(
            "id" to sheetId,
            "ownerId" to (desired["ownerId"]?.toString()?.ifBlank { actorUid } ?: actorUid),
            "campaignId" to normalizeCampaignId(desired["campaignId"]?.toString().orEmpty()),
            "authorizedCampaignId" to normalizeCampaignId(desired["authorizedCampaignId"]?.toString().orEmpty()),
            "deleted" to false,
            "schemaVersion" to SparkWireCodec.SCHEMA_VERSION,
            "canonicalSchemaVersion" to (asInt(desired["canonicalSchemaVersion"]) ?: CURRENT_CHARACTER_SCHEMA_VERSION),
            "itemSchemaVersion" to (asInt(desired["itemSchemaVersion"]) ?: 0),
            "protocolVersion" to SparkWireCodec.PROTOCOL_VERSION,
            "lockType" to lockType,
            "isLocked" to isLocked,
            "lockedBy" to if (isLocked) actorUid else "",
            "lockedAt" to if (isLocked) serverTimestampToken else null,
            "revision" to nextRevision,
            "lastOperationId" to operationId,
            "updatedBy" to actorUid,
            "updatedAt" to serverTimestampToken,
            "appliedDeliveryIds" to deliveries,
        )
    }

    val result = LinkedHashMap<String, Any?>(remoteRoot)
    result["id"] = sheetId
    result["schemaVersion"] = SparkWireCodec.SCHEMA_VERSION
    result["protocolVersion"] = SparkWireCodec.PROTOCOL_VERSION
    result["revision"] = nextRevision
    result["lastOperationId"] = operationId
    result["updatedBy"] = actorUid
    result["updatedAt"] = serverTimestampToken
    when (target.op) {
        TargetOp.LOCK -> {
            val lockType = desired["lockType"]?.toString() ?: "NONE"
            val locked = lockType != "NONE"
            result["lockType"] = lockType
            result["isLocked"] = locked
            result["lockedBy"] = if (locked) actorUid else ""
            result["lockedAt"] = if (locked) serverTimestampToken else null
        }
        TargetOp.TRANSFER -> {
            result["ownerId"] = desired["ownerId"]?.toString() ?: result["ownerId"]
            result["authorizedCampaignId"] = ""
        }
        TargetOp.LINK -> {
            val nextCid = normalizeCampaignId(desired["campaignId"]?.toString().orEmpty())
            val authCid = normalizeCampaignId(desired["authorizedCampaignId"]?.toString().orEmpty())
            result["campaignId"] = nextCid
            result["authorizedCampaignId"] = authCid
        }
        TargetOp.UNLINK -> {
            result["campaignId"] = ""
            result["authorizedCampaignId"] = ""
        }
        TargetOp.TOMBSTONE -> {
            result["deleted"] = true
            result["authorizedCampaignId"] = ""
        }
        TargetOp.RESTORE -> {
            result["deleted"] = false
        }
        TargetOp.DELIVERY -> {
            val prev = (remoteRoot["appliedDeliveryIds"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
            result["appliedDeliveryIds"] = if (deliveryId.isNotBlank() && deliveryId !in prev) {
                prev + deliveryId
            } else {
                prev
            }
        }
        TargetOp.SET, TargetOp.CREATE, TargetOp.DELTA -> {
            desired["canonicalSchemaVersion"]?.let { result["canonicalSchemaVersion"] = asInt(it) ?: 0 }
            desired["itemSchemaVersion"]?.let { result["itemSchemaVersion"] = asInt(it) ?: 0 }
        }
    }
    return result
}

private fun applyExtendedTargetProjection(
    character: Character,
    target: SparkTargetSpec,
    absoluteForDelta: Boolean = true,
): Character {
    if (target.op == TargetOp.DELTA && !absoluteForDelta) return character
    val raw = target.valueJson.orEmpty()
    if (target.kind == TargetKind.FIELD) {
        val plain = jsonToPlainValue(raw)
        return when (target.key) {
            "creationStatus" -> character.copy(
                creationStatus = runCatching {
                    com.kinderman.sdo.domain.model.CharacterCreationStatus.valueOf(plain?.toString().orEmpty())
                }.getOrDefault(character.creationStatus),
            )
            "creationRulesVersion" -> character.copy(creationRulesVersion = asInt(plain) ?: character.creationRulesVersion)
            "creationCompletedAt" -> character.copy(creationCompletedAt = asLong(plain))
            "progressionLifeBonus" -> character.copy(progressionLifeBonus = asInt(plain) ?: character.progressionLifeBonus)
            "progressionSanityBonus" -> character.copy(progressionSanityBonus = asInt(plain) ?: character.progressionSanityBonus)
            "progressionArcaneBonus" -> character.copy(progressionArcaneBonus = asInt(plain) ?: character.progressionArcaneBonus)
            "progressionEnergyBonus" -> character.copy(progressionEnergyBonus = asInt(plain) ?: character.progressionEnergyBonus)
            "canonicalBodyPayload" -> {
                val payload = plain?.toString().orEmpty()
                val rec = character.toRecord().copy(canonicalBodyPayload = payload).toDomain()
                character.copy(bodyState = rec.bodyState, bodyRegions = rec.bodyRegions, organs = rec.organs)
            }
            "activeModifiersPayload" -> {
                val payload = plain?.toString().orEmpty()
                val rec = character.toRecord().copy(activeModifiersPayload = payload).toDomain()
                character.copy(activeModifiers = rec.activeModifiers)
            }
            "scopedItemCatalogPayload" -> {
                val payload = plain?.toString().orEmpty()
                val rec = character.toRecord().copy(scopedItemCatalogPayload = payload).toDomain()
                character.copy(customItemCatalog = rec.customItemCatalog)
            }
            "canonicalProgressionPayload" -> {
                val payload = plain?.toString().orEmpty()
                val rec = character.toRecord().copy(canonicalProgressionPayload = payload).toDomain()
                character.copy(progression = rec.progression)
            }
            "migrationReviewPayload" -> {
                val payload = plain?.toString().orEmpty()
                val rec = character.toRecord().copy(migrationReviewPayload = payload).toDomain()
                character.copy(migrationReviews = rec.migrationReviews)
            }
            "itemCreationDraft" -> {
                val payload = plain?.toString().orEmpty()
                character.copy(itemCreationDraft = sharedConverters.stringToItemCreationDraft(payload))
            }
            "learnedKnowledges" -> character.copy(learnedKnowledges = decodeListViaGson<SpecialKnowledge>(plain))
            "arcaneKnowledges" -> character.copy(arcaneKnowledges = decodeListViaGson<SpecialKnowledge>(plain))
            "battleTechniques" -> character.copy(battleTechniques = decodeListViaGson<SpecialKnowledge>(plain))
            "progressionHistory" -> character.copy(progressionHistory = decodeListViaGson<ProgressionRecord>(plain))
            "money" -> if (target.op == TargetOp.DELTA && target.valueJson == null) {
                character
            } else {
                character.copy(money = asInt(plain) ?: character.money)
            }
            "life", "sanity", "arcane", "energy", "destiny", "exhaustion", "corruption" -> {
                val map = plain as? Map<*, *> ?: return character
                val res = ResourceValue(
                    current = asInt(map["current"]) ?: 0,
                    maximum = asInt(map["maximum"]) ?: 0,
                    adjustment = asInt(map["adjustment"]) ?: 0,
                )
                when (target.key) {
                    "life" -> character.copy(life = res)
                    "sanity" -> character.copy(sanity = res)
                    "arcane" -> character.copy(arcane = res)
                    "energy" -> character.copy(energy = res)
                    "destiny" -> character.copy(destiny = res)
                    "exhaustion" -> character.copy(exhaustion = res)
                    "corruption" -> character.copy(corruption = res)
                    else -> character
                }
            }
            else -> character
        }
    }
    if (target.kind == TargetKind.ITEM && target.op == TargetOp.TOMBSTONE) {
        return when {
            target.itemId.startsWith("abil_") -> {
                val id = decodeWireElementId("abil", target.itemId)
                character.copy(abilities = character.abilities.filterNot { it.id == id })
            }
            target.itemId.startsWith("cond_") -> {
                val id = decodeWireElementId("cond", target.itemId)
                character.copy(conditionInstances = character.conditionInstances.filterNot { it.instanceId.value == id })
            }
            target.itemId.startsWith("lcond_") -> {
                val id = decodeWireElementId("lcond", target.itemId)
                character.copy(conditions = character.conditions.filterNot { it.id == id })
            }
            target.itemId.startsWith("istate_") -> {
                val id = decodeWireElementId("istate", target.itemId)
                character.copy(itemStates = character.itemStates.filterNot { it.id.value == id })
            }
            else -> character
        }
    }
    if (target.kind == TargetKind.ITEM_FIELD) {
        val plain = jsonToPlainValue(raw)
        if (target.op == TargetOp.DELTA && target.itemId.startsWith("inv_") && target.key == "quantity" && target.valueJson != null) {
            val invId = decodeWireElementId("inv", target.itemId)
            val newQty = asInt(plain) ?: return character
            return character.copy(
                inventory = character.inventory.map { item ->
                    if (item.id == invId) item.copy(quantity = newQty) else item
                },
            )
        }
        if (target.itemId.startsWith("cond_") && target.key == "payload") {
            val instance = runCatching {
                sharedJson.decodeFromString(ConditionInstance.serializer(), sharedPlainGson.toJson(plain))
            }.getOrNull() ?: return character
            val existing = character.conditionInstances.any { it.instanceId == instance.instanceId }
            return character.copy(
                conditionInstances = if (existing) {
                    character.conditionInstances.map { if (it.instanceId == instance.instanceId) instance else it }
                } else {
                    character.conditionInstances + instance
                },
            )
        }
        if (target.itemId.startsWith("abil_")) {
            val id = decodeWireElementId("abil", target.itemId)
            val existing = character.abilities.firstOrNull { it.id == id }
                ?: Ability(
                    id = id,
                    name = id.ifBlank { "Habilidade" },
                    kind = com.kinderman.sdo.domain.model.AbilityKind.POWER,
                    cost = com.kinderman.sdo.domain.model.AbilityCost.PowerCost(0),
                )
            val updated = when (target.key) {
                "name" -> plain?.toString()?.takeIf { it.isNotBlank() }?.let { existing.copy(name = it) } ?: existing
                "effect" -> existing.copy(effect = plain?.toString().orEmpty())
                "favorite" -> existing.copy(favorite = plain as? Boolean ?: existing.favorite)
                "available" -> existing.copy(available = plain as? Boolean ?: existing.available)
                "cost" -> runCatching {
                    sharedJson.decodeFromString(
                        com.kinderman.sdo.domain.model.AbilityCost.serializer(),
                        sharedPlainGson.toJson(plain),
                    )
                }.getOrNull()?.let { existing.copy(cost = it) } ?: existing
                "action" -> runCatching {
                    sharedJson.decodeFromString(
                        com.kinderman.sdo.domain.model.Execution.serializer(),
                        sharedPlainGson.toJson(plain),
                    )
                }.getOrNull()?.let { existing.copy(execution = it) } ?: existing
                "range" -> runCatching {
                    sharedJson.decodeFromString(
                        com.kinderman.sdo.domain.model.RangeSpec.serializer(),
                        sharedPlainGson.toJson(plain),
                    )
                }.getOrNull()?.let { existing.copy(range = it) } ?: existing
                "duration" -> if (plain == null) {
                    existing.copy(duration = null)
                } else {
                    runCatching {
                        sharedJson.decodeFromString(
                            com.kinderman.sdo.domain.model.Duration.serializer(),
                            sharedPlainGson.toJson(plain),
                        )
                    }.getOrNull()?.let { existing.copy(duration = it) } ?: existing
                }
                "meta" -> {
                    val meta = (plain as? Map<*, *>)?.entries
                        ?.associate { it.key.toString() to it.value }
                        ?.toMutableMap()
                        ?: return character
                    val kindVal = runCatching {
                        com.kinderman.sdo.domain.model.AbilityKind.valueOf(meta["kind"]?.toString().orEmpty())
                    }.getOrDefault(existing.kind)
                    existing.copy(kind = kindVal)
                }
                else -> existing
            }
            val exists = character.abilities.any { it.id == id }
            return character.copy(
                abilities = if (exists) {
                    character.abilities.map { if (it.id == id) updated else it }
                } else {
                    character.abilities + updated
                },
            )
        }
    }
    return character
}

private inline fun <reified T> decodeListViaGson(plain: Any?): List<T> {
    val list = plain as? List<*> ?: return emptyList()
    val type = TypeToken.getParameterized(List::class.java, T::class.java).type
    return runCatching {
        sharedPlainGson.fromJson<List<T>>(sharedPlainGson.toJson(list), type)
    }.getOrDefault(emptyList())
}
