package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.AcidCyan
import com.kinderman.sdo.ui.AcidMagenta
import com.kinderman.sdo.ui.AuraBlue
import com.kinderman.sdo.ui.Carbon
import com.kinderman.sdo.ui.EnergyBlue
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.LabelFunctional
import com.kinderman.sdo.ui.NeonCoral
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.StatHeader
import com.kinderman.sdo.ui.StatHeaderLight
import com.kinderman.sdo.ui.TechCutDark
import com.kinderman.sdo.ui.TechPanel

@Composable
internal fun IdentitySection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel {
        SectionHeader("01", "Identidade")
        HudTextField("Nome", character.name, enabled = enabled) { onChange(character.copy(name = it)) }
        TwoFields(
            { HudTextField("Raça", character.race, it, enabled = enabled) { value -> onChange(character.copy(race = value)) } },
            { HudTextField("Sub-raça", character.subRace, it, enabled = enabled) { value -> onChange(character.copy(subRace = value)) } },
        )
        HudTextField("Ocupação", character.occupation, enabled = enabled) { onChange(character.copy(occupation = it)) }
        TwoFields(
            { HudTextField("Altura", character.height, it, enabled = enabled) { value -> onChange(character.copy(height = value)) } },
            { HudTextField("Idade", character.age, it, enabled = enabled) { value -> onChange(character.copy(age = value)) } },
        )
        TwoFields(
            { HudTextField("Sexo", character.sex, it, enabled = enabled) { value -> onChange(character.copy(sex = value)) } },
            { HudTextField("Tamanho", character.size, it, enabled = enabled) { value -> onChange(character.copy(size = value)) } },
        )
        TwoFields(
            { IntegerField("Nível", character.level, enabled, it) { value -> onChange(character.copy(level = value.coerceAtLeast(1))) } },
            { IntegerField("Dinheiro (E$)", character.money, enabled, it) { value -> onChange(character.copy(money = value)) } },
        )
    }
}

@Composable
internal fun ResourceSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = Acid) {
        SectionHeader("02", "Recursos")
        ResourceEditor("VIDA", character.life, StatHeaderLight, enabled) { onChange(character.copy(life = it)) }
        ResourceEditor("SANIDADE", character.sanity, StatHeader, enabled) { onChange(character.copy(sanity = it)) }
        ResourceEditor("ARCANO", character.arcane, AuraBlue, enabled) { onChange(character.copy(arcane = it)) }
        ResourceEditor("ENERGIA", character.energy, EnergyBlue, enabled) { onChange(character.copy(energy = it)) }
        ResourceEditor("DESTINO", character.destiny, AcidCyan, enabled) { onChange(character.copy(destiny = it)) }
        ResourceEditor("EXAUSTÃO", character.exhaustion, NeonCoral, enabled) { onChange(character.copy(exhaustion = it)) }
        ResourceEditor("CORRUPÇÃO DIVINA (%)", character.corruption, AcidMagenta, enabled) { onChange(character.copy(corruption = it)) }
    }
}

@Composable
private fun ResourceEditor(label: String, resource: ResourceValue, accent: Color, enabled: Boolean, onValue: (ResourceValue) -> Unit) {
    Column(
        Modifier.fillMaxWidth().border(1.dp, accent, CutCornerShape(topEnd = 12.dp)).background(Carbon).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = Ice, style = MaterialTheme.typography.labelLarge)
        TwoFields(
            { IntegerField("Atual", resource.current, enabled, it) { value -> onValue(resource.copy(current = value)) } },
            { IntegerField("Máximo", resource.maximum, enabled, it) { value -> onValue(resource.copy(maximum = value.coerceAtLeast(0))) } },
        )
    }
}

@Composable
internal fun TraitSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = Signal) {
        SectionHeader("03", "Traços")
        TraitList("Positivos", character.positiveTraits, enabled) { onChange(character.copy(positiveTraits = it)) }
        TraitList("Negativos", character.negativeTraits, enabled) { onChange(character.copy(negativeTraits = it)) }
    }
}

@Composable
private fun TraitList(title: String, values: List<String>, enabled: Boolean, onValues: (List<String>) -> Unit) {
    Text(title.uppercase(), color = LabelFunctional, style = MaterialTheme.typography.labelLarge)
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
    TechPanel(accent = TechCutDark) {
        SectionHeader("04", "Atributos e conhecimentos")
        character.attributes.forEachIndexed { attributeIndex, attribute ->
            AttributeEditor(attribute, enabled) { updated -> onChange(character.copy(attributes = character.attributes.replace(attributeIndex, updated))) }
            if (attributeIndex != character.attributes.lastIndex) HorizontalDivider(color = TechCutDark)
        }
    }
}

@Composable
private fun AttributeEditor(attribute: AttributeValue, enabled: Boolean, onValue: (AttributeValue) -> Unit) {
    Text("${attribute.acronym} // ${attribute.name.uppercase()}", color = Ice, style = MaterialTheme.typography.titleLarge)
    TwoFields(
        { IntegerField("Valor", attribute.value, enabled, it) { value -> onValue(attribute.copy(value = value)) } },
        { IntegerField("Modificador", attribute.modifier, enabled, it) { value -> onValue(attribute.copy(modifier = value)) } },
    )
    attribute.skills.forEachIndexed { index, skill ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(skill.name.uppercase(), color = LabelFunctional, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f).padding(top = 18.dp))
            IntegerField("Valor", skill.value, enabled, Modifier.weight(1f)) { value -> onValue(attribute.copy(skills = attribute.skills.replace(index, skill.copy(value = value)))) }
            IntegerField("Mod.", skill.modifier, enabled, Modifier.weight(1f)) { value -> onValue(attribute.copy(skills = attribute.skills.replace(index, skill.copy(modifier = value)))) }
        }
    }
}

@Composable
internal fun SpecialKnowledgeSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel {
        SectionHeader("05", "Conhecimentos especiais")
        KnowledgeList("Aprendidos", character.learnedKnowledges, enabled) { onChange(character.copy(learnedKnowledges = it)) }
        KnowledgeList("Arcanos", character.arcaneKnowledges, enabled) { onChange(character.copy(arcaneKnowledges = it)) }
        KnowledgeList("Técnicas de batalha", character.battleTechniques, enabled) { onChange(character.copy(battleTechniques = it)) }
    }
}

@Composable
private fun KnowledgeList(title: String, values: List<SpecialKnowledge>, enabled: Boolean, onValues: (List<SpecialKnowledge>) -> Unit) {
    Text(title.uppercase(), color = Acid, style = MaterialTheme.typography.labelLarge)
    values.forEachIndexed { index, knowledge ->
        Column(Modifier.fillMaxWidth().background(Carbon).padding(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("REG.${(index + 1).toString().padStart(2, '0')}", color = LabelFunctional, modifier = Modifier.weight(1f))
                RemoveButton(enabled, "Remover conhecimento") { onValues(values.filterIndexed { itemIndex, _ -> itemIndex != index }) }
            }
            HudTextField("Nome", knowledge.name, enabled = enabled) { onValues(values.replace(index, knowledge.copy(name = it))) }
            TwoFields(
                { HudTextField("Atributo", knowledge.attribute, it, enabled = enabled) { value -> onValues(values.replace(index, knowledge.copy(attribute = value.uppercase()))) } },
                { IntegerField("Valor", knowledge.value, enabled, it) { value -> onValues(values.replace(index, knowledge.copy(value = value))) } },
            )
        }
    }
    AddButton("Adicionar $title", enabled) { onValues(values + SpecialKnowledge()) }
}

@Composable
internal fun ProtectionSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = AcidCyan) {
        SectionHeader("06", "Proteções")
        character.protections.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (name, value) ->
                    IntegerField(name, value, enabled, Modifier.weight(1f)) { updated -> onChange(character.copy(protections = character.protections + (name to updated))) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
