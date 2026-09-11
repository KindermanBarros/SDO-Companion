package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalAbilitiesTest {
    @Test fun ashBuilderCalculatesHeritageAndLoadFromPurityAndMergesDoses() {
        val ash = MysticAbility(type = "Cinza", name = "Brasa Rubra", ashPurity = AshPurity.REFINED, catalogEntryId = "ash.brasa.refinada")

        val created = Character(creationStatus = CharacterCreationStatus.DRAFT).withAddedAsh(ash, doses = 3, initialCreation = true)
        val item = created.inventory.single()
        assertEquals(3, item.quantity)
        assertEquals(2, item.effectiveLoad())
        assertEquals(6, item.heritageCost)
        assertEquals(ItemAcquisitionSource.HERITAGE, item.acquisitionSource)

        val merged = created.withAddedAsh(ash, doses = 2, initialCreation = true)
        assertEquals(5, merged.inventory.single().quantity)
        assertEquals(10, merged.inventory.single().heritageCost)
        assertEquals(1, merged.mysticAbilities.size)
    }

    @Test fun ashAddedAfterCreationNeverReceivesHeritageCost() {
        val ash = MysticAbility(type = "Cinza", name = "Bruma", ashPurity = AshPurity.PURE)
        val item = Character().withAddedAsh(ash, doses = 2, initialCreation = false).inventory.single()

        assertEquals(null, item.heritageCost)
        assertEquals(ItemAcquisitionSource.NARRATIVE, item.acquisitionSource)
    }

    @Test fun eachAbilityFamilyOwnsExactlyOneCostResource() {
        val activePower = Power(costType = AbilityCostType.ARCANE, costValue = 2).canonicalized()
        val passivePower = Power(executionType = AbilityExecution.PASSIVE, costType = AbilityCostType.LIFE, costValue = 3).canonicalized()
        val magic = MysticAbility(type = "Magia", costType = AbilityCostType.ENERGY, costValue = 2).canonicalized()
        val rune = MysticAbility(type = "Runa", costType = AbilityCostType.LIFE, costValue = 2).canonicalized()
        val ash = MysticAbility(type = "Cinza", costType = AbilityCostType.ARCANE, costValue = 1).canonicalized()

        assertEquals(AbilityCostType.ENERGY, activePower.costType)
        assertEquals(2, activePower.costValue)
        assertEquals(AbilityCostType.ENERGY, passivePower.costType)
        assertEquals(0, passivePower.costValue)

        val lifePower = Power(costType = AbilityCostType.LIFE, costValue = 2).canonicalized()
        val sanityPower = Power(costType = AbilityCostType.SANITY, costValue = 2).canonicalized()
        val rejectedDestinyPower = Power(costType = AbilityCostType.DESTINY, costValue = 1).canonicalized()
        val eligibleDestinyPower = Power(costType = AbilityCostType.DESTINY, costValue = 1, destinyCostEligible = true).canonicalized()
        assertEquals(AbilityCostType.LIFE, lifePower.costType)
        assertEquals(AbilityCostType.SANITY, sanityPower.costType)
        assertEquals(AbilityCostType.ENERGY, rejectedDestinyPower.costType)
        assertEquals(AbilityCostType.DESTINY, eligibleDestinyPower.costType)
        assertEquals(AbilityCostType.ARCANE, magic.costType)
        assertEquals(AbilityCostType.ARCANE, rune.costType)
        assertEquals(AbilityCostType.DOSE, ash.costType)
    }

    @Test fun activePowerCannotHaveZeroCost() {
        val power = Power(executionType = AbilityExecution.ACTION, costValue = 0).canonicalized()
        assertEquals(AbilityCostType.ENERGY, power.costType)
        assertEquals(1, power.costValue)
    }

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
        assertFalse(character.canUseAbilityKey(MysticAbility(type = "Magia", name = "Água—Viva!")))
        assertTrue(character.canUseAbilityKey(MysticAbility(type = "Runa", name = "Água Viva")))
    }

    @Test fun structuredLabelsUseIndependentExecutionAndDurationValues() {
        assertEquals("Tempo: 15 minutos", formattedAbilityExecution(AbilityExecution.TIME, 15, AbilityTimeUnit.MINUTES))
        assertEquals("2 dias", formattedAbilityDuration(AbilityDuration.TIME, 2, AbilityTimeUnit.DAYS))
        assertEquals("3 turnos", formattedAbilityDuration(AbilityDuration.TURNS, 3, AbilityTimeUnit.HOURS))
        assertEquals("Nenhum", formattedAbilityCost(AbilityCostType.NONE, 99))
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
