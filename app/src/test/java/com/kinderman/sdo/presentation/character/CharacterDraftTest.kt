package com.kinderman.sdo.presentation.character

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterDraftTest {
    @Test
    fun `stale repository emissions never replace newer typing`() {
        assertFalse(shouldReplaceDraft(currentUpdatedAt = 200, incomingUpdatedAt = 199))
        assertFalse(shouldReplaceDraft(currentUpdatedAt = 200, incomingUpdatedAt = 200))
        assertTrue(shouldReplaceDraft(currentUpdatedAt = 200, incomingUpdatedAt = 201))
    }
}
