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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.ItemCreationDraft
import com.kinderman.sdo.domain.model.InventoryState
import com.kinderman.sdo.domain.model.inventoryState
import com.kinderman.sdo.domain.model.withInventoryState
import com.kinderman.sdo.domain.model.initialCreationCost
import com.kinderman.sdo.domain.model.participatesInInitialCreation
import com.kinderman.sdo.domain.model.effectiveLoad
import com.kinderman.sdo.domain.model.addInventoryItem
import com.kinderman.sdo.domain.model.withItemInventoryState
import com.kinderman.sdo.domain.model.withRemovedAbility
import com.kinderman.sdo.domain.model.withAddedAbility
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

    TechPanel {
        SectionHeader("09", "Inventário")
        Text(
            "CARGA ${character.currentLoad} / ${character.maximumLoad}",
            color = if (character.currentLoad > character.maximumLoad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
        )
        Text("Efeitos são definidos pelos componentes do item e aplicados automaticamente quando equipado.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        IntegerField("Capacidade do recipiente equipado", character.containerCapacity, enabled) {
            onChange(character.copy(containerCapacity = it.coerceAtLeast(0)))
        }
        HudTextField("Buscar por nome, categoria, material ou estado", inventoryQuery) { inventoryQuery = it }
        ChoiceField("Grupo", inventoryGroup, listOf("Todos", "Armas", "Armaduras", "Itens"), true) { inventoryGroup = it }

        inventoryGroups(character.inventory).forEach { (group, groupItems) ->
            val visible = groupItems.filter { item ->
                (inventoryGroup == "Todos" || inventoryGroup == group) &&
                    (inventoryQuery.isBlank() || listOf(item.name, item.category, item.quality, item.effect, item.inventoryState.label).any { it.contains(inventoryQuery, true) })
            }
            if (inventoryGroup == "Todos" || inventoryGroup == group) {
                Text("$group // ${groupItems.size} // CARGA ${groupItems.sumOf { it.effectiveLoad() }}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                if (groupItems.isEmpty()) Text("Nenhum item nesta categoria.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            visible.forEach { item ->
            val index = character.inventory.indexOfFirst { it.id == item.id }
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text(item.name.ifBlank { "ITEM ${(index + 1).toString().padStart(2, '0')}" }, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    RemoveButton(enabled, "Remover item") {
                        runCatching {
                            if (item.linkedAshId.isNotBlank()) character.withRemovedAbility(item.linkedAshId)
                            else character.removeInventoryItem(item.id)
                        }.onSuccess(onChange).onFailure {
                            android.widget.Toast.makeText(context, it.message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Text("${item.category.ifBlank { "OBJETO" }} // ${item.quality} // PG ${item.pg} // PL ${item.pl}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                Text("REGIÃO ${item.region.ifBlank { "—" }} // CARGA ${item.effectiveLoad()} // LA ${item.agilityLimit ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (item.linkedAshId.isNotBlank()) {
                    IntegerField("Doses", item.quantity, enabled) { doses ->
                        onChange(character.copy(inventory = character.inventory.replace(index, item.copy(quantity = doses.coerceAtLeast(0)))))
                    }
                    Text("PUREZA // ${item.ashPurity.label}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
                item.mechanicalEffects.forEach { effect ->
                    Text("${effect.type.name}: ${if (effect.value > 0) "+" else ""}${effect.value} ${effect.resolvedTargetId.ifBlank { effect.target }}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
                val states = validItemStates(item)
                ChoiceField("Estado", item.inventoryState, states, enabled, display = InventoryState::label) { value ->
                    onChange(character.withItemInventoryState(item.id, value))
                }
                HudTextField("Efeito", item.effect, multiline = true, enabled = enabled) { value ->
                    onChange(character.copy(inventory = character.inventory.replace(index, item.copy(effect = value))))
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
            initialCreation = remainingHeritage > 0,
            canUseCatalog = itemCatalog.isNotEmpty(),
            canAddAsh = ashCatalog.isNotEmpty() && remainingHeritage == 0,
            onDismiss = { dialog = null },
            onChoice = { dialog = it },
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
        "initial_builder" -> StrictItemBuilderDialog(
            remainingHeritage = remainingHeritage,
            draft = character.itemCreationDraft ?: ItemCreationDraft(),
            onDraftChange = { onChange(character.copy(itemCreationDraft = it)) },
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.addInventoryItem(item).copy(itemCreationDraft = null))
            dialog = null
        }
        "builder" -> StrictItemBuilderDialog(
            remainingHeritage = null,
            draft = character.itemCreationDraft ?: ItemCreationDraft(),
            onDraftChange = { onChange(character.copy(itemCreationDraft = it)) },
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.addInventoryItem(item).copy(itemCreationDraft = null))
            dialog = null
        }
        "ash_catalog" -> CatalogPickerDialog("CATÁLOGO DE CINZAS", ashCatalog, { dialog = null }) { entry ->
            onChange(character.withAddedAbility(entry.toMysticAbility(), reuseExistingAsh = true))
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
                AddButton("Criar personalizado", true) { onChoice(if (initialCreation) "initial_builder" else "builder") }
                if (canAddAsh) AddButton("Adicionar Cinzas", true) { onChoice("ash_catalog") }
                if (!initialCreation) TextButton(onClick = { onChoice("narrative") }, modifier = Modifier.fillMaxWidth()) { Text("ADICIONAR ITEM NARRATIVO") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

private fun inventoryGroups(items: List<InventoryItem>): Map<String, List<InventoryItem>> = linkedMapOf(
    "Armas" to items.filter { it.category.contains("arma", true) && !it.category.contains("armadura", true) },
    "Armaduras" to items.filter { it.category.contains("armadura", true) || it.category.contains("acessório", true) },
    "Itens" to items.filterNot { it.category.contains("arma", true) },
).let { groups -> groups + ("Itens" to groups.getValue("Itens").filterNot { it.category.contains("armadura", true) || it.category.contains("acessório", true) }) }

private fun validItemStates(item: InventoryItem): List<InventoryState> = buildList {
    if (item.category.contains("arma", true) && !item.category.contains("armadura", true)) add(InventoryState.WIELDED)
    if (item.category.contains("armadura", true) || item.category.contains("acessório", true)) add(InventoryState.EQUIPPED)
    add(InventoryState.CONTAINER)
    add(InventoryState.BACKPACK)
    add(InventoryState.STORED)
}

@Composable
private fun StrictItemBuilderDialog(
    remainingHeritage: Int?,
    draft: ItemCreationDraft,
    onDraftChange: (ItemCreationDraft) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (InventoryItem) -> Unit,
) {
    val step = draft.step
    val category = draft.category
    val weapon = category == "Arma"
    val basePool: List<ItemPart> = if (weapon) ItemCreationRules.weaponBases else ItemCreationRules.armorBases
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
    val manualPrice = draft.manualPrice
    val commonName = draft.commonName
    val commonEffect = draft.commonEffect
    val commonLoad = draft.commonLoad
    val commonQuantity = draft.commonQuantity

    val initialCreation = remainingHeritage != null
    val bases: List<ItemPart> = if (weapon) ItemCreationRules.weaponBases else ItemCreationRules.armorBases
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
        priceOverride = manualPrice.toIntOrNull().takeIf { !initialCreation },
    )
    val allowedByBudget = remainingHeritage == null || (built.creationCost != null && built.creationCost <= remainingHeritage)
    val requiresPrice = !initialCreation && built.creationCost == null && manualPrice.isBlank()
    val commonItem = InventoryItem(name = commonName.trim(), category = "Item", effect = commonEffect, load = commonLoad, quantity = commonQuantity)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (initialCreation) "CRIAR ITEM // $remainingHeritage PH" else "CRIAR ITEM")
                Text("$step DE 7  //  ${listOf("CATEGORIA", "BASE", "MATERIAL", "QUALIDADE", "MODIFICAÇÕES", "GEMAS", "REVISÃO")[step - 1]}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (step == 1) {
                Text("O que você quer criar? As próximas opções serão adaptadas à categoria.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChoiceField("Categoria", category, if (initialCreation) listOf("Arma", "Armadura") else listOf("Arma", "Armadura", "Item"), true) { selected ->
                    val nextWeapon = selected == "Arma"
                    val nextBase = if (nextWeapon) ItemCreationRules.weaponBases.first() else ItemCreationRules.armorBases.first()
                    val nextMaterial = if (nextWeapon) ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" }
                        else ItemCreationRules.armorMaterials.first { it.id == "ligas_comuns" }
                    onDraftChange(draft.copy(category = selected, baseId = nextBase.id, materialId = nextMaterial.id, modificationIds = emptyList()))
                }
                Text(when (category) { "Arma" -> "Armas possuem dano, material e modificações de combate."; "Armadura" -> "Armaduras e acessórios possuem proteção, região e limitações."; else -> "Itens comuns usam apenas nome, quantidade, carga e efeito." }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (step == 2 && category == "Item") {
                    HudTextField("Nome do item", commonName) { onDraftChange(draft.copy(commonName = it)) }
                    TwoFields(
                        { IntegerField("Quantidade", commonQuantity, true, it) { value -> onDraftChange(draft.copy(commonQuantity = value.coerceAtLeast(1))) } },
                        { IntegerField("Carga total", commonLoad, true, it) { value -> onDraftChange(draft.copy(commonLoad = value.coerceAtLeast(0))) } },
                    )
                    HudTextField("Descrição ou efeito", commonEffect, multiline = true) { onDraftChange(draft.copy(commonEffect = it)) }
                    if (!initialCreation) HudTextField("Preço em E$ (opcional)", manualPrice) { onDraftChange(draft.copy(manualPrice = it.filter(Char::isDigit))) }
                }
                if (step == 2 && category != "Item") {
                    HudTextField("Nome personalizado", customName) { onDraftChange(draft.copy(customName = it)) }
                    ChoiceField<String>("Tipo", base.id, bases.map { it.id }, true, display = { id: String -> bases.first { it.id == id }.name }) { id: String ->
                        val selected = bases.first { it.id == id }
                        val nextMaterialId = if (!weapon && selected.id == "gibao") "organico" else draft.materialId
                        onDraftChange(draft.copy(baseId = id, materialId = nextMaterialId, modificationIds = modifications.filter { it in ItemCreationRules.compatibleModifications(selected, weapon) }.map { it.id }))
                    }
                }
                if (step == 3 && category != "Item") {
                    ChoiceField<String>("Material", material.id, materials.map { it.id }, true, display = { id: String -> materials.first { it.id == id }.name }) { id: String -> onDraftChange(draft.copy(materialId = id)) }
                    Text(material.effect, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (step == 4 && category != "Item") {
                    ChoiceField<String>("Qualidade", quality.name, ItemQuality.entries.map { it.name }, true, display = { name: String -> ItemQuality.valueOf(name).label }) { name: String -> onDraftChange(draft.copy(quality = ItemQuality.valueOf(name))) }
                    Text("CUSTO ATUAL // ${built.creationCost ?: "#"} PH // SALDO ${remainingHeritage?.minus(built.creationCost ?: 0) ?: "—"}", color = MaterialTheme.colorScheme.primary)
                }
                if (step == 5 && category != "Item") {
                    Text("MODIFICAÇÕES", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    availableModifications.forEach { modification: ItemPart ->
                        val checked = modification in modifications
                        Row(Modifier.fillMaxWidth().clickable { onDraftChange(draft.copy(modificationIds = strictToggleModification(modifications, modification).map { it.id })) }) {
                            Checkbox(checked, onCheckedChange = { onDraftChange(draft.copy(modificationIds = strictToggleModification(modifications, modification).map { it.id })) })
                            Column(Modifier.padding(top = 8.dp)) {
                                Text(modification.name, color = MaterialTheme.colorScheme.onSurface)
                                if (modification.effect.isNotBlank()) Text(modification.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (step == 6 && category != "Item") {
                    TwoFields(
                        { IntegerField("Espaços de Gema", gemSlots, true, it) { value -> onDraftChange(draft.copy(gemSlots = value.coerceIn(gems.size, 5))) } },
                        { IntegerField("Espaços de Tecnologia", technologySlots, true, it) { value -> onDraftChange(draft.copy(technologySlots = value.coerceIn(0, 5))) } },
                    )
                    val availableGems: List<ItemPart> = ItemCreationRules.gemComponents.filterNot { candidate: ItemPart -> gems.any { it.id == candidate.id } }
                    if (quality != ItemQuality.MUNDANE && gems.size < gemSlots && availableGems.isNotEmpty()) {
                        ChoiceField<String>("Adicionar gema", "", availableGems.map { it.id }, true, display = { id: String -> availableGems.firstOrNull { it.id == id }?.name ?: "Selecionar" }) { id: String ->
                            onDraftChange(draft.copy(gemIds = draft.gemIds + id))
                        }
                    }
                    gems.forEach { gem: ItemPart ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(gem.name); Text(gem.effect, style = MaterialTheme.typography.bodySmall) }
                            RemoveButton(true, "Remover ${gem.name}") { onDraftChange(draft.copy(gemIds = draft.gemIds - gem.id)) }
                        }
                    }
                }
                if (step in 3..6 && category == "Item") {
                    Text("ITEM NARRATIVO // SEM COMPONENTES MECÂNICOS", color = MaterialTheme.colorScheme.primary)
                    Text("Continue até a revisão. O rascunho permanece salvo durante a navegação.")
                }
                if (step == 7) {
                Text("REVISE ANTES DE CRIAR", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(if (category == "Item") commonName.ifBlank { "Item sem nome" } else built.name, style = MaterialTheme.typography.titleLarge)
                if (category == "Item") Text("Item comum // Quantidade $commonQuantity // Carga $commonLoad")
                else Text("${if (weapon) "Arma" else "Armadura / Acessório"} // ${material.name} // ${quality.label}")
                if (modifications.isNotEmpty()) Text("MODIFICAÇÕES // ${modifications.joinToString { it.name }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (gems.isNotEmpty()) Text("GEMAS // ${gems.joinToString { it.name }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!initialCreation && category != "Item") {
                    HudTextField("Preço final em E$", manualPrice) { onDraftChange(draft.copy(manualPrice = it.filter(Char::isDigit))) }
                }
                if (category != "Item") {
                    Text("CUSTO // ${built.creationCost ?: "#"} PH // PREÇO ${built.price} E$", color = MaterialTheme.colorScheme.primary)
                    Text("PG ${built.pg} // PL ${built.pl}", color = MaterialTheme.colorScheme.onSurface)
                    Text("CARGA ${built.load} // DURABILIDADE ${built.durability}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!allowedByBudget) Text("Custo acima dos PH restantes ou item # não disponível na criação inicial.", color = MaterialTheme.colorScheme.error)
                    if (requiresPrice) Text("Este material exige preço manual.", color = MaterialTheme.colorScheme.error)
                }
            }
            }
        },
        confirmButton = {
            TextButton(
                enabled = step < 7 || if (category == "Item") commonName.isNotBlank() else allowedByBudget && !requiresPrice,
                onClick = { if (step < 7) onDraftChange(draft.copy(step = step + 1)) else onAdd(if (category == "Item") commonItem else built.toInventoryItem(initialCreation = initialCreation)) },
            ) { Text(if (step < 7) "CONTINUAR" else "CRIAR ITEM") }
        },
        dismissButton = { TextButton(onClick = { if (step > 1) onDraftChange(draft.copy(step = step - 1)) else onDismiss() }) { Text(if (step > 1) "VOLTAR" else "CANCELAR") } },
    )
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
