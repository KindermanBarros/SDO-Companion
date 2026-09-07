package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.initialCreationCost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemCreationRulesTest {
    @Test fun canonicalCharacterExampleSpendsExactlyTwentyHeritagePoints() {
        val names = setOf(
            "Jaqueta Revestida de Aço Negro",
            "Perneiras de Ligas Comuns",
            "Arco Curto de Madeira ou Plástico",
            "Adaga de Ligas Comuns",
            "Mochila de Viajante",
            "Kit de Sutura",
            "Granada de Fumaça",
            "Morfina",
        )
        val selected = ItemCreationRules.catalog.filter { it.name in names }
        assertEquals(names.size, selected.size)
        assertEquals(20, selected.sumOf { it.creationCost.toInt() })
    }

    @Test fun builderUsesTypeMaterialModificationsAndSlots() {
        val item = ItemCreationRules.build(
            base = ItemCreationRules.weaponBases.first { it.id == "adaga" },
            material = ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" },
            modifications = listOf(ItemCreationRules.weaponModifications.first { it.id == "afiada" }),
            gemSlots = 1,
            technologySlots = 1,
        )
        assertEquals(7, item.creationCost)
        assertEquals(1, item.load)
        assertEquals(2, item.durability)
        assertTrue(item.effect.contains("Afiada"))
        assertTrue(item.effect.contains("CD 18; 8 Progressos; 2 dias"))
        assertEquals(7, item.toInventoryItem(initialCreation = true).initialCreationCost())
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
}
