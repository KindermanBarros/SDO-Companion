package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.EquipmentEffectEngine
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.InventoryState
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancementRulesTest {
    @Test fun catalogsProvideThirtyFunctionalEntriesOfEachKind() {
        assertEquals(30, BundledItemCatalog.gems.size)
        assertEquals(30, BundledItemCatalog.technologies.size)
        assertTrue(BundledItemCatalog.enhancements.all { it.effect.description.isNotBlank() })
    }

    @Test fun gemAndTechnologyShareTheSameCapacity() {
        val item = InventoryItem(enhancementSlots = 2)
        val withGem = ItemCreationRules.installEnhancement(item, "gema_forca")
        val full = ItemCreationRules.installEnhancement(withGem, "tech_termico_conversor")

        assertEquals(2, full.installedEnhancements.size)
        assertTrue(runCatching { ItemCreationRules.installEnhancement(full, "gema_vigor") }.isFailure)
    }

    @Test fun duplicateTechnologyIsRejected() {
        val item = InventoryItem(enhancementSlots = 2)
        val installed = ItemCreationRules.installEnhancement(item, "tech_termico_conversor")
        assertTrue(runCatching { ItemCreationRules.installEnhancement(installed, "tech_termico_conversor") }.isFailure)
    }

    @Test fun highestGemAndHighestTechnologyBonusesStackByFamily() {
        val effects = listOf(
            ItemEffect("gema_forca", ItemEffectType.ATTRIBUTE, 1, "FOR", ItemEffectCondition.WIELDED),
            ItemEffect("gema_forca_rara", ItemEffectType.ATTRIBUTE, 2, "FOR", ItemEffectCondition.WIELDED),
            ItemEffect("tech_forca", ItemEffectType.ATTRIBUTE, 3, "FOR", ItemEffectCondition.WIELDED),
        )
        val item = InventoryItem(state = InventoryState.WIELDED.storageCode, mechanicalEffects = effects)
        assertEquals(5, EquipmentEffectEngine.resolve(Character(inventory = listOf(item))).total(ItemEffectType.ATTRIBUTE, "FOR"))
    }

    @Test fun legacyEnhancementFieldsAreDeletedInsteadOfMigrated() {
        val legacy = InventoryItem(
            gemIds = listOf("gema_atributo_for"), technologyIds = listOf("tech_antiga"),
            gemSlots = 2, technologySlots = 3, dataVersion = 7,
            mechanicalEffects = listOf(ItemEffect("gema_atributo_for", ItemEffectType.ATTRIBUTE, 1, "FOR")),
        )
        val cleaned = legacy.normalized()!!
        assertTrue(cleaned.gemIds.isEmpty() && cleaned.technologyIds.isEmpty())
        assertEquals(0, cleaned.gemSlots)
        assertEquals(0, cleaned.technologySlots)
        assertTrue(cleaned.installedEnhancements.isEmpty())
        assertTrue(cleaned.mechanicalEffects.none { it.id == "gema_atributo_for" })
    }
}
