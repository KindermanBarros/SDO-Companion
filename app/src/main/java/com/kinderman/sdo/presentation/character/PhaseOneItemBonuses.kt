package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.CalculatedValue
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ModifierSourceType
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.agilityLimitBreakdown
import com.kinderman.sdo.domain.model.arcaneMaximumBreakdown
import com.kinderman.sdo.domain.model.energyMaximumBreakdown
import com.kinderman.sdo.domain.model.generalProtectionBreakdown
import com.kinderman.sdo.domain.model.lifeMaximumBreakdown
import com.kinderman.sdo.domain.model.loadCapacityBreakdown
import com.kinderman.sdo.domain.model.localProtectionBreakdown
import com.kinderman.sdo.domain.model.sanityMaximumBreakdown
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.Carbon
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechPanel

@Composable
internal fun PhaseOneInventoryWithBonusSection(
    character: Character,
    catalog: List<com.kinderman.sdo.domain.model.CatalogEntry>,
    enabled: Boolean,
    onChange: (Character) -> Unit,
) {
    PhaseOneStrictInventorySection(character, catalog, enabled, onChange)
}

@Composable
internal fun CalculatedValuesAuditSection(character: Character) {
    TechPanel(accent = MaterialTheme.colorScheme.primary) {
        SectionHeader("06.B", "Auditoria de valores calculados")
        Text("RECURSOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        CalculationLine("Vida máxima", "10 + Vitalidade + ajustes + bônus ativos", character.lifeMaximumBreakdown())
        CalculationLine("Sanidade máxima", "10 + Sanidade + ajustes + bônus ativos", character.sanityMaximumBreakdown())
        CalculationLine("Arcano máximo", "POD + Arcano + ajustes + bônus ativos", character.arcaneMaximumBreakdown())
        CalculationLine("Energia máxima", "VIG + Energia + ajustes + bônus ativos", character.energyMaximumBreakdown())
        CalculationLine("Capacidade de carga", "2 + FOR + recipiente + bônus ativos", character.loadCapacityBreakdown())

        Text("ATRIBUTOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        character.attributes.forEach { attribute ->
            CalculationLine(attribute.acronym, "Base + ajuste + bônus ativos", character.attributeCalculation(attribute.acronym))
        }

        Text("CONHECIMENTOS BÁSICOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        character.attributes.forEach { attribute ->
            attribute.skills.forEach { skill ->
                CalculationLine(skill.name, "Base + ajuste + bônus ativos", character.basicKnowledgeCalculation(attribute.acronym, skill.name))
            }
        }

        if (character.learnedKnowledges.isNotEmpty()) {
            Text("CONHECIMENTOS ADQUIRIDOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            character.learnedKnowledges.map(SpecialKnowledge::name).filter(String::isNotBlank).distinct().forEach { name ->
                CalculationLine(name, "Base + ajuste + bônus ativos", character.acquiredKnowledgeCalculation(name))
            }
        }

        Text("PROTEÇÕES", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        CalculationLine("PG / Geral", "10 + PG dos itens equipados + ajuste", character.generalProtectionBreakdown())
        listOf("Esquiva", "Postura", "Mental", "Arcana").forEach { name ->
            CalculationLine(name, "Fórmula da proteção + ajuste", character.protectionCalculation(name))
        }
        character.bodyRegions.forEach { region ->
            CalculationLine("PL — ${region.name}", "PL base regional + itens equipados", character.localProtectionBreakdown(region))
        }
        val agilityLimit = character.agilityLimitBreakdown()
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("LA // ${agilityLimit.total ?: "—"}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
            Text("REGRA // menor LA entre itens equipados", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            agilityLimit.contributions.forEach { source ->
                Text("Item: LA ${source.value} — ${source.label}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CalculationLine(label: String, formula: String, value: CalculatedValue) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$label // TOTAL ${value.total}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
        Text("FÓRMULA // $formula", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text("BASE ${value.base} // AJUSTE ${signed(value.adjustment)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        value.modifiers.forEach { modifier ->
            val type = when (modifier.sourceType) {
                ModifierSourceType.ITEM -> "Item"
                ModifierSourceType.RACE -> "Raça"
                ModifierSourceType.TRAIT -> "Traço"
                ModifierSourceType.CONDITION -> "Condição"
                ModifierSourceType.ADJUSTMENT -> "Ajuste"
                ModifierSourceType.BASE -> "Base"
                ModifierSourceType.OTHER -> "Outro"
            }
            Text("$type: ${signed(modifier.value)} — ${modifier.label}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()
