package com.kinderman.sdo.domain.policy

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterAccessPolicyTest {
    private val player = UserSession("player-1", "player@example.com", "Player", UserRole.PLAYER)
    private val otherPlayer = UserSession("player-2", "other@example.com", "Other", UserRole.PLAYER)
    private val master = UserSession("master", "kindbarros@gmail.com", "Historiador", UserRole.MASTER)

    @Test fun ownerCanEditAndDeleteUnlockedCharacter() {
        val character = Character(ownerId = player.uid)
        assertTrue(CharacterAccessPolicy.canEdit(player, character))
        assertTrue(CharacterAccessPolicy.canDelete(player, character))
    }

    @Test fun lockedCharacterIsReadOnlyForOwner() {
        val character = Character(ownerId = player.uid, isLocked = true)
        assertFalse(CharacterAccessPolicy.canEdit(player, character))
        assertFalse(CharacterAccessPolicy.canDelete(player, character))
    }

    @Test fun masterCanManageAnyCharacterEvenWhenLocked() {
        val character = Character(ownerId = player.uid, isLocked = true)
        assertTrue(CharacterAccessPolicy.canRead(master, character))
        assertTrue(CharacterAccessPolicy.canEdit(master, character))
        assertTrue(CharacterAccessPolicy.canDelete(master, character))
        assertTrue(CharacterAccessPolicy.canChangeLock(master))
    }

    @Test fun otherPlayerCannotAccessCharacter() {
        val character = Character(ownerId = player.uid)
        assertFalse(CharacterAccessPolicy.canRead(otherPlayer, character))
        assertFalse(CharacterAccessPolicy.canEdit(otherPlayer, character))
        assertFalse(CharacterAccessPolicy.canDelete(otherPlayer, character))
    }
}
