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
            mechanicalEffects = listOf(
                ItemEffect("armor:pg", ItemEffectType.PG, 2, condition = ItemEffectCondition.EQUIPPED),
                ItemEffect("armor:pl", ItemEffectType.PL, 3, target = "torso", condition = ItemEffectCondition.EQUIPPED),
                ItemEffect("armor:la", ItemEffectType.AGILITY_LIMIT, 4, condition = ItemEffectCondition.EQUIPPED),
            ),
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
            mechanicalEffects = listOf(
                ItemEffect("shield:pg", ItemEffectType.PG, 2, condition = ItemEffectCondition.WIELDED),
            ),
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

    @Test fun lowestEquippedAgilityLimitCapsAgilityAndExplainsThePenalty() {
        fun armor(id: String, name: String, limit: Int) = InventoryItem(
            id = id,
            name = name,
            category = "Armadura",
            agilityLimit = limit,
            mechanicalEffects = listOf(
                ItemEffect("$id:la", ItemEffectType.AGILITY_LIMIT, limit, condition = ItemEffectCondition.EQUIPPED),
            ),
        )
        val loose = armor("loose", "Armadura flexível", 4).withInventoryState(InventoryState.EQUIPPED)
        val strict = armor("strict", "Armadura pesada", 3).withInventoryState(InventoryState.EQUIPPED)
        val attributes = defaultAttributes().map { if (it.acronym == "AGI") it.copy(value = 5) else it }
        val character = Character(attributes = attributes, inventory = listOf(loose, strict))

        val agility = character.attributeCalculation("AGI")

        assertEquals(3, agility.total)
        assertEquals(-2, agility.modifiers.single { it.label.startsWith("LA —") }.value)
        assertTrue(agility.modifiers.any { it.label.contains("Armadura pesada") })
        assertEquals(13, character.protectionBase("Esquiva"))
    }

    @Test fun unequippedItemDoesNotLimitAgility() {
        val armor = InventoryItem(
            id = "stored",
            name = "Armadura guardada",
            agilityLimit = 2,
            mechanicalEffects = listOf(
                ItemEffect("stored:la", ItemEffectType.AGILITY_LIMIT, 2, condition = ItemEffectCondition.EQUIPPED),
            ),
        )
        val attributes = defaultAttributes().map { if (it.acronym == "AGI") it.copy(value = 5) else it }

        assertEquals(5, Character(attributes = attributes, inventory = listOf(armor)).attributeTotal("AGI"))
    }
}
