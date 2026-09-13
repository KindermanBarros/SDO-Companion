package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.initialCreationCost
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
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
        assertTrue(item.effect.contains(ItemCreationRules.weaponModifications.first { it.id == "afiada" }.effect))
        assertEquals(ItemEffectType.RULE, item.mechanicalEffects.single().type)
        assertEquals("attack", item.mechanicalEffects.single().target)
        assertTrue(ItemCreationRules.complexity(item.creationCost).contains("CD 15; 5 Progressos; 4 horas"))
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

    @Test fun builderUsesOnePredominantMaterial() {
        val common = ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" }
        val item = ItemCreationRules.build(
            base = ItemCreationRules.weaponBases.first { it.id == "lanca" },
            material = common,
            modifications = emptyList(),
            gemSlots = 0,
            technologySlots = 0,
        )

        assertEquals(6, item.creationCost)
        assertEquals(2, item.durability)
        assertEquals("ligas_comuns", item.materialId)
        assertTrue(!item.effect.contains("Material predominante"))
        assertTrue(item.mechanicalEffects.isEmpty())
    }

    @Test fun canonicalMaterialsExposeTheirRuleEffects() {
        val materials = ItemCreationRules.weaponMaterials + ItemCreationRules.armorMaterials

        assertTrue(materials.all { it.effect.isNotBlank() })
        assertEquals("Padrão metálico.", ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" }.effect)
    }

    @Test fun everyNonCommonQualityExposesItsRuleEffect() {
        ItemQuality.entries.filterNot { it == ItemQuality.COMMON }.forEach { quality ->
            assertTrue(ItemCreationRules.qualityEffect(quality, armor = false).orEmpty().isNotBlank())
            assertTrue(ItemCreationRules.qualityEffect(quality, armor = true).orEmpty().isNotBlank())
        }
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

    @Test fun accessoryDoesNotInheritPgFromMaterialOrQuality() {
        val item = ItemCreationRules.build(
            base = ItemCreationRules.armorBases.first { it.group == "Acessório" && it.pg == 0 },
            material = ItemCreationRules.armorMaterials.first { it.id == "ligas_comuns" },
            modifications = emptyList(), gemSlots = 0, technologySlots = 0,
            quality = ItemQuality.MASTERPIECE,
        )

        assertEquals(0, item.pg)
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
        assertTrue(item.effect.contains(gem.effect))
        assertTrue(!item.effect.contains("Espaços de Gema"))
        assertEquals(1, item.gemSlots)
        assertEquals(gem.id, item.gemIds.single())
        assertEquals(ItemEffectType.KNOWLEDGE, item.mechanicalEffects.single().type)
        assertEquals("*", item.mechanicalEffects.single().target)
        assertEquals("faca", item.baseId)
        assertEquals("madeira", item.materialId)
    }

    @Test fun runtimeCatalogIsLoadedDirectlyFromCanonicalJson() {
        val itemDefinitionCount = CanonicalItemCatalog.weaponMaterials.size +
            CanonicalItemCatalog.armorMaterials.size + CanonicalItemCatalog.weaponBases.size +
            CanonicalItemCatalog.armorBases.size + CanonicalItemCatalog.catalogItems.size

        assertEquals(161, itemDefinitionCount)
        assertEquals(26, CanonicalItemCatalog.modifications.size)
        assertEquals(57, CanonicalItemCatalog.gems.size)
        assertEquals(50, CanonicalItemCatalog.technologies.size)
    }

    @Test fun catalogInventoryUsesTypedProtectionInsteadOfDescriptionParsing() {
        val armor = ItemCreationRules.catalog.first { it.name.startsWith("Elmo de") }
        val inventory = armor.toInventoryItem()

        assertEquals(2, inventory.pg)
        assertEquals(2, inventory.pl)
        assertNull(inventory.agilityLimit)
        assertEquals(armor.id, inventory.catalogEntryId)
    }

    @Test fun catalogItemWithoutDeclaredDurabilityDefaultsToOne() {
        val inventory = CatalogEntry(
            id = "item.sem_durabilidade",
            kind = CatalogKind.ITEM,
            name = "Item",
            group = "Item",
            summary = "",
        ).toInventoryItem()

        assertEquals(1, inventory.durabilityCurrent)
        assertEquals(1, inventory.durabilityMax)
    }
}
