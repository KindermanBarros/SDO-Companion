package com.kinderman.sdo.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignInvitePreview
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
    owners: List<UserProfile>,
    session: UserSession?,
    syncing: Boolean,
    invitePreview: CampaignInvitePreview?,
    snackbarHost: @Composable () -> Unit,
    onAdd: () -> Unit,
    onAddToCampaign: (Campaign) -> Unit,
    onOpen: (String) -> Unit,
    onOwnerTransfer: (Character, UserProfile) -> Unit,
    onCreateCampaign: (String, String) -> Unit,
    onArchiveCampaign: (Campaign, Boolean) -> Unit,
    onLeaveCampaign: (Campaign) -> Unit,
    onCreateInvite: (Campaign) -> Unit,
    onPreviewInvite: (String) -> Unit,
    onAcceptInvite: (String, Character?, Boolean) -> Unit,
    onDismissInvitePreview: () -> Unit,
    onSync: () -> Unit,
    onLogout: () -> Unit,
) {
    val master = session?.isMaster == true
    val uid = session?.uid.orEmpty()
    val ownersById = remember(owners) { owners.associateBy(UserProfile::uid) }
    val activeCampaigns = campaigns.filterNot(Campaign::isArchived)
    val archivedCampaigns = campaigns.filter(Campaign::isArchived)
    val standalone = characters.filter { normalizeCampaignId(it.campaignId).isBlank() }
    var ownerTarget by remember { mutableStateOf<Character?>(null) }
    var createCampaign by remember { mutableStateOf(false) }
    var joinCampaign by remember { mutableStateOf(false) }

    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = Acid,
                    contentColor = Void,
                    shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp),
                ) { Icon(Icons.Default.Add, "Criar personagem sem campanha") }
            },
        ) { padding ->
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TelemetryTag(if (master) "HISTORIAN_ACCESS" else "PLAYER_ACCESS")
                        Row {
                            IconButton(onClick = onSync, enabled = !syncing) { Icon(Icons.Default.Sync, "Sincronizar", tint = Acid) }
                            IconButton(onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, "Sair", tint = Signal) }
                        }
                    }
                    Text("SDO", color = Acid, style = MaterialTheme.typography.labelLarge)
                    Text(if (master) "PAINEL DA MESTRE" else "ARQUIVOS DE CAMPO", style = MaterialTheme.typography.headlineLarge, color = Ice)
                    Text("${activeCampaigns.size} CAMPANHAS ATIVAS // ${standalone.size} FICHAS SEM CAMPANHA", color = Muted, style = MaterialTheme.typography.labelSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { createCampaign = true }, modifier = Modifier.weight(1f)) { Text("+ CAMPANHA") }
                        TextButton(onClick = { joinCampaign = true }, modifier = Modifier.weight(1f)) { Text("ENTRAR POR CÓDIGO") }
                    }
                }

                if (syncing) item("sync-loading") {
                    TechPanel(accent = Acid) {
                        TelemetryTag("DATA.13 // SYNC")
                        CyberLoadingIndicator("Sincronizando campanhas e personagens")
                        Text("CACHE LOCAL // FIREBASE", color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (activeCampaigns.isEmpty() && standalone.isEmpty() && archivedCampaigns.isEmpty()) item {
                    TechPanel(accent = Signal) {
                        SectionHeader("00", "Nenhum sinal detectado")
                        Text("Crie uma campanha, entre por código ou crie uma ficha sem campanha.")
                        Barcode("EMPTY-SDO-ARCHIVE")
                    }
                }

                activeCampaigns.forEachIndexed { campaignIndex, campaign ->
                    item("campaign-${campaign.id}") {
                        CampaignPanel(
                            campaign = campaign,
                            owner = campaign.ownerId == uid,
                            archived = false,
                            onAdd = { onAddToCampaign(campaign) },
                            onInvite = { onCreateInvite(campaign) },
                            onArchive = { onArchiveCampaign(campaign, true) },
                            onLeave = { onLeaveCampaign(campaign) },
                            index = (campaignIndex + 1).toString().padStart(2, '0'),
                        )
                    }
                    items(
                        characters.filter { normalizeCampaignId(it.campaignId) == campaign.id },
                        key = { "${campaign.id}:${it.id}" },
                    ) { character ->
                        CharacterAccessCard(
                            character = character,
                            master = master,
                            owner = ownersById[character.ownerId],
                            onOpen = { onOpen(character.id) },
                            onOwnerClick = { ownerTarget = character },
                        )
                    }
                }

                if (standalone.isNotEmpty()) {
                    item("standalone-header") {
                        TechPanel {
                            SectionHeader("SC", "Fichas sem campanha")
                            Text("Arquivos pessoais ainda não vinculados a uma campanha.", color = Muted)
                        }
                    }
                    items(standalone, key = { "standalone:${it.id}" }) { character ->
                        CharacterAccessCard(
                            character = character,
                            master = master,
                            owner = ownersById[character.ownerId],
                            onOpen = { onOpen(character.id) },
                            onOwnerClick = { ownerTarget = character },
                        )
                    }
                }

                if (archivedCampaigns.isNotEmpty()) {
                    item("archived-header") { SectionHeader("AR", "Campanhas arquivadas") }
                    archivedCampaigns.forEach { campaign ->
                        item("archived-${campaign.id}") {
                            CampaignPanel(
                                campaign = campaign,
                                owner = campaign.ownerId == uid,
                                archived = true,
                                onAdd = {},
                                onInvite = {},
                                onArchive = { onArchiveCampaign(campaign, false) },
                                onLeave = {},
                                index = "AR",
                            )
                        }
                        items(
                            characters.filter { normalizeCampaignId(it.campaignId) == campaign.id },
                            key = { "archived:${campaign.id}:${it.id}" },
                        ) { character ->
                            CharacterAccessCard(
                                character = character,
                                master = master,
                                owner = ownersById[character.ownerId],
                                onOpen = { onOpen(character.id) },
                                onOwnerClick = { ownerTarget = character },
                            )
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
    }
}

@Composable
private fun CampaignPanel(
    campaign: Campaign,
    owner: Boolean,
    archived: Boolean,
    onAdd: () -> Unit,
    onInvite: () -> Unit,
    onArchive: () -> Unit,
    onLeave: () -> Unit,
    index: String,
) {
    TechPanel(accent = if (archived) TechCutDark else AcidCyan) {
        SectionHeader(index, campaign.name)
        if (campaign.description.isNotBlank()) Text(campaign.description, color = Muted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TelemetryTag(if (archived) "ARCHIVED" else "ACTIVE", if (archived) Muted else AcidCyan)
            TelemetryTag(if (owner) "MESTRE" else "MEMBRO")
        }
        if (!archived) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("+ FICHA") }
                if (owner) TextButton(onClick = onInvite, modifier = Modifier.weight(1f)) { Text("CONVITE") }
            }
        }
        if (owner) {
            TextButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                Text(if (archived) "RESTAURAR CAMPANHA" else "ARQUIVAR CAMPANHA")
            }
        } else if (!archived) {
            TextButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) { Text("SAIR DA CAMPANHA", color = Signal) }
        }
        Barcode(campaign.id)
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
                if (preview.campaign.description.isNotBlank()) Text(preview.campaign.description, color = Muted)
                Text("CONVITE // ${preview.invite.code}", color = Acid)
                if (preview.alreadyMember) {
                    Text("Você já participa desta campanha.", color = AcidCyan)
                } else {
                    Text("Escolha uma ficha sem campanha ou crie uma nova.", color = Ice)
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
    onOpen: () -> Unit,
    onOwnerClick: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().border(
            1.dp,
            when { character.isLocked -> Signal; character.dirty -> Acid; else -> TechCutDark },
            CutCornerShape(topEnd = 24.dp, bottomStart = 12.dp),
        ),
        shape = CutCornerShape(topEnd = 24.dp, bottomStart = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .96f)),
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
                    when { character.isLocked -> Signal; character.dirty -> Acid; else -> AcidCyan },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).background(Acid, CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)), contentAlignment = Alignment.Center) {
                    Text(character.name.take(2).uppercase(), color = Void, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.size(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(character.name.uppercase(), color = Ice, style = MaterialTheme.typography.titleLarge)
                    Text(
                        listOf(character.race, character.occupation, "LV.${character.level}").filter(String::isNotBlank).joinToString(" // "),
                        color = LabelFunctional,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    if (master) {
                        TextButton(onClick = onOwnerClick, contentPadding = PaddingValues(0.dp)) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("OWNER // ${owner?.firstName?.uppercase() ?: "SEM PERFIL"}", color = Acid, style = MaterialTheme.typography.labelSmall)
                                Text("UID.${character.ownerId.take(8)}", color = Muted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Icon(
                    when { character.isLocked -> Icons.Default.Lock; master -> Icons.Default.AdminPanelSettings; else -> Icons.Default.ChevronRight },
                    null,
                    tint = if (character.isLocked) Signal else Acid,
                )
            }
            Barcode(character.id)
        }
    }
}
