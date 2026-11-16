package com.kinderman.sdo.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.kinderman.sdo.domain.model.ConflictResolutionStatus
import com.kinderman.sdo.domain.model.OperationType
import com.kinderman.sdo.domain.model.SheetConflictItem
import com.kinderman.sdo.domain.model.SheetIntent
import com.kinderman.sdo.domain.model.SheetIntentStatus
import com.kinderman.sdo.domain.model.SparkTargetSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Entity(tableName = "sheet_routes", primaryKeys = ["accountId", "sheetId"])
data class SheetRouteRecord(
    val accountId: String,
    val sheetId: String,
    val status: String,
    val accessEpoch: Long = 1,
    val revision: Long = 0,
    val confirmedJson: String = "",
)

@Entity(tableName = "sheet_outbox")
data class SheetOutboxRecord(
    @PrimaryKey val operationId: String,
    val accountId: String,
    val sheetId: String,
    val deviceId: String,
    val sequence: Long,
    val envelopeJson: String,
    val optimisticJson: String,
    val status: String = "PENDING",
    val error: String = "",
)

@Entity(tableName = "sheet_sequences", primaryKeys = ["accountId", "sheetId"])
data class SheetSequenceRecord(val accountId: String, val sheetId: String, val deviceId: String, val nextSequence: Long)

@Dao
interface SheetSyncDao {
    @Query("SELECT * FROM sheet_routes WHERE accountId = :accountId AND sheetId = :sheetId")
    suspend fun route(accountId: String, sheetId: String): SheetRouteRecord?

    @Query("SELECT * FROM sheet_routes WHERE accountId = :accountId")
    fun observeRoutes(accountId: String): Flow<List<SheetRouteRecord>>

    @Query("SELECT * FROM sheet_outbox WHERE accountId = :accountId AND status = 'PENDING' ORDER BY sheetId, sequence")
    suspend fun pending(accountId: String): List<SheetOutboxRecord>

    @Query("SELECT * FROM sheet_outbox WHERE accountId = :accountId AND sheetId = :sheetId AND status = 'PENDING' ORDER BY sequence")
    suspend fun pendingSheet(accountId: String, sheetId: String): List<SheetOutboxRecord>

    @Query("SELECT * FROM sheet_outbox WHERE accountId = :accountId AND status != 'COMMITTED'")
    fun observeOutbox(accountId: String): Flow<List<SheetOutboxRecord>>

    @Query("SELECT * FROM sheet_outbox WHERE operationId = :id")
    suspend fun operation(id: String): SheetOutboxRecord?

    @Query("SELECT * FROM sheet_sequences WHERE accountId = :accountId AND sheetId = :sheetId")
    suspend fun sequence(accountId: String, sheetId: String): SheetSequenceRecord?

    @Upsert suspend fun upsertRoute(route: SheetRouteRecord)

    @Upsert suspend fun upsertOperation(operation: SheetOutboxRecord)

    @Upsert suspend fun upsertSequence(sequence: SheetSequenceRecord)

    @Query("DELETE FROM sheet_routes WHERE accountId = :accountId AND sheetId = :sheetId")
    suspend fun removeRoute(accountId: String, sheetId: String)
}

@Entity(tableName = "account_sheets", primaryKeys = ["accountId", "sheetId"])
data class AccountSheetRecord(
    val accountId: String,
    val sheetId: String,
    val sheetRevision: Long = 0L,
    val fieldRevisionsJson: String = "{}",
    val confirmedJson: String = "",
    val localPhotoUri: String = "",
    val updatedAt: Long = 0L,
)

@Entity(
    tableName = "sheet_intents",
    indices = [
        Index(value = ["accountId", "sheetId", "status"]),
        Index(value = ["accountId", "status"]),
    ],
)
data class SheetIntentRecord(
    @PrimaryKey val operationId: String,
    val accountId: String,
    val sheetId: String,
    val type: String,
    val targetKey: String,
    val dependsOnOperationId: String? = null,
    val baseRevision: Long = 0L,
    val baseValueJson: String = "",
    val desiredPayloadJson: String = "",
    val payloadHash: String = "",
    val status: String = SheetIntentStatus.PENDING.name,
    val error: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Entity(
    tableName = "sheet_conflicts",
    indices = [
        Index(value = ["accountId", "sheetId", "resolutionStatus"]),
        Index(value = ["accountId", "operationId"]),
    ],
)
data class SheetConflictRecord(
    @PrimaryKey val id: String,
    val accountId: String,
    val sheetId: String,
    val operationId: String,
    val targetKey: String,
    val label: String,
    val baseRevision: Long = 0L,
    val remoteRevision: Long = 0L,
    val baseSummary: String = "",
    val localSummary: String = "",
    val remoteSummary: String = "",
    val basePayloadJson: String = "",
    val localPayloadJson: String = "",
    val remotePayloadJson: String = "",
    val resolutionStatus: String = ConflictResolutionStatus.OPEN.name,
    val createdAt: Long = 0L,
)

@Dao
interface AccountSheetDao {
    @Query("SELECT * FROM account_sheets WHERE accountId = :accountId AND sheetId = :sheetId")
    suspend fun get(accountId: String, sheetId: String): AccountSheetRecord?

    @Query("SELECT * FROM account_sheets WHERE accountId = :accountId")
    fun observeAll(accountId: String): Flow<List<AccountSheetRecord>>

    @Query("SELECT * FROM account_sheets WHERE accountId = :accountId AND sheetId = :sheetId")
    fun observeOne(accountId: String, sheetId: String): Flow<AccountSheetRecord?>

    @Upsert suspend fun upsert(record: AccountSheetRecord)

    @Query("DELETE FROM account_sheets WHERE accountId = :accountId AND sheetId = :sheetId")
    suspend fun delete(accountId: String, sheetId: String)
}

@Dao
interface SheetIntentDao {
    @Query("SELECT * FROM sheet_intents WHERE accountId = :accountId ORDER BY createdAt ASC, operationId ASC")
    fun observeForAccount(accountId: String): Flow<List<SheetIntentRecord>>

    @Query("SELECT * FROM sheet_intents WHERE accountId = :accountId AND sheetId = :sheetId ORDER BY createdAt ASC, operationId ASC")
    fun observeForSheet(accountId: String, sheetId: String): Flow<List<SheetIntentRecord>>

    @Query("SELECT * FROM sheet_intents WHERE accountId = :accountId AND sheetId = :sheetId ORDER BY createdAt ASC, operationId ASC")
    suspend fun listForSheet(accountId: String, sheetId: String): List<SheetIntentRecord>

    @Query("SELECT * FROM sheet_intents WHERE accountId = :accountId AND status IN ('PENDING', 'SENDING') ORDER BY createdAt ASC, operationId ASC")
    suspend fun listActive(accountId: String): List<SheetIntentRecord>

    @Query("SELECT * FROM sheet_intents WHERE accountId = :accountId AND operationId = :operationId")
    suspend fun get(accountId: String, operationId: String): SheetIntentRecord?

    @Upsert suspend fun upsert(intent: SheetIntentRecord)

    @Upsert suspend fun upsertAll(intents: List<SheetIntentRecord>)

    @Query("DELETE FROM sheet_intents WHERE accountId = :accountId AND operationId = :operationId")
    suspend fun delete(accountId: String, operationId: String)
}

@Dao
interface SheetConflictDao {
    @Query("SELECT * FROM sheet_conflicts WHERE accountId = :accountId AND resolutionStatus = 'OPEN' ORDER BY createdAt ASC, id ASC")
    fun observeOpen(accountId: String): Flow<List<SheetConflictRecord>>

    @Query("SELECT * FROM sheet_conflicts WHERE accountId = :accountId AND sheetId = :sheetId ORDER BY createdAt ASC, id ASC")
    fun observeForSheet(accountId: String, sheetId: String): Flow<List<SheetConflictRecord>>

    @Query("SELECT * FROM sheet_conflicts WHERE accountId = :accountId AND id = :id")
    suspend fun get(accountId: String, id: String): SheetConflictRecord?

    @Upsert suspend fun upsert(conflict: SheetConflictRecord)

    @Upsert suspend fun upsertAll(conflicts: List<SheetConflictRecord>)

    @Query("DELETE FROM sheet_conflicts WHERE accountId = :accountId AND id = :id")
    suspend fun delete(accountId: String, id: String)
}

private val intentJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun SheetIntent.toRecord(): SheetIntentRecord = SheetIntentRecord(
    operationId = operationId,
    accountId = accountId,
    sheetId = sheetId,
    type = type.name,
    targetKey = targetKey,
    dependsOnOperationId = dependsOnOperationId,
    baseRevision = baseRevision,
    baseValueJson = baseValueJson,
    desiredPayloadJson = if (targets.isNotEmpty()) {
        intentJson.encodeToString(ListSerializer(SparkTargetSpec.serializer()), targets)
    } else {
        desiredPayloadJson
    },
    payloadHash = payloadHash,
    status = status.name,
    error = error,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun SheetIntentRecord.toDomain(): SheetIntent {
    val parsedTargets = runCatching {
        if (desiredPayloadJson.trimStart().startsWith("[")) {
            intentJson.decodeFromString(ListSerializer(SparkTargetSpec.serializer()), desiredPayloadJson)
        } else {
            emptyList()
        }
    }.getOrDefault(emptyList())
    val opType = runCatching { OperationType.valueOf(type) }.getOrDefault(OperationType.PATCH_FIELDS)
    val intentStatus = runCatching { SheetIntentStatus.valueOf(status) }.getOrDefault(SheetIntentStatus.PENDING)
    return SheetIntent(
        operationId = operationId,
        accountId = accountId,
        sheetId = sheetId,
        type = opType,
        targetKey = targetKey,
        targets = parsedTargets,
        dependsOnOperationId = dependsOnOperationId,
        baseRevision = baseRevision,
        baseValueJson = baseValueJson,
        desiredPayloadJson = desiredPayloadJson,
        payloadHash = payloadHash,
        status = intentStatus,
        error = error,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun SheetConflictItem.toRecord(): SheetConflictRecord = SheetConflictRecord(
    id = id,
    accountId = accountId,
    sheetId = sheetId,
    operationId = operationId,
    targetKey = targetKey,
    label = label,
    baseRevision = baseRevision,
    remoteRevision = remoteRevision,
    baseSummary = baseSummary,
    localSummary = localSummary,
    remoteSummary = remoteSummary,
    basePayloadJson = basePayloadJson,
    localPayloadJson = localPayloadJson,
    remotePayloadJson = remotePayloadJson,
    resolutionStatus = resolutionStatus.name,
    createdAt = createdAt,
)

fun SheetConflictRecord.toDomain(): SheetConflictItem = SheetConflictItem(
    id = id,
    accountId = accountId,
    sheetId = sheetId,
    operationId = operationId,
    targetKey = targetKey,
    label = label,
    baseRevision = baseRevision,
    remoteRevision = remoteRevision,
    baseSummary = baseSummary,
    localSummary = localSummary,
    remoteSummary = remoteSummary,
    basePayloadJson = basePayloadJson,
    localPayloadJson = localPayloadJson,
    remotePayloadJson = remotePayloadJson,
    resolutionStatus = runCatching { ConflictResolutionStatus.valueOf(resolutionStatus) }
        .getOrDefault(ConflictResolutionStatus.OPEN),
    createdAt = createdAt,
)
