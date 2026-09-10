package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ProgressionReward
import com.kinderman.sdo.domain.model.ProgressionRewardType
import com.kinderman.sdo.domain.progression.LevelProgression

@Composable
internal fun LevelProgressionDialog(
    character: Character,
    catalog: List<CatalogEntry>,
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit,
) {
    var target by remember { mutableStateOf(character.level) }
    var confirmedReduction by remember { mutableStateOf(false) }
    var choices by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val levels = if (target > character.level) ((character.level + 1)..target).toList() else emptyList()
    val known = character.attributes.flatMap { attribute -> attribute.skills.filter { it.value < 5 }.map { it.name to it.name } } +
        (character.learnedKnowledges + character.arcaneKnowledges + character.battleTechniques).filter { it.value < 5 }.map { it.id to it.name }
    val knowledgeEntries = catalog.filter { entry ->
        entry.kind in knowledgeKinds && character.attributes.any { it.acronym == entry.relatedAttribute && it.value >= 1 } &&
            (character.learnedKnowledges + character.arcaneKnowledges + character.battleTechniques).none { it.catalogEntryId == entry.id }
    }
    val pathPowers = catalog.filter { entry ->
        val sourceKnowledge = (character.learnedKnowledges + character.arcaneKnowledges + character.battleTechniques)
            .firstOrNull { it.name.equals(entry.sourceKnowledge, true) }
        entry.kind == CatalogKind.POWER && character.powers.none { it.catalogEntryId == entry.id } &&
            (entry.sourceKnowledge.isBlank() || sourceKnowledge != null && sourceKnowledge.value >= (entry.sourceLevel ?: 0))
    }
    val requiredKeys = levels.flatMap { level -> buildList {
        add("resource-$level"); add("knowledge-$level")
        repeat(attributeRewards(level)) { add("attribute-$level-$it") }
        if (level % 5 == 0) { add("new-$level"); add("power-$level") }
    } }
    val complete = requiredKeys.all { !choices[it].isNullOrBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PROGRESSÃO // NÍVEL ${character.level} → $target") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 580.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceField("Novo nível", target, (1..LevelProgression.MAX_LEVEL).toList(), true, display = { it.toString() }) {
                    target = it; choices = emptyMap(); confirmedReduction = false
                }
                if (target < character.level) {
                    Text("Reduzir o nível não remove recompensas já recebidas.", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { confirmedReduction = !confirmedReduction }) { Text(if (confirmedReduction) "✓ REDUÇÃO CONFIRMADA" else "CONFIRMAR REDUÇÃO") }
                }
                levels.forEach { level ->
                    HorizontalDivider()
                    Text("NÍVEL $level", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    Picker("Arcano ou Energia +1", choices["resource-$level"], listOf("ARCANE" to "Arcano", "ENERGY" to "Energia")) { choices = choices + ("resource-$level" to it) }
                    Picker("Conhecimento +1", choices["knowledge-$level"], known) { choices = choices + ("knowledge-$level" to it) }
                    repeat(attributeRewards(level)) { index ->
                        Picker("Atributo +1${if (attributeRewards(level) > 1) " (${index + 1})" else ""}", choices["attribute-$level-$index"], character.attributes.filter { it.value < 10 }.map { it.acronym to it.name }) {
                            choices = choices + ("attribute-$level-$index" to it)
                        }
                    }
                    if (level % 5 == 0) {
                        Picker("Novo Conhecimento", choices["new-$level"], knowledgeEntries.map { it.id to "${it.name} — ${it.group}" }) { choices = choices + ("new-$level" to it) }
                        Picker("Marco de Caminho", choices["power-$level"], pathPowers.map { it.id to it.name }) { choices = choices + ("power-$level" to it) }
                    }
                    Text("Vida +${1 + character.skillValue("VIG", "Vitalidade")} // Sanidade +1${if (level % 5 == 0) " // Energia +1 adicional" else ""}", style = MaterialTheme.typography.bodySmall)
                }
                if (levels.isNotEmpty()) Text("Revise todas as escolhas. Elas serão aplicadas juntas ao confirmar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            val enabled = when { target == character.level -> false; target < character.level -> confirmedReduction; else -> complete }
            TextButton(onClick = {
                if (target < character.level) onConfirm(character.copy(level = target))
                else onConfirm(LevelProgression.apply(character, target, buildRewards(levels, choices), catalog))
            }, enabled = enabled) { Text(if (target < character.level) "REDUZIR NÍVEL" else "APLICAR PROGRESSÃO") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun Picker(label: String, selected: String?, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    val current = options.firstOrNull { it.first == selected } ?: ("" to "Selecionar")
    ChoiceField(label, current, listOf("" to "Selecionar") + options, true, display = { it.second }) { onSelect(it.first) }
}

private fun attributeRewards(level: Int) = (if (level % 2 == 0) 1 else 0) + (if (level % 5 == 0) 1 else 0)

private fun buildRewards(levels: List<Int>, choices: Map<String, String>) = levels.flatMap { level -> buildList {
    add(ProgressionReward(level, ProgressionRewardType.RESOURCE, choices.getValue("resource-$level")))
    add(ProgressionReward(level, ProgressionRewardType.KNOWLEDGE, choices.getValue("knowledge-$level")))
    repeat(attributeRewards(level)) { add(ProgressionReward(level, ProgressionRewardType.ATTRIBUTE, choices.getValue("attribute-$level-$it"))) }
    if (level % 5 == 0) {
        add(ProgressionReward(level, ProgressionRewardType.NEW_KNOWLEDGE, catalogEntryId = choices.getValue("new-$level")))
        add(ProgressionReward(level, ProgressionRewardType.PATH_POWER, catalogEntryId = choices.getValue("power-$level")))
    }
} }

private val knowledgeKinds = setOf(CatalogKind.ACQUIRED_KNOWLEDGE, CatalogKind.ARCANE_KNOWLEDGE, CatalogKind.BATTLE_TECHNIQUE)
