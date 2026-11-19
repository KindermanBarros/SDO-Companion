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
                    throw SparkPermissionDeniedException(error.message ?: "PermissÃ£o negada.", error)
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                FirebaseFirestoreException.Code.ABORTED,
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                    throw SparkTransientNetworkException(error.message ?: "Falha temporÃ¡ria de rede.", error)
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
            // Never swallow listing failures here: an empty result would make sync() purge every
            // clean local sheet as "no longer accessible". Let the caller report the failure.
            return pagedSheetIds(characters)
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
        isLocalOperation: (String) -> Boolean,
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
                    val changedIds = snapshot.documentChanges
                        .filterNot { change ->
                            val lastOpId = change.document.getString("lastOperationId")
                            !lastOpId.isNullOrBlank() && isLocalOperation(lastOpId)
                        }
                        .mapTo(linkedSetOf()) { it.document.id }
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
            listenCharactersQuery(characters.orderBy("updatedAt", Query.Direction.DESCENDING))
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

    override fun observeSheetReceipts(
        sheetId: String,
        ownUid: String,
        isLocalOperation: (String) -> Boolean,
    ): Flow<RemoteSyncEvent> = callbackFlow {
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
                    snapshot.documentChanges.any { !isLocalOperation(it.document.id) }
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

internal sealed interface DrainTxOutcome {
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
