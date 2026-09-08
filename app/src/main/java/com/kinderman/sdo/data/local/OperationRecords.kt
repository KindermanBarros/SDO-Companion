package com.kinderman.sdo.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.kinderman.sdo.domain.model.CampaignAlertSettings
import com.kinderman.sdo.domain.model.CampaignContentKind
import com.kinderman.sdo.domain.model.CampaignDelivery
import com.kinderman.sdo.domain.model.CampaignDeliveryState
import com.kinderman.sdo.domain.model.CampaignLibraryEntry
import com.kinderman.sdo.domain.model.SessionOperation
import com.kinderman.sdo.domain.model.SessionOperationType

@Entity(tableName = "session_operations", indices = [Index(value = ["idempotencyKey"], unique = true), Index("campaignId"), Index("characterId")])
data class SessionOperationRecord(
    @PrimaryKey val id: String = "",
    val idempotencyKey: String = "",
    val campaignId: String = "",
    val characterId: String = "",
    val actorId: String = "",
    val type: String = SessionOperationType.RESOURCE.name,
    val target: String = "",
    val previousValue: String = "",
    val newValue: String = "",
    val amount: Int = 0,
    val reason: String = "",
    val createdAt: Long = 0,
    @get:Exclude @field:Exclude val dirty: Boolean = false,
    @get:Exclude @field:Exclude val lastSyncedAt: Long = 0,
)

@Entity(tableName = "campaign_library_entries", indices = [Index("campaignId")])
data class CampaignLibraryRecord(
    @PrimaryKey val id: String = "",
    val campaignId: String = "",
    val kind: String = CampaignContentKind.NOTE.name,
    val name: String = "",
    val summary: String = "",
    val payload: String = "",
    val catalogEntryId: String = "",
    val knowledgeBonus: Int = 0,
    val archived: Boolean = false,
    val version: Int = 1,
    val createdBy: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    @get:Exclude @field:Exclude val dirty: Boolean = false,
    @get:Exclude @field:Exclude val lastSyncedAt: Long = 0,
)

@Entity(tableName = "campaign_deliveries", indices = [Index("campaignId"), Index("recipientId"), Index("recipientCharacterId")])
data class CampaignDeliveryRecord(
    @PrimaryKey val id: String = "",
    val campaignId: String = "",
    val libraryEntryId: String = "",
    val recipientId: String = "",
    val recipientCharacterId: String = "",
    val snapshotKind: String = CampaignContentKind.NOTE.name,
    val snapshotName: String = "",
    val snapshotSummary: String = "",
    val snapshotPayload: String = "",
    val knowledgeMapping: String = "",
    val knowledgeBonus: Int = 0,
    val state: String = CampaignDeliveryState.PENDING.name,
    val createdBy: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    @get:Exclude @field:Exclude val dirty: Boolean = false,
    @get:Exclude @field:Exclude val lastSyncedAt: Long = 0,
)

@Entity(tableName = "campaign_alert_settings")
data class CampaignAlertSettingsRecord(
    @PrimaryKey val campaignId: String = "",
    val lifeThresholdPercent: Int = 25,
    val sanityThresholdPercent: Int = 25,
    val exhaustionThresholdPercent: Int = 75,
    val staleAfterHours: Int = 24,
    val alertConditions: Boolean = true,
    val alertBodyFailures: Boolean = true,
)

fun SessionOperationRecord.toDomain() = SessionOperation(id, idempotencyKey, campaignId, characterId, actorId, enumValue(type, SessionOperationType.RESOURCE), target, previousValue, newValue, amount, reason, createdAt, dirty, lastSyncedAt)
fun SessionOperation.toRecord() = SessionOperationRecord(id, idempotencyKey, campaignId, characterId, actorId, type.name, target, previousValue, newValue, amount, reason, createdAt, dirty, lastSyncedAt)
fun CampaignLibraryRecord.toDomain() = CampaignLibraryEntry(id, campaignId, enumValue(kind, CampaignContentKind.NOTE), name, summary, payload, catalogEntryId, knowledgeBonus, archived, version, createdBy, createdAt, updatedAt, dirty, lastSyncedAt)
fun CampaignLibraryEntry.toRecord() = CampaignLibraryRecord(id, campaignId, kind.name, name, summary, payload, catalogEntryId, knowledgeBonus, archived, version, createdBy, createdAt, updatedAt, dirty, lastSyncedAt)
fun CampaignDeliveryRecord.toDomain() = CampaignDelivery(id, campaignId, libraryEntryId, recipientId, recipientCharacterId, enumValue(snapshotKind, CampaignContentKind.NOTE), snapshotName, snapshotSummary, snapshotPayload, knowledgeMapping, knowledgeBonus, enumValue(state, CampaignDeliveryState.PENDING), createdBy, createdAt, updatedAt, dirty, lastSyncedAt)
fun CampaignDelivery.toRecord() = CampaignDeliveryRecord(id, campaignId, libraryEntryId, recipientId, recipientCharacterId, snapshotKind.name, snapshotName, snapshotSummary, snapshotPayload, knowledgeMapping, knowledgeBonus, state.name, createdBy, createdAt, updatedAt, dirty, lastSyncedAt)
fun CampaignAlertSettingsRecord.toDomain() = CampaignAlertSettings(campaignId, lifeThresholdPercent, sanityThresholdPercent, exhaustionThresholdPercent, staleAfterHours, alertConditions, alertBodyFailures)
fun CampaignAlertSettings.toRecord() = CampaignAlertSettingsRecord(campaignId, lifeThresholdPercent, sanityThresholdPercent, exhaustionThresholdPercent, staleAfterHours, alertConditions, alertBodyFailures)
private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T) = enumValues<T>().firstOrNull { it.name == value } ?: fallback

