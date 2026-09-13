package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.catalog.EquipmentGlossary
import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.matchesRegion
import com.kinderman.sdo.domain.catalog.toInventoryItem
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechCutDark

@Composable
internal fun InitialShopDialog(
    remainingHeritage: Int,
    catalogAvailable: Boolean,
    onDismiss: () -> Unit,
    onCatalog: () -> Unit,
    onBuilder: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LOJA INICIAL // $remainingHeritage PH") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Use seus Pontos de Herança em equipamentos prontos ou monte um item parte a parte.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onCatalog, enabled = catalogAvailable, modifier = Modifier.fillMaxWidth()) { Text("ESCOLHER ITEM PRONTO") }
                TextButton(onClick = onBuilder, modifier = Modifier.fillMaxWidth()) { Text("CONSTRUIR ITEM COM PH") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("FECHAR") } },
    )
}

@Composable
internal fun ItemCatalogDialog(
    title: String,
    entries: List<CatalogEntry>,
    remainingHeritage: Int?,
    onDismiss: () -> Unit,
    onSelect: (InventoryItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    val categories = remember(entries) {
        entries.filter { it.kind == CatalogKind.ITEM }.map(CatalogEntry::group).filter(String::isNotBlank).distinct().sorted()
    }
    val filtered = remember(entries, query, category, remainingHeritage) {
        val needle = query.trim()
        entries.filter { entry ->
            entry.kind == CatalogKind.ITEM &&
                (category.isBlank() || entry.group.equals(category, ignoreCase = true)) &&
                (remainingHeritage == null || entry.creationCost.toIntOrNull() != null) &&
                (needle.isEmpty() || entry.name.contains(needle, true) || entry.group.contains(needle, true) || entry.summary.contains(needle, true))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                remainingHeritage?.let {
                    Text("PONTOS DE HERANÇA RESTANTES // $it / ${ItemCreationRules.HERITAGE_BUDGET}", color = if (it > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Text("Itens acima do saldo são bloqueados. Itens # não participam da criação com PH.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                HudTextField("Buscar item", query) { query = it }
                ChoiceField(
                    label = "CATEGORIA",
                    value = category,
                    options = listOf("") + categories,
                    enabled = true,
                    display = { it.ifBlank { "TODAS" } },
                ) { category = it }
                Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    filtered.forEach { entry ->
                        val numericCost = entry.creationCost.toIntOrNull()
                        val allowed = remainingHeritage == null || numericCost != null && numericCost <= remainingHeritage
                        val overBudget = remainingHeritage != null && (numericCost == null || numericCost > remainingHeritage)
                        Column(
                            Modifier.fillMaxWidth().clickable(enabled = !overBudget) {
                                if (allowed) {
                                    onSelect(entry.toInventoryItem(initialCreation = remainingHeritage != null))
                                }
                            }.padding(vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(entry.name, color = if (allowed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (remainingHeritage != null) {
                                    "${entry.group} // CRIAÇÃO ${entry.creationCost.ifBlank { "—" }} PH // CARGA ${entry.load}"
                                } else {
                                    "${entry.group} // CARGA ${entry.load}"
                                },
                                color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(entry.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
internal fun ItemBuilderDialog(
    remainingHeritage: Int?,
    onDismiss: () -> Unit,
    onAdd: (InventoryItem) -> Unit,
) {
    var weapon by remember { mutableStateOf(true) }
    var base by remember { mutableStateOf(ItemCreationRules.weaponBases.first()) }
    var material by remember { mutableStateOf(ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" }) }
    var modifications by remember { mutableStateOf(emptyList<ItemPart>()) }
    var gemSlots by remember { mutableIntStateOf(0) }
    var technologySlots by remember { mutableIntStateOf(0) }
    var customName by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf(ItemQuality.COMMON) }
    var gems by remember { mutableStateOf(emptyList<ItemPart>()) }
    var picker by remember { mutableStateOf<String?>(null) }
    val availableBases = if (weapon) ItemCreationRules.weaponBases else ItemCreationRules.armorBases
    val allMaterials = when {
        weapon -> ItemCreationRules.weaponMaterials
        base.id == "gibao" -> ItemCreationRules.armorMaterials.filter { it.id == "organico" }
        else -> ItemCreationRules.armorMaterials
    }
    val initialCreation = remainingHeritage != null
    val availableMaterials = if (initialCreation) allMaterials.filter { it.creationCost != null } else allMaterials
    val availableModifications = ItemCreationRules.compatibleModifications(base, weapon)
    val built = ItemCreationRules.build(
        base, material, modifications, gemSlots, technologySlots, customName, quality,
        components = gems,
    )
    val allowed = remainingHeritage == null || built.creationCost != null && built.creationCost <= remainingHeritage
    val overBudget = remainingHeritage != null && (built.creationCost == null || built.creationCost > remainingHeritage)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCreation) "CONSTRUTOR // CRIAÇÃO INICIAL" else "CONSTRUTOR DE ITEM") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 570.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                remainingHeritage?.let { remaining ->
                    val spent = ItemCreationRules.HERITAGE_BUDGET - remaining
                    val preview = built.creationCost ?: 0
                    Text("HERANÇA // $spent GASTOS + $preview PREVIEW // ${remaining - preview} RESTANTES", color = MaterialTheme.colorScheme.primary)
                    LinearProgressIndicator(
                        progress = { ((spent + preview).toFloat() / ItemCreationRules.HERITAGE_BUDGET).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = {
                        weapon = true
                        base = ItemCreationRules.weaponBases.first()
                        material = ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" }
                        modifications = emptyList()
                    }) { Text(if (weapon) "[ ARMA ]" else "ARMA") }
                    TextButton(onClick = {
                        weapon = false
                        base = ItemCreationRules.armorBases.first()
                        material = ItemCreationRules.armorMaterials.first { it.id == "ligas_comuns" }
                        modifications = emptyList()
                    }) { Text(if (!weapon) "[ ARMADURA / ACESSÓRIO ]" else "ARMADURA / ACESSÓRIO") }
                }
                HudTextField("Nome personalizado (opcional)", customName) { customName = it }
                TextButton(onClick = { picker = "base" }, modifier = Modifier.fillMaxWidth()) { Text("TIPO // ${base.name}") }
                Text("MATERIAL PREDOMINANTE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text("Partes, camadas e ligas compatíveis pertencem à mesma composição; o material só é contabilizado uma vez.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { picker = "material" }, modifier = Modifier.fillMaxWidth()) { Text("MATERIAL // ${material.name}") }
                TextButton(
                    onClick = { quality = ItemQuality.entries[(quality.ordinal + 1) % ItemQuality.entries.size] },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("QUALIDADE // ${quality.label}") }
                Text("MODIFICAÇÕES", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                availableModifications.forEach { modification ->
                    val checked = modification in modifications
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            modifications = toggleModification(modifications, modification)
                        },
                    ) {
                        Checkbox(checked, onCheckedChange = { modifications = toggleModification(modifications, modification) })
                        Column(Modifier.padding(top = 8.dp)) {
                            Text(
                                if (initialCreation) "${modification.name} // ${modification.creationCost ?: "#"} PH"
                                else modification.name,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(modification.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                TwoFields(
                    { IntegerField(if (initialCreation) "Espaços de Gema (1 PH)" else "Espaços de Gema", gemSlots, true, it) { value -> gemSlots = value.coerceIn(gems.size, 5) } },
                    { IntegerField(if (initialCreation) "Espaços de Tecnologia (2 PH)" else "Espaços de Tecnologia", technologySlots, true, it) { value -> technologySlots = value.coerceIn(0, 5) } },
                )
                Text("GEMAS INSTALADAS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                ItemCreationRules.gemComponents.forEach { gem ->
                    val checked = gem in gems
                    Row(Modifier.fillMaxWidth().clickable(enabled = quality != ItemQuality.MUNDANE) {
                        gems = if (checked) gems - gem else if (gems.size < 5) gems + gem else gems
                        gemSlots = gemSlots.coerceAtLeast(gems.size)
                    }) {
                        Checkbox(checked, enabled = quality != ItemQuality.MUNDANE, onCheckedChange = {
                            gems = if (checked) gems - gem else if (gems.size < 5) gems + gem else gems
                            gemSlots = gemSlots.coerceAtLeast(gems.size)
                        })
                        Column(Modifier.padding(top = 8.dp)) {
                            Text("${gem.name} // ${gem.creationCost} PH", color = MaterialTheme.colorScheme.onSurface)
                            Text(gem.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (initialCreation) {
                    Text("CUSTO // ${built.creationCost ?: "#"} PH", color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                }
                Text("PG ${built.pg} // PL ${built.pl} // LA ${built.agilityLimit ?: "—"}", color = MaterialTheme.colorScheme.primary)
                Text("CARGA ${built.load} // DURABILIDADE ${built.durability}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(built.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (initialCreation && !allowed) Text(
                    if (built.creationCost == null) "Itens # não podem ser criados com PH." else "Custo acima dos Pontos de Herança restantes.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (allowed) onAdd(built.toInventoryItem(initialCreation = initialCreation))
            }, enabled = !overBudget) {
                Text(when {
                    allowed -> "ADICIONAR"
                    overBudget -> "SALDO INSUFICIENTE"
                    else -> "REVISAR E ADICIONAR"
                })
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )

    if (picker != null) ItemPartPickerDialog(
        title = if (picker == "base") "SELECIONAR TIPO" else "SELECIONAR MATERIAL",
        parts = if (picker == "base") availableBases else availableMaterials,
        showHeritageCost = initialCreation,
        onDismiss = { picker = null },
    ) { part ->
        if (picker == "base") {
            base = part
            modifications = modifications.filter { it in ItemCreationRules.compatibleModifications(part, weapon) }
            if (!weapon && part.id == "gibao") {
                material = ItemCreationRules.armorMaterials.first { it.id == "organico" }
            }
        } else {
            material = part
        }
        picker = null
    }
}

@Composable
private fun ItemPartPickerDialog(
    title: String,
    parts: List<ItemPart>,
    showHeritageCost: Boolean,
    onDismiss: () -> Unit,
    onSelect: (ItemPart) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                parts.forEach { part ->
                    Column(Modifier.fillMaxWidth().clickable { onSelect(part) }.padding(vertical = 10.dp)) {
                        Text(part.name, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (showHeritageCost) {
                                "CRIAÇÃO ${part.creationCost ?: "#"} PH"
                            } else {
                                part.group
                            },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (part.effect.isNotBlank()) Text(part.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

private fun toggleModification(current: List<ItemPart>, item: ItemPart): List<ItemPart> {
    if (item in current) return current - item
    var next = current + item
    if (item.id == "nobre") next = next.filterNot { it.id == "chamativa" }
    if (item.id == "chamativa") next = next.filterNot { it.id == "nobre" }
    if (item.id == "sob_medida" && next.none { it.id == "ajustada" }) {
        next = next + ItemCreationRules.armorModifications.first { it.id == "ajustada" }
    }
    if (item.id == "ajustada" && current.any { it.id == "sob_medida" }) return current
    return next
}


@Composable
internal fun EquipmentGlossaryDialog(onUse: ((com.kinderman.sdo.domain.catalog.GlossaryEntry) -> Unit)? = null, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("Termos") }
    var group by remember { mutableStateOf("Todos") }
    val groups = remember(section) { EquipmentGlossary.entries.filter { it.section == section }.map { it.group }.distinct().sorted() }
    val filtered = remember(query, section, group) {
        val needle = query.trim()
        EquipmentGlossary.entries.filter { entry ->
            entry.section == section && (group == "Todos" || entry.group == group) && (needle.isEmpty() ||
                entry.term.contains(needle, ignoreCase = true) ||
                entry.group.contains(needle, ignoreCase = true) ||
                entry.definition.contains(needle, ignoreCase = true))
        }.sortedWith(compareBy({ it.group }, { it.term }))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GLOSSÁRIO DE EQUIPAMENTOS") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HudTextField("Buscar termo, item ou material", query) { query = it }
                ChoiceField("Consulta", section, listOf("Termos", "Materiais", "Armas"), true) { section = it; group = "Todos" }
                ChoiceField("Categoria", group, listOf("Todos") + groups, true) { group = it }
                Text("RESULTADOS // ${filtered.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                Column(Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                    filtered.forEach { entry ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(entry.term, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
                            Text(entry.group.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            Text(entry.definition, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            if (onUse != null && entry.referenceId.isNotBlank()) TextButton(onClick = { onUse(entry) }) { Text("USAR COMO BASE") }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("FECHAR") } },
    )
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
                        Modifier.fillMaxWidth().clickable(enabled = compatible) {
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
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("APLICAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
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
