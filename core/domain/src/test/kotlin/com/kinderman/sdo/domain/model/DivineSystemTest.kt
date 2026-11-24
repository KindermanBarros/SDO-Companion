package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DivineSystemTest {
    @Test
    fun greaterGodCeilingIncludesDestinyAdjustment() {
        val character = Character(destiny = ResourceValue(adjustment = 2))
        val lesser = character.copy(divineAscension = CharacterDivineAscension(tier = DivineTier.LESSER_GOD))
        val greater = character.copy(divineAscension = CharacterDivineAscension(tier = DivineTier.GREATER_GOD))

        assertEquals(5, lesser.derivedDivineEffects().destinyBaseCeiling)
        assertEquals(10, greater.derivedDivineEffects().destinyBaseCeiling)
        assertEquals(12, greater.destinyMaximum)
        assertTrue(greater.derivedDivineEffects().hasMaterialRestriction)
        assertTrue(greater.derivedDivineEffects().immuneToMortalMentalEffects)
    }

    @Test
    fun resistanceUsesPillarAndInvestedDestiny() {
        assertEquals(22, DivineRules.calculateDirectResistanceDc(4, 3))
    }

    @Test
    fun sanctuaryBreaksClashTieBeforeTier() {
        val result = DivineRules.resolveDeityClash(
            attackerPillar = 4,
            attackerPdInvested = 1,
            defenderPillar = 3,
            defenderPdInvested = 2,
            defenderInOwnSanctuary = true,
            attackerTier = DivineTier.GREATER_GOD,
        )

        assertEquals(DeityClashWinner.DEFENDER, result.winner)
        assertEquals(
            DeityClashWinner.ATTACKER,
            DivineRules.resolveDeityClash(4, 1, 3, 2, attackerTier = DivineTier.GREATER_GOD).winner,
        )
    }

    @Test
    fun tabuBlocksDomainActsUntilSceneReset() {
        val character = Character(destiny = ResourceValue(current = 3))

        val penalized = character.violateDivineTabu()
        val reset = penalized.resetDivineScene()

        assertEquals(1, penalized.destiny.current)
        assertEquals(2, penalized.divineRuntime.pdSpentInCurrentScene)
        assertTrue(penalized.derivedDivineEffects().domainActsBlockedByTabu)
        assertFalse(reset.derivedDivineEffects().domainActsBlockedByTabu)
        assertEquals(0, reset.divineRuntime.pdSpentInCurrentScene)
        assertEquals(1, reset.destiny.current)
    }

    @Test
    fun avatarCorruptionDependsOnPatronAndRoundsUp() {
        val avatar = Character(
            divineAscension = CharacterDivineAscension(
                tier = DivineTier.AVATAR,
                patronDeityId = "deity.patron",
            ),
        )

        val fromPatron = avatar.applyDivineCorruption(sourceDeityId = "deity.patron", rawAmount = 9)
        val fromOther = avatar.applyDivineCorruption(sourceDeityId = "deity.other", rawAmount = 9)

        assertEquals(0, fromPatron.corruption.current)
        assertEquals(5, fromOther.corruption.current)
    }

    @Test
    fun activeMarksDetermineLivingAnchorStatus() {
        val mark = DivineMark(id = "mark", deityId = "deity.patron")
        val marked = Character().withAddedDivineMark(mark)

        assertTrue(marked.derivedDivineEffects().isLivingSensoryAnchor)
        assertFalse(
            marked.withUpdatedDivineMark(mark.copy(active = false))
                .derivedDivineEffects().isLivingSensoryAnchor,
        )
        assertTrue(marked.withRemovedDivineMark(mark.id).divineRuntime.marks.isEmpty())
    }

    @Test
    fun tacticalConversionSpendsRoundedUpTenPercentOfMaximumSanity() {
        val avatar = Character(
            divineAscension = CharacterDivineAscension(tier = DivineTier.AVATAR),
            sanity = ResourceValue(current = 10, adjustment = 1),
            destiny = ResourceValue(current = 0),
        )
        val cost = (avatar.sanityMaximum + 9) / 10

        val recovered = avatar.convertVitalityOrAnchorToDestiny()

        assertEquals(avatar.sanity.current - cost, recovered.sanity.current)
        assertEquals(1, recovered.destiny.current)
    }
}
