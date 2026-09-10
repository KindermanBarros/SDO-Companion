package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SpecialKnowledge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInCatalogTest {
    @Test fun containsAllPublishedExamplesAndPaths() {
        assertEquals(100, BuiltInCatalog.entries.count { it.kind == CatalogKind.POWER })
        assertEquals(50, BuiltInCatalog.entries.count { it.kind == CatalogKind.POWER && it.source == "50 Exemplos de Poderes Mágicos" })
        assertEquals(50, BuiltInCatalog.entries.count { it.kind == CatalogKind.POWER && it.source == "50 Exemplos de Poderes de Profissão e Conhecimento" })
        assertEquals(50, BuiltInCatalog.entries.count { it.kind == CatalogKind.MAGIC })
        assertEquals(150, BuiltInCatalog.entries.count { it.kind == CatalogKind.ASH })
        assertEquals(50, BuiltInCatalog.entries.count { it.kind == CatalogKind.RUNE })
        assertTrue(BuiltInCatalog.entries.count { it.kind == CatalogKind.PATH } >= 18)
        assertTrue(BuiltInCatalog.entries.count { it.kind == CatalogKind.ITEM } >= 60)
    }

    @Test fun idsAreStableAndUnique() {
        val ids = BuiltInCatalog.entries.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.all { it.matches(Regex("[a-z]+(?:\\.[a-z0-9_]+)+")) })
    }

    @Test fun everyCatalogEntryTracksTheCurrentCanonicalRules() {
        assertTrue(BuiltInCatalog.entries.filter { it.kind !in setOf(CatalogKind.MAGIC, CatalogKind.RUNE, CatalogKind.ASH) }
            .all { it.version == BuiltInCatalog.VERSION })
        assertTrue(BuiltInCatalog.entries.filter { it.kind in setOf(CatalogKind.MAGIC, CatalogKind.RUNE, CatalogKind.ASH) }
            .all { it.version == 5 })
        assertTrue(KnowledgeCatalog.entries.all { it.version == KnowledgeCatalog.VERSION })
        val entries = BuiltInCatalog.entries + KnowledgeCatalog.entries
        assertTrue(entries.all { it.source.isNotBlank() })
        assertTrue(entries.all { it.ruleReference.isNotBlank() })
        assertTrue(RaceCatalog.races.all { it.version == BuiltInCatalog.VERSION && it.ruleReference == RaceCatalog.RULE_REFERENCE })
        assertTrue(RaceCatalog.subRaces.all { it.version == BuiltInCatalog.VERSION && it.ruleReference == RaceCatalog.RULE_REFERENCE })
    }

    @Test fun knowledgeCatalogHasFiftyEntriesOfEachType() {
        assertEquals(50, KnowledgeCatalog.entries.count { it.kind == CatalogKind.ACQUIRED_KNOWLEDGE })
        assertEquals(50, KnowledgeCatalog.entries.count { it.kind == CatalogKind.ARCANE_KNOWLEDGE })
        assertEquals(50, KnowledgeCatalog.entries.count { it.kind == CatalogKind.BATTLE_TECHNIQUE })
        assertTrue(KnowledgeCatalog.entries.none { it.name.startsWith("Estudo de ", ignoreCase = true) })
        assertTrue(KnowledgeCatalog.entries.all { it.initialValue == 1 })
        assertTrue(KnowledgeCatalog.entries.any { it.name == "Natureza" && it.kind == CatalogKind.ARCANE_KNOWLEDGE })
        assertTrue(KnowledgeCatalog.entries.any { it.name == "Cura" && it.kind == CatalogKind.ARCANE_KNOWLEDGE })
    }

    @Test fun everyDefaultPowerIsACompleteCreationGuide() {
        val powers = BuiltInCatalog.entries.filter { it.kind == CatalogKind.POWER }
        assertTrue(powers.all { it.name.isNotBlank() && it.summary.isNotBlank() })
        assertTrue(powers.all { it.cost.isNotBlank() && it.action.isNotBlank() })
        assertTrue(powers.all { it.range.isNotBlank() && it.duration.isNotBlank() })
        assertTrue(powers.all { it.mechanicalEffect.isNotBlank() && it.ruleReference.isNotBlank() })
        assertTrue(powers.all { it.activationCondition != it.mechanicalEffect })
        assertTrue(powers.none { "Profissão:" in it.mechanicalEffect || "Categoria:" in it.mechanicalEffect })
        assertTrue(powers.all { it.targetArea.isNotBlank() && it.abilitySource != null })
        assertTrue(powers.all {
            it.abilityCostType in setOf(
                com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
                com.kinderman.sdo.domain.model.AbilityCostType.LIFE,
                com.kinderman.sdo.domain.model.AbilityCostType.SANITY,
                com.kinderman.sdo.domain.model.AbilityCostType.DESTINY,
            )
        })
        assertTrue(powers.none { it.abilityCostType == com.kinderman.sdo.domain.model.AbilityCostType.ARCANE })
        assertTrue(powers.all { if (it.abilityExecution == com.kinderman.sdo.domain.model.AbilityExecution.PASSIVE) it.abilityCostValue == 0 else (it.abilityCostValue ?: 0) > 0 })
        assertTrue(powers.all { entry ->
            entry.toStructuredPower().let { power ->
                power.canonicalSource == entry.abilitySource && power.costType == entry.abilityCostType &&
                    power.costValue == entry.abilityCostValue && power.executionType == entry.abilityExecution &&
                    power.rangeType == entry.abilityRange && power.targetArea == entry.targetArea &&
                    power.durationType == entry.abilityDuration && power.resistance == entry.abilityResistance
            }
        })
    }

    @Test fun everyMysticExampleKeepsMetadataOutOfItsEffect() {
        val abilities = BuiltInCatalog.entries.filter { it.kind in setOf(CatalogKind.MAGIC, CatalogKind.RUNE, CatalogKind.ASH) }
        assertTrue(abilities.all { it.targetArea.isNotBlank() })
        assertTrue(abilities.none { it.mechanicalEffect.startsWith("Suporte e gatilho:", ignoreCase = true) })
        assertTrue(abilities.filter { it.kind in setOf(CatalogKind.MAGIC, CatalogKind.RUNE) }
            .all { it.abilityCostType == com.kinderman.sdo.domain.model.AbilityCostType.ARCANE })
        assertTrue(abilities.filter { it.kind == CatalogKind.ASH }
            .all { it.abilityCostType == com.kinderman.sdo.domain.model.AbilityCostType.DOSE })
    }

    @Test fun everyDefaultMagicIsCompleteAndKeepsItsCanonicalProvenance() {
        val magics = BuiltInCatalog.entries.filter { it.kind == CatalogKind.MAGIC }
        assertTrue(magics.all { it.name.isNotBlank() && it.summary.isNotBlank() })
        assertTrue(magics.all { it.cost.isNotBlank() && it.action.isNotBlank() })
        assertTrue(magics.all { it.range.isNotBlank() && it.duration.isNotBlank() })
        assertTrue(magics.all { it.mechanicalEffect.isNotBlank() && it.ruleReference.isNotBlank() })
        assertTrue(magics.all { entry ->
            entry.toMysticAbility().let { ability ->
                ability.catalogEntryId == entry.id && ability.catalogVersion == entry.version &&
                    ability.source == entry.source && ability.category == entry.group &&
                    ability.costType == entry.abilityCostType && ability.costValue == entry.abilityCostValue &&
                    ability.executionType == entry.abilityExecution && ability.rangeType == entry.abilityRange &&
                    ability.targetArea == entry.targetArea && ability.durationType == entry.abilityDuration &&
                    ability.resistance == entry.abilityResistance
            }
        })
    }

    @Test fun catalogMagicBindsOnlyToAnOwnedKnowledgeAtTheRequiredLevel() {
        val entry = BuiltInCatalog.entries.first { it.kind == CatalogKind.MAGIC && it.sourceKnowledge == "Abjuração" }
        val lowLevel = Character(arcaneKnowledges = listOf(SpecialKnowledge(id = "abj", name = "Abjuração", value = 0)))
        val ready = lowLevel.copy(arcaneKnowledges = listOf(lowLevel.arcaneKnowledges.single().copy(value = entry.sourceLevel!!)))

        assertTrue(runCatching { entry.toMysticAbility(lowLevel) }.isFailure)
        assertEquals("abj", entry.toMysticAbility(ready).knowledgeId)
        assertEquals(entry.sourceLevel, entry.toMysticAbility(ready).knowledgeLevel)
    }
    @Test fun presetPowersKeepMechanicsInStructuredFields() {
        val racialPowers = RaceCatalog.races.flatMap { it.powers } + RaceCatalog.subRaces.flatMap { it.powers }
        val pathPowers = PathPresets.entries.flatMap { it.powers }
        val allowedCosts = setOf(
            com.kinderman.sdo.domain.model.AbilityCostType.NONE,
            com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
            com.kinderman.sdo.domain.model.AbilityCostType.LIFE,
            com.kinderman.sdo.domain.model.AbilityCostType.SANITY,
            com.kinderman.sdo.domain.model.AbilityCostType.DESTINY,
        )

        assertTrue(racialPowers.all { it.costType in allowedCosts })
        assertTrue(pathPowers.all { it.costType in allowedCosts })
        assertTrue(racialPowers.none { it.costType == com.kinderman.sdo.domain.model.AbilityCostType.ARCANE })
        assertTrue(pathPowers.none { it.costType == com.kinderman.sdo.domain.model.AbilityCostType.ARCANE })
        assertTrue(racialPowers.all { it.costValue == 0 || it.executionType != com.kinderman.sdo.domain.model.AbilityExecution.PASSIVE })
        assertTrue(pathPowers.all { it.costValue == 0 || it.executionType != com.kinderman.sdo.domain.model.AbilityExecution.PASSIVE })
    }

    @Test fun presetPowerCostsPreserveTheirPublishedValuesWithoutParsingEffects() {
        fun racial(name: String) = (RaceCatalog.races.flatMap { it.powers } + RaceCatalog.subRaces.flatMap { it.powers })
            .first { it.name == name }
        fun path(name: String) = PathPresets.entries.flatMap { it.powers }.first { it.name == name }

        assertEquals(2, racial("Marca do Pacto").costValue)
        assertEquals(2, racial("Anatomia Impossível").costValue)
        assertEquals(3, racial("Presença Anômala").costValue)
        assertEquals(2, racial("Vislumbre do Possível").costValue)
        assertTrue(racial("Vislumbre do Possível").destinyCostEligible)
        assertEquals(3, path("Cara ou Coroa").costValue)
        assertTrue(path("Cara ou Coroa").destinyCostEligible)
        assertEquals(2, path("Escudo de Escamas").costValue)
        assertEquals(2, path("Fio Coagulado").costValue)

        val activationCostInEffect = Regex("(?i)\\b(?:gaste|gastar|custo)\\b[^.\\n]*\\b(?:PE|PM|PV|PS|PD)\\b")
        assertTrue((RaceCatalog.races.flatMap { it.powers } + RaceCatalog.subRaces.flatMap { it.powers })
            .none { activationCostInEffect.containsMatchIn(it.effect) })
        assertTrue(PathPresets.entries.flatMap { it.powers }
            .none { activationCostInEffect.containsMatchIn(it.effect) })
    }

}
