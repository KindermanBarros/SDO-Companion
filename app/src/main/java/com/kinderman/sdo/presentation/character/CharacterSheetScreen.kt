package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.policy.CharacterAccessPolicy
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.AcidCyan
import com.kinderman.sdo.ui.Barcode
import com.kinderman.sdo.ui.ComplianceMark
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag
import com.kinderman.sdo.ui.Void

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    character: Character?,
    session: UserSession?,
    snackbarHost: @Composable () -> Unit,
    onBack: () -> Unit,
    onSave: (Character) -> Unit,
    onLock: (Character, Boolean) -> Unit,
    onDelete: (Character) -> Unit,
) {
    if (character == null || session == null) return
    var current by remember(character.id, character.updatedAt) { mutableStateOf(character) }
    var confirmDelete by remember { mutableStateOf(false) }
    val editable = CharacterAccessPolicy.canEdit(session, current)
    val canDelete = CharacterAccessPolicy.canDelete(session, current)

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("REMOVER PERSONAGEM") },
        text = { Text("A exclusão de ${current.name} será sincronizada com o Firebase e removida do cache local.") },
        confirmButton = {
            TextButton(onClick = { confirmDelete = false; onDelete(current) }) { Text("REMOVER", color = Signal) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("CANCELAR") } },
    )

    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(current.name.uppercase(), style = MaterialTheme.typography.titleMedium)
                            Text(
                                when { session.isMaster -> "HISTORIAN_OVERRIDE"; current.isLocked -> "PLAYER_FILE // READ_ONLY"; else -> "PLAYER_FILE // EDIT" },
                                color = if (current.isLocked) Signal else Acid,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                    navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                    actions = {
                        if (session.isMaster) IconButton({ onLock(current, !current.isLocked) }) {
                            Icon(if (current.isLocked) Icons.Default.LockOpen else Icons.Default.Lock, if (current.isLocked) "Destrancar" else "Trancar", tint = if (current.isLocked) Acid else Signal)
                        }
                        if (canDelete) IconButton({ confirmDelete = true }) { Icon(Icons.Default.DeleteForever, "Remover personagem", tint = Signal) }
                        if (editable) IconButton({ onSave(current) }) { Icon(Icons.Default.Save, "Salvar", tint = Acid) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Void, titleContentColor = Ice, navigationIconContentColor = Ice),
                )
            },
        ) { padding ->
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                item { SheetHero(current, session) }
                item { IdentitySection(current, editable) { current = it } }
                item { ResourceSection(current, editable) { current = it } }
                item { TraitSection(current, editable) { current = it } }
                item { AttributeSection(current, editable) { current = it } }
                item { SpecialKnowledgeSection(current, editable) { current = it } }
                item { ProtectionSection(current, editable) { current = it } }
                item { PathSection(current, editable) { current = it } }
                item { InventorySection(current, editable) { current = it } }
                item { BodySection(current, editable) { current = it } }
                item { OrganSection(current, editable) { current = it } }
                item { MysticSection(current, editable) { current = it } }
                item { ConditionSection(current, editable) { current = it } }
                item { NarrativeSection(current, editable) { current = it } }
                item {
                    Button(
                        onClick = { onSave(current) },
                        enabled = editable,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Acid, contentColor = Void),
                    ) {
                        Icon(Icons.Default.CloudUpload, null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (editable) "SALVAR // SINCRONIZAR" else "FICHA TRANCADA", style = MaterialTheme.typography.labelLarge)
                    }
                    Text("ROOM_LOCAL → FIRESTORE_REMOTE // FAILSAFE ATIVO", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun SheetHero(character: Character, session: UserSession) {
    TechPanel(accent = if (character.isLocked) Signal else Acid) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TelemetryTag(if (session.isMaster) "OVERRIDE.M" else "PROFILE.P")
            TelemetryTag(if (character.isLocked) "LOCKED" else "LV.${character.level}", if (character.isLocked) Signal else Acid)
        }
        Text("ARQUIVO", color = Acid, style = MaterialTheme.typography.labelLarge)
        Text(character.name.uppercase(), style = MaterialTheme.typography.headlineLarge, fontStyle = FontStyle.Italic, color = Ice)
        Barcode("${character.id}-${character.name}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ComplianceMark()
            Column(horizontalAlignment = Alignment.End) {
                Icon(if (character.isLocked) Icons.Default.Lock else Icons.Default.CloudDone, null, tint = if (character.isLocked) Signal else AcidCyan)
                Text(if (character.isLocked) "CONTROLE DO HISTORIADOR" else "CACHE PROTEGIDO", color = if (character.isLocked) Signal else AcidCyan, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
