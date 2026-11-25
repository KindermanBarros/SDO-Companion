package com.kinderman.sdo.presentation.character

import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kinderman.sdo.R
import com.kinderman.sdo.domain.catalog.toMysticAbility
import com.kinderman.sdo.domain.model.CanonicalItemCategory
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.addCatalogInventoryItems
import com.kinderman.sdo.domain.model.addInventoryItem
import com.kinderman.sdo.domain.model.initialCreationCost
import com.kinderman.sdo.domain.model.resolvedItemCategory
import com.kinderman.sdo.domain.model.withAddedAsh
import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.catalog.enhancementInventoryItem
import com.kinderman.sdo.domain.catalog.toInventoryItem
import com.kinderman.sdo.domain.model.AshPurity
import com.kinderman.sdo.domain.model.BuiltItem
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemCreationDraft
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.handsRequired
import com.kinderman.sdo.domain.model.heritageCostPerDose
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.SdoActionButton
import com.kinderman.sdo.ui.SdoActionStyle
import com.kinderman.sdo.ui.SdoCardTitle
import com.kinderman.sdo.ui.SdoCompactFilterBar
import com.kinderman.sdo.ui.SdoFilterChip
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.SdoTextButton
import com.kinderman.sdo.ui.SdoTheme
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.kinderman.sdo.ui.sdoClickable
import kotlin.math.ceil

internal sealed interface ItemModalMode {
    data class Creation(val remainingHeritage: Int) : ItemModalMode
    data object Sheet : ItemModalMode

    val remainingHeritageOrNull: Int?
        get() = (this as? Creation)?.remainingHeritage

    val isCreation: Boolean
        get() = this is Creation
}


private enum class ItemModalTab(val labelRes: Int) {
    CATALOG(R.string.inventory_select_catalog),
    BUILDER(R.string.inventory_create_custom),
    ENHANCEMENTS(R.string.inventory_add_enhancement),
    ASH(R.string.inventory_add_ash),
}

private enum class CatalogSort(val labelRes: Int) {
    NAME(R.string.inventory_sort_name),
    COST_ASC(R.string.inventory_sort_cost_asc),
    COST_DESC(R.string.inventory_sort_cost_desc),
    LOAD_ASC(R.string.inventory_sort_load),
}

@Composable
internal fun InventoryAcquisitionActions(
    character: Character,
    catalog: List<CatalogEntry>,
    mode: ItemModalMode,
    enabled: Boolean,
    onChange: (Character) -> Unit,
) {
    var activeTab by rememberSaveable(character.id, mode.isCreation) {
        mutableStateOf<ItemModalTab?>(null)
    }
    val itemCatalog = remember(catalog) { catalog.filter { it.kind == CatalogKind.ITEM } }
    val ashCatalog = remember(catalog) { catalog.filter { it.kind == CatalogKind.ASH } }
    ItemModalTab.entries.forEach { tab ->
        if (tab != ItemModalTab.ASH || ashCatalog.isNotEmpty()) {
            SdoActionButton(
                label = stringResource(tab.labelRes),
                onClick = { activeTab = tab },
                enabled = enabled && (tab != ItemModalTab.CATALOG || itemCatalog.isNotEmpty()),
                modifier = Modifier.fillMaxWidth().testTag("inventory_open_" + tab.name.lowercase()),
                style = SdoActionStyle.SECONDARY,
            )
        }
    }
    val tab = activeTab ?: return
    InventoryItemModal(
        mode = mode,
        tab = tab,
        itemCatalog = itemCatalog,
        ashCatalog = ashCatalog,
        regionOptions = character.bodyRegions.map { it.name },
        itemCreationDraft = character.itemCreationDraft,
        onDraftChange = { onChange(character.copy(itemCreationDraft = it)) },
        onDismiss = { activeTab = null },
        onAddCatalogItems = { items ->
            onChange(character.addCatalogInventoryItems(items))
            activeTab = null
        },
        onAddItem = { item ->
            val updated = character.addInventoryItem(item)
            onChange(if (tab == ItemModalTab.BUILDER) updated.copy(itemCreationDraft = null) else updated)
            activeTab = null
        },
        onAddAsh = { entry, doses ->
            onChange(character.withAddedAsh(entry.toMysticAbility(), doses, mode.isCreation))
            activeTab = null
        },
    )
}

@Composable
private fun InventoryItemModal(
    mode: ItemModalMode,
    tab: ItemModalTab,
    itemCatalog: List<CatalogEntry>,
    ashCatalog: List<CatalogEntry>,
    regionOptions: List<String>,
    itemCreationDraft: ItemCreationDraft?,
    onDraftChange: (ItemCreationDraft) -> Unit,
    onDismiss: () -> Unit,
    onAddCatalogItems: (List<InventoryItem>) -> Unit,
    onAddItem: (InventoryItem) -> Unit,
    onAddAsh: (entry: CatalogEntry, doses: Int) -> Unit,
) {
    var previewCost by remember(tab) { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
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
                ModalHeader(
                    mode = mode,
                    previewCost = previewCost,
                    tab = tab,
                    onDismiss = onDismiss,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        ItemModalTab.CATALOG -> CatalogTabContent(
                            mode = mode,
                            entries = itemCatalog,
                            onPreviewCostChange = { previewCost = it },
                            onDismiss = onDismiss,
                            onSelect = onAddCatalogItems,
                        )
                        ItemModalTab.BUILDER -> BuilderTabContent(
                            mode = mode,
                            regionOptions = regionOptions,
                            itemCreationDraft = itemCreationDraft,
                            onDraftChange = onDraftChange,
                            onPreviewCostChange = { previewCost = it },
                            onDismiss = onDismiss,
                            onAdd = onAddItem,
                        )
                        ItemModalTab.ENHANCEMENTS -> EnhancementsTabContent(
                            mode = mode,
                            onPreviewCostChange = { previewCost = it },
                            onDismiss = onDismiss,
                            onAdd = onAddItem,
                        )
                        ItemModalTab.ASH -> AshTabContent(
                            mode = mode,
                            entries = ashCatalog,
                            onPreviewCostChange = { previewCost = it },
                            onDismiss = onDismiss,
                            onAdd = onAddAsh,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModalHeader(
    mode: ItemModalMode,
    previewCost: Int?,
    tab: ItemModalTab,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(tab.labelRes),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            SdoTextButton(onClick = onDismiss) { Text(stringResource(R.string.inventory_close)) }
        }
        mode.remainingHeritageOrNull?.let { remaining ->
            val spent = (ItemCreationRules.HERITAGE_BUDGET - remaining).coerceAtLeast(0)
            val preview = previewCost ?: 0
            val afterPreview = remaining - preview
            val overBudget = previewCost != null && afterPreview < 0
            val budgetText = if (previewCost != null) {
                stringResource(
                    R.string.inventory_heritage_preview,
                    spent,
                    preview,
                    ItemCreationRules.HERITAGE_BUDGET,
                    afterPreview,
                )
            } else {
                stringResource(
                    R.string.inventory_heritage_remaining,
                    spent,
                    ItemCreationRules.HERITAGE_BUDGET,
                    remaining,
                )
            }
            SdoMetadataText(
                text = budgetText,
                color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
            LinearProgressIndicator(
                progress = { ((spent + preview).toFloat() / ItemCreationRules.HERITAGE_BUDGET).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CatalogTabContent(
    mode: ItemModalMode,
    entries: List<CatalogEntry>,
    onPreviewCostChange: (Int?) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (List<InventoryItem>) -> Unit,
) {
    val remainingHeritage = mode.remainingHeritageOrNull
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var onlyAffordable by rememberSaveable { mutableStateOf(mode.isCreation) }
    val defaultSort = if (mode.isCreation) CatalogSort.COST_ASC else CatalogSort.NAME
    var sort by rememberSaveable { mutableStateOf(defaultSort) }
    var selectedIds by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var toolNames by rememberSaveable { mutableStateOf(Bundle()) }
    var reviewing by rememberSaveable { mutableStateOf(false) }
    val itemEntries = remember(entries, mode.isCreation) {
        entries.filter { !mode.isCreation || it.creationCost.toIntOrNull() != null }
            .distinctBy(CatalogEntry::id)
    }
    val resolvedCategories = remember(itemEntries) {
        itemEntries.associate { it.id to it.resolvedItemCategory() }
    }
    val categories = remember(resolvedCategories) {
        resolvedCategories.values.sortedBy { it.displayLabel }.associate { it.filterKey to it.displayLabel }
    }
    val costs = remember(itemEntries, mode.isCreation) {
        if (mode.isCreation) itemEntries.associate { it.id to it.toInventoryItem(true).initialCreationCost() }
        else emptyMap()
    }
    val sortOptions = if (mode.isCreation) {
        listOf(CatalogSort.COST_ASC, CatalogSort.COST_DESC, CatalogSort.NAME)
    } else {
        listOf(CatalogSort.NAME, CatalogSort.LOAD_ASC)
    }
    val filtered = remember(itemEntries, query, category, onlyAffordable, remainingHeritage, sort) {
        val needle = query.trim()
        val matched = itemEntries.filter { entry ->
            val resolved = resolvedCategories.getValue(entry.id)
            (category !in categories || resolved.filterKey == category) &&
                (!onlyAffordable || remainingHeritage == null || costs.getValue(entry.id) <= remainingHeritage) &&
                (entry.searchableText().contains(needle, true) || resolved.displayLabel.contains(needle, true))
        }
        when (sort) {
            CatalogSort.NAME -> matched.sortedBy { it.name.lowercase() }
            CatalogSort.COST_ASC -> matched.sortedWith(
                compareBy<CatalogEntry> { costs.getValue(it.id) }.thenBy { it.name.lowercase() },
            )
            CatalogSort.COST_DESC -> matched.sortedWith(
                compareByDescending<CatalogEntry> { costs.getValue(it.id) }.thenBy { it.name.lowercase() },
            )
            CatalogSort.LOAD_ASC -> matched.sortedWith(
                compareBy(CatalogEntry::load, { it.name.lowercase() }),
            )
        }
    }
    val selectedEntries = remember(itemEntries, selectedIds) {
        val entriesById = itemEntries.associateBy(CatalogEntry::id)
        selectedIds.mapNotNull(entriesById::get)
    }
    val selectedCost = selectedEntries.sumOf { costs[it.id] ?: 0 }
    val overBudget = remainingHeritage != null && selectedCost > remainingHeritage
    val missingToolName = selectedEntries.any {
        resolvedCategories.getValue(it.id).canonical == CanonicalItemCategory.TOOL &&
            toolNames.getString(it.id).isNullOrBlank()
    }
    SideEffect {
        onPreviewCostChange(selectedCost.takeIf { mode.isCreation && selectedEntries.isNotEmpty() })
    }
    val selectionLabel = pluralStringResource(
        R.plurals.inventory_selected_count,
        selectedEntries.size,
        selectedEntries.size,
    )
    val affordableLabel = stringResource(R.string.inventory_affordable)
    val categoryLabel = categories[category]
    val sortLabels = sortOptions.associateWith { stringResource(it.labelRes) }
    val sortLabel = sortLabels.getValue(sort)

    val catalogListState = rememberLazyListState()
    val reviewListState = rememberLazyListState()
    Column(Modifier.fillMaxSize().testTag("inventory_catalog_picker")) {
        SdoTextButton(
            onClick = { reviewing = !reviewing },
            enabled = reviewing || selectedEntries.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("inventory_review_selection"),
        ) {
            Text(if (reviewing) stringResource(R.string.inventory_back_to_catalog) else selectionLabel)
        }
        LazyColumn(
            state = if (reviewing) reviewListState else catalogListState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!reviewing) {
                item(key = "catalog-filters") {
                    SdoCompactFilterBar(
                        activeFilters = listOfNotNull(
                            query.trim().takeIf(String::isNotEmpty),
                            categoryLabel,
                            affordableLabel.takeIf { onlyAffordable },
                            sortLabel.takeIf { sort != defaultSort },
                        ),
                        onClear = {
                            query = ""
                            category = ""
                            onlyAffordable = false
                            sort = defaultSort
                        },
                        modifier = Modifier.testTag("inventory_catalog_filters"),
                    ) {
                        HudTextField(stringResource(R.string.inventory_catalog_search), query) { query = it }
                        InventoryCategoryFilter(categories, category) { category = it }
                        ChoiceField(
                            stringResource(R.string.inventory_sort),
                            sort,
                            sortOptions,
                            true,
                            modifier = Modifier.testTag("inventory_catalog_sort"),
                            display = sortLabels::getValue,
                        ) { sort = it }
                        if (mode.isCreation) {
                            SdoFilterChip(
                                label = affordableLabel,
                                selected = onlyAffordable,
                                onClick = { onlyAffordable = !onlyAffordable },
                            )
                        }
                    }
                }
            }
            val displayed = if (reviewing) selectedEntries else filtered
            if (displayed.isEmpty()) {
                item(key = "catalog-empty") {
                    Text(
                        stringResource(
                            if (reviewing) R.string.inventory_selection_empty else R.string.inventory_no_results,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(displayed, key = { "catalog:" + it.id }) { entry ->
                val resolved = resolvedCategories.getValue(entry.id)
                val selected = entry.id in selectedIds
                val numericCost = costs[entry.id]
                SelectableModalCard(
                    selected = selected,
                    modifier = Modifier.testTag("inventory_catalog_entry_" + entry.id),
                    onClick = {
                        selectedIds = ArrayList(
                            if (selected) selectedIds - entry.id else selectedIds + entry.id,
                        )
                        if (selected) toolNames = android.os.Bundle(toolNames).apply { remove(entry.id) }
                    },
                ) {
                    Text(entry.name, style = MaterialTheme.typography.titleSmall)
                    if (selected) Text(stringResource(R.string.inventory_entry_selected), style = MaterialTheme.typography.labelSmall)
                    val loadLabel = stringResource(R.string.inventory_load, entry.load)
                    val costLabel = numericCost?.let { stringResource(R.string.inventory_heritage_cost, it) }
                    SdoMetadataText(
                        listOfNotNull(
                            resolved.displayLabel,
                            loadLabel,
                            costLabel,
                            entry.durability.takeIf(String::isNotBlank)
                                ?.let { stringResource(R.string.inventory_durability, it) },
                            entry.region.takeIf(String::isNotBlank),
                        ).joinToString(" // "),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    if (entry.summary.isNotBlank()) {
                        Text(entry.summary, style = MaterialTheme.typography.bodySmall)
                    }
                    if (selected && entry.mechanicalEffect.isNotBlank() && entry.mechanicalEffect != entry.summary) {
                        Text(entry.mechanicalEffect, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (selected && resolved.canonical == CanonicalItemCategory.TOOL) {
                    HudTextField(
                        stringResource(R.string.inventory_tool_name),
                        toolNames.getString(entry.id).orEmpty(),
                    ) { name ->
                        toolNames = android.os.Bundle(toolNames).apply { putString(entry.id, name) }
                    }
                }
            }
        }
        ModalBottomActions(
            secondaryLabel = stringResource(R.string.inventory_cancel),
            onSecondary = onDismiss,
            primaryLabel = when {
                overBudget -> stringResource(R.string.inventory_over_budget)
                missingToolName -> stringResource(R.string.inventory_missing_tool_name)
                else -> stringResource(R.string.inventory_confirm_selection)
            },
            primaryEnabled = selectedEntries.isNotEmpty() && !overBudget && !missingToolName,
            onPrimary = {
                onSelect(selectedEntries.map { entry ->
                    val item = entry.toInventoryItem(initialCreation = mode.isCreation)
                    val toolName = toolNames.getString(entry.id)?.trim()
                    if (toolName.isNullOrEmpty()) item else item.copy(name = entry.name + " — " + toolName)
                })
            },
        )
    }
}

@Composable
internal fun InventoryCategoryFilter(
    categories: Map<String, String>,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    if (categories.isEmpty()) return
    val allCategories = stringResource(R.string.inventory_all_categories)
    ChoiceField(
        stringResource(R.string.inventory_category),
        selectedKey.takeIf(categories::containsKey).orEmpty(),
        listOf("") + categories.keys,
        true,
        modifier = Modifier.testTag("inventory_category_filter"),
        display = { categories[it] ?: allCategories },
        onValue = onSelect,
    )
}

private enum class BuilderKind(val label: String, val draftCategory: String) {
    WEAPON("Arma", "Arma"),
    ARMOR("Armadura", "Armadura"),
    ACCESSORY("Acessório", "Acessório"),
    FREE_ITEM("Item livre", "Item"),
    NARRATIVE("Narrativo", "Narrativo"),
}

private enum class BuilderStep(val label: String) {
    BASE("Base"),
    MATERIAL("Material"),
    QUALITY("Qualidade"),
    MODIFICATIONS("Modificações"),
    ENHANCEMENTS("Aprimoramentos"),
    REVIEW("Revisão"),
}

@Composable
private fun BuilderTabContent(
    mode: ItemModalMode,
    regionOptions: List<String>,
    itemCreationDraft: ItemCreationDraft?,
    onDraftChange: (ItemCreationDraft) -> Unit,
    onPreviewCostChange: (Int?) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (InventoryItem) -> Unit,
) {
    val remainingHeritage = mode.remainingHeritageOrNull
    val initialCreation = mode.isCreation
    val availableKinds = remember(initialCreation) {
        if (initialCreation) {
            listOf(BuilderKind.WEAPON, BuilderKind.ARMOR, BuilderKind.ACCESSORY)
        } else {
            BuilderKind.entries
        }
    }
    var draft by remember { mutableStateOf(itemCreationDraft ?: ItemCreationDraft()) }
    val kind = availableKinds.firstOrNull { it.draftCategory == draft.category } ?: BuilderKind.WEAPON
    val stepIndex = (draft.step - 1).coerceIn(0, BuilderStep.entries.lastIndex)
    var searchQuery by remember(kind, stepIndex) { mutableStateOf("") }

    fun updateDraft(next: ItemCreationDraft) {
        draft = next
        onDraftChange(next)
    }

    fun selectKind(selected: BuilderKind) {
        if (selected == kind) return
        updateDraft(
            draft.copy(
                category = selected.draftCategory,
                step = 1,
                baseId = "",
                materialId = "",
                secondaryMaterialId = "",
                modificationIds = emptyList(),
                enhancementIds = emptyList(),
            ),
        )
    }

    if (kind == BuilderKind.NARRATIVE) {
        SideEffect { onPreviewCostChange(null) }
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BuilderKindSelector(availableKinds, kind, ::selectKind)
                Text(
                    text = "Crie um objeto narrativo sem valores mecânicos fixos. Você poderá editá-lo depois no inventário.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                HudTextField("Nome do item", draft.commonName) { updateDraft(draft.copy(commonName = it)) }
                HudTextField("Descrição ou uso narrativo (opcional)", draft.commonEffect, multiline = true) {
                    updateDraft(draft.copy(commonEffect = it))
                }
            }
            ModalBottomActions(
                secondaryLabel = "Cancelar",
                onSecondary = onDismiss,
                primaryLabel = "Adicionar narrativo",
                primaryEnabled = true,
                onPrimary = {
                    onAdd(
                        InventoryItem(
                            name = draft.commonName.trim().ifBlank { "Item narrativo" },
                            category = "Item",
                            effect = draft.commonEffect.trim(),
                            quantity = 1,
                        ),
                    )
                },
            )
        }
        return
    }

    if (kind == BuilderKind.FREE_ITEM) {
        SideEffect { onPreviewCostChange(null) }
        val freeCategories = remember {
            listOf("Item", "Arma", "Armadura", "Acessório", "Escudo", "Consumível", "Munição", "Ferramenta", "Recipiente de Carga")
        }
        val qualityOptions = remember { ItemCreationRules.qualitiesFor(initialCreation = false) }
        val quality = draft.quality.takeIf { it in qualityOptions } ?: ItemQuality.COMMON
        val regions = remember(regionOptions) { listOf("Nenhuma") + regionOptions.distinct() }

        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BuilderKindSelector(availableKinds, kind, ::selectKind)
                HudTextField("Nome do item (obrigatório)", draft.commonName) { updateDraft(draft.copy(commonName = it)) }
                SdoMetadataText("CATEGORIA", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    freeCategories.forEach { cat ->
                        SdoFilterChip(
                            label = cat,
                            selected = draft.commonCategory.equals(cat, ignoreCase = true),
                            onClick = { updateDraft(draft.copy(commonCategory = cat)) },
                        )
                    }
                }
                SdoMetadataText("QUALIDADE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    qualityOptions.forEach { opt ->
                        SdoFilterChip(
                            label = opt.label,
                            selected = quality == opt,
                            onClick = { updateDraft(draft.copy(quality = opt)) },
                        )
                    }
                }
                TwoFields(
                    { IntegerField("Quantidade", draft.commonQuantity, true, it) { value -> updateDraft(draft.copy(commonQuantity = value.coerceAtLeast(1))) } },
                    { IntegerField("Carga total", draft.commonLoad, true, it) { value -> updateDraft(draft.copy(commonLoad = value.coerceAtLeast(0))) } },
                )
                SdoMetadataText("REGIÃO CORPORAL", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    regions.forEach { region ->
                        val selected = (draft.commonRegion.ifBlank { "Nenhuma" }).equals(region, ignoreCase = true)
                        SdoFilterChip(
                            label = region,
                            selected = selected,
                            onClick = { updateDraft(draft.copy(commonRegion = if (region == "Nenhuma") "" else region)) },
                        )
                    }
                }
                TwoFields(
                    { IntegerField("Durabilidade", draft.commonDurability.coerceAtLeast(1), true, it) { value -> updateDraft(draft.copy(commonDurability = value.coerceAtLeast(1))) } },
                    { IntegerField("Alcance", draft.commonRange, true, it) { value -> updateDraft(draft.copy(commonRange = value.coerceAtLeast(0))) } },
                )
                TwoFields(
                    { IntegerField("PG", draft.commonPg, true, it) { value -> updateDraft(draft.copy(commonPg = value)) } },
                    { IntegerField("PL", draft.commonPl, true, it) { value -> updateDraft(draft.copy(commonPl = value)) } },
                )
                TwoFields(
                    { IntegerField("Ataque", draft.commonAttack, true, it) { value -> updateDraft(draft.copy(commonAttack = value)) } },
                    { IntegerField("Dano", draft.commonDamage, true, it) { value -> updateDraft(draft.copy(commonDamage = value)) } },
                )
                HudTextField("Descrição ou efeito", draft.commonEffect, multiline = true) { updateDraft(draft.copy(commonEffect = it)) }
            }
            ModalBottomActions(
                secondaryLabel = "Cancelar",
                onSecondary = onDismiss,
                primaryLabel = if (draft.commonName.isNotBlank()) "Criar item" else "Informe o nome",
                primaryEnabled = draft.commonName.isNotBlank(),
                onPrimary = {
                    val effects = buildList {
                        if (draft.commonPg != 0) add(ItemEffect("custom:pg", ItemEffectType.PG, draft.commonPg, condition = ItemEffectCondition.EQUIPPED))
                        if (draft.commonPl != 0) add(ItemEffect("custom:pl", ItemEffectType.PL, draft.commonPl, target = draft.commonRegion, condition = ItemEffectCondition.EQUIPPED))
                        draft.commonAgilityLimit?.let { add(ItemEffect("custom:la", ItemEffectType.AGILITY_LIMIT, it, condition = ItemEffectCondition.EQUIPPED)) }
                        if (draft.commonAttack != 0) add(ItemEffect("custom:attack", ItemEffectType.ATTACK, draft.commonAttack, condition = ItemEffectCondition.WIELDED))
                        if (draft.commonDamage != 0) add(ItemEffect("custom:damage", ItemEffectType.PHYSICAL_DAMAGE, draft.commonDamage, condition = ItemEffectCondition.WIELDED))
                    }
                    onAdd(
                        InventoryItem(
                            name = draft.commonName.trim(),
                            category = draft.commonCategory.ifBlank { "Item" },
                            effect = draft.commonEffect.trim(),
                            load = draft.commonLoad,
                            quantity = draft.commonQuantity.coerceAtLeast(1),
                            region = draft.commonRegion,
                            durabilityCurrent = draft.commonDurability.coerceAtLeast(1),
                            durabilityMax = draft.commonDurability.coerceAtLeast(1),
                            pg = draft.commonPg,
                            pl = draft.commonPl,
                            agilityLimit = draft.commonAgilityLimit,
                            quality = quality,
                            mechanicalEffects = effects,
                        ),
                    )
                },
            )
        }
        return
    }

    // Structured builder: Weapon / Armor / Accessory
    val weapon = kind == BuilderKind.WEAPON
    val basePool = remember(kind) {
        when (kind) {
            BuilderKind.WEAPON -> ItemCreationRules.weaponBases
            BuilderKind.ACCESSORY -> ItemCreationRules.armorBases.filter { it.group == "Acessório" }
            else -> ItemCreationRules.armorBases.filterNot { it.group == "Acessório" }
        }
    }
    if (basePool.isEmpty()) {
        SideEffect { onPreviewCostChange(null) }
        EmptyBuilderCatalogState(onDismiss)
        return
    }
    val base = basePool.firstOrNull { it.id == draft.baseId } ?: basePool.first()
    val materialPool = remember(weapon, base.id, initialCreation) {
        val raw = if (weapon) ItemCreationRules.weaponMaterials else ItemCreationRules.armorMaterials
        val restricted = if (!weapon && base.id == "gibao") raw.filter { it.id == "organico" } else raw
        ItemCreationRules.materialsFor(restricted, initialCreation)
    }
    if (materialPool.isEmpty()) {
        SideEffect { onPreviewCostChange(null) }
        EmptyBuilderCatalogState(onDismiss)
        return
    }
    val material = materialPool.firstOrNull { it.id == draft.materialId }
        ?: materialPool.firstOrNull { it.id == if (!weapon && base.id == "gibao") "organico" else "ligas_comuns" }
        ?: materialPool.first()
    val hasAlloy = material.id.startsWith("ligas_")
    val secondaryMaterial = if (hasAlloy) {
        materialPool.firstOrNull { it.id == draft.secondaryMaterialId && it.id != material.id }
    } else null
    val qualityOptions = remember(initialCreation) { ItemCreationRules.qualitiesFor(initialCreation) }
    val quality = draft.quality.takeIf { it in qualityOptions } ?: ItemQuality.COMMON
    val availableModifications = remember(base.id, weapon) {
        ItemCreationRules.compatibleModifications(base, weapon)
    }
    val modifications = availableModifications.filter { it.id in draft.modificationIds }
    val enhancementSlots = if (quality == ItemQuality.MUNDANE) 0 else {
        base.enhancementSlots + modifications.sumOf(ItemPart::enhancementSlots).coerceAtMost(5)
    }
    val enhancementOptions = remember(base.id, initialCreation) {
        ItemCreationRules.compatibleEnhancements(base, initialCreation)
    }
    val enhancements = enhancementOptions.filter { it.id in draft.enhancementIds }.take(enhancementSlots)
    val gems = enhancements.filterNot { ItemCreationRules.enhancementIsTechnology(it.id) }
    val technologies = enhancements.filter { ItemCreationRules.enhancementIsTechnology(it.id) }
    val built: BuiltItem = ItemCreationRules.build(
        base = base,
        material = material,
        secondaryMaterial = secondaryMaterial,
        modifications = modifications,
        gemSlots = 0,
        technologySlots = 0,
        customName = draft.customName,
        quality = quality,
        components = gems,
        technologies = technologies,
        enhancementSlots = enhancementSlots,
    )
    val builtCost = built.creationCost
    val allowedByBudget = remainingHeritage == null || (builtCost != null && builtCost <= remainingHeritage)
    SideEffect { onPreviewCostChange(if (initialCreation) builtCost else null) }

    val steps = BuilderStep.entries
    val currentStep = steps[stepIndex.coerceIn(0, steps.lastIndex)]

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BuilderKindSelector(availableKinds, kind, ::selectKind)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                steps.forEachIndexed { idx, step ->
                    SdoFilterChip(
                        label = "${idx + 1}. ${step.label}",
                        selected = currentStep == step,
                        onClick = { updateDraft(draft.copy(step = idx + 1)) },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (currentStep) {
                BuilderStep.BASE -> {
                    HudTextField("Nome personalizado (opcional)", draft.customName) {
                        updateDraft(draft.copy(customName = it))
                    }
                    HudTextField("Filtrar tipo de ${kind.label.lowercase()}", searchQuery) { searchQuery = it }
                    val filteredBases = basePool.filter { part ->
                        searchQuery.isBlank() ||
                            part.name.contains(searchQuery, true) ||
                            part.effect.contains(searchQuery, true) ||
                            part.region.contains(searchQuery, true)
                    }
                    filteredBases.forEach { part ->
                        SelectableModalCard(
                            selected = base.id == part.id,
                            onClick = {
                                val nextMaterialId = if (!weapon && part.id == "gibao") "organico" else draft.materialId
                                val nextMods = draft.modificationIds.filter { id ->
                                    ItemCreationRules.compatibleModifications(part, weapon).any { it.id == id }
                                }
                                updateDraft(draft.copy(
                                    baseId = part.id,
                                    materialId = nextMaterialId,
                                    modificationIds = nextMods,
                                ))
                            },
                        ) {
                            PartCardSummary(part = part, weapon = weapon, showHeritageCost = initialCreation)
                        }
                    }
                }
                BuilderStep.MATERIAL -> {
                    HudTextField("Filtrar material", searchQuery) { searchQuery = it }
                    val filteredMaterials = materialPool.filter { part ->
                        searchQuery.isBlank() ||
                            part.name.contains(searchQuery, true) ||
                            part.effect.contains(searchQuery, true)
                    }
                    SdoMetadataText("MATERIAL PRINCIPAL", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    filteredMaterials.forEach { part ->
                        SelectableModalCard(
                            selected = material.id == part.id,
                            onClick = {
                                updateDraft(draft.copy(
                                    materialId = part.id,
                                    secondaryMaterialId = draft.secondaryMaterialId.takeIf { part.id.startsWith("ligas_") && it != part.id }.orEmpty(),
                                ))
                            },
                        ) {
                            PartCardSummary(part = part, weapon = weapon, showHeritageCost = initialCreation)
                        }
                    }
                    if (hasAlloy) {
                        SdoMetadataText("LIGA // SEGUNDO MATERIAL (OPCIONAL)", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        SelectableModalCard(
                            selected = secondaryMaterial == null,
                            onClick = { updateDraft(draft.copy(secondaryMaterialId = "")) },
                        ) {
                            Text("Material puro (sem mistura)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                        materialPool.filter { it.id != material.id }.forEach { part ->
                            SelectableModalCard(
                                selected = secondaryMaterial?.id == part.id,
                                onClick = { updateDraft(draft.copy(secondaryMaterialId = part.id)) },
                            ) {
                                PartCardSummary(part = part, weapon = weapon, showHeritageCost = initialCreation)
                            }
                        }
                    }
                }
                BuilderStep.QUALITY -> {
                    qualityOptions.forEach { opt ->
                        SelectableModalCard(
                            selected = quality == opt,
                            onClick = { updateDraft(draft.copy(quality = opt)) },
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(opt.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                if (initialCreation) {
                                    SdoMetadataText(
                                        text = "${if (opt.creationAdjustment >= 0) "+" else ""}${opt.creationAdjustment} PH",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            Text(
                                text = ItemCreationRules.qualityEffect(opt, armor = !weapon)
                                    ?: "Funcionamento padrão, sem bônus adicionais.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                BuilderStep.MODIFICATIONS -> {
                    HudTextField("Filtrar modificação por nome, grupo ou efeito", searchQuery) { searchQuery = it }
                    val filteredMods = availableModifications.filter { mod ->
                        searchQuery.isBlank() ||
                            mod.name.contains(searchQuery, true) ||
                            mod.group.contains(searchQuery, true) ||
                            mod.effect.contains(searchQuery, true)
                    }
                    if (filteredMods.isEmpty()) {
                        Text(
                            text = "Nenhuma modificação compatível encontrada.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        filteredMods.groupBy(ItemPart::group).forEach { (group, groupMods) ->
                            if (group.isNotBlank()) {
                                SdoMetadataText(group.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            }
                            groupMods.forEach { mod ->
                                val checked = mod in modifications
                                SelectableModalCard(
                                    selected = checked,
                                    onClick = {
                                        val next = ItemCreationRules.toggleModification(modifications, mod).map(ItemPart::id)
                                        updateDraft(draft.copy(modificationIds = next))
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Checkbox(checked = checked, onCheckedChange = null)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            PartCardSummary(part = mod, weapon = weapon, showHeritageCost = initialCreation)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                BuilderStep.ENHANCEMENTS -> {
                    SdoMetadataText(
                        text = "ESPAÇOS DE APRIMORAMENTO // ${enhancements.size} / $enhancementSlots",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (quality == ItemQuality.MUNDANE || enhancementSlots == 0) {
                        Text(
                            text = "Itens mundanos ou sem encaixes não aceitam aprimoramentos. Adicione uma modificação de Encaixe ou altere a qualidade.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        HudTextField("Filtrar aprimoramento", searchQuery) { searchQuery = it }
                        val filteredEnh = enhancementOptions.filter { enh ->
                            searchQuery.isBlank() ||
                                enh.name.contains(searchQuery, true) ||
                                enh.effect.contains(searchQuery, true)
                        }
                        filteredEnh.forEach { enh ->
                            val checked = enhancements.any { it.id == enh.id }
                            val canToggle = checked || enhancements.size < enhancementSlots
                            SelectableModalCard(
                                selected = checked,
                                enabled = canToggle,
                                onClick = {
                                    val nextIds = if (checked) {
                                        draft.enhancementIds - enh.id
                                    } else if (enhancements.size < enhancementSlots) {
                                        draft.enhancementIds + enh.id
                                    } else {
                                        draft.enhancementIds
                                    }
                                    updateDraft(draft.copy(enhancementIds = nextIds))
                                },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = null, enabled = canToggle)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(enh.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                        SdoMetadataText(
                                            text = buildList {
                                                add(if (ItemCreationRules.enhancementIsTechnology(enh.id)) "TECNOLOGIA" else "GEMA")
                                                if (enh.materialTier.isNotBlank()) add(enh.materialTier)
                                                if (initialCreation) add("${enh.creationCost ?: "#"} PH")
                                            }.joinToString(" // "),
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                        if (enh.effect.isNotBlank()) {
                                            Text(enh.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                BuilderStep.REVIEW -> {
                    val mixedMaterial = ItemCreationRules.mixMaterials(material, secondaryMaterial)
                    SdoInsetCard(contentPadding = 14.dp, verticalSpacing = 6.dp) {
                        SdoCardTitle(built.name, style = MaterialTheme.typography.titleLarge)
                        SdoMetadataText(
                            text = "${kind.label} // ${mixedMaterial.name} // ${quality.label}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        SdoMetadataText(
                            text = buildList {
                                add("PG ${built.pg}")
                                add("PL ${built.pl}")
                                add("CARGA ${built.load}")
                                add("DUR ${built.durability}")
                                built.agilityLimit?.let { add("LA $it") }
                                if (weapon) add("${built.toInventoryItem().handsRequired()} MÃO(S)")
                                if (built.region.isNotBlank()) add(built.region)
                            }.joinToString(" // "),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (initialCreation) {
                            SdoMetadataText(
                                text = "CUSTO TOTAL // ${builtCost ?: "#"} PH",
                                color = if (allowedByBudget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        if (modifications.isNotEmpty()) {
                            SdoMetadataText("MODIFICAÇÕES // ${modifications.joinToString { it.name }}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        if (gems.isNotEmpty()) {
                            SdoMetadataText("GEMAS // ${gems.joinToString { it.name }}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        if (technologies.isNotEmpty()) {
                            SdoMetadataText("TECNOLOGIAS // ${technologies.joinToString { it.name }}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        val effects = buildList {
                            add(base.effect)
                            add(mixedMaterial.effect)
                            mixedMaterial.traitIds.mapTo(this) { traitId ->
                                "${ItemCreationRules.traitName(traitId)}: ${ItemCreationRules.traitDescription(traitId)}"
                            }
                            ItemCreationRules.qualityEffect(quality, armor = !weapon)?.let(::add)
                            modifications.mapTo(this) { "${it.name}: ${it.effect}" }
                            gems.mapTo(this) { "${it.name}: ${it.effect}" }
                            technologies.mapTo(this) { "${it.name}: ${it.effect}" }
                        }.filter(String::isNotBlank).distinct()
                        if (effects.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            effects.forEach { effect ->
                                Text("• $effect", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (initialCreation && !allowedByBudget) {
                            Text(
                                text = if (builtCost == null) "Itens # não podem ser criados com PH." else "Custo acima dos Pontos de Herança restantes.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        val isLastStep = stepIndex >= steps.lastIndex
        ModalBottomActions(
            secondaryLabel = if (stepIndex > 0) "Voltar" else "Cancelar",
            onSecondary = { if (stepIndex > 0) updateDraft(draft.copy(step = stepIndex)) else onDismiss() },
            primaryLabel = when {
                !isLastStep -> "Continuar"
                !allowedByBudget -> "Saldo PH insuficiente"
                initialCreation && builtCost != null -> "Criar item • $builtCost PH"
                else -> "Criar item"
            },
            primaryEnabled = !isLastStep || allowedByBudget,
            onPrimary = {
                if (!isLastStep) {
                    updateDraft(draft.copy(step = stepIndex + 2))
                } else {
                    onAdd(built.toInventoryItem(initialCreation = initialCreation))
                }
            },
        )
    }
}

@Composable
private fun EnhancementsTabContent(
    mode: ItemModalMode,
    onPreviewCostChange: (Int?) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (InventoryItem) -> Unit,
) {
    val remainingHeritage = mode.remainingHeritageOrNull
    var showTechnology by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    val options = remember(showTechnology, mode.isCreation) {
        ItemCreationRules.enhancementCreationOptions(technology = if (mode.isCreation) false else showTechnology)
    }

    if (mode.isCreation) {
        val categories = remember(options) {
            options.map { ItemCreationRules.enhancementCategory(it.id) }.distinct().sorted()
        }
        SideEffect { onPreviewCostChange(null) }
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "gem-info") {
                    Text(
                        text = "Na criação inicial, escolha a categoria para sortear uma gema dentro do seu saldo de PH. Opções ancestrais não participam da criação.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                itemsIndexed(categories, key = { _, cat -> cat }) { _, category ->
                    val eligible = options.filter {
                        ItemCreationRules.enhancementCategory(it.id) == category &&
                            (it.creationCost ?: Int.MAX_VALUE) <= (remainingHeritage ?: Int.MAX_VALUE)
                    }
                    val minCost = eligible.mapNotNull(ItemPart::creationCost).minOrNull()
                    SdoInsetCard(contentPadding = 12.dp, verticalSpacing = 6.dp) {
                        Text(category, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        SdoMetadataText(
                            text = if (eligible.isNotEmpty()) "${eligible.size} OPÇÕES COMPATÍVEIS // A PARTIR DE ${minCost ?: 1} PH" else "SEM OPÇÕES DENTRO DO SALDO",
                            color = if (eligible.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        SdoActionButton(
                            label = "Sortear gema",
                            onClick = { onAdd(enhancementInventoryItem(eligible.random().id, initialCreation = true)) },
                            enabled = eligible.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            style = SdoActionStyle.PRIMARY,
                        )
                    }
                }
            }
            ModalBottomActions(
                secondaryLabel = "Fechar",
                onSecondary = onDismiss,
                primaryLabel = null,
                primaryEnabled = false,
                onPrimary = {},
            )
        }
        return
    }

    val filtered = remember(options, query) {
        val needle = query.trim()
        options.filter { enh ->
            needle.isEmpty() ||
                enh.name.contains(needle, true) ||
                enh.effect.contains(needle, true) ||
                ItemCreationRules.enhancementCategory(enh.id).contains(needle, true)
        }
    }
    val selected = options.firstOrNull { it.id == selectedId }
    SideEffect { onPreviewCostChange(null) }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SdoFilterChip(label = "Gemas", selected = !showTechnology, onClick = { showTechnology = false; selectedId = null })
                SdoFilterChip(label = "Tecnologias", selected = showTechnology, onClick = { showTechnology = true; selectedId = null })
            }
            HudTextField("Buscar aprimoramento", query) { query = it }
            SdoMetadataText("RESULTADOS // ${filtered.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(filtered, key = { index, enh -> "${enh.id}:$index" }) { _, enh ->
                SelectableModalCard(
                    selected = selectedId == enh.id,
                    onClick = { selectedId = if (selectedId == enh.id) null else enh.id },
                ) {
                    Text(enh.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    SdoMetadataText(
                        text = "${ItemCreationRules.enhancementRarity(enh.id)} // ${ItemCreationRules.enhancementCategory(enh.id)}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    if (enh.effect.isNotBlank()) {
                        Text(enh.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        ModalBottomActions(
            secondaryLabel = "Cancelar",
            onSecondary = onDismiss,
            primaryLabel = if (selected != null) "Adicionar ${if (showTechnology) "tecnologia" else "gema"}" else "Selecione um item",
            primaryEnabled = selected != null,
            onPrimary = { selected?.let { onAdd(enhancementInventoryItem(it.id)) } },
        )
    }
}

@Composable
private fun AshTabContent(
    mode: ItemModalMode,
    entries: List<CatalogEntry>,
    onPreviewCostChange: (Int?) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (CatalogEntry, Int) -> Unit,
) {
    val remainingHeritage = mode.remainingHeritageOrNull
    var query by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf("Todas") }
    var selectedAshName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf("") }
    var doses by rememberSaveable { mutableIntStateOf(1) }

    val sources = remember(entries) {
        listOf("Todas") + entries.mapNotNull { it.catalogAshSource?.label }.distinct().sorted()
    }
    val grouped = remember(entries) { entries.groupBy(CatalogEntry::name).toSortedMap() }
    val filteredGroups = remember(grouped, source, query) {
        grouped.filterValues { variants ->
            (source == "Todas" || variants.any { it.catalogAshSource?.label == source }) &&
                (query.isBlank() || variants.any { it.searchableText().contains(query, ignoreCase = true) })
        }.entries.toList()
    }
    val selectedVariants = remember(selectedAshName, grouped) {
        selectedAshName?.let(grouped::get).orEmpty().sortedBy { entry ->
            AshPurity.entries.indexOf(entry.catalogAshPurity ?: AshPurity.RAW)
        }
    }
    val selected = selectedVariants.firstOrNull { it.id == selectedId }
    val purity = selected?.catalogAshPurity ?: AshPurity.RAW
    val previewCost = if (selected != null) doses.coerceAtLeast(1) * purity.heritageCostPerDose else null
    val allowed = selected != null && (remainingHeritage == null || (previewCost != null && previewCost <= remainingHeritage))
    SideEffect { onPreviewCostChange(if (remainingHeritage != null) previewCost else null) }

    if (selectedAshName == null) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HudTextField("Buscar por nome, fonte ou efeito", query) { query = it }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    sources.forEach { src ->
                        SdoFilterChip(
                            label = src,
                            selected = source == src,
                            onClick = { source = src },
                        )
                    }
                }
                SdoMetadataText("CINZAS // ${filteredGroups.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(filteredGroups, key = { index, (name, _) -> "$name:$index" }) { _, (name, variants) ->
                    SelectableModalCard(
                        selected = false,
                        onClick = {
                            selectedAshName = name
                            selectedId = variants.firstOrNull { it.catalogAshPurity == AshPurity.RAW }?.id
                                ?: variants.firstOrNull()?.id.orEmpty()
                            doses = 1
                        },
                    ) {
                        Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        SdoMetadataText(
                            text = "${variants.firstOrNull()?.catalogAshSource?.label ?: "Fonte desconhecida"} // ${variants.size} PUREZAS",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            ModalBottomActions(
                secondaryLabel = "Fechar",
                onSecondary = onDismiss,
                primaryLabel = null,
                primaryEnabled = false,
                onPrimary = {},
            )
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(selectedAshName.orEmpty().uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            SdoMetadataText(
                text = selectedVariants.firstOrNull()?.catalogAshSource?.label ?: "Fonte desconhecida",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
            selectedVariants.forEach { entry ->
                val entryPurity = entry.catalogAshPurity ?: AshPurity.RAW
                SelectableModalCard(
                    selected = entry.id == selectedId,
                    onClick = { selectedId = entry.id },
                ) {
                    Text(entryPurity.label.uppercase(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(entry.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    SdoMetadataText(
                        text = buildString {
                            append("${entryPurity.dosesPerLoad} DOSE(S) POR CARGA")
                            if (remainingHeritage != null) append(" // ${entryPurity.heritageCostPerDose} PH POR DOSE")
                        },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            IntegerField("Doses", doses, true) { doses = it.coerceAtLeast(1) }
            selected?.let {
                SdoMetadataText(
                    text = "CARGA ${ceil(doses.toDouble() / purity.dosesPerLoad).toInt()} // ${purity.dosesPerLoad} DOSE(S) POR CARGA",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        ModalBottomActions(
            secondaryLabel = "Voltar à lista",
            onSecondary = {
                selectedAshName = null
                selectedId = ""
            },
            primaryLabel = when {
                !allowed -> "Saldo PH insuficiente"
                remainingHeritage != null && previewCost != null -> "Adicionar • $previewCost PH"
                else -> "Adicionar cinzas"
            },
            primaryEnabled = allowed,
            onPrimary = { selected?.let { onAdd(it, doses.coerceAtLeast(1)) } },
        )
    }
}

@Composable
private fun BuilderKindSelector(
    kinds: List<BuilderKind>,
    selected: BuilderKind,
    onSelect: (BuilderKind) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        kinds.forEach { item ->
            SdoFilterChip(
                label = item.label,
                selected = selected == item,
                onClick = { onSelect(item) },
            )
        }
    }
}

@Composable
private fun PartCardSummary(
    part: ItemPart,
    weapon: Boolean,
    showHeritageCost: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(part.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (showHeritageCost) {
            SdoMetadataText(
                text = "${part.creationCost ?: "#"} PH",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
    val stats = buildList {
        if (part.load != 0) add("CARGA ${if (part.load > 0) "+${part.load}" else part.load}")
        if (part.durability != 0) add("DUR ${part.durability}")
        if (!weapon && (part.pg != 0 || part.pl != 0)) add("PG ${part.pg} / PL ${part.pl}")
        if (part.agilityLimit != null) add("LA ${part.agilityLimit}")
        if (part.damageBonus != 0) add("DANO +${part.damageBonus}")
        if (part.region.isNotBlank()) add(part.region)
    }
    if (stats.isNotEmpty()) {
        SdoMetadataText(stats.joinToString(" // "), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
    }
    if (part.traitIds.isNotEmpty()) {
        SdoMetadataText(
            text = "TRAÇOS // ${part.traitIds.joinToString { ItemCreationRules.traitName(it).uppercase() }}",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
    if (part.effect.isNotBlank()) {
        Text(part.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SelectableModalCard(
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { this.selected = selected }
            .sdoClickable(enabled = enabled, onClick = onClick),
        shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ModalBottomActions(
    secondaryLabel: String,
    onSecondary: () -> Unit,
    primaryLabel: String?,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SdoActionButton(
            label = secondaryLabel,
            onClick = onSecondary,
            modifier = Modifier.weight(1f),
            style = SdoActionStyle.SECONDARY,
        )
        if (primaryLabel != null) {
            SdoActionButton(
                label = primaryLabel,
                onClick = onPrimary,
                enabled = primaryEnabled,
                modifier = Modifier.weight(1f),
                style = SdoActionStyle.PRIMARY,
            )
        }
    }
}

@Composable
private fun EmptyBuilderCatalogState(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Catálogo de peças indisponível no momento. Feche e tente sincronizar novamente.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        SdoActionButton(
            label = "Fechar",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            style = SdoActionStyle.SECONDARY,
        )
    }
}

@PreviewLightDark
@Composable
private fun InventoryItemModalPreview() {
    SdoTheme {
        InventoryItemModal(
            mode = ItemModalMode.Sheet,
            tab = ItemModalTab.CATALOG,
            itemCatalog = emptyList(),
            ashCatalog = emptyList(),
            regionOptions = listOf("Mão Direita", "Tronco"),
            itemCreationDraft = null,
            onDraftChange = {},
            onDismiss = {},
            onAddCatalogItems = {},
            onAddItem = {},
            onAddAsh = { _, _ -> },
        )
    }
}
