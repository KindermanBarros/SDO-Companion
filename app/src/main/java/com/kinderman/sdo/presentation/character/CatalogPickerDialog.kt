package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.kinderman.sdo.domain.model.userFacingSource
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechCutDark

@Composable
internal fun CatalogPickerDialog(
    title: String,
    entries: List<CatalogEntry>,
    onDismiss: () -> Unit,
    alreadyAddedCatalogIds: Set<String> = emptySet(),
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null,
    groupAshVariants: Boolean = false,
    onSelect: (CatalogEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedAttribute by remember { mutableStateOf("") }
    var selectedKind by remember { mutableStateOf<CatalogKind?>(null) }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("") }
    var details by remember { mutableStateOf<CatalogEntry?>(null) }

    val attributes = remember(entries) { entries.map(CatalogEntry::relatedAttribute).filter(String::isNotBlank).distinct().sorted() }
    val kinds = remember(entries) { entries.map(CatalogEntry::kind).distinct() }
    val categories = remember(entries) { entries.map(CatalogEntry::group).filter(String::isNotBlank).distinct().sorted() }
    val sources = remember(entries) { entries.map(CatalogEntry::userFacingSource).filter(String::isNotBlank).distinct().sorted() }
    val filtered = remember(entries, query, selectedKind, selectedAttribute, selectedCategory, selectedSource) {
        val needle = query.trim()
        entries.filter { entry ->
            (needle.isEmpty() || entry.searchableText().contains(needle, true)) &&
                (selectedKind == null || entry.kind == selectedKind) &&
                (selectedAttribute.isEmpty() || entry.relatedAttribute.equals(selectedAttribute, true)) &&
                (selectedCategory.isEmpty() || entry.group.equals(selectedCategory, true)) &&
                (selectedSource.isEmpty() || entry.userFacingSource().equals(selectedSource, true))
        }
    }
    val displayedEntries = remember(filtered, groupAshVariants) {
        if (!groupAshVariants) filtered else (
            filtered.filterNot { it.kind == CatalogKind.ASH } +
                filtered.filter { it.kind == CatalogKind.ASH }
                    .groupBy { it.name.trim().lowercase() }
                    .values
                    .map { variants -> variants.minBy { it.catalogAshPurity?.ordinal ?: Int.MAX_VALUE } }
            ).sortedWith(compareBy<CatalogEntry>({ it.kind.ordinal }, { it.name.lowercase() }))
    }
    val selectedAshVariants = details?.takeIf { groupAshVariants && it.kind == CatalogKind.ASH }?.let { selected ->
        entries.filter { it.kind == CatalogKind.ASH && it.name.equals(selected.name, ignoreCase = true) }
            .sortedBy { it.catalogAshPurity?.ordinal ?: Int.MAX_VALUE }
    }.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(details?.name ?: title) },
        text = {
            if (details != null) {
                CatalogDetails(
                    details!!,
                    details!!.id in alreadyAddedCatalogIds,
                    ashVariants = selectedAshVariants,
                    onAshVariantSelect = { details = it },
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HudTextField("Buscar no catálogo", query) { query = it }
                    if (kinds.size > 1) ChoiceField("TIPO", selectedKind, listOf(null) + kinds, true, display = { it?.catalogLabel() ?: "TODOS" }) { selectedKind = it }
                    if (attributes.isNotEmpty()) ChoiceField("ATRIBUTO", selectedAttribute, listOf("") + attributes, true, display = { it.ifBlank { "TODOS" } }) { selectedAttribute = it }
                    if (categories.isNotEmpty()) ChoiceField("CATEGORIA", selectedCategory, listOf("") + categories, true, display = { it.ifBlank { "TODAS" } }) { selectedCategory = it }
                    if (sources.size > 1) ChoiceField("ORIGEM", selectedSource, listOf("") + sources, true, display = { it.ifBlank { "TODAS" } }) { selectedSource = it }
                    Text("RESULTADOS // ${displayedEntries.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        items(displayedEntries, key = CatalogEntry::id) { entry ->
                            val alreadyAdded = entry.id in alreadyAddedCatalogIds
                            Column(
                                Modifier.fillMaxWidth().clickable { details = entry },
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(entry.name, color = Ice, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    val groupAlreadyAdded = if (groupAshVariants && entry.kind == CatalogKind.ASH) {
                                        entries.any { variant ->
                                            variant.kind == CatalogKind.ASH &&
                                                variant.name.equals(entry.name, true) &&
                                                variant.id in alreadyAddedCatalogIds
                                        }
                                    } else alreadyAdded
                                    if (groupAlreadyAdded) Text("ADICIONADO", color = Signal, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    buildList {
                                        add(entry.group)
                                        add(entry.relatedAttribute)
                                        if (groupAshVariants && entry.kind == CatalogKind.ASH) {
                                            val variants = entries.count { it.kind == CatalogKind.ASH && it.name.trim().equals(entry.name.trim(), true) }
                                            add("$variants PUREZAS")
                                        }
                                    }.filter(String::isNotBlank).joinToString(" // "),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Text(entry.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val selected = details
            if (selected != null) {
                val blocked = selected.id in alreadyAddedCatalogIds && !selected.repeatable
                TextButton(onClick = { onSelect(selected) }, enabled = !blocked) {
                    val purity = selected.catalogAshPurity?.label?.uppercase()
                    Text(if (blocked) "JÁ ADICIONADO" else purity?.let { "ADICIONAR $it" } ?: "ADICIONAR")
                }
            }
        },
        dismissButton = {
            Row {
                if (details == null && extraActionLabel != null && onExtraAction != null) {
                    TextButton(onClick = onExtraAction) { Text(extraActionLabel) }
                }
                TextButton(onClick = {
                    if (details != null) details = null else onDismiss()
                }) { Text(if (details != null) "VOLTAR" else "CANCELAR") }
            }
        },
    )
}

private fun CatalogKind.catalogLabel(): String = when (this) {
    CatalogKind.PATH -> "Caminho"
    CatalogKind.POWER -> "Poder"
    CatalogKind.MAGIC -> "Magia"
    CatalogKind.ASH -> "Cinza"
    CatalogKind.RUNE -> "Runa"
    CatalogKind.ITEM -> "Item"
    CatalogKind.ACQUIRED_KNOWLEDGE -> "Conhecimento adquirido"
    CatalogKind.ARCANE_KNOWLEDGE -> "Conhecimento arcano"
    CatalogKind.BATTLE_TECHNIQUE -> "Poder marcial"
}

@Composable
private fun CatalogDetails(
    entry: CatalogEntry,
    alreadyAdded: Boolean,
    ashVariants: List<CatalogEntry> = emptyList(),
    onAshVariantSelect: (CatalogEntry) -> Unit = {},
) {
    val isKnowledge = entry.kind in setOf(
        com.kinderman.sdo.domain.model.CatalogKind.ACQUIRED_KNOWLEDGE,
        com.kinderman.sdo.domain.model.CatalogKind.ARCANE_KNOWLEDGE,
        com.kinderman.sdo.domain.model.CatalogKind.BATTLE_TECHNIQUE,
    )
    Column(Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(entry.group.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        if (ashVariants.isNotEmpty()) {
            Text("ESCOLHA A PUREZA PELO EFEITO", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                ashVariants.forEach { variant ->
                    val selected = variant.id == entry.id
                    val purity = variant.catalogAshPurity
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { onAshVariantSelect(variant) }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                                purity?.label?.uppercase() ?: "PADRÃO",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall,
                        )
                            if (selected) Text("SELECIONADA", color = Signal, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(variant.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        purity?.let {
                            Text(
                                "${it.dosesPerLoad} DOSE(S) POR CARGA",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
            Text("DETALHES DA PUREZA SELECIONADA", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
        }
        if (alreadyAdded) Text(if (entry.repeatable) "JÁ ADICIONADO // REPETÍVEL" else "JÁ ADICIONADO", color = MaterialTheme.colorScheme.error)
        Text(entry.summary, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
        DetailLine("ATRIBUTO", entry.relatedAttribute)
        entry.initialValue?.let { DetailLine("VALOR INICIAL", it.toString()) }
        DetailLine("CUSTO", entry.cost)
        DetailLine("AÇÃO", entry.action)
        DetailLine("ALCANCE", entry.range)
        DetailLine("ALVO / ÁREA", entry.targetArea)
        DetailLine("DURAÇÃO", entry.duration)
        DetailLine("RESISTÊNCIA", entry.abilityResistance?.label.orEmpty())
        DetailLine("LIMITE", entry.limit)
        DetailLine("ATIVAÇÃO", entry.activationCondition)
        DetailLine("APRIMORAMENTOS", entry.enhancements)
        DetailLine("ENCERRAMENTO", entry.deactivationCondition)
        if (!isKnowledge) DetailLine("PRÉ-REQUISITOS", entry.prerequisites.joinToString("; "))
        if (entry.mechanicalEffect != entry.summary) DetailLine("EFEITO MECÂNICO", entry.mechanicalEffect)
        DetailLine("FONTE", entry.userFacingSource())
        if (!isKnowledge) DetailLine("REFERÊNCIA", entry.ruleReference)
        DetailLine("PALAVRAS-CHAVE", entry.keywords.joinToString(", "))
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Text("$label // $value", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}
