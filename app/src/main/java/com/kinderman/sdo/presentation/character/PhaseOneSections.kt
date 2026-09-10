package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.catalog.canAddCatalogEntry
import com.kinderman.sdo.domain.catalog.previewPathChange
import com.kinderman.sdo.domain.catalog.toSpecialKnowledge
import com.kinderman.sdo.domain.catalog.toStructuredPower
import com.kinderman.sdo.domain.catalog.withStructuredPathPreset
import com.kinderman.sdo.domain.catalog.withKnowledgeLevel
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.PowerSourceType
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.AbilityCostType
import com.kinderman.sdo.domain.model.AbilityDuration
import com.kinderman.sdo.domain.model.AbilityExecution
import com.kinderman.sdo.domain.model.AbilityModifier
import com.kinderman.sdo.domain.model.AbilityModifierTarget
import com.kinderman.sdo.domain.model.AbilityRange
import com.kinderman.sdo.domain.model.AbilityResistance
import com.kinderman.sdo.domain.model.AbilitySource
import com.kinderman.sdo.domain.model.AbilityTimeUnit
import com.kinderman.sdo.domain.model.formattedAbilityCost
import com.kinderman.sdo.domain.model.formattedAbilityExecution
import com.kinderman.sdo.domain.model.withAddedPower
import com.kinderman.sdo.domain.model.withUpdatedPower
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
            attributeOptions = character.attributes.map { it.acronym to it.name },
            onLevelChange = { knowledge, level -> onChange(character.withKnowledgeLevel(knowledge.id, level, catalog)) },
        ) { onChange(character.copy(learnedKnowledges = it)) }
        PhaseOneKnowledgeList(
            title = "Conhecimentos arcanos",
            kind = CatalogKind.ARCANE_KNOWLEDGE,
            values = character.arcaneKnowledges,
            catalog = catalog,
            enabled = enabled,
            totalValue = character::acquiredKnowledgeValue,
            attributeOptions = character.attributes.map { it.acronym to it.name },
            onLevelChange = { knowledge, level -> onChange(character.withKnowledgeLevel(knowledge.id, level, catalog)) },
        ) { onChange(character.copy(arcaneKnowledges = it)) }
        PhaseOneKnowledgeList(
            title = "Técnicas de batalha",
            kind = CatalogKind.BATTLE_TECHNIQUE,
            values = character.battleTechniques,
            catalog = catalog,
            enabled = enabled,
            totalValue = character::acquiredKnowledgeValue,
            attributeOptions = character.attributes.map { it.acronym to it.name },
            onLevelChange = { knowledge, level -> onChange(character.withKnowledgeLevel(knowledge.id, level, catalog)) },
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
    attributeOptions: List<Pair<String, String>>,
    onLevelChange: (SpecialKnowledge, Int) -> Unit,
    onValues: (List<SpecialKnowledge>) -> Unit,
) {
    var selecting by remember { mutableStateOf(false) }
    var expandedKnowledgeId by remember { mutableStateOf<String?>(null) }
    val options = remember(catalog, kind) { catalog.filter { it.kind == kind } }
    Text(title.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    values.forEachIndexed { index, knowledge ->
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(knowledge.name.ifBlank { "REG.${(index + 1).toString().padStart(2, '0')}" }, color = MaterialTheme.colorScheme.onSurface)
                    val adjustedLevel = (knowledge.value + knowledge.adjustment).coerceAtLeast(0)
                    val modifier = knowledge.adjustment.takeUnless { it == 0 }?.let { if (it > 0) " // MOD +$it" else " // MOD $it" }.orEmpty()
                    Text("${knowledge.attribute.ifBlank { "SEM ATRIBUTO" }} // NÍVEL $adjustedLevel$modifier", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = { expandedKnowledgeId = knowledge.id.takeUnless { it == expandedKnowledgeId } }) {
                    Text(if (expandedKnowledgeId == knowledge.id) "FECHAR" else "EDITAR")
                }
                RemoveButton(enabled, "Remover conhecimento") {
                    onValues(values.filterIndexed { itemIndex, _ -> itemIndex != index })
                }
            }
            if (expandedKnowledgeId != knowledge.id) return@Column
            HudTextField("Nome", knowledge.name, enabled = enabled) { onValues(values.replace(index, knowledge.copy(name = it))) }
            val selectedAttribute = knowledge.attribute.takeIf { current -> attributeOptions.any { it.first == current } }
                ?: attributeOptions.firstOrNull()?.first.orEmpty()
            ChoiceField("Atributo", selectedAttribute, attributeOptions.map { it.first }, enabled,
                display = { acronym -> attributeOptions.firstOrNull { option -> option.first == acronym }?.let { option -> "${option.first} — ${option.second}" }.orEmpty() }) { value ->
                onValues(values.replace(index, knowledge.copy(attribute = value)))
            }
            TwoFields(
                { IntegerField("Valor (0–5)", knowledge.value, enabled, it) { value -> onLevelChange(knowledge, value) } },
                { IntegerField("Modificador", knowledge.adjustment, enabled, it) { value ->
                    onValues(values.replace(index, knowledge.copy(adjustment = value)))
                } },
            )
            if (knowledge.category.isNotBlank()) Text("CATEGORIA // ${knowledge.category}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            val usefulDescription = knowledge.mechanicalEffect.ifBlank { knowledge.description }
            if (usefulDescription.isNotBlank()) Text(usefulDescription, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
            if (knowledge.name.isNotBlank() && totalValue(knowledge.name) != knowledge.value + knowledge.adjustment) {
                Text("TOTAL EQUIPADO // ${totalValue(knowledge.name)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    AddButton("Selecionar do catálogo", enabled && options.isNotEmpty()) { selecting = true }
    AddButton("Adicionar manualmente", enabled) {
        val knowledge = SpecialKnowledge(attribute = attributeOptions.firstOrNull()?.first.orEmpty())
        expandedKnowledgeId = knowledge.id
        onValues(values + knowledge)
    }
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
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        SectionHeader("07", "Caminho")
        HudTextField("Nome do Caminho", character.pathName, enabled = enabled) { onChange(character.copy(pathName = it)) }
        AddButton("Preencher pelo catálogo", enabled && catalog.isNotEmpty()) { selecting = true }
        HudTextField("Lema", character.pathMotto, enabled = enabled) { onChange(character.copy(pathMotto = it)) }
        Text("PALAVRAS-CHAVE // 3", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        character.pathKeywords.forEachIndexed { index, keyword ->
            HudTextField("Palavra-chave ${index + 1}", keyword, enabled = enabled) {
                onChange(character.copy(pathKeywords = character.pathKeywords.replace(index, it)))
            }
        }
        Text("PILARES // 3", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
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
                    Text("SERÃO REMOVIDOS // ${preview.removed.size}", color = MaterialTheme.colorScheme.error)
                    preview.removed.forEach { Text("− ${it.name}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("SERÃO ADICIONADOS // ${preview.added.size}", color = MaterialTheme.colorScheme.primary)
                    preview.added.forEach { Text("+ ${it.name}", color = MaterialTheme.colorScheme.onSurface) }
                    Text("Poderes raciais, de itens, Conhecimentos, recompensas narrativas e registros manuais são preservados.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
    var expandedPowerId by remember(character.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    fun applyPowerChange(block: () -> Character) {
        runCatching(block).onSuccess(onChange).onFailure { android.widget.Toast.makeText(context, it.message, android.widget.Toast.LENGTH_SHORT).show() }
    }
    TechPanel(accent = MaterialTheme.colorScheme.primary) {
        SectionHeader("08", "Poderes")
        Text("REGISTROS // ${character.powers.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        if (character.powers.isEmpty()) {
            Text(
                "Nenhum poder registrado. Selecione um padrão do catálogo ou crie um registro manual.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        character.powers.forEachIndexed { index, power ->
            StructuredPowerEditor(
                index = index,
                power = power,
                character = character,
                enabled = enabled,
                expanded = expandedPowerId == power.id,
                onToggle = { expandedPowerId = power.id.takeUnless { it == expandedPowerId } },
                onRemove = {
                    if (expandedPowerId == power.id) expandedPowerId = null
                    onChange(character.copy(powers = character.powers.filterNot { it.id == power.id }))
                },
                onValue = { value -> applyPowerChange { character.withUpdatedPower(value.copy(revision = power.revision + 1)) } },
            )
        }
        AddButton("Selecionar poder do catálogo", enabled && catalog.isNotEmpty()) { selecting = true }
        AddButton("Adicionar poder manualmente", enabled) {
            val power = Power(sourceType = PowerSourceType.MANUAL)
            expandedPowerId = power.id
            applyPowerChange { character.withAddedPower(power) }
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
                    val power = entry.toStructuredPower()
                    expandedPowerId = power.id
                    applyPowerChange { character.withAddedPower(power) }
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
    character: Character,
    enabled: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onValue: (Power) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    power.name.ifBlank { "PODER ${(index + 1).toString().padStart(2, '0')}" },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    listOf(
                        power.canonicalSource.label,
                        formattedAbilityExecution(power.executionType, power.timeValue, power.timeUnit),
                        formattedAbilityCost(power.costType, power.costValue),
                    ).joinToString(" // "),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            TextButton(onClick = onToggle) { Text(if (expanded) "FECHAR" else "EDITAR") }
            RemoveButton(enabled, "Remover poder", onRemove)
        }
        if (!expanded) {
            Text(
                power.effect.ifBlank { "Sem efeito descrito." },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
            )
            return@Column
        }
        HudTextField("Nome", power.name, enabled = enabled) { onValue(power.copy(name = it)) }
        val knowledges = character.learnedKnowledges + character.arcaneKnowledges + character.battleTechniques
        val sourceOptions = AbilitySource.entries.filterNot { it == AbilitySource.KNOWLEDGE && knowledges.isEmpty() }
        ChoiceField("Fonte", power.canonicalSource, sourceOptions, enabled, display = { it.label }) { source ->
            val selectedKnowledge = knowledges.firstOrNull()
            val selectedItem = character.inventory.firstOrNull { it.linkedAshId.isBlank() }
            onValue(power.copy(
                canonicalSource = source,
                knowledgeId = selectedKnowledge?.id.orEmpty().takeIf { source == AbilitySource.KNOWLEDGE }.orEmpty(),
                knowledgeLevel = if (source == AbilitySource.KNOWLEDGE) 0 else null,
                linkedItemId = selectedItem?.id.orEmpty().takeIf { source == AbilitySource.ITEM }.orEmpty(),
                active = false,
            ))
        }
        if (power.canonicalSource == AbilitySource.KNOWLEDGE) {
            if (knowledges.isEmpty()) Text("Adicione um Conhecimento à ficha antes de selecionar esta fonte.", color = MaterialTheme.colorScheme.error)
            else {
                val selected = knowledges.firstOrNull { it.id == power.knowledgeId } ?: knowledges.first()
                ChoiceField("Conhecimento", selected, knowledges, enabled, display = { it.name }) {
                    onValue(power.copy(knowledgeId = it.id, knowledgeLevel = (power.knowledgeLevel ?: 0).coerceIn(0, it.value)))
                }
                ChoiceField("Nível", (power.knowledgeLevel ?: 0).coerceIn(0, selected.value), (0..selected.value).toList(), enabled) {
                    onValue(power.copy(knowledgeId = selected.id, knowledgeLevel = it))
                }
            }
        }
        if (power.canonicalSource == AbilitySource.ITEM) {
            val items = character.inventory.filter { it.linkedAshId.isBlank() }
            if (items.isEmpty()) Text("Adicione um item ao inventário antes de vincular o poder.", color = MaterialTheme.colorScheme.error)
            else ChoiceField("Item", power.linkedItemId.takeIf { id -> items.any { it.id == id } } ?: items.first().id, items.map { it.id }, enabled,
                display = { id -> items.firstOrNull { it.id == id }?.name.orEmpty() }) { onValue(power.copy(linkedItemId = it, active = false)) }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = power.destinyCostEligible,
                onCheckedChange = { eligible ->
                    onValue(power.copy(
                        destinyCostEligible = eligible,
                        costType = if (!eligible && power.costType == AbilityCostType.DESTINY) AbilityCostType.ENERGY else power.costType,
                    ))
                },
                enabled = enabled,
            )
            Text(
                "Poder divino ou ligado a sorte, Destino, probabilidades ou porcentagens (permite PD)",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val costOptions = buildList {
            add(AbilityCostType.ENERGY)
            add(AbilityCostType.LIFE)
            add(AbilityCostType.SANITY)
            if (power.destinyCostEligible) add(AbilityCostType.DESTINY)
        }
        TwoFields(
            { ChoiceField("Custo", power.costType.takeIf { it in costOptions } ?: AbilityCostType.ENERGY, costOptions, enabled && power.executionType != AbilityExecution.PASSIVE, it, display = { value -> value.label }) { value -> onValue(power.copy(costType = value)) } },
            { IntegerField("Valor do custo", if (power.executionType == AbilityExecution.PASSIVE) 0 else power.costValue, enabled && power.executionType != AbilityExecution.PASSIVE, it) { value -> onValue(power.copy(costValue = value.coerceAtLeast(1))) } },
        )
        TwoFields(
            { ChoiceField("Execução", power.executionType, AbilityExecution.entries, enabled, it, display = { value -> value.label }) { value -> onValue(power.copy(executionType = value, action = value.label, costType = if (value == AbilityExecution.PASSIVE) AbilityCostType.ENERGY else power.costType.takeIf { it in costOptions } ?: AbilityCostType.ENERGY, costValue = if (value == AbilityExecution.PASSIVE) 0 else power.costValue.coerceAtLeast(1))) } },
            { ChoiceField("Alcance", power.rangeType, AbilityRange.entries, enabled, it, display = { value -> value.label }) { value -> onValue(power.copy(rangeType = value, range = value.label)) } },
        )
        if (power.executionType == AbilityExecution.TIME) TwoFields(
            { IntegerField("Tempo de execução", power.timeValue, enabled, it) { value -> onValue(power.copy(timeValue = value.coerceAtLeast(0))) } },
            { ChoiceField("Unidade", power.timeUnit, AbilityTimeUnit.entries, enabled, it, display = { value -> value.label }) { value -> onValue(power.copy(timeUnit = value)) } },
        )
        HudTextField("Alvo / Área (opcional)", power.targetArea, enabled = enabled) { onValue(power.copy(targetArea = it)) }
        if (power.executionType != AbilityExecution.PASSIVE) {
            TwoFields(
                { ChoiceField("Duração", power.durationType, AbilityDuration.entries, enabled, it, display = { value -> value.label }) { value -> onValue(power.copy(durationType = value, duration = value.label)) } },
                { ChoiceField("Resistência", power.resistance, AbilityResistance.entries, enabled, it, display = { value -> value.label }) { value -> onValue(power.copy(resistance = value)) } },
            )
            if (power.durationType == AbilityDuration.TURNS) {
                IntegerField("Quantidade de turnos", power.durationValue, enabled) { value -> onValue(power.copy(durationValue = value.coerceAtLeast(0))) }
            }
            if (power.durationType == AbilityDuration.TIME) TwoFields(
                { IntegerField("Tempo de duração", power.durationValue, enabled, it) { value -> onValue(power.copy(durationValue = value.coerceAtLeast(0))) } },
                { ChoiceField("Unidade da duração", power.durationUnit, listOf(AbilityTimeUnit.HOURS, AbilityTimeUnit.DAYS), enabled, it, display = { value -> value.label }) { value -> onValue(power.copy(durationUnit = value)) } },
            )
        }
        AbilityAvailabilityEditor(power.favorite, power.available, enabled) { favorite, available ->
            onValue(power.copy(favorite = favorite, available = available))
        }
        if (power.executionType == AbilityExecution.PASSIVE) {
            Row(Modifier.fillMaxWidth()) {
                Checkbox(power.grantsPermanentBonus, { onValue(power.copy(grantsPermanentBonus = it)) }, enabled = enabled)
                Text("Concede bônus permanente", modifier = Modifier.weight(1f))
                if (power.canonicalSource != AbilitySource.ITEM) Switch(power.active, { onValue(power.copy(active = it)) }, enabled = enabled)
            }
            if (power.grantsPermanentBonus) {
                power.modifiers.forEachIndexed { modifierIndex, modifier ->
                    PowerModifierEditor(character, modifier, enabled,
                        onRemove = { onValue(power.copy(modifiers = power.modifiers.filterIndexed { i, _ -> i != modifierIndex })) },
                        onValue = { onValue(power.copy(modifiers = power.modifiers.replace(modifierIndex, it))) })
                }
                AddButton("Adicionar modificador", enabled) { onValue(power.copy(modifiers = power.modifiers + AbilityModifier())) }
            } else HudTextField("Efeito", power.effect, multiline = true, enabled = enabled) { onValue(power.copy(effect = it)) }
        } else HudTextField("Efeito", power.effect, multiline = true, enabled = enabled) { onValue(power.copy(effect = it)) }
    }
}

@Composable
private fun PowerModifierEditor(character: Character, modifier: AbilityModifier, enabled: Boolean, onRemove: () -> Unit, onValue: (AbilityModifier) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            ChoiceField("Alvo", modifier.targetType, AbilityModifierTarget.entries, enabled, Modifier.weight(1f), display = { it.label }) {
                onValue(modifier.copy(targetType = it, targetId = ""))
            }
            RemoveButton(enabled, "Remover modificador", onRemove)
        }
        val targets = when (modifier.targetType) {
            AbilityModifierTarget.ATTRIBUTE -> character.attributes.map { it.acronym to it.name }
            AbilityModifierTarget.KNOWLEDGE -> (character.learnedKnowledges + character.arcaneKnowledges + character.battleTechniques).map { it.id to it.name }
            AbilityModifierTarget.RESOURCE_MAXIMUM -> listOf("LIFE" to "Vida", "SANITY" to "Sanidade", "ARCANE" to "Arcano", "ENERGY" to "Energia", "DESTINY" to "Destino")
            AbilityModifierTarget.PROTECTION -> listOf("Geral", "Esquiva", "Postura", "Mental", "Arcana").map { it to it }
        }
        if (targets.isNotEmpty()) {
            val targetId = modifier.targetId.takeIf { id -> targets.any { it.first == id } } ?: targets.first().first
            ChoiceField("Aplicar em", targetId, targets.map { it.first }, enabled, display = { id -> targets.first { it.first == id }.second }) {
                onValue(modifier.copy(targetId = it))
            }
        }
        IntegerField("Modificador", modifier.value, enabled) { onValue(modifier.copy(value = it)) }
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
