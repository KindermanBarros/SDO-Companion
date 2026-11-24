package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.R
import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.catalog.installEnhancement
import com.kinderman.sdo.domain.catalog.rechargeEnhancement
import com.kinderman.sdo.domain.catalog.removeEnhancement
import com.kinderman.sdo.domain.catalog.spendEnhancementCharge
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.InventorySectionGroup
import com.kinderman.sdo.domain.model.InventoryState
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.addInventoryItem
import com.kinderman.sdo.domain.model.durabilityLabel
import com.kinderman.sdo.domain.model.effectiveLoad
import com.kinderman.sdo.domain.model.handsRequired
import com.kinderman.sdo.domain.model.initialCreationCost
import com.kinderman.sdo.domain.model.inventoryState
import com.kinderman.sdo.domain.model.isBroken
import com.kinderman.sdo.domain.model.isScrap
import com.kinderman.sdo.domain.model.isWeaponItem
import com.kinderman.sdo.domain.model.isWieldableItem
import com.kinderman.sdo.domain.model.recycleBrokenItem
import com.kinderman.sdo.domain.model.resolvedCategory
import com.kinderman.sdo.domain.model.validInventoryStates
import com.kinderman.sdo.domain.model.withItemInventoryState
import com.kinderman.sdo.domain.model.withRemovedAshInventoryItem
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.SdoCardTitle
import com.kinderman.sdo.ui.SdoCompactFilterBar
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.SdoTextButton
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.sdoClickable

@Composable
internal fun PhaseOneStrictInventorySection(
    character: Character,
    catalog: List<CatalogEntry>,
    enabled: Boolean,
    onChange: (Character) -> Unit,
) {
    var showGlossary by rememberSaveable(character.id) { mutableStateOf(false) }
    var inventoryQuery by rememberSaveable(character.id) { mutableStateOf("") }
    var inventoryCategory by rememberSaveable(character.id) { mutableStateOf("") }
    var inventorySort by rememberSaveable(character.id) { mutableStateOf(InventorySort.DEFAULT) }
    val context = LocalContext.current
    val spentHeritage = character.inventory.sumOf { it.initialCreationCost() }
    val remainingHeritage = (ItemCreationRules.HERITAGE_BUDGET - spentHeritage).coerceAtLeast(0)
    val resolvedCategories = remember(character.inventory) {
        character.inventory.associate { it.id to it.resolvedCategory() }
    }
    val categories = remember(resolvedCategories) {
        buildMap {
            InventorySectionGroup.entries.forEach { put("group:" + it.name, it.label) }
            resolvedCategories.values.sortedBy { it.displayLabel }.forEach {
                put(it.filterKey, it.displayLabel)
            }
        }
    }
    val materialNames = remember {
        (ItemCreationRules.weaponMaterials + ItemCreationRules.armorMaterials).associate { it.id to it.name }
    }
    val sortLabels = InventorySort.entries.associateWith { stringResource(it.labelRes) }
    val visibleItems = remember(character.inventory, inventoryQuery, inventoryCategory, inventorySort) {
        val needle = inventoryQuery.trim()
        val activeCategory = inventoryCategory.takeIf(categories::containsKey)
        val matches = character.inventory.filter { item ->
            val category = resolvedCategories.getValue(item.id)
            val matchesCategory = activeCategory == null ||
                category.filterKey == activeCategory ||
                activeCategory == "group:" + category.sectionGroup.name
            matchesCategory &&
                listOf(
                    item.name, item.category, category.displayLabel, category.sectionGroup.label,
                    item.quality.label, item.effect, item.inventoryState.label,
                    materialNames[item.materialId].orEmpty(), materialNames[item.secondaryMaterialId].orEmpty(),
                ).any { it.contains(needle, ignoreCase = true) }
        }
        when (inventorySort) {
            InventorySort.DEFAULT -> matches
            InventorySort.NAME -> matches.sortedBy { it.name.lowercase() }
            InventorySort.LOAD -> matches.sortedBy { it.effectiveLoad() }
        }
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
        val categoryLabel = categories[inventoryCategory]
        SdoCompactFilterBar(
            activeFilters = listOfNotNull(
                inventoryQuery.trim().takeIf(String::isNotEmpty),
                categoryLabel,
                sortLabels.getValue(inventorySort).takeIf { inventorySort != InventorySort.DEFAULT },
            ),
            onClear = {
                inventoryQuery = ""
                inventoryCategory = ""
                inventorySort = InventorySort.DEFAULT
            },
            modifier = Modifier.testTag("inventory_sheet_filters"),
        ) {
            HudTextField(stringResource(R.string.inventory_sheet_search), inventoryQuery) {
                inventoryQuery = it
            }
            InventoryCategoryFilter(categories, inventoryCategory) { inventoryCategory = it }
            ChoiceField(
                stringResource(R.string.inventory_sort),
                inventorySort,
                InventorySort.entries,
                true,
                modifier = Modifier.testTag("inventory_sheet_sort"),
                display = sortLabels::getValue,
            ) { inventorySort = it }
        }
        if (visibleItems.isEmpty()) {
            Text(
                stringResource(R.string.inventory_no_results),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        InventorySectionGroup.entries.forEach { group ->
            val visible = visibleItems.filter { resolvedCategories.getValue(it.id).sectionGroup == group }
            if (visible.isNotEmpty()) {
                SdoMetadataText(
                    stringResource(
                        R.string.inventory_group_summary,
                        group.label,
                        visible.size,
                        visible.sumOf { it.effectiveLoad() },
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            for (item in visible) key(item.id) {
                val index = character.inventory.indexOfFirst { it.id == item.id }
                var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
                val compactDetails = buildList {
                    add(item.inventoryState.label.uppercase())
                    if (character.isInCreation && item.acquisitionSource == com.kinderman.sdo.domain.model.ItemAcquisitionSource.HERITAGE) {
                        add("${item.initialCreationCost()} PH")
                    }
                    add("CARGA ${item.effectiveLoad()}")
                    if (item.quantity > 1 || item.linkedAshId.isNotBlank() || item.category.equals("Munição", true) || item.category.contains("Consumível", true)) {
                        add("QTD ${item.quantity}")
                    }
                    add("DUR ${item.durabilityLabel}")
                }.joinToString(" // ")
                SdoInsetCard(contentPadding = 12.dp, verticalSpacing = 6.dp) {
                    Row(Modifier.fillMaxWidth().sdoClickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            SdoCardTitle(item.name.ifBlank { "ITEM ${(index + 1).toString().padStart(2, '0')}" }, color = MaterialTheme.colorScheme.onSurface)
                            SdoMetadataText(compactDetails, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
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
                    SdoMetadataText("${resolvedCategories.getValue(item.id).displayLabel} // ${item.quality.label} // PG ${item.pg} // PL ${item.pl}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    if (!expanded) return@SdoInsetCard
                    SdoMetadataText("REGIÃO ${item.region.ifBlank { "—" }} // CARGA ${item.effectiveLoad()} // LA ${item.agilityLimit ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    if (item.isWieldableItem()) SdoMetadataText("EMPUNHADURA // ${item.handsRequired()} MÃO(S)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text("DURABILIDADE ${item.durabilityLabel}${when { item.isBroken -> " // [QUEBRADO]"; item.isScrap -> " // [SUCATA]"; else -> "" }}", color = if (item.isScrap || item.isBroken) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.isScrap && item.isWeaponItem()) Text(
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
                        SdoTextButton(
                            onClick = { onChange(character.recycleBrokenItem(item.id)) },
                            enabled = enabled,
                        ) { Text("RECICLAR") }
                    }
                    ItemCreationRules.run {
                        val materialName = (weaponMaterials + armorMaterials).firstOrNull { it.id == item.materialId }?.name
                        val secondaryMaterialName = (weaponMaterials + armorMaterials).firstOrNull { it.id == item.secondaryMaterialId }?.name
                        val modificationNames = (weaponModifications + armorModifications).filter { it.id in item.modificationIds }.map { it.name }
                        val installedEnhancementIds = item.installedEnhancements.map { it.catalogEntryId }.toSet()
                        val enhancementNames = enhancementComponents.filter { it.id in installedEnhancementIds }.map { it.name }
                        materialName?.let { SdoMetadataText("MATERIAL // ${it.uppercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                        secondaryMaterialName?.let { SdoMetadataText("LIGA // ${it.uppercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                        if (modificationNames.isNotEmpty()) SdoMetadataText("MODIFICAÇÕES // ${modificationNames.joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        SdoMetadataText("ESPAÇOS DE APRIMORAMENTO // ${item.installedEnhancements.size}/${item.enhancementSlots}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        if (enhancementNames.isNotEmpty()) SdoMetadataText("INSTALADOS // ${enhancementNames.joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        val basePart = (weaponBases + armorBases).firstOrNull { it.id == item.baseId }
                        val compatibleIds = basePart?.let { compatibleEnhancements(it, initialCreation = false).mapTo(hashSetOf()) { part -> part.id } }.orEmpty()
                        val availableEnhancements = character.inventory.filter { loose ->
                            loose.id != item.id && loose.category in setOf("Gema", "Tecnologia") &&
                                loose.catalogEntryId in compatibleIds &&
                                !(loose.category == "Tecnologia" && loose.catalogEntryId in installedEnhancementIds)
                        }
                        if (enabled && item.installedEnhancements.size < item.enhancementSlots && availableEnhancements.isNotEmpty()) {
                            ChoiceField("Instalar aprimoramento do inventário", "", availableEnhancements.map { it.id }, true, display = { id ->
                                availableEnhancements.firstOrNull { it.id == id }?.let { "${it.category} // ${it.name}" } ?: "Selecionar"
                            }) { sourceId ->
                                runCatching { character.installEnhancement(sourceId, item.id) }
                                    .onSuccess(onChange)
                                    .onFailure { android.widget.Toast.makeText(context, it.message, android.widget.Toast.LENGTH_SHORT).show() }
                            }
                        }
                        item.installedEnhancements.forEach { installed ->
                            enhancementComponents.firstOrNull { it.id == installed.catalogEntryId }?.let { enhancement ->
                                SdoMetadataText("${enhancement.name} // DUR ${installed.durabilityCurrent}/${installed.durabilityMax} // CARGAS ${installed.chargesCurrent}/${installed.chargesMax}", style = MaterialTheme.typography.bodySmall)
                                if (installed.chargesMax > 0) Row {
                                    SdoTextButton(onClick = { onChange(character.spendEnhancementCharge(item.id, installed.id)) }, enabled = enabled && installed.chargesCurrent > 0) { Text("ATIVAR") }
                                    SdoTextButton(onClick = { onChange(character.rechargeEnhancement(item.id, installed.id)) }, enabled = enabled && installed.chargesCurrent < installed.chargesMax) { Text("RECARREGAR") }
                                }
                                SdoTextButton(onClick = {
                                    onChange(character.removeEnhancement(item.id, installed.id))
                                }, enabled = enabled) { SdoMetadataText("REMOVER // ${enhancement.name.uppercase()}") }
                            }
                        }
                    }
                    if (item.linkedAshId.isNotBlank()) {
                        IntegerField("Doses", item.quantity, enabled) { doses ->
                            onChange(character.copy(inventory = character.inventory.replace(index, item.copy(quantity = doses.coerceAtLeast(0)))))
                        }
                        SdoMetadataText("PUREZA // ${item.ashPurity.label}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    } else if (item.category.equals("Munição", true) || item.category.contains("Consumível", true)) {
                        IntegerField(if (item.category.equals("Munição", true)) "Munição restante" else "Doses restantes", item.quantity, enabled) { quantity ->
                            onChange(character.copy(inventory = character.inventory.replace(index, item.copy(quantity = quantity.coerceAtLeast(0)))))
                        }
                    }
                    item.mechanicalEffects.forEach { effect ->
                        Text(effect.presentationLabel(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                    val states = character.validInventoryStates(item)
                    ChoiceField("Estado", item.inventoryState, states, enabled, display = InventoryState::label) { value ->
                        onChange(character.withItemInventoryState(item.id, value))
                    }
                    var editingItem by rememberSaveable(item.id) { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SdoMetadataText("DETALHES DO ITEM", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                        CompactEditToggleButton(
                            editing = enabled && editingItem,
                            enabled = enabled,
                            onClick = { editingItem = !editingItem },
                        )
                    }
                    if (enabled && editingItem) {
                        SheetTextField("Nome", item.name, enabled = enabled, fieldKey = "inventory/${item.id}/name") { value ->
                            onChange(character.copy(inventory = character.inventory.replace(index, item.copy(name = value))))
                        }
                        SheetTextField("Categoria", item.category, enabled = enabled, fieldKey = "inventory/${item.id}/category") { value ->
                            onChange(character.copy(inventory = character.inventory.replace(index, item.copy(category = value))))
                        }
                        TwoFields(
                            { IntegerField("Quantidade", item.quantity, enabled, it, fieldKey = "inventory/${item.id}/quantity") { qty ->
                                onChange(character.copy(inventory = character.inventory.replace(index, item.copy(quantity = qty.coerceAtLeast(0)))))
                            } },
                            { IntegerField("Carga", item.load, enabled, it, fieldKey = "inventory/${item.id}/load") { load ->
                                onChange(character.copy(inventory = character.inventory.replace(index, item.copy(load = load.coerceAtLeast(0)))))
                            } },
                        )
                        TwoFields(
                            { IntegerField("Durabilidade atual", item.durabilityCurrent, enabled, it, fieldKey = "inventory/${item.id}/durabilityCurrent") { cur ->
                                onChange(character.copy(inventory = character.inventory.replace(index, item.copy(durabilityCurrent = cur.coerceAtLeast(0)))))
                            } },
                            { IntegerField("Durabilidade máxima", item.durabilityMax, enabled, it, fieldKey = "inventory/${item.id}/durabilityMax") { max ->
                                onChange(character.copy(inventory = character.inventory.replace(index, item.copy(durabilityMax = max.coerceAtLeast(0)))))
                            } },
                        )
                        SheetTextField("Descrição", item.effect, multiline = true, enabled = enabled, fieldKey = "inventory/${item.id}/effect") { value ->
                            onChange(character.copy(inventory = character.inventory.replace(index, item.copy(effect = value))))
                        }
                    } else {
                        Text(
                            item.effect.ifBlank { "Sem descrição registrada." },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        InventoryAcquisitionActions(
            character = character,
            catalog = catalog,
            mode = if (character.isInCreation) ItemModalMode.Creation(remainingHeritage) else ItemModalMode.Sheet,
            enabled = enabled && (!character.isInCreation || remainingHeritage > 0),
            onChange = onChange,
        )
        if (character.isInCreation && remainingHeritage == 0) {
            SdoMetadataText("LIMITE DE PH ATINGIDO // remova ou altere um item de herança para adicionar outro.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        SdoTextButton(onClick = { showGlossary = true }, modifier = Modifier.fillMaxWidth()) { Text("CONSULTAR GLOSSÁRIOS") }
        if (character.isInCreation && remainingHeritage > 0) {
            SdoMetadataText("CRIAÇÃO INICIAL // $remainingHeritage / ${ItemCreationRules.HERITAGE_BUDGET} PH RESTANTES", color = MaterialTheme.colorScheme.primary)
        }
    }

    if (showGlossary) {
        EquipmentGlossaryDialog(
            onDismiss = { showGlossary = false },
            onUse = if (character.isInCreation) null else { entry ->
                onChange(character.addInventoryItem(InventoryItem(name = entry.term, category = if (entry.section == "Armas") "Arma" else "Item", effect = entry.definition)))
                showGlossary = false
            },
        )
    }
}

private enum class InventorySort(val labelRes: Int) {
    DEFAULT(R.string.inventory_sort_default),
    NAME(R.string.inventory_sort_name),
    LOAD(R.string.inventory_sort_load),
}

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
        ItemEffectType.DAMAGE_REDUCTION -> "Redução de dano"
        ItemEffectType.DIE_CATEGORY -> "Categoria de dado"
        ItemEffectType.GEM_POWER -> "Poder de gema"
        ItemEffectType.RULE -> error("handled above")
    }
    val target = resolvedTargetId.ifBlank { target }.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
    return "$typeLabel ${if (value > 0) "+" else ""}$value$target"
}
