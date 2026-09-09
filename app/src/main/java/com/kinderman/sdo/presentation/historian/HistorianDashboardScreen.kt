package com.kinderman.sdo.presentation.historian

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
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
    var section by remember { mutableStateOf(HistorianSection.OPERATION) }
    var search by remember { mutableStateOf("") }
    var actionTarget by remember { mutableStateOf<Character?>(null) }
    var editingLibrary by remember { mutableStateOf<CampaignLibraryEntry?>(null) }
    var delivering by remember { mutableStateOf<CampaignLibraryEntry?>(null) }
    val historianCampaignIds = remember(session, campaigns) {
        campaigns.filter { campaign ->
            session.isAdmin || campaign.ownerId == session.uid
        }.mapTo(linkedSetOf(), Campaign::id)
    }
    val visibleCampaigns = campaigns.filter { it.id in historianCampaignIds }

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
                                    if (section == target) Modifier.border(1.dp, Acid, CutCornerShape(6.dp)) else Modifier,
                                ),
                            ) { com.kinderman.sdo.ui.AdaptiveActionLabel(target.label, color = if (section == target) Acid else Muted) }
                        }
                    }
                }
                when (section) {
                    HistorianSection.OPERATION -> {
                        item {
                            TechPanel {
                                TelemetryTag("CAMPAIGNS.${visibleCampaigns.size}")
                                Text("VISÃO OPERACIONAL", color = Ice, style = MaterialTheme.typography.titleLarge)
                                Text("Recursos, alertas e sincronização por ficha em um único lugar.", color = Muted)
                            }
                        }
                        if (visibleCampaigns.isEmpty()) item {
                            TechPanel(accent = Signal) {
                                Text("Nenhuma campanha com acesso de Historiador.", color = Ice)
                                Text("Entre como Historiador ou responsável por uma campanha para acessar esta visão.", color = Muted)
                            }
                        }
                        visibleCampaigns.forEach { campaign ->
                            val campaignCharacters = characters.filter { normalizeCampaignId(it.campaignId) == campaign.id }
                            item("campaign:${campaign.id}") {
                                TechPanel(accent = if (campaign.isArchived) Muted else Acid) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        TelemetryTag(if (campaign.isArchived) "ARCHIVED" else "LIVE")
                                        TelemetryTag("FILES.${campaignCharacters.size}")
                                    }
                                    Text(campaign.name.uppercase(), color = Ice, style = MaterialTheme.typography.titleLarge)
                                    if (campaignCharacters.isEmpty()) Text("Nenhuma ficha vinculada.", color = Muted)
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
                                    onQuickAction = { actionTarget = character },
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
                                    visibleCampaigns.firstOrNull()?.let { editingLibrary = CampaignLibraryEntry(campaignId = it.id, createdBy = session.uid) }
                                }, enabled = visibleCampaigns.isNotEmpty()) { Text("+ NOVO MODELO") }
                            }
                        }
                        val filteredLibrary = library.filter { entry -> search.isBlank() || listOf(entry.name, entry.summary, entry.kind.name).any { it.contains(search.trim(), true) } }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${filteredLibrary.size} MODELOS", color = Muted, style = MaterialTheme.typography.labelSmall)
                                Text("${deliveries.count { it.state.name == "PENDING" }} ENTREGAS PENDENTES", color = Muted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        items(filteredLibrary, key = { "library:${it.id}" }) { entry ->
                            LibraryCard(entry, onEdit = { editingLibrary = entry }, onDuplicate = { onDuplicateLibrary(entry) }, onArchive = { onArchiveLibrary(entry, !entry.archived) }, onDeliver = { delivering = entry })
                        }
                        item { Text("CATÁLOGO LOCAL DE REFERÊNCIA", color = Muted, style = MaterialTheme.typography.labelSmall) }
                        val filtered = catalog.filter { search.isBlank() || it.searchableText().contains(search.trim(), true) }
                        items(filtered.take(40), key = { "catalog:${it.id}" }) { entry -> CatalogReferenceCard(entry) }
                    }
                    HistorianSection.AUDIT -> {
                        item {
                            TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                                TelemetryTag("APPEND_ONLY.${audit.size}")
                                Text("HISTÓRICO DA CAMPANHA", color = Ice, style = MaterialTheme.typography.titleLarge)
                                Text("Autor, alvo e valores anteriores/novos são preservados em registros imutáveis.", color = Muted)
                            }
                        }
                        items(audit, key = { "audit:${it.id}" }) { operation ->
                            TechPanel(accent = if (operation.dirty) Signal else Acid) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TelemetryTag(operation.type.name)
                                    TelemetryTag(if (operation.dirty) "LOCAL_DELTA" else "SYNC_OK")
                                }
                                Text("${operation.target}: ${operation.previousValue} → ${operation.newValue}", color = Ice)
                                Text("ATOR ${operation.actorId.take(10)} // ALVO ${operation.characterId.take(10)} // ${operation.createdAt}", color = Muted, style = MaterialTheme.typography.labelSmall)
                                if (operation.reason.isNotBlank()) Text(operation.reason, color = Muted)
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
    TechPanel(accent = if (alerts.isEmpty()) Acid else Signal) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(character.name.uppercase(), color = Ice, style = MaterialTheme.typography.titleMedium)
            TelemetryTag(if (character.dirty) "LOCAL_DELTA" else "SYNC_OK")
        }
        Text(
            "VIDA ${character.life.current}/${character.lifeMaximum}  //  SAN ${character.sanity.current}/${character.sanityMaximum}  //  EXA ${character.exhaustion.current}/${character.exhaustion.maximum}",
            color = Ice,
            style = MaterialTheme.typography.bodySmall,
        )
        if (alerts.isNotEmpty()) Text(alerts.joinToString(" // "), color = Signal, style = MaterialTheme.typography.labelSmall)
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
        Text(entry.name, color = Ice, style = MaterialTheme.typography.titleMedium)
        Text(entry.group, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        Text(entry.summary, color = Muted, style = MaterialTheme.typography.bodySmall)
        if (entry.ruleReference.isNotBlank()) Text(entry.ruleReference, color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun AlertSettingsRow(settings: CampaignAlertSettings, onSave: (CampaignAlertSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("ALERTAS // VIDA ≤${settings.lifeThresholdPercent}% // SAN ≤${settings.sanityThresholdPercent}% // EXA ≥${settings.exhaustionThresholdPercent}%", color = Muted, style = MaterialTheme.typography.labelSmall)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AÇÃO RÁPIDA // ${character.name.uppercase()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton({ amount = (amount - 1).coerceAtLeast(1) }) { Text("−") }
                    Text("VALOR $amount", modifier = Modifier.padding(12.dp))
                    TextButton({ amount += 1 }) { Text("+") }
                }
                HudTextField("Motivo / nome da condição / anotação", label, onValue = { label = it })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton({ onApply(SessionCommand(type = SessionOperationType.DAMAGE, amount = amount, regionId = character.bodyRegions.firstOrNull()?.name.orEmpty(), reason = label)) }, modifier = Modifier.weight(1f)) { Text("DANO") }
                    TextButton({ onApply(SessionCommand(type = SessionOperationType.HEAL, amount = amount, resource = SessionResource.LIFE, reason = label)) }, modifier = Modifier.weight(1f)) { Text("CURA") }
                    TextButton({ onApply(SessionCommand(type = SessionOperationType.RESOURCE, amount = amount, resource = SessionResource.ENERGY, reason = label)) }, modifier = Modifier.weight(1f)) { Text("ENERGIA") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton({ onApply(SessionCommand(type = SessionOperationType.CONDITION_ADD, amount = amount, label = label.ifBlank { "Condição" }, detail = "Aplicada pela Mestre", reason = label)) }, modifier = Modifier.weight(1f)) { Text("CONDIÇÃO") }
                    TextButton({ onApply(SessionCommand(type = SessionOperationType.MONEY, amount = amount, reason = label)) }, modifier = Modifier.weight(1f)) { Text("DINHEIRO") }
                    TextButton({ onApply(SessionCommand(type = SessionOperationType.DESTINY, amount = amount, resource = SessionResource.DESTINY, reason = label)) }, modifier = Modifier.weight(1f)) { Text("DESTINO") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton({ onApply(SessionCommand(type = SessionOperationType.NOTE, label = label.ifBlank { "Anotação da Mestre" }, detail = "Registro operacional", reason = label)) }, modifier = Modifier.weight(1f)) { Text("ANOTAÇÃO") }
                    TextButton({ onApply(SessionCommand(type = SessionOperationType.REWARD, label = label.ifBlank { "Recompensa" }, detail = amount.toString(), reason = label)) }, modifier = Modifier.weight(1f)) { Text("RECOMPENSA") }
                    TextButton({ onApply(SessionCommand(type = SessionOperationType.REGION_FAILURE, amount = amount, regionId = character.bodyRegions.firstOrNull()?.name.orEmpty(), reason = label)) }, modifier = Modifier.weight(1f)) { Text("REGIÃO") }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss) { Text("FECHAR") } },
    )
}

@Composable
private fun LibraryCard(
    entry: CampaignLibraryEntry,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
    onDeliver: () -> Unit,
) {
    TechPanel(accent = if (entry.archived) Muted else MaterialTheme.colorScheme.secondary) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TelemetryTag(entry.kind.name)
            TelemetryTag("V.${entry.version}")
        }
        Text(entry.name.ifBlank { "Modelo sem nome" }, color = Ice, style = MaterialTheme.typography.titleMedium)
        Text(entry.summary, color = Muted)
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
