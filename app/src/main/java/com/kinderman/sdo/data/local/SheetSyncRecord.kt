package com.kinderman.sdo.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

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
