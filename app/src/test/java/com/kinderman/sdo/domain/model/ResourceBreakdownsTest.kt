package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceBreakdownsTest {
    @Test fun lifeMaximumBreakdownKeepsItemBonusVisible() {
        val item = InventoryItem(
            id = "vitality",
            name = "Colete Vital",
            region = "torso",
            category = "Acessório",
            bonuses = listOf(
                ItemBonus(
                    ItemBonusType.BASIC_KNOWLEDGE,
                    ItemBonus.basicKnowledgeTarget("VIG", "Vitalidade"),
                    2,
                ),
            ),
        )
        val attributes = defaultAttributes().map { attribute ->
            if (attribute.acronym != "VIG") attribute else attribute.copy(
                skills = attribute.skills.map { skill ->
                    if (skill.name == "Vitalidade") skill.copy(value = 3, modifier = 1) else skill
                },
            )
        }
        val character = Character(
            life = ResourceValue(adjustment = -1),
            attributes = attributes,
            inventory = listOf(item),
        ).equipItems(1, setOf(item.id))

        val value = character.lifeMaximumBreakdown()

        assertEquals(13, value.base)
        assertEquals(0, value.adjustment)
        assertEquals(15, value.total)
        assertTrue(value.modifiers.any { it.label == "Colete Vital" && it.value == 2 })
    }

    @Test fun loadCapacityBreakdownIncludesStrengthItem() {
        val item = InventoryItem(
            id = "strength",
            name = "Exoesqueleto",
            region = "torso",
            category = "Acessório",
            bonuses = listOf(ItemBonus(ItemBonusType.ATTRIBUTE, "FOR", 2)),
        )
        val attributes = defaultAttributes().map { if (it.acronym == "FOR") it.copy(value = 3) else it }
        val character = Character(
            attributes = attributes,
            containerCapacity = 5,
            inventory = listOf(item),
        ).equipItems(1, setOf(item.id))

        val value = character.loadCapacityBreakdown()

        assertEquals(5, value.base)
        assertEquals(5, value.adjustment)
        assertEquals(12, value.total)
        assertEquals("Exoesqueleto", value.modifiers.single().label)
    }
}
