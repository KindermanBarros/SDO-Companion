package com.kinderman.sdo.data.local

import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.defaultAttributes
import org.junit.Assert.*
import org.junit.Test

class CharacterConvertersIntegrityTest {
    private val converter = CharacterConverters()

    @Test fun truncatedLegacyAttributeDoesNotCrashCharacterLoading() {
        val decoded = converter.stringToAttributes("Força")
        assertEquals("Força", decoded.single().name)
        assertEquals(0, decoded.single().value)
    }

    @Test fun validAttributeValuesAndSkillsSurviveRoundTrip() {
        val attributes = defaultAttributes().map { it.copy(value = 4, modifier = 2) }
        assertEquals(attributes, converter.stringToAttributes(converter.attributesToString(attributes)))
    }

    @Test fun oldMysticUsageFieldsAreNotMistakenForCatalogMetadata() {
        val legacy = listOf("id", "Magia", "Nome", "2 PM", "Ação", "Pessoal", "Cena", "Efeito",
            "true", "false", "ARCANE", "2", "SCENE", "1", "1", "old-period").joinToString("\u001f")
        val decoded = converter.stringToAbilities(legacy).single()
        assertEquals("Efeito", decoded.effect)
        assertTrue(decoded.favorite)
        assertFalse(decoded.available)
        assertEquals("", decoded.catalogEntryId)
        assertEquals("", decoded.category)
    }

    @Test fun completeMysticCatalogSnapshotSurvivesRoundTrip() {
        val ability = MysticAbility(id = "id", name = "Magia", effect = "Efeito\ncompleto", category = "Categoria",
            source = "Fonte", ruleReference = "Regra", catalogEntryId = "canonical:1", catalogVersion = 2)
        assertEquals(listOf(ability), converter.stringToAbilities(converter.abilitiesToString(listOf(ability))))
    }
}
