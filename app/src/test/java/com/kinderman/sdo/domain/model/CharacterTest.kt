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

    @Test fun calculatedResourcesUseCanonicalFormulasAndAdjustments() {
        val attributes = defaultAttributes().map { attribute ->
            when (attribute.acronym) {
                "VIG" -> attribute.copy(
                    value = 3,
                    skills = attribute.skills.map { skill ->
                        when (skill.name) {
                            "Vitalidade" -> skill.copy(value = 4)
                            "Energia" -> skill.copy(value = 2)
                            else -> skill
                        }
                    },
                )
                "POD" -> attribute.copy(
                    value = 5,
                    skills = attribute.skills.map { skill ->
                        if (skill.name == "Arcano") skill.copy(value = 3) else skill
                    },
                )
                "INT" -> attribute.copy(
                    skills = attribute.skills.map { skill ->
                        if (skill.name == "Sanidade") skill.copy(value = 6) else skill
                    },
                )
                else -> attribute
            }
        }
        val character = Character(
            attributes = attributes,
            life = ResourceValue(adjustment = 2),
            sanity = ResourceValue(adjustment = -1),
            arcane = ResourceValue(adjustment = 4),
            energy = ResourceValue(adjustment = 1),
        )

        assertEquals(16, character.lifeMaximum)
        assertEquals(15, character.sanityMaximum)
        assertEquals(12, character.arcaneMaximum)
        assertEquals(6, character.energyMaximum)
    }

    @Test fun personalNoteTitlesContinueAfterTheHighestDefaultNumber() {
        val notes = listOf(
            PersonalNote(title = "Registro Pessoal 1"),
            PersonalNote(title = "Uma pista importante"),
            PersonalNote(title = "Registro Pessoal 4"),
        )

        assertEquals("Registro Pessoal 5", nextPersonalNoteTitle(notes))
    }
}
