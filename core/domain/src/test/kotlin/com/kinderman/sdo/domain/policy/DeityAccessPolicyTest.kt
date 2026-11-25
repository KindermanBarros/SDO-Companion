package com.kinderman.sdo.domain.policy

import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignState
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeityAccessPolicyTest {
    private val viewer = UserSession("viewer", "", "", UserRole.MASTER)
    private val owned = Campaign(id = "owned", ownerId = viewer.uid)
    private val joined = Campaign(id = "joined", ownerId = "another-owner")
    private val deleted = Campaign(id = "deleted", ownerId = viewer.uid, state = CampaignState.DELETED)
    private val campaigns = listOf(owned, joined, deleted).associateBy(Campaign::id)

    @Test
    fun spoilersRequireOwnershipOfCurrentNonDeletedCampaignOrAdmin() {
        assertTrue(DeityAccessPolicy.canReadGmContent(viewer, owned.id, campaigns))
        assertFalse(DeityAccessPolicy.canReadGmContent(viewer, joined.id, campaigns))
        assertFalse(DeityAccessPolicy.canReadGmContent(viewer, deleted.id, campaigns))
        assertFalse(DeityAccessPolicy.canReadGmContent(viewer, null, campaigns))
        assertFalse(DeityAccessPolicy.canReadGmContent(null, owned.id, campaigns))
        assertTrue(
            DeityAccessPolicy.canReadGmContent(viewer.copy(role = UserRole.ADMIN), null, campaigns),
        )
    }

    @Test
    fun mixedLibraryMembershipRequiresExplicitOwnedContext() {
        val memberships = setOf(owned.id, joined.id)

        assertNull(DeityAccessPolicy.resolveLibraryCampaignContext(viewer, "ALL", campaigns.values, memberships))
        val selected = DeityAccessPolicy.resolveLibraryCampaignContext(viewer, joined.id, campaigns.values, memberships)
        assertEquals(joined.id, selected)
        assertFalse(DeityAccessPolicy.canReadGmContent(viewer, selected, campaigns))
    }
}
