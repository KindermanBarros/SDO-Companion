package com.kinderman.sdo.domain.model

import java.util.UUID

enum class CampaignState { ACTIVE, ARCHIVED }
enum class CampaignRole { MASTER, PLAYER }
enum class CampaignMemberState { ACTIVE, LEFT, REMOVED }

data class CampaignSettings(
    val allowPlayerCharacterCreation: Boolean = true,
)

data class Campaign(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Nova campanha",
    val description: String = "",
    val ownerId: String = "",
    val state: CampaignState = CampaignState.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null,
    val settings: CampaignSettings = CampaignSettings(),
    val dirty: Boolean = true,
    val lastSyncedAt: Long = 0,
) {
    val isArchived: Boolean get() = state == CampaignState.ARCHIVED
}

data class CampaignMember(
    val campaignId: String = "",
    val userId: String = "",
    val role: CampaignRole = CampaignRole.PLAYER,
    val state: CampaignMemberState = CampaignMemberState.ACTIVE,
    val joinedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val characterIds: List<String> = emptyList(),
    val joinedByInviteId: String = "",
    val dirty: Boolean = true,
    val lastSyncedAt: Long = 0,
) {
    val isActive: Boolean get() = state == CampaignMemberState.ACTIVE
}

data class CampaignInvite(
    val id: String = UUID.randomUUID().toString(),
    val campaignId: String = "",
    val code: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val revokedAt: Long? = null,
    val generation: Int = 1,
    val dirty: Boolean = true,
    val lastSyncedAt: Long = 0,
) {
    fun isUsable(now: Long = System.currentTimeMillis()): Boolean =
        revokedAt == null && (expiresAt == null || expiresAt > now)
}

data class CampaignInvitePreview(
    val campaign: Campaign,
    val invite: CampaignInvite,
    val alreadyMember: Boolean,
)

fun normalizeCampaignId(value: String): String = if (value == "default") "" else value
