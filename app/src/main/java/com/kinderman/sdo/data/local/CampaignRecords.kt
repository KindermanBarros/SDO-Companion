package com.kinderman.sdo.data.local

import androidx.room.Entity
import androidx.room.Index
import com.google.firebase.firestore.Exclude
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignInvite
import com.kinderman.sdo.domain.model.CampaignMember
import com.kinderman.sdo.domain.model.CampaignMemberState
import com.kinderman.sdo.domain.model.CampaignRole
import com.kinderman.sdo.domain.model.CampaignSettings
import com.kinderman.sdo.domain.model.CampaignState

@Entity(tableName = "campaigns")
data class CampaignRecord(
    @androidx.room.PrimaryKey val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val state: String = CampaignState.ACTIVE.name,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val archivedAt: Long? = null,
    val allowPlayerCharacterCreation: Boolean = true,
    @get:Exclude @field:Exclude val dirty: Boolean = false,
    @get:Exclude @field:Exclude val lastSyncedAt: Long = 0,
)

@Entity(
    tableName = "campaign_members",
    primaryKeys = ["campaignId", "userId"],
    indices = [Index("userId"), Index("campaignId")],
)
data class CampaignMemberRecord(
    val campaignId: String = "",
    val userId: String = "",
    val role: String = CampaignRole.PLAYER.name,
    val state: String = CampaignMemberState.ACTIVE.name,
    val joinedAt: Long = 0,
    val updatedAt: Long = 0,
    val characterIds: List<String> = emptyList(),
    val joinedByInviteId: String = "",
    @get:Exclude @field:Exclude val dirty: Boolean = false,
    @get:Exclude @field:Exclude val lastSyncedAt: Long = 0,
)

@Entity(
    tableName = "campaign_invites",
    indices = [Index(value = ["code"], unique = true), Index("campaignId")],
)
data class CampaignInviteRecord(
    @androidx.room.PrimaryKey val id: String = "",
    val campaignId: String = "",
    val campaignName: String = "",
    val campaignDescription: String = "",
    val code: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0,
    val expiresAt: Long? = null,
    val revokedAt: Long? = null,
    val generation: Int = 1,
    @get:Exclude @field:Exclude val dirty: Boolean = false,
    @get:Exclude @field:Exclude val lastSyncedAt: Long = 0,
)

fun CampaignRecord.toDomain() = Campaign(
    id = id,
    name = name,
    description = description,
    ownerId = ownerId,
    state = runCatching { CampaignState.valueOf(state) }.getOrDefault(CampaignState.ACTIVE),
    createdAt = createdAt,
    updatedAt = updatedAt,
    archivedAt = archivedAt,
    settings = CampaignSettings(allowPlayerCharacterCreation),
    dirty = dirty,
    lastSyncedAt = lastSyncedAt,
)

fun Campaign.toRecord() = CampaignRecord(
    id = id,
    name = name,
    description = description,
    ownerId = ownerId,
    state = state.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    archivedAt = archivedAt,
    allowPlayerCharacterCreation = settings.allowPlayerCharacterCreation,
    dirty = dirty,
    lastSyncedAt = lastSyncedAt,
)

fun CampaignMemberRecord.toDomain() = CampaignMember(
    campaignId = campaignId,
    userId = userId,
    role = CampaignRole.PLAYER,
    state = runCatching { CampaignMemberState.valueOf(state) }.getOrDefault(CampaignMemberState.ACTIVE),
    joinedAt = joinedAt,
    updatedAt = updatedAt,
    characterIds = characterIds,
    joinedByInviteId = joinedByInviteId,
    dirty = dirty,
    lastSyncedAt = lastSyncedAt,
)

fun CampaignMember.toRecord() = CampaignMemberRecord(
    campaignId = campaignId,
    userId = userId,
    role = role.name,
    state = state.name,
    joinedAt = joinedAt,
    updatedAt = updatedAt,
    characterIds = characterIds,
    joinedByInviteId = joinedByInviteId,
    dirty = dirty,
    lastSyncedAt = lastSyncedAt,
)

fun CampaignInviteRecord.toDomain() = CampaignInvite(
    id = id,
    campaignId = campaignId,
    campaignName = campaignName,
    campaignDescription = campaignDescription,
    code = code,
    createdBy = createdBy,
    createdAt = createdAt,
    expiresAt = expiresAt,
    revokedAt = revokedAt,
    generation = generation,
    dirty = dirty,
    lastSyncedAt = lastSyncedAt,
)

fun CampaignInvite.toRecord() = CampaignInviteRecord(
    id = id,
    campaignId = campaignId,
    campaignName = campaignName,
    campaignDescription = campaignDescription,
    code = code,
    createdBy = createdBy,
    createdAt = createdAt,
    expiresAt = expiresAt,
    revokedAt = revokedAt,
    generation = generation,
    dirty = dirty,
    lastSyncedAt = lastSyncedAt,
)
