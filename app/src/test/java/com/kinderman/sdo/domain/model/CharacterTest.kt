package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterTest {
    @Test fun canonicalDefaultsArePresent() {
        val character = Character()
        assertEquals(listOf("FOR", "VIG", "AGI", "POD", "INT", "CAR"), character.attributes.map { it.acronym })
        assertEquals(5, character.protections.size)
        assertEquals(10, character.bodyRegions.size)
        assertEquals(5, character.organs.size)
        assertEquals(3, character.pathKeywords.size)
        assertEquals(3, character.pathPillars.size)
    }

    @Test fun loadIgnoresStoredItemsAndUsesStrengthAndContainer() {
        val attributes = defaultAttributes().map { if (it.acronym == "FOR") it.copy(value = 3) else it }
        val character = Character(
            attributes = attributes,
            containerCapacity = 10,
            inventory = listOf(
                InventoryItem(state = "E", load = 3),
                InventoryItem(state = "M", load = 2),
                InventoryItem(state = "G", load = 8),
            ),
        )
        assertEquals(5, character.currentLoad)
        assertEquals(15, character.maximumLoad)
    }
}
