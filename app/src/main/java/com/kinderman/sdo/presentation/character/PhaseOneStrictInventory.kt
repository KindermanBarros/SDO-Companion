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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemBonus
import com.kinderman.sdo.domain.model.ItemBonusType
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.initialCreationCost
import com.kinderman.sdo.domain.model.participatesInInitialCreation
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.effectiveLoad
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
    var dialog by remember { mutableStateOf<String?>(null) }
    var inventoryQuery by remember { mutableStateOf("") }
    var inventoryGroup by remember { mutableStateOf("Todos") }
    val context = LocalContext.current
    val spentHeritage = character.inventory.sumOf { it.initialCreationCost() }
    val hasInitialShopping = character.inventory.any { it.participatesInInitialCreation() }
    val remainingHeritage = when {
        hasInitialShopping || character.inventory.isEmpty() -> (ItemCreationRules.HERITAGE_BUDGET - spentHeritage).coerceAtLeast(0)
        else -> 0
    }
    val acquiredTargets = character.learnedKnowledges
        .map(SpecialKnowledge::name)
        .filter(String::isNotBlank)
        .distinct()
    val itemCatalog = catalog.filter { it.kind == CatalogKind.ITEM }
    val ashCatalog = catalog.filter { it.kind == CatalogKind.ASH }

    TechPanel {
        SectionHeader("09", "Inventário")
        Text(
            "CARGA ${character.currentLoad} / ${character.maximumLoad}",
            color = if (character.currentLoad > character.maximumLoad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
        )
        Text("Bônus mecânicos usam somente seletores controlados de tipo e destino.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        IntegerField("Capacidade do recipiente equipado", character.containerCapacity, enabled) {
            onChange(character.copy(containerCapacity = it.coerceAtLeast(0)))
        }
        HudTextField("Buscar por nome, categoria, material ou estado", inventoryQuery) { inventoryQuery = it }
        ChoiceField("Grupo", inventoryGroup, listOf("Todos", "Armas", "Armaduras", "Itens"), true) { inventoryGroup = it }

        inventoryGroups(character.inventory).forEach { (group, groupItems) ->
            val visible = groupItems.filter { item ->
                (inventoryGroup == "Todos" || inventoryGroup == group) &&
                    (inventoryQuery.isBlank() || listOf(item.name, item.category, item.quality, item.effect, itemStateLabel(item.state)).any { it.contains(inventoryQuery, true) })
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
                if (item.bonuses.isNotEmpty()) {
                    item.bonuses.forEach { bonus ->
                        Text(
                            "${bonus.type.label}: ${if (bonus.value >= 0) "+" else ""}${bonus.value} ${bonus.displayTarget()}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                val states = validItemStates(item)
                ChoiceField("Estado", item.state, states, enabled, display = ::itemStateLabel) { value ->
                    onChange(character.copy(inventory = character.inventory.replace(index, item.copy(state = value))))
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
                onChange(character.copy(inventory = character.inventory + InventoryItem(name = entry.term, category = if (entry.section == "Armas") "Arma" else "Item", effect = entry.definition)))
                dialog = null
            },
        )
        "initial_catalog" -> ItemCatalogDialog(
            title = "LOJA INICIAL // ITENS PRONTOS",
            entries = itemCatalog,
            remainingHeritage = remainingHeritage,
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.copy(inventory = character.inventory + item))
            dialog = null
        }
        "catalog" -> ItemCatalogDialog(
            title = "CATÁLOGO DE ITENS",
            entries = itemCatalog,
            remainingHeritage = null,
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.copy(inventory = character.inventory + item))
            dialog = null
        }
        "initial_builder" -> StrictItemBuilderDialog(
            remainingHeritage = remainingHeritage,
            acquiredKnowledgeTargets = acquiredTargets,
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.copy(inventory = character.inventory + item))
            dialog = null
        }
        "builder" -> StrictItemBuilderDialog(
            remainingHeritage = null,
            acquiredKnowledgeTargets = acquiredTargets,
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.copy(inventory = character.inventory + item))
            dialog = null
        }
        "ash_catalog" -> CatalogPickerDialog("CATÁLOGO DE CINZAS", ashCatalog, { dialog = null }) { entry ->
            onChange(character.withAddedAbility(entry.toMysticAbility(), reuseExistingAsh = true))
            dialog = null
        }
        "narrative" -> {
            onChange(character.copy(inventory = character.inventory + InventoryItem(category = "Item", quantity = 1)))
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

private fun validItemStates(item: InventoryItem): List<String> = buildList {
    if (item.category.contains("arma", true) || item.category.contains("armadura", true) || item.category.contains("acessório", true)) add("E")
    add("R"); add("M"); add("G")
}

private fun itemStateLabel(state: String): String = when (state) {
    "E" -> "Equipado"
    "R" -> "Recipiente"
    "G" -> "Guardado"
    else -> "Mochila"
}

@Composable
private fun StrictItemBuilderDialog(
    remainingHeritage: Int?,
    acquiredKnowledgeTargets: List<String>,
    onDismiss: () -> Unit,
    onAdd: (InventoryItem) -> Unit,
) {
    var step by remember { mutableIntStateOf(1) }
    var category by remember { mutableStateOf("Arma") }
    var weapon by remember { mutableStateOf(true) }
    var base by remember { mutableStateOf(ItemCreationRules.weaponBases.first()) }
    var material by remember { mutableStateOf(ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" }) }
    var modifications by remember { mutableStateOf(emptyList<ItemPart>()) }
    var gemSlots by remember { mutableIntStateOf(0) }
    var technologySlots by remember { mutableIntStateOf(0) }
    var customName by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf(ItemQuality.COMMON) }
    var bonuses by remember { mutableStateOf(emptyList<ItemBonus>()) }
    var gems by remember { mutableStateOf(emptyList<ItemPart>()) }
    var manualPrice by remember { mutableStateOf("") }
    var commonName by remember { mutableStateOf("") }
    var commonEffect by remember { mutableStateOf("") }
    var commonLoad by remember { mutableIntStateOf(0) }
    var commonQuantity by remember { mutableIntStateOf(1) }

    val initialCreation = remainingHeritage != null
    val bases = if (weapon) ItemCreationRules.weaponBases else ItemCreationRules.armorBases
    val materials = when {
        weapon -> ItemCreationRules.weaponMaterials
        base.id == "gibao" -> ItemCreationRules.armorMaterials.filter { it.id == "organico" }
        else -> ItemCreationRules.armorMaterials
    }.let { list -> if (initialCreation) list.filter { it.creationCost != null } else list }
    val availableModifications = if (weapon) ItemCreationRules.weaponModifications else ItemCreationRules.armorModifications
    val built = ItemCreationRules.build(
        base = base,
        material = material,
        modifications = modifications,
        gemSlots = gemSlots,
        technologySlots = technologySlots,
        customName = customName,
        quality = quality,
        bonuses = bonuses,
        components = gems,
        priceOverride = manualPrice.toIntOrNull().takeIf { !initialCreation },
    )
    val allowedByBudget = remainingHeritage == null || (built.creationCost != null && built.creationCost <= remainingHeritage)
    val requiresPrice = !initialCreation && built.creationCost == null && manualPrice.isBlank()
    val bonusesComplete = bonuses.all(ItemBonus::isComplete)
    val commonItem = InventoryItem(name = commonName.trim(), category = "Item", effect = commonEffect, load = commonLoad, quantity = commonQuantity)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (initialCreation) "CRIAR ITEM // $remainingHeritage PH" else "CRIAR ITEM")
                Text("$step DE 4  //  ${listOf("CATEGORIA", "BASE", "PERSONALIZAÇÃO", "REVISÃO")[step - 1]}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (step == 1) {
                Text("O que você quer criar? As próximas opções serão adaptadas à categoria.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        category = "Arma"
                        weapon = true
                        base = ItemCreationRules.weaponBases.first()
                        material = ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" }
                        modifications = emptyList()
                    }) { Text(if (category == "Arma") "[ ARMA ]" else "ARMA") }
                    TextButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        category = "Armadura"
                        weapon = false
                        base = ItemCreationRules.armorBases.first()
                        material = ItemCreationRules.armorMaterials.first { it.id == "ligas_comuns" }
                        modifications = emptyList()
                    }) { Text(if (category == "Armadura") "[ ARMADURA / ACESSÓRIO ]" else "ARMADURA / ACESSÓRIO") }
                    TextButton(modifier = Modifier.fillMaxWidth(), onClick = { category = "Item" }) { Text(if (category == "Item") "[ ITEM COMUM ]" else "ITEM COMUM") }
                }
                Text(when (category) { "Arma" -> "Armas possuem dano, material e modificações de combate."; "Armadura" -> "Armaduras e acessórios possuem proteção, região e limitações."; else -> "Itens comuns usam apenas nome, quantidade, carga e efeito." }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (step == 2) {
                if (category == "Item") {
                    HudTextField("Nome do item", commonName) { commonName = it }
                    TwoFields(
                        { IntegerField("Quantidade", commonQuantity, true, it) { value -> commonQuantity = value.coerceAtLeast(1) } },
                        { IntegerField("Carga total", commonLoad, true, it) { value -> commonLoad = value.coerceAtLeast(0) } },
                    )
                    HudTextField("Descrição ou efeito", commonEffect, multiline = true) { commonEffect = it }
                    if (!initialCreation) HudTextField("Preço em E$ (opcional)", manualPrice) { manualPrice = it.filter(Char::isDigit) }
                } else {
                HudTextField("Nome personalizado", customName) { customName = it }
                CyclePartButton("TIPO", base, bases) { selected ->
                    base = selected
                    if (!weapon && selected.id == "gibao") {
                        material = ItemCreationRules.armorMaterials.first { it.id == "organico" }
                    }
                }
                CyclePartButton("MATERIAL", material, materials) { material = it }
                TextButton(
                    onClick = { quality = ItemQuality.entries[(quality.ordinal + 1) % ItemQuality.entries.size] },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("QUALIDADE // ${quality.label}") }
                Text("PRÉVIA", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(built.name, style = MaterialTheme.typography.titleMedium)
                Text("PG ${built.pg} // PL ${built.pl} // CARGA ${built.load} // DURABILIDADE ${built.durability}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                }

                if (step == 3) {
                if (category == "Item") {
                    Text("ITEM COMUM", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text("Itens comuns não recebem modificações de arma, proteção, gemas ou bônus de equipamento.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                Text("MODIFICAÇÕES", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(if (modifications.isEmpty()) "Nenhuma selecionada" else modifications.joinToString(" // ") { it.name }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                availableModifications.forEach { modification ->
                    val checked = modification in modifications
                    Row(Modifier.fillMaxWidth().clickable { modifications = strictToggleModification(modifications, modification) }) {
                        Checkbox(checked, onCheckedChange = { modifications = strictToggleModification(modifications, modification) })
                        Column(Modifier.padding(top = 8.dp)) {
                            Text(modification.name, color = MaterialTheme.colorScheme.onSurface)
                            if (modification.effect.isNotBlank()) Text(modification.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                TwoFields(
                    { IntegerField("Espaços de Gema", gemSlots, true, it) { value -> gemSlots = value.coerceIn(gems.size, 5) } },
                    { IntegerField("Espaços de Tecnologia", technologySlots, true, it) { value -> technologySlots = value.coerceIn(0, 5) } },
                )
                Text("GEMAS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
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
                        Text(gem.name, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 10.dp))
                    }
                }

                Text("BÔNUS CONCEDIDOS AO EQUIPAR", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                bonuses.forEachIndexed { index, bonus ->
                    val targets = strictBonusTargets(bonus.type, acquiredKnowledgeTargets)
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = {
                                val nextType = ItemBonusType.entries[(bonus.type.ordinal + 1) % ItemBonusType.entries.size]
                                val target = strictBonusTargets(nextType, acquiredKnowledgeTargets).firstOrNull().orEmpty()
                                bonuses = bonuses.replace(index, bonus.copy(type = nextType, target = target))
                            }) { Text("TIPO // ${bonus.type.label.uppercase()}") }
                            TextButton(onClick = { bonuses = bonuses.filterIndexed { itemIndex, _ -> itemIndex != index } }) {
                                Text("REMOVER", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        TextButton(
                            enabled = targets.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val current = targets.indexOf(bonus.target)
                                val next = targets[(current.coerceAtLeast(-1) + 1) % targets.size]
                                bonuses = bonuses.replace(index, bonus.copy(target = next))
                            },
                        ) {
                            Text("APLICAR EM // ${bonus.displayTarget().ifBlank { if (targets.isEmpty()) "SEM OPÇÕES" else "SELECIONAR" }}")
                        }
                        IntegerField("Valor (-5 a +5)", bonus.value, true) { value ->
                            bonuses = bonuses.replace(index, bonus.copy(value = value.coerceIn(-5, 5)))
                        }
                        if (bonus.target.isBlank()) {
                            Text(
                                if (bonus.type == ItemBonusType.ACQUIRED_KNOWLEDGE) "A ficha não possui Conhecimentos Adquiridos disponíveis para este bônus."
                                else "Selecione um destino para o bônus.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { bonuses = bonuses + ItemBonus(type = ItemBonusType.ATTRIBUTE, target = "FOR") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("+ ADICIONAR BÔNUS") }
                }
                }

                if (step == 4) {
                Text("REVISE ANTES DE CRIAR", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(if (category == "Item") commonName.ifBlank { "Item sem nome" } else built.name, style = MaterialTheme.typography.titleLarge)
                if (category == "Item") Text("Item comum // Quantidade $commonQuantity // Carga $commonLoad")
                else Text("${if (weapon) "Arma" else "Armadura / Acessório"} // ${material.name} // ${quality.label}")
                if (modifications.isNotEmpty()) Text("MODIFICAÇÕES // ${modifications.joinToString { it.name }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (gems.isNotEmpty()) Text("GEMAS // ${gems.joinToString { it.name }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!initialCreation && category != "Item") {
                    HudTextField("Preço final em E$", manualPrice) { manualPrice = it.filter(Char::isDigit) }
                }
                if (category != "Item") {
                    Text("CUSTO // ${built.creationCost ?: "#"} PH // PREÇO ${built.price} E$", color = MaterialTheme.colorScheme.primary)
                    Text("PG ${built.pg} // PL ${built.pl} // LA ${built.agilityLimit ?: "—"}", color = MaterialTheme.colorScheme.onSurface)
                    Text("CARGA ${built.load} // DURABILIDADE ${built.durability}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!allowedByBudget) Text("Custo acima dos PH restantes ou item # não disponível na criação inicial.", color = MaterialTheme.colorScheme.error)
                    if (requiresPrice) Text("Este material exige preço manual.", color = MaterialTheme.colorScheme.error)
                    if (!bonusesComplete) Text("Todos os bônus precisam de um destino válido.", color = MaterialTheme.colorScheme.error)
                }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = step < 4 || if (category == "Item") commonName.isNotBlank() else allowedByBudget && !requiresPrice && bonusesComplete,
                onClick = { if (step < 4) step++ else onAdd(if (category == "Item") commonItem else built.toInventoryItem(initialCreation = initialCreation)) },
            ) { Text(if (step < 4) "CONTINUAR" else "CRIAR ITEM") }
        },
        dismissButton = { TextButton(onClick = { if (step > 1) step-- else onDismiss() }) { Text(if (step > 1) "VOLTAR" else "CANCELAR") } },
    )
}

@Composable
private fun CyclePartButton(label: String, current: ItemPart, options: List<ItemPart>, onSelected: (ItemPart) -> Unit) {
    TextButton(
        enabled = options.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            if (options.isEmpty()) return@TextButton
            val currentIndex = options.indexOfFirst { it.id == current.id }
            onSelected(options[(currentIndex.coerceAtLeast(-1) + 1) % options.size])
        },
    ) { Text("$label // ${current.name}") }
}

private fun strictBonusTargets(type: ItemBonusType, acquiredKnowledgeTargets: List<String>): List<String> = when (type) {
    ItemBonusType.ATTRIBUTE -> listOf("FOR", "VIG", "AGI", "POD", "INT", "CAR")
    ItemBonusType.BASIC_KNOWLEDGE -> listOf(
        "FOR" to listOf("Atletismo", "Brutalidade", "Luta", "Arremesso"),
        "VIG" to listOf("Energia", "Vitalidade", "Tolerância", "Regeneração"),
        "AGI" to listOf("Furtividade", "Reflexos", "Movimento", "Pontaria"),
        "POD" to listOf("Arcano", "Sentidos", "Controle", "Recuperação"),
        "INT" to listOf("Sanidade", "Intuição", "Religião", "Raciocínio"),
        "CAR" to listOf("Política", "Lábia", "Enganação", "Intimidação"),
    ).flatMap { (attribute, skills) -> skills.map { ItemBonus.basicKnowledgeTarget(attribute, it) } }
    ItemBonusType.ACQUIRED_KNOWLEDGE -> acquiredKnowledgeTargets
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
