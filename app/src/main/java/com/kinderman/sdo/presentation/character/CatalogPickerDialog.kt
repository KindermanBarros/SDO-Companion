package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
    onSelect: (CatalogEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedAttribute by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("") }
    var details by remember { mutableStateOf<CatalogEntry?>(null) }

    val attributes = remember(entries) { entries.map(CatalogEntry::relatedAttribute).filter(String::isNotBlank).distinct().sorted() }
    val categories = remember(entries) { entries.map(CatalogEntry::group).filter(String::isNotBlank).distinct().sorted() }
    val sources = remember(entries) { entries.map(CatalogEntry::source).filter(String::isNotBlank).distinct().sorted() }
    val filtered = remember(entries, query, selectedAttribute, selectedCategory, selectedSource) {
        val needle = query.trim()
        entries.filter { entry ->
            (needle.isEmpty() || entry.searchableText().contains(needle, true)) &&
                (selectedAttribute.isEmpty() || entry.relatedAttribute.equals(selectedAttribute, true)) &&
                (selectedCategory.isEmpty() || entry.group.equals(selectedCategory, true)) &&
                (selectedSource.isEmpty() || entry.source.equals(selectedSource, true))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(details?.name ?: title) },
        text = {
            if (details != null) {
                CatalogDetails(details!!, details!!.id in alreadyAddedCatalogIds)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HudTextField("Buscar no catálogo", query) { query = it }
                    if (attributes.isNotEmpty()) FilterButton("ATRIBUTO", selectedAttribute, attributes) { selectedAttribute = it }
                    if (categories.isNotEmpty()) FilterButton("CATEGORIA", selectedCategory, categories) { selectedCategory = it }
                    if (sources.size > 1) FilterButton("ORIGEM", selectedSource, sources) { selectedSource = it }
                    Text("RESULTADOS // ${filtered.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        items(filtered, key = CatalogEntry::id) { entry ->
                            val alreadyAdded = entry.id in alreadyAddedCatalogIds
                            Column(
                                Modifier.fillMaxWidth().clickable { details = entry },
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(entry.name, color = Ice, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    if (alreadyAdded) Text("ADICIONADO", color = Signal, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    listOf(entry.group, entry.relatedAttribute).filter(String::isNotBlank).joinToString(" // "),
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
                    Text(if (blocked) "JÁ ADICIONADO" else "ADICIONAR")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (details != null) details = null else onDismiss()
            }) { Text(if (details != null) "VOLTAR" else "CANCELAR") }
        },
    )
}

@Composable
private fun FilterButton(label: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    TextButton(
        onClick = {
            val all = listOf("") + options
            val index = all.indexOf(selected).coerceAtLeast(0)
            onSelected(all[(index + 1) % all.size])
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("$label // ${selected.ifBlank { "TODOS" }}")
    }
}

@Composable
private fun CatalogDetails(entry: CatalogEntry, alreadyAdded: Boolean) {
    Column(Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(entry.group.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        if (alreadyAdded) Text(if (entry.repeatable) "JÁ ADICIONADO // REPETÍVEL" else "JÁ ADICIONADO", color = MaterialTheme.colorScheme.error)
        Text(entry.summary, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
        DetailLine("ATRIBUTO", entry.relatedAttribute)
        entry.initialValue?.let { DetailLine("VALOR INICIAL", it.toString()) }
        DetailLine("CUSTO", entry.cost)
        DetailLine("AÇÃO", entry.action)
        DetailLine("ALCANCE", entry.range)
        DetailLine("DURAÇÃO", entry.duration)
        DetailLine("LIMITE", entry.limit)
        DetailLine("ATIVAÇÃO", entry.activationCondition)
        DetailLine("APRIMORAMENTOS", entry.enhancements)
        DetailLine("ENCERRAMENTO", entry.deactivationCondition)
        DetailLine("PRÉ-REQUISITOS", entry.prerequisites.joinToString("; "))
        DetailLine("EFEITO MECÂNICO", entry.mechanicalEffect)
        DetailLine("FONTE", entry.source)
        DetailLine("REFERÊNCIA", entry.ruleReference)
        DetailLine("PALAVRAS-CHAVE", entry.keywords.joinToString(", "))
        DetailLine("VERSÃO", entry.version.toString())
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Text("$label // $value", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}
