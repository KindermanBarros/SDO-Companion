package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.catalog.canAddCatalogEntry
import com.kinderman.sdo.domain.catalog.previewPathChange
import com.kinderman.sdo.domain.catalog.toSpecialKnowledge
import com.kinderman.sdo.domain.catalog.toStructuredPower
import com.kinderman.sdo.domain.catalog.withStructuredPathPreset
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.ArcanePanel
import com.kinderman.sdo.ui.Carbon
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.LabelFunctional
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechPanel

internal val LocalAcquiredKnowledgeTargets = compositionLocalOf { emptyList<String>() }

@Composable
internal fun PhaseOneKnowledgeSection(
    character: Character,
    catalog: List<CatalogEntry>,
    enabled: Boolean,
    onChange: (Character) -> Unit,
) {
    TechPanel {
        SectionHeader("05", "Conhecimentos especiais")
        PhaseOneKnowledgeList(
            title = "Conhecimentos adquiridos",
            kind = CatalogKind.ACQUIRED_KNOWLEDGE,
            values = character.learnedKnowledges,
            catalog = catalog,
            enabled = enabled,
            totalValue = character::acquiredKnowledgeValue,
        ) { onChange(character.copy(learnedKnowledges = it)) }
        PhaseOneKnowledgeList(
            title = "Conhecimentos arcanos",
            kind = CatalogKind.ARCANE_KNOWLEDGE,
            values = character.arcaneKnowledges,
            catalog = catalog,
            enabled = enabled,
            totalValue = character::acquiredKnowledgeValue,
        ) { onChange(character.copy(arcaneKnowledges = it)) }
        PhaseOneKnowledgeList(
            title = "Técnicas de batalha",
            kind = CatalogKind.BATTLE_TECHNIQUE,
            values = character.battleTechniques,
            catalog = catalog,
            enabled = enabled,
            totalValue = character::acquiredKnowledgeValue,
        ) { onChange(character.copy(battleTechniques = it)) }
    }
}

@Composable
private fun PhaseOneKnowledgeList(
    title: String,
    kind: CatalogKind,
    values: List<SpecialKnowledge>,
    catalog: List<CatalogEntry>,
    enabled: Boolean,
    totalValue: (String) -> Int,
    onValues: (List<SpecialKnowledge>) -> Unit,
) {
    var selecting by remember { mutableStateOf(false) }
    val options = remember(catalog, kind) { catalog.filter { it.kind == kind } }
    Text(title.uppercase(), color = Acid, style = MaterialTheme.typography.labelLarge)
    values.forEachIndexed { index, knowledge ->
        Column(Modifier.fillMaxWidth().background(Carbon).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("REG.${(index + 1).toString().padStart(2, '0')}", color = LabelFunctional, modifier = Modifier.weight(1f))
                if (knowledge.isCatalogEntry) {
                    Text("CAT v${knowledge.catalogVersion}", color = Acid, style = MaterialTheme.typography.labelSmall)
                }
                RemoveButton(enabled, "Remover conhecimento") {
                    onValues(values.filterIndexed { itemIndex, _ -> itemIndex != index })
                }
            }
            HudTextField("Nome", knowledge.name, enabled = enabled) { onValues(values.replace(index, knowledge.copy(name = it))) }
            TwoFields(
                { HudTextField("Atributo", knowledge.attribute, it, enabled = enabled) { value -> onValues(values.replace(index, knowledge.copy(attribute = value.uppercase()))) } },
                { IntegerField("Base", knowledge.value, enabled, it) { value -> onValues(values.replace(index, knowledge.copy(value = value))) } },
            )
            IntegerField("Ajuste excepcional", knowledge.adjustment, enabled) { value ->
                onValues(values.replace(index, knowledge.copy(adjustment = value)))
            }
            if (knowledge.category.isNotBlank()) Text("CATEGORIA // ${knowledge.category}", color = Acid, style = MaterialTheme.typography.labelSmall)
            if (knowledge.description.isNotBlank()) Text(knowledge.description, color = Muted, style = MaterialTheme.typography.bodySmall)
            if (knowledge.prerequisites.isNotEmpty()) Text("PRÉ-REQUISITOS // ${knowledge.prerequisites.joinToString("; ")}", color = Muted, style = MaterialTheme.typography.bodySmall)
            if (knowledge.mechanicalEffect.isNotBlank()) Text("EFEITO // ${knowledge.mechanicalEffect}", color = Ice, style = MaterialTheme.typography.bodySmall)
            if (knowledge.ruleReference.isNotBlank()) Text("REF. // ${knowledge.ruleReference}", color = Muted, style = MaterialTheme.typography.labelSmall)
            if (knowledge.name.isNotBlank() && totalValue(knowledge.name) != knowledge.value + knowledge.adjustment) {
                Text("TOTAL EQUIPADO // ${totalValue(knowledge.name)}", color = Acid, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    AddButton("Selecionar do catálogo", enabled && options.isNotEmpty()) { selecting = true }
    AddButton("Adicionar manualmente", enabled) { onValues(values + SpecialKnowledge()) }
    if (selecting) {
        CatalogPickerDialog(
            title = "SELECIONAR // ${title.uppercase()}",
            entries = options,
            onDismiss = { selecting = false },
            alreadyAddedCatalogIds = values.mapNotNull { it.catalogEntryId.takeIf(String::isNotBlank) }.toSet(),
            onSelect = { entry ->
                if (values.canAddCatalogEntry(entry)) onValues(values + entry.toSpecialKnowledge())
                selecting = false
            },
        )
    }
}

@Composable
internal fun PhaseOnePathSection(
    character: Character,
    catalog: List<CatalogEntry>,
    enabled: Boolean,
    onChange: (Character) -> Unit,
) {
    var selecting by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<CatalogEntry?>(null) }
    TechPanel(accent = Signal) {
        SectionHeader("07", "Caminho")
        HudTextField("Nome do Caminho", character.pathName, enabled = enabled) { onChange(character.copy(pathName = it)) }
        AddButton("Preencher pelo catálogo", enabled && catalog.isNotEmpty()) { selecting = true }
        HudTextField("Lema", character.pathMotto, enabled = enabled) { onChange(character.copy(pathMotto = it)) }
        Text("PALAVRAS-CHAVE // 3", color = LabelFunctional, style = MaterialTheme.typography.labelLarge)
        character.pathKeywords.forEachIndexed { index, keyword ->
            HudTextField("Palavra-chave ${index + 1}", keyword, enabled = enabled) {
                onChange(character.copy(pathKeywords = character.pathKeywords.replace(index, it)))
            }
        }
        Text("PILARES // 3", color = LabelFunctional, style = MaterialTheme.typography.labelLarge)
        character.pathPillars.forEachIndexed { index, pillar ->
            HudTextField("Pilar ${index + 1}", pillar, multiline = true, enabled = enabled) {
                onChange(character.copy(pathPillars = character.pathPillars.replace(index, it)))
            }
        }
    }
    if (selecting) {
        CatalogPickerDialog("SELECIONAR CAMINHO", catalog, { selecting = false }) { entry ->
            pending = entry
            selecting = false
        }
    }
    pending?.let { entry ->
        val preview = character.previewPathChange(entry)
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("TROCAR CAMINHO // ${entry.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SERÃO REMOVIDOS // ${preview.removed.size}", color = Signal)
                    preview.removed.forEach { Text("− ${it.name}", color = Muted) }
                    Text("SERÃO ADICIONADOS // ${preview.added.size}", color = Acid)
                    preview.added.forEach { Text("+ ${it.name}", color = Ice) }
                    Text("Poderes raciais, de itens, Conhecimentos, recompensas narrativas e registros manuais são preservados.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onChange(character.withStructuredPathPreset(entry))
                    pending = null
                }) { Text("CONFIRMAR") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("CANCELAR") } },
        )
    }
}

@Composable
internal fun PhaseOnePowerSection(
    character: Character,
    catalog: List<CatalogEntry>,
    enabled: Boolean,
    onChange: (Character) -> Unit,
) {
    var selecting by remember { mutableStateOf(false) }
    TechPanel(accent = Acid) {
        SectionHeader("08", "Poderes")
        Text("REGISTROS // ${character.powers.size}", color = Acid, style = MaterialTheme.typography.labelLarge)
        character.powers.forEachIndexed { index, power ->
            StructuredPowerEditor(
                index = index,
                power = power,
                enabled = enabled,
                onRemove = { onChange(character.copy(powers = character.powers.filterIndexed { itemIndex, _ -> itemIndex != index })) },
                onValue = { onChange(character.copy(powers = character.powers.replace(index, it))) },
            )
        }
        AddButton("Selecionar poder do catálogo", enabled && catalog.isNotEmpty()) { selecting = true }
        AddButton("Adicionar poder manualmente", enabled) {
            onChange(character.copy(powers = character.powers + Power(sourceType = PowerSourceType.MANUAL)))
        }
    }
    if (selecting) {
        CatalogPickerDialog(
            title = "SELECIONAR PODER",
            entries = catalog,
            onDismiss = { selecting = false },
            alreadyAddedCatalogIds = character.powers.mapNotNull { it.catalogEntryId.takeIf(String::isNotBlank) }.toSet(),
            onSelect = { entry ->
                if (character.powers.none { it.catalogEntryId == entry.id } || entry.repeatable) {
                    onChange(character.copy(powers = character.powers + entry.toStructuredPower()))
                }
                selecting = false
            },
        )
    }
}

@Composable
private fun StructuredPowerEditor(
    index: Int,
    power: Power,
    enabled: Boolean,
    onRemove: () -> Unit,
    onValue: (Power) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(ArcanePanel).padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("PODER ${(index + 1).toString().padStart(2, '0')}", color = Ice, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(power.sourceType.name, color = Acid, style = MaterialTheme.typography.labelSmall)
            RemoveButton(enabled, "Remover poder", onRemove)
        }
        HudTextField("Nome", power.name, enabled = enabled) { onValue(power.copy(name = it)) }
        HudTextField("Caminho / origem", power.origin, multiline = true, enabled = enabled) { onValue(power.copy(origin = it)) }
        HudTextField("Categoria", power.category, enabled = enabled) { onValue(power.copy(category = it)) }
        TwoFields(
            { HudTextField("Custo", power.cost, it, enabled = enabled) { value -> onValue(power.copy(cost = value)) } },
            { HudTextField("Tipo de ação", power.action, it, enabled = enabled) { value -> onValue(power.copy(action = value)) } },
        )
        TwoFields(
            { HudTextField("Alcance", power.range, it, enabled = enabled) { value -> onValue(power.copy(range = value)) } },
            { HudTextField("Duração", power.duration, it, enabled = enabled) { value -> onValue(power.copy(duration = value)) } },
        )
        HudTextField("Limite de uso", power.limit, enabled = enabled) { onValue(power.copy(limit = it)) }
        AbilityUsageEditor(power.favorite, power.available, power.costResource, power.costAmount, power.usage, enabled) { favorite, available, resource, amount, usage ->
            onValue(power.copy(favorite = favorite, available = available, costResource = resource, costAmount = amount, usage = usage))
        }
        HudTextField("Pré-requisitos", power.prerequisites.joinToString("; "), multiline = true, enabled = enabled) {
            onValue(power.copy(prerequisites = it.split(';').map(String::trim).filter(String::isNotBlank)))
        }
        HudTextField("Condição de ativação", power.activationCondition, multiline = true, enabled = enabled) { onValue(power.copy(activationCondition = it)) }
        HudTextField("Efeito principal", power.effect, multiline = true, enabled = enabled) { onValue(power.copy(effect = it)) }
        HudTextField("Aprimoramentos", power.enhancements, multiline = true, enabled = enabled) { onValue(power.copy(enhancements = it)) }
        HudTextField("Perda / desativação", power.deactivationCondition, multiline = true, enabled = enabled) { onValue(power.copy(deactivationCondition = it)) }
        HudTextField("Referência nas regras", power.ruleReference, enabled = enabled) { onValue(power.copy(ruleReference = it)) }
    }
}

@Composable
internal fun PhaseOneInventorySection(
    character: Character,
    catalog: List<CatalogEntry>,
    enabled: Boolean,
    onChange: (Character) -> Unit,
) {
    val acquiredTargets = character.learnedKnowledges.map(SpecialKnowledge::name).filter(String::isNotBlank).distinct()
    CompositionLocalProvider(LocalAcquiredKnowledgeTargets provides acquiredTargets) {
        InventorySection(character, catalog, enabled, onChange)
    }
}
