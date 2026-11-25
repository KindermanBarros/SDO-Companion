package com.kinderman.sdo.domain.creation

import com.kinderman.sdo.domain.model.AbilityCost
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterDivineAscension
import com.kinderman.sdo.domain.model.DivineAscensionActDraft
import com.kinderman.sdo.domain.model.DivineHeraldGiftDraft
import com.kinderman.sdo.domain.model.DivinePassiveCapability
import com.kinderman.sdo.domain.model.DivinePillars
import com.kinderman.sdo.domain.model.DivineTier
import com.kinderman.sdo.domain.model.ExecutionKind
import com.kinderman.sdo.domain.model.SpendableResource
import com.kinderman.sdo.domain.model.allCanonicalAbilitiesSafely
import com.kinderman.sdo.domain.creation.DivineAscensionCreation.hasPendingDivinePromotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DivineAscensionCreationTest {
    @Test
    fun lesserGodWizardValidatesFourStepsAndPromotesCharacterWithPdPowers() {
        val mortal = Character(id = "char-1", name = "Aurelia")
        val unlocked = DivineAscensionCreation.unlockPromotion(mortal, DivineTier.LESSER_GOD)
        assertTrue(unlocked.hasPendingDivinePromotion)
        assertNotNull(DivineAscensionCreation.validateStep(1, unlocked))

        val draft1 = DivineAscensionCreation.currentDraft(unlocked).copy(
            dominio = "Bruma",
            santuario = "Ilha Velada",
            postura = "Preservação",
            linhaDePensamento = "Ocultar os perseguidos sob a névoa.",
            tabu = "Jamais revelar um refugiado sob sua bruma.",
            step = 2,
        )
        val step1Done = DivineAscensionCreation.updateDraft(unlocked, draft1)
        assertNull(DivineAscensionCreation.validateStep(1, step1Done))
        assertNotNull(DivineAscensionCreation.validateStep(2, step1Done))

        val draft2 = draft1.copy(pillars = DivinePillars(2, 3, 2, 1), step = 3)
        val step2Done = DivineAscensionCreation.updateDraft(step1Done, draft2)
        assertNull(DivineAscensionCreation.validateStep(2, step2Done))
        assertNotNull(DivineAscensionCreation.validateStep(3, step2Done))

        val draft3 = draft2.copy(
            passiveCapabilities = listOf(
                DivinePassiveCapability(id = "p1", name = "Manto Cinzento", description = "Visão perfeita na névoa."),
                DivinePassiveCapability(id = "p2", name = "Passos Silentes", description = "Não deixa pegadas."),
            ),
            domainActs = listOf(
                DivineAscensionActDraft(id = "a1", name = "Cerrar Cerração", costPd = 2, description = "Cobre 15m com bruma densa."),
            ),
            heraldGift = DivineHeraldGiftDraft(
                name = "Sopro Húmido",
                executionKind = ExecutionKind.Free,
                effect = "Oculta o Arauto por 1 turno.",
                downside = "Voz sempre sussurrante.",
            ),
            step = 4,
        )
        val ready = DivineAscensionCreation.updateDraft(step2Done, draft3)
        assertNull(DivineAscensionCreation.validateStep(4, ready))

        val finished = DivineAscensionCreation.finish(ready)
        assertFalse(finished.hasPendingDivinePromotion)
        assertNull(finished.divineRuntime.ascensionDraft)
        assertEquals(DivineTier.LESSER_GOD, finished.divineAscension.tier)
        assertEquals(8, finished.divineAscension.pillars.totalPoints)
        assertEquals(4, finished.destiny.current)
        assertEquals("Sopro Húmido", finished.divineAscension.heraldGift.name)

        val pdAbilities = finished.allCanonicalAbilitiesSafely().filter {
            (it.cost as? AbilityCost.PowerCost)?.resource == SpendableResource.PD
        }
        assertEquals(3, pdAbilities.size)
        assertTrue(DivineAscensionCreation.isAuthorizedPlayerPromotionCompletion(unlocked.divineAscension, finished.divineAscension))
        assertFalse(
            DivineAscensionCreation.isAuthorizedPlayerPromotionCompletion(
                unlocked.divineAscension,
                finished.divineAscension.copy(tier = DivineTier.GREATER_GOD, pillars = DivinePillars(3, 3, 3, 3)),
            ),
        )
    }

    @Test
    fun greaterGodWizardEnforcesPlusFourBudgetWithoutReducingExistingPillars() {
        val lesser = Character(
            id = "char-2",
            name = "Aurelia",
            divineAscension = CharacterDivineAscension(
                tier = DivineTier.LESSER_GOD,
                dominio = "Bruma",
                santuario = "Ilha Velada",
                postura = "Preservação",
                linhaDePensamento = "Ocultar os perseguidos.",
                tabu = "Jamais trair refugiados.",
                pillars = DivinePillars(2, 3, 2, 2), // grown via milestone to 9!
                passiveCapabilities = listOf(
                    DivinePassiveCapability(id = "p1", name = "Manto", description = "Visão."),
                    DivinePassiveCapability(id = "p2", name = "Passos", description = "Silêncio."),
                ),
            ),
        )
        val unlocked = DivineAscensionCreation.unlockPromotion(lesser, DivineTier.GREATER_GOD)
        val baseDraft = DivineAscensionCreation.currentDraft(unlocked)

        // Reducing aspecto from 2 to 1 must fail even if total is 9 + 4 = 13
        val reducedDraft = baseDraft.copy(pillars = DivinePillars(1, 5, 4, 3))
        assertNotNull(DivineAscensionCreation.validateStep(2, unlocked, reducedDraft))

        val validDraft = baseDraft.copy(
            pillars = DivinePillars(3, 5, 3, 2), // 2+1, 3+2, 2+1, 2+0 = 13 (9 + 4), none reduced
            domainActs = listOf(DivineAscensionActDraft(id = "a2", name = "Bruma Continental", costPd = 5, description = "Cobre a costa.")),
        )
        val ready = DivineAscensionCreation.updateDraft(unlocked, validDraft)
        val finished = DivineAscensionCreation.finish(ready)

        assertEquals(DivineTier.GREATER_GOD, finished.divineAscension.tier)
        assertEquals(13, finished.divineAscension.pillars.totalPoints)
        assertEquals(10, finished.destinyMaximum)
        assertEquals(5, finished.destiny.current)
        assertTrue(DivineAscensionCreation.isAuthorizedPlayerPromotionCompletion(unlocked.divineAscension, finished.divineAscension))
    }
}
