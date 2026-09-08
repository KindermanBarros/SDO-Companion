package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.CalculatedValue
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemBonus
import com.kinderman.sdo.domain.model.ItemBonusType
import com.kinderman.sdo.domain.model.ModifierSourceType
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.agilityLimitBreakdown
import com.kinderman.sdo.domain.model.generalProtectionBreakdown
import com.kinderman.sdo.domain.model.localProtectionBreakdown
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
    ItemBonusAuditSection(character, enabled, onChange)
}

@Composable
private fun ItemBonusAuditSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = Acid) {
        SectionHeader("09.B", "Bônus mecânicos dos itens")
        Text(
            "Todo bônus abaixo usa tipo e destino controlados. O efeito só entra no cálculo enquanto o item estiver equipado.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        if (character.inventory.isEmpty()) {
            Text("Nenhum item no inventário.", color = Muted)
        }
        character.inventory.forEachIndexed { itemIndex, item ->
            ControlledItemBonusEditor(
                character = character,
                item = item,
                enabled = enabled,
                onItem = { updated ->
                    onChange(character.copy(inventory = character.inventory.replace(itemIndex, updated)))
                },
            )
        }
    }
}

@Composable
private fun ControlledItemBonusEditor(
    character: Character,
    item: InventoryItem,
    enabled: Boolean,
    onItem: (InventoryItem) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(Carbon).padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(item.name.ifBlank { "ITEM SEM NOME" }, color = Ice, style = MaterialTheme.typography.titleSmall)
        if (item.bonuses.isEmpty()) Text("SEM BÔNUS", color = Muted, style = MaterialTheme.typography.labelSmall)
        item.bonuses.forEachIndexed { bonusIndex, bonus ->
            val options = controlledBonusTargets(character, bonus.type)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        enabled = enabled,
                        onClick = {
                            val nextType = ItemBonusType.entries[(bonus.type.ordinal + 1) % ItemBonusType.entries.size]
                            val nextTarget = controlledBonusTargets(character, nextType).firstOrNull().orEmpty()
                            onItem(item.copy(bonuses = item.bonuses.replace(bonusIndex, bonus.copy(type = nextType, target = nextTarget))))
                        },
                    ) { Text("TIPO // ${bonus.type.label.uppercase()}") }
                    TextButton(
                        enabled = enabled,
                        onClick = { onItem(item.copy(bonuses = item.bonuses.filterIndexed { index, _ -> index != bonusIndex })) },
                    ) { Text("REMOVER", color = Signal) }
                }
                TextButton(
                    enabled = enabled && options.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val current = options.indexOf(bonus.target)
                        val next = options[(current.coerceAtLeast(-1) + 1) % options.size]
                        onItem(item.copy(bonuses = item.bonuses.replace(bonusIndex, bonus.copy(target = next))))
                    },
                ) {
                    Text("APLICAR EM // ${bonus.displayTarget().ifBlank { if (options.isEmpty()) "SEM OPÇÕES" else "SELECIONAR" }}")
                }
                IntegerField("Valor", bonus.value, enabled) { value ->
                    onItem(item.copy(bonuses = item.bonuses.replace(bonusIndex, bonus.copy(value = value.coerceIn(-99, 99)))))
                }
                if (bonus.target.isBlank()) Text("Selecione um destino antes de considerar o bônus configurado.", color = Signal, style = MaterialTheme.typography.bodySmall)
            }
        }
        AddButton("Adicionar bônus", enabled) {
            onItem(
                item.copy(
                    bonuses = item.bonuses + ItemBonus(
                        type = ItemBonusType.ATTRIBUTE,
                        target = controlledBonusTargets(character, ItemBonusType.ATTRIBUTE).first(),
                        value = 0,
                    ),
                ),
            )
        }
    }
}

private fun controlledBonusTargets(character: Character, type: ItemBonusType): List<String> = when (type) {
    ItemBonusType.ATTRIBUTE -> listOf("FOR", "VIG", "AGI", "POD", "INT", "CAR")
    ItemBonusType.BASIC_KNOWLEDGE -> character.attributes.flatMap { attribute ->
        attribute.skills.map { skill -> ItemBonus.basicKnowledgeTarget(attribute.acronym, skill.name) }
    }
    ItemBonusType.ACQUIRED_KNOWLEDGE -> character.learnedKnowledges
        .map(SpecialKnowledge::name)
        .filter(String::isNotBlank)
        .distinct()
}

@Composable
internal fun CalculatedValuesAuditSection(character: Character) {
    TechPanel(accent = Acid) {
        SectionHeader("06.B", "Auditoria de valores calculados")
        Text("RECURSOS", color = Acid, style = MaterialTheme.typography.labelLarge)
        CalculationLine("Vida máxima", "10 + Vitalidade", character.lifeCalculation())
        CalculationLine("Sanidade máxima", "10 + Sanidade", character.sanityCalculation())
        CalculationLine("Arcano máximo", "POD + Arcano", character.arcaneCalculation())
        CalculationLine("Energia máxima", "VIG + Energia", character.energyCalculation())

        Text("ATRIBUTOS", color = Acid, style = MaterialTheme.typography.labelLarge)
        character.attributes.forEach { attribute ->
            CalculationLine(attribute.acronym, "Base + ajuste + bônus ativos", character.attributeCalculation(attribute.acronym))
        }

        Text("CONHECIMENTOS BÁSICOS", color = Acid, style = MaterialTheme.typography.labelLarge)
        character.attributes.forEach { attribute ->
            attribute.skills.forEach { skill ->
                CalculationLine(skill.name, "Base + ajuste + bônus ativos", character.basicKnowledgeCalculation(attribute.acronym, skill.name))
            }
        }

        if (character.learnedKnowledges.isNotEmpty()) {
            Text("CONHECIMENTOS ADQUIRIDOS", color = Acid, style = MaterialTheme.typography.labelLarge)
            character.learnedKnowledges.map(SpecialKnowledge::name).filter(String::isNotBlank).distinct().forEach { name ->
                CalculationLine(name, "Base + ajuste + bônus ativos", character.acquiredKnowledgeCalculation(name))
            }
        }

        Text("PROTEÇÕES", color = Acid, style = MaterialTheme.typography.labelLarge)
        CalculationLine("PG / Geral", "10 + PG dos itens equipados + ajuste", character.generalProtectionBreakdown())
        listOf("Esquiva", "Postura", "Mental", "Arcana").forEach { name ->
            CalculationLine(name, "Fórmula da proteção + ajuste", character.protectionCalculation(name))
        }
        character.bodyRegions.forEach { region ->
            CalculationLine("PL — ${region.name}", "PL base regional + itens equipados", character.localProtectionBreakdown(region))
        }
        val agilityLimit = character.agilityLimitBreakdown()
        Column(Modifier.fillMaxWidth().background(Carbon).padding(7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("LA // ${agilityLimit.total ?: "—"}", color = Ice, style = MaterialTheme.typography.labelLarge)
            Text("REGRA // menor LA entre itens equipados", color = Muted, style = MaterialTheme.typography.labelSmall)
            agilityLimit.contributions.forEach { source ->
                Text("Item: LA ${source.value} — ${source.label}", color = Acid, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CalculationLine(label: String, formula: String, value: CalculatedValue) {
    Column(Modifier.fillMaxWidth().background(Carbon).padding(7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$label // TOTAL ${value.total}", color = Ice, style = MaterialTheme.typography.labelLarge)
        Text("FÓRMULA // $formula", color = Muted, style = MaterialTheme.typography.labelSmall)
        Text("BASE ${value.base} // AJUSTE ${signed(value.adjustment)}", color = Muted, style = MaterialTheme.typography.bodySmall)
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
            Text("$type: ${signed(modifier.value)} — ${modifier.label}", color = Acid, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()
