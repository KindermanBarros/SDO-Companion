package com.kinderman.sdo.presentation.historian

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import com.kinderman.sdo.ui.sdoClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import com.kinderman.sdo.ui.SdoIconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.kinderman.sdo.ui.SdoTextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignAlertSettings
import com.kinderman.sdo.domain.model.CampaignContentKind
import com.kinderman.sdo.domain.model.CampaignDelivery
import com.kinderman.sdo.domain.model.CampaignLibraryEntry
import com.kinderman.sdo.domain.model.CampaignMember
import com.kinderman.sdo.domain.model.CampaignRole
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperation
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SessionResource
import com.kinderman.sdo.domain.model.AbilityCostType
import com.kinderman.sdo.domain.model.AbilityDuration
import com.kinderman.sdo.domain.model.AbilityExecution
import com.kinderman.sdo.domain.model.AbilityRange
import com.kinderman.sdo.domain.model.AbilityResistance
import com.kinderman.sdo.domain.model.AbilitySource
import com.kinderman.sdo.domain.model.AbilityTimeUnit
import com.kinderman.sdo.domain.model.BodyRegionSlot
import com.kinderman.sdo.domain.model.ConditionKind
import com.kinderman.sdo.domain.model.ConditionPayload
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.domain.model.canonicalBodyState
import com.kinderman.sdo.domain.model.canonicalConditions
import com.kinderman.sdo.presentation.character.ChoiceField
import com.kinderman.sdo.presentation.character.IntegerField
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.SdoActionButton
import com.kinderman.sdo.ui.SdoActionStyle
import com.kinderman.sdo.ui.SdoFilterChip
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoDataModules
import com.kinderman.sdo.ui.SdoResponsiveGrid
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag
import com.kinderman.sdo.ui.SdoScreenMasthead
import com.kinderman.sdo.ui.LocalSdoPreferences
import com.kinderman.sdo.ui.SdoVisualMode
import java.text.DateFormat
import java.util.Date

private enum class HistorianSection(val label: String) {
    OPERATION("OPERAÇÃO"),
    LIBRARY("BIBLIOTECA"),
    AUDIT("HISTÓRICO"),
}

private enum class LibrarySource(val label: String) { ALL("TUDO"), CAMPAIGN("MODELOS"), CATALOG("CATÁLOGO") }

private enum class LibraryCategory(val label: String) {
    ALL("TODAS"), ITEM("ITENS"), ABILITY("HABILIDADES"), CONDITION("CONDIÇÕES"), KNOWLEDGE("CONHECIMENTOS"), NOTE("NOTAS")
}

private fun LibraryCategory.matches(kind: CampaignContentKind): Boolean = when (this) {
    LibraryCategory.ALL -> true
    LibraryCategory.ITEM -> kind == CampaignContentKind.ITEM
    LibraryCategory.ABILITY -> kind == CampaignContentKind.POWER
    LibraryCategory.CONDITION -> kind == CampaignContentKind.CONDITION
    LibraryCategory.KNOWLEDGE -> false
    LibraryCategory.NOTE -> kind in setOf(CampaignContentKind.NOTE, CampaignContentKind.REWARD, CampaignContentKind.TEMPLATE)
}

private fun LibraryCategory.matches(kind: CatalogKind): Boolean = when (this) {
    LibraryCategory.ALL -> true
    LibraryCategory.ITEM -> kind == CatalogKind.ITEM
    LibraryCategory.ABILITY -> kind in setOf(CatalogKind.POWER, CatalogKind.MAGIC, CatalogKind.RUNE, CatalogKind.ASH, CatalogKind.BATTLE_TECHNIQUE)
    LibraryCategory.CONDITION, LibraryCategory.NOTE -> false
    LibraryCategory.KNOWLEDGE -> kind in setOf(CatalogKind.ACQUIRED_KNOWLEDGE, CatalogKind.ARCANE_KNOWLEDGE)
}

private fun CampaignLibraryEntry.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    val structured = listOfNotNull(
        itemSnapshot?.let { listOf(it.name, it.category, it.effect).joinToString(" ") },
        powerSnapshot?.let { listOf(it.name, it.category, it.effect, it.canonicalSource.label).joinToString(" ") },
        conditionSnapshot?.let { listOf(it.name, it.summary, it.intensity, it.duration, it.origin).joinToString(" ") },
    )
    return (listOf(name, summary, payload, kind.name, catalogEntryId) + structured)
        .any { it.contains(query.trim(), ignoreCase = true) }
}

private fun CampaignContentKind.displayLabel(): String = when (this) {
    CampaignContentKind.ITEM -> "ITEM"
    CampaignContentKind.POWER -> "HABILIDADE"
    CampaignContentKind.NOTE -> "NOTA"
    CampaignContentKind.REWARD -> "RECOMPENSA"
    CampaignContentKind.CONDITION -> "CONDIÇÃO"
    CampaignContentKind.TEMPLATE -> "MODELO"
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HistorianDashboardScreen(
    session: UserSession,
    campaigns: List<Campaign>,
    memberships: List<CampaignMember>,
    characters: List<Character>,
    catalog: List<CatalogEntry>,
    audit: List<SessionOperation>,
    library: List<CampaignLibraryEntry>,
    deliveries: List<CampaignDelivery>,
    alertSettings: List<CampaignAlertSettings>,
    onApplyCommand: (Character, SessionCommand) -> Unit,
    onSaveLibrary: (CampaignLibraryEntry) -> Unit,
    onDuplicateLibrary: (CampaignLibraryEntry) -> Unit,
    onArchiveLibrary: (CampaignLibraryEntry, Boolean) -> Unit,
    onDeliverLibrary: (CampaignLibraryEntry, List<Character>, Map<String, String>) -> Unit,
    onSaveAlertSettings: (CampaignAlertSettings) -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenSheet: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var section by rememberSaveable { mutableStateOf(HistorianSection.OPERATION) }
    var search by rememberSaveable { mutableStateOf("") }
    var librarySource by rememberSaveable { mutableStateOf(LibrarySource.ALL) }
    var libraryCategory by rememberSaveable { mutableStateOf(LibraryCategory.ALL) }
    var showArchivedLibrary by rememberSaveable { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<Character?>(null) }
    var editingLibrary by remember { mutableStateOf<CampaignLibraryEntry?>(null) }
    var delivering by remember { mutableStateOf<CampaignLibraryEntry?>(null) }
    var choosingCampaign by remember { mutableStateOf(false) }
    var selectedCampaignId by rememberSaveable { mutableStateOf("") }
    var auditSearch by rememberSaveable { mutableStateOf("") }
    var auditType by rememberSaveable { mutableStateOf<SessionOperationType?>(null) }
    val historianCampaignIds = remember(session, campaigns, memberships) {
        val memberCampaignIds = memberships.filter {
            it.userId == session.uid && it.isActive && it.role == CampaignRole.HISTORIAN
        }.map(CampaignMember::campaignId)
        campaigns.filter { campaign ->
            session.isAdmin || campaign.ownerId == session.uid || campaign.id in memberCampaignIds
        }.mapTo(linkedSetOf(), Campaign::id)
    }
    val visibleCampaigns = campaigns.filter { it.id in historianCampaignIds }
    LaunchedEffect(visibleCampaigns.map(Campaign::id)) {
        if (selectedCampaignId !in historianCampaignIds) selectedCampaignId = visibleCampaigns.firstOrNull()?.id.orEmpty()
    }
    val selectedCampaign = visibleCampaigns.firstOrNull { it.id == selectedCampaignId }
    val selectedCharacters = characters.filter { selectedCampaignId.isNotBlank() && normalizeCampaignId(it.campaignId) == selectedCampaignId }
    val availableAuditTypes = remember(audit, selectedCampaignId) {
        audit.asSequence()
            .filter { it.campaignId == selectedCampaignId }
            .map(SessionOperation::type)
            .distinct()
            .sortedBy(SessionOperationType::name)
            .toList()
    }
    LaunchedEffect(availableAuditTypes) {
        if (auditType != null && auditType !in availableAuditTypes) auditType = null
    }
    val selectedAudit = audit.filter { operation ->
        val characterName = characters.firstOrNull { it.id == operation.characterId }?.name.orEmpty()
        operation.campaignId == selectedCampaignId &&
            (auditType == null || operation.type == auditType) &&
            (auditSearch.isBlank() || listOf(operation.target, operation.reason, operation.type.name, characterName)
                .any { it.contains(auditSearch.trim(), ignoreCase = true) })
    }.sortedByDescending(SessionOperation::createdAt)
    val selectedLibrary = library.filter { it.campaignId == selectedCampaignId }
    val libraryWritable = selectedCampaign != null && !selectedCampaign.isArchived
    val cybergrunge = LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE

    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { if (!cybergrunge) Text("PAINEL DO HISTORIADOR") },
                    navigationIcon = { SdoIconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SdoScreenMasthead(
                        eyebrow = "HISTORIAN//CONTROL",
                        title = "PAINEL DO HISTORIADOR",
                        metadata = "${visibleCampaigns.size} CAMPANHAS // ${selectedCharacters.size} FICHAS",
                    )
                }
                item {
                    SdoResponsiveGrid(HistorianSection.entries.toList(), minItemWidth = 104.dp, maxColumns = 3) { target, modifier ->
                        SdoFilterChip(target.label, section == target, { section = target }, modifier)
                    }
                }
                item {
                    androidx.compose.foundation.layout.Box {
                        SdoTextButton(
                            onClick = { choosingCampaign = true },
                            enabled = visibleCampaigns.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("${selectedCampaign?.name ?: "Nenhuma campanha"} ▾", maxLines = 1) }
                        androidx.compose.material3.DropdownMenu(choosingCampaign, { choosingCampaign = false }) {
                            visibleCampaigns.forEach { campaign ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(campaign.name) },
                                    onClick = { selectedCampaignId = campaign.id; choosingCampaign = false },
                                )
                            }
                        }
                    }
                }
                when (section) {
                    HistorianSection.OPERATION -> {
                        if (visibleCampaigns.isEmpty()) item {
                            TechPanel(accent = MaterialTheme.colorScheme.error) {
                                Text("Nenhuma campanha com acesso de Historiador.", color = MaterialTheme.colorScheme.onSurface)
                                Text("Entre como Historiador ou responsável por uma campanha para acessar esta visão.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        selectedCampaign?.let { campaign ->
                            val campaignCharacters = selectedCharacters
                            item("campaign:${campaign.id}") {
                                TechPanel(accent = if (campaign.isArchived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        TelemetryTag(if (campaign.isArchived) "ARCHIVED" else "LIVE")
                                        TelemetryTag("FILES.${campaignCharacters.size}")
                                    }
                                    SectionHeader("01", campaign.name)
                                    if (campaignCharacters.isEmpty()) Text("Nenhuma ficha vinculada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    AlertSettingsRow(
                                        alertSettings.firstOrNull { it.campaignId == campaign.id } ?: CampaignAlertSettings(campaign.id),
                                        onSaveAlertSettings,
                                    )
                                }
                            }
                            items(campaignCharacters, key = { "operative:${campaign.id}:${it.id}" }) { character ->
                                OperationalCharacterCard(
                                    character,
                                    alertSettings.firstOrNull { it.campaignId == campaign.id } ?: CampaignAlertSettings(campaign.id),
                                    onOpenSession,
                                    onOpenSheet,
                                    onQuickAction = { if (!campaign.isArchived) actionTarget = character },
                                )
                            }
                        }
                    }
                    HistorianSection.LIBRARY -> {
                        item {
                            TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                                TelemetryTag("LOCAL.CATALOG")
                                SectionHeader("02", "Biblioteca de referência")
                                Text("Filtre modelos da campanha e referências canônicas por origem e categoria.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HudTextField(
                                    label = "Buscar nome, categoria, efeito ou regra",
                                    value = search,
                                    modifier = Modifier.fillMaxWidth(),
                                    onValue = { search = it },
                                )
                                Text("ORIGEM", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                SdoResponsiveGrid(LibrarySource.entries, minItemWidth = 104.dp) { source, modifier ->
                                    SdoFilterChip(source.label, librarySource == source, { librarySource = source }, modifier)
                                }
                                Text("CATEGORIA", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                SdoResponsiveGrid(LibraryCategory.entries, minItemWidth = 128.dp) { category, modifier ->
                                    SdoFilterChip(category.label, libraryCategory == category, { libraryCategory = category }, modifier)
                                }
                                SdoFilterChip("INCLUIR ARQUIVADOS", showArchivedLibrary, { showArchivedLibrary = !showArchivedLibrary })
                                SdoActionButton("NOVO MODELO", {
                                    selectedCampaign?.takeUnless { it.isArchived }?.let { editingLibrary = CampaignLibraryEntry(campaignId = it.id, createdBy = session.uid) }
                                }, enabled = libraryWritable, style = SdoActionStyle.PRIMARY, modifier = Modifier.fillMaxWidth())
                                if (!libraryWritable && selectedCampaign != null) {
                                    Text("Campanha arquivada: biblioteca disponível somente para leitura.", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        val filteredLibrary = selectedLibrary.filter { entry ->
                            (showArchivedLibrary || !entry.archived) &&
                                libraryCategory.matches(entry.kind) &&
                                entry.matchesSearch(search)
                        }
                        val filtered = (catalog + racialReferences() + characterReferences(selectedCharacters)).filter { entry ->
                            libraryCategory.matches(entry.kind) && (search.isBlank() || entry.searchableText().contains(search.trim(), true))
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${if (librarySource == LibrarySource.CATALOG) 0 else filteredLibrary.size} MODELOS // ${if (librarySource == LibrarySource.CAMPAIGN) 0 else filtered.size} REFERÊNCIAS", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                Text("${deliveries.count { it.campaignId == selectedCampaignId && it.state.name == "PENDING" }} ENTREGAS PENDENTES", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (librarySource != LibrarySource.CATALOG) {
                            item { SectionHeader("02.A", "Modelos da campanha") }
                            items(filteredLibrary, key = { "library:${it.id}" }) { entry ->
                                LibraryCard(entry, enabled = libraryWritable, onEdit = { editingLibrary = entry }, onDuplicate = { onDuplicateLibrary(entry) }, onArchive = { onArchiveLibrary(entry, !entry.archived) }, onDeliver = { delivering = entry })
                            }
                        }
                        if (librarySource != LibrarySource.CAMPAIGN) {
                            item { SectionHeader("02.B", "Catálogo local de referência") }
                            items(filtered, key = { "catalog:${it.id}" }) { entry -> CatalogReferenceCard(entry) }
                        }
                        val visibleResultCount =
                            (if (librarySource == LibrarySource.CATALOG) 0 else filteredLibrary.size) +
                                (if (librarySource == LibrarySource.CAMPAIGN) 0 else filtered.size)
                        if (visibleResultCount == 0) item("library-empty") {
                            SdoInsetCard { Text("Nenhum registro corresponde aos filtros.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                    HistorianSection.AUDIT -> {
                        item {
                            TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                                TelemetryTag("APPEND_ONLY.${selectedAudit.size}")
                                Text("HISTÓRICO DA CAMPANHA", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                                Text("Ações mais recentes primeiro. Busque por personagem, alvo, motivo ou tipo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HudTextField("Buscar no histórico", auditSearch, onValue = { auditSearch = it })
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SdoTextButton(onClick = {
                                        val options = listOf<SessionOperationType?>(null) + availableAuditTypes
                                        auditType = options[(options.indexOf(auditType) + 1) % options.size]
                                    }, enabled = availableAuditTypes.isNotEmpty(), modifier = Modifier.weight(1f)) {
                                        com.kinderman.sdo.ui.AdaptiveActionLabel("TIPO // ${auditType?.displayLabel() ?: "TODOS"}")
                                    }
                                    SdoTextButton(onClick = { auditSearch = ""; auditType = null }, modifier = Modifier.weight(1f)) { Text("LIMPAR") }
                                }
                            }
                        }
                        if (selectedAudit.isEmpty()) item("audit-empty") {
                            Text(
                                if (auditSearch.isBlank() && auditType == null) "Nenhuma ação registrada nesta campanha." else "Nenhum registro corresponde aos filtros.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(selectedAudit, key = { "audit:${it.id}" }) { operation ->
                            TechPanel(accent = if (operation.dirty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TelemetryTag(operation.type.name)
                                    TelemetryTag(if (operation.dirty) "LOCAL_DELTA" else "SYNC_OK")
                                }
                                val characterName = characters.firstOrNull { it.id == operation.characterId }?.name ?: operation.characterId.take(10)
                                Text("$characterName // ${operation.target.ifBlank { operation.type.name }}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
                                Text("${operation.previousValue.ifBlank { "—" }} → ${operation.newValue.ifBlank { "—" }}", color = MaterialTheme.colorScheme.onSurface)
                                Text("${formatAuditTime(operation.createdAt)} // ATOR ${operation.actorId.take(10)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                if (operation.reason.isNotBlank()) Text(operation.reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
    actionTarget?.let { character -> QuickActionsDialog(character, { actionTarget = null }) { command -> onApplyCommand(character, command); actionTarget = null } }
    editingLibrary?.takeIf { libraryWritable }?.let { entry -> LibraryEditorDialog(entry, visibleCampaigns.filterNot { it.isArchived }, { editingLibrary = null }) { onSaveLibrary(it); editingLibrary = null } }
    delivering?.takeIf { libraryWritable }?.let { entry -> DeliveryDialog(entry, characters.filter { normalizeCampaignId(it.campaignId) == entry.campaignId }, { delivering = null }) { selected, mappings -> onDeliverLibrary(entry, selected, mappings); delivering = null } }
}

private fun formatAuditTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))

@Composable
private fun OperationalCharacterCard(
    character: Character,
    settings: CampaignAlertSettings,
    onOpenSession: (String) -> Unit,
    onOpenSheet: (String) -> Unit,
    onQuickAction: () -> Unit,
) {
    var expanded by rememberSaveable(character.id) { mutableStateOf(false) }
    val typedConditions = character.canonicalConditions()
    val body = runCatching { character.canonicalBodyState() }.getOrNull()
    val failedRegions = body?.regions.orEmpty().filter { it.failures > 0 }
    val failedOrgans = body?.organs.orEmpty().filter { it.failures > 0 }
    val alerts = buildList {
        if (character.lifeMaximum > 0 && character.life.current * 100 <= character.lifeMaximum * settings.lifeThresholdPercent) add("VIDA CRÍTICA")
        if (character.sanityMaximum > 0 && character.sanity.current * 100 <= character.sanityMaximum * settings.sanityThresholdPercent) add("SANIDADE CRÍTICA")
        if (character.exhaustion.maximum > 0 && character.exhaustion.current * 100 >= character.exhaustion.maximum * settings.exhaustionThresholdPercent) add("EXAUSTÃO ALTA")
        if (settings.alertConditions && typedConditions.isNotEmpty()) add("${typedConditions.size} CONDIÇÃO(ÕES)")
        if (settings.alertBodyFailures && (failedRegions.isNotEmpty() || failedOrgans.isNotEmpty())) add("FALHA CORPORAL")
        if (character.lastSyncedAt > 0 && System.currentTimeMillis() - character.lastSyncedAt > settings.staleAfterHours * 3_600_000L) add("SYNC ANTIGO")
        if (character.dirty) add("ALTERAÇÃO LOCAL")
    }
    val initials = character.name.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        .take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifBlank { "?" }
    TechPanel(accent = if (alerts.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) {
        Row(
            Modifier.fillMaxWidth().sdoClickable { expanded = !expanded },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).border(1.dp, MaterialTheme.colorScheme.primary, CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(initials, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium) }
            Column(Modifier.weight(1f)) {
                Text(character.name.uppercase(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${character.race.ifBlank { "SEM RAÇA" }} // ${character.pathName.ifBlank { "SEM CAMINHO" }}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                TelemetryTag("NV.${character.level}")
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    if (expanded) "Recolher personagem" else "Expandir personagem",
                )
            }
        }
        SdoDataModules(
            listOf(
                "Vida" to "${character.life.current}/${character.lifeMaximum}",
                "Sanidade" to "${character.sanity.current}/${character.sanityMaximum}",
                "Exaustão" to "${character.exhaustion.current}/${character.exhaustion.maximum}",
                "Sincronização" to if (character.dirty) "ALTERAÇÃO LOCAL" else "EM DIA",
            )
        )
        if (alerts.isNotEmpty()) {
            SdoInsetCard(accent = MaterialTheme.colorScheme.error) {
                Text("ALERTAS", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                Text(alerts.joinToString("\n"), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (expanded) {
            SdoDataModules(
                listOf(
                    "Arcano" to "${character.arcane.current}/${character.arcaneMaximum}",
                    "Energia" to "${character.energy.current}/${character.energyMaximum}",
                    "Destino" to "${character.destiny.current}/${character.destinyMaximum}",
                    "Corrupção" to "${character.corruption.current}/${character.corruption.maximum}%",
                    "Proteção geral" to character.protectionTotal("Geral").toString(),
                    "Esquiva" to character.protectionTotal("Esquiva").toString(),
                    "Postura" to character.protectionTotal("Postura").toString(),
                    "Mental / Arcana" to "${character.protectionTotal("Mental")} / ${character.protectionTotal("Arcana")}",
                    "Condições" to typedConditions.joinToString("\n") { condition ->
                        condition.name.uppercase() + condition.intensity?.let { " $it" }.orEmpty()
                    }.ifBlank { "NENHUMA" },
                    "Corpo" to buildList {
                        addAll(failedRegions.map { "${it.region.label.uppercase()} ${it.failures}/4" })
                        addAll(failedOrgans.map { "${it.organ.label.uppercase()} ${it.failures}/3" })
                    }.joinToString("\n").ifBlank { "SEM FALHAS" },
                )
            )
            SdoResponsiveGrid(listOf("SESSION", "ACTION", "SHEET"), minItemWidth = 136.dp) { action, modifier ->
                when (action) {
                    "SESSION" -> SdoActionButton("ABRIR SESSÃO", { onOpenSession(character.id) }, modifier, style = SdoActionStyle.PRIMARY)
                    "ACTION" -> SdoActionButton("AÇÃO RÁPIDA", onQuickAction, modifier)
                    else -> SdoActionButton("ABRIR FICHA", { onOpenSheet(character.id) }, modifier)
                }
            }
        }
    }
}

private fun SessionOperationType.displayLabel(): String = when (this) {
    SessionOperationType.DAMAGE -> "DANO"
    SessionOperationType.HEAL -> "CURA"
    SessionOperationType.RESOURCE -> "RECURSO"
    SessionOperationType.CONDITION_ADD -> "CONDIÇÃO +"
    SessionOperationType.CONDITION_REMOVE -> "CONDIÇÃO −"
    SessionOperationType.MONEY -> "DINHEIRO"
    SessionOperationType.DESTINY -> "DESTINO"
    SessionOperationType.REWARD -> "RECOMPENSA"
    SessionOperationType.NOTE -> "ANOTAÇÃO"
    SessionOperationType.REGION_FAILURE -> "FALHA CORPORAL"
    SessionOperationType.ABILITY_USE -> "HABILIDADE"
    SessionOperationType.USAGE_RESET -> "REINÍCIO"
    SessionOperationType.EFFECT_OPERATION -> "OPERAÇÃO CANÔNICA"
    SessionOperationType.MODIFIER_APPLY -> "MODIFICADOR"
    SessionOperationType.ITEM_CONSUME -> "CONSUMO"
}

@Composable
private fun CatalogReferenceCard(entry: CatalogEntry) {
    TechPanel(accent = MaterialTheme.colorScheme.secondary) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TelemetryTag(entry.kind.name)
            TelemetryTag("V.${entry.version}")
        }
        Text(entry.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
        Text(entry.group, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        Text(entry.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        CompendiumDetails(entry)
    }
}

@Composable
private fun AlertSettingsRow(settings: CampaignAlertSettings, onSave: (CampaignAlertSettings) -> Unit) {
    SdoInsetCard(verticalSpacing = 8.dp) {
        Text("ALERTAS // VIDA ≤${settings.lifeThresholdPercent}% // SAN ≤${settings.sanityThresholdPercent}% // EXA ≥${settings.exhaustionThresholdPercent}%", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        SdoResponsiveGrid(listOf("LIFE_DOWN", "LIFE_UP", "SYNC"), minItemWidth = 112.dp) { action, modifier ->
            when (action) {
                "LIFE_DOWN" -> SdoActionButton("VIDA −", { onSave(settings.copy(lifeThresholdPercent = (settings.lifeThresholdPercent - 5).coerceAtLeast(5))) }, modifier)
                "LIFE_UP" -> SdoActionButton("VIDA +", { onSave(settings.copy(lifeThresholdPercent = (settings.lifeThresholdPercent + 5).coerceAtMost(95))) }, modifier)
                else -> SdoActionButton("SYNC ${settings.staleAfterHours}H", { onSave(settings.copy(staleAfterHours = if (settings.staleAfterHours == 24) 48 else 24)) }, modifier)
            }
        }
    }
}

@Composable
private fun QuickActionsDialog(character: Character, onDismiss: () -> Unit, onApply: (SessionCommand) -> Unit) {
    var amount by remember { mutableIntStateOf(1) }
    var label by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<QuickActionKind?>(null) }
    var selectedCondition by remember { mutableStateOf(ConditionKind.Abalado) }
    var resource by remember { mutableStateOf(SessionResource.ENERGY) }
    var regionIndex by remember { mutableIntStateOf(0) }
    val regions = character.canonicalBodyState().regions
    val region = regions.getOrNull(regionIndex)
    val command = when (selected) {
        QuickActionKind.DAMAGE -> SessionCommand(type = SessionOperationType.DAMAGE, amount = amount, bodyRegion = region?.region, reason = label)
        QuickActionKind.HEAL -> SessionCommand(type = SessionOperationType.HEAL, amount = amount, resource = SessionResource.LIFE, reason = label)
        QuickActionKind.RESOURCE -> SessionCommand(type = SessionOperationType.RESOURCE, amount = amount, resource = resource, reason = label)
        QuickActionKind.CONDITION -> SessionCommand(type = SessionOperationType.CONDITION_ADD, condition = ConditionPayload(kind = selectedCondition, intensity = amount), reason = label)
        QuickActionKind.MONEY -> SessionCommand(type = SessionOperationType.MONEY, amount = amount, reason = label)
        QuickActionKind.DESTINY -> SessionCommand(type = SessionOperationType.DESTINY, amount = amount, resource = SessionResource.DESTINY, reason = label)
        QuickActionKind.NOTE -> SessionCommand(type = SessionOperationType.NOTE, label = label.ifBlank { "Anotação do Historiador" }, detail = "Registro operacional", reason = label)
        QuickActionKind.REGION -> SessionCommand(type = SessionOperationType.REGION_FAILURE, amount = amount, bodyRegion = region?.region, reason = label)
        null -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AÇÃO RÁPIDA // ${character.name.uppercase()}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. ESCOLHA A AÇÃO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                SdoResponsiveGrid(QuickActionKind.entries, minItemWidth = 126.dp) { action, modifier ->
                    SdoFilterChip(action.label, selected == action, { selected = action }, modifier)
                }
                Text("2. DEFINA OS DADOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                SdoInsetCard(verticalSpacing = 8.dp) {
                    Text("VALOR // $amount", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    SdoResponsiveGrid(listOf("DECREASE", "INCREASE"), minItemWidth = 140.dp) { action, modifier ->
                        when (action) {
                            "DECREASE" -> SdoActionButton("DIMINUIR", { amount = (amount - 1).coerceAtLeast(1) }, modifier)
                            else -> SdoActionButton("AUMENTAR", { amount += 1 }, modifier)
                        }
                    }
                }
                if (selected == QuickActionKind.RESOURCE) {
                    SdoActionButton("RECURSO // ${resource.name}", {
                        val options = listOf(SessionResource.ENERGY, SessionResource.ARCANE, SessionResource.SANITY, SessionResource.LIFE)
                        resource = options[(options.indexOf(resource) + 1) % options.size]
                    }, modifier = Modifier.fillMaxWidth())
                }
                if (selected == QuickActionKind.DAMAGE || selected == QuickActionKind.REGION) {
                    SdoActionButton("REGIÃO // ${region?.name?.uppercase() ?: "NENHUMA"}", {
                        if (regions.isNotEmpty()) regionIndex = (regionIndex + 1) % regions.size
                    }, modifier = Modifier.fillMaxWidth(), enabled = regions.isNotEmpty())
                }
                if (selected == QuickActionKind.CONDITION) {
                    ChoiceField("Condição", selectedCondition, ConditionKind.entries, true, display = { it.label }) {
                        selectedCondition = it
                    }
                }
                HudTextField("Motivo / anotação", label, onValue = { label = it })
                Text("3. CONFIRME", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                Text(selected?.let { "${it.label} // valor $amount // ${character.name}" } ?: "Selecione uma ação acima.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { SdoActionButton("APLICAR", { command?.let(onApply) }, enabled = command != null, style = SdoActionStyle.PRIMARY) },
        dismissButton = { SdoActionButton("CANCELAR", onDismiss) },
    )
}

private enum class QuickActionKind(val label: String) {
    DAMAGE("DANO"), HEAL("CURA"), RESOURCE("RECURSO"),
    CONDITION("CONDIÇÃO"), MONEY("DINHEIRO"), DESTINY("DESTINO"),
    NOTE("ANOTAÇÃO"), REGION("FALHA LOCAL"),
}

@Composable
private fun LibraryCard(
    entry: CampaignLibraryEntry,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
    onDeliver: () -> Unit,
) {
    TechPanel(accent = if (entry.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TelemetryTag(entry.kind.displayLabel())
                if (entry.archived) TelemetryTag("ARQUIVADO", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TelemetryTag(if (entry.dirty) "LOCAL_DELTA" else "V.${entry.version}")
        }
        Text(entry.name.ifBlank { "Modelo sem nome" }, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
        if (entry.summary.isNotBlank()) Text(entry.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LibraryStructuredDetails(entry)
        if (entry.payload.isNotBlank()) SdoInsetCard {
            Text("NOTAS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            Text(entry.payload, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        if (entry.knowledgeBonus != 0) TelemetryTag("CONHECIMENTO ${if (entry.knowledgeBonus > 0) "+" else ""}${entry.knowledgeBonus}", MaterialTheme.colorScheme.secondary)
        SdoResponsiveGrid(listOf("EDIT", "DUPLICATE", "ARCHIVE", "DELIVER"), minItemWidth = 128.dp) { action, modifier ->
            when (action) {
                "EDIT" -> SdoActionButton("EDITAR", onEdit, modifier, enabled)
                "DUPLICATE" -> SdoActionButton("DUPLICAR", onDuplicate, modifier, enabled)
                "ARCHIVE" -> SdoActionButton(if (entry.archived) "RESTAURAR" else "ARQUIVAR", onArchive, modifier, enabled, SdoActionStyle.DESTRUCTIVE)
                else -> SdoActionButton("ENTREGAR", onDeliver, modifier, enabled && !entry.archived, SdoActionStyle.PRIMARY)
            }
        }
    }
}

@Composable
private fun LibraryStructuredDetails(entry: CampaignLibraryEntry) {
    val fields = when (entry.kind) {
        CampaignContentKind.ITEM -> entry.itemSnapshot?.let { item -> buildList {
            add("Categoria" to item.category.ifBlank { "Item" })
            add("Carga" to item.load.toString())
            if (item.durabilityMax > 0) add("Durabilidade" to "${item.durabilityCurrent}/${item.durabilityMax}")
            if (item.pg != 0 || item.pl != 0) add("Proteção" to "PG ${item.pg} // PL ${item.pl}")
            if (item.effect.isNotBlank()) add("Efeito" to item.effect)
        } }.orEmpty()
        CampaignContentKind.POWER -> entry.powerSnapshot?.let { power -> buildList {
            add("Categoria" to power.category.ifBlank { power.canonicalSource.label })
            add("Custo" to "${power.costType.label} ${power.costValue}".trim())
            add("Execução" to power.executionType.label)
            add("Alcance" to power.rangeType.label)
            if (power.targetArea.isNotBlank()) add("Alvo / Área" to power.targetArea)
            add("Duração" to power.durationType.label)
            add("Resistência" to power.resistance.label)
            if (power.effect.isNotBlank()) add("Efeito" to power.effect)
        } }.orEmpty()
        CampaignContentKind.CONDITION -> entry.conditionSnapshot?.let { condition -> buildList {
            if (condition.intensity.isNotBlank()) add("Intensidade" to condition.intensity)
            if (condition.duration.isNotBlank()) add("Duração" to condition.duration)
            if (condition.origin.isNotBlank()) add("Origem" to condition.origin)
            if (condition.summary.isNotBlank()) add("Efeito" to condition.summary)
        } }.orEmpty()
        else -> emptyList()
    }
    SdoDataModules(fields)
}

@Composable
private fun LibraryEditorDialog(
    initial: CampaignLibraryEntry,
    campaigns: List<Campaign>,
    onDismiss: () -> Unit,
    onSave: (CampaignLibraryEntry) -> Unit,
) {
    var value by remember(initial.id) { mutableStateOf(initial) }
    val campaignCanChange = initial.version == 1 && initial.lastSyncedAt == 0L
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MODELO DA BIBLIOTECA") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SdoTextButton({
                        val options = CampaignContentKind.entries
                        val kind = options[(options.indexOf(value.kind) + 1) % options.size]
                        value = value.copy(
                            kind = kind,
                            itemSnapshot = if (kind == CampaignContentKind.ITEM) value.itemSnapshot ?: InventoryItem(name = value.name, effect = value.summary) else null,
                            powerSnapshot = if (kind == CampaignContentKind.POWER) value.powerSnapshot ?: Power(name = value.name, effect = value.summary) else null,
                            conditionSnapshot = if (kind == CampaignContentKind.CONDITION) value.conditionSnapshot ?: ConditionEffect(name = value.name, summary = value.summary) else null,
                        )
                    }) { Text("TIPO // ${value.kind.name}") }
                    SdoTextButton({
                        val options = campaigns
                        if (options.isNotEmpty()) value = value.copy(campaignId = options[(options.indexOfFirst { it.id == value.campaignId } + 1).coerceAtLeast(0) % options.size].id)
                    }, enabled = campaignCanChange) { Text("CAMPANHA // ${campaigns.firstOrNull { it.id == value.campaignId }?.name ?: "FIXA"}") }
                    HudTextField("Nome", value.name, onValue = { value = value.copy(name = it) })
                    HudTextField("Resumo", value.summary, multiline = true, onValue = { value = value.copy(summary = it) })
                    when (value.kind) {
                        CampaignContentKind.ITEM -> {
                            val item = value.itemSnapshot ?: InventoryItem(name = value.name, effect = value.summary)
                            HudTextField("Categoria", item.category, onValue = { value = value.copy(itemSnapshot = item.copy(category = it)) })
                            HudTextField("Carga", item.load.toString(), onValue = { raw -> value = value.copy(itemSnapshot = item.copy(load = raw.toIntOrNull()?.coerceAtLeast(0) ?: 0)) })
                            HudTextField("Durabilidade atual", item.durabilityCurrent.toString(), onValue = { raw -> value = value.copy(itemSnapshot = item.copy(durabilityCurrent = raw.toIntOrNull()?.coerceIn(0, item.durabilityMax) ?: 0)) })
                            HudTextField("Durabilidade máxima", item.durabilityMax.toString(), onValue = { raw ->
                                val maximum = raw.toIntOrNull()?.coerceAtLeast(0) ?: 0
                                value = value.copy(itemSnapshot = item.copy(durabilityMax = maximum, durabilityCurrent = item.durabilityCurrent.coerceAtMost(maximum)))
                            })
                            HudTextField("PG", item.pg.toString(), onValue = { raw -> value = value.copy(itemSnapshot = item.copy(pg = raw.toIntOrNull() ?: 0)) })
                            HudTextField("PL", item.pl.toString(), onValue = { raw -> value = value.copy(itemSnapshot = item.copy(pl = raw.toIntOrNull() ?: 0)) })
                            HudTextField("Efeito estruturado", item.effect, multiline = true, onValue = { value = value.copy(itemSnapshot = item.copy(name = value.name, effect = it)) })
                        }
                        CampaignContentKind.POWER -> {
                            val power = value.powerSnapshot ?: Power(name = value.name, effect = value.summary)
                            ChoiceField("Fonte", power.canonicalSource, AbilitySource.entries, true, display = { it.label }) {
                                value = value.copy(powerSnapshot = power.copy(canonicalSource = it, knowledgeId = "", knowledgeLevel = null, linkedItemId = "", active = false))
                            }
                            ChoiceField("Custo", power.costType, AbilityCostType.entries.filterNot { it == AbilityCostType.DOSE }, true, display = { it.label }) {
                                value = value.copy(powerSnapshot = power.copy(costType = it, costValue = if (it == AbilityCostType.NONE) 0 else power.costValue))
                            }
                            if (power.costType != AbilityCostType.NONE) IntegerField("Valor do custo", power.costValue, true) {
                                value = value.copy(powerSnapshot = power.copy(costValue = it.coerceAtLeast(0)))
                            }
                            ChoiceField("Execução", power.executionType, AbilityExecution.entries, true, display = { it.label }) {
                                value = value.copy(powerSnapshot = power.copy(executionType = it))
                            }
                            if (power.executionType == AbilityExecution.TIME) {
                                IntegerField("Tempo de execução", power.timeValue, true) { raw -> value = value.copy(powerSnapshot = power.copy(timeValue = raw.coerceAtLeast(0))) }
                                ChoiceField("Unidade da execução", power.timeUnit, AbilityTimeUnit.entries, true, display = { it.label }) { unit -> value = value.copy(powerSnapshot = power.copy(timeUnit = unit)) }
                            }
                            ChoiceField("Alcance", power.rangeType, AbilityRange.entries, true, display = { it.label }) {
                                value = value.copy(powerSnapshot = power.copy(rangeType = it))
                            }
                            HudTextField("Alvo / Área (opcional)", power.targetArea, onValue = { value = value.copy(powerSnapshot = power.copy(targetArea = it)) })
                            if (power.executionType != AbilityExecution.PASSIVE) {
                                ChoiceField("Duração", power.durationType, AbilityDuration.entries, true, display = { it.label }) {
                                    value = value.copy(powerSnapshot = power.copy(durationType = it))
                                }
                                if (power.durationType == AbilityDuration.TURNS) IntegerField("Quantidade de turnos", power.durationValue, true) { raw ->
                                    value = value.copy(powerSnapshot = power.copy(durationValue = raw.coerceAtLeast(0)))
                                }
                                if (power.durationType == AbilityDuration.TIME) {
                                    IntegerField("Tempo de duração", power.durationValue, true) { raw -> value = value.copy(powerSnapshot = power.copy(durationValue = raw.coerceAtLeast(0))) }
                                    ChoiceField("Unidade da duração", power.durationUnit, listOf(AbilityTimeUnit.HOURS, AbilityTimeUnit.DAYS), true, display = { it.label }) { unit -> value = value.copy(powerSnapshot = power.copy(durationUnit = unit)) }
                                }
                            }
                            ChoiceField("Resistência", power.resistance, AbilityResistance.entries, true, display = { it.label }) {
                                value = value.copy(powerSnapshot = power.copy(resistance = it))
                            }
                            HudTextField("Categoria", power.category, onValue = { value = value.copy(powerSnapshot = power.copy(category = it)) })
                            HudTextField("Efeito estruturado", power.effect, multiline = true, onValue = { value = value.copy(powerSnapshot = power.copy(name = value.name, effect = it)) })
                        }
                        CampaignContentKind.CONDITION -> {
                            val condition = value.conditionSnapshot ?: ConditionEffect(name = value.name, summary = value.summary)
                            HudTextField("Intensidade", condition.intensity, onValue = { value = value.copy(conditionSnapshot = condition.copy(intensity = it)) })
                            HudTextField("Duração", condition.duration, onValue = { value = value.copy(conditionSnapshot = condition.copy(duration = it)) })
                            HudTextField("Origem", condition.origin, onValue = { value = value.copy(conditionSnapshot = condition.copy(origin = it)) })
                            HudTextField("Efeito estruturado", condition.summary, multiline = true, onValue = { value = value.copy(conditionSnapshot = condition.copy(name = value.name, summary = it)) })
                        }
                        else -> Unit
                    }
                    HudTextField("Notas adicionais", value.payload, multiline = true, onValue = { value = value.copy(payload = it) })
                    HudTextField("Bônus em Conhecimento Adquirido", value.knowledgeBonus.toString(), onValue = { value = value.copy(knowledgeBonus = it.toIntOrNull() ?: 0) })
                }
            }
        },
        confirmButton = { SdoTextButton({
            val synchronized = value.copy(
                itemSnapshot = value.itemSnapshot?.copy(name = value.name),
                powerSnapshot = value.powerSnapshot?.copy(name = value.name),
                conditionSnapshot = value.conditionSnapshot?.copy(name = value.name),
            )
            onSave(synchronized.copy(version = if (value.updatedAt == initial.updatedAt) value.version + 1 else value.version))
        }) { Text("SALVAR") } },
        dismissButton = { SdoTextButton(onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun DeliveryDialog(
    entry: CampaignLibraryEntry,
    candidates: List<Character>,
    onDismiss: () -> Unit,
    onDeliver: (List<Character>, Map<String, String>) -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var mappings by remember { mutableStateOf(emptyMap<String, String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ENTREGAR // ${entry.name}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(candidates, key = Character::id) { character ->
                    Column {
                        Row(Modifier.fillMaxWidth()) {
                            Checkbox(character.id in selected, { checked -> selected = if (checked) selected + character.id else selected - character.id })
                            Text(character.name, modifier = Modifier.padding(12.dp))
                        }
                        if (entry.knowledgeBonus != 0 && character.id in selected) HudTextField(
                            "Conhecimento Adquirido de ${character.name}",
                            mappings[character.id].orEmpty(),
                            onValue = { mappings = mappings + (character.id to it) },
                        )
                    }
                }
            }
        },
        confirmButton = { SdoTextButton({ onDeliver(candidates.filter { it.id in selected }, mappings) }, enabled = selected.isNotEmpty()) { Text("ENVIAR") } },
        dismissButton = { SdoTextButton(onDismiss) { Text("CANCELAR") } },
    )
}
