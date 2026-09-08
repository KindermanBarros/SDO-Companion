package com.kinderman.sdo.domain.policy

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterAccessPolicyTest {
    private val player = UserSession("player-1", "player@example.com", "Player", UserRole.USER)
    private val otherPlayer = UserSession("player-2", "other@example.com", "Other", UserRole.USER)
    private val admin = UserSession("admin", "kindbarros@gmail.com", "Admin", UserRole.ADMIN)

    @Test fun ownerCanEditAndDeleteUnlockedCharacter() {
        val character = Character(ownerId = player.uid)
        assertTrue(CharacterAccessPolicy.canEdit(player, character))
        assertTrue(CharacterAccessPolicy.canDelete(player, character))
    }

    @Test fun lockedOwnerCanEditAndDeleteOwnCharacter() {
        val character = Character(ownerId = player.uid, lockType = CharacterLock.PLAYER)
        assertTrue(CharacterAccessPolicy.canEdit(player, character))
        assertTrue(CharacterAccessPolicy.canDelete(player, character))
        assertTrue(CharacterAccessPolicy.canChangePlayerLock(player, character))
    }

    @Test fun administratorCanManageAnyCharacterEvenWhenLocked() {
        val character = Character(ownerId = player.uid, lockType = CharacterLock.HISTORIAN)
        assertTrue(CharacterAccessPolicy.canRead(admin, character))
        assertTrue(CharacterAccessPolicy.canEdit(admin, character))
        assertTrue(CharacterAccessPolicy.canDelete(admin, character))
        assertTrue(CharacterAccessPolicy.canChangeHistorianLock(admin))
        assertTrue(CharacterAccessPolicy.canTransferOwnership(admin))
    }

    @Test fun historianLockCannotBeChangedByOwner() {
        val character = Character(ownerId = player.uid, lockType = CharacterLock.HISTORIAN)
        assertFalse(CharacterAccessPolicy.canChangePlayerLock(player, character))
    }

    @Test fun otherPlayerCannotAccessCharacter() {
        val character = Character(ownerId = player.uid)
        assertFalse(CharacterAccessPolicy.canRead(otherPlayer, character))
        assertFalse(CharacterAccessPolicy.canEdit(otherPlayer, character))
        assertFalse(CharacterAccessPolicy.canDelete(otherPlayer, character))
        assertFalse(CharacterAccessPolicy.canTransferOwnership(otherPlayer))
    }

    @Test fun campaignHistorianCanManageLinkedCharacterButNotTransferOwnership() {
        val character = Character(ownerId = otherPlayer.uid, campaignId = "campaign")
        assertTrue(CharacterAccessPolicy.canRead(player, character, isCampaignHistorian = true))
        assertTrue(CharacterAccessPolicy.canEdit(player, character, isCampaignHistorian = true))
        assertFalse(CharacterAccessPolicy.canDelete(player, character, isCampaignHistorian = true))
        assertTrue(CharacterAccessPolicy.canChangeHistorianLock(player, isCampaignHistorian = true))
        assertFalse(CharacterAccessPolicy.canTransferOwnership(player, isCampaignResponsible = false))
        assertTrue(CharacterAccessPolicy.canTransferOwnership(player, isCampaignResponsible = true))
    }
}
