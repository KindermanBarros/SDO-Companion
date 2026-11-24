package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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

    @Test
    fun ascensionRequiresInitialBudgetAndPromotionAddsFourWithoutReducingPillars() {
        val mortal = CharacterDivineAscension()
        val lesser = CharacterDivineAscension(tier = DivineTier.LESSER_GOD, pillars = DivinePillars(2, 2, 2, 2))
        assertTrue(DivineRules.validAscensionPillars(mortal, lesser))
        assertFalse(DivineRules.validAscensionPillars(mortal, lesser.copy(pillars = DivinePillars(1, 1, 1, 1))))
        val greater = lesser.copy(tier = DivineTier.GREATER_GOD, pillars = DivinePillars(3, 3, 3, 3))
        assertTrue(DivineRules.validAscensionPillars(lesser, greater))
        assertFalse(DivineRules.validAscensionPillars(lesser, greater.copy(pillars = DivinePillars(1, 4, 4, 3))))
        assertFalse(DivineRules.validAscensionPillars(lesser, greater.copy(pillars = DivinePillars(2, 2, 2, 2))))
    }

    @Test
    fun sameTierEditsPreserveGrownTotalAndNeverRefundDestiny() {
        val ascension = CharacterDivineAscension(tier = DivineTier.LESSER_GOD, pillars = DivinePillars(3, 3, 2, 2))
        val character = Character(divineAscension = ascension, destiny = ResourceValue(current = 0))
        val edited = character.withDivineAscension(ascension.copy(dominio = "Novo domínio"))
        assertEquals(10, edited.divineAscension.pillars.totalPoints)
        assertEquals(0, edited.destiny.current)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidAscensionBudgetIsRejectedByDomain() {
        Character().withDivineAscension(CharacterDivineAscension(tier = DivineTier.LESSER_GOD))
    }

    @Test
    fun avatarSessionAndManualSpendingAccumulateWithoutRefundingCounter() {
        val avatar = Character(divineAscension = CharacterDivineAscension(tier = DivineTier.AVATAR), destiny = ResourceValue(current = 5))
        val spent = avatar.applySessionCommand(SessionCommand(type = SessionOperationType.DESTINY, amount = -1), "owner").character
        val manual = spent.withTrackedDestiny(spent.destiny.copy(current = 3))
        val recovered = manual.applySessionCommand(SessionCommand(type = SessionOperationType.DESTINY, amount = 1), "owner").character
        assertEquals(2, recovered.divineRuntime.pdSpentInCurrentScene)
        assertEquals(4, recovered.destiny.current)
        assertEquals(null, recovered.derivedDivineEffects().avatarAutonomyCheckDc)
        assertEquals(15, recovered.derivedDivineEffects(0).avatarAutonomyCheckDc)
        assertEquals(20, recovered.derivedDivineEffects(5).avatarAutonomyCheckDc)
    }

    @Test
    fun manualDestinyUsesGreaterGodCeilingAndAdjustment() {
        val god = Character(divineAscension = CharacterDivineAscension(tier = DivineTier.GREATER_GOD), destiny = ResourceValue(current = 4, adjustment = 2))
        val updated = god.withTrackedDestiny(god.destiny.copy(current = 11))
        assertEquals(11, updated.destiny.current)
        assertEquals(12, updated.destiny.maximum)
    }

    @Test
    fun saturationExhaustsOnlyOnFirstChannelOfSceneIncludingFreeActs() {
        val character = Character(corruption = ResourceValue(current = 75), destiny = ResourceValue(current = 5))
        val first = character.performDomainAct(0)
        val second = first.performDomainAct(1)
        assertEquals(1, first.exhaustion.current)
        assertEquals(1, second.exhaustion.current)
        assertEquals(2, second.resetDivineScene().performDomainAct(0).exhaustion.current)
        assertEquals(0, character.copy(corruption = ResourceValue(current = 74)).performDomainAct(0).exhaustion.current)
    }

    @Test(expected = IllegalArgumentException::class)
    fun ruptureBlocksEvenFreeDomainActs() {
        Character(corruption = ResourceValue(current = 100)).performDomainAct(0)
    }

    @Test
    fun purificationRestoresChannelingWithoutResettingScene() {
        val character = Character(corruption = ResourceValue(current = 100))
        val restored = character.applyDivineCorruption(rawAmount = -5).performDomainAct(0)
        assertEquals(95, restored.corruption.current)
        assertEquals(1, restored.exhaustion.current)
        assertTrue(restored.divineRuntime.channeledInCurrentScene)
    }

    @Test
    fun avatarTabuTriggersAutonomyWithoutTwoDestinyAvailable() {
        val avatar = Character(divineAscension = CharacterDivineAscension(tier = DivineTier.AVATAR), destiny = ResourceValue(current = 0))
        assertEquals(17, avatar.violateDivineTabu().derivedDivineEffects(2).avatarAutonomyCheckDc)
    }

    @Test
    fun divineSceneChannelingRoundTripsAndOlderPayloadsDefaultToFalse() {
        val runtime = CharacterDivineRuntime(channeledInCurrentScene = true, pdSpentInCurrentScene = 2)
        assertEquals(runtime, Json.decodeFromString<CharacterDivineRuntime>(Json.encodeToString(runtime)))
        assertFalse(Json.decodeFromString<CharacterDivineRuntime>("{}").channeledInCurrentScene)
        val character = Character(divineRuntime = runtime)
        assertFalse(character.resetDivineScene().divineRuntime.channeledInCurrentScene)
    }

    private fun characterWithSustainedEffects(): Character {
        val sustained = Duration(DurationKind.Scene, requiresChanneling = true)
        val timed = Duration(DurationKind.Scene)
        val source = Source.narrative(NarrativeSourceId("regression"))
        val modifier = ActiveModifier(
            modifierId = "sustained",
            target = BonusTarget.AttributeTarget(Attribute.FOR),
            amount = 2,
            source = source,
            duration = sustained,
        )
        return Character(
            corruption = ResourceValue(current = 95),
            activeModifiers = listOf(modifier, modifier.copy(modifierId = "ordinary", duration = timed)),
        ).withCanonicalConditions(listOf(
            ConditionInstance(instanceId = ConditionInstanceId("sustained"), payload = ConditionPayload(duration = sustained)),
            ConditionInstance(instanceId = ConditionInstanceId("ordinary"), payload = ConditionPayload(duration = timed)),
        ))
    }

    @Test
    fun ruptureTerminatesOnlyExplicitlySustainedEffectsThroughEveryResourceEntryPoint() {
        val character = characterWithSustainedEffects()
        val exposed = character.applyDivineCorruption(rawAmount = 5)
        val manual = character.withDivineCorruption(ResourceValue(current = 100))
        val session = character.applySessionCommand(
            SessionCommand(type = SessionOperationType.RESOURCE, resource = SessionResource.CORRUPTION, amount = 5),
            "owner",
        ).character
        listOf(exposed, manual, session).forEach { ruptured ->
            assertEquals(100, ruptured.corruption.current)
            assertEquals(listOf("ordinary"), ruptured.activeModifiers.map { it.modifierId })
            assertEquals(listOf("ordinary"), ruptured.canonicalConditions().map { it.instanceId.value })
            assertEquals(1, ruptured.conditions.size)
            assertEquals(ruptured, ruptured.withDivineCorruption(ruptured.corruption))
        }
    }

    @Test
    fun belowRupturePreservesEffectsAndPurificationNeverRestoresTerminatedEffects() {
        val character = characterWithSustainedEffects()
        assertEquals(character.activeModifiers, character.applyDivineCorruption(rawAmount = 4).activeModifiers)
        val purified = character.applyDivineCorruption(rawAmount = 5).applyDivineCorruption(rawAmount = -5)
        assertEquals(95, purified.corruption.current)
        assertEquals(listOf("ordinary"), purified.activeModifiers.map { it.modifierId })
        assertEquals(listOf("ordinary"), purified.canonicalConditions().map { it.instanceId.value })
    }

    @Test
    fun sustainedDurationRoundTripsAndLegacyDurationsDefaultToNotSustained() {
        val duration = Duration(DurationKind.Scene, requiresChanneling = true)
        assertEquals(duration, Json.decodeFromString<Duration>(Json.encodeToString(duration)))
        assertFalse(Json.decodeFromString<Duration>("{\"kind\":\"Scene\"}").requiresChanneling)
    }

    @Test
    fun sustainedAbilityExecutionProducesEffectsThatEndAtRupture() {
        val source = Source.narrative(NarrativeSourceId("manual"))
        val ability = Ability(
            id = "sustained-spell", name = "Maintained spell", kind = AbilityKind.SPELL,
            cost = AbilityCost.SpellCost(0),
            duration = Duration(DurationKind.Scene, requiresChanneling = true),
            mechanicalEffect = MechanicalEffect(listOf(
                EffectOperation.AddModifier(ActiveModifier(
                    target = BonusTarget.AttributeTarget(Attribute.FOR), amount = 2, source = source,
                )),
                EffectOperation.ApplyCondition(ConditionPayload()),
            )),
        )
        val character = Character(abilities = listOf(ability), corruption = ResourceValue(current = 95))
        val cast = character.applySessionCommand(
            SessionCommand(type = SessionOperationType.ABILITY_USE, targetId = ability.id), "owner",
        ).character
        assertTrue(cast.activeModifiers.single().duration?.requiresChanneling == true)
        assertTrue(cast.canonicalConditions().single().duration?.requiresChanneling == true)
        val ruptured = cast.applyDivineCorruption(rawAmount = 5)
        assertTrue(ruptured.activeModifiers.isEmpty())
        assertTrue(ruptured.canonicalConditions().isEmpty())
    }

    @Test
    fun durationEditsPreserveMaintenanceWithoutInferringItForTimedEffects() {
        val duration = Duration(DurationKind.Scene, requiresChanneling = true)
        DurationKind.entries.forEach { kind ->
            assertTrue(duration.withKind(kind).requiresChanneling)
            assertFalse(Duration(DurationKind.Scene).withKind(kind).requiresChanneling)
        }
        val ordinary = EffectOperation.ApplyCondition(ConditionPayload(duration = Duration(DurationKind.Scene)))
        assertEquals(ordinary, ordinary.withChannelingDuration(Duration(DurationKind.Scene)))
        val maintained = ordinary.withChannelingDuration(duration) as EffectOperation.ApplyCondition
        assertTrue(maintained.condition.duration?.requiresChanneling == true)
    }

    @Test
    fun suppressedPresenceChangesDerivedSummaryAndEffectiveResistance() {
        val god = Character(divineAscension = CharacterDivineAscension(
            tier = DivineTier.LESSER_GOD, pillars = DivinePillars(2, 2, 2, 2),
        ))
        val suppressed = god.suppressDivinePillar(DivinePillarKind.PRESENCA, 2, "cause", "scene")
        assertEquals(DivineRules.presencaQualitativeDescription(0), suppressed.derivedDivineEffects().presencaQualitativeSummary)
        assertEquals(15, DivineRules.calculateDirectResistanceDc(suppressed.currentDivinePillars().presenca))
        assertEquals(2, suppressed.divineAscension.pillars.presenca)
    }

    @Test
    fun sealedAndErasedFatesRejectDomainActsAndChannelingWithoutMutation() {
        listOf(DivineFate.SEALED, DivineFate.ERASED).forEach { fate ->
            val character = Character(
                destiny = ResourceValue(current = 5), corruption = ResourceValue(current = 80),
                divineRuntime = CharacterDivineRuntime(divineFate = fate),
            )
            assertTrue(runCatching { character.performDomainAct(2) }.exceptionOrNull() is IllegalArgumentException)
            assertTrue(runCatching { character.channelDivineOrArcanePower() }.exceptionOrNull() is IllegalArgumentException)
            assertEquals(5, character.destiny.current)
            assertEquals(0, character.exhaustion.current)
            assertFalse(character.divineRuntime.channeledInCurrentScene)
        }
    }

    @Test
    fun defeatedFateDoesNotImplyChannelingBan() {
        val defeated = Character(divineRuntime = CharacterDivineRuntime(divineFate = DivineFate.DEFEATED))
        assertTrue(defeated.performDomainAct(0).divineRuntime.channeledInCurrentScene)
    }

    @Test
    fun sameTierAscensionCannotRedistributePillarsEvenWithUnchangedTotal() {
        listOf(DivineTier.LESSER_GOD, DivineTier.GREATER_GOD).forEach { tier ->
            val ascension = CharacterDivineAscension(tier = tier, pillars = DivinePillars(3, 3, 2, 2))
            val redistributed = ascension.copy(pillars = DivinePillars(4, 2, 2, 2))
            val character = Character(divineAscension = ascension)
            assertFalse(DivineRules.validAscensionPillars(ascension, redistributed))
            assertTrue(runCatching { character.withDivineAscension(redistributed) }.exceptionOrNull() is IllegalArgumentException)
            assertEquals(ascension, character.divineAscension)
            assertTrue(DivineRules.validAscensionPillars(ascension, ascension.copy(dominio = "Profile edit")))
        }
    }
}
