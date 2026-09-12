package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.AbilitySource
import com.kinderman.sdo.domain.model.AshPurity
import com.kinderman.sdo.domain.model.AshSource
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.SourceKind
import com.kinderman.sdo.domain.model.allCanonicalAbilitiesSafely
import com.kinderman.sdo.domain.model.toCanonicalAbility
import com.kinderman.sdo.domain.model.userFacingSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseOneCatalogTest {
    @Test fun everyPublishedPowerCanBeAddedToTheCanonicalSheet() {
        BuiltInCatalog.entries.filter { it.kind == CatalogKind.POWER }.forEach { entry ->
            entry.toStructuredPower(PowerSourceType.NARRATIVE, entry.id).toCanonicalAbility()
        }
    }
    @Test fun mysticCatalogOriginUsesRuleValuesInsteadOfInternalCatalogIds() {
        val magic = CatalogEntry(
            id = "magic.test",
            kind = CatalogKind.MAGIC,
            name = "Magia",
            group = "Arcanismo",
            summary = "",
            source = "CATALOGO;; magic.test",
            abilitySource = AbilitySource.KNOWLEDGE,
            sourceKnowledge = "Arcanismo",
            sourceLevel = 2,
        )
        val ash = CatalogEntry(
            id = "ash.test",
            kind = CatalogKind.ASH,
            name = "Cinza",
            group = "",
            summary = "",
            source = "CATALOGO;; ash.test",
            catalogAshSource = AshSource.FIRE,
            catalogAshPurity = AshPurity.REFINED,
        )
        val runeWithoutTypedSource = CatalogEntry(
            id = "rune.test",
            kind = CatalogKind.RUNE,
            name = "Runa",
            group = "",
            summary = "",
            source = "CATALOGO;; rune.test",
        )

        assertEquals("Conhecimento — Arcanismo (nível 2)", magic.userFacingSource())
        assertEquals("Fogo — Refinada", ash.userFacingSource())
        assertEquals("Catálogo", runeWithoutTypedSource.userFacingSource())
    }

    @Test fun knowledgeCatalogContainsAllThreeIndependentKinds() {
        val kinds = KnowledgeCatalog.entries.map { it.kind }.toSet()
        assertTrue(CatalogKind.ACQUIRED_KNOWLEDGE in kinds)
        assertTrue(CatalogKind.ARCANE_KNOWLEDGE in kinds)
        assertTrue(CatalogKind.BATTLE_TECHNIQUE in kinds)
    }

    @Test fun catalogKnowledgeIsCopiedAsVersionedSnapshot() {
        val entry = CatalogEntry(
            id = "knowledge.test",
            kind = CatalogKind.ACQUIRED_KNOWLEDGE,
            name = "Cartografia",
            group = "Ofício",
            summary = "Mapas e orientação.",
            relatedAttribute = "INT",
            initialValue = 2,
            prerequisites = listOf("Treinamento"),
            mechanicalEffect = "+2 em situações publicadas pela regra.",
            source = "Livro básico",
            ruleReference = "p. 42",
            keywords = listOf("mapa"),
            version = 3,
        )

        val copied = entry.toSpecialKnowledge()

        assertEquals("knowledge.test", copied.catalogEntryId)
        assertEquals(3, copied.catalogVersion)
        assertEquals(2, copied.value)
        assertEquals("Cartografia", copied.name)
        assertEquals(listOf("Treinamento"), copied.prerequisites)
    }

    @Test fun nonRepeatableCatalogEntriesCannotBeAddedTwice() {
        val entry = CatalogEntry(
            id = "knowledge.test",
            kind = CatalogKind.ACQUIRED_KNOWLEDGE,
            name = "Cartografia",
            group = "Ofício",
            summary = "",
        )
        val values = listOf(SpecialKnowledge(catalogEntryId = entry.id, name = entry.name))

        assertFalse(values.canAddCatalogEntry(entry))
        assertTrue(values.canAddCatalogEntry(entry.copy(id = "knowledge.other")))
        assertTrue(values.canAddCatalogEntry(entry.copy(repeatable = true)))
    }

    @Test fun pathChangePreservesPowersFromOtherSources() {
        val pathEntry = CatalogEntry(
            id = "path.engrenagens",
            kind = CatalogKind.PATH,
            name = "Engrenagens",
            group = "Caminho",
            summary = "",
        )
        val racePower = Power(name = "Poder racial", sourceType = PowerSourceType.RACE, sourceId = "race.test")
        val manualPower = Power(name = "Manual", sourceType = PowerSourceType.MANUAL)
        val oldPathPower = Power(name = "Antigo", sourceType = PowerSourceType.PATH, sourceId = "path.old", origin = "Caminho — Antigo")
        val character = Character(powers = listOf(racePower, manualPower, oldPathPower))

        val changed = character.withStructuredPathPreset(pathEntry)

        assertTrue(changed.powers.any { it.id == racePower.id })
        assertTrue(changed.powers.any { it.id == manualPower.id })
        assertFalse(changed.powers.any { it.id == oldPathPower.id })
        assertTrue(changed.powers.filter { it.sourceType == PowerSourceType.PATH }.all { it.sourceId == pathEntry.id })
    }

    @Test fun selectingSamePathDoesNotDuplicatePathPowers() {
        val pathEntry = CatalogEntry(
            id = "path.engrenagens",
            kind = CatalogKind.PATH,
            name = "Engrenagens",
            group = "Caminho",
            summary = "",
        )
        val once = Character().withStructuredPathPreset(pathEntry)
        val twice = once.withStructuredPathPreset(pathEntry)

        assertEquals(once.powers.count { it.sourceType == PowerSourceType.PATH }, twice.powers.count { it.sourceType == PowerSourceType.PATH })
        assertEquals(twice.powers.size, twice.powers.map { it.name to it.sourceType }.distinct().size)
        assertEquals(2, twice.allCanonicalAbilitiesSafely().count { it.source?.kind == SourceKind.Path })
    }
    @Test fun loadingCharacterRefreshesManagedRacialAndPathPowers() {
        val stored = Character(
            race = "Skayra",
            powers = listOf(
                Power(
                    name = "Marca do Pacto",
                    origin = "Raça — Skayra",
                    sourceType = PowerSourceType.RACE,
                    costValue = 1,
                ),
                Power(
                    name = "Ritos de Passagem",
                    origin = "Caminho — Necrocamminus",
                    sourceType = PowerSourceType.PATH,
                    sourceId = "path.necrocamminus",
                    costValue = 1,
                ),
            ),
        )

        val refreshed = stored.withRefreshedPresetPowers()
        val racial = refreshed.powers.first { it.name == "Marca do Pacto" }
        val path = refreshed.powers.first { it.name == "Ritos de Passagem" }

        assertEquals(2, racial.costValue)
        assertEquals("2 PE", racial.cost)
        assertEquals(2, path.costValue)
        assertEquals("2 PE", path.cost)
    }

}
