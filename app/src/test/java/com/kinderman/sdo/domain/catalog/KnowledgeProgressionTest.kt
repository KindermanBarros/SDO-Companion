package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SpecialKnowledge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeProgressionTest {
    @Test fun knowledgeLevelCannotExceedItsPermanentAttribute() {
        val music = SpecialKnowledge(id = "music", name = "Música", attribute = "CAR")
        val character = Character(
            attributes = Character().attributes.map { if (it.acronym == "CAR") it.copy(value = 3) else it },
            learnedKnowledges = listOf(music),
        )
        assertEquals(3, character.withKnowledgeLevel("music", 5, emptyList()).learnedKnowledges.single().value)
    }

    @Test fun runicGrantsEachCanonicalPackageOnceAtItsThreshold() {
        val runic = SpecialKnowledge(id = "runic", name = "Rúnico", attribute = "POD", value = 0)
        val character = Character(
            attributes = Character().attributes.map { if (it.acronym == "POD") it.copy(value = 5) else it },
            arcaneKnowledges = listOf(runic),
        )
        val catalog = BuiltInCatalog.entries.filter { it.kind == CatalogKind.RUNE }

        val levelOne = character.withKnowledgeLevel("runic", 1, catalog)
        assertEquals(setOf("Centelha", "Solo Firme", "Frescor", "Mensagem de Eco"), levelOne.mysticAbilities.map { it.name }.toSet())
        val levelThree = levelOne.withKnowledgeLevel("runic", 3, catalog)
        assertEquals(8, levelThree.mysticAbilities.size)
        val levelFive = levelThree.withKnowledgeLevel("runic", 5, catalog)
        assertEquals(12, levelFive.mysticAbilities.size)
        assertEquals(12, levelFive.withKnowledgeLevel("runic", 5, catalog).mysticAbilities.size)
        assertEquals(listOf(1, 3, 5), levelFive.arcaneKnowledges.single().milestoneLevels)
    }

    @Test fun creationAllocationIsEphemeralAndRequiresAllCanonicalChoices() {
        val choices = List(5) { SpecialKnowledge(name = "K$it", attribute = "INT", value = 1) }
        InitialKnowledgeAllocation(choices, 15).validate()
        assertTrue(runCatching { InitialKnowledgeAllocation(choices.take(4), 15).validate() }.isFailure)
        assertTrue(runCatching { InitialKnowledgeAllocation(choices, 14).validate() }.isFailure)
    }

    @Test fun runeCatalogNeverRequestsAshDoses() {
        val runes = BuiltInCatalog.entries.filter { it.kind == CatalogKind.RUNE }
        assertFalse(runes.any { it.summary.contains("dose", true) || it.mechanicalEffect.contains("dose", true) })
        assertTrue(runes.all { it.toMysticAbility().costType.name == "ARCANE" })
    }
}
