package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemCategoryTest {
    @Test
    fun catalogBatchRefillsEmptyStackWithoutCreatingAnExtraUnit() {
        val empty = InventoryItem(
            id = "empty", catalogEntryId = "item.potion", category = "Consumível",
            quantity = 0, load = 0,
        )
        val original = Character(inventory = listOf(empty))

        val changed = original.addCatalogInventoryItems(
            listOf(empty.copy(id = "refill", quantity = 3, load = 2)),
        )

        assertEquals(0, original.inventory.single().quantity)
        assertEquals(empty.id, changed.inventory.single().id)
        assertEquals(3, changed.inventory.single().quantity)
        assertEquals(2, changed.inventory.single().effectiveLoad())
    }

    @Test
    fun catalogBatchPreservesTotalConsumableLoadAcrossMultipleMerges() {
        for (category in listOf("Consumível", "Consumível arcano")) {
            val existing = InventoryItem(
                id = "existing", catalogEntryId = "item.potion", category = category,
                quantity = 2, load = 3,
                acquisitionSource = ItemAcquisitionSource.HERITAGE, heritageCost = 4,
            )
            val incoming = existing.copy(id = "incoming", quantity = 3, load = 2, heritageCost = 6)
            val original = Character(inventory = listOf(existing))

            val changed = original.addCatalogInventoryItems(
                listOf(incoming, incoming.copy(id = "another", quantity = 1, load = 1, heritageCost = 2)),
            )

            val merged = changed.inventory.single()
            assertEquals(existing, original.inventory.single())
            assertEquals(existing.id, merged.id)
            assertEquals(6, merged.quantity)
            assertEquals(6, merged.effectiveLoad())
            assertEquals(12, merged.heritageCost)
        }
    }

    @Test
    fun catalogBatchRetainsQuantityBasedLoadForAmmunitionAndLinkedAshes() {
        val ammunition = InventoryItem(
            id = "ammo", catalogEntryId = "item.ammo", category = "Munição",
            quantity = 6, load = 1,
        )
        val ash = InventoryItem(
            id = "ash", catalogEntryId = "item.ash", category = "Cinza",
            linkedAshId = "ability", ashPurity = AshPurity.REFINED, quantity = 1, load = 1,
        )
        for (existing in listOf(ammunition, ash)) {
            val original = Character(inventory = listOf(existing))
            val changed = original.addCatalogInventoryItems(listOf(existing.copy(id = "incoming")))
            val merged = changed.inventory.single()

            assertEquals(existing.quantity * 2, merged.quantity)
            assertEquals(existing.load, merged.load)
            assertEquals(existing.copy(quantity = existing.quantity * 2).effectiveLoad(), merged.effectiveLoad())
        }
    }

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
