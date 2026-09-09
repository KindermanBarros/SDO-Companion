package com.kinderman.sdo.presentation.historian

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperation
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SessionResource
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag
import java.text.DateFormat
import java.util.Date

private enum class HistorianSection(val label: String) {
    OPERATION("OPERAÇÃO"),
    LIBRARY("BIBLIOTECA"),
    AUDIT("HISTÓRICO"),
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
    val selectedAudit = audit.filter { operation ->
        val characterName = characters.firstOrNull { it.id == operation.characterId }?.name.orEmpty()
        operation.campaignId == selectedCampaignId &&
            (auditType == null || operation.type == auditType) &&
            (auditSearch.isBlank() || listOf(operation.target, operation.reason, operation.type.name, characterName)
                .any { it.contains(auditSearch.trim(), ignoreCase = true) })
    }.sortedByDescending(SessionOperation::createdAt)
    val selectedLibrary = library.filter { it.campaignId == selectedCampaignId }

    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("PAINEL DO HISTORIADOR") },
                    navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HistorianSection.entries.forEach { target ->
                            TextButton(
                                onClick = { section = target },
                                modifier = Modifier.weight(1f).then(
                                    if (section == target) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CutCornerShape(6.dp)) else Modifier,
                                ),
                            ) { com.kinderman.sdo.ui.AdaptiveActionLabel(target.label, color = if (section == target) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
                item {
                    androidx.compose.foundation.layout.Box {
                        TextButton(
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
                                    Text(campaign.name.uppercase(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
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
                                Text("BIBLIOTECA DE REFERÊNCIA", color = Ice, style = MaterialTheme.typography.titleLarge)
                                HudTextField(
                                    label = "Buscar nome, grupo ou regra",
                                    value = search,
                                    modifier = Modifier.fillMaxWidth(),
                                    onValue = { search = it },
                                )
                                TextButton({
                                    selectedCampaign?.let { editingLibrary = CampaignLibraryEntry(campaignId = it.id, createdBy = session.uid) }
                                }, enabled = selectedCampaign != null) { Text("+ NOVO MODELO") }
                            }
                        }
                        val filteredLibrary = selectedLibrary.filter { entry -> search.isBlank() || listOf(entry.name, entry.summary, entry.kind.name).any { it.contains(search.trim(), true) } }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${filteredLibrary.size} MODELOS", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                Text("${deliveries.count { it.campaignId == selectedCampaignId && it.state.name == "PENDING" }} ENTREGAS PENDENTES", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        items(filteredLibrary, key = { "library:${it.id}" }) { entry ->
                            LibraryCard(entry, onEdit = { editingLibrary = entry }, onDuplicate = { onDuplicateLibrary(entry) }, onArchive = { onArchiveLibrary(entry, !entry.archived) }, onDeliver = { delivering = entry })
                        }
                        item { Text("CATÁLOGO LOCAL DE REFERÊNCIA", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }
                        val filtered = (catalog + racialReferences() + characterReferences(selectedCharacters)).filter { search.isBlank() || it.searchableText().contains(search.trim(), true) }
                        items(filtered, key = { "catalog:${it.id}" }) { entry -> CatalogReferenceCard(entry) }
                    }
                    HistorianSection.AUDIT -> {
                        item {
                            TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                                TelemetryTag("APPEND_ONLY.${selectedAudit.size}")
                                Text("HISTÓRICO DA CAMPANHA", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                                Text("Ações mais recentes primeiro. Busque por personagem, alvo, motivo ou tipo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HudTextField("Buscar no histórico", auditSearch, onValue = { auditSearch = it })
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(onClick = {
                                        val options = listOf<SessionOperationType?>(null) + SessionOperationType.entries
                                        auditType = options[(options.indexOf(auditType) + 1) % options.size]
                                    }, modifier = Modifier.weight(1f)) { Text("TIPO // ${auditType?.name ?: "TODOS"}", maxLines = 1) }
                                    TextButton(onClick = { auditSearch = ""; auditType = null }, modifier = Modifier.weight(1f)) { Text("LIMPAR") }
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
    editingLibrary?.let { entry -> LibraryEditorDialog(entry, visibleCampaigns, { editingLibrary = null }) { onSaveLibrary(it); editingLibrary = null } }
    delivering?.let { entry -> DeliveryDialog(entry, characters.filter { normalizeCampaignId(it.campaignId) == entry.campaignId }, { delivering = null }) { selected, mappings -> onDeliverLibrary(entry, selected, mappings); delivering = null } }
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
    val alerts = buildList {
        if (character.lifeMaximum > 0 && character.life.current * 100 <= character.lifeMaximum * settings.lifeThresholdPercent) add("VIDA CRÍTICA")
        if (character.sanityMaximum > 0 && character.sanity.current * 100 <= character.sanityMaximum * settings.sanityThresholdPercent) add("SANIDADE CRÍTICA")
        if (character.exhaustion.maximum > 0 && character.exhaustion.current * 100 >= character.exhaustion.maximum * settings.exhaustionThresholdPercent) add("EXAUSTÃO ALTA")
        if (settings.alertConditions && character.conditions.isNotEmpty()) add("${character.conditions.size} CONDIÇÃO(ÕES)")
        if (settings.alertBodyFailures && (character.bodyRegions.any { it.failures > 0 } || character.organs.any { it.failures > 0 })) add("FALHA CORPORAL")
        if (character.lastSyncedAt > 0 && System.currentTimeMillis() - character.lastSyncedAt > settings.staleAfterHours * 3_600_000L) add("SYNC ANTIGO")
        if (character.dirty) add("ALTERAÇÃO LOCAL")
    }
    TechPanel(accent = if (alerts.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(character.name.uppercase(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            TelemetryTag(if (character.dirty) "LOCAL_DELTA" else "SYNC_OK")
        }
        Text(
            "VIDA ${character.life.current}/${character.lifeMaximum}  //  SAN ${character.sanity.current}/${character.sanityMaximum}  //  EXA ${character.exhaustion.current}/${character.exhaustion.maximum}",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
        )
        if (alerts.isNotEmpty()) Text(alerts.joinToString(" // "), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onQuickAction, modifier = Modifier.weight(1f)) { Text("AÇÃO") }
            TextButton({ onOpenSession(character.id) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow, null)
                Text(" SESSÃO")
            }
            TextButton({ onOpenSheet(character.id) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Description, null)
                Text(" FICHA")
            }
        }
    }
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("ALERTAS // VIDA ≤${settings.lifeThresholdPercent}% // SAN ≤${settings.sanityThresholdPercent}% // EXA ≥${settings.exhaustionThresholdPercent}%", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton({ onSave(settings.copy(lifeThresholdPercent = (settings.lifeThresholdPercent - 5).coerceAtLeast(5))) }, modifier = Modifier.weight(1f)) { Text("VIDA −") }
            TextButton({ onSave(settings.copy(lifeThresholdPercent = (settings.lifeThresholdPercent + 5).coerceAtMost(95))) }, modifier = Modifier.weight(1f)) { Text("VIDA +") }
            TextButton({ onSave(settings.copy(staleAfterHours = if (settings.staleAfterHours == 24) 48 else 24)) }, modifier = Modifier.weight(1f)) { Text("SYNC ${settings.staleAfterHours}H") }
        }
    }
}

@Composable
private fun QuickActionsDialog(character: Character, onDismiss: () -> Unit, onApply: (SessionCommand) -> Unit) {
    var amount by remember { mutableIntStateOf(1) }
    var label by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<QuickActionKind?>(null) }
    var resource by remember { mutableStateOf(SessionResource.ENERGY) }
    var regionIndex by remember { mutableIntStateOf(0) }
    val region = character.bodyRegions.getOrNull(regionIndex)
    val command = when (selected) {
        QuickActionKind.DAMAGE -> SessionCommand(type = SessionOperationType.DAMAGE, amount = amount, regionId = region?.name.orEmpty(), reason = label)
        QuickActionKind.HEAL -> SessionCommand(type = SessionOperationType.HEAL, amount = amount, resource = SessionResource.LIFE, reason = label)
        QuickActionKind.RESOURCE -> SessionCommand(type = SessionOperationType.RESOURCE, amount = amount, resource = resource, reason = label)
        QuickActionKind.CONDITION -> SessionCommand(type = SessionOperationType.CONDITION_ADD, amount = amount, label = label.ifBlank { "Condição" }, detail = "Aplicada pelo Historiador", reason = label)
        QuickActionKind.MONEY -> SessionCommand(type = SessionOperationType.MONEY, amount = amount, reason = label)
        QuickActionKind.DESTINY -> SessionCommand(type = SessionOperationType.DESTINY, amount = amount, resource = SessionResource.DESTINY, reason = label)
        QuickActionKind.NOTE -> SessionCommand(type = SessionOperationType.NOTE, label = label.ifBlank { "Anotação do Historiador" }, detail = "Registro operacional", reason = label)
        QuickActionKind.REWARD -> SessionCommand(type = SessionOperationType.REWARD, label = label.ifBlank { "Recompensa" }, detail = amount.toString(), reason = label)
        QuickActionKind.REGION -> SessionCommand(type = SessionOperationType.REGION_FAILURE, amount = amount, regionId = region?.name.orEmpty(), reason = label)
        null -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AÇÃO RÁPIDA // ${character.name.uppercase()}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. ESCOLHA A AÇÃO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                QuickActionKind.entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { action ->
                            TextButton(
                                onClick = { selected = action },
                                modifier = Modifier.weight(1f).then(if (selected == action) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CutCornerShape(4.dp)) else Modifier),
                            ) { Text(action.label, maxLines = 1) }
                        }
                    }
                }
                Text("2. DEFINA OS DADOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton({ amount = (amount - 1).coerceAtLeast(1) }) { Text("−") }
                    Text("VALOR $amount", modifier = Modifier.padding(12.dp))
                    TextButton({ amount += 1 }) { Text("+") }
                }
                if (selected == QuickActionKind.RESOURCE) {
                    TextButton(onClick = {
                        val options = listOf(SessionResource.ENERGY, SessionResource.ARCANE, SessionResource.SANITY, SessionResource.LIFE)
                        resource = options[(options.indexOf(resource) + 1) % options.size]
                    }, modifier = Modifier.fillMaxWidth()) { Text("RECURSO // ${resource.name}") }
                }
                if (selected == QuickActionKind.DAMAGE || selected == QuickActionKind.REGION) {
                    TextButton(onClick = {
                        if (character.bodyRegions.isNotEmpty()) regionIndex = (regionIndex + 1) % character.bodyRegions.size
                    }, enabled = character.bodyRegions.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text("REGIÃO // ${region?.name?.uppercase() ?: "NENHUMA"}")
                    }
                }
                HudTextField("Motivo / nome / anotação", label, onValue = { label = it })
                Text("3. CONFIRME", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                Text(selected?.let { "${it.label} // valor $amount // ${character.name}" } ?: "Selecione uma ação acima.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { command?.let(onApply) }, enabled = command != null) { Text("APLICAR") } },
        dismissButton = { TextButton(onDismiss) { Text("CANCELAR") } },
    )
}

private enum class QuickActionKind(val label: String) {
    DAMAGE("DANO"), HEAL("CURA"), RESOURCE("RECURSO"),
    CONDITION("CONDIÇÃO"), MONEY("DINHEIRO"), DESTINY("DESTINO"),
    NOTE("ANOTAÇÃO"), REWARD("RECOMPENSA"), REGION("FALHA LOCAL"),
}

@Composable
private fun LibraryCard(
    entry: CampaignLibraryEntry,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
    onDeliver: () -> Unit,
) {
    TechPanel(accent = if (entry.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TelemetryTag(entry.kind.name)
            TelemetryTag("V.${entry.version}")
        }
        Text(entry.name.ifBlank { "Modelo sem nome" }, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
        Text(entry.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(entry.payload, style = MaterialTheme.typography.bodySmall)
        if (entry.knowledgeBonus != 0) Text("Bônus de Conhecimento: ${entry.knowledgeBonus}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onEdit, modifier = Modifier.weight(1f)) { Text("EDITAR") }
            TextButton(onDuplicate, modifier = Modifier.weight(1f)) { Text("DUPLICAR") }
            TextButton(onArchive, modifier = Modifier.weight(1f)) { Text(if (entry.archived) "RESTAURAR" else "ARQUIVAR") }
            TextButton(onDeliver, enabled = !entry.archived, modifier = Modifier.weight(1f)) { Text("ENTREGAR") }
        }
    }
}

@Composable
private fun LibraryEditorDialog(
    initial: CampaignLibraryEntry,
    campaigns: List<Campaign>,
    onDismiss: () -> Unit,
    onSave: (CampaignLibraryEntry) -> Unit,
) {
    var value by remember(initial.id) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MODELO DA BIBLIOTECA") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    TextButton({
                        val options = CampaignContentKind.entries
                        value = value.copy(kind = options[(options.indexOf(value.kind) + 1) % options.size])
                    }) { Text("TIPO // ${value.kind.name}") }
                    TextButton({
                        val options = campaigns
                        if (options.isNotEmpty()) value = value.copy(campaignId = options[(options.indexOfFirst { it.id == value.campaignId } + 1).coerceAtLeast(0) % options.size].id)
                    }) { Text("CAMPANHA // ${campaigns.firstOrNull { it.id == value.campaignId }?.name ?: "SELECIONE"}") }
                    HudTextField("Nome", value.name, onValue = { value = value.copy(name = it) })
                    HudTextField("Resumo", value.summary, multiline = true, onValue = { value = value.copy(summary = it) })
                    HudTextField("Conteúdo / payload", value.payload, multiline = true, onValue = { value = value.copy(payload = it) })
                    HudTextField("Bônus em Conhecimento Adquirido", value.knowledgeBonus.toString(), onValue = { value = value.copy(knowledgeBonus = it.toIntOrNull() ?: 0) })
                }
            }
        },
        confirmButton = { TextButton({ onSave(value.copy(version = if (value.updatedAt == initial.updatedAt) value.version + 1 else value.version)) }) { Text("SALVAR") } },
        dismissButton = { TextButton(onDismiss) { Text("CANCELAR") } },
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
        confirmButton = { TextButton({ onDeliver(candidates.filter { it.id in selected }, mappings) }, enabled = selected.isNotEmpty()) { Text("ENVIAR") } },
        dismissButton = { TextButton(onDismiss) { Text("CANCELAR") } },
    )
}
