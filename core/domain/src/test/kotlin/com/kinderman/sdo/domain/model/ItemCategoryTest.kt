package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemCategoryTest {
    @Test
    fun bowsAndShieldsUseCanonicalSectionsAndEquipmentOptions() {
        val bow = InventoryItem(category = "Arco ou besta")
        val shield = InventoryItem(category = "Escudo")

        assertEquals(InventorySectionGroup.WEAPONS, bow.resolvedCategory().sectionGroup)
        assertTrue(bow.isWieldableItem())
        assertEquals(InventorySectionGroup.ARMOR, shield.resolvedCategory().sectionGroup)
        assertTrue(shield.isWieldableItem())
        assertTrue(shield.isBodyEquippableItem())
    }

    @Test
    fun legacyBlankAndCustomCategoriesHaveDistinctFilters() {
        val legacy = "LEGACY_NARRATIVE".resolveItemCategory()
        val blank = "".resolveItemCategory()
        val custom = " Relicário cerimonial ".resolveItemCategory()

        assertEquals(CanonicalItemCategory.LEGACY_NARRATIVE, legacy.canonical)
        assertEquals(CanonicalItemCategory.UNCATEGORIZED, blank.canonical)
        assertEquals(CanonicalItemCategory.UNKNOWN, custom.canonical)
        assertEquals("Relicário cerimonial", custom.displayLabel)
        assertEquals(3, setOf(legacy.filterKey, blank.filterKey, custom.filterKey).size)
    }

    @Test
    fun catalogBatchStacksConsumablesAndKeepsEquipmentInstancesDistinct() {
        val potion = InventoryItem(id = "potion", catalogEntryId = "item.potion", category = "Consumível", quantity = 2)
        val bow = InventoryItem(id = "bow-one", catalogEntryId = "item.bow", category = "Arco ou besta")
        val original = Character(inventory = listOf(potion))

        val changed = original.addCatalogInventoryItems(
            listOf(potion.copy(id = "potion-new"), bow, bow.copy(id = "bow-two")),
        )

        assertEquals(2, original.inventory.single().quantity)
        assertEquals(4, changed.inventory.single { it.id == potion.id }.quantity)
        assertEquals(setOf("bow-one", "bow-two"), changed.inventory.filter { it.isWeaponItem() }.map { it.id }.toSet())
    }
}
