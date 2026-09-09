package com.kinderman.sdo.domain.model

import com.kinderman.sdo.data.local.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseTwoCampaignTest {
    @Test
    fun `legacy default campaign id becomes standalone`() {
        assertEquals("", normalizeCampaignId("default"))
        assertEquals("", normalizeCampaignId(""))
        assertEquals("camp-42", normalizeCampaignId("camp-42"))
    }

    @Test
    fun `archived campaign exposes read only state`() {
        val active = Campaign(state = CampaignState.ACTIVE)
        val archived = Campaign(state = CampaignState.ARCHIVED, archivedAt = 10L)

        assertFalse(active.isArchived)
        assertTrue(archived.isArchived)
    }

    @Test
    fun `invite is usable until revoked or expired`() {
        val active = CampaignInvite(expiresAt = 2_000L)
        val expired = CampaignInvite(expiresAt = 900L)
        val revoked = CampaignInvite(revokedAt = 500L)

        assertTrue(active.isUsable(now = 1_000L))
        assertFalse(expired.isUsable(now = 1_000L))
        assertFalse(revoked.isUsable(now = 1_000L))
    }

    @Test
    fun `membership only counts while active`() {
        assertTrue(CampaignMember(state = CampaignMemberState.ACTIVE).isActive)
        assertFalse(CampaignMember(state = CampaignMemberState.LEFT).isActive)
        assertFalse(CampaignMember(state = CampaignMemberState.REMOVED).isActive)
    }

    @Test
    fun `legacy master membership is normalized to player`() {
        val record = com.kinderman.sdo.data.local.CampaignMemberRecord(role = "MASTER")
        assertEquals(CampaignRole.PLAYER, record.toDomain().role)
    }
}
