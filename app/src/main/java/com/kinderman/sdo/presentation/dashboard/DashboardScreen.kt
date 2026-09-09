package com.kinderman.sdo.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignInvitePreview
import com.kinderman.sdo.domain.model.CampaignInvite
import com.kinderman.sdo.domain.model.CampaignDelivery
import com.kinderman.sdo.domain.model.CampaignDeliveryState
import com.kinderman.sdo.domain.model.CampaignMember
import com.kinderman.sdo.domain.model.CampaignRole
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.AcidCyan
import com.kinderman.sdo.ui.Barcode
import com.kinderman.sdo.ui.CyberLoadingIndicator
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.LabelFunctional
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Panel
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechCutDark
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag
import com.kinderman.sdo.ui.Void

@Composable
fun DashboardScreen(
    characters: List<Character>,
    campaigns: List<Campaign>,
    memberships: List<CampaignMember>,
    campaignInvites: List<CampaignInvite>,
    campaignMembers: List<CampaignMember>,
    owners: List<UserProfile>,
    session: UserSession?,
    syncing: Boolean,
    compactCards: Boolean,
    collapseCampaignCards: Boolean,
    showArchivedCampaigns: Boolean,
    invitePreview: CampaignInvitePreview?,
    snackbarHost: @Composable () -> Unit,
    onAdd: () -> Unit,
    onAddToCampaign: (Campaign, String) -> Unit,
    onOpen: (String) -> Unit,
    onOwnerTransfer: (Character, UserProfile) -> Unit,
    onCreateCampaign: (String, String) -> Unit,
    onArchiveCampaign: (Campaign, Boolean) -> Unit,
    onDeleteCampaign: (Campaign) -> Unit,
    onLeaveCampaign: (Campaign) -> Unit,
    onPreviewInvite: (String) -> Unit,
    onAcceptInvite: (String, Character?, Boolean) -> Unit,
    onDismissInvitePreview: () -> Unit,
    onSync: () -> Unit,
    onOpenSession: () -> Unit,
    onOpenHistorian: () -> Unit,
    onOpenSettings: () -> Unit,
    deliveries: List<CampaignDelivery>,
    onRespondDelivery: (CampaignDelivery, Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    val admin = session?.isAdmin == true
    val uid = session?.uid.orEmpty()
    val ownersById = remember(owners) { owners.associateBy(UserProfile::uid) }
    val rolesByCampaign = remember(memberships) { memberships.associateBy({ it.campaignId }, { it.role }) }
    val activeCampaigns = campaigns.filterNot(Campaign::isArchived)
    val archivedCampaigns = if (showArchivedCampaigns) campaigns.filter(Campaign::isArchived) else emptyList()
    var ownerTarget by remember { mutableStateOf<Character?>(null) }
    var createCampaign by remember { mutableStateOf(false) }
    var joinCampaign by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var ownerFilter by remember { mutableStateOf<String?>(null) }
    var campaignFilter by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf(AdminCharacterStatus.ALL) }
    var choosingOwner by remember { mutableStateOf(false) }
    var choosingCampaign by remember { mutableStateOf(false) }
    var choosingStatus by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf(DashboardSection.CHARACTERS) }
    var assigningCampaign by remember { mutableStateOf<Campaign?>(null) }
    var deletingCampaign by remember { mutableStateOf<Campaign?>(null) }
    val filtersActive = searchQuery.isNotBlank() || ownerFilter != null ||
        campaignFilter != null || statusFilter != AdminCharacterStatus.ALL
    val filteredCharacters = remember(
        characters, ownersById, campaigns, searchQuery, ownerFilter, campaignFilter, statusFilter, admin,
    ) {
        characters.filter { character ->
            val campaignId = normalizeCampaignId(character.campaignId)
            val owner = ownersById[character.ownerId]
            val campaign = campaigns.firstOrNull { it.id == campaignId }
            val queryMatches = searchQuery.isBlank() || listOf(
                character.name,
                character.race,
                character.occupation,
                character.ownerId,
                owner?.displayName.orEmpty(),
                owner?.email.orEmpty(),
                campaign?.name.orEmpty(),
            ).any { it.contains(searchQuery.trim(), ignoreCase = true) }
            val ownerMatches = !admin || ownerFilter == null || character.ownerId == ownerFilter
            val campaignMatches = when (campaignFilter) {
                null -> true
                STANDALONE_FILTER -> campaignId.isBlank()
                else -> campaignId == campaignFilter
            }.let { !admin || it }
            val statusMatches = when (statusFilter) {
                AdminCharacterStatus.ALL -> true
                AdminCharacterStatus.SYNCED -> !character.dirty
                AdminCharacterStatus.PENDING -> character.dirty
                AdminCharacterStatus.LOCKED -> character.isLocked
            }.let { !admin || it }
            queryMatches && ownerMatches && campaignMatches && statusMatches
        }
    }
    val standalone = filteredCharacters.filter { normalizeCampaignId(it.campaignId).isBlank() }

    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost,
            floatingActionButton = {
                if (section == DashboardSection.CHARACTERS) {
                    FloatingActionButton(
                        onClick = onAdd,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp),
                    ) { Icon(Icons.Default.Add, "Criar personagem sem campanha") }
                }
            },
        ) { padding ->
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(if (compactCards) 9.dp else 14.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(if (compactCards) 8.dp else 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TelemetryTag(if (admin) "ADMIN_ACCESS" else "ACCOUNT_ACCESS")
                        Row {
                            IconButton(onClick = onSync, enabled = !syncing) { Icon(Icons.Default.Sync, "Sincronizar", tint = MaterialTheme.colorScheme.primary) }
                            IconButton(onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, "Sair", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                    Text("SDO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text(if (admin) "PAINEL ADMINISTRATIVO" else "MINHAS FICHAS", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        if (admin) "${filteredCharacters.size}/${characters.size} FICHAS // ${activeCampaigns.size} CAMPANHAS ATIVAS"
                        else "${activeCampaigns.size} CAMPANHAS ATIVAS // ${standalone.size} FICHAS SEM CAMPANHA",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardSection.entries.forEach { target ->
                            TextButton(
                                onClick = { section = target },
                                modifier = Modifier.weight(1f).then(
                                    if (section == target) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CutCornerShape(6.dp)) else Modifier,
                                ),
                            ) {
                                Text(target.label, color = if (section == target) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                        TelemetryTag("QUICK_ACCESS")
                        Text("Acessos rápidos", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = onOpenSession, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PlayCircle, null)
                                Text(" SESSÃO")
                            }
                            if (admin || activeCampaigns.any { it.ownerId == uid }) TextButton(onClick = onOpenHistorian, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Visibility, null)
                                Text(" MESTRE")
                            }
                            TextButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Palette, null)
                                Text(" TEMA")
                            }
                        }
                    }
                    val pendingDeliveries = deliveries.filter { it.state == CampaignDeliveryState.PENDING }
                    if (pendingDeliveries.isNotEmpty()) TechPanel(accent = MaterialTheme.colorScheme.error) {
                        TelemetryTag("INBOX.${pendingDeliveries.size}", MaterialTheme.colorScheme.error)
                        Text("Entregas da campanha", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                        pendingDeliveries.forEach { delivery ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${delivery.snapshotKind.name} // ${delivery.snapshotName}", color = MaterialTheme.colorScheme.onSurface)
                                Text(delivery.snapshotSummary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton({ onRespondDelivery(delivery, true) }, modifier = Modifier.weight(1f)) { Text("ACEITAR") }
                                    TextButton({ onRespondDelivery(delivery, false) }, modifier = Modifier.weight(1f)) { Text("RECUSAR", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                    if (section == DashboardSection.CAMPAIGNS) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { createCampaign = true }, modifier = Modifier.weight(1f)) { Text("+ CAMPANHA") }
                            TextButton(onClick = { joinCampaign = true }, modifier = Modifier.weight(1f)) { Text("ENTRAR POR CÓDIGO") }
                        }
                    }
                    }
                }

                if (admin && section == DashboardSection.CHARACTERS) item("admin-filters") {
                    AdminCharacterFilters(
                        query = searchQuery,
                        ownerLabel = ownerFilter?.let { ownersById[it]?.firstName ?: it.take(8) } ?: "Todos os owners",
                        campaignLabel = when (campaignFilter) {
                            null -> "Todas as campanhas"
                            STANDALONE_FILTER -> "Sem campanha"
                            else -> campaigns.firstOrNull { it.id == campaignFilter }?.name ?: "Campanha"
                        },
                        statusLabel = statusFilter.label,
                        onQueryChange = { searchQuery = it },
                        onChooseOwner = { choosingOwner = true },
                        onChooseCampaign = { choosingCampaign = true },
                        onChooseStatus = { choosingStatus = true },
                        onClear = {
                            searchQuery = ""
                            ownerFilter = null
                            campaignFilter = null
                            statusFilter = AdminCharacterStatus.ALL
                        },
                    )
                }

                if (!admin && section == DashboardSection.CHARACTERS) item("character-search") {
                    CompactSearchField(
                        label = "Buscar minhas fichas",
                        query = searchQuery,
                        placeholder = "Nome, raça ou ocupação",
                        onQueryChange = { searchQuery = it },
                    )
                }

                if (syncing) item("sync-loading") {
                    TechPanel(accent = MaterialTheme.colorScheme.primary) {
                        TelemetryTag("SINCRONIZAÇÃO")
                        CyberLoadingIndicator("Sincronizando campanhas e personagens")
                        Text("Enviando alterações preservadas neste aparelho", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }

                when (section) {
                    DashboardSection.CHARACTERS -> {
                        if (filteredCharacters.isEmpty()) item("empty-characters") {
                            EmptyDashboardPanel(
                                title = if (filtersActive) "Nenhuma ficha encontrada" else "Nenhuma ficha detectada",
                                message = if (filtersActive) "Ajuste ou limpe os filtros de pesquisa." else "Crie uma ficha para iniciar o arquivo.",
                            )
                        }
                        items(filteredCharacters, key = { "character:${it.id}" }) { character ->
                            CharacterAccessCard(
                                character = character,
                                master = admin,
                                owner = ownersById[character.ownerId],
                                campaignName = campaigns.firstOrNull {
                                    it.id == normalizeCampaignId(character.campaignId)
                                }?.name,
                                onOpen = { onOpen(character.id) },
                                onOwnerClick = { ownerTarget = character },
                            )
                        }
                    }

                    DashboardSection.CAMPAIGNS -> {
                        if (activeCampaigns.isEmpty() && archivedCampaigns.isEmpty()) item("empty-campaigns") {
                            EmptyDashboardPanel(
                                title = "Nenhuma campanha detectada",
                                message = "Crie uma campanha ou entre usando um código de convite.",
                            )
                        }
                        activeCampaigns.forEachIndexed { campaignIndex, campaign ->
                            item("campaign-${campaign.id}") {
                                CampaignPanel(
                                    campaign = campaign,
                                    owner = campaign.ownerId == uid,
                                    administrator = admin,
                                    canAdd = campaign.ownerId == uid || rolesByCampaign[campaign.id] != null,
                                    role = rolesByCampaign[campaign.id],
                                    archived = false,
                                    inviteCode = campaignInvites.firstOrNull { it.campaignId == campaign.id }?.code.orEmpty(),
                                    characterCount = characters.count { normalizeCampaignId(it.campaignId) == campaign.id },
                                    memberCount = campaignMembers.count { it.campaignId == campaign.id && it.isActive },
                                    initiallyExpanded = !collapseCampaignCards,
                                    onAdd = {
                                        if (campaign.ownerId == uid || admin) assigningCampaign = campaign
                                        else onAddToCampaign(campaign, uid)
                                    },
                                    onArchive = { onArchiveCampaign(campaign, true) },
                                    onDelete = {},
                                    onLeave = { onLeaveCampaign(campaign) },
                                    index = (campaignIndex + 1).toString().padStart(2, '0'),
                                )
                            }
                        }
                        if (archivedCampaigns.isNotEmpty()) item("archived-header") {
                            SectionHeader("AR", "Campanhas arquivadas")
                        }
                        archivedCampaigns.forEach { campaign ->
                            item("archived-${campaign.id}") {
                                CampaignPanel(
                                    campaign = campaign,
                                    owner = campaign.ownerId == uid,
                                    administrator = admin,
                                    canAdd = false,
                                    role = rolesByCampaign[campaign.id],
                                    archived = true,
                                    inviteCode = campaignInvites.firstOrNull { it.campaignId == campaign.id }?.code.orEmpty(),
                                    characterCount = characters.count { normalizeCampaignId(it.campaignId) == campaign.id },
                                    memberCount = campaignMembers.count { it.campaignId == campaign.id && it.isActive },
                                    initiallyExpanded = !collapseCampaignCards,
                                    onAdd = {},
                                    onArchive = { onArchiveCampaign(campaign, false) },
                                    onDelete = { deletingCampaign = campaign },
                                    onLeave = {},
                                    index = "AR",
                                )
                            }
                        }
                    }
                }
            }
        }

        ownerTarget?.let { character ->
            OwnerPickerDialog(
                character = character,
                owners = owners,
                onDismiss = { ownerTarget = null },
                onSelect = { owner ->
                    ownerTarget = null
                    onOwnerTransfer(character, owner)
                },
            )
        }
        if (createCampaign) {
            CreateCampaignDialog(
                onDismiss = { createCampaign = false },
                onCreate = { name, description ->
                    createCampaign = false
                    onCreateCampaign(name, description)
                },
            )
        }
        if (joinCampaign) {
            JoinCampaignDialog(
                onDismiss = { joinCampaign = false },
                onPreview = { code ->
                    joinCampaign = false
                    onPreviewInvite(code)
                },
            )
        }
        invitePreview?.let { preview ->
            InvitePreviewDialog(
                preview = preview,
                characters = standalone.filter { it.ownerId == uid },
                onDismiss = onDismissInvitePreview,
                onAccept = onAcceptInvite,
            )
        }
        deletingCampaign?.let { campaign ->
            AlertDialog(
                onDismissRequest = { deletingCampaign = null },
                title = { Text("APAGAR CAMPANHA") },
                text = { Text("${campaign.name} será apagada definitivamente. As fichas serão preservadas e desvinculadas. Esta ação exige conexão.") },
                confirmButton = {
                    TextButton(onClick = {
                        deletingCampaign = null
                        onDeleteCampaign(campaign)
                    }) { Text("APAGAR", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { deletingCampaign = null }) { Text("CANCELAR") } },
            )
        }
        if (choosingOwner) {
            val ownerOptions = characters.map(Character::ownerId).distinct().map { ownerId ->
                ownerId to (ownersById[ownerId]?.let { "${it.firstName} // ${it.email}" } ?: "UID.${ownerId.take(8)}")
            }.sortedBy { it.second }
            FilterSelectionDialog(
                title = "FILTRAR POR OWNER",
                options = listOf(null to "Todos os owners") + ownerOptions,
                onDismiss = { choosingOwner = false },
                onSelect = { ownerFilter = it; choosingOwner = false },
            )
        }
        if (choosingCampaign) FilterSelectionDialog(
            title = "FILTRAR POR CAMPANHA",
            options = listOf(
                null to "Todas as campanhas",
                STANDALONE_FILTER to "Sem campanha",
            ) + campaigns.sortedBy(Campaign::name).map { it.id to it.name },
            onDismiss = { choosingCampaign = false },
            onSelect = { campaignFilter = it; choosingCampaign = false },
        )
        if (choosingStatus) FilterSelectionDialog(
            title = "FILTRAR POR STATUS",
            options = AdminCharacterStatus.entries.map { it.name to it.label },
            onDismiss = { choosingStatus = false },
            onSelect = { value ->
                statusFilter = AdminCharacterStatus.entries.firstOrNull { it.name == value } ?: AdminCharacterStatus.ALL
                choosingStatus = false
            },
        )
        assigningCampaign?.let { campaign ->
            val activePlayers = campaignMembers.filter {
                it.campaignId == campaign.id && it.isActive && it.userId != campaign.ownerId
            }
            AssignCharacterDialog(
                campaign = campaign,
                ownerId = campaign.ownerId,
                players = activePlayers,
                onDismiss = { assigningCampaign = null },
                onSelect = { ownerId ->
                    assigningCampaign = null
                    onAddToCampaign(campaign, ownerId)
                },
            )
        }
    }
}

private const val STANDALONE_FILTER = "__standalone__"

private enum class DashboardSection(val label: String) {
    CHARACTERS("FICHAS"),
    CAMPAIGNS("CAMPANHAS"),
}

private enum class AdminCharacterStatus(val label: String) {
    ALL("Todos os status"),
    SYNCED("Sincronizadas"),
    PENDING("Pendentes"),
    LOCKED("Bloqueadas"),
}

@Composable
private fun AdminCharacterFilters(
    query: String,
    ownerLabel: String,
    campaignLabel: String,
    statusLabel: String,
    onQueryChange: (String) -> Unit,
    onChooseOwner: () -> Unit,
    onChooseCampaign: () -> Unit,
    onChooseStatus: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, CutCornerShape(8.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HudTextField(
            label = "Pesquisa administrativa",
            value = query,
            placeholder = "Nome, raça, ocupação, e-mail ou UID",
            onValue = onQueryChange,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onChooseOwner, modifier = Modifier.weight(1f)) {
                Text("OWNER // ${ownerLabel.uppercase()}", maxLines = 1)
            }
            TextButton(onClick = onChooseCampaign, modifier = Modifier.weight(1f)) {
                Text("CAMPANHA // ${campaignLabel.uppercase()}", maxLines = 1)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onChooseStatus, modifier = Modifier.weight(1f)) {
                Text("STATUS // ${statusLabel.uppercase()}", maxLines = 1)
            }
            TextButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                Text("LIMPAR", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CompactSearchField(
    label: String,
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, CutCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        HudTextField(label = label, value = query, placeholder = placeholder, onValue = onQueryChange)
    }
}

@Composable
private fun FilterSelectionDialog(
    title: String,
    options: List<Pair<String?, String>>,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                items(options, key = { "${it.first.orEmpty()}:${it.second}" }) { (value, label) ->
                    TextButton(onClick = { onSelect(value) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label.uppercase())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun EmptyDashboardPanel(title: String, message: String) {
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        SectionHeader("00", title)
        Text(message)
        Barcode("EMPTY-SDO-ARCHIVE")
    }
}

@Composable
private fun AssignCharacterDialog(
    campaign: Campaign,
    ownerId: String,
    players: List<CampaignMember>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ATRIBUIR NOVA FICHA") },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                item {
                    TextButton(onClick = { onSelect(ownerId) }, modifier = Modifier.fillMaxWidth()) {
                        Text("PARA MIM // MESTRE")
                    }
                }
                items(players, key = CampaignMember::userId) { player ->
                    TextButton(onClick = { onSelect(player.userId) }, modifier = Modifier.fillMaxWidth()) {
                        Text("JOGADOR // ${player.userId.take(12).uppercase()}")
                    }
                }
                if (players.isEmpty()) item {
                    Text("Nenhum jogador convidado está ativo em ${campaign.name}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun CampaignPanel(
    campaign: Campaign,
    owner: Boolean,
    administrator: Boolean,
    canAdd: Boolean,
    role: CampaignRole?,
    archived: Boolean,
    inviteCode: String,
    characterCount: Int,
    memberCount: Int,
    initiallyExpanded: Boolean,
    onAdd: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onLeave: () -> Unit,
    index: String,
) {
    var expanded by rememberSaveable(campaign.id) { mutableStateOf(initiallyExpanded) }
    TechPanel(
        modifier = Modifier.animateContentSize(),
        accent = if (archived) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.secondary,
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TelemetryTag(if (archived) "ARCHIVED" else "ACTIVE", if (archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary)
                    TelemetryTag(
                        when {
                            administrator -> "ADMIN"
                            owner -> "MESTRE // CRIADOR"
                            else -> "PLAYER"
                        },
                    )
                }
                Text(campaign.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall)
            }
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                if (expanded) "Recolher campanha" else "Expandir campanha",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(
            Modifier.fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary, CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("CÓDIGO DA CAMPANHA", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(inviteCode.ifBlank { "SINCRONIZANDO" }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            }
        }
        if (expanded) {
            if (campaign.description.isNotBlank()) {
                Text(campaign.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampaignMetric("FICHAS", characterCount.toString(), Modifier.weight(1f))
                CampaignMetric("PARTICIPANTES", memberCount.toString(), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!archived && canAdd) {
                    androidx.compose.material3.Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("NOVA FICHA") }
                }
                if (owner || administrator) {
                    TextButton(onClick = onArchive, modifier = Modifier.weight(1f)) {
                        Text(if (archived) "RESTAURAR" else "ARQUIVAR")
                    }
                } else if (!archived) {
                    TextButton(onClick = onLeave, modifier = Modifier.weight(1f)) { Text("SAIR", color = MaterialTheme.colorScheme.error) }
                }
            }
            if ((owner || administrator) && archived) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("APAGAR DEFINITIVAMENTE", color = MaterialTheme.colorScheme.error)
                }
            }
            Text("ID ${campaign.id.take(12).uppercase()} // REG.$index", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CampaignMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CutCornerShape(6.dp)).padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun CreateCampaignDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NOVA CAMPANHA") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HudTextField("Nome", name) { name = it }
                HudTextField("Descrição", description, multiline = true) { description = it }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name, description) }) { Text("CRIAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun JoinCampaignDialog(onDismiss: () -> Unit, onPreview: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ENTRAR POR CÓDIGO") },
        text = {
            HudTextField("Código do convite", code, placeholder = "XXXXXXXX") {
                code = it.uppercase().filter(Char::isLetterOrDigit).take(8)
            }
        },
        confirmButton = {
            TextButton(enabled = code.length == 8, onClick = { onPreview(code) }) { Text("VER CAMPANHA") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun InvitePreviewDialog(
    preview: CampaignInvitePreview,
    characters: List<Character>,
    onDismiss: () -> Unit,
    onAccept: (String, Character?, Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(preview.campaign.name.uppercase()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (preview.campaign.description.isNotBlank()) Text(preview.campaign.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("CONVITE // ${preview.invite.code}", color = MaterialTheme.colorScheme.primary)
                if (preview.alreadyMember) {
                    Text("Você já participa desta campanha.", color = MaterialTheme.colorScheme.secondary)
                } else {
                    Text("Escolha uma ficha sem campanha ou crie uma nova.", color = MaterialTheme.colorScheme.onSurface)
                    characters.forEach { character ->
                        TextButton(
                            onClick = { onAccept(preview.invite.code, character, false) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("USAR // ${character.name.uppercase()}") }
                    }
                    TextButton(
                        onClick = { onAccept(preview.invite.code, null, true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("+ CRIAR NOVA FICHA") }
                    TextButton(
                        onClick = { onAccept(preview.invite.code, null, false) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("ENTRAR SEM VINCULAR FICHA") }
                }
            }
        },
        confirmButton = {
            if (preview.alreadyMember) TextButton(onClick = onDismiss) { Text("FECHAR") }
        },
        dismissButton = {
            if (!preview.alreadyMember) TextButton(onClick = onDismiss) { Text("CANCELAR") }
        },
    )
}

@Composable
private fun CharacterAccessCard(
    character: Character,
    master: Boolean,
    owner: UserProfile?,
    campaignName: String?,
    onOpen: () -> Unit,
    onOwnerClick: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().border(
            1.dp,
            when { character.isLocked -> MaterialTheme.colorScheme.error; character.dirty -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.outlineVariant },
            CutCornerShape(topEnd = 24.dp, bottomStart = 12.dp),
        ),
        shape = CutCornerShape(topEnd = 24.dp, bottomStart = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TelemetryTag("ID.${character.id.take(6)}")
                TelemetryTag(
                    when {
                        character.lockType == CharacterLock.HISTORIAN -> "LOCK.H"
                        character.lockType == CharacterLock.PLAYER -> "LOCK.P"
                        character.dirty -> "LOCAL_DELTA"
                        else -> "SYNC_OK"
                    },
                    when { character.isLocked -> MaterialTheme.colorScheme.error; character.dirty -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.secondary },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).background(MaterialTheme.colorScheme.primary, CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)), contentAlignment = Alignment.Center) {
                    Text(character.name.take(2).uppercase(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.size(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(character.name.uppercase(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                    Text(
                        listOf(character.race, character.occupation, "LV.${character.level}").filter(String::isNotBlank).joinToString(" // "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        campaignName?.let { "CAMPANHA // ${it.uppercase()}" } ?: "SEM CAMPANHA",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    if (master) {
                        TextButton(onClick = onOwnerClick, contentPadding = PaddingValues(0.dp)) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("OWNER // ${owner?.firstName?.uppercase() ?: "SEM PERFIL"}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                Text("UID.${character.ownerId.take(8)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Icon(
                    when { character.isLocked -> Icons.Default.Lock; master -> Icons.Default.AdminPanelSettings; else -> Icons.Default.ChevronRight },
                    null,
                    tint = if (character.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            Barcode(character.id)
        }
    }
}
