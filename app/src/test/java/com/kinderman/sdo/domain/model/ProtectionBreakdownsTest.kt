package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionBreakdownsTest {
    @Test fun equippedArmorExplainsPgPlAndAgilityLimit() {
        val armor = InventoryItem(
            id = "armor",
            name = "Armadura de Teste",
            region = "torso",
            category = "Armadura",
            pg = 2,
            pl = 3,
            agilityLimit = 4,
        )
        val character = Character(inventory = listOf(armor)).equipItems(1, setOf(armor.id))

        val pg = character.generalProtectionBreakdown()
        val pl = character.localProtectionBreakdown(character.bodyRegions[1])
        val la = character.agilityLimitBreakdown()

        assertEquals(12, pg.total)
        assertEquals(3, pl.total)
        assertEquals(4, la.total)
        assertEquals("Armadura de Teste", pg.modifiers.single().label)
        assertEquals("Armadura de Teste", pl.modifiers.single().label)
        assertTrue(la.contributions.any { it.label == "Armadura de Teste" })
    }
}
