package com.kinderman.sdo.presentation.historian

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.catalog.PathPresets
import com.kinderman.sdo.domain.catalog.RaceCatalog
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.userFacingSource
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.formattedAbilityCost
import com.kinderman.sdo.domain.model.formattedAbilityDuration
import com.kinderman.sdo.domain.model.formattedAbilityExecution

@Composable
internal fun CompendiumDetails(entry: CatalogEntry) {
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
    Text(if (expanded) "Recolher detalhes ▴" else "Ver todos os valores ▾",
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded })
    if (expanded) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val values = linkedMapOf(
            "Custo" to entry.cost, "Ação" to entry.action, "Alcance" to entry.range,
            "Duração" to entry.duration, "Limite" to entry.limit,
            "Ativação" to entry.activationCondition, "Encerramento" to entry.deactivationCondition,
            "Aprimoramentos" to entry.enhancements, "Efeito mecânico" to entry.mechanicalEffect,
            "Atributo" to entry.relatedAttribute, "Valor inicial" to entry.initialValue?.toString().orEmpty(),
            "Pré-requisitos" to entry.prerequisites.joinToString("; "), "Fonte" to entry.userFacingSource(),
            "Referência" to entry.ruleReference, "Versão" to entry.version.toString(),
            "Palavras-chave" to entry.keywords.joinToString(", "),
        )
        if (entry.kind == CatalogKind.ITEM) values.putAll(linkedMapOf(
            "Criação" to entry.creationCost, "Preço" to "E$ ${entry.price}",
            "Carga" to entry.load.toString(), "Durabilidade" to entry.durability, "Região" to entry.region,
        ))
        values.filterValues(String::isNotBlank).forEach { (label, value) ->
            Text("$label: $value", style = MaterialTheme.typography.bodySmall)
        }
        PathPresets.find(entry.id)?.let { path ->
            Text("Lema: ${path.motto}")
            Text("Palavras-chave: ${path.keywords.joinToString(", ")}")
            path.pillars.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            path.powers.forEach { Text("${it.name}\n${it.effect}", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

internal fun racialReferences(): List<CatalogEntry> =
    RaceCatalog.races.map { race ->
        CatalogEntry("race:${race.name}", CatalogKind.POWER, race.name, "Raça",
            "HP ${race.hp}; Sanidade ${race.sanity}; Arcano ${race.arcane}; Energia ${race.energy}; Atributo ${race.attribute} +1",
            mechanicalEffect = race.powers.joinToString("\n\n") { "${it.name}: ${it.effect}" },
            source = race.source, ruleReference = race.ruleReference, version = race.version)
    } + RaceCatalog.subRaces.map { race ->
        CatalogEntry("subrace:${race.name}", CatalogKind.POWER, race.name, "Sub-raça",
            if (race.organicOnly) "Somente raças orgânicas" else "Compatibilidade conforme regras raciais",
            mechanicalEffect = race.powers.joinToString("\n\n") { "${it.name}: ${it.effect}" },
            source = race.source, ruleReference = race.ruleReference, version = race.version)
    }

/** Read-only snapshots are restricted to characters already authorized for this campaign. */
internal fun characterReferences(characters: List<Character>): List<CatalogEntry> = characters.flatMap { character ->
    val source = "Ficha: ${character.name}"
    character.powers.map { power ->
        CatalogEntry("${character.id}:power:${power.id}", CatalogKind.POWER, power.name, power.category,
            power.effect,
            formattedAbilityCost(power.costType, power.costValue),
            formattedAbilityExecution(power.executionType, power.timeValue, power.timeUnit),
            power.rangeType.label,
            formattedAbilityDuration(power.durationType, power.durationValue, power.durationUnit),
            source,
            mechanicalEffect = listOf(power.effect, "Fonte: ${power.canonicalSource.label}", "Alvo/Área: ${power.targetArea}", "Resistência: ${power.resistance.label}",
                "Encerramento: ${power.deactivationCondition}", "Limite: ${power.limit}", "Aprimoramentos: ${power.enhancements}",
                "Disponível: ${power.available}; Favorito: ${power.favorite}").joinToString("\n"),
            prerequisites = power.prerequisites, ruleReference = power.ruleReference)
    } + character.mysticAbilities.map { magic ->
        val kind = when {
            magic.type.equals("Runa", true) -> CatalogKind.RUNE
            magic.type.equals("Cinza", true) -> CatalogKind.ASH
            else -> CatalogKind.MAGIC
        }
        val abilitySource = if (magic.type.equals("Cinza", true)) "${magic.ashSource.label} / ${magic.ashPurity.label}" else magic.canonicalSource.label
        CatalogEntry("${character.id}:magic:${magic.id}", kind, magic.name, magic.type,
            magic.effect,
            formattedAbilityCost(magic.costType, magic.costValue),
            formattedAbilityExecution(magic.executionType, magic.timeValue, magic.timeUnit),
            magic.rangeType.label,
            formattedAbilityDuration(magic.durationType, magic.durationValue, magic.durationUnit),
            source,
            mechanicalEffect = "${magic.effect}\nFonte: $abilitySource\nAlvo/Área: ${magic.targetArea}\nResistência: ${magic.resistance.label}\nDisponível: ${magic.available}; Favorito: ${magic.favorite}",
            ruleReference = magic.ruleReference)
    } + (character.learnedKnowledges + character.arcaneKnowledges + character.battleTechniques).map { knowledge ->
        CatalogEntry("${character.id}:knowledge:${knowledge.id}", CatalogKind.ACQUIRED_KNOWLEDGE,
            knowledge.name, knowledge.category, knowledge.description, source = source,
            initialValue = knowledge.value, relatedAttribute = knowledge.attribute,
            mechanicalEffect = "${knowledge.mechanicalEffect}\nAjuste: ${knowledge.adjustment}",
            prerequisites = knowledge.prerequisites, ruleReference = knowledge.ruleReference, keywords = knowledge.keywords)
    } + character.inventory.map { item ->
        CatalogEntry("${character.id}:item:${item.id}", CatalogKind.ITEM, item.name, item.category,
            item.effect, source = source, load = item.load, durability = item.durability, region = item.region,
            mechanicalEffect = "${item.effect}\nPG: ${item.pg}; PL: ${item.pl}; Qualidade: ${item.quality}; Estado: ${item.state}\nLimite de AGI: ${item.agilityLimit ?: "—"}\nBônus: ${item.bonuses.joinToString()}")
    }
}
