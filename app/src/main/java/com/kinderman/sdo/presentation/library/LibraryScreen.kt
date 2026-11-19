package com.kinderman.sdo.presentation.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignContentKind
import com.kinderman.sdo.domain.model.CampaignLibraryEntry
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SessionOperation
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.presentation.character.ChoiceField
import com.kinderman.sdo.presentation.historian.CompendiumDetails
import com.kinderman.sdo.presentation.historian.racialReferences
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.SdoCardTitle
import com.kinderman.sdo.ui.SdoFilterChip
import com.kinderman.sdo.ui.SdoIconButton
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.SdoResponsiveGrid
import com.kinderman.sdo.ui.SdoScreenMasthead
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag
import java.text.DateFormat
import java.util.Date

private enum class LibraryTab(val label: String) {
    COMPENDIUM("Compêndio"),
    RULES("Regras do Jogo"),
}

private enum class LibraryScopeFilter(val label: String) {
    ALL("Tudo"),
    BASE("Base local"),
    CAMPAIGN("Campanha"),
    SESSION("Sessão"),
}

private enum class LibraryKindFilter(val label: String) {
    ALL("Todas"),
    PATH("Caminhos"),
    POWER("Poderes"),
    MYSTIC("Místico"),
    ITEM("Itens"),
    KNOWLEDGE("Conhecimentos"),
    CONDITION_NOTE("Condições / Notas"),
}

private fun LibraryKindFilter.matchesCatalog(kind: CatalogKind): Boolean = when (this) {
    LibraryKindFilter.ALL -> true
    LibraryKindFilter.PATH -> kind == CatalogKind.PATH
    LibraryKindFilter.POWER -> kind == CatalogKind.POWER
    LibraryKindFilter.MYSTIC -> kind in setOf(CatalogKind.MAGIC, CatalogKind.RUNE, CatalogKind.ASH)
    LibraryKindFilter.ITEM -> kind == CatalogKind.ITEM
    LibraryKindFilter.KNOWLEDGE -> kind in setOf(
        CatalogKind.ACQUIRED_KNOWLEDGE,
        CatalogKind.ARCANE_KNOWLEDGE,
        CatalogKind.BATTLE_TECHNIQUE,
    )
    LibraryKindFilter.CONDITION_NOTE -> false
}

private fun LibraryKindFilter.matchesCampaignEntry(kind: CampaignContentKind): Boolean = when (this) {
    LibraryKindFilter.ALL -> true
    LibraryKindFilter.PATH -> false
    LibraryKindFilter.POWER -> kind == CampaignContentKind.POWER
    LibraryKindFilter.MYSTIC -> false
    LibraryKindFilter.ITEM -> kind == CampaignContentKind.ITEM
    LibraryKindFilter.KNOWLEDGE -> false
    LibraryKindFilter.CONDITION_NOTE -> kind in setOf(
        CampaignContentKind.CONDITION,
        CampaignContentKind.NOTE,
        CampaignContentKind.REWARD,
        CampaignContentKind.TEMPLATE,
    )
}

private fun LibraryKindFilter.matchesSessionOp(type: SessionOperationType): Boolean = when (this) {
    LibraryKindFilter.ALL -> true
    LibraryKindFilter.CONDITION_NOTE -> type in setOf(
        SessionOperationType.CONDITION_ADD,
        SessionOperationType.NOTE,
        SessionOperationType.REWARD,
    )
    LibraryKindFilter.ITEM -> type == SessionOperationType.ITEM_CONSUME
    LibraryKindFilter.POWER, LibraryKindFilter.MYSTIC -> type in setOf(
        SessionOperationType.ABILITY_USE,
        SessionOperationType.MODIFIER_APPLY,
        SessionOperationType.EFFECT_OPERATION,
    )
    else -> false
}

private fun CatalogKind.displayLabel(): String = when (this) {
    CatalogKind.PATH -> "CAMINHO"
    CatalogKind.POWER -> "PODER"
    CatalogKind.MAGIC -> "MAGIA"
    CatalogKind.ASH -> "CINZA"
    CatalogKind.RUNE -> "RUNA"
    CatalogKind.ITEM -> "ITEM"
    CatalogKind.ACQUIRED_KNOWLEDGE -> "CONHECIMENTO"
    CatalogKind.ARCANE_KNOWLEDGE -> "ARCANO"
    CatalogKind.BATTLE_TECHNIQUE -> "TÉCNICA"
}

private fun CampaignContentKind.displayLabel(): String = when (this) {
    CampaignContentKind.ITEM -> "ITEM"
    CampaignContentKind.POWER -> "PODER"
    CampaignContentKind.NOTE -> "NOTA"
    CampaignContentKind.REWARD -> "RECOMPENSA"
    CampaignContentKind.CONDITION -> "CONDIÇÃO"
    CampaignContentKind.TEMPLATE -> "MODELO"
}

private fun SessionOperationType.displayLabel(): String = when (this) {
    SessionOperationType.REWARD -> "RECOMPENSA DE SESSÃO"
    SessionOperationType.NOTE -> "NOTA DE SESSÃO"
    SessionOperationType.CONDITION_ADD -> "CONDIÇÃO APLICADA"
    SessionOperationType.MODIFIER_APPLY -> "MODIFICADOR"
    SessionOperationType.EFFECT_OPERATION -> "EFEITO CANÔNICO"
    SessionOperationType.ITEM_CONSUME -> "ITEM NA SESSÃO"
    else -> name
}

private val sessionAdditionTypes = setOf(
    SessionOperationType.REWARD,
    SessionOperationType.NOTE,
    SessionOperationType.CONDITION_ADD,
    SessionOperationType.MODIFIER_APPLY,
    SessionOperationType.EFFECT_OPERATION,
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    campaigns: List<Campaign>,
    characters: List<Character>,
    catalog: List<CatalogEntry>,
    campaignLibrary: List<CampaignLibraryEntry>,
    audit: List<SessionOperation>,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var activeTab by rememberSaveable { mutableStateOf(LibraryTab.COMPENDIUM) }
    var search by rememberSaveable { mutableStateOf("") }
    var scopeFilter by rememberSaveable { mutableStateOf(LibraryScopeFilter.ALL) }
    var kindFilter by rememberSaveable { mutableStateOf(LibraryKindFilter.ALL) }
    var selectedCampaignId by rememberSaveable { mutableStateOf("") }
    var ruleSearch by rememberSaveable { mutableStateOf("") }
    var ruleCategory by rememberSaveable { mutableStateOf("Todas") }
    var showFiltersDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val handbookRules = remember(context) { GameRulesGuide.load(context) }

    val campaignById = remember(campaigns) { campaigns.associateBy { it.id } }
    val characterById = remember(characters) { characters.associateBy { it.id } }
    val campaignOptions = remember(campaigns) { listOf("") + campaigns.map(Campaign::id) }
    val activeCampaignId = selectedCampaignId.takeIf { it in campaignOptions }.orEmpty()
    val query = search.trim()

    val filteredCampaignEntries = remember(campaignLibrary, activeCampaignId, kindFilter, query, scopeFilter) {
        if (scopeFilter == LibraryScopeFilter.BASE || scopeFilter == LibraryScopeFilter.SESSION) {
            emptyMap()
        } else {
            campaignLibrary.filter { entry ->
                !entry.archived &&
                    (activeCampaignId.isBlank() || normalizeCampaignId(entry.campaignId) == activeCampaignId) &&
                    kindFilter.matchesCampaignEntry(entry.kind) &&
                    (query.isBlank() || listOf(entry.name, entry.summary, entry.payload, entry.kind.name)
                        .any { it.contains(query, ignoreCase = true) })
            }.groupBy { normalizeCampaignId(it.campaignId) }
        }
    }

    val filteredSessionAdditions = remember(audit, activeCampaignId, kindFilter, query, scopeFilter, characterById) {
        if (scopeFilter == LibraryScopeFilter.BASE || scopeFilter == LibraryScopeFilter.CAMPAIGN) {
            emptyList()
        } else {
            audit.asSequence()
                .filter { it.type in sessionAdditionTypes }
                .filter { activeCampaignId.isBlank() || normalizeCampaignId(it.campaignId) == activeCampaignId }
                .filter { kindFilter.matchesSessionOp(it.type) }
                .filter { op ->
                    if (query.isBlank()) true
                    else {
                        val charName = characterById[op.characterId]?.name.orEmpty()
                        listOf(op.target, op.newValue, op.reason, charName, op.type.displayLabel())
                            .any { it.contains(query, ignoreCase = true) }
                    }
                }
                .sortedByDescending { it.createdAt }
                .toList()
        }
    }

    val filteredBaseEntries = remember(catalog, kindFilter, query, scopeFilter) {
        if (scopeFilter == LibraryScopeFilter.CAMPAIGN || scopeFilter == LibraryScopeFilter.SESSION) {
            emptyList()
        } else {
            (catalog + racialReferences()).filter { entry ->
                kindFilter.matchesCatalog(entry.kind) &&
                    (query.isBlank() || listOf(entry.name, entry.group, entry.summary, entry.mechanicalEffect)
                        .plus(entry.keywords)
                        .any { it.contains(query, ignoreCase = true) })
            }
        }
    }

    val ruleQuery = ruleSearch.trim()
    val filteredRules = remember(handbookRules, ruleQuery, ruleCategory) {
        handbookRules.filter { topic ->
            (ruleCategory == "Todas" || topic.category == ruleCategory) &&
                (ruleQuery.isBlank() ||
                    listOf(topic.title, topic.chapter, topic.part, topic.category, topic.summary)
                        .plus(topic.formulas)
                        .plus(topic.scales)
                        .plus(topic.examples)
                        .plus(topic.blocks.flatMap { listOf(it.heading) + it.paragraphs + it.bullets })
                        .any { it.contains(ruleQuery, ignoreCase = true) })
        }
    }

    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "masthead") {
                    SdoScreenMasthead(
                        eyebrow = "LIB//COMPENDIUM",
                        title = "BIBLIOTECA",
                        metadata = "BASE LOCAL // CAMPANHAS // REGRAS DO JOGO",
                    )
                }

                item(key = "section-selector") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LibraryTab.entries.forEach { tab ->
                            SdoFilterChip(
                                label = tab.label,
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                if (activeTab == LibraryTab.COMPENDIUM) {
                item(key = "filters") {
                    val activeFilterCount = (if (scopeFilter != LibraryScopeFilter.ALL) 1 else 0) +
                        (if (kindFilter != LibraryKindFilter.ALL) 1 else 0) +
                        (if (activeCampaignId.isNotBlank()) 1 else 0)
                    val total = filteredCampaignEntries.values.sumOf { it.size } +
                        filteredSessionAdditions.size +
                        filteredBaseEntries.size
                    TechPanel(accent = MaterialTheme.colorScheme.primary) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            HudTextField(
                                label = "Buscar na biblioteca",
                                value = search,
                                placeholder = "Nome, efeito ou campanha",
                                modifier = Modifier.weight(1f),
                                onValue = { search = it },
                            )
                            com.kinderman.sdo.ui.SdoOutlinedButton(
                                onClick = { showFiltersDialog = true },
                                modifier = Modifier.testTag("library_filter_trigger"),
                            ) {
                                Text(if (activeFilterCount > 0) "Filtros ($activeFilterCount)" else "Filtros")
                            }
                        }
                        val activeSummary = buildList {
                            add(scopeFilter.label)
                            if (activeCampaignId.isNotBlank()) add(campaignById[activeCampaignId]?.name ?: activeCampaignId)
                            add(kindFilter.label)
                        }.joinToString(" // ")
                        SdoMetadataText(
                            "$total REGISTROS // $activeSummary",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (filteredSessionAdditions.isNotEmpty()) {
                    item(key = "session-header") {
                        SectionHeader("02", "Adições em sessão pelo Mestre")
                    }
                    items(filteredSessionAdditions, key = { "session:${it.id}" }) { op ->
                        val campaignName = campaignById[normalizeCampaignId(op.campaignId)]?.name
                        val charName = characterById[op.characterId]?.name
                        TechPanel(accent = MaterialTheme.colorScheme.tertiary) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                TelemetryTag(op.type.displayLabel(), MaterialTheme.colorScheme.tertiary)
                                if (!campaignName.isNullOrBlank()) {
                                    TelemetryTag(campaignName.uppercase(), MaterialTheme.colorScheme.secondary)
                                }
                            }
                            SdoCardTitle(
                                text = listOfNotNull(charName?.uppercase(), op.target.takeIf(String::isNotBlank))
                                    .joinToString(" // ")
                                    .ifBlank { op.type.displayLabel() },
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (op.newValue.isNotBlank()) {
                                Text(
                                    text = op.newValue,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (op.reason.isNotBlank()) {
                                Text(
                                    text = op.reason,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            SdoMetadataText(
                                text = formatLibraryTime(op.createdAt),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (filteredCampaignEntries.isNotEmpty()) {
                    item(key = "campaign-header") {
                        SectionHeader("03", "Modelos por campanha")
                    }
                    filteredCampaignEntries.forEach { (cid, entries) ->
                        val campaignName = campaignById[cid]?.name ?: "Campanha"
                        item(key = "campaign-group:$cid") {
                            SdoMetadataText(
                                "CAMPANHA // ${campaignName.uppercase()} (${entries.size})",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        items(entries, key = { "camp:${it.id}" }) { entry ->
                            TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    TelemetryTag(entry.kind.displayLabel(), MaterialTheme.colorScheme.secondary)
                                    TelemetryTag("V.${entry.version}")
                                }
                                SdoCardTitle(entry.name, color = MaterialTheme.colorScheme.primary)
                                if (entry.summary.isNotBlank()) {
                                    Text(
                                        text = entry.summary,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (entry.payload.isNotBlank()) {
                                    SdoInsetCard(contentPadding = 10.dp) {
                                        Text(
                                            text = entry.payload,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredBaseEntries.isNotEmpty()) {
                    item(key = "base-header") {
                        SectionHeader("04", "Conhecimento base local")
                    }
                    items(filteredBaseEntries, key = { "base:${it.id}" }) { entry ->
                        TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                TelemetryTag(entry.kind.displayLabel(), MaterialTheme.colorScheme.primary)
                                TelemetryTag("BASE LOCAL", MaterialTheme.colorScheme.secondary)
                                TelemetryTag("V.${entry.version}")
                            }
                            SdoCardTitle(entry.name, color = MaterialTheme.colorScheme.onSurface)
                            if (entry.group.isNotBlank()) {
                                Text(entry.group, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            }
                            if (entry.summary.isNotBlank()) {
                                Text(entry.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            CompendiumDetails(entry)
                        }
                    }
                }

                if (filteredCampaignEntries.isEmpty() && filteredSessionAdditions.isEmpty() && filteredBaseEntries.isEmpty()) {
                    item(key = "empty") {
                        SdoInsetCard(contentPadding = 16.dp) {
                            Text(
                                "Nenhum registro encontrado para os filtros selecionados.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                } else {
                    item(key = "rules-filters") {
                        TechPanel(accent = MaterialTheme.colorScheme.primary) {
                            SectionHeader("01", "Livro do Jogador • Regras")
                            HudTextField(
                                label = "Buscar regra",
                                value = ruleSearch,
                                placeholder = "Ex.: teste, trauma, esquiva, runas",
                                onValue = { ruleSearch = it },
                            )
                            SdoMetadataText("TEMA", color = MaterialTheme.colorScheme.secondary)
                            SdoResponsiveGrid(GameRulesGuide.categories, minItemWidth = 136.dp) { category, modifier ->
                                SdoFilterChip(
                                    label = category,
                                    selected = ruleCategory == category,
                                    onClick = { ruleCategory = category },
                                    modifier = modifier,
                                )
                            }
                            SdoMetadataText(
                                "${filteredRules.size} seções de regras locais (sem tabelas)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (filteredRules.isEmpty()) {
                        item(key = "rules-empty") {
                            SdoInsetCard(contentPadding = 16.dp) {
                                Text(
                                    "Nenhuma regra encontrada para essa busca.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(filteredRules, key = { "rule:${it.id}" }) { topic ->
                            GameRuleCard(topic = topic, forceExpanded = ruleQuery.isNotBlank())
                        }
                    }
                }
            }
        }
    }
    if (showFiltersDialog) {
        AlertDialog(
            onDismissRequest = { showFiltersDialog = false },
            title = { Text("FILTROS DA BIBLIOTECA") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SdoMetadataText("ORIGEM DOS DADOS", color = MaterialTheme.colorScheme.secondary)
                    SdoResponsiveGrid(LibraryScopeFilter.entries, minItemWidth = 112.dp) { option, modifier ->
                        SdoFilterChip(
                            label = option.label,
                            selected = scopeFilter == option,
                            onClick = { scopeFilter = option },
                            modifier = modifier,
                        )
                    }
                    if (campaigns.isNotEmpty() && scopeFilter != LibraryScopeFilter.BASE) {
                        ChoiceField(
                            label = "Campanha",
                            value = activeCampaignId,
                            options = campaignOptions,
                            enabled = true,
                            display = { id ->
                                if (id.isBlank()) "Todas as campanhas"
                                else campaignById[id]?.name ?: id
                            },
                        ) { selectedCampaignId = it }
                    }
                    SdoMetadataText("CATEGORIA", color = MaterialTheme.colorScheme.secondary)
                    SdoResponsiveGrid(LibraryKindFilter.entries, minItemWidth = 116.dp) { option, modifier ->
                        SdoFilterChip(
                            label = option.label,
                            selected = kindFilter == option,
                            onClick = { kindFilter = option },
                            modifier = modifier,
                        )
                    }
                }
            },
            confirmButton = {
                com.kinderman.sdo.ui.SdoTextButton(onClick = { showFiltersDialog = false }) { Text("APLICAR") }
            },
            dismissButton = {
                com.kinderman.sdo.ui.SdoTextButton(
                    onClick = {
                        scopeFilter = LibraryScopeFilter.ALL
                        kindFilter = LibraryKindFilter.ALL
                        selectedCampaignId = ""
                    },
                ) { Text("LIMPAR") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GameRuleCard(
    topic: GameRuleTopic,
    forceExpanded: Boolean,
) {
    var expanded by rememberSaveable(topic.id) { mutableStateOf(false) }
    val isExpanded = forceExpanded || expanded
    TechPanel(
        accent = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.clickable { expanded = !expanded },
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TelemetryTag(topic.category.uppercase(), MaterialTheme.colorScheme.primary)
            TelemetryTag(topic.chapter.uppercase(), MaterialTheme.colorScheme.secondary)
        }
        SdoCardTitle(topic.title, color = MaterialTheme.colorScheme.onSurface)
        val preview = topic.summary.ifBlank {
            topic.formulas.firstOrNull() ?: topic.scales.firstOrNull().orEmpty()
        }
        if (!isExpanded && preview.isNotBlank()) {
            Text(
                text = preview,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
            val detailCount = topic.blocks.sumOf { it.paragraphs.size + it.bullets.size } +
                topic.formulas.size + topic.scales.size + topic.examples.size
            SdoMetadataText("Toque para ler regra completa ($detailCount itens)", color = MaterialTheme.colorScheme.primary)
        } else {
            if (topic.formulas.isNotEmpty()) {
                SdoInsetCard(contentPadding = 10.dp, verticalSpacing = 4.dp) {
                    SdoMetadataText("FÓRMULA / REGRA-CHAVE", color = MaterialTheme.colorScheme.primary)
                    topic.formulas.forEach { formula ->
                        Text(
                            text = formula,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
            topic.blocks.forEach { block ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (block.heading.isNotBlank()) {
                        SdoMetadataText(block.heading.uppercase(), color = MaterialTheme.colorScheme.secondary)
                    }
                    block.paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    block.bullets.forEach { bullet ->
                        Text(
                            text = "• $bullet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (topic.scales.isNotEmpty()) {
                SdoInsetCard(contentPadding = 10.dp, verticalSpacing = 4.dp) {
                    SdoMetadataText("ESCALA / REFERÊNCIA", color = MaterialTheme.colorScheme.secondary)
                    topic.scales.forEach { row ->
                        Text(
                            text = row,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (topic.examples.isNotEmpty()) {
                SdoInsetCard(contentPadding = 10.dp, verticalSpacing = 4.dp) {
                    SdoMetadataText("EXEMPLO PRÁTICO", color = MaterialTheme.colorScheme.primary)
                    topic.examples.forEach { example ->
                        Text(
                            text = example,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private fun formatLibraryTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
