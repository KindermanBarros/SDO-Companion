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

internal const val ROOT_QUERY_LIMIT = 100L
internal const val LAST_OPERATION_MARKER = "@lastOperationAt"

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
        isLocalOperation: (String) -> Boolean = { false },
    ): Flow<RemoteSyncEvent>

    /** Receipt listener for exactly one OPEN sheet. */
    fun observeSheetReceipts(
        sheetId: String,
        ownUid: String,
        isLocalOperation: (String) -> Boolean = { false },
    ): Flow<RemoteSyncEvent>
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

