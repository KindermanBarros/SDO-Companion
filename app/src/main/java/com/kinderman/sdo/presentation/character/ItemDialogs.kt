package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kinderman.sdo.domain.catalog.EquipmentGlossary
import com.kinderman.sdo.domain.catalog.GlossaryEntry
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.matchesRegion
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.SdoFilterChip
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.SdoTextButton
import com.kinderman.sdo.ui.sdoClickable

@Composable
internal fun EquipmentGlossaryDialog(
    onUse: ((GlossaryEntry) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("Termos") }
    var group by remember { mutableStateOf("Todos") }
    val sections = remember { listOf("Termos", "Materiais", "Armas") }
    val groups = remember(section) {
        EquipmentGlossary.entries.filter { it.section == section }.map { it.group }.distinct().sorted()
    }
    val filtered = remember(query, section, group) {
        val needle = query.trim()
        EquipmentGlossary.entries.filter { entry ->
            entry.section == section &&
                (group == "Todos" || entry.group == group) &&
                (needle.isEmpty() ||
                    entry.term.contains(needle, ignoreCase = true) ||
                    entry.group.contains(needle, ignoreCase = true) ||
                    entry.definition.contains(needle, ignoreCase = true))
        }.sortedWith(compareBy({ it.group }, { it.term }))
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "GLOSSÁRIO DE EQUIPAMENTOS",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    SdoTextButton(onClick = onDismiss) { Text("FECHAR") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HudTextField("Buscar termo, item ou material", query) { query = it }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        sections.forEach { sec ->
                            SdoFilterChip(
                                label = sec,
                                selected = section == sec,
                                onClick = {
                                    section = sec
                                    group = "Todos"
                                },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SdoFilterChip(
                            label = "Todos",
                            selected = group == "Todos",
                            onClick = { group = "Todos" },
                        )
                        groups.forEach { grp ->
                            SdoFilterChip(
                                label = grp,
                                selected = group == grp,
                                onClick = { group = if (group == grp) "Todos" else grp },
                            )
                        }
                    }
                    SdoMetadataText(
                        text = "RESULTADOS // ${filtered.size}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(filtered, key = { index, it -> "${it.section}:${it.group}:${it.referenceId}:${it.term}:$index" }) { _, entry ->
                        SdoInsetCard(contentPadding = 12.dp, verticalSpacing = 4.dp) {
                            Text(entry.term, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
                            SdoMetadataText(entry.group.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            Text(entry.definition, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            if (onUse != null && entry.referenceId.isNotBlank()) {
                                SdoTextButton(onClick = { onUse(entry) }) { Text("USAR COMO BASE") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EquipmentPickerDialog(
    regionName: String,
    inventory: List<InventoryItem>,
    selectedIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    var selected by remember(selectedIds) { mutableStateOf(selectedIds) }
    val orderedInventory = remember(inventory, regionName) {
        inventory.filter { it.matchesEquipmentRegion(regionName) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("EQUIPAR // ${regionName.uppercase()}") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                if (orderedInventory.isEmpty()) Text("Nenhum item equipável nesta região.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                orderedInventory.forEach { item ->
                    val checked = item.id in selected
                    val compatible = item.matchesRegion(regionName)
                    Row(
                        Modifier.fillMaxWidth().sdoClickable(enabled = compatible) {
                            selected = toggleEquipment(selected, item, inventory)
                        }.padding(vertical = 6.dp),
                    ) {
                        Checkbox(checked, enabled = compatible, onCheckedChange = { value ->
                            selected = if (value) toggleEquipment(selected, item, inventory) else selected - item.id
                        })
                        Column(Modifier.padding(top = 8.dp)) {
                            Text(item.name.ifBlank { "Item sem nome" }, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "PG ${item.pg} // PL ${item.pl}${item.region.takeIf(String::isNotBlank)?.let { " // $it" }.orEmpty()}${if (compatible) " // COMPATÍVEL" else ""}",
                                color = if (compatible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        },
        confirmButton = { SdoTextButton(onClick = { onConfirm(selected) }) { Text("APLICAR") } },
        dismissButton = { SdoTextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

internal fun InventoryItem.matchesEquipmentRegion(bodyRegionName: String): Boolean {
    return matchesRegion(bodyRegionName)
}

private fun toggleEquipment(selected: Set<String>, item: InventoryItem, inventory: List<InventoryItem>): Set<String> {
    if (item.id in selected) return selected - item.id
    if (!item.usesExclusiveRegionSlot()) return selected + item.id
    val conflictingIds = inventory.filter { it.exclusiveSlot() == item.exclusiveSlot() }.mapTo(hashSetOf()) { it.id }
    return (selected - conflictingIds) + item.id
}

private fun InventoryItem.usesExclusiveRegionSlot(): Boolean = exclusiveSlot().isNotBlank()

private fun InventoryItem.exclusiveSlot(): String = when {
    category.equals("Armadura", true) -> "armadura"
    category.equals("Acessório", true) -> "acessório"
    else -> ""
}
