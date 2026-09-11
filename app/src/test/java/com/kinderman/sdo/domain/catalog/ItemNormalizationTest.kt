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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemNormalizationTest {
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

    @Test fun unrecognizedLegacyItemWithoutValidDurabilityIsRemoved() {
        val legacy = InventoryItem(
            id = "legacy-weapon",
            state = "WIELDED",
            name = "Arma personalizada",
            category = "Arma",
        )

        val character = Character(inventory = listOf(legacy)).withNormalizedInventory()
        assertTrue(character.inventory.isEmpty())
        assertEquals(0, character.attributeTotal("FOR"))
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
        )

        val migrated = Character(inventory = listOf(legacy)).withNormalizedInventory().inventory.single()

        assertEquals(setOf("afiada", "gema_atributo_for"), migrated.mechanicalEffects.map { it.id }.toSet())
        assertFalse(migrated.canonical)
        assertEquals(CURRENT_ITEM_DATA_VERSION, migrated.dataVersion)
    }
}
