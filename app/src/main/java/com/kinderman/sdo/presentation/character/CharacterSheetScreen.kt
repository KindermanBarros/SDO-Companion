package com.kinderman.sdo.presentation.character

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import com.kinderman.sdo.ui.SdoIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import com.kinderman.sdo.ui.SdoTextButton
import com.kinderman.sdo.ui.SdoScreenMasthead
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.creation.CharacterCreation
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
import kotlinx.coroutines.launch

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
    snackbarHost: @Composable () -> Unit,
    onBack: () -> Unit,
    onClose: (Character) -> Unit,
    onOpenSession: (String) -> Unit,
    onSave: (Character) -> Unit,
    onAutosave: (Character) -> Unit,
    onPlayerLock: (Character, Boolean) -> Unit,
    onHistorianLock: (Character, Boolean) -> Unit,
    onDelete: (Character) -> Unit,
) {
    if (character == null || session == null) {
        MissingCharacterState(sessionAvailable = session != null, snackbarHost = snackbarHost, onBack = onBack)
        return
    }
    var current by remember(character.id) { mutableStateOf(character) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmHeritageReset by remember { mutableStateOf(false) }
    val undoSnackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(character.updatedAt) {
        if (shouldReplaceDraft(current.updatedAt, character.updatedAt)) current = character
    }
    val editable = !readOnly && CharacterAccessPolicy.canEdit(session, current, isCampaignHistorian)
    // Ownership survives campaign archive/locks: the owner can always remove their own sheet.
    val canDelete = CharacterAccessPolicy.canDelete(session, current, isCampaignHistorian)
    val canChangePlayerLock = !readOnly &&
        CharacterAccessPolicy.canChangePlayerLock(session, current, isCampaignHistorian)
    val canLeaveCreation = !current.isInCreation || CharacterCreation.validateStep(1, current).isEmpty()
    BackHandler { if (canLeaveCreation) onClose(current) }

    val changeCharacter: (Character) -> Unit = { changed ->
        if (!readOnly) {
            val before = current
            val removedItems = before.inventory.filter { previous -> changed.inventory.none { it.id == previous.id } }
            val draft = changed.copy(updatedAt = maxOf(System.currentTimeMillis(), before.updatedAt + 1), dirty = true)
            current = draft
            onAutosave(draft)
            if (removedItems.isNotEmpty()) scope.launch {
                val result = undoSnackbar.showSnackbar(
                    message = if (removedItems.size == 1) "${removedItems.single().name.ifBlank { "Item" }} removido" else "Itens removidos",
                    actionLabel = "DESFAZER",
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    val latest = current
                    val restored = if (latest.updatedAt == draft.updatedAt) {
                        before
                    } else {
                        latest.copy(inventory = (latest.inventory + removedItems).distinctBy { it.id })
                    }.copy(updatedAt = maxOf(System.currentTimeMillis(), latest.updatedAt + 1), dirty = true)
                    current = restored
                    onAutosave(restored)
                }
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("REMOVER PERSONAGEM") },
        text = { Text("A exclusão de ${current.name} será sincronizada com o Firebase e removida do cache local.") },
        confirmButton = {
            SdoTextButton(onClick = { confirmDelete = false; onDelete(current) }) { Text("REMOVER", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { SdoTextButton(onClick = { confirmDelete = false }) { Text("CANCELAR") } },
    )

    if (confirmHeritageReset) AlertDialog(
        onDismissRequest = { confirmHeritageReset = false },
        title = { Text("REABRIR 30 PH") },
        text = { Text("Os itens escolhidos anteriormente com Pontos de Herança serão removidos. A ficha voltará somente às etapas de Equipamento inicial e Revisão com 30 PH disponíveis.") },
        confirmButton = {
            SdoTextButton(onClick = {
                confirmHeritageReset = false
                val reopened = CharacterCreation.reopenHeritage(current).copy(
                    updatedAt = maxOf(System.currentTimeMillis(), current.updatedAt + 1),
                    dirty = true,
                )
                current = reopened
                onAutosave(reopened)
            }) { Text("REABRIR HERANÇA", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { SdoTextButton(onClick = { confirmHeritageReset = false }) { Text("CANCELAR") } },
    )

    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                Box {
                    snackbarHost()
                    SnackbarHost(undoSnackbar)
                }
            },
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
                    navigationIcon = { SdoIconButton(onClick = { onClose(current) }, enabled = canLeaveCreation) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                    actions = {
                        SdoIconButton({ onOpenSession(current.id) }) {
                            Icon(Icons.Default.PlayCircle, "Abrir modo sessão", tint = MaterialTheme.colorScheme.secondary)
                        }
                        if (session.isAdmin && !readOnly && !current.isInCreation) SdoIconButton({ confirmHeritageReset = true }) {
                            Icon(Icons.Default.Refresh, "Reabrir escolha secreta de 30 PH", tint = MaterialTheme.colorScheme.secondary)
                        }
                        if (!readOnly && CharacterAccessPolicy.canChangeHistorianLock(session, isCampaignHistorian)) SdoIconButton({
                            onHistorianLock(current, current.lockType != CharacterLock.HISTORIAN)
                        }) {
                            Icon(
                                if (current.lockType == CharacterLock.HISTORIAN) Icons.Default.LockOpen else Icons.Default.Lock,
                                if (current.lockType == CharacterLock.HISTORIAN) "Remover bloqueio do historiador" else "Aplicar bloqueio do historiador",
                                tint = if (current.lockType == CharacterLock.HISTORIAN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        } else if (canChangePlayerLock) SdoIconButton({
                            onPlayerLock(current, current.lockType != CharacterLock.PLAYER)
                        }) {
                            Icon(
                                if (current.lockType == CharacterLock.PLAYER) Icons.Default.LockOpen else Icons.Default.Lock,
                                if (current.lockType == CharacterLock.PLAYER) "Remover meu bloqueio" else "Impedir que eu apague esta ficha",
                                tint = if (current.lockType == CharacterLock.PLAYER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        }
                        if (canDelete) SdoIconButton({ confirmDelete = true }) { Icon(Icons.Default.DeleteForever, "Remover personagem", tint = MaterialTheme.colorScheme.error) }
                        if (editable) SdoIconButton({ onSave(current) }) { Icon(Icons.Default.Save, "Salvar", tint = MaterialTheme.colorScheme.primary) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
        ) { padding ->
            if (current.isInCreation) CharacterCreationWizard(
                character = current,
                catalog = catalog,
                enabled = editable,
                onChange = changeCharacter,
                modifier = Modifier.padding(padding),
            ) else CharacterSheetPager(
                character = current,
                saveError = saveError,
                session = session,
                catalog = catalog,
                editable = editable,
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
                    navigationIcon = { SdoIconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar ao painel") } },
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
                SdoTextButton(onClick = onBack) { Text("VOLTAR AO PAINEL") }
            }
        }
    }
}

@Composable
internal fun SheetHero(character: Character, session: UserSession, saveError: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    SdoScreenMasthead(
        eyebrow = "CHARACTER//ARCHIVE",
        title = character.name.ifBlank { "FICHA SEM NOME" }.uppercase(),
        metadata = "NÍVEL ${character.level} // ${if (character.dirty) "ALTERAÇÕES LOCAIS" else "ARQUIVO ESTÁVEL"}",
    )
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
            SdoIconButton(onClick = {
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
