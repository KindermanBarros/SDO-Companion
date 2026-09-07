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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.AcidCyan
import com.kinderman.sdo.ui.Barcode
import com.kinderman.sdo.ui.HudBackground
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
    session: UserSession?,
    snackbarHost: @Composable () -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onSync: () -> Unit,
    onLogout: () -> Unit,
) {
    val master = session?.isMaster == true
    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost,
            floatingActionButton = {
                FloatingActionButton(onClick = onAdd, containerColor = Acid, contentColor = Void, shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp)) {
                    Icon(Icons.Default.Add, "Criar personagem")
                }
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
                            IconButton(onSync) { Icon(Icons.Default.Sync, "Sincronizar", tint = Acid) }
                            IconButton(onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, "Sair", tint = Signal) }
                        }
                    }
                    Text("SDO", color = Acid, style = MaterialTheme.typography.labelLarge)
                    Text(if (master) "PAINEL DA MESTRE" else "ARQUIVOS DE CAMPO", style = MaterialTheme.typography.headlineLarge, color = Ice)
                    Text("LOCAL_CACHE // FIREBASE_SYNC // ${characters.size.toString().padStart(2, '0')} REGISTROS", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
                if (characters.isEmpty()) item {
                    TechPanel(accent = Signal) {
                        SectionHeader("00", "Nenhum sinal detectado")
                        Text("Crie o primeiro personagem no comando +.")
                        Barcode("EMPTY-SDO-ARCHIVE")
                    }
                }
                items(characters, key = Character::id) { character ->
                    CharacterAccessCard(character, master) { onOpen(character.id) }
                }
            }
        }
    }
}

@Composable
private fun CharacterAccessCard(character: Character, master: Boolean, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().border(1.dp, when { character.isLocked -> Signal; character.dirty -> Acid; else -> TechCutDark }, CutCornerShape(topEnd = 24.dp, bottomStart = 12.dp)),
        shape = CutCornerShape(topEnd = 24.dp, bottomStart = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .96f)),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TelemetryTag("ID.${character.id.take(6)}")
                TelemetryTag(
                    when { character.isLocked -> "LOCKED"; character.dirty -> "LOCAL_DELTA"; else -> "SYNC_OK" },
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
                    Text(listOf(character.race, character.occupation, "LV.${character.level}").filter(String::isNotBlank).joinToString(" // "), color = LabelFunctional, style = MaterialTheme.typography.labelSmall)
                    if (master) Text("OWNER.${character.ownerId.take(8)}", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
                Icon(when { character.isLocked -> Icons.Default.Lock; master -> Icons.Default.AdminPanelSettings; else -> Icons.Default.ChevronRight }, null, tint = if (character.isLocked) Signal else Acid)
            }
            Barcode(character.id)
        }
    }
}
