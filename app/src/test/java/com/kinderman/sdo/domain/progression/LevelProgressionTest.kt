package com.kinderman.sdo.domain.progression

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ProgressionReward
import com.kinderman.sdo.domain.model.ProgressionRewardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LevelProgressionTest {
    @Test fun `applies every intermediate level atomically`() {
        val character = Character()
        val rewards = listOf(
            ProgressionReward(2, ProgressionRewardType.RESOURCE, "ENERGY"),
            ProgressionReward(2, ProgressionRewardType.KNOWLEDGE, "Vitalidade"),
            ProgressionReward(2, ProgressionRewardType.ATTRIBUTE, "VIG"),
            ProgressionReward(3, ProgressionRewardType.RESOURCE, "ARCANE"),
            ProgressionReward(3, ProgressionRewardType.KNOWLEDGE, "Sanidade"),
        )
        val result = LevelProgression.apply(character, 3, rewards, emptyList(), now = 42)
        assertEquals(3, result.level)
        assertEquals(2, result.progressionSanityBonus)
        assertEquals(1, result.progressionEnergyBonus)
        assertEquals(1, result.progressionArcaneBonus)
        assertEquals(1, result.progressionHistory.size)
        assertEquals(42, result.progressionHistory.single().appliedAt)
    }

    @Test fun `rejects incomplete selection without changing source character`() {
        val character = Character()
        assertThrows(IllegalArgumentException::class.java) {
            LevelProgression.apply(character, 2, listOf(ProgressionReward(2, ProgressionRewardType.RESOURCE, "ENERGY")), emptyList())
        }
        assertEquals(1, character.level)
        assertEquals(0, character.progressionEnergyBonus)
    }
}
