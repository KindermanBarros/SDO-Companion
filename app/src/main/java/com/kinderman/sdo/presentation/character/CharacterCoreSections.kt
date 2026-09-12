package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.agilityLimitBreakdown
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
            { IntegerField("Altura (cm)", character.height.toIntOrNull() ?: 0, enabled, it) { value -> onChange(character.copy(height = value.coerceAtLeast(0).toString())) } },
            { IntegerField("Idade", character.age.toIntOrNull() ?: 0, enabled, it) { value -> onChange(character.copy(age = value.coerceAtLeast(0).toString())) } },
        )
        ChoiceField("Gênero", character.sex.takeIf { it in listOf("Masculino", "Feminino", "N/A") } ?: "N/A", listOf("Masculino", "Feminino", "N/A"), enabled) {
            onChange(character.copy(sex = it))
        }
        TwoFields(
            { if (character.isInCreation) Text("NÍVEL 1", color = MaterialTheme.colorScheme.primary) else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { onChange(character.copy(level = (character.level - 1).coerceAtLeast(1))) }, enabled = enabled && character.level > 1) {
                    Icon(Icons.Default.Remove, "Diminuir nível")
                }
                Text("NÍVEL ${character.level}", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
                IconButton(onClick = { selectingLevel = true }, enabled = enabled) { Icon(Icons.Default.Add, "Aumentar nível") }
            } },
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
        ManualResourceEditor("DESTINO", character.destiny.copy(maximum = character.destinyMaximum), MaterialTheme.colorScheme.secondary, enabled, fixedMaximum = character.destinyMaximum) {
            onChange(character.copy(destiny = it.copy(maximum = 5)))
        }
        ManualResourceEditor("EXAUSTÃO", character.exhaustion, MaterialTheme.colorScheme.error, enabled) { onChange(character.copy(exhaustion = it)) }
        ManualResourceEditor("CORRUPÇÃO DIVINA (%)", character.corruption.copy(maximum = 100), MaterialTheme.colorScheme.tertiary, enabled, fixedMaximum = 100) {
            onChange(character.copy(corruption = it.copy(maximum = 100)))
        }
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
    var expanded by rememberSaveable(label) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().border(1.dp, accent, CutCornerShape(topEnd = 12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                ResourceSummary(resource.current, maximum, accent)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Recolher $label" else "Expandir $label", tint = accent)
        }
        if (expanded) {
            Text("CÁLCULO // $formula = $base", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            ResourceCurrentControls(label, resource.current, maximum, accent, enabled) { onValue(resource.copy(current = it)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = enabled && resource.adjustment > -base, onClick = { onValue(resource.copy(adjustment = resource.adjustment - 1)) }) {
                    Icon(Icons.Default.Remove, "Diminuir ajuste de $label", tint = accent)
                }
                Text("AJUSTE ${resource.adjustment.signed()}", color = accent)
                IconButton(enabled = enabled, onClick = { onValue(resource.copy(adjustment = resource.adjustment + 1)) }) {
                    Icon(Icons.Default.Add, "Aumentar ajuste de $label", tint = accent)
                }
            }
            Text("BASE $base // AJUSTE ${resource.adjustment.signed()} // TOTAL $maximum", color = accent, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ManualResourceEditor(
    label: String,
    resource: ResourceValue,
    accent: Color,
    enabled: Boolean,
    fixedMaximum: Int? = null,
    onValue: (ResourceValue) -> Unit,
) {
    var expanded by rememberSaveable(label) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().border(1.dp, accent, CutCornerShape(topEnd = 12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val maximum = fixedMaximum ?: resource.maximum.coerceAtLeast(0)
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                ResourceSummary(resource.current, maximum, accent)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Recolher $label" else "Expandir $label", tint = accent)
        }
        if (expanded) {
            ResourceCurrentControls(label, resource.current, maximum, accent, enabled) { onValue(resource.copy(current = it, maximum = maximum)) }
            if (fixedMaximum == null) ChoiceField("Máximo", maximum.coerceIn(0, 100), (0..100).toList(), enabled) {
                onValue(resource.copy(current = resource.current.coerceAtMost(it), maximum = it))
            } else Text("MÁXIMO FIXO // $fixedMaximum", color = accent, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ResourceSummary(current: Int, maximum: Int, accent: Color) {
    val safeMaximum = maximum.coerceAtLeast(0)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
            Text("ATUAL", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(current.coerceIn(0, safeMaximum).toString(), color = accent, style = MaterialTheme.typography.titleLarge)
        }
        Column {
            Text("MÁXIMO", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(safeMaximum.toString(), color = accent, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ResourceCurrentControls(label: String, current: Int, maximum: Int, accent: Color, enabled: Boolean, onCurrent: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(enabled = enabled && current > 0, onClick = { onCurrent((current - 1).coerceAtLeast(0)) }) {
            Icon(Icons.Default.Remove, "Diminuir $label", tint = accent)
        }
        ChoiceField("Atual", current.coerceIn(0, maximum), (0..maximum).toList(), enabled, Modifier.weight(1f)) { onCurrent(it) }
        IconButton(enabled = enabled && current < maximum, onClick = { onCurrent((current + 1).coerceAtMost(maximum)) }) {
            Icon(Icons.Default.Add, "Aumentar $label", tint = accent)
        }
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
internal fun AttributeSection(
    character: Character,
    enabled: Boolean,
    onChange: (Character) -> Unit,
    showAttributes: Boolean = true,
    showBasicKnowledges: Boolean = true,
) {
    TechPanel(accent = MaterialTheme.colorScheme.outlineVariant) {
        SectionHeader("04", "Atributos e conhecimentos")
        character.attributes.forEachIndexed { attributeIndex, attribute ->
            AttributeEditor(character, attribute, enabled, showAttributes, showBasicKnowledges) { updated -> onChange(character.copy(attributes = character.attributes.replace(attributeIndex, updated))) }
            if (attributeIndex != character.attributes.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun AttributeEditor(character: Character, attribute: AttributeValue, enabled: Boolean, showAttributes: Boolean, showBasicKnowledges: Boolean, onValue: (AttributeValue) -> Unit) {
    val creationMode = character.isInCreation
    var expanded by rememberSaveable(attribute.acronym) { mutableStateOf(creationMode) }
    val calculation = character.attributeCalculation(attribute.acronym)
    val bonuses = buildList {
        if (calculation.adjustment != 0) add("Ajuste da ficha ${calculation.adjustment.signed()}")
        addAll(calculation.modifiers.filter { it.value != 0 && !it.label.startsWith("LA —") }.map { "${it.label} [${it.sourceType.name}] ${it.value.signed()}" })
    }
    Column(
        Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("${attribute.acronym} // ${attribute.name.uppercase()}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                Text("VALOR TOTAL // ${calculation.total}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Recolher atributo" else "Expandir atributo")
        }
        if (bonuses.isNotEmpty()) Text("BÔNUS APLICADOS: ${bonuses.joinToString(" // ")}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        else Text("BÔNUS APLICADOS: NENHUM", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        if (attribute.acronym.equals("AGI", true)) {
            val limit = character.agilityLimitBreakdown()
            Text(
                "LA // ${limit.total ?: "SEM LIMITE"}" + limit.contributions.joinToString(prefix = if (limit.contributions.isEmpty()) "" else " // ") { "${it.label}: ${it.value}" },
                color = if (limit.total != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            calculation.modifiers.firstOrNull { it.label.startsWith("LA —") }?.let { penalty ->
                Text("PENALIDADE APLICADA // ${penalty.label} ${penalty.value.signed()}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (expanded) {
            if (showAttributes) ChoiceField("Valor-base", attribute.value.coerceIn(0, 5), (0..5).toList(), enabled) { onValue(attribute.copy(value = it)) }
            if (showBasicKnowledges) attribute.skills.forEachIndexed { index, skill ->
                val skillCalculation = character.basicKnowledgeCalculation(attribute.acronym, skill.name)
                val skillBonuses = buildList {
                    if (skillCalculation.adjustment != 0) add("Ajuste ${skillCalculation.adjustment.signed()}")
                    addAll(skillCalculation.modifiers.filter { it.value != 0 }.map { "${it.label} [${it.sourceType.name}] ${it.value.signed()}" })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1.35f)) {
                        Text(skill.name.uppercase(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                        Text("TOTAL ${skillCalculation.total}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    }
                    ChoiceField("Base", skill.value.coerceIn(0, 5), (0..5).toList(), enabled, Modifier.weight(1f)) { value ->
                        onValue(attribute.copy(skills = attribute.skills.replace(index, skill.copy(value = value))))
                    }
                }
                if (skillBonuses.isNotEmpty()) Text("Bônus aplicados: ${skillBonuses.joinToString(" // ")}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun Int.signed(): String = if (this >= 0) "+$this" else toString()

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
        when (character.loadCondition) {
            com.kinderman.sdo.domain.model.LoadCondition.OVERLOADED -> Text("SOBRECARREGADO // −2 aplicado à Esquiva", color = MaterialTheme.colorScheme.error)
            com.kinderman.sdo.domain.model.LoadCondition.IMMOBILE -> Text("IMÓVEL // não pode usar Esquiva", color = MaterialTheme.colorScheme.error)
            else -> Unit
        }
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
    var expanded by rememberSaveable(name) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.secondary, CutCornerShape(topEnd = 12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(name.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                Text("TOTAL $total", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleLarge)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Recolher $name" else "Expandir $name", tint = MaterialTheme.colorScheme.secondary)
        }
        if (expanded) {
        Text("CÁLCULO // $formula = $base", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                enabled = enabled && adjustment > -base,
                onClick = { onAdjustment(adjustment - 1) },
            ) {
                Icon(Icons.Default.Remove, "Diminuir ajuste de $name", tint = MaterialTheme.colorScheme.secondary)
            }
            Text("AJUSTE ${adjustment.signed()}", color = MaterialTheme.colorScheme.secondary)
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
}
