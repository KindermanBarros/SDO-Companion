package com.kinderman.sdo.domain.model

import java.util.UUID

enum class CampaignContentKind { ITEM, POWER, NOTE, REWARD, CONDITION, TEMPLATE }
enum class CampaignDeliveryState { PENDING, ACCEPTED, DECLINED }

data class CampaignLibraryEntry(
    val id: String = UUID.randomUUID().toString(),
    val campaignId: String = "",
    val kind: CampaignContentKind = CampaignContentKind.NOTE,
    val name: String = "",
    val summary: String = "",
    val payload: String = "",
    val catalogEntryId: String = "",
    val knowledgeBonus: Int = 0,
    val archived: Boolean = false,
    val version: Int = 1,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = true,
    val lastSyncedAt: Long = 0,
    val itemSnapshot: InventoryItem? = null,
    val powerSnapshot: Power? = null,
    val conditionSnapshot: ConditionEffect? = null,
)

data class CampaignDelivery(
    val id: String = UUID.randomUUID().toString(),
    val campaignId: String = "",
    val libraryEntryId: String = "",
    val recipientId: String = "",
    val recipientCharacterId: String = "",
    val snapshotKind: CampaignContentKind = CampaignContentKind.NOTE,
    val snapshotName: String = "",
    val snapshotSummary: String = "",
    val snapshotPayload: String = "",
    val knowledgeMapping: String = "",
    val knowledgeBonus: Int = 0,
    val state: CampaignDeliveryState = CampaignDeliveryState.PENDING,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = true,
    val lastSyncedAt: Long = 0,
)

data class CampaignAlertSettings(
    val campaignId: String,
    val lifeThresholdPercent: Int = 25,
    val sanityThresholdPercent: Int = 25,
    val exhaustionThresholdPercent: Int = 75,
    val staleAfterHours: Int = 24,
    val alertConditions: Boolean = true,
    val alertBodyFailures: Boolean = true,
)
