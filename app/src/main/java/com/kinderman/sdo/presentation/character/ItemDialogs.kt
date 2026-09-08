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
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.ItemMaterialPart
import com.kinderman.sdo.domain.model.toInventoryItem
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechCutDark

@Composable
internal fun InitialShopDialog(
    remainingHeritage: Int,
    onDismiss: () -> Unit,
    onCatalog: () -> Unit,
    onBuilder: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LOJA INICIAL // $remainingHeritage PH") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Use seus Pontos de Herança em equipamentos prontos ou monte um item parte a parte.", color = Muted)
                TextButton(onClick = onCatalog, modifier = Modifier.fillMaxWidth()) { Text("ESCOLHER ITEM PRONTO") }
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
    var pendingOverride by remember { mutableStateOf<CatalogEntry?>(null) }
    val filtered = remember(entries, query) {
        val needle = query.trim()
        entries.filter { entry ->
            needle.isEmpty() || entry.name.contains(needle, true) || entry.group.contains(needle, true) || entry.summary.contains(needle, true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                remainingHeritage?.let {
                    Text("PONTOS DE HERANÇA RESTANTES // $it / ${ItemCreationRules.HERITAGE_BUDGET}", color = if (it > 0) Acid else Signal)
                    Text("Itens # e itens acima do saldo exigem decisão do Historiador.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                HudTextField("Buscar item", query) { query = it }
                Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    filtered.forEach { entry ->
                        val numericCost = entry.creationCost.toIntOrNull()
                        val allowed = remainingHeritage == null || numericCost != null && numericCost <= remainingHeritage
                        val overBudget = remainingHeritage != null && numericCost != null && numericCost > remainingHeritage
                        Column(
                            Modifier.fillMaxWidth().clickable(enabled = !overBudget) {
                                if (allowed) {
                                    onSelect(entry.toInventoryItem(initialCreation = remainingHeritage != null))
                                } else {
                                    pendingOverride = entry
                                }
                            }.padding(vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(entry.name, color = if (allowed) Ice else Muted, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${entry.group} // CRIAÇÃO ${entry.creationCost.ifBlank { "—" }} // ${entry.price} E$ // CARGA ${entry.load}",
                                color = if (allowed) Acid else Signal,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(entry.summary, color = Muted, style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(color = TechCutDark)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
    pendingOverride?.let { entry ->
        AssistedItemValidationDialog(
            message = "Este item tem custo # e exige uma decisão do Historiador. Deseja adicioná-lo mesmo assim?",
            onDismiss = { pendingOverride = null },
        ) {
            onSelect(entry.toInventoryItem(initialCreation = true))
            pendingOverride = null
        }
    }
}

@Composable
internal fun ItemBuilderDialog(
    remainingHeritage: Int,
    onDismiss: () -> Unit,
    onAdd: (InventoryItem) -> Unit,
) {
    var weapon by remember { mutableStateOf(true) }
    var base by remember { mutableStateOf(ItemCreationRules.weaponBases.first()) }
    var parts by remember {
        mutableStateOf(listOf(ItemMaterialPart("Parte principal", ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" })))
    }
    var modifications by remember { mutableStateOf(emptyList<ItemPart>()) }
    var gemSlots by remember { mutableIntStateOf(0) }
    var technologySlots by remember { mutableIntStateOf(0) }
    var customName by remember { mutableStateOf("") }
    var picker by remember { mutableStateOf<String?>(null) }
    var materialPartIndex by remember { mutableIntStateOf(-1) }
    var confirmOverride by remember { mutableStateOf(false) }
    val availableBases = if (weapon) ItemCreationRules.weaponBases else ItemCreationRules.armorBases
    val availableMaterials = when {
        weapon -> ItemCreationRules.weaponMaterials
        base.id == "gibao" -> ItemCreationRules.armorMaterials.filter { it.id == "organico" }
        else -> ItemCreationRules.armorMaterials
    }
    val availableModifications = if (weapon) ItemCreationRules.weaponModifications else ItemCreationRules.armorModifications
    val built = ItemCreationRules.build(base, parts, modifications, gemSlots, technologySlots, customName)
    val allowed = built.creationCost != null && built.creationCost <= remainingHeritage
    val overBudget = built.creationCost != null && built.creationCost > remainingHeritage

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CONSTRUTOR DE ITEM") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 570.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text("CRIAÇÃO INICIAL // $remainingHeritage PH RESTANTES", color = Acid)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = {
                        weapon = true
                        base = ItemCreationRules.weaponBases.first()
                        parts = listOf(ItemMaterialPart("Parte principal", ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" }))
                        modifications = emptyList()
                    }) { Text(if (weapon) "[ ARMA ]" else "ARMA") }
                    TextButton(onClick = {
                        weapon = false
                        base = ItemCreationRules.armorBases.first()
                        parts = listOf(ItemMaterialPart("Parte principal", ItemCreationRules.armorMaterials.first { it.id == "ligas_comuns" }))
                        modifications = emptyList()
                    }) { Text(if (!weapon) "[ ARMADURA / ACESSÓRIO ]" else "ARMADURA / ACESSÓRIO") }
                }
                HudTextField("Nome personalizado (opcional)", customName) { customName = it }
                TextButton(onClick = { picker = "base" }, modifier = Modifier.fillMaxWidth()) { Text("TIPO // ${base.name}") }
                Text("PARTES E MATERIAIS", color = Acid, style = MaterialTheme.typography.labelLarge)
                parts.forEachIndexed { index, part ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        HudTextField("Nome da parte ${index + 1}", part.name) { value ->
                            parts = parts.toMutableList().also { it[index] = part.copy(name = value) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = {
                                materialPartIndex = index
                                picker = "material"
                            }) { Text("MATERIAL // ${part.material.name}") }
                            if (parts.size > 1) TextButton(onClick = {
                                parts = parts.toMutableList().also { it.removeAt(index) }
                            }) { Text("REMOVER", color = Signal) }
                        }
                    }
                }
                TextButton(onClick = {
                    val defaultMaterial = availableMaterials.firstOrNull() ?: return@TextButton
                    parts = parts + ItemMaterialPart("Parte ${parts.size + 1}", defaultMaterial)
                }, modifier = Modifier.fillMaxWidth()) { Text("+ ADICIONAR PARTE") }
                Text("MODIFICAÇÕES", color = Acid, style = MaterialTheme.typography.labelLarge)
                availableModifications.forEach { modification ->
                    val checked = modification in modifications
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            modifications = toggleModification(modifications, modification)
                        },
                    ) {
                        Checkbox(checked, onCheckedChange = { modifications = toggleModification(modifications, modification) })
                        Column(Modifier.padding(top = 8.dp)) {
                            Text("${modification.name} // ${modification.creationCost ?: "#"} PH", color = Ice)
                            Text(modification.effect, color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                TwoFields(
                    { IntegerField("Espaços de Gema (1 PH)", gemSlots, true, it) { value -> gemSlots = value.coerceIn(0, 5) } },
                    { IntegerField("Espaços de Tecnologia (2 PH)", technologySlots, true, it) { value -> technologySlots = value.coerceIn(0, 5) } },
                )
                Text("CUSTO // ${built.creationCost ?: "#"} PH", color = if (allowed) Acid else Signal, style = MaterialTheme.typography.titleMedium)
                Text("PREÇO COMUM // ${built.price} E$", color = Ice)
                Text("CARGA ${built.load} // DURABILIDADE ${built.durability}", color = Muted)
                Text(built.effect, color = Muted, style = MaterialTheme.typography.bodySmall)
                if (!allowed) Text(
                    if (built.creationCost == null) "Item # exige permissão do Historiador." else "Custo acima dos Pontos de Herança restantes.",
                    color = Signal,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (allowed) onAdd(built.toInventoryItem(initialCreation = true)) else confirmOverride = true
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
        onDismiss = { picker = null },
    ) { part ->
        if (picker == "base") {
            base = part
            if (!weapon && part.id == "gibao") {
                val organic = ItemCreationRules.armorMaterials.first { it.id == "organico" }
                parts = parts.map { it.copy(material = organic) }
            }
        } else if (materialPartIndex in parts.indices) {
            parts = parts.toMutableList().also { list ->
                list[materialPartIndex] = list[materialPartIndex].copy(material = part)
            }
        }
        picker = null
        materialPartIndex = -1
    }
    if (confirmOverride) AssistedItemValidationDialog(
        message = "A composição contém custo # e exige uma decisão do Historiador. Deseja adicioná-la mesmo assim?",
        onDismiss = { confirmOverride = false },
    ) {
        onAdd(built.toInventoryItem(initialCreation = true))
        confirmOverride = false
    }
}

@Composable
private fun AssistedItemValidationDialog(message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("VALIDAÇÃO ASSISTIDA") },
        text = { Text(message, color = Muted) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("ADICIONAR MESMO ASSIM", color = Signal) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("VOLTAR") } },
    )
}

@Composable
private fun ItemPartPickerDialog(title: String, parts: List<ItemPart>, onDismiss: () -> Unit, onSelect: (ItemPart) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                parts.forEach { part ->
                    Column(Modifier.fillMaxWidth().clickable { onSelect(part) }.padding(vertical = 10.dp)) {
                        Text(part.name, color = Ice)
                        Text("CRIAÇÃO ${part.creationCost ?: "#"} // ${part.price} E$", color = Acid, style = MaterialTheme.typography.labelSmall)
                        if (part.effect.isNotBlank()) Text(part.effect, color = Muted, style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(color = TechCutDark)
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
internal fun EquipmentGlossaryDialog(onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val needle = query.trim()
        EquipmentGlossary.entries.filter { entry ->
            needle.isEmpty() ||
                entry.term.contains(needle, ignoreCase = true) ||
                entry.group.contains(needle, ignoreCase = true) ||
                entry.definition.contains(needle, ignoreCase = true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GLOSSÁRIO DE EQUIPAMENTOS") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HudTextField("Buscar termo, item ou material", query) { query = it }
                Column(Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                    filtered.forEach { entry ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(entry.term, color = Ice, style = MaterialTheme.typography.titleSmall)
                            Text(entry.group.uppercase(), color = Acid, style = MaterialTheme.typography.labelSmall)
                            Text(entry.definition, color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(color = TechCutDark)
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
        inventory.sortedByDescending { it.matchesEquipmentRegion(regionName) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("EQUIPAR // ${regionName.uppercase()}") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                if (inventory.isEmpty()) Text("Nenhum item pronto no inventário.", color = Muted)
                orderedInventory.forEach { item ->
                    val checked = item.id in selected
                    val compatible = item.matchesEquipmentRegion(regionName)
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            selected = selected.toggle(item.id)
                        }.padding(vertical = 6.dp),
                    ) {
                        Checkbox(checked, onCheckedChange = { value ->
                            selected = if (value) selected + item.id else selected - item.id
                        })
                        Column(Modifier.padding(top = 8.dp)) {
                            Text(item.name.ifBlank { "Item sem nome" }, color = Ice)
                            Text(
                                "PG ${item.pg} // PL ${item.pl}${item.region.takeIf(String::isNotBlank)?.let { " // $it" }.orEmpty()}${if (compatible) " // COMPATÍVEL" else ""}",
                                color = if (compatible) Acid else Muted,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    HorizontalDivider(color = TechCutDark)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("APLICAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

internal fun InventoryItem.matchesEquipmentRegion(bodyRegionName: String): Boolean {
    if (region.isBlank()) return false
    val itemRegion = region.normalizedRegion()
    val bodyRegion = bodyRegionName.normalizedRegion()
    return when {
        bodyRegion.startsWith("pe ") || bodyRegion == "pe" -> "pe" in itemRegion
        bodyRegion.startsWith("mao ") || bodyRegion == "mao" -> "mao" in itemRegion
        bodyRegion.startsWith("braco ") || bodyRegion == "braco" -> "braco" in itemRegion
        bodyRegion.startsWith("perna ") || bodyRegion == "perna" -> "perna" in itemRegion
        else -> bodyRegion in itemRegion || itemRegion in bodyRegion
    }
}

private fun String.normalizedRegion(): String = lowercase()
    .replace('á', 'a').replace('à', 'a').replace('â', 'a').replace('ã', 'a')
    .replace('é', 'e').replace('ê', 'e')
    .replace('í', 'i').replace('ó', 'o').replace('ô', 'o').replace('õ', 'o')
    .replace('ú', 'u').replace('ç', 'c')
    .replace(Regex("\\b(pes|maos|bracos|pernas)\\b")) { match -> match.value.dropLast(1) }

private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value
