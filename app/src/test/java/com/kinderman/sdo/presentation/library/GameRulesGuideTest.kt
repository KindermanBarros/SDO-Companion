package com.kinderman.sdo.presentation.library

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesGuideTest {

    @Test
    fun parsesBundledAtomicHandbookRulesJsonIntoKeywordOrganizedTopics() {
        val assetFile = listOf(
            File("src/main/assets/player_handbook_rules.json"),
            File("app/src/main/assets/player_handbook_rules.json"),
        ).first { it.exists() }

        val json = Json { ignoreUnknownKeys = true }
        val doc = json.decodeFromString<AtomicRulesDocument>(assetFile.readText(Charsets.UTF_8))
        assertEquals(1902, doc.regras.size)

        val topics = GameRulesGuide.parseAtomicRules(doc)
        assertEquals(192, topics.size)
        assertTrue(topics.all { it.title.isNotBlank() && it.category.isNotBlank() && it.effectiveKeywords().isNotEmpty() })
        assertTrue(topics.any { "Combate" in it.effectiveKeywords() })
        assertTrue(topics.any { "Ataque" in it.effectiveKeywords() })
        assertTrue(topics.any { "Ações" in it.effectiveKeywords() })
        val divineTopics = topics.filter { it.category == "Deidades & Divino" }
        assertEquals(8, divineTopics.size)
        assertTrue(divineTopics.all { "Deidades" in it.effectiveKeywords() })
        assertTrue(divineTopics.any { it.id == "deidades_e_avatares.ascensao_e_restricao_material" && it.formulas.isNotEmpty() })
        assertTrue(divineTopics.any { it.id == "deidades_e_avatares.atos_de_dominio_e_conflitos" && it.examples.isNotEmpty() })
        assertTrue(topics.any { it.scales.isNotEmpty() })
        assertTrue(topics.any { it.formulas.isNotEmpty() })
    }
}
