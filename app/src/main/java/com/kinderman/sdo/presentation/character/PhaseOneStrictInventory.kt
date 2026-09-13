package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.model.BuiltItem
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.ItemCreationDraft
import com.kinderman.sdo.domain.model.InventoryState
import com.kinderman.sdo.domain.model.inventoryState
import com.kinderman.sdo.domain.model.withInventoryState
import com.kinderman.sdo.domain.model.initialCreationCost
import com.kinderman.sdo.domain.model.durabilityLabel
import com.kinderman.sdo.domain.model.isScrap
import com.kinderman.sdo.domain.model.isBroken
import com.kinderman.sdo.domain.model.participatesInInitialCreation
import com.kinderman.sdo.domain.model.effectiveLoad
import com.kinderman.sdo.domain.model.addInventoryItem
import com.kinderman.sdo.domain.model.withItemInventoryState
import com.kinderman.sdo.domain.model.withRemovedAbility
import com.kinderman.sdo.domain.model.withRemovedAshInventoryItem
import com.kinderman.sdo.domain.model.withAddedAsh
import com.kinderman.sdo.domain.model.recycleBrokenItem
import com.kinderman.sdo.domain.model.AshPurity
import com.kinderman.sdo.domain.model.heritageCostPerDose
import com.kinderman.sdo.domain.model.handsRequired
import com.kinderman.sdo.domain.model.synchronizeItemPowers
import com.kinderman.sdo.domain.catalog.toMysticAbility
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.Carbon
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechPanel

@Composable
internal fun PhaseOneStrictInventorySection(
    character: Character,
    catalog: List<CatalogEntry>,
    enabled: Boolean,
    onChange: (Character) -> Unit,
) {
    var dialog by rememberSaveable(character.id) { mutableStateOf<String?>(null) }
    var inventoryQuery by rememberSaveable(character.id) { mutableStateOf("") }
    var inventoryGroup by rememberSaveable(character.id) { mutableStateOf("Todos") }
    val context = LocalContext.current
    val spentHeritage = character.inventory.sumOf { it.initialCreationCost() }
    val remainingHeritage = if (character.isInCreation) (ItemCreationRules.HERITAGE_BUDGET - spentHeritage).coerceAtLeast(0) else 0
    val itemCatalog = catalog.filter { it.kind == CatalogKind.ITEM }
    val ashCatalog = catalog.filter { it.kind == CatalogKind.ASH }
    val inventoryFilterOptions = remember(character.inventory) {
        listOf("Todos", "Armas", "Armaduras", "Itens") + character.inventory
            .map(InventoryItem::category)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
            .map { "Categoria // $it" }
    }

    TechPanel {
        SectionHeader("10", "Inventário")
        Text(
            "CARGA ${character.currentLoad} / ${character.maximumLoad}",
            color = if (character.currentLoad > character.maximumLoad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
        )
        when {
            character.currentLoad >= character.maximumLoad + 4 -> Text("IMÓVEL // sem Movimento ou Esquiva; abandone Carga", color = MaterialTheme.colorScheme.error)
            character.currentLoad > character.maximumLoad -> Text("SOBRECARREGADO // −5 m, −2 Esquiva, Desvantagem física e corrida +1 PE", color = MaterialTheme.colorScheme.error)
        }
        Text("Efeitos são definidos pelos componentes do item e aplicados automaticamente quando equipado.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        HudTextField("Buscar por nome, categoria, material ou estado", inventoryQuery) { inventoryQuery = it }
        ChoiceField("Grupo", inventoryGroup, inventoryFilterOptions, true) { inventoryGroup = it }

        inventoryGroups(character.inventory).forEach { (group, groupItems) ->
            val visible = groupItems.filter { item ->
                (inventoryGroup == "Todos" || inventoryGroup == group ||
                    inventoryGroup.removePrefix("Categoria // ").takeIf { inventoryGroup.startsWith("Categoria // ") }
                        ?.let { item.category.equals(it, true) } == true) &&
                    (inventoryQuery.isBlank() || listOf(item.name, item.category, item.quality.label, item.effect, item.inventoryState.label).any { it.contains(inventoryQuery, true) })
            }
            if (inventoryGroup == "Todos" || inventoryGroup == group) {
                Text("$group // ${groupItems.size} // CARGA ${groupItems.sumOf { it.effectiveLoad() }}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                if (groupItems.isEmpty()) Text("Nenhum item nesta categoria.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            visible.forEach { item ->
            val index = character.inventory.indexOfFirst { it.id == item.id }
            var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
            val compactDetails = buildList {
                add(item.inventoryState.label.uppercase())
                add("CARGA ${item.effectiveLoad()}")
                if (item.quantity > 1 || item.linkedAshId.isNotBlank() || item.category.equals("Munição", true) || item.category.contains("Consumível", true)) {
                    add("QTD ${item.quantity}")
                }
                add("DUR ${item.durabilityLabel}")
            }.joinToString(" // ")
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name.ifBlank { "ITEM ${(index + 1).toString().padStart(2, '0')}" }, color = MaterialTheme.colorScheme.onSurface)
                        Text(compactDetails, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    }
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Recolher ${item.name}" else "Expandir ${item.name}")
                    RemoveButton(enabled, "Remover item") {
                        runCatching {
                            if (item.linkedAshId.isNotBlank()) character.withRemovedAshInventoryItem(item.id)
                            else character.removeInventoryItem(item.id)
                        }.onSuccess(onChange).onFailure {
                            android.widget.Toast.makeText(context, it.message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Text("${item.category.ifBlank { "OBJETO" }} // ${item.quality.label} // PG ${item.pg} // PL ${item.pl}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                if (!expanded) return@Column
                Text("REGIÃO ${item.region.ifBlank { "—" }} // CARGA ${item.effectiveLoad()} // LA ${item.agilityLimit ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (item.category.contains("arma", true) && !item.category.contains("armadura", true)) Text("EMPUNHADURA // ${item.handsRequired()} MÃO(S)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text("DURABILIDADE ${item.durabilityLabel}${when { item.isBroken -> " // [QUEBRADO]"; item.isScrap -> " // [SUCATA]"; else -> "" }}", color = if (item.isScrap || item.isBroken) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.isScrap && item.category.contains("arma", true)) Text(
                    "SUCATA // DESVANTAGEM NO ATAQUE // DADO DE DANO −1 CATEGORIA // BÔNUS E MODIFICAÇÕES INATIVOS",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
                if (item.isScrap && (item.pg != 0 || item.pl != 0)) Text(
                    "SUCATA // PG ${item.pg / 2} // PL ${item.pl / 2}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
                if (item.isBroken) {
                    Text("QUEBRADO // sem efeitos mecânicos e sem reparo normal", color = MaterialTheme.colorScheme.error)
                    TextButton(
                        onClick = { onChange(character.recycleBrokenItem(item.id)) },
                        enabled = enabled,
                    ) { Text("RECICLAR") }
                }
                ItemCreationRules.run {
                    val materialName = (weaponMaterials + armorMaterials).firstOrNull { it.id == item.materialId }?.name
                    val modificationNames = (weaponModifications + armorModifications).filter { it.id in item.modificationIds }.map { it.name }
                    val gemNames = gemComponents.filter { it.id in item.gemIds }.map { it.name }
                    val technologyNames = technologyComponents.filter { it.id in item.technologyIds }.map { it.name }
                    materialName?.let { Text("MATERIAL // ${it.uppercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                    if (modificationNames.isNotEmpty()) Text("MODIFICAÇÕES // ${modificationNames.joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text("ESPAÇOS // GEMAS ${item.gemIds.size}/${item.gemSlots} // TECNOLOGIA ${item.technologyIds.size}/${item.technologySlots}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    if (gemNames.isNotEmpty()) Text("GEMAS // ${gemNames.joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    if (technologyNames.isNotEmpty()) Text("TECNOLOGIAS // ${technologyNames.joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    fun updateComponents(gemIds: List<String> = item.gemIds, technologyIds: List<String> = item.technologyIds) {
                        val componentEffectIds = (gemComponents.map { it.id } + technologyComponents.map { it.id }).toSet()
                        val updated = item.copy(
                            gemIds = gemIds,
                            technologyIds = technologyIds,
                            mechanicalEffects = (
                                item.mechanicalEffects.filterNot { it.id in componentEffectIds } +
                                    componentEffects(item.modificationIds, gemIds, technologyIds)
                                ).distinctBy(ItemEffect::id),
                        )
                        onChange(character.copy(inventory = character.inventory.replace(index, updated)).synchronizeItemPowers())
                    }
                    val availableGems = gemComponents.filterNot { it.id in item.gemIds }
                    if (enabled && item.gemIds.size < item.gemSlots && availableGems.isNotEmpty()) {
                        ChoiceField("Instalar gema", "", availableGems.map { it.id }, true, display = { id -> availableGems.firstOrNull { it.id == id }?.name ?: "Selecionar" }) { id ->
                            updateComponents(gemIds = item.gemIds + id)
                        }
                    }
                    item.gemIds.forEach { id ->
                        gemComponents.firstOrNull { it.id == id }?.let { gem ->
                            TextButton(onClick = { updateComponents(gemIds = item.gemIds - id) }, enabled = enabled) { Text("REMOVER GEMA // ${gem.name.uppercase()}") }
                        }
                    }
                    val availableTechnologies = technologyComponents.filterNot { it.id in item.technologyIds }
                    if (enabled && item.technologyIds.size < item.technologySlots && availableTechnologies.isNotEmpty()) {
                        ChoiceField("Instalar tecnologia", "", availableTechnologies.map { it.id }, true, display = { id -> availableTechnologies.firstOrNull { it.id == id }?.name ?: "Selecionar" }) { id ->
                            updateComponents(technologyIds = item.technologyIds + id)
                        }
                    }
                    item.technologyIds.forEach { id ->
                        technologyComponents.firstOrNull { it.id == id }?.let { technology ->
                            TextButton(onClick = { updateComponents(technologyIds = item.technologyIds - id) }, enabled = enabled) { Text("REMOVER TECNOLOGIA // ${technology.name.uppercase()}") }
                        }
                    }
                }
                if (item.linkedAshId.isNotBlank()) {
                    IntegerField("Doses", item.quantity, enabled) { doses ->
                        onChange(character.copy(inventory = character.inventory.replace(index, item.copy(quantity = doses.coerceAtLeast(0)))))
                    }
                    Text("PUREZA // ${item.ashPurity.label}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                } else if (item.category.equals("Munição", true) || item.category.contains("Consumível", true)) {
                    IntegerField(if (item.category.equals("Munição", true)) "Munição restante" else "Doses restantes", item.quantity, enabled) { quantity ->
                        onChange(character.copy(inventory = character.inventory.replace(index, item.copy(quantity = quantity.coerceAtLeast(0)))))
                    }
                }
                item.mechanicalEffects.forEach { effect ->
                    Text(effect.presentationLabel(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
                val states = validItemStates(character, item)
                ChoiceField("Estado", item.inventoryState, states, enabled, display = InventoryState::label) { value ->
                    onChange(character.withItemInventoryState(item.id, value))
                }
                if (item.canonical) Text(item.effect, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else {
                    HudTextField("Nome", item.name, enabled = enabled) { value ->
                        onChange(character.copy(inventory = character.inventory.replace(index, item.copy(name = value))))
                    }
                    HudTextField("Descrição", item.effect, multiline = true, enabled = enabled) { value ->
                        onChange(character.copy(inventory = character.inventory.replace(index, item.copy(effect = value))))
                    }
                }
            }
            }
        }

        AddButton("Adicionar item", enabled) { dialog = "add" }
        TextButton(onClick = { dialog = "glossary" }, modifier = Modifier.fillMaxWidth()) { Text("CONSULTAR GLOSSÁRIOS") }
        if (remainingHeritage > 0) {
            Text("CRIAÇÃO INICIAL // $remainingHeritage / ${ItemCreationRules.HERITAGE_BUDGET} PH RESTANTES", color = MaterialTheme.colorScheme.primary)
        }
    }

    when (dialog) {
        "add" -> AddInventoryChoiceDialog(
            initialCreation = character.isInCreation,
            canUseCatalog = itemCatalog.isNotEmpty(),
            canAddAsh = ashCatalog.isNotEmpty(),
            onDismiss = { dialog = null },
            onChoice = { choice ->
                onChange(character.copy(itemCreationDraft = null))
                dialog = choice
            },
        )
        "glossary" -> EquipmentGlossaryDialog(
            onDismiss = { dialog = null },
            onUse = { entry ->
                onChange(character.addInventoryItem(InventoryItem(name = entry.term, category = if (entry.section == "Armas") "Arma" else "Item", effect = entry.definition)))
                dialog = null
            },
        )
        "initial_catalog" -> ItemCatalogDialog(
            title = "LOJA INICIAL // ITENS PRONTOS",
            entries = itemCatalog,
            remainingHeritage = remainingHeritage,
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.addInventoryItem(item))
            dialog = null
        }
        "catalog" -> ItemCatalogDialog(
            title = "CATÁLOGO DE ITENS",
            entries = itemCatalog,
            remainingHeritage = null,
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.addInventoryItem(item))
            dialog = null
        }
        "initial_weapon", "initial_armor", "initial_accessory" -> StrictItemBuilderDialog(
            remainingHeritage = remainingHeritage,
            regionOptions = character.bodyRegions.map { it.name },
            draft = (character.itemCreationDraft ?: ItemCreationDraft()).copy(category = when (dialog) {
                "initial_weapon" -> "Arma"
                "initial_accessory" -> "Acessório"
                else -> "Armadura"
            }),
            onDraftChange = { onChange(character.copy(itemCreationDraft = it)) },
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.addInventoryItem(item).copy(itemCreationDraft = null))
            dialog = null
        }
        "builder_weapon", "builder_armor", "builder_accessory", "builder_item" -> StrictItemBuilderDialog(
            remainingHeritage = null,
            regionOptions = character.bodyRegions.map { it.name },
            draft = (character.itemCreationDraft ?: ItemCreationDraft()).copy(category = when (dialog) {
                "builder_weapon" -> "Arma"
                "builder_armor" -> "Armadura"
                "builder_accessory" -> "Acessório"
                else -> "Item"
            }),
            onDraftChange = { onChange(character.copy(itemCreationDraft = it)) },
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.addInventoryItem(item).copy(itemCreationDraft = null))
            dialog = null
        }
        "ash_builder" -> AshBuilderDialog(
            entries = ashCatalog,
            remainingHeritage = remainingHeritage.takeIf { character.isInCreation },
            onDismiss = { dialog = null },
        ) { entry, doses ->
            onChange(character.withAddedAsh(entry.toMysticAbility(), doses, character.isInCreation))
            dialog = null
        }
        "narrative" -> {
            onChange(character.addInventoryItem(InventoryItem(category = "Item", quantity = 1)))
            dialog = null
        }
    }
}

@Composable
private fun AddInventoryChoiceDialog(initialCreation: Boolean, canUseCatalog: Boolean, canAddAsh: Boolean, onDismiss: () -> Unit, onChoice: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ADICIONAR AO INVENTÁRIO") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Escolha como o item será adicionado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                AddButton("Do catálogo", canUseCatalog) { onChoice(if (initialCreation) "initial_catalog" else "catalog") }
                AddButton("Construir arma", true) { onChoice(if (initialCreation) "initial_weapon" else "builder_weapon") }
                AddButton("Construir armadura", true) { onChoice(if (initialCreation) "initial_armor" else "builder_armor") }
                AddButton("Construir acessório", true) { onChoice(if (initialCreation) "initial_accessory" else "builder_accessory") }
                if (!initialCreation) AddButton("Construir item comum", true) { onChoice("builder_item") }
                if (canAddAsh) AddButton("Preparar Cinzas", true) { onChoice("ash_builder") }
                if (!initialCreation) TextButton(onClick = { onChoice("narrative") }, modifier = Modifier.fillMaxWidth()) { Text("ADICIONAR ITEM NARRATIVO") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun AshBuilderDialog(
    entries: List<CatalogEntry>,
    remainingHeritage: Int?,
    onDismiss: () -> Unit,
    onAdd: (CatalogEntry, Int) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf("Todas") }
    var selectedAshName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf("") }
    var doses by rememberSaveable { mutableIntStateOf(1) }
    val sources = listOf("Todas") + entries.mapNotNull { it.catalogAshSource?.label }.distinct().sorted()
    val grouped = remember(entries) { entries.groupBy(CatalogEntry::name).toSortedMap() }
    val filteredGroups = grouped.filterValues { variants ->
        (source == "Todas" || variants.any { it.catalogAshSource?.label == source }) &&
            (query.isBlank() || variants.any { it.searchableText().contains(query, ignoreCase = true) })
    }
    val selectedVariants = selectedAshName?.let(grouped::get).orEmpty().sortedBy { entry ->
        AshPurity.entries.indexOf(entry.catalogAshPurity ?: AshPurity.RAW)
    }
    val selected = selectedVariants.firstOrNull { it.id == selectedId }
    val purity = selected?.catalogAshPurity ?: AshPurity.RAW
    val preview = doses.coerceAtLeast(1) * purity.heritageCostPerDose
    val allowed = selected != null && (remainingHeritage == null || preview <= remainingHeritage)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(selectedAshName?.uppercase() ?: "PREPARAR CINZAS") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                remainingHeritage?.let { remaining ->
                    val spent = ItemCreationRules.HERITAGE_BUDGET - remaining
                    Text(
                        if (selected == null) "HERANÇA // $spent GASTOS // $remaining RESTANTES"
                        else "HERANÇA // $spent GASTOS + $preview PREVIEW // ${remaining - preview} RESTANTES",
                        color = if (selected == null || allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    LinearProgressIndicator(
                        progress = { ((spent + if (selected != null) preview else 0).toFloat() / ItemCreationRules.HERITAGE_BUDGET).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (selectedAshName == null) {
                    HudTextField("Buscar por nome, fonte ou efeito", query) { query = it }
                    ChoiceField("Fonte", source, sources, true) { source = it }
                    Text("CINZAS // ${filteredGroups.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        filteredGroups.forEach { (name, variants) ->
                            Column(
                                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).clickable {
                                    selectedAshName = name
                                    selectedId = variants.firstOrNull { it.catalogAshPurity == AshPurity.RAW }?.id
                                        ?: variants.firstOrNull()?.id.orEmpty()
                                }.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(name, color = Ice, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${variants.firstOrNull()?.catalogAshSource?.label ?: "Fonte desconhecida"} // ${variants.size} PUREZAS",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                } else {
                    Text(selectedVariants.firstOrNull()?.catalogAshSource?.label ?: "Fonte desconhecida", color = MaterialTheme.colorScheme.primary)
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        selectedVariants.forEach { entry ->
                            val entryPurity = entry.catalogAshPurity ?: AshPurity.RAW
                            val isSelected = entry.id == selectedId
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = .14f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedId = entry.id }
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(entryPurity.label.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                                    if (isSelected) Text("SELECIONADA", color = Signal, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(entry.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${entryPurity.dosesPerLoad} DOSE(S) POR CARGA" + if (remainingHeritage != null) " // ${entryPurity.heritageCostPerDose} PH POR DOSE" else "",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                    IntegerField("Doses", doses, true) { doses = it.coerceAtLeast(1) }
                    selected?.let {
                        Text("CARGA ${kotlin.math.ceil(doses.toDouble() / purity.dosesPerLoad).toInt()} // ${purity.dosesPerLoad} DOSE(S) POR CARGA")
                        if (remainingHeritage != null) Text("CUSTO // $preview PH", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            if (selectedAshName != null) TextButton(onClick = { selected?.let { onAdd(it, doses) } }, enabled = allowed) {
                Text(if (allowed) "ADICIONAR" else "SALDO INSUFICIENTE")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (selectedAshName != null) {
                    selectedAshName = null
                    selectedId = ""
                } else onDismiss()
            }) { Text(if (selectedAshName != null) "VOLTAR" else "CANCELAR") }
        },
    )
}

private fun inventoryGroups(items: List<InventoryItem>): Map<String, List<InventoryItem>> = linkedMapOf(
    "Armas" to items.filter { it.category.contains("arma", true) && !it.category.contains("armadura", true) },
    "Armaduras" to items.filter { it.category.contains("armadura", true) || it.category.contains("acessório", true) },
    "Itens" to items.filterNot { it.category.contains("arma", true) },
).let { groups -> groups + ("Itens" to groups.getValue("Itens").filterNot { it.category.contains("armadura", true) || it.category.contains("acessório", true) }) }

private fun validItemStates(character: Character, item: InventoryItem): List<InventoryState> = buildList {
    if (item.category.contains("arma", true) && !item.category.contains("armadura", true)) add(InventoryState.WIELDED)
    if (item.category.contains("armadura", true) || item.category.contains("acessório", true) || item.category.equals("Recipiente de Carga", true) || item.backpackCapacity > 0) add(InventoryState.EQUIPPED)
    val quickAccessCount = character.inventory.count { it.inventoryState == InventoryState.QUICK_ACCESS && it.id != item.id }
    if (item.effectiveLoad() <= 1 && (item.inventoryState == InventoryState.QUICK_ACCESS || quickAccessCount < 2)) {
        add(InventoryState.QUICK_ACCESS)
    }
    if (character.backpackCapacity > 0) add(InventoryState.BACKPACK)
    add(InventoryState.STORED)
}

@Composable
private fun StrictItemBuilderDialog(
    remainingHeritage: Int?,
    regionOptions: List<String>,
    draft: ItemCreationDraft,
    onDraftChange: (ItemCreationDraft) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (InventoryItem) -> Unit,
) {
    var componentQuery by rememberSaveable { mutableStateOf("") }
    val step = draft.step
    val category = draft.category
    val weapon = category == "Arma"
    val basePool: List<ItemPart> = when (category) {
        "Arma" -> ItemCreationRules.weaponBases
        "Acessório" -> ItemCreationRules.armorBases.filter { it.group == "Acessório" }
        else -> ItemCreationRules.armorBases.filterNot { it.group == "Acessório" }
    }
    val base: ItemPart = basePool.firstOrNull { it.id == draft.baseId } ?: basePool.first()
    val materialPool: List<ItemPart> = if (weapon) ItemCreationRules.weaponMaterials else ItemCreationRules.armorMaterials
    val material: ItemPart = materialPool.firstOrNull { it.id == draft.materialId }
        ?: materialPool.firstOrNull { it.id == if (!weapon && base.id == "gibao") "organico" else "ligas_comuns" }
        ?: materialPool.first()
    val modifications: List<ItemPart> = (ItemCreationRules.weaponModifications + ItemCreationRules.armorModifications)
        .filter { it.id in draft.modificationIds }
    val gemSlots = draft.gemSlots
    val technologySlots = draft.technologySlots
    val customName = draft.customName
    val quality = draft.quality
    val gems: List<ItemPart> = ItemCreationRules.gemComponents.filter { it.id in draft.gemIds }
    val technologies: List<ItemPart> = ItemCreationRules.technologyComponents.filter { it.id in draft.technologyIds }
    val commonName = draft.commonName
    val commonEffect = draft.commonEffect
    val commonLoad = draft.commonLoad
    val commonQuantity = draft.commonQuantity
    val commonCategory = draft.commonCategory
    val totalSteps = if (category == "Item") 2 else 6
    val stepTitle = if (category == "Item") listOf("DADOS", "REVISÃO")[step - 1]
        else listOf("BASE", "MATERIAL", "QUALIDADE", "MODIFICAÇÕES", "GEMAS", "REVISÃO")[step - 1]

    val initialCreation = remainingHeritage != null
    val bases: List<ItemPart> = basePool
    val materials: List<ItemPart> = when {
        weapon -> ItemCreationRules.weaponMaterials
        base.id == "gibao" -> ItemCreationRules.armorMaterials.filter { it.id == "organico" }
        else -> ItemCreationRules.armorMaterials
    }.let { list -> if (initialCreation) list.filter { it.creationCost != null } else list }
    val availableModifications: List<ItemPart> = ItemCreationRules.compatibleModifications(base, weapon)
    val built: BuiltItem = ItemCreationRules.build(
        base = base,
        material = material,
        modifications = modifications,
        gemSlots = gemSlots,
        technologySlots = technologySlots,
        customName = customName,
        quality = quality,
        components = gems,
        technologies = technologies,
    )
    val allowedByBudget = remainingHeritage == null || (built.creationCost != null && built.creationCost <= remainingHeritage)
    val commonEffects = buildList {
        if (draft.commonPg != 0) add(ItemEffect("custom:pg", ItemEffectType.PG, draft.commonPg, condition = ItemEffectCondition.EQUIPPED))
        if (draft.commonPl != 0) add(ItemEffect("custom:pl", ItemEffectType.PL, draft.commonPl, target = draft.commonRegion, condition = ItemEffectCondition.EQUIPPED))
        draft.commonAgilityLimit?.let { add(ItemEffect("custom:la", ItemEffectType.AGILITY_LIMIT, it, condition = ItemEffectCondition.EQUIPPED)) }
        if (draft.commonAttack != 0) add(ItemEffect("custom:attack", ItemEffectType.ATTACK, draft.commonAttack, condition = ItemEffectCondition.WIELDED))
        if (draft.commonDamage != 0) add(ItemEffect("custom:damage", ItemEffectType.PHYSICAL_DAMAGE, draft.commonDamage, condition = ItemEffectCondition.WIELDED))
    }
    val commonItem = InventoryItem(
        name = commonName.trim(), category = commonCategory, effect = commonEffect,
        load = commonLoad, quantity = commonQuantity, region = draft.commonRegion,
        durabilityCurrent = draft.commonDurability.coerceAtLeast(1), durabilityMax = draft.commonDurability.coerceAtLeast(1),
        pg = draft.commonPg, pl = draft.commonPl, agilityLimit = draft.commonAgilityLimit, quality = quality,
        mechanicalEffects = commonEffects,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (initialCreation) "CRIAR ITEM // $remainingHeritage PH" else "CRIAR ITEM")
                Text("$step DE $totalSteps  //  $stepTitle", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                remainingHeritage?.let { remaining ->
                    val spent = ItemCreationRules.HERITAGE_BUDGET - remaining
                    val preview = if (category == "Item") 0 else built.creationCost ?: 0
                    Text("HERANÇA // $spent GASTOS + $preview PREVIEW // ${remaining - preview} RESTANTES", color = MaterialTheme.colorScheme.primary)
                    LinearProgressIndicator(
                        progress = { ((spent + preview).toFloat() / ItemCreationRules.HERITAGE_BUDGET).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (step == 1 && category == "Item") {
                    ChoiceField("Categoria", commonCategory, listOf("Arma", "Armadura", "Acessório", "Escudo", "Consumível", "Munição", "Ferramenta", "Recipiente de Carga", "Item"), true) { onDraftChange(draft.copy(commonCategory = it)) }
                    ChoiceField("Qualidade", quality, ItemQuality.entries, true, display = { it.label }) { onDraftChange(draft.copy(quality = it)) }
                    HudTextField("Nome do item", commonName) { onDraftChange(draft.copy(commonName = it)) }
                    TwoFields(
                        { IntegerField("Quantidade", commonQuantity, true, it) { value -> onDraftChange(draft.copy(commonQuantity = value.coerceAtLeast(1))) } },
                        { IntegerField("Carga total", commonLoad, true, it) { value -> onDraftChange(draft.copy(commonLoad = value.coerceAtLeast(0))) } },
                    )
                    HudTextField("Descrição ou efeito", commonEffect, multiline = true) { onDraftChange(draft.copy(commonEffect = it)) }
                    val regions = listOf("Nenhuma") + regionOptions.distinct()
                    ChoiceField("Região corporal", draft.commonRegion.ifBlank { "Nenhuma" }, regions, true) {
                        onDraftChange(draft.copy(commonRegion = it.takeUnless { value -> value == "Nenhuma" }.orEmpty()))
                    }
                    TwoFields(
                        { IntegerField("Durabilidade", draft.commonDurability.coerceAtLeast(1), true, it) { value -> onDraftChange(draft.copy(commonDurability = value.coerceAtLeast(1))) } },
                        { IntegerField("Alcance", draft.commonRange, true, it) { value -> onDraftChange(draft.copy(commonRange = value.coerceAtLeast(0))) } },
                    )
                    TwoFields(
                        { IntegerField("PG", draft.commonPg, true, it) { value -> onDraftChange(draft.copy(commonPg = value)) } },
                        { IntegerField("PL", draft.commonPl, true, it) { value -> onDraftChange(draft.copy(commonPl = value)) } },
                    )
                    TwoFields(
                        { IntegerField("Ataque", draft.commonAttack, true, it) { value -> onDraftChange(draft.copy(commonAttack = value)) } },
                        { IntegerField("Dano", draft.commonDamage, true, it) { value -> onDraftChange(draft.copy(commonDamage = value)) } },
                    )
                }
                if (step == 1 && category != "Item") {
                    HudTextField("Nome personalizado", customName) { onDraftChange(draft.copy(customName = it)) }
                    ChoiceField<String>("Tipo", base.id, bases.map { it.id }, true, display = { id: String -> bases.first { it.id == id }.name }) { id: String ->
                        val selected = bases.first { it.id == id }
                        val nextMaterialId = if (!weapon && selected.id == "gibao") "organico" else draft.materialId
                        onDraftChange(draft.copy(baseId = id, materialId = nextMaterialId, modificationIds = modifications.filter { it in ItemCreationRules.compatibleModifications(selected, weapon) }.map { it.id }))
                    }
                    ItemPartDetails("DETALHES DO TIPO", base, weapon, initialCreation)
                }
                if (step == 2 && category != "Item") {
                    ChoiceField<String>("Material", material.id, materials.map { it.id }, true, display = { id: String -> materials.first { it.id == id }.name }) { id: String -> onDraftChange(draft.copy(materialId = id)) }
                    ItemPartDetails("DETALHES DO MATERIAL", material, weapon, initialCreation)
                }
                if (step == 3 && category != "Item") {
                    ChoiceField<String>("Qualidade", quality.name, ItemQuality.entries.map { it.name }, true, display = { name: String -> ItemQuality.valueOf(name).label }) { name: String -> onDraftChange(draft.copy(quality = ItemQuality.valueOf(name))) }
                    QualityDetails(quality, weapon, initialCreation)
                    if (initialCreation) Text(
                        "CUSTO ATUAL // ${built.creationCost} PH // SALDO ${remainingHeritage - (built.creationCost ?: 0)}",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (step == 4 && category != "Item") {
                    Text("MODIFICAÇÕES", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    HudTextField("Filtrar por nome, grupo ou efeito", componentQuery) { componentQuery = it }
                    availableModifications.filter { modification ->
                        componentQuery.isBlank() || listOf(modification.name, modification.group, modification.effect).any { it.contains(componentQuery, true) }
                    }.groupBy { it.group }.forEach { (group, groupModifications) ->
                        Text(group.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        groupModifications.forEach { modification: ItemPart ->
                        val checked = modification in modifications
                        Row(Modifier.fillMaxWidth().clickable { onDraftChange(draft.copy(modificationIds = strictToggleModification(modifications, modification).map { it.id })) }) {
                            Checkbox(checked, onCheckedChange = { onDraftChange(draft.copy(modificationIds = strictToggleModification(modifications, modification).map { it.id })) })
                            Column(Modifier.padding(top = 8.dp)) {
                                Text(modification.name, color = MaterialTheme.colorScheme.onSurface)
                                if (modification.effect.isNotBlank()) Text(modification.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                ItemPartStats(modification, initialCreation)
                            }
                        }
                    }
                    }
                }
                if (step == 5 && category != "Item") {
                    TwoFields(
                        { IntegerField("Espaços de Gema", gemSlots, true, it) { value -> onDraftChange(draft.copy(gemSlots = value.coerceIn(gems.size, 5))) } },
                        { IntegerField("Espaços de Tecnologia", technologySlots, true, it) { value -> onDraftChange(draft.copy(technologySlots = value.coerceIn(technologies.size, 5))) } },
                    )
                    val availableGems: List<ItemPart> = ItemCreationRules.gemComponents.filterNot { candidate: ItemPart -> gems.any { it.id == candidate.id } }
                    if (quality != ItemQuality.MUNDANE && gems.size < gemSlots && availableGems.isNotEmpty()) {
                        ChoiceField<String>("Adicionar gema", "", availableGems.map { it.id }, true, display = { id: String -> availableGems.firstOrNull { it.id == id }?.let { "${it.name} // ${it.effect}" } ?: "Selecionar" }) { id: String ->
                            onDraftChange(draft.copy(gemIds = draft.gemIds + id))
                        }
                    }
                    gems.forEach { gem: ItemPart ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(gem.name)
                                Text(gem.effect, style = MaterialTheme.typography.bodySmall)
                                ItemPartStats(gem, initialCreation)
                            }
                            RemoveButton(true, "Remover ${gem.name}") { onDraftChange(draft.copy(gemIds = draft.gemIds - gem.id)) }
                        }
                    }
                    val availableTechnologies = ItemCreationRules.technologyComponents.filterNot { candidate -> technologies.any { it.id == candidate.id } }
                    if (quality != ItemQuality.MUNDANE && technologies.size < technologySlots && availableTechnologies.isNotEmpty()) {
                        ChoiceField<String>("Adicionar tecnologia", "", availableTechnologies.map { it.id }, true, display = { id ->
                            availableTechnologies.firstOrNull { it.id == id }?.let { "${it.name} // ${it.effect}" } ?: "Selecionar"
                        }) { id -> onDraftChange(draft.copy(technologyIds = draft.technologyIds + id)) }
                    }
                    technologies.forEach { technology ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(technology.name)
                                Text(technology.effect, style = MaterialTheme.typography.bodySmall)
                                ItemPartStats(technology, initialCreation)
                            }
                            RemoveButton(true, "Remover ${technology.name}") {
                                onDraftChange(draft.copy(technologyIds = draft.technologyIds - technology.id))
                            }
                        }
                    }
                }
                if (step == totalSteps) {
                Text("REVISE ANTES DE CRIAR", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(if (category == "Item") commonName.ifBlank { "Item sem nome" } else built.name, style = MaterialTheme.typography.titleLarge)
                if (category == "Item") Text("Item comum // Quantidade $commonQuantity // Carga $commonLoad")
                else Text("${if (weapon) "Arma" else "Armadura / Acessório"} // ${material.name} // ${quality.label}")
                if (weapon) Text("EMPUNHADURA // ${built.toInventoryItem().handsRequired()} MÃO(S)")
                if (modifications.isNotEmpty()) Text("MODIFICAÇÕES // ${modifications.joinToString { it.name }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (gems.isNotEmpty()) Text("GEMAS // ${gems.joinToString { it.name }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (technologies.isNotEmpty()) Text("TECNOLOGIAS // ${technologies.joinToString { it.name }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (category != "Item") {
                    val selectedEffects = buildList {
                        add(base.effect)
                        add(material.effect)
                        ItemCreationRules.qualityEffect(quality, armor = !weapon)?.let(::add)
                        modifications.mapTo(this) { "${it.name}: ${it.effect}" }
                        gems.mapTo(this) { "${it.name}: ${it.effect}" }
                        technologies.mapTo(this) { "${it.name}: ${it.effect}" }
                    }.filter(String::isNotBlank).distinct()
                    if (selectedEffects.isNotEmpty()) {
                        Text("EFEITOS ESCOLHIDOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        selectedEffects.forEach { effect -> Text("• $effect", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                    }
                    if (initialCreation) Text("CUSTO // ${built.creationCost ?: "#"} PH", color = MaterialTheme.colorScheme.primary)
                    Text("PG ${built.pg} // PL ${built.pl}", color = MaterialTheme.colorScheme.onSurface)
                    Text("CARGA ${built.load} // DURABILIDADE ${built.durability}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    built.agilityLimit?.let { Text("LIMITE DE AGILIDADE // $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (built.region.isNotBlank()) Text("REGIÃO // ${built.region}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!allowedByBudget) Text("Custo acima dos PH restantes ou item # não disponível na criação inicial.", color = MaterialTheme.colorScheme.error)
                }
            }
            }
        },
        confirmButton = {
            TextButton(
                enabled = step < totalSteps || if (category == "Item") commonName.isNotBlank() else allowedByBudget,
                onClick = { if (step < totalSteps) onDraftChange(draft.copy(step = step + 1)) else onAdd(if (category == "Item") commonItem else built.toInventoryItem(initialCreation = initialCreation)) },
            ) { Text(if (step < totalSteps) "CONTINUAR" else "CRIAR ITEM") }
        },
        dismissButton = { TextButton(onClick = { if (step > 1) onDraftChange(draft.copy(step = step - 1)) else onDismiss() }) { Text(if (step > 1) "VOLTAR" else "CANCELAR") } },
    )
}

@Composable
private fun ItemPartDetails(title: String, part: ItemPart, weapon: Boolean, showHeritageCost: Boolean) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text(part.effect.ifBlank { "Sem efeito adicional." }, color = MaterialTheme.colorScheme.onSurface)
        ItemPartStats(part, showHeritageCost, includeProtection = false)
        if (!weapon) {
            Text("PROTEÇÃO // PG ${part.pg} // PL ${part.pl}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            part.agilityLimit?.let { limit ->
                Text("LIMITE DE AGILIDADE // $limit", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (part.region.isNotBlank()) Text("REGIÃO // ${part.region}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ItemPartStats(part: ItemPart, showHeritageCost: Boolean, includeProtection: Boolean = true) {
    val load = if (part.load > 0) "+${part.load}" else part.load.toString()
    Text(
        buildList {
            if (part.durability != 0) add("DURABILIDADE ${part.durability}")
            if (part.load != 0) add("CARGA $load")
            if (includeProtection && part.pg != 0) add("PG ${signed(part.pg)}")
            if (includeProtection && part.pl != 0) add("PL ${signed(part.pl)}")
            if (includeProtection) part.agilityLimit?.let { add("LA $it") }
        }.ifEmpty { listOf("SEM AJUSTES NUMÉRICOS") }.joinToString(" // "),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    if (showHeritageCost) Text("CUSTO // ${part.creationCost ?: "#"} PH", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun QualityDetails(quality: ItemQuality, weapon: Boolean, showHeritageCost: Boolean) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("EFEITO DA QUALIDADE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text(ItemCreationRules.qualityEffect(quality, armor = !weapon) ?: "Funcionamento padrão, sem bônus adicionais.")
        if (showHeritageCost) Text("AJUSTE DE CUSTO // ${signed(quality.creationAdjustment)} PH", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    }
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

internal fun ItemEffect.presentationLabel(): String {
    if (type == ItemEffectType.RULE) return description.ifBlank { "Regra especial" }
    val typeLabel = when (type) {
        ItemEffectType.ATTRIBUTE -> "Atributo"
        ItemEffectType.KNOWLEDGE -> "Conhecimento"
        ItemEffectType.ATTACK -> "Ataque"
        ItemEffectType.PHYSICAL_DAMAGE -> "Dano físico"
        ItemEffectType.MAGIC_DAMAGE -> "Dano mágico"
        ItemEffectType.PG -> "Proteção geral"
        ItemEffectType.PL -> "Proteção local"
        ItemEffectType.AGILITY_LIMIT -> "Limite de Agilidade"
        ItemEffectType.DURABILITY -> "Durabilidade"
        ItemEffectType.GEM_POWER -> "Poder de gema"
        ItemEffectType.RULE -> error("handled above")
    }
    val target = resolvedTargetId.ifBlank { target }.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
    return "$typeLabel ${if (value > 0) "+" else ""}$value$target"
}

private fun strictToggleModification(current: List<ItemPart>, item: ItemPart): List<ItemPart> {
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
