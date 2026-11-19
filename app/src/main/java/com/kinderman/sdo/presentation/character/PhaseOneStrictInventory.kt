package com.kinderman.sdo.presentation.character

import com.kinderman.sdo.domain.model.validInventoryStates

import com.kinderman.sdo.ui.SdoCardTitle
import com.kinderman.sdo.ui.sdoClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.kinderman.sdo.ui.SdoFilterChip
import com.kinderman.sdo.ui.SdoTextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.InventoryState
import com.kinderman.sdo.domain.model.inventoryState
import com.kinderman.sdo.domain.model.initialCreationCost
import com.kinderman.sdo.domain.model.durabilityLabel
import com.kinderman.sdo.domain.model.isScrap
import com.kinderman.sdo.domain.model.isBroken
import com.kinderman.sdo.domain.model.effectiveLoad
import com.kinderman.sdo.domain.model.addInventoryItem
import com.kinderman.sdo.domain.model.withItemInventoryState
import com.kinderman.sdo.domain.model.withRemovedAshInventoryItem
import com.kinderman.sdo.domain.model.withAddedAsh
import com.kinderman.sdo.domain.model.recycleBrokenItem
import com.kinderman.sdo.domain.model.handsRequired
import com.kinderman.sdo.domain.catalog.toMysticAbility
import com.kinderman.sdo.domain.catalog.installEnhancement
import com.kinderman.sdo.domain.catalog.removeEnhancement
import com.kinderman.sdo.domain.catalog.rechargeEnhancement
import com.kinderman.sdo.domain.catalog.spendEnhancementCharge
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            inventoryFilterOptions.forEach { option ->
                val chipLabel = option.removePrefix("Categoria // ")
                SdoFilterChip(
                    label = chipLabel,
                    selected = inventoryGroup == option,
                    onClick = { inventoryGroup = option },
                )
            }
        }

        inventoryGroups(character.inventory).forEach { (group, groupItems) ->
            val visible = groupItems.filter { item ->
                (inventoryGroup == "Todos" || inventoryGroup == group ||
                    inventoryGroup.removePrefix("Categoria // ").takeIf { inventoryGroup.startsWith("Categoria // ") }
                        ?.let { item.category.equals(it, true) } == true) &&
                    (inventoryQuery.isBlank() || listOf(item.name, item.category, item.quality.label, item.effect, item.inventoryState.label).any { it.contains(inventoryQuery, true) })
            }
            if (inventoryGroup == "Todos" || inventoryGroup == group) {
                SdoMetadataText("$group // ${groupItems.size} // CARGA ${groupItems.sumOf { it.effectiveLoad() }}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                if (groupItems.isEmpty()) Text("Nenhum item nesta categoria.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            visible.forEach { item ->
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
                SdoMetadataText("${item.category.ifBlank { "OBJETO" }} // ${item.quality.label} // PG ${item.pg} // PL ${item.pl}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                if (!expanded) return@SdoInsetCard
                SdoMetadataText("REGIÃO ${item.region.ifBlank { "—" }} // CARGA ${item.effectiveLoad()} // LA ${item.agilityLimit ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (item.category.contains("arma", true) && !item.category.contains("armadura", true)) SdoMetadataText("EMPUNHADURA // ${item.handsRequired()} MÃO(S)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
                    HudTextField("Nome", item.name, enabled = enabled) { value ->
                        onChange(character.copy(inventory = character.inventory.replace(index, item.copy(name = value))))
                    }
                    HudTextField("Categoria", item.category, enabled = enabled) { value ->
                        onChange(character.copy(inventory = character.inventory.replace(index, item.copy(category = value))))
                    }
                    TwoFields(
                        { IntegerField("Quantidade", item.quantity, enabled, it) { qty ->
                            onChange(character.copy(inventory = character.inventory.replace(index, item.copy(quantity = qty.coerceAtLeast(0)))))
                        } },
                        { IntegerField("Carga", item.load, enabled, it) { load ->
                            onChange(character.copy(inventory = character.inventory.replace(index, item.copy(load = load.coerceAtLeast(0)))))
                        } },
                    )
                    TwoFields(
                        { IntegerField("Durabilidade atual", item.durabilityCurrent, enabled, it) { cur ->
                            onChange(character.copy(inventory = character.inventory.replace(index, item.copy(durabilityCurrent = cur.coerceAtLeast(0)))))
                        } },
                        { IntegerField("Durabilidade máxima", item.durabilityMax, enabled, it) { max ->
                            onChange(character.copy(inventory = character.inventory.replace(index, item.copy(durabilityMax = max.coerceAtLeast(0)))))
                        } },
                    )
                    HudTextField("Descrição", item.effect, multiline = true, enabled = enabled) { value ->
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

        val canAddItem = enabled && (!character.isInCreation || remainingHeritage > 0)
        AddButton("Adicionar item", canAddItem) { dialog = "add" }
        if (character.isInCreation && remainingHeritage == 0) {
            SdoMetadataText("LIMITE DE PH ATINGIDO // remova ou altere um item de herança para adicionar outro.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        SdoTextButton(onClick = { dialog = "glossary" }, modifier = Modifier.fillMaxWidth()) { Text("CONSULTAR GLOSSÁRIOS") }
        if (remainingHeritage > 0) {
            SdoMetadataText("CRIAÇÃO INICIAL // $remainingHeritage / ${ItemCreationRules.HERITAGE_BUDGET} PH RESTANTES", color = MaterialTheme.colorScheme.primary)
        }
    }

    when (dialog) {
        "add" -> InventoryItemModal(
            mode = if (character.isInCreation) ItemModalMode.Creation(remainingHeritage) else ItemModalMode.Sheet,
            itemCatalog = itemCatalog,
            ashCatalog = ashCatalog,
            regionOptions = character.bodyRegions.map { it.name },
            onDismiss = { dialog = null },
            onAddItem = { item ->
                onChange(character.addInventoryItem(item).copy(itemCreationDraft = null))
                dialog = null
            },
            onAddAsh = { entry, doses ->
                onChange(character.withAddedAsh(entry.toMysticAbility(), doses, character.isInCreation))
                dialog = null
            },
        )
        "glossary" -> EquipmentGlossaryDialog(
            onDismiss = { dialog = null },
            onUse = if (character.isInCreation) null else { entry ->
                onChange(character.addInventoryItem(InventoryItem(name = entry.term, category = if (entry.section == "Armas") "Arma" else "Item", effect = entry.definition)))
                dialog = null
            },
        )
    }
}

private fun inventoryGroups(items: List<InventoryItem>): Map<String, List<InventoryItem>> = linkedMapOf(
    "Armas" to items.filter { it.category.contains("arma", true) && !it.category.contains("armadura", true) },
    "Armaduras" to items.filter { it.category.contains("armadura", true) || it.category.contains("acessório", true) },
    "Itens" to items.filterNot { it.category.contains("arma", true) },
).let { groups -> groups + ("Itens" to groups.getValue("Itens").filterNot { it.category.contains("armadura", true) || it.category.contains("acessório", true) }) }


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
