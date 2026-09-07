package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.AcidCyan
import com.kinderman.sdo.ui.ArcanePanel
import com.kinderman.sdo.ui.Carbon
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.LabelFunctional
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechCutDark
import com.kinderman.sdo.ui.TechPanel

@Composable
internal fun PathSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = Signal) {
        SectionHeader("07", "Caminho")
        HudTextField("Nome do Caminho", character.pathName, enabled = enabled) { onChange(character.copy(pathName = it)) }
        HudTextField("Lema", character.pathMotto, enabled = enabled) { onChange(character.copy(pathMotto = it)) }
        Text("PALAVRAS-CHAVE // 3", color = LabelFunctional, style = MaterialTheme.typography.labelLarge)
        character.pathKeywords.forEachIndexed { index, keyword ->
            HudTextField("Palavra-chave ${index + 1}", keyword, enabled = enabled) { onChange(character.copy(pathKeywords = character.pathKeywords.replace(index, it))) }
        }
        Text("PILARES // 3", color = LabelFunctional, style = MaterialTheme.typography.labelLarge)
        character.pathPillars.forEachIndexed { index, pillar ->
            HudTextField("Pilar ${index + 1}", pillar, multiline = true, enabled = enabled) { onChange(character.copy(pathPillars = character.pathPillars.replace(index, it))) }
        }
    }
}

@Composable
internal fun PowerSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = Acid) {
        SectionHeader("08", "Poderes")
        Text("REGISTROS // ${character.powers.size}", color = Acid, style = MaterialTheme.typography.labelLarge)
        character.powers.forEachIndexed { index, power ->
            PowerEditor(index, power, enabled,
                onRemove = { onChange(character.copy(powers = character.powers.filterIndexed { itemIndex, _ -> itemIndex != index })) },
                onValue = { onChange(character.copy(powers = character.powers.replace(index, it))) },
            )
        }
        AddButton("Adicionar poder", enabled) { onChange(character.copy(powers = character.powers + Power())) }
    }
}

@Composable
private fun PowerEditor(index: Int, power: Power, enabled: Boolean, onRemove: () -> Unit, onValue: (Power) -> Unit) {
    Column(Modifier.fillMaxWidth().background(ArcanePanel).padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("PODER ${(index + 1).toString().padStart(2, '0')}", color = Ice, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            RemoveButton(enabled, "Remover poder", onRemove)
        }
        HudTextField("Nome", power.name, enabled = enabled) { onValue(power.copy(name = it)) }
        HudTextField("Origem narrativa", power.origin, multiline = true, enabled = enabled) { onValue(power.copy(origin = it)) }
        TwoFields(
            { HudTextField("Custo", power.cost, it, enabled = enabled) { value -> onValue(power.copy(cost = value)) } },
            { HudTextField("Ação", power.action, it, enabled = enabled) { value -> onValue(power.copy(action = value)) } },
        )
        TwoFields(
            { HudTextField("Alcance", power.range, it, enabled = enabled) { value -> onValue(power.copy(range = value)) } },
            { HudTextField("Duração", power.duration, it, enabled = enabled) { value -> onValue(power.copy(duration = value)) } },
        )
        HudTextField("Limite", power.limit, enabled = enabled) { onValue(power.copy(limit = it)) }
        HudTextField("Efeito", power.effect, multiline = true, enabled = enabled) { onValue(power.copy(effect = it)) }
    }
}

@Composable
internal fun InventorySection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel {
        SectionHeader("09", "Inventário")
        Text("CARGA ${character.currentLoad} / ${character.maximumLoad}", color = if (character.currentLoad > character.maximumLoad) Signal else AcidCyan, style = MaterialTheme.typography.titleLarge)
        Text("Máxima = 2 + FOR + capacidade do recipiente. Itens [G] não contam como carregados.", color = Muted, style = MaterialTheme.typography.bodySmall)
        IntegerField("Capacidade do recipiente equipado", character.containerCapacity, enabled) { onChange(character.copy(containerCapacity = it.coerceAtLeast(0))) }
        character.inventory.forEachIndexed { index, item ->
            InventoryEditor(index, item, enabled,
                onRemove = { onChange(character.copy(inventory = character.inventory.filterIndexed { itemIndex, _ -> itemIndex != index })) },
                onValue = { onChange(character.copy(inventory = character.inventory.replace(index, it))) },
            )
        }
        AddButton("Adicionar item", enabled) { onChange(character.copy(inventory = character.inventory + InventoryItem())) }
    }
}

@Composable
private fun InventoryEditor(index: Int, item: InventoryItem, enabled: Boolean, onRemove: () -> Unit, onValue: (InventoryItem) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Carbon).padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("ITEM ${(index + 1).toString().padStart(2, '0')}", color = Ice, modifier = Modifier.weight(1f))
            RemoveButton(enabled, "Remover item", onRemove)
        }
        HudTextField("Nome", item.name, enabled = enabled) { onValue(item.copy(name = it)) }
        TwoFields(
            { HudTextField("Estado E/R/M/G", item.state, it, enabled = enabled) { value -> onValue(item.copy(state = value.uppercase().take(1))) } },
            { IntegerField("Carga", item.load, enabled, it) { value -> onValue(item.copy(load = value.coerceAtLeast(0))) } },
        )
        TwoFields(
            { HudTextField("Durabilidade", item.durability, it, enabled = enabled) { value -> onValue(item.copy(durability = value)) } },
            { HudTextField("Região", item.region, it, enabled = enabled) { value -> onValue(item.copy(region = value)) } },
        )
        HudTextField("Efeito", item.effect, multiline = true, enabled = enabled) { onValue(item.copy(effect = it)) }
    }
}

@Composable
internal fun BodySection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = Signal) {
        SectionHeader("10", "Corpo e armadura")
        HudTextField("Limitação de Agilidade", character.agilityLimit, enabled = enabled) { onChange(character.copy(agilityLimit = it)) }
        character.bodyRegions.forEachIndexed { index, region ->
            Column(Modifier.fillMaxWidth().background(Carbon).padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("D10.${region.roll.toString().padStart(2, '0')} // ${region.name.uppercase()}", color = Ice, style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntegerField("Falhas", region.failures, enabled, Modifier.weight(1f)) { value -> onChange(character.copy(bodyRegions = character.bodyRegions.replace(index, region.copy(failures = value.coerceIn(0, 4))))) }
                    IntegerField("PL", region.localProtection, enabled, Modifier.weight(1f)) { value -> onChange(character.copy(bodyRegions = character.bodyRegions.replace(index, region.copy(localProtection = value.coerceAtLeast(0))))) }
                    IntegerField("PG", region.generalProtection, enabled, Modifier.weight(1f)) { value -> onChange(character.copy(bodyRegions = character.bodyRegions.replace(index, region.copy(generalProtection = value.coerceAtLeast(0))))) }
                }
                HudTextField("Danos", region.damage, enabled = enabled) { onChange(character.copy(bodyRegions = character.bodyRegions.replace(index, region.copy(damage = it)))) }
                HudTextField("Implantes", region.implants, enabled = enabled) { onChange(character.copy(bodyRegions = character.bodyRegions.replace(index, region.copy(implants = it)))) }
                HudTextField("Equipamentos", region.equipment, enabled = enabled) { onChange(character.copy(bodyRegions = character.bodyRegions.replace(index, region.copy(equipment = it)))) }
            }
        }
    }
}

@Composable
internal fun OrganSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = Signal) {
        SectionHeader("11", "Órgãos")
        character.organs.forEachIndexed { index, organ ->
            Column(Modifier.fillMaxWidth().background(Carbon).padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                HudTextField("Órgão", organ.name, enabled = enabled) { onChange(character.copy(organs = character.organs.replace(index, organ.copy(name = it)))) }
                IntegerField("Falhas", organ.failures, enabled) { onChange(character.copy(organs = character.organs.replace(index, organ.copy(failures = it.coerceIn(0, 3))))) }
                HudTextField("Implante", organ.implant, enabled = enabled) { onChange(character.copy(organs = character.organs.replace(index, organ.copy(implant = it)))) }
                HudTextField("Efeito", organ.effect, multiline = true, enabled = enabled) { onChange(character.copy(organs = character.organs.replace(index, organ.copy(effect = it)))) }
            }
            if (index != character.organs.lastIndex) HorizontalDivider(color = TechCutDark)
        }
    }
}

@Composable
internal fun MysticSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = AcidCyan) {
        SectionHeader("12", "Magias, runas e cinzas")
        character.mysticAbilities.forEachIndexed { index, ability ->
            MysticEditor(index, ability, enabled,
                onRemove = { onChange(character.copy(mysticAbilities = character.mysticAbilities.filterIndexed { itemIndex, _ -> itemIndex != index })) },
                onValue = { onChange(character.copy(mysticAbilities = character.mysticAbilities.replace(index, it))) },
            )
        }
        AddButton("Adicionar magia, runa ou cinza", enabled) { onChange(character.copy(mysticAbilities = character.mysticAbilities + MysticAbility())) }
    }
}

@Composable
private fun MysticEditor(index: Int, ability: MysticAbility, enabled: Boolean, onRemove: () -> Unit, onValue: (MysticAbility) -> Unit) {
    Column(Modifier.fillMaxWidth().background(ArcanePanel).padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("EFEITO ${(index + 1).toString().padStart(2, '0')}", color = Ice, modifier = Modifier.weight(1f))
            RemoveButton(enabled, "Remover efeito", onRemove)
        }
        TwoFields(
            { HudTextField("Tipo", ability.type, it, enabled = enabled) { value -> onValue(ability.copy(type = value)) } },
            { HudTextField("Nome", ability.name, it, enabled = enabled) { value -> onValue(ability.copy(name = value)) } },
        )
        TwoFields(
            { HudTextField("Custo", ability.cost, it, enabled = enabled) { value -> onValue(ability.copy(cost = value)) } },
            { HudTextField("Ação", ability.action, it, enabled = enabled) { value -> onValue(ability.copy(action = value)) } },
        )
        TwoFields(
            { HudTextField("Alcance", ability.range, it, enabled = enabled) { value -> onValue(ability.copy(range = value)) } },
            { HudTextField("Duração", ability.duration, it, enabled = enabled) { value -> onValue(ability.copy(duration = value)) } },
        )
        HudTextField("Efeito", ability.effect, multiline = true, enabled = enabled) { onValue(ability.copy(effect = it)) }
    }
}

@Composable
internal fun ConditionSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = Signal) {
        SectionHeader("13", "Condições")
        character.conditions.forEachIndexed { index, condition ->
            ConditionEditor(index, condition, enabled,
                onRemove = { onChange(character.copy(conditions = character.conditions.filterIndexed { itemIndex, _ -> itemIndex != index })) },
                onValue = { onChange(character.copy(conditions = character.conditions.replace(index, it))) },
            )
        }
        AddButton("Adicionar condição", enabled) { onChange(character.copy(conditions = character.conditions + ConditionEffect())) }
    }
}

@Composable
private fun ConditionEditor(index: Int, condition: ConditionEffect, enabled: Boolean, onRemove: () -> Unit, onValue: (ConditionEffect) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Carbon).padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("CONDIÇÃO ${(index + 1).toString().padStart(2, '0')}", color = Ice, modifier = Modifier.weight(1f))
            RemoveButton(enabled, "Remover condição", onRemove)
        }
        HudTextField("Condição", condition.name, enabled = enabled) { onValue(condition.copy(name = it)) }
        TwoFields(
            { HudTextField("Intensidade", condition.intensity, it, enabled = enabled) { value -> onValue(condition.copy(intensity = value)) } },
            { HudTextField("Duração", condition.duration, it, enabled = enabled) { value -> onValue(condition.copy(duration = value)) } },
        )
        HudTextField("Origem", condition.origin, enabled = enabled) { onValue(condition.copy(origin = it)) }
    }
}

@Composable
internal fun NarrativeSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel {
        SectionHeader("14", "História")
        HudTextField("História", character.story, multiline = true, enabled = enabled) { onChange(character.copy(story = it)) }
    }
}
