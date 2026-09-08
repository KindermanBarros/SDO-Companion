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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignMember
import com.kinderman.sdo.domain.model.CampaignRole
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserSession
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
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HistorianDashboardScreen(
    session: UserSession,
    campaigns: List<Campaign>,
    memberships: List<CampaignMember>,
    characters: List<Character>,
    catalog: List<CatalogEntry>,
    onOpenSession: (String) -> Unit,
    onOpenSheet: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var section by remember { mutableStateOf(HistorianSection.OPERATION) }
    var search by remember { mutableStateOf("") }
    val historianCampaignIds = remember(session, campaigns, memberships) {
        campaigns.filter { campaign ->
            session.isAdmin || campaign.ownerId == session.uid || memberships.any {
                it.campaignId == campaign.id && it.userId == session.uid && it.role == CampaignRole.HISTORIAN && it.isActive
            }
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
                            ) { Text(target.label, color = if (section == target) Acid else Muted) }
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
                                }
                            }
                            items(campaignCharacters, key = { "operative:${campaign.id}:${it.id}" }) { character ->
                                OperationalCharacterCard(character, onOpenSession, onOpenSheet)
                            }
                        }
                    }
                    HistorianSection.LIBRARY -> {
                        item {
                            TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                                TelemetryTag("LOCAL.CATALOG")
                                Text("BIBLIOTECA DE REFERÊNCIA", color = Ice, style = MaterialTheme.typography.titleLarge)
                                Text("Consulta o catálogo embarcado. Distribuição e auditoria serão conectadas na etapa de domínio da issue #18.", color = Muted)
                                HudTextField(
                                    label = "Buscar nome, grupo ou regra",
                                    value = search,
                                    modifier = Modifier.fillMaxWidth(),
                                    onValue = { search = it },
                                )
                            }
                        }
                        val filtered = catalog.filter { search.isBlank() || it.searchableText().contains(search.trim(), true) }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${filtered.size} ENTRADAS", color = Muted, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    CatalogKind.entries.joinToString(" · ") { kind -> "${kind.name}:${filtered.count { it.kind == kind }}" },
                                    color = Muted,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        items(filtered.take(120), key = { "catalog:${it.id}" }) { entry -> CatalogReferenceCard(entry) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationalCharacterCard(
    character: Character,
    onOpenSession: (String) -> Unit,
    onOpenSheet: (String) -> Unit,
) {
    val alerts = buildList {
        if (character.lifeMaximum > 0 && character.life.current * 4 <= character.lifeMaximum) add("VIDA CRÍTICA")
        if (character.sanityMaximum > 0 && character.sanity.current * 4 <= character.sanityMaximum) add("SANIDADE CRÍTICA")
        if (character.exhaustion.maximum > 0 && character.exhaustion.current * 4 >= character.exhaustion.maximum * 3) add("EXAUSTÃO ALTA")
        if (character.conditions.isNotEmpty()) add("${character.conditions.size} CONDIÇÃO(ÕES)")
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
