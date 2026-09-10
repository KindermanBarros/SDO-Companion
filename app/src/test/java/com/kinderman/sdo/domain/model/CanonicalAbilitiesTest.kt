package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalAbilitiesTest {
    @Test fun canonicalCostCanReachZeroButNeverGoNegative() {
        val character = Character(arcane = ResourceValue(current = 3, maximum = 3))
        assertEquals(0, character.payCanonicalAbilityCost(AbilityCostType.ARCANE, 3).arcane.current)

        val failure = runCatching { character.payCanonicalAbilityCost(AbilityCostType.ARCANE, 4) }.exceptionOrNull()
        assertEquals("Recurso insuficiente para esta ação.", failure?.message)
        assertEquals(3, character.arcane.current)
    }

    @Test fun ashesOwnTheirInventoryStockLoadAndIdentity() {
        val ash = MysticAbility(type = "Cinza", name = "  Pó   Lunar ", ashPurity = AshPurity.REFINED, costValue = 2)
        val created = Character().withAddedAbility(ash).withAshDoses(ash.id, 3)
        val originalItem = created.inventory.single()
        assertEquals(2, originalItem.effectiveLoad())

        val renamed = created.withUpdatedAbility(created.mysticAbilities.single().copy(name = "Pó Lunar Azul"))
        assertEquals(originalItem.id, renamed.inventory.single().id)
        assertEquals(3, renamed.inventory.single().quantity)
        assertEquals("Pó Lunar Azul", renamed.inventory.single().name)
        assertEquals("Ainda existem essas cinzas no inventário", runCatching { renamed.withRemovedAbility(ash.id) }.exceptionOrNull()?.message)

        val consumed = renamed.payCanonicalAbilityCost(AbilityCostType.DOSE, 2, ash.id)
        assertEquals(1, consumed.inventory.single().quantity)
        assertEquals("Sem cinzas necessárias", runCatching { consumed.payCanonicalAbilityCost(AbilityCostType.DOSE, 2, ash.id) }.exceptionOrNull()?.message)
    }

    @Test fun uniquenessIsNormalizedInsideEachTypeOnly() {
        val magic = MysticAbility(type = "Magia", name = "Água Viva")
        val character = Character().withAddedAbility(magic)
        assertFalse(character.canUseAbilityKey(MysticAbility(type = "Magia", name = " agua   viva ")))
        assertTrue(character.canUseAbilityKey(MysticAbility(type = "Runa", name = "Água Viva")))
    }

    @Test fun passiveModifiersComposeWithoutChangingBaseAndClampCurrentMaximum() {
        val first = Power(
            name = "Vigor reduzido",
            executionType = AbilityExecution.PASSIVE,
            grantsPermanentBonus = true,
            active = true,
            modifiers = listOf(AbilityModifier(targetType = AbilityModifierTarget.RESOURCE_MAXIMUM, targetId = "LIFE", value = -3)),
        )
        val second = first.copy(id = "second", name = "Fôlego", modifiers = listOf(first.modifiers.single().copy(id = "m2", value = 2)))
        val character = Character(life = ResourceValue(current = 20), powers = listOf(first, second)).constrainedToResourceMaximums()

        assertEquals(10, character.lifeBase)
        assertEquals(9, character.lifeMaximum)
        assertEquals(9, character.life.current)
        assertEquals(2, character.lifeCalculation().modifiers.size)
    }

    @Test fun itemPassiveUsesEquipmentStateAsItsOnlyActivationControl() {
        val item = InventoryItem(id = "item", state = "M", name = "Anel")
        val power = Power(
            executionType = AbilityExecution.PASSIVE,
            canonicalSource = AbilitySource.ITEM,
            linkedItemId = item.id,
            grantsPermanentBonus = true,
            active = true,
            modifiers = listOf(AbilityModifier(targetType = AbilityModifierTarget.ATTRIBUTE, targetId = "FOR", value = 2)),
        )
        val stored = Character(inventory = listOf(item), powers = listOf(power))
        assertEquals(0, stored.attributeTotal("FOR"))
        assertEquals(2, stored.copy(inventory = listOf(item.copy(state = "E"))).attributeTotal("FOR"))
    }

    @Test fun knowledgeRollIsLimitedByPermanentAttributeBeforeModifiers() {
        val attributes = defaultAttributes().map { if (it.acronym == "INT") it.copy(value = 2, modifier = 5) else it }
        val knowledge = SpecialKnowledge(name = "História", attribute = "INT", value = 5, adjustment = 1)
        val character = Character(attributes = attributes, learnedKnowledges = listOf(knowledge))
        assertEquals(3, character.acquiredKnowledgeValue("História"))
        assertEquals(2, character.acquiredKnowledgeCalculation("História").base)
    }

    @Test fun duplicateMigrationDeletesOnlyAfterExplicitKeepChoice() {
        val first = MysticAbility(id = "one", type = "Magia", name = "Eco")
        val second = first.copy(id = "two")
        val character = Character(mysticAbilities = listOf(first, second))
        val duplicate = character.abilityDuplicates().single()
        val resolved = character.resolveAbilityDuplicate(duplicate, "two")
        assertEquals(listOf("two"), resolved.mysticAbilities.map { it.id })
    }
}
