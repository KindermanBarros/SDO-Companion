package com.kinderman.sdo.domain.model

import org.junit.Assert.*
import org.junit.Test

class CampaignDeletionTest {
    @Test fun deletedCampaignIsNeverActive() {
        val campaign = Campaign(state = CampaignState.DELETED)
        assertTrue(campaign.isDeleted)
        assertTrue(campaign.isArchived)
    }
    @Test fun archivedCampaignIsNotDeleted() {
        assertFalse(Campaign(state = CampaignState.ARCHIVED).isDeleted)
    }
}
