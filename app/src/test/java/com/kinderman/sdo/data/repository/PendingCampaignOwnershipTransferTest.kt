package com.kinderman.sdo.data.repository

import com.kinderman.sdo.data.local.CampaignMemberRecord
import com.kinderman.sdo.data.local.CampaignRecord
import com.kinderman.sdo.domain.model.CampaignMemberState
import com.kinderman.sdo.domain.model.CampaignRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingCampaignOwnershipTransferTest {
    @Test
    fun `matching master promotion marks a pending ownership transfer`() {
        val campaign = campaign()
        val target = member(userId = "new-owner", role = CampaignRole.HISTORIAN, updatedAt = 200L)

        assertEquals(target, pendingOwnershipTransfer(campaign, listOf(target)))
    }

    @Test
    fun `unrelated dirty member does not mark a transfer`() {
        val campaign = campaign()
        val stalePromotion = member(userId = "new-owner", role = CampaignRole.HISTORIAN, updatedAt = 199L)
        val ordinaryPlayer = member(userId = "player", role = CampaignRole.PLAYER, updatedAt = 200L)
        val currentOwner = member(userId = "old-owner", role = CampaignRole.HISTORIAN, updatedAt = 200L)

        assertNull(
            pendingOwnershipTransfer(
                campaign,
                listOf(stalePromotion, ordinaryPlayer, currentOwner),
            ),
        )
    }

    private fun campaign() = CampaignRecord(
        id = "campaign",
        ownerId = "old-owner",
        updatedAt = 200L,
        dirty = true,
        lastSyncedAt = 100L,
    )

    private fun member(userId: String, role: CampaignRole, updatedAt: Long) = CampaignMemberRecord(
        campaignId = "campaign",
        userId = userId,
        role = role.name,
        state = CampaignMemberState.ACTIVE.name,
        updatedAt = updatedAt,
        dirty = true,
    )
}
