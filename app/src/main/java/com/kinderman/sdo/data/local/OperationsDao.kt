package com.kinderman.sdo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.kinderman.sdo.domain.model.DomainError

@Dao
interface OperationsDao {
    @Query("SELECT * FROM session_operations WHERE campaignId IN (:campaignIds) ORDER BY createdAt DESC")
    fun observeAudit(campaignIds: Set<String>): Flow<List<SessionOperationRecord>>
    @Query("SELECT * FROM campaign_library_entries WHERE campaignId IN (:campaignIds) ORDER BY archived, updatedAt DESC")
    fun observeLibrary(campaignIds: Set<String>): Flow<List<CampaignLibraryRecord>>
    @Query("SELECT * FROM campaign_deliveries WHERE recipientId = :uid OR campaignId IN (:campaignIds) ORDER BY createdAt DESC")
    fun observeDeliveries(uid: String, campaignIds: Set<String>): Flow<List<CampaignDeliveryRecord>>
    @Query("SELECT * FROM campaign_alert_settings WHERE campaignId IN (:campaignIds)")
    fun observeAlertSettings(campaignIds: Set<String>): Flow<List<CampaignAlertSettingsRecord>>
    @Query("SELECT * FROM session_operations WHERE dirty = 1") suspend fun dirtyOperations(): List<SessionOperationRecord>
    @Query("SELECT * FROM campaign_library_entries WHERE dirty = 1") suspend fun dirtyLibrary(): List<CampaignLibraryRecord>
    @Query("SELECT * FROM campaign_deliveries WHERE dirty = 1") suspend fun dirtyDeliveries(): List<CampaignDeliveryRecord>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertOperation(value: SessionOperationRecord): Long
    @Upsert suspend fun upsertCharacter(value: CharacterRecord)
    @Upsert suspend fun upsertOperation(value: SessionOperationRecord)
    @Upsert suspend fun upsertLibrary(value: CampaignLibraryRecord)
    @Upsert suspend fun upsertDelivery(value: CampaignDeliveryRecord)
    @Upsert suspend fun upsertAlertSettings(value: CampaignAlertSettingsRecord)
    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1") suspend fun character(id: String): CharacterRecord?
    @Query("SELECT * FROM session_operations WHERE idempotencyKey = :key LIMIT 1") suspend fun operationByIdempotencyKey(key: String): SessionOperationRecord?
    @Query("SELECT * FROM campaign_library_entries WHERE id = :id LIMIT 1") suspend fun library(id: String): CampaignLibraryRecord?
    @Query("UPDATE session_operations SET dirty = 0, lastSyncedAt = :at WHERE id = :id") suspend fun markOperationSynced(id: String, at: Long)
    @Query("UPDATE campaign_library_entries SET dirty = 0, lastSyncedAt = :at WHERE id = :id") suspend fun markLibrarySynced(id: String, at: Long)
    @Query("UPDATE campaign_deliveries SET dirty = 0, lastSyncedAt = :at WHERE id = :id") suspend fun markDeliverySynced(id: String, at: Long)

    @Transaction
    suspend fun applyOnce(operation: SessionOperationRecord, updatedCharacter: CharacterRecord): Boolean {
        if (operationByIdempotencyKey(operation.idempotencyKey) != null) return false
        val current = character(updatedCharacter.id)
        if (current != null && current.updatedAt != operation.baseCharacterUpdatedAt) {
            throw DomainError.RevisionConflict(
                "A ficha mudou desde a leitura da operação (${operation.baseCharacterUpdatedAt} → ${current.updatedAt}). Recarregue e reaplique.",
            )
        }
        if (insertOperation(operation) == -1L) return false
        upsertCharacter(updatedCharacter)
        return true
    }

    @Transaction
    suspend fun acceptDeliveryOnce(delivery: CampaignDeliveryRecord, updatedCharacter: CharacterRecord): Boolean {
        val stored = character(updatedCharacter.id)
        if (delivery.id in stored?.appliedDeliveryIds.orEmpty()) {
            upsertDelivery(delivery)
            return false
        }
        upsertCharacter(updatedCharacter)
        upsertDelivery(delivery)
        return true
    }
}
