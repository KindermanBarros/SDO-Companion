package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignJournalPolicyTest {
    private val activeCampaign = Campaign(
        id = "camp-1",
        name = "Crônicas de Aethel",
        ownerId = "gm-1",
        state = CampaignState.ACTIVE,
    )

    private val archivedCampaign = activeCampaign.copy(state = CampaignState.ARCHIVED)

    private val gmSession = UserSession(
        uid = "gm-1",
        email = "gm@sdo.test",
        displayName = "Mestre",
        role = UserRole.USER,
    )

    private val adminSession = UserSession(
        uid = "admin-1",
        email = "admin@sdo.test",
        displayName = "Admin",
        role = UserRole.ADMIN,
    )

    private val playerSession = UserSession(
        uid = "player-1",
        email = "p1@sdo.test",
        displayName = "Jogador Um",
        role = UserRole.USER,
    )

    private val otherPlayerSession = UserSession(
        uid = "player-2",
        email = "p2@sdo.test",
        displayName = "Jogador Dois",
        role = UserRole.USER,
    )

    private val entryByPlayer1 = CampaignJournalEntry(
        id = "entry-1",
        campaignId = activeCampaign.id,
        authorId = playerSession.uid,
        authorName = playerSession.displayName,
        title = "Portão Norte",
        body = "Encontramos marcas rúnicas na entrada.",
        revision = 1L,
    )

    @Test
    fun validateDraftEnforcesBlankAndLengthBoundaries() {
        assertNotNull(CampaignJournalPolicy.validateDraft("   ", "Texto válido"))
        assertNotNull(CampaignJournalPolicy.validateDraft("Título válido", "   "))
        assertNotNull(
            CampaignJournalPolicy.validateDraft(
                "A".repeat(CampaignJournalPolicy.TITLE_MAX_LENGTH + 1),
                "Texto",
            ),
        )
        assertNotNull(
            CampaignJournalPolicy.validateDraft(
                "Título",
                "B".repeat(CampaignJournalPolicy.BODY_MAX_LENGTH + 1),
            ),
        )
        assertNull(
            CampaignJournalPolicy.validateDraft(
                "A".repeat(CampaignJournalPolicy.TITLE_MAX_LENGTH),
                "B".repeat(CampaignJournalPolicy.BODY_MAX_LENGTH),
            ),
        )
    }

    @Test
    fun memberCanCreateAndMutateOwnEntryWhileOnlineInActiveCampaign() {
        assertTrue(CampaignJournalPolicy.canCreate(activeCampaign, playerSession, isActiveMember = true, isOnline = true))
        assertTrue(CampaignJournalPolicy.canMutateEntry(entryByPlayer1, activeCampaign, playerSession, isActiveMember = true, isOnline = true))
        assertFalse(CampaignJournalPolicy.canMutateEntry(entryByPlayer1, activeCampaign, otherPlayerSession, isActiveMember = true, isOnline = true))
    }

    @Test
    fun gmAndAdminCanMutateAnyActiveEntry() {
        assertTrue(CampaignJournalPolicy.canMutateEntry(entryByPlayer1, activeCampaign, gmSession, isActiveMember = true, isOnline = true))
        assertTrue(CampaignJournalPolicy.canMutateEntry(entryByPlayer1, activeCampaign, adminSession, isActiveMember = false, isOnline = true))
    }

    @Test
    fun archivedAndOfflineStatesAreReadOnly() {
        assertFalse(CampaignJournalPolicy.canCreate(archivedCampaign, gmSession, isActiveMember = true, isOnline = true))
        assertFalse(CampaignJournalPolicy.canMutateEntry(entryByPlayer1, archivedCampaign, gmSession, isActiveMember = true, isOnline = true))
        assertFalse(CampaignJournalPolicy.canCreate(activeCampaign, playerSession, isActiveMember = true, isOnline = false))
        assertFalse(CampaignJournalPolicy.canMutateEntry(entryByPlayer1, activeCampaign, playerSession, isActiveMember = true, isOnline = false))
    }

    @Test
    fun removedAuthorFallsBackWhenNameMissing() {
        assertEquals("Ex-participante", CampaignJournalPolicy.displayAuthor("", isActiveMemberOrOwner = false))
        assertEquals("Kael", CampaignJournalPolicy.displayAuthor(" Kael ", isActiveMemberOrOwner = false))
    }
}
