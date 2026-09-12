package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.InventoryState
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.EquipmentEffectEngine
import com.kinderman.sdo.domain.model.addInventoryItem
import com.kinderman.sdo.domain.model.withItemInventoryState
import com.kinderman.sdo.domain.model.activeItemEffects
import com.kinderman.sdo.domain.model.damageInventoryItem
import com.kinderman.sdo.domain.model.ItemCondition
import com.kinderman.sdo.domain.model.hasScrapAttackDisadvantage
import com.kinderman.sdo.domain.model.scrapDamageDieCategoryPenalty
import com.kinderman.sdo.domain.model.handsRequired
import com.kinderman.sdo.domain.model.inventoryState
import com.kinderman.sdo.domain.model.recycleBrokenItem
import com.kinderman.sdo.domain.model.wieldedHandsUsed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemNormalizationTest {
    @Test fun heavyWeaponConsumesTwoHandsAndBlocksAnotherWieldedItem() {
        val heavy = InventoryItem(id = "heavy", baseId = "montante", materialId = "ligas_comuns", state = "G", durabilityCurrent = 2, durabilityMax = 2)
        val dagger = InventoryItem(id = "dagger", baseId = "adaga", state = "W", durabilityCurrent = 1, durabilityMax = 1)
        val character = Character(inventory = listOf(heavy, dagger))

        assertEquals(2, heavy.handsRequired())
        assertEquals(character, character.withItemInventoryState("heavy", InventoryState.WIELDED))
        val wieldedHeavy = character.withItemInventoryState("dagger", InventoryState.STORED)
            .withItemInventoryState("heavy", InventoryState.WIELDED)
        assertEquals(2, wieldedHeavy.wieldedHandsUsed)
        assertEquals(1, heavy.copy(materialId = "ossos_comuns").handsRequired())
    }

    @Test fun normalizationSanitizesDuplicateContainersAndInvalidStates() {
        val small = InventoryItem(id = "small", state = "E", category = "Recipiente de Carga", catalogEntryId = "legacy.small", backpackCapacity = 5, durabilityCurrent = 1, durabilityMax = 1, dataVersion = 4)
        val large = InventoryItem(id = "large", state = "E", category = "Recipiente de Carga", catalogEntryId = "legacy.large", backpackCapacity = 10, durabilityCurrent = 1, durabilityMax = 1, dataVersion = 4)
        val invalidQuick = InventoryItem(id = "quick", state = "R", load = 2, durabilityCurrent = 1, durabilityMax = 1, dataVersion = 4)

        val normalized = Character(inventory = listOf(small, large, invalidQuick)).withNormalizedInventory()

        assertEquals(InventoryState.STORED, normalized.inventory.first { it.id == "small" }.inventoryState)
        assertEquals(InventoryState.EQUIPPED, normalized.inventory.first { it.id == "large" }.inventoryState)
        assertEquals(InventoryState.STORED, normalized.inventory.first { it.id == "quick" }.inventoryState)
        assertEquals(10, normalized.backpackCapacity)
    }

    @Test fun scrapWeaponAppliesEveryRulesPenalty() {
        val effects = listOf(
            ItemEffect("attack", ItemEffectType.ATTACK, 3, condition = ItemEffectCondition.WIELDED),
            ItemEffect("damage", ItemEffectType.PHYSICAL_DAMAGE, 2, condition = ItemEffectCondition.WIELDED),
            ItemEffect("mod", ItemEffectType.RULE, description = "Modificação", condition = ItemEffectCondition.WIELDED),
            ItemEffect("gem", ItemEffectType.GEM_POWER, condition = ItemEffectCondition.WIELDED),
            ItemEffect("pg", ItemEffectType.PG, 5, condition = ItemEffectCondition.EQUIPPED),
            ItemEffect("pl", ItemEffectType.PL, 3, condition = ItemEffectCondition.EQUIPPED),
        )
        val scrap = InventoryItem(
            id = "scrap", name = "Espada", category = "Arma", state = "W",
            durabilityCurrent = 0, durabilityMax = 4, itemCondition = ItemCondition.SCRAP,
            modificationIds = listOf("mod"), gemIds = listOf("gem"), mechanicalEffects = effects,
        )
        val character = Character(inventory = listOf(scrap))
        val active = character.activeItemEffects()

        assertTrue(character.hasScrapAttackDisadvantage)
        assertEquals(1, character.scrapDamageDieCategoryPenalty)
        assertEquals(0, character.equipmentAttackBonus)
        assertEquals(2, character.equipmentPhysicalDamageBonus)
        assertEquals(2, active.single { it.effect.id == "pg" }.effect.value)
        assertEquals(1, active.single { it.effect.id == "pl" }.effect.value)
        assertFalse(active.any { it.effect.id in setOf("attack", "mod", "gem") })
    }

    @Test fun durabilityLossTransitionsFromNormalToScrapThenBroken() {
        val effect = ItemEffect("power", ItemEffectType.GEM_POWER, condition = ItemEffectCondition.WIELDED)
        val item = InventoryItem(
            id = "weapon", name = "Espada", category = "Arma", state = "W",
            durabilityCurrent = 1, durabilityMax = 3, mechanicalEffects = listOf(effect),
        )

        val scrap = Character(inventory = listOf(item)).damageInventoryItem("weapon")
        assertEquals(ItemCondition.SCRAP, scrap.inventory.single().itemCondition)
        assertTrue(scrap.activeItemEffects().isEmpty())

        val broken = scrap.damageInventoryItem("weapon")
        assertEquals(ItemCondition.BROKEN, broken.inventory.single().itemCondition)
        assertEquals(InventoryState.STORED, broken.inventory.single().inventoryState)
        assertEquals(broken, broken.withItemInventoryState("weapon", InventoryState.EQUIPPED))
    }

    @Test fun brokenItemCannotRemainEquippedThroughBodyReferences() {
        val broken = InventoryItem(
            id = "armor", name = "Peitoral", category = "Armadura", state = "E",
            region = "torso", pg = 4, pl = 2, durabilityCurrent = 0, durabilityMax = 3,
            itemCondition = ItemCondition.BROKEN,
        )
        val character = Character(
            inventory = listOf(broken),
            bodyRegions = com.kinderman.sdo.domain.model.defaultBodyRegions().mapIndexed { index, region ->
                if (index == 1) region.copy(equippedItemIds = listOf(broken.id)) else region
            },
        )

        val normalized = character.withNormalizedInventory()

        assertEquals(InventoryState.STORED, normalized.inventory.single().inventoryState)
        assertTrue(normalized.bodyRegions[1].equippedItemIds.isEmpty())
        assertEquals(10, normalized.protectionBase("Geral"))
        assertEquals(0, normalized.localProtection(normalized.bodyRegions[1]))
    }

    @Test fun brokenItemRecyclesIntoScrapWorthHalfItsEstribosValue() {
        val expensive = InventoryItem(
            id = "broken", name = "Montante", category = "Arma", purchasePrice = 90,
            durabilityCurrent = 0, durabilityMax = 6, itemCondition = ItemCondition.BROKEN,
        )

        val recycled = Character(inventory = listOf(expensive)).recycleBrokenItem(expensive.id)

        assertFalse(recycled.inventory.any { it.id == expensive.id })
        val scrap = recycled.inventory.single { it.name.startsWith("Sucata recuperada") }
        assertEquals(45, scrap.purchasePrice)
        assertEquals(1, scrap.quantity)
    }

    @Test fun inventoryStatesEnforceQuickAccessAndBackpackCapacity() {
        val first = InventoryItem(id = "first", load = 1, state = "R", durabilityCurrent = 1, durabilityMax = 1)
        val second = InventoryItem(id = "second", load = 1, state = "R", durabilityCurrent = 1, durabilityMax = 1)
        val third = InventoryItem(id = "third", load = 1, state = "G", durabilityCurrent = 1, durabilityMax = 1)
        val heavy = InventoryItem(id = "heavy", load = 2, state = "G", durabilityCurrent = 1, durabilityMax = 1)
        val withoutBackpack = Character(inventory = listOf(first, second, third, heavy))

        assertEquals(withoutBackpack, withoutBackpack.withItemInventoryState("third", InventoryState.QUICK_ACCESS))
        assertEquals(withoutBackpack, withoutBackpack.withItemInventoryState("heavy", InventoryState.QUICK_ACCESS))
        assertEquals(withoutBackpack, withoutBackpack.withItemInventoryState("third", InventoryState.BACKPACK))

        val container = InventoryItem(
            id = "bag", state = "E", category = "Recipiente de Carga", backpackCapacity = 5,
            catalogEntryId = "item.mochila_pequena", durabilityCurrent = 1, durabilityMax = 1,
        )
        val withBackpack = withoutBackpack.copy(inventory = withoutBackpack.inventory + container)
        assertEquals(InventoryState.BACKPACK, withBackpack.withItemInventoryState("third", InventoryState.BACKPACK)
            .inventory.first { it.id == "third" }.inventoryState)
    }

    @Test fun randomKnowledgeTargetIsResolvedOnceAndSurvivesStateChanges() {
        val effect = ItemEffect("random", ItemEffectType.KNOWLEDGE, 1, "*", ItemEffectCondition.WIELDED)
        val item = InventoryItem(id = "item-1", name = "Gema", state = "W", mechanicalEffects = listOf(effect))
        val knowledge = SpecialKnowledge(id = "knowledge-1", name = "Música", attribute = "CAR")

        val added = Character(learnedKnowledges = listOf(knowledge)).addInventoryItem(item)
        val target = added.inventory.single().mechanicalEffects.single().resolvedTargetId
        val cycled = added.withItemInventoryState("item-1", InventoryState.BACKPACK)
            .withItemInventoryState("item-1", InventoryState.WIELDED)

        assertTrue(target.isNotBlank())
        assertEquals(target, cycled.inventory.single().mechanicalEffects.single().resolvedTargetId)
    }

    @Test fun gemPowerExistsOnlyWhileItsConditionIsActive() {
        val effect = ItemEffect("combat_pulse", ItemEffectType.GEM_POWER, target = "combat_pulse", condition = ItemEffectCondition.WIELDED, description = "Pulso de combate.")
        val item = InventoryItem(id = "weapon-1", name = "Lâmina", state = "M", mechanicalEffects = listOf(effect))
        val character = Character().addInventoryItem(item)

        assertTrue(character.powers.isEmpty())
        val wielded = character.withItemInventoryState("weapon-1", InventoryState.WIELDED)
        assertEquals("weapon-1", wielded.powers.single().linkedItemId)
        assertTrue(wielded.withItemInventoryState("weapon-1", InventoryState.STORED).powers.isEmpty())
    }

    @Test fun engineAuditsTypedCombatAndDurabilityEffectsByOrigin() {
        val effects = listOf(
            ItemEffect("attack", ItemEffectType.ATTACK, 2, condition = ItemEffectCondition.WIELDED),
            ItemEffect("physical", ItemEffectType.PHYSICAL_DAMAGE, 3, condition = ItemEffectCondition.WIELDED),
            ItemEffect("magic", ItemEffectType.MAGIC_DAMAGE, 1, condition = ItemEffectCondition.WIELDED),
            ItemEffect("durability", ItemEffectType.DURABILITY, 1, condition = ItemEffectCondition.WIELDED),
            ItemEffect("agility", ItemEffectType.AGILITY_LIMIT, 4, condition = ItemEffectCondition.WIELDED),
            ItemEffect("rule", ItemEffectType.RULE, description = "Regra ativa", condition = ItemEffectCondition.WIELDED),
        )
        val character = Character(inventory = listOf(InventoryItem(id = "weapon", name = "Arma", state = "W", mechanicalEffects = effects)))
        val resolution = EquipmentEffectEngine.resolve(character)

        assertEquals(2, character.equipmentAttackBonus)
        assertEquals(3, character.equipmentPhysicalDamageBonus)
        assertEquals(1, character.equipmentMagicDamageBonus)
        assertEquals(1, character.equipmentDurabilityBonus("weapon"))
        assertEquals(4, character.equippedAgilityLimit)
        assertEquals("rule", character.activeEquipmentRules.single().effectId)
        assertEquals(setOf("weapon"), resolution.entries.map { it.itemId }.toSet())
    }
    @Test fun legacyCatalogItemReceivesCanonicalTypedFieldsByExactIdentity() {
        val legacy = InventoryItem(
            id = "legacy-armor",
            state = "EQUIPPED",
            name = "Elmo de Ligas Comuns",
            effect = "descrição antiga sem campos mecânicos",
            dataVersion = 0,
        )

        val migrated = Character(inventory = listOf(legacy)).withNormalizedInventory().inventory.single()

        assertEquals("legacy-armor", migrated.id)
        assertEquals("E", migrated.state)
        assertEquals(2, migrated.pg)
        assertEquals(2, migrated.pl)
        assertTrue(migrated.catalogEntryId.startsWith("item.regular_"))
        assertTrue(migrated.canonical)
        assertEquals(CURRENT_ITEM_DATA_VERSION, migrated.dataVersion)
        assertEquals(migrated, Character(inventory = listOf(migrated)).withNormalizedInventory().inventory.single())
    }

    @Test fun unrecognizedLegacyItemWithoutValidDurabilityIsPreservedForScopedCatalogMigration() {
        val legacy = InventoryItem(
            id = "legacy-weapon",
            state = "WIELDED",
            name = "Arma personalizada",
            category = "Arma",
            dataVersion = 0,
        )

        val character = Character(inventory = listOf(legacy)).withNormalizedInventory()
        assertEquals("Arma personalizada", character.inventory.single().name)
        assertEquals("LEGACY_NARRATIVE", character.inventory.single().category)
        assertEquals(1, character.inventory.single().durabilityCurrent)
        assertEquals(1, character.inventory.single().durabilityMax)
        assertEquals(0, character.attributeTotal("FOR"))
    }

    @Test fun newlyAddedItemWithoutDurabilityStartsAtOneWithoutRepairingRealWear() {
        val newItem = Character().addInventoryItem(InventoryItem(id = "new", name = "Item sem durabilidade"))
            .inventory.single()
        assertEquals(1, newItem.durabilityCurrent)
        assertEquals(1, newItem.durabilityMax)

        val worn = Character().addInventoryItem(InventoryItem(id = "worn", durabilityCurrent = 0, durabilityMax = 3))
            .inventory.single()
        assertEquals(0, worn.durabilityCurrent)
        assertEquals(3, worn.durabilityMax)
    }

    @Test fun effectConditionFollowsEquipmentState() {
        val equippedEffect = ItemEffect("equipped", ItemEffectType.RULE, condition = ItemEffectCondition.EQUIPPED)
        val wieldedEffect = ItemEffect("wielded", ItemEffectType.GEM_POWER, condition = ItemEffectCondition.WIELDED)
        val equipped = InventoryItem(state = "E", mechanicalEffects = listOf(equippedEffect, wieldedEffect))
        val wielded = equipped.copy(id = "wielded-item", state = "W")

        val equippedActive = Character(inventory = listOf(equipped)).activeItemEffects().map { it.effect.id }
        val wieldedActive = Character(inventory = listOf(wielded)).activeItemEffects().map { it.effect.id }

        assertEquals(listOf("equipped"), equippedActive)
        assertTrue("equipped" in wieldedActive)
        assertTrue("wielded" in wieldedActive)
        assertFalse("wielded" in equippedActive)
    }

    @Test fun versionThreeCustomItemHydratesEffectsFromComponentIds() {
        val legacy = InventoryItem(
            name = "Lâmina personalizada",
            baseId = "faca",
            materialId = "madeira",
            modificationIds = listOf("afiada"),
            gemIds = listOf("gema_atributo_for"),
            dataVersion = 3,
        )

        val migrated = Character(inventory = listOf(legacy)).withNormalizedInventory().inventory.single()

        assertEquals(setOf("afiada", "gema_atributo_for"), migrated.mechanicalEffects.map { it.id }.toSet())
        assertFalse(migrated.canonical)
        assertEquals(CURRENT_ITEM_DATA_VERSION, migrated.dataVersion)
    }
}
