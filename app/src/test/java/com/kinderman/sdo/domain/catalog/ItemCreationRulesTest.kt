package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.initialCreationCost
import com.kinderman.sdo.domain.model.ItemBonus
import com.kinderman.sdo.domain.model.ItemBonusType
import com.kinderman.sdo.domain.model.ItemQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemCreationRulesTest {
    @Test fun initialHeritageBudgetIsThirty() {
        assertEquals(30, ItemCreationRules.HERITAGE_BUDGET)
    }

    @Test fun builderUsesTypeMaterialModificationsAndSlots() {
        val item = ItemCreationRules.build(
            base = ItemCreationRules.weaponBases.first { it.id == "adaga" },
            material = ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" },
            modifications = listOf(ItemCreationRules.weaponModifications.first { it.id == "afiada" }),
            gemSlots = 1,
            technologySlots = 1,
        )
        assertEquals(8, item.creationCost)
        assertEquals(1, item.load)
        assertEquals(2, item.durability)
        assertTrue(item.effect.contains("Afiada"))
        assertTrue(item.effect.contains("CD 15; 5 Progressos; 4 horas"))
        assertEquals(8, item.toInventoryItem(initialCreation = true).initialCreationCost())
        assertEquals(0, item.toInventoryItem(initialCreation = false).initialCreationCost())
    }

    @Test fun variableMaterialsRemainHistorianControlled() {
        val item = ItemCreationRules.build(
            base = ItemCreationRules.weaponBases.first(),
            material = ItemCreationRules.weaponMaterials.first { it.creationCost == null },
            modifications = emptyList(),
            gemSlots = 0,
            technologySlots = 0,
        )
        assertNull(item.creationCost)
    }

    @Test fun builderUsesOnePredominantMaterialAndStructuredBonuses() {
        val common = ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" }
        val item = ItemCreationRules.build(
            base = ItemCreationRules.weaponBases.first { it.id == "lanca" },
            material = common,
            modifications = emptyList(),
            gemSlots = 0,
            technologySlots = 0,
            bonuses = listOf(ItemBonus(ItemBonusType.ATTRIBUTE, "FOR", 1)),
        )

        assertEquals(9, item.creationCost)
        assertEquals(2, item.durability)
        assertTrue(item.effect.contains("Material predominante: Ligas Comuns"))
        assertEquals("FOR", item.bonuses.single().target)
    }

    @Test fun qualityChangesCostPriceAndArmorProtection() {
        val item = ItemCreationRules.build(
            base = ItemCreationRules.armorBases.first { it.id == "elmo" },
            material = ItemCreationRules.armorMaterials.first { it.id == "ligas_comuns" },
            modifications = emptyList(), gemSlots = 0, technologySlots = 0,
            quality = ItemQuality.IMPROVED,
        )

        assertEquals(5, item.creationCost)
        assertEquals(68, item.price)
        assertEquals(3, item.pg)
        assertEquals(2, item.pl)
    }

    @Test fun installedGemRequiresAndConsumesItsOwnSlotAndCost() {
        val gem = ItemCreationRules.gemComponents.first()
        val item = ItemCreationRules.build(
            base = ItemCreationRules.weaponBases.first { it.id == "faca" },
            material = ItemCreationRules.weaponMaterials.first { it.id == "madeira" },
            modifications = emptyList(), gemSlots = 1, technologySlots = 0,
            components = listOf(gem),
        )

        assertEquals(4, item.creationCost)
        assertTrue(item.effect.contains(gem.name))
        assertTrue(item.effect.contains("Espaços de Gema: 1"))
    }
}
