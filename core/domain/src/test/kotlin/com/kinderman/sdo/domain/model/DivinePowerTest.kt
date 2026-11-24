package com.kinderman.sdo.domain.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DivinePowerTest {
    @Test
    fun sceneDisruptionExpiresFullyWithoutRemovingPersistentSuppressionOrCauseHistory() {
        val persistent = deity().suppressDivinePillar(
            DivinePillarKind.PRESENCA, 1, "persistent", "scene",
        )
        val disrupted = persistent.suppressDivinePillar(
            DivinePillarKind.PRESENCA, 2, "manifestation", "scene", sceneOnly = true,
        )
        assertEquals(0, disrupted.currentDivinePillars().presenca)
        assertEquals(1, disrupted.divineRuntime.pillarSuppression.presenca)
        assertEquals(2, disrupted.divineRuntime.scenePillarSuppression.presenca)
        assertEquals(setOf("manifestation"), disrupted.divineRuntime.sceneSuppressionCauseIds)
        assertEquals(disrupted.divineRuntime, Json.decodeFromString<CharacterDivineRuntime>(
            Json.encodeToString(disrupted.divineRuntime),
        ))
        val reset = disrupted.resetDivineScene()
        assertEquals(DivinePillars(), reset.divineRuntime.scenePillarSuppression)
        assertTrue(reset.divineRuntime.sceneSuppressionCauseIds.isEmpty())
        assertEquals(persistent.currentDivinePillars(), reset.currentDivinePillars())
        assertEquals(persistent.divineRuntime.pillarSuppression, reset.divineRuntime.pillarSuppression)
        assertEquals(reset, reset.suppressDivinePillar(
            DivinePillarKind.PRESENCA, 2, "manifestation", "next", sceneOnly = true,
        ))
        val sceneOnly = deity().suppressDivinePillar(
            DivinePillarKind.ASPECTO, 2, "disruption", "scene", sceneOnly = true,
        )
        assertEquals(2, sceneOnly.resetDivineScene().currentDivinePillars().aspecto)
    }

    private fun deity(presence: Int = 2, pd: Int = 4): Character = Character(
        divineAscension = CharacterDivineAscension(
            tier = DivineTier.LESSER_GOD,
            pillars = DivinePillars(2, 2, 2, presence),
        ),
        destiny = ResourceValue(current = pd),
    )

    private fun linked(presence: Int = 1, pd: Int = 4): Character = deity(presence, pd).copy(
        divineRuntime = CharacterDivineRuntime(links = listOf(
            DivineLink("old", DivineLinkKind.AVATAR),
            DivineLink("middle"),
            DivineLink("new"),
        )),
    )

    @Test
    fun overloadChargesOncePerSceneIncludingAfterCounterResetAndSerialization() {
        val paid = linked().settleDivineLinks("scene")
        assertEquals(2, paid.destiny.current)
        assertEquals(2, paid.divineRuntime.pdSpentInCurrentScene)
        assertEquals(paid, paid.settleDivineLinks("scene"))
        val restored = paid.resetDivineScene().copy(divineRuntime = Json.decodeFromString(
            Json.encodeToString(paid.resetDivineScene().divineRuntime),
        ))
        assertEquals(2, restored.settleDivineLinks("scene").destiny.current)
        assertEquals(0, paid.settleDivineLinks("next").destiny.current)
    }

    @Test
    fun suspensionIsNewestFirstWithoutDebtAndZeroPresenceForbidsOverload() {
        val poor = linked(pd = 1).settleDivineLinks("scene")
        assertEquals(listOf(true, true, false), poor.divineRuntime.links.map { it.active })
        assertEquals(0, poor.destiny.current)
        val absent = linked(presence = 0).settleDivineLinks("scene")
        assertTrue(absent.divineRuntime.links.none { it.active })
        assertEquals(4, absent.destiny.current)
        val large = linked().withDivineLink(DivineLink("latest", DivineLinkKind.AVATAR), "scene")
        assertFalse(large.divineRuntime.links.last().active)
        assertEquals(4, large.divineLinkLoad())
    }

    @Test
    fun suppressionCapsDeduplicatesAndImmediatelySettlesCapacityDrop() {
        val base = linked(presence = 2, pd = 0)
        val weakened = base.suppressDivinePillar(DivinePillarKind.PRESENCA, 1, "cause", "scene")
        assertEquals(1, weakened.currentDivinePillars().presenca)
        assertEquals(listOf(true, false, false), weakened.divineRuntime.links.map { it.active })
        assertEquals(weakened, weakened.suppressDivinePillar(
            DivinePillarKind.ASPECTO, 2, "cause", "scene",
        ))
        val capped = weakened.suppressDivinePillar(DivinePillarKind.PRESENCA, 2, "other", "scene")
        assertEquals(2, capped.divineRuntime.pillarSuppression.presenca)
        assertEquals(0, capped.currentDivinePillars().presenca)
        assertEquals(base, base.suppressDivinePillar(DivinePillarKind.PRESENCA, 3, "bad", "scene"))
    }

    @Test
    fun restorationRequiresResolvedCauseAndCannotFarmScenesOrSessions() {
        val weak = deity().suppressDivinePillar(DivinePillarKind.ASPECTO, 2, "cause", "attack")
        assertEquals(weak, weak.restoreDivinePillar(DivinePillarKind.ASPECTO, "restore", "session"))
        val recovered = weak.restoreDivinePillar(DivinePillarKind.ASPECTO, "restore", "session", true)
        assertEquals(1, recovered.divineRuntime.pillarSuppression.aspecto)
        assertEquals(recovered, recovered.restoreDivinePillar(DivinePillarKind.ASPECTO, "other", "session", true))
        assertEquals(recovered, recovered.restoreDivinePillar(DivinePillarKind.ASPECTO, "restore", "other", true))
        assertEquals(0, recovered.restoreDivinePillar(DivinePillarKind.ASPECTO, "next", "next", true)
            .divineRuntime.pillarSuppression.aspecto)
    }

    @Test
    fun permanentChangesUseAscensionWithSeparateArcLimitsAndMilestoneDeduplication() {
        val base = deity()
        val grown = base.changePermanentDivinePillar(DivinePillarKind.ASPECTO, 1, "arc", "gain", "scene")
        assertEquals(3, grown.divineAscension.pillars.aspecto)
        assertEquals(grown, grown.changePermanentDivinePillar(DivinePillarKind.DOMINIO, 1, "arc", "new", "scene"))
        assertEquals(grown, grown.changePermanentDivinePillar(DivinePillarKind.DOMINIO, 1, "new", "gain", "scene"))
        val lost = grown.changePermanentDivinePillar(DivinePillarKind.ASPECTO, -1, "arc", "loss", "scene")
        assertEquals(2, lost.divineAscension.pillars.aspecto)
        assertEquals(lost, lost.changePermanentDivinePillar(DivinePillarKind.ASPECTO, -1, "arc", "loss2", "scene"))
        for (tier in listOf(DivineTier.LESSER_GOD, DivineTier.GREATER_GOD)) {
            val cap = if (tier == DivineTier.LESSER_GOD) 4 else 5
            val full = base.copy(divineAscension = base.divineAscension.copy(
                tier = tier, pillars = DivinePillars(aspecto = cap),
            ))
            assertEquals(full, full.changePermanentDivinePillar(DivinePillarKind.ASPECTO, 1, "cap", "cap", "scene"))
        }
    }

    @Test
    fun defeatAndSealingNeverEraseAndValidationMatchesRejectedMutations() {
        val base = deity()
        val defeated = base.withDivineFate(DivineFate.DEFEATED, "defeat")
        assertTrue(defeated.canPerformDivineActs())
        assertNotNull(base.divineFateValidationError(DivineFate.SEALED, "seal"))
        assertEquals(base, base.withDivineFate(DivineFate.SEALED, "seal"))
        val sealed = defeated.withDivineFate(DivineFate.SEALED, "seal", "vulnerability")
        assertFalse(sealed.canPerformDivineActs())
        assertFalse(sealed.canDelegateDivinePower())
        assertEquals(sealed, sealed.withDivineLink(DivineLink("link"), "scene"))
        assertEquals(sealed, sealed.withDivineFate(DivineFate.ERASED, "death", "vulnerability"))
    }

    @Test
    fun erasureRequiresEverySafeguardAndExactlySevenPd() {
        val target = deity()
        val attacker = deity(pd = 7).copy(divineAscension = CharacterDivineAscension(tier = DivineTier.GREATER_GOD))
        assertEquals(attacker to target, attacker.eraseLesserDeity(target, "death", "weakness"))
        for (flags in listOf(Triple(false, true, true), Triple(true, false, true), Triple(true, true, false))) {
            assertEquals(attacker to target, attacker.eraseLesserDeity(target, "death", "weakness", flags.first, flags.second, flags.third))
        }
        val (spent, erased) = attacker.eraseLesserDeity(target, "death", "weakness", true, true, true)
        assertEquals(0, spent.destiny.current)
        assertEquals(7, spent.divineRuntime.pdSpentInCurrentScene)
        assertEquals(DivineFate.ERASED, erased.divineRuntime.divineFate)
        assertEquals(spent to erased, spent.eraseLesserDeity(erased, "death", "weakness", true, true, true))
    }

    @Test
    fun legacyDefaultsAndNewRuntimeRoundTrip() {
        assertEquals(CharacterDivineRuntime(), Json.decodeFromString<CharacterDivineRuntime>("{}"))
        val runtime = linked().settleDivineLinks("scene")
            .suppressDivinePillar(DivinePillarKind.ASPECTO, 1, "cause", "scene").divineRuntime
        assertEquals(runtime, Json.decodeFromString<CharacterDivineRuntime>(Json.encodeToString(runtime)))
    }

    @Test
    fun rejectedInputsHaveValidationErrorsAndLeaveStateUnchanged() {
        val base = deity()
        assertNotNull(base.divineLinkValidationError(DivineLink(), "scene"))
        assertEquals(base, base.withDivineLink(DivineLink(), "scene"))
        assertNotNull(base.divineSuppressionValidationError(0, "cause", "scene"))
        assertEquals(base, base.suppressDivinePillar(DivinePillarKind.ASPECTO, 0, "cause", "scene"))
        assertNotNull(base.divineRestorationValidationError(DivinePillarKind.ASPECTO, "scene", "session"))
        assertEquals(base, base.restoreDivinePillar(DivinePillarKind.ASPECTO, "scene", "session"))
        assertNotNull(base.permanentDivinePillarValidationError(DivinePillarKind.ASPECTO, 2, "arc", "milestone", "scene"))
        assertEquals(base, base.changePermanentDivinePillar(DivinePillarKind.ASPECTO, 2, "arc", "milestone", "scene"))
        assertNotNull(base.divineErasureValidationError(base, "cause", "weakness"))
        assertEquals(base to base, base.eraseLesserDeity(base, "cause", "weakness"))
    }

    @Test
    fun removingAndReaddingLinksDoesNotRechargeTheSameScene() {
        val paid = linked().settleDivineLinks("scene")
        val replaced = paid.withRemovedDivineLink("new").withDivineLink(DivineLink("replacement"), "scene")
        assertEquals(paid.destiny.current, replaced.destiny.current)
        assertEquals(2, replaced.divineLinkOverloadCost())
        assertEquals(2, replaced.divineLinkCapacity())
        assertEquals(4, replaced.divineLinkLoad())
    }
}
