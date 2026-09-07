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
import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.toInventoryItem
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechCutDark

@Composable
internal fun ItemCatalogDialog(
    title: String,
    entries: List<CatalogEntry>,
    remainingHeritage: Int?,
    onDismiss: () -> Unit,
    onSelect: (InventoryItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
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
                        Column(
                            Modifier.fillMaxWidth().clickable(enabled = allowed) {
                                onSelect(entry.toInventoryItem(initialCreation = remainingHeritage != null))
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
}

@Composable
internal fun ItemBuilderDialog(
    remainingHeritage: Int,
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
    var picker by remember { mutableStateOf<String?>(null) }
    val availableBases = if (weapon) ItemCreationRules.weaponBases else ItemCreationRules.armorBases
    val availableMaterials = when {
        weapon -> ItemCreationRules.weaponMaterials
        base.id == "gibao" -> ItemCreationRules.armorMaterials.filter { it.id == "organico" }
        else -> ItemCreationRules.armorMaterials
    }
    val availableModifications = if (weapon) ItemCreationRules.weaponModifications else ItemCreationRules.armorModifications
    val built = ItemCreationRules.build(base, material, modifications, gemSlots, technologySlots, customName)
    val allowed = built.creationCost != null && built.creationCost <= remainingHeritage

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
                TextButton(onClick = { picker = "material" }, modifier = Modifier.fillMaxWidth()) { Text("MATERIAL // ${material.name}") }
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
        confirmButton = { TextButton(onClick = { onAdd(built.toInventoryItem(initialCreation = true)) }, enabled = allowed) { Text("ADICIONAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )

    if (picker != null) ItemPartPickerDialog(
        title = if (picker == "base") "SELECIONAR TIPO" else "SELECIONAR MATERIAL",
        parts = if (picker == "base") availableBases else availableMaterials,
        onDismiss = { picker = null },
    ) { part ->
        if (picker == "base") {
            base = part
            if (!weapon && part.id == "gibao") material = ItemCreationRules.armorMaterials.first { it.id == "organico" }
        } else material = part
        picker = null
    }
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
