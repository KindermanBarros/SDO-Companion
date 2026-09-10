package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.catalog.withRaceSelection
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.AcidCyan
import com.kinderman.sdo.ui.AcidMagenta
import com.kinderman.sdo.ui.AuraBlue
import com.kinderman.sdo.ui.Carbon
import com.kinderman.sdo.ui.EnergyBlue
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.LabelFunctional
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.NeonCoral
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.StatHeader
import com.kinderman.sdo.ui.StatHeaderLight
import com.kinderman.sdo.ui.TechCutDark
import com.kinderman.sdo.ui.TechPanel

@Composable
internal fun IdentitySection(character: Character, catalog: List<CatalogEntry>, enabled: Boolean, onChange: (Character) -> Unit) {
    var selectingRace by remember { mutableStateOf(false) }
    var selectingLevel by remember { mutableStateOf(false) }
    TechPanel {
        SectionHeader("01", "Identidade")
        HudTextField("Nome", character.name, enabled = enabled) { onChange(character.copy(name = it)) }
        Text("RAÇA // ${character.race.ifBlank { "NÃO SELECIONADA" }}", color = MaterialTheme.colorScheme.onSurface)
        Text("SUB-RAÇA // ${character.subRace.ifBlank { "NENHUMA" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        AddButton("Selecionar raça, sub-raça e poderes", enabled) { selectingRace = true }
        HudTextField("Ocupação", character.occupation, enabled = enabled) { onChange(character.copy(occupation = it)) }
        TwoFields(
            { HudTextField("Altura", character.height, it, enabled = enabled) { value -> onChange(character.copy(height = value)) } },
            { HudTextField("Idade", character.age, it, enabled = enabled) { value -> onChange(character.copy(age = value)) } },
        )
        HudTextField("Sexo", character.sex, enabled = enabled) { value -> onChange(character.copy(sex = value)) }
        TwoFields(
            { AddButton("Nível ${character.level} — alterar", enabled) { selectingLevel = true } },
            { IntegerField("Dinheiro (E$)", character.money, enabled, it) { value -> onChange(character.copy(money = value)) } },
        )
    }
    if (selectingRace) RacePickerDialog(character, { selectingRace = false }) { race, subRace, attribute, basePowers, subRacePower ->
        onChange(character.withRaceSelection(race, subRace, attribute, basePowers, subRacePower))
        selectingRace = false
    }
    if (selectingLevel) LevelProgressionDialog(character, catalog, { selectingLevel = false }) {
        onChange(it)
        selectingLevel = false
    }
}

@Composable
internal fun ResourceSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = MaterialTheme.colorScheme.primary) {
        SectionHeader("02", "Recursos")
        CalculatedResourceEditor(
            label = "VIDA",
            formula = "10 + VITALIDADE",
            resource = character.life,
            base = character.lifeBase,
            maximum = character.lifeMaximum,
            accent = MaterialTheme.colorScheme.secondary,
            enabled = enabled,
        ) { onChange(character.copy(life = it)) }
        CalculatedResourceEditor(
            label = "SANIDADE",
            formula = "10 + SANIDADE",
            resource = character.sanity,
            base = character.sanityBase,
            maximum = character.sanityMaximum,
            accent = MaterialTheme.colorScheme.secondary,
            enabled = enabled,
        ) { onChange(character.copy(sanity = it)) }
        CalculatedResourceEditor(
            label = "ARCANO",
            formula = "POD + ARCANO",
            resource = character.arcane,
            base = character.arcaneBase,
            maximum = character.arcaneMaximum,
            accent = MaterialTheme.colorScheme.secondary,
            enabled = enabled,
        ) { onChange(character.copy(arcane = it)) }
        CalculatedResourceEditor(
            label = "ENERGIA",
            formula = "VIG + ENERGIA",
            resource = character.energy,
            base = character.energyBase,
            maximum = character.energyMaximum,
            accent = MaterialTheme.colorScheme.secondary,
            enabled = enabled,
        ) { onChange(character.copy(energy = it)) }
        ManualResourceEditor("DESTINO", character.destiny, MaterialTheme.colorScheme.secondary, enabled) { onChange(character.copy(destiny = it)) }
        ManualResourceEditor("EXAUSTÃO", character.exhaustion, MaterialTheme.colorScheme.error, enabled) { onChange(character.copy(exhaustion = it)) }
        ManualResourceEditor("CORRUPÇÃO DIVINA (%)", character.corruption, MaterialTheme.colorScheme.tertiary, enabled) { onChange(character.copy(corruption = it)) }
    }
}

@Composable
private fun CalculatedResourceEditor(
    label: String,
    formula: String,
    resource: ResourceValue,
    base: Int,
    maximum: Int,
    accent: Color,
    enabled: Boolean,
    onValue: (ResourceValue) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().border(1.dp, accent, CutCornerShape(topEnd = 12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Text("MÁXIMO $maximum", color = accent, style = MaterialTheme.typography.titleLarge)
        }
        Text("CÁLCULO // $formula = $base", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        IntegerField("Atual", resource.current, enabled) { value ->
            onValue(resource.copy(current = value.coerceAtLeast(0)))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                enabled = enabled && resource.adjustment > -base,
                onClick = { onValue(resource.copy(adjustment = resource.adjustment - 1)) },
            ) {
                Icon(Icons.Default.Remove, "Diminuir ajuste de $label", tint = accent)
            }
            IntegerField(
                label = "Ajuste (+/-)",
                value = resource.adjustment,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) { value ->
                onValue(resource.copy(adjustment = value.coerceAtLeast(-base)))
            }
            IconButton(
                enabled = enabled,
                onClick = { onValue(resource.copy(adjustment = resource.adjustment + 1)) },
            ) {
                Icon(Icons.Default.Add, "Aumentar ajuste de $label", tint = accent)
            }
        }
        val adjustmentLabel = if (resource.adjustment >= 0) "+${resource.adjustment}" else resource.adjustment.toString()
        Text(
            "BASE $base // AJUSTE $adjustmentLabel // TOTAL $maximum",
            color = accent,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ManualResourceEditor(label: String, resource: ResourceValue, accent: Color, enabled: Boolean, onValue: (ResourceValue) -> Unit) {
    Column(
        Modifier.fillMaxWidth().border(1.dp, accent, CutCornerShape(topEnd = 12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        TwoFields(
            { IntegerField("Atual", resource.current, enabled, it) { value -> onValue(resource.copy(current = value)) } },
            { IntegerField("Máximo", resource.maximum, enabled, it) { value -> onValue(resource.copy(maximum = value.coerceAtLeast(0))) } },
        )
    }
}

@Composable
internal fun TraitSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        SectionHeader("03", "Traços")
        TraitList("Positivos", character.positiveTraits, enabled) { onChange(character.copy(positiveTraits = it)) }
        TraitList("Negativos", character.negativeTraits, enabled) { onChange(character.copy(negativeTraits = it)) }
    }
}

@Composable
private fun TraitList(title: String, values: List<String>, enabled: Boolean, onValues: (List<String>) -> Unit) {
    Text(title.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
    values.forEachIndexed { index, value ->
        Row(Modifier.fillMaxWidth()) {
            HudTextField("Traço ${index + 1}", value, Modifier.weight(1f), enabled = enabled) { onValues(values.replace(index, it)) }
            RemoveButton(enabled, "Remover traço") { onValues(values.filterIndexed { itemIndex, _ -> itemIndex != index }) }
        }
    }
    AddButton("Adicionar traço", enabled) { onValues(values + "") }
}

@Composable
internal fun AttributeSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = MaterialTheme.colorScheme.outlineVariant) {
        SectionHeader("04", "Atributos e conhecimentos")
        character.attributes.forEachIndexed { attributeIndex, attribute ->
            AttributeEditor(character, attribute, enabled) { updated -> onChange(character.copy(attributes = character.attributes.replace(attributeIndex, updated))) }
            if (attributeIndex != character.attributes.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun AttributeEditor(character: Character, attribute: AttributeValue, enabled: Boolean, onValue: (AttributeValue) -> Unit) {
    Text("${attribute.acronym} // ${attribute.name.uppercase()}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
    TwoFields(
        { IntegerField("Valor", attribute.value, enabled, it) { value -> onValue(attribute.copy(value = value)) } },
        { IntegerField("Modificador", attribute.modifier, enabled, it) { value -> onValue(attribute.copy(modifier = value)) } },
    )
    if (character.attributeTotal(attribute.acronym) != attribute.value) {
        Text("TOTAL EQUIPADO // ${character.attributeTotal(attribute.acronym)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
    }
    attribute.skills.forEachIndexed { index, skill ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(skill.name.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f).padding(top = 18.dp))
            IntegerField("Valor", skill.value, enabled, Modifier.weight(1f)) { value -> onValue(attribute.copy(skills = attribute.skills.replace(index, skill.copy(value = value)))) }
            IntegerField("Mod.", skill.modifier, enabled, Modifier.weight(1f)) { value -> onValue(attribute.copy(skills = attribute.skills.replace(index, skill.copy(modifier = value)))) }
        }
        if (character.basicKnowledgeTotal(attribute.acronym, skill.name) != skill.value) {
            Text("${skill.name.uppercase()} EQUIPADO // ${character.basicKnowledgeTotal(attribute.acronym, skill.name)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun SpecialKnowledgeSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel {
        SectionHeader("05", "Conhecimentos especiais")
        KnowledgeList("Aprendidos", character.learnedKnowledges, enabled, character::acquiredKnowledgeValue) { onChange(character.copy(learnedKnowledges = it)) }
        KnowledgeList("Arcanos", character.arcaneKnowledges, enabled, character::acquiredKnowledgeValue) { onChange(character.copy(arcaneKnowledges = it)) }
        KnowledgeList("Técnicas de batalha", character.battleTechniques, enabled, character::acquiredKnowledgeValue) { onChange(character.copy(battleTechniques = it)) }
    }
}

@Composable
private fun KnowledgeList(
    title: String,
    values: List<SpecialKnowledge>,
    enabled: Boolean,
    totalValue: (String) -> Int,
    onValues: (List<SpecialKnowledge>) -> Unit,
) {
    Text(title.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    values.forEachIndexed { index, knowledge ->
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("REG.${(index + 1).toString().padStart(2, '0')}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                RemoveButton(enabled, "Remover conhecimento") { onValues(values.filterIndexed { itemIndex, _ -> itemIndex != index }) }
            }
            HudTextField("Nome", knowledge.name, enabled = enabled) { onValues(values.replace(index, knowledge.copy(name = it))) }
            TwoFields(
                { HudTextField("Atributo", knowledge.attribute, it, enabled = enabled) { value -> onValues(values.replace(index, knowledge.copy(attribute = value.uppercase()))) } },
                { IntegerField("Valor", knowledge.value, enabled, it) { value -> onValues(values.replace(index, knowledge.copy(value = value))) } },
            )
            if (knowledge.name.isNotBlank() && totalValue(knowledge.name) != knowledge.value) {
                Text("TOTAL EQUIPADO // ${totalValue(knowledge.name)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    AddButton("Adicionar $title", enabled) { onValues(values + SpecialKnowledge()) }
}

@Composable
internal fun ProtectionSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = MaterialTheme.colorScheme.secondary) {
        SectionHeader("06", "Proteções")
        val formulas = linkedMapOf(
            "Geral" to "10 + PG DOS EQUIPAMENTOS",
            "Esquiva" to "PG + AGI + REFLEXOS",
            "Postura" to "10 + CAR + LÁBIA",
            "Mental" to "10 + INT + SANIDADE",
            "Arcana" to "10 + POD + ARCANO",
        )
        formulas.forEach { (name, formula) ->
            ProtectionEditor(
                name = name,
                formula = formula,
                base = character.protectionBase(name),
                adjustment = character.protectionAdjustments[name] ?: 0,
                total = character.protectionTotal(name),
                enabled = enabled,
            ) { updatedAdjustment ->
                onChange(
                    character.copy(
                        protectionAdjustments = character.protectionAdjustments + (name to updatedAdjustment),
                    ),
                )
            }
        }
    }
}

@Composable
private fun ProtectionEditor(
    name: String,
    formula: String,
    base: Int,
    adjustment: Int,
    total: Int,
    enabled: Boolean,
    onAdjustment: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.secondary, CutCornerShape(topEnd = 12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Text("TOTAL $total", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleLarge)
        }
        Text("CÁLCULO // $formula = $base", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                enabled = enabled && adjustment > -base,
                onClick = { onAdjustment(adjustment - 1) },
            ) {
                Icon(Icons.Default.Remove, "Diminuir ajuste de $name", tint = MaterialTheme.colorScheme.secondary)
            }
            IntegerField(
                label = "Ajuste (+/-)",
                value = adjustment,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) { value ->
                onAdjustment(value.coerceAtLeast(-base))
            }
            IconButton(
                enabled = enabled,
                onClick = { onAdjustment(adjustment + 1) },
            ) {
                Icon(Icons.Default.Add, "Aumentar ajuste de $name", tint = MaterialTheme.colorScheme.secondary)
            }
        }
        val adjustmentLabel = if (adjustment >= 0) "+$adjustment" else adjustment.toString()
        Text(
            "BASE $base // AJUSTE $adjustmentLabel // TOTAL $total",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
