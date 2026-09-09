package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterTest {
    @Test fun canonicalDefaultsArePresent() {
        val character = Character()
        assertEquals(listOf("FOR", "VIG", "AGI", "POD", "INT", "CAR"), character.attributes.map { it.acronym })
        assertEquals(5, character.protections.size)
        assertEquals(10, character.bodyRegions.size)
        assertEquals(0, character.organs.size)
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

    @Test fun aSingleTextCostDeductsEveryFixedResourceWithoutUsageCounters() {
        val character = Character(
            life = ResourceValue(current = 10),
            arcane = ResourceValue(current = 8, adjustment = 10),
        )

        val paid = character.payFixedAbilityCosts("2 PM + 1 PV")

        assertEquals(6, paid.arcane.current)
        assertEquals(9, paid.life.current)
    }

    @Test fun variableDiceCostsRemainUnderTableControl() {
        val character = Character(life = ResourceValue(current = 10))

        assertEquals(10, character.payFixedAbilityCosts("1d6 HP").life.current)
    }

    @Test fun protectionsUseCanonicalFormulasAndManualAdjustments() {
        val attributes = defaultAttributes().map { attribute ->
            when (attribute.acronym) {
                "AGI" -> attribute.copy(
                    value = 2,
                    skills = attribute.skills.map { if (it.name == "Reflexos") it.copy(value = 3) else it },
                )
                "CAR" -> attribute.copy(
                    value = 4,
                    skills = attribute.skills.map { if (it.name == "Lábia") it.copy(value = 2) else it },
                )
                "INT" -> attribute.copy(
                    value = 5,
                    skills = attribute.skills.map { if (it.name == "Sanidade") it.copy(value = 4) else it },
                )
                "POD" -> attribute.copy(
                    value = 3,
                    skills = attribute.skills.map { if (it.name == "Arcano") it.copy(value = 1) else it },
                )
                else -> attribute
            }
        }
        val character = Character(
            attributes = attributes,
            protectionAdjustments = mapOf(
                "Geral" to 6,
                "Esquiva" to 1,
                "Postura" to -1,
                "Mental" to 2,
                "Arcana" to 0,
            ),
        )

        assertEquals(16, character.protectionTotal("Geral"))
        assertEquals(22, character.protectionTotal("Esquiva"))
        assertEquals(15, character.protectionTotal("Postura"))
        assertEquals(21, character.protectionTotal("Mental"))
        assertEquals(14, character.protectionTotal("Arcana"))
    }

    @Test fun equippedArmorAddsPgToCharacterAndPlToSelectedBodyRegion() {
        val armor = InventoryItem(id = "armor-1", name = "Peitoral", pg = 3, pl = 2, region = "torso", category = "Armadura")
        val initial = Character(inventory = listOf(armor))
        val equipped = initial.equipItems(regionIndex = 1, itemIds = setOf(armor.id))

        assertEquals("E", equipped.inventory.single().state)
        assertEquals(13, equipped.protectionBase("Geral"))
        assertEquals(2, equipped.localProtection(equipped.bodyRegions[1]))
        assertEquals(0, equipped.localProtection(equipped.bodyRegions[2]))
    }

    @Test fun theSameEquipmentContributesPgOnlyOnceAcrossMultipleRegions() {
        val armor = InventoryItem(id = "armor-1", pg = 3, pl = 2, region = "braços", category = "Armadura")
        val character = Character(inventory = listOf(armor))
            .equipItems(2, setOf(armor.id))
            .equipItems(3, setOf(armor.id))

        assertEquals(13, character.protectionBase("Geral"))
        assertEquals(2, character.localProtection(character.bodyRegions[2]))
        assertEquals(2, character.localProtection(character.bodyRegions[3]))
    }

    @Test fun removingAnEquippedItemAlsoClearsBodyReferences() {
        val armor = InventoryItem(id = "armor-1", pg = 3, pl = 2, region = "torso", category = "Armadura")
        val character = Character(inventory = listOf(armor))
            .equipItems(1, setOf(armor.id))
            .removeInventoryItem(armor.id)

        assertEquals(0, character.inventory.size)
        assertEquals(emptyList<String>(), character.bodyRegions[1].equippedItemIds)
        assertEquals(10, character.protectionBase("Geral"))
    }

    @Test fun equipmentCanBeAssignedToBothFeetAndEveryBodyRegion() {
        val item = InventoryItem(id = "boots-1", name = "Botas", pg = 1, pl = 2, region = "pés")
        val character = defaultBodyRegions().indices.fold(Character(inventory = listOf(item))) { current, index ->
            current.equipItems(index, setOf(item.id))
        }

        assertEquals(listOf(item.id), character.bodyRegions[8].equippedItemIds)
        assertEquals(listOf(item.id), character.bodyRegions[9].equippedItemIds)
        assertEquals(2, character.localProtection(character.bodyRegions[8]))
        assertEquals(2, character.localProtection(character.bodyRegions[9]))
        assertEquals(11, character.protectionBase("Geral"))
    }

    @Test fun onlyOneArmorCanOccupyTheSameBodyRegion() {
        val first = InventoryItem(id = "a", region = "torso", category = "Armadura", pg = 1)
        val second = InventoryItem(id = "b", region = "torso", category = "Armadura", pg = 2)
        val character = Character(inventory = listOf(first, second)).equipItems(1, setOf(first.id, second.id))

        assertEquals(listOf(second.id), character.bodyRegions[1].equippedItemIds)
        assertEquals(12, character.protectionBase("Geral"))
    }

    @Test fun equippedBonusesAffectAttributesBasicAndAcquiredKnowledges() {
        val item = InventoryItem(
            id = "bonus", region = "torso", category = "Acessório",
            bonuses = listOf(
                ItemBonus(ItemBonusType.ATTRIBUTE, "FOR", 2),
                ItemBonus(ItemBonusType.BASIC_KNOWLEDGE, "Vitalidade", 3),
                ItemBonus(ItemBonusType.ACQUIRED_KNOWLEDGE, "Ferreiro", 1),
            ),
        )
        val character = Character(
            inventory = listOf(item),
            learnedKnowledges = listOf(SpecialKnowledge(name = "Ferreiro", value = 2)),
        ).equipItems(1, setOf(item.id))

        assertEquals(4, character.maximumLoad)
        assertEquals(13, character.lifeBase)
        assertEquals(3, character.acquiredKnowledgeValue("Ferreiro"))
    }

    @Test fun legacyBodyOrderIsNormalizedWithoutLosingAssignments() {
        val legacy = listOf(
            BodyRegion(1, "Cabeça"), BodyRegion(2, "Braço esquerdo", equippedItemIds = listOf("brace")),
            BodyRegion(3, "Braço direito"), BodyRegion(4, "Torso", damage = "ferido"),
            BodyRegion(5, "Mão esquerda"), BodyRegion(6, "Mão direita"),
            BodyRegion(7, "Perna esquerda"), BodyRegion(8, "Perna direita"),
            BodyRegion(9, "Pé esquerdo"), BodyRegion(10, "Pé direito"),
        )
        val normalized = normalizeBodyRegions(legacy)

        assertEquals("Torso", normalized[1].name)
        assertEquals("ferido", normalized[1].damage)
        assertEquals(listOf("brace"), normalized[3].equippedItemIds)
        assertEquals((1..10).toList(), normalized.map { it.roll })
    }
}
