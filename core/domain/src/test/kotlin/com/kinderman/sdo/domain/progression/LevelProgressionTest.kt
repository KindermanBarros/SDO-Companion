package com.kinderman.sdo.domain.progression

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ProgressionReward
import com.kinderman.sdo.domain.model.ProgressionRewardType
import com.kinderman.sdo.domain.model.basicKnowledgeId
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.PowerSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LevelProgressionTest {
    @Test fun `applies every intermediate level atomically`() {
        val character = Character()
        val rewards = listOf(
            ProgressionReward(2, ProgressionRewardType.RESOURCE, "ENERGY"),
            ProgressionReward(2, ProgressionRewardType.KNOWLEDGE, basicKnowledgeId("VIG", "Vitalidade")),
            ProgressionReward(2, ProgressionRewardType.ATTRIBUTE, "VIG"),
            ProgressionReward(3, ProgressionRewardType.RESOURCE, "ARCANE"),
            ProgressionReward(3, ProgressionRewardType.KNOWLEDGE, basicKnowledgeId("INT", "Sanidade")),
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

    @Test fun `level five accepts a path enhancement instead of forcing a new power`() {
        val power = Power(id = "path-power", name = "Legado", sourceType = PowerSourceType.PATH, enhancements = "Efeito ampliado")
        val character = Character(level = 4, powers = listOf(power))
        val rewards = listOf(
            ProgressionReward(5, ProgressionRewardType.RESOURCE, "ENERGY"),
            ProgressionReward(5, ProgressionRewardType.KNOWLEDGE, basicKnowledgeId("VIG", "Vitalidade")),
            ProgressionReward(5, ProgressionRewardType.ATTRIBUTE, "VIG"),
            ProgressionReward(5, ProgressionRewardType.NEW_KNOWLEDGE, catalogEntryId = "new-knowledge"),
            ProgressionReward(5, ProgressionRewardType.PATH_ENHANCEMENT, targetId = power.id),
        )
        val knowledge = com.kinderman.sdo.domain.model.CatalogEntry("new-knowledge", com.kinderman.sdo.domain.model.CatalogKind.ACQUIRED_KNOWLEDGE, "Ofício", "", "", relatedAttribute = "VIG")
        val result = LevelProgression.apply(character, 5, rewards, listOf(knowledge))
        assertEquals(true, result.powers.single().effect.contains("Efeito ampliado"))
        assertEquals("", result.powers.single().enhancements)
    }
}
