package com.kinderman.sdo.presentation.character

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.CatalogEntry
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
import com.kinderman.sdo.ui.TechInterfaceFont
import com.kinderman.sdo.ui.TelemetryTag
import com.kinderman.sdo.ui.Void

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    character: Character?,
    session: UserSession?,
    catalog: List<CatalogEntry>,
    snackbarHost: @Composable () -> Unit,
    onBack: () -> Unit,
    onSave: (Character) -> Unit,
    onPlayerLock: (Character, Boolean) -> Unit,
    onHistorianLock: (Character, Boolean) -> Unit,
    onDelete: (Character) -> Unit,
) {
    if (character == null || session == null) return
    var current by remember(character.id, character.updatedAt) { mutableStateOf(character) }
    var confirmDelete by remember { mutableStateOf(false) }
    val editable = CharacterAccessPolicy.canEdit(session, current)
    val canDelete = CharacterAccessPolicy.canDelete(session, current)
    val canChangePlayerLock = CharacterAccessPolicy.canChangePlayerLock(session, current)

    BackHandler(onBack = onBack)

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
                                when {
                                    session.isMaster -> "HISTORIAN_OVERRIDE"
                                    current.lockType == CharacterLock.HISTORIAN -> "PLAYER_FILE // HISTORIAN_LOCK"
                                    current.lockType == CharacterLock.PLAYER -> "PLAYER_FILE // PERSONAL_LOCK"
                                    else -> "PLAYER_FILE // EDIT"
                                },
                                color = if (current.isLocked) Signal else Acid,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                    navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                    actions = {
                        if (session.isMaster) IconButton({
                            onHistorianLock(current, current.lockType != CharacterLock.HISTORIAN)
                        }) {
                            Icon(
                                if (current.lockType == CharacterLock.HISTORIAN) Icons.Default.LockOpen else Icons.Default.Lock,
                                if (current.lockType == CharacterLock.HISTORIAN) "Remover bloqueio do historiador" else "Aplicar bloqueio do historiador",
                                tint = if (current.lockType == CharacterLock.HISTORIAN) Acid else Signal,
                            )
                        } else if (canChangePlayerLock) IconButton({
                            onPlayerLock(current, current.lockType != CharacterLock.PLAYER)
                        }) {
                            Icon(
                                if (current.lockType == CharacterLock.PLAYER) Icons.Default.LockOpen else Icons.Default.Lock,
                                if (current.lockType == CharacterLock.PLAYER) "Remover meu bloqueio" else "Impedir que eu apague esta ficha",
                                tint = if (current.lockType == CharacterLock.PLAYER) Acid else Signal,
                            )
                        }
                        if (canDelete) IconButton({ confirmDelete = true }) { Icon(Icons.Default.DeleteForever, "Remover personagem", tint = Signal) }
                        if (editable) IconButton({ onSave(current) }) { Icon(Icons.Default.Save, "Salvar", tint = Acid) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Void, titleContentColor = Ice, navigationIconContentColor = Ice),
                )
            },
        ) { padding ->
            CharacterSheetPager(
                character = current,
                session = session,
                catalog = catalog,
                editable = editable,
                onChange = { current = it },
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun SheetHero(character: Character, session: UserSession) {
    TechPanel(accent = if (character.isLocked) Signal else Acid) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TelemetryTag(if (session.isMaster) "OVERRIDE.M" else "PROFILE.P")
            TelemetryTag(
                when (character.lockType) {
                    CharacterLock.HISTORIAN -> "LOCK.H"
                    CharacterLock.PLAYER -> "LOCK.P"
                    CharacterLock.NONE -> "LV.${character.level}"
                },
                if (character.isLocked) Signal else Acid,
            )
        }
        Text("ARQUIVO", color = Acid, style = MaterialTheme.typography.labelLarge)
        Text(
            character.name.uppercase(),
            color = Ice,
            fontFamily = TechInterfaceFont,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleLarge,
        )
        Barcode("${character.id}-${character.name}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ComplianceMark()
            Column(horizontalAlignment = Alignment.End) {
                Icon(if (character.isLocked) Icons.Default.Lock else Icons.Default.CloudDone, null, tint = if (character.isLocked) Signal else AcidCyan)
                Text(
                    when (character.lockType) {
                        CharacterLock.HISTORIAN -> "EXCLUSÃO: SOMENTE HISTORIADOR"
                        CharacterLock.PLAYER -> "EXCLUSÃO: BLOQUEIO PESSOAL"
                        CharacterLock.NONE -> "CACHE PROTEGIDO"
                    },
                    color = if (character.isLocked) Signal else AcidCyan,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
