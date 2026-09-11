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

    @Test fun shieldPgOnlyAppliesWhenWielded() {
        val shield = InventoryItem(
            id = "shield",
            name = "Escudo Leve",
            category = "Escudo",
            pg = 2,
            inventoryState = InventoryState.EQUIPPED,
        )
        val charEquipped = Character(inventory = listOf(shield)).equipItems(2, setOf(shield.id))
        assertEquals(10, charEquipped.equippedGeneralProtection + 10)
        assertEquals(10, charEquipped.generalProtectionBreakdown().total)

        val charWielded = charEquipped.withItemInventoryState(shield.id, InventoryState.WIELDED)
        assertEquals(12, charWielded.protectionTotal("Geral"))
        assertEquals(12, charWielded.generalProtectionBreakdown().total)
    }

    @Test fun typedShieldPgOnlyAppliesWhenWielded() {
        val typedShield = InventoryItem(
            id = "typed_shield",
            name = "Escudo Redondo",
            category = "Escudo",
            pg = 3,
            inventoryState = InventoryState.EQUIPPED,
            mechanicalEffects = listOf(
                ItemEffect("shield:pg", ItemEffectType.PG, 3, condition = ItemEffectCondition.WIELDED),
            ),
        )
        val charEquipped = Character(inventory = listOf(typedShield))
        assertEquals(10, charEquipped.protectionTotal("Geral"))
        assertEquals(10, charEquipped.generalProtectionBreakdown().total)

        val charWielded = charEquipped.withItemInventoryState(typedShield.id, InventoryState.WIELDED)
        assertEquals(13, charWielded.protectionTotal("Geral"))
        assertEquals(13, charWielded.generalProtectionBreakdown().total)
    }
}
