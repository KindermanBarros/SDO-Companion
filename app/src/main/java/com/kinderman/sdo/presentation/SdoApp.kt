package com.kinderman.sdo.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kinderman.sdo.MainActivity
import com.kinderman.sdo.SdoApplication
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.presentation.character.CharacterSheetScreen
import com.kinderman.sdo.presentation.dashboard.DashboardScreen
import com.kinderman.sdo.presentation.historian.HistorianDashboardScreen
import com.kinderman.sdo.presentation.journal.JournalScreen
import com.kinderman.sdo.presentation.journal.JournalViewModel
import com.kinderman.sdo.presentation.login.LoginScreen
import com.kinderman.sdo.presentation.session.SessionModeScreen
import com.kinderman.sdo.presentation.settings.SettingsScreen
import com.kinderman.sdo.presentation.sync.CharacterConflictDialog
import com.kinderman.sdo.ui.CyberLoadingMode
import com.kinderman.sdo.ui.CyberLoadingScreen
import com.kinderman.sdo.ui.LocalSdoUseNavigationRail
import com.kinderman.sdo.ui.SdoContentDensity
import com.kinderman.sdo.ui.SdoContentFrame
import com.kinderman.sdo.ui.SdoNavigationBar
import com.kinderman.sdo.ui.SdoNavigationItem
import com.kinderman.sdo.ui.SdoNavigationRail
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.SdoTextButton

private enum class AppSurface { DASHBOARD, SHEET, SESSION, HISTORIAN, JOURNAL, SETTINGS }

@Composable
fun SdoApp(
    activity: MainActivity,
    preferences: SdoPreferences,
    onPreferencesChange: (SdoPreferences) -> Unit,
) {
    val application = LocalContext.current.applicationContext as SdoApplication
    val appViewModel: AppViewModel = viewModel(
        factory = AppViewModelFactory(
            application.characterRepository,
            application.ownerRepository,
            application.catalogRepository,
            application.campaignRepository,
            application.operationsRepository,
            application.connectivityMonitor,
        ),
    )
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application.authRepository))
    val journalViewModel: JournalViewModel = viewModel(factory = JournalViewModelFactory(application.campaignJournalRepository))
    val authenticatedSession by authViewModel.session.collectAsStateWithLifecycle()
    val appSession by appViewModel.session.collectAsStateWithLifecycle()
    val characters by appViewModel.characters.collectAsStateWithLifecycle()
    val campaigns by appViewModel.campaigns.collectAsStateWithLifecycle()
    val memberships by appViewModel.memberships.collectAsStateWithLifecycle()
    val campaignInvites by appViewModel.campaignInvites.collectAsStateWithLifecycle()
    val campaignMembers by appViewModel.campaignMembers.collectAsStateWithLifecycle()
    val owners by appViewModel.owners.collectAsStateWithLifecycle()
    val characterLoadState by appViewModel.loadState.collectAsStateWithLifecycle()
    val offlineReadOnly by appViewModel.isOfflineReadOnly.collectAsStateWithLifecycle()
    val syncFailure by appViewModel.syncFailure.collectAsStateWithLifecycle()
    val conflicts by appViewModel.conflicts.collectAsStateWithLifecycle()
    val invitePreview by appViewModel.invitePreview.collectAsStateWithLifecycle()
    val message by appViewModel.message.collectAsStateWithLifecycle()
    val saveErrors by appViewModel.saveErrors.collectAsStateWithLifecycle()
    val catalog by appViewModel.catalog.collectAsStateWithLifecycle()
    val deliveries by appViewModel.deliveries.collectAsStateWithLifecycle()
    val journalState by journalViewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var demo by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedJournalCampaignId by rememberSaveable { mutableStateOf<String?>(null) }
    var surface by rememberSaveable { mutableStateOf(AppSurface.DASHBOARD) }

    LaunchedEffect(surface, appSession?.uid) {
        if (surface == AppSurface.HISTORIAN && appSession != null) {
            appViewModel.sync()
        }
    }

    LaunchedEffect(surface, selectedJournalCampaignId, appSession?.uid, campaigns, memberships) {
        if (appSession == null) {
            journalViewModel.clearContext()
            return@LaunchedEffect
        }
        if (surface == AppSurface.JOURNAL) {
            val selectedCampaign = campaigns.firstOrNull { it.id == selectedJournalCampaignId }
            val isActiveMember = selectedCampaign != null && memberships.any {
                it.campaignId == selectedCampaign.id && it.isActive
            }
            journalViewModel.bind(appSession, selectedCampaign, isActiveMember)
        }
    }

    var backStack by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    val screenState = rememberSaveableStateHolder()
    fun navigate(
        target: AppSurface,
        characterId: String? = selectedId,
        campaignId: String? = selectedJournalCampaignId,
    ) {
        if (target == surface && characterId == selectedId && campaignId == selectedJournalCampaignId) return
        backStack = ArrayList(backStack + "${surface.name}|${selectedId.orEmpty()}|${selectedJournalCampaignId.orEmpty()}")
        selectedId = characterId
        selectedJournalCampaignId = campaignId
        surface = target
    }
    fun goBack() {
        val previous = backStack.lastOrNull()
        backStack = ArrayList(backStack.dropLast(1))
        val parts = previous?.split('|').orEmpty()
        surface = parts.getOrNull(0)?.takeIf(String::isNotBlank)?.let(AppSurface::valueOf) ?: AppSurface.DASHBOARD
        selectedId = parts.getOrNull(1)?.takeIf(String::isNotBlank)
        selectedJournalCampaignId = parts.getOrNull(2)?.takeIf(String::isNotBlank)
    }

    LaunchedEffect(authenticatedSession, demo, preferences.autoSync) {
        appViewModel.setAutomaticSync(preferences.autoSync)
        when {
            demo -> appViewModel.setDemoSession()
            authenticatedSession != null -> appViewModel.setSession(authenticatedSession!!)
            else -> appViewModel.clearSession()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { appViewModel.onAppForegrounded() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        appViewModel.onAppBackgrounded(changingConfiguration = activity.isChangingConfigurations)
    }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            appViewModel.dismissMessage()
        }
    }

    when {
        authViewModel.state.initializing -> CyberLoadingScreen(CyberLoadingMode.AUTH_BOOT)

        authViewModel.state.loading -> CyberLoadingScreen(CyberLoadingMode.GOOGLE_AUTH)

        authenticatedSession == null && !demo -> SdoContentFrame(maxContentWidth = 640.dp) { LoginScreen(
            activity = activity,
            state = authViewModel.state,
            allowDemo = !authViewModel.configured,
            onGoogleLogin = { authViewModel.login(activity) },
            onDemo = { demo = true },
        ) }

        appSession == null ->
            CyberLoadingScreen(CyberLoadingMode.CHARACTERS)

        else -> {
            val selectedCharacter = characters.firstOrNull { it.id == selectedId }
            val selectedCampaignId = normalizeCampaignId(selectedCharacter?.campaignId.orEmpty())
            val archived = campaigns.firstOrNull { it.id == selectedCampaignId }?.isArchived == true
            val selectedCampaign = campaigns.firstOrNull { it.id == selectedCampaignId }
            val membership = memberships.firstOrNull { it.campaignId == selectedCampaignId }
            val isCampaignHistorian = appSession?.isAdmin == true || selectedCampaign?.ownerId == appSession?.uid
            val isCampaignResponsible = appSession?.isAdmin == true || selectedCampaign?.ownerId == appSession?.uid
            val wide = LocalSdoUseNavigationRail.current
            val canOpenHistorian = appSession?.isAdmin == true || campaigns.any { it.ownerId == appSession?.uid }
            Row(Modifier.fillMaxSize()) {
                if (wide) SdoNavigationRail(buildList {
                    add(SdoNavigationItem("Painel", surface == AppSurface.DASHBOARD, Icons.Default.Home) { navigate(AppSurface.DASHBOARD, null) })
                    add(SdoNavigationItem("Sessão", surface == AppSurface.SESSION, Icons.Default.PlayCircle) { navigate(AppSurface.SESSION, null) })
                    if (canOpenHistorian) add(SdoNavigationItem("Mestre", surface == AppSurface.HISTORIAN, Icons.Default.Visibility) { navigate(AppSurface.HISTORIAN) })
                    add(SdoNavigationItem("Ajustes", surface == AppSurface.SETTINGS, Icons.Default.Settings) { navigate(AppSurface.SETTINGS) })
                })
                Column(Modifier.weight(1f).fillMaxSize()) {
                SdoContentFrame(Modifier.weight(1f)) {
            screenState.SaveableStateProvider("${appSession?.uid}:${surface.name}") {
            when (surface) {
                AppSurface.DASHBOARD -> DashboardScreen(
                    characters = characters,
                    campaigns = campaigns,
                    memberships = memberships,
                    campaignInvites = campaignInvites,
                    campaignMembers = campaignMembers,
                    owners = owners,
                    session = appSession,
                    syncing = characterLoadState.syncing,
                    compactCards = preferences.compactCards,
                    collapseCampaignCards = preferences.collapseLongSections,
                    showArchivedCampaigns = preferences.showArchivedCampaigns,
                    invitePreview = invitePreview,
                    snackbarHost = { SnackbarHost(snackbar) },
                    onAdd = appViewModel::add,
                    onAddToCampaign = appViewModel::addToCampaign,
                    onLinkCharacter = appViewModel::linkCharacter,
                    onUnlinkCharacter = appViewModel::unlinkCharacter,
                    onOpen = {
                        navigate(AppSurface.SHEET, it)
                    },
                    onOwnerTransfer = appViewModel::transferOwner,
                    onCreateCampaign = appViewModel::createCampaign,
                    onArchiveCampaign = appViewModel::archiveCampaign,
                    onDeleteCampaign = appViewModel::deleteCampaign,
                    onLeaveCampaign = appViewModel::leaveCampaign,
                    onOpenJournal = { campaign ->
                        navigate(AppSurface.JOURNAL, campaignId = campaign.id)
                    },
                    onPreviewInvite = appViewModel::previewCampaignInvite,
                    onAcceptInvite = appViewModel::acceptCampaignInvite,
                    onDismissInvitePreview = appViewModel::dismissInvitePreview,
                    onSync = appViewModel::sync,
                    onOpenSession = {
                        navigate(AppSurface.SESSION, null)
                    },
                    onOpenHistorian = { navigate(AppSurface.HISTORIAN) },
                    onOpenSettings = { navigate(AppSurface.SETTINGS) },
                    deliveries = if (preferences.notifications) deliveries.filter { it.recipientId == appSession?.uid } else emptyList(),
                    onRespondDelivery = appViewModel::respondDelivery,
                    onLogout = {
                        selectedId = null
                        selectedJournalCampaignId = null
                        surface = AppSurface.DASHBOARD
                        backStack = arrayListOf()
                        demo = false
                        journalViewModel.clearContext()
                        authViewModel.logout()
                        appViewModel.clearSession()
                    },
                )

                AppSurface.SHEET -> CharacterSheetScreen(
                    character = selectedCharacter,
                    saveError = saveErrors[selectedCharacter?.id],
                    session = appSession,
                    catalog = catalog,
                    readOnly = archived || offlineReadOnly,
                    isCampaignHistorian = isCampaignHistorian,
                    isCampaignResponsible = isCampaignResponsible,
                    snackbarHost = { SnackbarHost(snackbar) },
                    onBack = { goBack() },
                    onClose = {
                        appViewModel.closeCharacter(it)
                        goBack()
                    },
                    onOpenSession = {
                        selectedCharacter?.let(appViewModel::closeCharacter)
                        navigate(AppSurface.SESSION, it)
                    },
                    onOpenJournal = selectedCharacter?.let { character ->
                        val cid = normalizeCampaignId(character.campaignId)
                        if (campaigns.any { it.id == cid }) {
                            {
                                appViewModel.closeCharacter(character)
                                navigate(AppSurface.JOURNAL, campaignId = cid)
                            }
                        } else {
                            null
                        }
                    },
                    onSave = appViewModel::save,
                    onAutosave = appViewModel::autosave,
                    onPlayerLock = appViewModel::setPlayerLocked,
                    onHistorianLock = appViewModel::setHistorianLocked,
                    onDelete = {
                        appViewModel.delete(it)
                        goBack()
                    },
                )

                AppSurface.SESSION -> SessionModeScreen(
                    characters = characters,
                    selectedId = selectedId,
                    compact = preferences.density == SdoContentDensity.COMPACT,
                    readOnly = archived || offlineReadOnly,
                    onSelect = { navigate(AppSurface.SESSION, it) },
                    onOpenSheet = {
                        navigate(AppSurface.SHEET, it)
                    },
                    canOpenJournal = { character ->
                        val cid = normalizeCampaignId(character.campaignId)
                        campaigns.any { it.id == cid }
                    },
                    onOpenJournal = { character ->
                        val cid = normalizeCampaignId(character.campaignId)
                        if (campaigns.any { it.id == cid }) {
                            navigate(AppSurface.JOURNAL, characterId = character.id, campaignId = cid)
                        }
                    },
                    onCommand = appViewModel::applySessionCommand,
                    onBack = { goBack() },
                )

                AppSurface.HISTORIAN -> {
                    val audit by appViewModel.audit.collectAsStateWithLifecycle()
                    val campaignLibrary by appViewModel.library.collectAsStateWithLifecycle()
                    val alertSettings by appViewModel.alertSettings.collectAsStateWithLifecycle()
                    HistorianDashboardScreen(
                    session = appSession!!,
                    campaigns = campaigns,
                    memberships = memberships,
                    characters = characters,
                    catalog = catalog,
                    audit = audit,
                    library = campaignLibrary,
                    deliveries = deliveries,
                    alertSettings = alertSettings,
                    onApplyCommand = appViewModel::applySessionCommand,
                    onSaveLibrary = appViewModel::saveLibrary,
                    onDuplicateLibrary = appViewModel::duplicateLibrary,
                    onArchiveLibrary = appViewModel::archiveLibrary,
                    onDeliverLibrary = appViewModel::deliverLibrary,
                    onSaveAlertSettings = appViewModel::saveAlertSettings,
                    onOpenSession = {
                        navigate(AppSurface.SESSION, it)
                    },
                    onOpenSheet = {
                        navigate(AppSurface.SHEET, it)
                    },
                    onBack = { goBack() },
                    )
                }

                AppSurface.JOURNAL -> JournalScreen(
                    state = journalState,
                    onRetry = journalViewModel::retryObserve,
                    onOpenEntry = journalViewModel::openEntry,
                    onNewEntry = journalViewModel::startNewEntry,
                    onEditEntry = journalViewModel::startEditEntry,
                    onUpdateDraft = journalViewModel::updateDraft,
                    onSaveDraft = journalViewModel::saveDraft,
                    onReloadConflict = journalViewModel::reloadConflict,
                    onRequestReplaceConflict = journalViewModel::requestReplaceConflict,
                    onDismissReplaceConflict = journalViewModel::dismissReplaceConflict,
                    onConfirmReplaceConflict = journalViewModel::confirmReplaceConflict,
                    onRequestDelete = journalViewModel::requestDelete,
                    onDismissDelete = journalViewModel::dismissDelete,
                    onConfirmDelete = journalViewModel::confirmDelete,
                    onBackPressed = {
                        journalViewModel.handleBack {
                            journalViewModel.clearContext()
                            goBack()
                        }
                    },
                    onDismissDiscard = journalViewModel::dismissDiscard,
                    onConfirmDiscard = journalViewModel::confirmDiscard,
                )

                AppSurface.SETTINGS -> SettingsScreen(
                    preferences = preferences,
                    onPreferencesChange = onPreferencesChange,
                    onBack = { goBack() },
                )
            }
                }
                }
                if (offlineReadOnly || syncFailure != null) {
                    SyncStatusBanner(
                        offlineReadOnly = offlineReadOnly,
                        syncFailure = syncFailure,
                        onRetry = appViewModel::sync,
                    )
                }
                if (!wide && surface != AppSurface.SHEET) SdoNavigationBar(buildList {
                    add(SdoNavigationItem("Painel", surface == AppSurface.DASHBOARD, Icons.Default.Home) { navigate(AppSurface.DASHBOARD, null) })
                    add(SdoNavigationItem("Sessão", surface == AppSurface.SESSION, Icons.Default.PlayCircle) { navigate(AppSurface.SESSION, null) })
                    if (canOpenHistorian) add(SdoNavigationItem("Mestre", surface == AppSurface.HISTORIAN, Icons.Default.Visibility) { navigate(AppSurface.HISTORIAN) })
                    add(SdoNavigationItem("Ajustes", surface == AppSurface.SETTINGS, Icons.Default.Settings) { navigate(AppSurface.SETTINGS) })
                })
            }
            }
        }
    }

    conflicts.firstOrNull()?.let { conflict ->
        CharacterConflictDialog(
            conflict = conflict,
            onResolve = { remoteFieldIds -> appViewModel.resolveConflict(conflict, remoteFieldIds) },
        )
    }
}

@Composable
private fun SyncStatusBanner(
    offlineReadOnly: Boolean,
    syncFailure: String?,
    onRetry: () -> Unit,
) {
    val containerColor = if (offlineReadOnly) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (offlineReadOnly) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val bannerText = if (offlineReadOnly) {
        "Modo offline · Somente leitura — reconecte para editar"
    } else {
        syncFailure.orEmpty()
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (offlineReadOnly) Icons.Default.CloudOff else Icons.Default.SyncProblem,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = bannerText,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            SdoTextButton(onClick = onRetry) {
                Text(if (offlineReadOnly) "Reconectar" else "Tentar")
            }
        }
    }
}
