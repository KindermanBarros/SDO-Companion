package com.kinderman.sdo.presentation.character

import com.kinderman.sdo.domain.model.InventoryItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EquipmentRegionTest {
    @Test fun pluralAndAccentedFootRegionMatchesBothFeet() {
        val boots = InventoryItem(region = "pés")

        assertTrue(boots.matchesEquipmentRegion("Pé esquerdo"))
        assertTrue(boots.matchesEquipmentRegion("Pé direito"))
        assertFalse(boots.matchesEquipmentRegion("Cabeça"))
    }

    @Test fun otherPluralRegionsMatchTheirIndividualMembers() {
        assertTrue(InventoryItem(region = "mãos").matchesEquipmentRegion("Mão esquerda"))
        assertTrue(InventoryItem(region = "braços").matchesEquipmentRegion("Braço direito"))
        assertTrue(InventoryItem(region = "pernas").matchesEquipmentRegion("Perna esquerda"))
    }
}
