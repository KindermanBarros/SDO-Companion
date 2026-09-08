package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseOneCalculatedValuesTest {
    @Test fun attributeCalculationIncludesAdjustmentAndEquippedItemOrigin() {
        val item = InventoryItem(
            id = "glasses",
            name = "Óculos de Precisão",
            region = "cabeça",
            category = "Acessório",
            bonuses = listOf(ItemBonus(ItemBonusType.ATTRIBUTE, "AGI", 2)),
        )
        val attributes = defaultAttributes().map {
            if (it.acronym == "AGI") it.copy(value = 3, modifier = -1) else it
        }
        val character = Character(attributes = attributes, inventory = listOf(item))
            .equipItems(0, setOf(item.id))

        val calculation = character.attributeCalculation("AGI")

        assertEquals(3, calculation.base)
        assertEquals(-1, calculation.adjustment)
        assertEquals(4, calculation.total)
        assertEquals("Óculos de Precisão", calculation.modifiers.single().label)
        assertEquals(ModifierSourceType.ITEM, calculation.modifiers.single().sourceType)
    }

    @Test fun canonicalBasicKnowledgeTargetDoesNotAffectSimilarNames() {
        val item = InventoryItem(
            id = "reflex",
            name = "Reflex Booster",
            region = "cabeça",
            category = "Acessório",
            bonuses = listOf(
                ItemBonus(ItemBonusType.BASIC_KNOWLEDGE, ItemBonus.basicKnowledgeTarget("AGI", "Reflexos"), 2),
            ),
        )
        val character = Character(inventory = listOf(item)).equipItems(0, setOf(item.id))

        assertEquals(2, character.basicKnowledgeTotal("AGI", "Reflexos"))
        assertEquals(0, character.basicKnowledgeTotal("AGI", "Movimento"))
    }

    @Test fun unequippingItemRemovesModifierImmediately() {
        val item = InventoryItem(
            id = "power",
            name = "Amplificador",
            region = "torso",
            category = "Acessório",
            bonuses = listOf(ItemBonus(ItemBonusType.ATTRIBUTE, "FOR", 3)),
        )
        val equipped = Character(inventory = listOf(item)).equipItems(1, setOf(item.id))
        val unequipped = equipped.equipItems(1, emptySet())

        assertEquals(3, equipped.attributeTotal("FOR"))
        assertEquals(0, unequipped.attributeTotal("FOR"))
        assertTrue(unequipped.attributeCalculation("FOR").modifiers.isEmpty())
    }
}
