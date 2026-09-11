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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.runtime.LaunchedEffect
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
    readOnly: Boolean = false,
    saveError: String? = null,
    isCampaignHistorian: Boolean = false,
    isCampaignResponsible: Boolean = false,
    showCalculationAudit: Boolean = false,
    snackbarHost: @Composable () -> Unit,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    onSave: (Character) -> Unit,
    onAutosave: (Character) -> Unit,
    onPlayerLock: (Character, Boolean) -> Unit,
    onHistorianLock: (Character, Boolean) -> Unit,
    onDelete: (Character) -> Unit,
) {
    BackHandler(onBack = onBack)
    if (character == null || session == null) {
        MissingCharacterState(sessionAvailable = session != null, snackbarHost = snackbarHost, onBack = onBack)
        return
    }
    var current by remember(character.id) { mutableStateOf(character) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(character.updatedAt) {
        if (shouldReplaceDraft(current.updatedAt, character.updatedAt)) current = character
    }
    val editable = !readOnly && CharacterAccessPolicy.canEdit(session, current, isCampaignHistorian)
    // Ownership survives campaign archive/locks: the owner can always remove their own sheet.
    val canDelete = CharacterAccessPolicy.canDelete(session, current, isCampaignHistorian)
    val canChangePlayerLock = !readOnly &&
        CharacterAccessPolicy.canChangePlayerLock(session, current, isCampaignHistorian)

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("REMOVER PERSONAGEM") },
        text = { Text("A exclusão de ${current.name} será sincronizada com o Firebase e removida do cache local.") },
        confirmButton = {
            TextButton(onClick = { confirmDelete = false; onDelete(current) }) { Text("REMOVER", color = MaterialTheme.colorScheme.error) }
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
                                    readOnly -> "Campanha arquivada · somente leitura"
                                    session.isAdmin -> "Acesso administrativo"
                                    isCampaignHistorian -> "Acesso da Mestre"
                                    current.lockType == CharacterLock.HISTORIAN -> "Ficha bloqueada pela Mestre"
                                    current.lockType == CharacterLock.PLAYER -> "Ficha com bloqueio pessoal"
                                    else -> "Ficha editável"
                                },
                                color = if (readOnly || current.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                    navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                    actions = {
                        IconButton({ onOpenSession(current.id) }) {
                            Icon(Icons.Default.PlayCircle, "Abrir modo sessão", tint = MaterialTheme.colorScheme.secondary)
                        }
                        if (!readOnly && CharacterAccessPolicy.canChangeHistorianLock(session, isCampaignHistorian)) IconButton({
                            onHistorianLock(current, current.lockType != CharacterLock.HISTORIAN)
                        }) {
                            Icon(
                                if (current.lockType == CharacterLock.HISTORIAN) Icons.Default.LockOpen else Icons.Default.Lock,
                                if (current.lockType == CharacterLock.HISTORIAN) "Remover bloqueio do historiador" else "Aplicar bloqueio do historiador",
                                tint = if (current.lockType == CharacterLock.HISTORIAN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        } else if (canChangePlayerLock) IconButton({
                            onPlayerLock(current, current.lockType != CharacterLock.PLAYER)
                        }) {
                            Icon(
                                if (current.lockType == CharacterLock.PLAYER) Icons.Default.LockOpen else Icons.Default.Lock,
                                if (current.lockType == CharacterLock.PLAYER) "Remover meu bloqueio" else "Impedir que eu apague esta ficha",
                                tint = if (current.lockType == CharacterLock.PLAYER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        }
                        if (canDelete) IconButton({ confirmDelete = true }) { Icon(Icons.Default.DeleteForever, "Remover personagem", tint = MaterialTheme.colorScheme.error) }
                        if (editable) IconButton({ onSave(current) }) { Icon(Icons.Default.Save, "Salvar", tint = MaterialTheme.colorScheme.primary) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
        ) { padding ->
            val changeCharacter: (Character) -> Unit = {
                if (!readOnly) {
                    val draft = it.copy(updatedAt = maxOf(System.currentTimeMillis(), current.updatedAt + 1), dirty = true)
                    current = draft
                    onAutosave(draft)
                }
            }
            if (current.isInCreation) CharacterCreationWizard(
                character = current,
                catalog = catalog,
                enabled = editable,
                onChange = changeCharacter,
            ) else CharacterSheetPager(
                character = current,
                saveError = saveError,
                session = session,
                catalog = catalog,
                editable = editable,
                showCalculationAudit = showCalculationAudit,
                onChange = changeCharacter,
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        }
    }
}

internal fun shouldReplaceDraft(currentUpdatedAt: Long, incomingUpdatedAt: Long): Boolean =
    incomingUpdatedAt > currentUpdatedAt

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MissingCharacterState(
    sessionAvailable: Boolean,
    snackbarHost: @Composable () -> Unit,
    onBack: () -> Unit,
) {
    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost,
            topBar = {
                TopAppBar(
                    title = { Text("FICHA INDISPONÍVEL") },
                    navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar ao painel") } },
                )
            },
        ) { padding ->
            Column(
                Modifier.padding(padding).padding(24.dp).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                Text(if (sessionAvailable) "Esta ficha não existe mais ou ainda não foi sincronizada." else "A sessão não está disponível.")
                TextButton(onClick = onBack) { Text("VOLTAR AO PAINEL") }
            }
        }
    }
}

@Composable
internal fun SheetHero(character: Character, session: UserSession, saveError: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    TechPanel(accent = if (character.isLocked) Signal else Acid) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TelemetryTag(if (session.isAdmin) "OVERRIDE.ADMIN" else "ACCOUNT")
            TelemetryTag(
                when (character.lockType) {
                    CharacterLock.HISTORIAN -> "LOCK.H"
                    CharacterLock.PLAYER -> "LOCK.P"
                    CharacterLock.NONE -> "LV.${character.level}"
                },
                if (character.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
        Text("ARQUIVO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text(
            character.name.uppercase(),
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = TechInterfaceFont,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleLarge,
        )
        Barcode("${character.id}-${character.name}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ComplianceMark()
            IconButton(onClick = {
                android.widget.Toast.makeText(context,
                    saveError ?: if (character.dirty) "Alterações salvas no aparelho; sincronização pendente"
                    else "Alterações salvas",
                    android.widget.Toast.LENGTH_SHORT).show()
            }) {
                Icon(if (saveError == null) Icons.Default.CloudDone else Icons.Default.ErrorOutline,
                    saveError ?: "Estado de salvamento", tint = if (saveError == null) Acid else Signal)
            }
        }
    }
}
