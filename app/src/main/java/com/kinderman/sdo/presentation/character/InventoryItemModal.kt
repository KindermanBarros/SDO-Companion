package com.kinderman.sdo.presentation.character

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

internal enum class ItemModalTab(val label: String) {
    CATALOG("Catálogo"),
    BUILDER("Construtor"),
    ENHANCEMENTS("Aprimoramentos"),
    ASH("Cinzas"),
}

private enum class CatalogSort(val label: String) {
    NAME("A–Z"),
    COST_ASC("Menor PH"),
    COST_DESC("Maior PH"),
    LOAD_ASC("Menor carga"),
}

@Composable
internal fun InventoryItemModal(
    mode: ItemModalMode,
    itemCatalog: List<CatalogEntry>,
    ashCatalog: List<CatalogEntry>,
    regionOptions: List<String>,
    initialTab: ItemModalTab = if (itemCatalog.isNotEmpty()) ItemModalTab.CATALOG else ItemModalTab.BUILDER,
    onDismiss: () -> Unit,
    onAddItem: (InventoryItem) -> Unit,
    onAddAsh: (entry: CatalogEntry, doses: Int) -> Unit,
) {
    val availableTabs = remember(itemCatalog.isNotEmpty(), ashCatalog.isNotEmpty()) {
        buildList {
            if (itemCatalog.isNotEmpty()) add(ItemModalTab.CATALOG)
            add(ItemModalTab.BUILDER)
            add(ItemModalTab.ENHANCEMENTS)
            if (ashCatalog.isNotEmpty()) add(ItemModalTab.ASH)
        }
    }
    var activeTab by remember(initialTab, availableTabs) {
        mutableStateOf(if (initialTab in availableTabs) initialTab else availableTabs.first())
    }
    var previewCost by remember(activeTab) { mutableStateOf<Int?>(null) }

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
                    availableTabs = availableTabs,
                    activeTab = activeTab,
                    onSelectTab = { activeTab = it },
                    onDismiss = onDismiss,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (activeTab) {
                        ItemModalTab.CATALOG -> CatalogTabContent(
                            mode = mode,
                            entries = itemCatalog,
                            onPreviewCostChange = { previewCost = it },
                            onDismiss = onDismiss,
                            onSelect = onAddItem,
                        )
                        ItemModalTab.BUILDER -> BuilderTabContent(
                            mode = mode,
                            regionOptions = regionOptions,
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
    availableTabs: List<ItemModalTab>,
    activeTab: ItemModalTab,
    onSelectTab: (ItemModalTab) -> Unit,
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
                text = if (mode.isCreation) "EQUIPAMENTOS INICIAIS" else "ADICIONAR ITEM",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            SdoTextButton(onClick = onDismiss) { Text("FECHAR") }
        }
        mode.remainingHeritageOrNull?.let { remaining ->
            val spent = (ItemCreationRules.HERITAGE_BUDGET - remaining).coerceAtLeast(0)
            val preview = previewCost ?: 0
            val afterPreview = remaining - preview
            val overBudget = previewCost != null && afterPreview < 0
            val budgetText = if (previewCost != null) {
                "HERANÇA // $spent + $preview PREVIEW = ${spent + preview}/${ItemCreationRules.HERITAGE_BUDGET} PH • $afterPreview RESTANTES"
            } else {
                "HERANÇA // $spent / ${ItemCreationRules.HERITAGE_BUDGET} PH GASTOS • $remaining RESTANTES"
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            availableTabs.forEach { tab ->
                SdoFilterChip(
                    label = tab.label,
                    selected = activeTab == tab,
                    onClick = { onSelectTab(tab) },
                )
            }
        }
    }
}

@Composable
private fun CatalogTabContent(
    mode: ItemModalMode,
    entries: List<CatalogEntry>,
    onPreviewCostChange: (Int?) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (InventoryItem) -> Unit,
) {
    val remainingHeritage = mode.remainingHeritageOrNull
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var onlyAffordable by rememberSaveable { mutableStateOf(remainingHeritage != null) }
    var sort by remember { mutableStateOf(if (remainingHeritage != null) CatalogSort.COST_ASC else CatalogSort.NAME) }
    var selectedEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    var toolName by rememberSaveable { mutableStateOf("") }

    val itemEntries = remember(entries, remainingHeritage) {
        entries.filter { entry ->
            entry.kind == CatalogKind.ITEM &&
                (remainingHeritage == null || entry.creationCost.toIntOrNull() != null)
        }
    }
    val categories = remember(itemEntries) {
        itemEntries.map(CatalogEntry::group).filter(String::isNotBlank).distinct().sorted()
    }
    val sortOptions = remember(remainingHeritage) {
        if (remainingHeritage != null) {
            listOf(CatalogSort.COST_ASC, CatalogSort.COST_DESC, CatalogSort.NAME)
        } else {
            listOf(CatalogSort.NAME, CatalogSort.LOAD_ASC)
        }
    }
    val filtered = remember(itemEntries, query, category, onlyAffordable, remainingHeritage, sort) {
        val needle = query.trim()
        val matched = itemEntries.filter { entry ->
            val numericCost = entry.creationCost.toIntOrNull()?.coerceAtLeast(1)
            (category.isBlank() || entry.group.equals(category, ignoreCase = true)) &&
                (!onlyAffordable || remainingHeritage == null || (numericCost != null && numericCost <= remainingHeritage)) &&
                (needle.isEmpty() ||
                    entry.name.contains(needle, true) ||
                    entry.group.contains(needle, true) ||
                    entry.summary.contains(needle, true))
        }
        when (sort) {
            CatalogSort.NAME -> matched.sortedBy { it.name.lowercase() }
            CatalogSort.COST_ASC -> matched.sortedWith(compareBy<CatalogEntry>({ it.creationCost.toIntOrNull()?.coerceAtLeast(1) ?: Int.MAX_VALUE }, { it.name.lowercase() }))
            CatalogSort.COST_DESC -> matched.sortedWith(compareByDescending<CatalogEntry> { it.creationCost.toIntOrNull()?.coerceAtLeast(1) ?: 0 }.thenBy { it.name.lowercase() })
            CatalogSort.LOAD_ASC -> matched.sortedWith(compareBy<CatalogEntry>({ it.load }, { it.name.lowercase() }))
        }
    }
    val selectedEntry = remember(itemEntries, selectedEntryId) {
        itemEntries.firstOrNull { it.id == selectedEntryId }
    }
    val selectedCost = selectedEntry?.creationCost?.toIntOrNull()?.coerceAtLeast(1)
    val selectedAllowed = selectedEntry != null && (remainingHeritage == null || (selectedCost != null && selectedCost <= remainingHeritage))
    val requiresToolName = selectedEntry?.group?.equals("Ferramenta", ignoreCase = true) == true
    val canConfirm = selectedAllowed && (!requiresToolName || toolName.isNotBlank())

    onPreviewCostChange(if (remainingHeritage != null) selectedCost else null)

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HudTextField("Buscar por nome, categoria ou efeito", query) { query = it }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SdoFilterChip(
                    label = "Todas",
                    selected = category.isBlank(),
                    onClick = { category = "" },
                )
                categories.forEach { cat ->
                    SdoFilterChip(
                        label = cat,
                        selected = category.equals(cat, ignoreCase = true),
                        onClick = { category = if (category.equals(cat, ignoreCase = true)) "" else cat },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (remainingHeritage != null) {
                    SdoFilterChip(
                        label = "Cabe nos PH",
                        selected = onlyAffordable,
                        onClick = { onlyAffordable = !onlyAffordable },
                    )
                }
                sortOptions.forEach { option ->
                    SdoFilterChip(
                        label = option.label,
                        selected = sort == option,
                        onClick = { sort = option },
                    )
                }
            }
            SdoMetadataText(
                text = "RESULTADOS // ${filtered.size} ITENS",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (filtered.isEmpty()) {
                item(key = "catalog-empty") {
                    SdoInsetCard(contentPadding = 14.dp, verticalSpacing = 8.dp) {
                        Text(
                            text = "Nenhum item encontrado para os filtros aplicados.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        SdoActionButton(
                            label = "Limpar filtros",
                            onClick = {
                                query = ""
                                category = ""
                                onlyAffordable = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            style = SdoActionStyle.SECONDARY,
                        )
                    }
                }
            } else {
                itemsIndexed(filtered, key = { index, entry -> "${entry.kind}:${entry.id}:$index" }) { _, entry ->
                    val numericCost = entry.creationCost.toIntOrNull()?.coerceAtLeast(1)
                    val allowed = remainingHeritage == null || (numericCost != null && numericCost <= remainingHeritage)
                    val isSelected = selectedEntryId == entry.id
                    SelectableModalCard(
                        selected = isSelected,
                        enabled = true,
                        onClick = {
                            if (isSelected) {
                                selectedEntryId = null
                            } else {
                                selectedEntryId = entry.id
                                toolName = ""
                            }
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = entry.name,
                                color = if (allowed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            if (remainingHeritage != null) {
                                SdoMetadataText(
                                    text = if (allowed) "${numericCost ?: "—"} PH" else "ACIMA (${numericCost ?: "#"} PH)",
                                    color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        SdoMetadataText(
                            text = buildList {
                                add(entry.group)
                                add("CARGA ${entry.load}")
                                if (entry.durability.isNotBlank()) add("DUR ${entry.durability}")
                                if (entry.region.isNotBlank()) add(entry.region)
                            }.filter(String::isNotBlank).joinToString(" // "),
                            color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (entry.summary.isNotBlank()) {
                            Text(
                                text = entry.summary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (isSelected && entry.mechanicalEffect.isNotBlank() && entry.mechanicalEffect != entry.summary) {
                            Text(
                                text = entry.mechanicalEffect,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (isSelected && entry.group.equals("Ferramenta", ignoreCase = true)) {
                            HudTextField("Nome ou ofício (obrigatório)", toolName) { toolName = it }
                            Text(
                                text = "Ex.: Relojoeiro — será salva como ${entry.name} — Relojoeiro.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        ModalBottomActions(
            secondaryLabel = "Cancelar",
            onSecondary = onDismiss,
            primaryLabel = when {
                selectedEntry == null -> "Selecione um item"
                !selectedAllowed -> "Saldo PH insuficiente"
                requiresToolName && toolName.isBlank() -> "Informe o ofício"
                remainingHeritage != null && selectedCost != null -> "Adicionar • $selectedCost PH"
                else -> "Adicionar item"
            },
            primaryEnabled = canConfirm,
            onPrimary = {
                val entry = selectedEntry ?: return@ModalBottomActions
                val baseItem = entry.toInventoryItem(initialCreation = remainingHeritage != null)
                val finalItem = if (requiresToolName) {
                    baseItem.copy(name = "${entry.name} — ${toolName.trim()}")
                } else {
                    baseItem
                }
                onSelect(finalItem)
            },
        )
    }
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
    var kind by remember { mutableStateOf(BuilderKind.WEAPON) }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf(ItemCreationDraft(category = BuilderKind.WEAPON.draftCategory)) }
    var searchQuery by remember(kind, stepIndex) { mutableStateOf("") }
    var narrativeName by rememberSaveable { mutableStateOf("") }
    var narrativeDescription by rememberSaveable { mutableStateOf("") }

    if (kind == BuilderKind.NARRATIVE) {
        onPreviewCostChange(null)
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BuilderKindSelector(availableKinds, kind) { selected ->
                    kind = selected
                    stepIndex = 0
                }
                Text(
                    text = "Crie um objeto narrativo sem valores mecânicos fixos. Você poderá editá-lo depois no inventário.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                HudTextField("Nome do item", narrativeName) { narrativeName = it }
                HudTextField("Descrição ou uso narrativo (opcional)", narrativeDescription, multiline = true) {
                    narrativeDescription = it
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
                            name = narrativeName.trim().ifBlank { "Item narrativo" },
                            category = "Item",
                            effect = narrativeDescription.trim(),
                            quantity = 1,
                        ),
                    )
                },
            )
        }
        return
    }

    if (kind == BuilderKind.FREE_ITEM) {
        onPreviewCostChange(null)
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
                BuilderKindSelector(availableKinds, kind) { selected ->
                    kind = selected
                    stepIndex = 0
                }
                HudTextField("Nome do item (obrigatório)", draft.commonName) { draft = draft.copy(commonName = it) }
                SdoMetadataText("CATEGORIA", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    freeCategories.forEach { cat ->
                        SdoFilterChip(
                            label = cat,
                            selected = draft.commonCategory.equals(cat, ignoreCase = true),
                            onClick = { draft = draft.copy(commonCategory = cat) },
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
                            onClick = { draft = draft.copy(quality = opt) },
                        )
                    }
                }
                TwoFields(
                    { IntegerField("Quantidade", draft.commonQuantity, true, it) { value -> draft = draft.copy(commonQuantity = value.coerceAtLeast(1)) } },
                    { IntegerField("Carga total", draft.commonLoad, true, it) { value -> draft = draft.copy(commonLoad = value.coerceAtLeast(0)) } },
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
                            onClick = { draft = draft.copy(commonRegion = if (region == "Nenhuma") "" else region) },
                        )
                    }
                }
                TwoFields(
                    { IntegerField("Durabilidade", draft.commonDurability.coerceAtLeast(1), true, it) { value -> draft = draft.copy(commonDurability = value.coerceAtLeast(1)) } },
                    { IntegerField("Alcance", draft.commonRange, true, it) { value -> draft = draft.copy(commonRange = value.coerceAtLeast(0)) } },
                )
                TwoFields(
                    { IntegerField("PG", draft.commonPg, true, it) { value -> draft = draft.copy(commonPg = value) } },
                    { IntegerField("PL", draft.commonPl, true, it) { value -> draft = draft.copy(commonPl = value) } },
                )
                TwoFields(
                    { IntegerField("Ataque", draft.commonAttack, true, it) { value -> draft = draft.copy(commonAttack = value) } },
                    { IntegerField("Dano", draft.commonDamage, true, it) { value -> draft = draft.copy(commonDamage = value) } },
                )
                HudTextField("Descrição ou efeito", draft.commonEffect, multiline = true) { draft = draft.copy(commonEffect = it) }
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
        onPreviewCostChange(null)
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
        onPreviewCostChange(null)
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
    onPreviewCostChange(if (initialCreation) builtCost else null)

    val steps = BuilderStep.entries
    val currentStep = steps[stepIndex.coerceIn(0, steps.lastIndex)]

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BuilderKindSelector(availableKinds, kind) { selected ->
                kind = selected
                stepIndex = 0
                draft = draft.copy(
                    category = selected.draftCategory,
                    baseId = "",
                    materialId = "",
                    secondaryMaterialId = "",
                    modificationIds = emptyList(),
                    enhancementIds = emptyList(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                steps.forEachIndexed { idx, step ->
                    SdoFilterChip(
                        label = "${idx + 1}. ${step.label}",
                        selected = currentStep == step,
                        onClick = { stepIndex = idx },
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
                        draft = draft.copy(customName = it)
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
                                draft = draft.copy(
                                    baseId = part.id,
                                    materialId = nextMaterialId,
                                    modificationIds = nextMods,
                                )
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
                                draft = draft.copy(
                                    materialId = part.id,
                                    secondaryMaterialId = draft.secondaryMaterialId.takeIf { part.id.startsWith("ligas_") && it != part.id }.orEmpty(),
                                )
                            },
                        ) {
                            PartCardSummary(part = part, weapon = weapon, showHeritageCost = initialCreation)
                        }
                    }
                    if (hasAlloy) {
                        SdoMetadataText("LIGA // SEGUNDO MATERIAL (OPCIONAL)", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        SelectableModalCard(
                            selected = secondaryMaterial == null,
                            onClick = { draft = draft.copy(secondaryMaterialId = "") },
                        ) {
                            Text("Material puro (sem mistura)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                        materialPool.filter { it.id != material.id }.forEach { part ->
                            SelectableModalCard(
                                selected = secondaryMaterial?.id == part.id,
                                onClick = { draft = draft.copy(secondaryMaterialId = part.id) },
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
                            onClick = { draft = draft.copy(quality = opt) },
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
                                        draft = draft.copy(modificationIds = next)
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
                                    draft = draft.copy(enhancementIds = nextIds)
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
            onSecondary = { if (stepIndex > 0) stepIndex -= 1 else onDismiss() },
            primaryLabel = when {
                !isLastStep -> "Continuar"
                !allowedByBudget -> "Saldo PH insuficiente"
                initialCreation && builtCost != null -> "Criar item • $builtCost PH"
                else -> "Criar item"
            },
            primaryEnabled = !isLastStep || allowedByBudget,
            onPrimary = {
                if (!isLastStep) {
                    stepIndex += 1
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
        onPreviewCostChange(null)
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
    onPreviewCostChange(null)

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
    onPreviewCostChange(if (remainingHeritage != null) previewCost else null)

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
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
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
            itemCatalog = emptyList(),
            ashCatalog = emptyList(),
            regionOptions = listOf("M�o Direita", "Tronco"),
            onDismiss = {},
            onAddItem = {},
            onAddAsh = { _, _ -> },
        )
    }
}
