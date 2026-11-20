package com.kinderman.sdo.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kinderman.sdo.MainActivity
import com.kinderman.sdo.R
import com.kinderman.sdo.SdoApplication
import com.kinderman.sdo.domain.model.ConflictResolutionStatus
import com.kinderman.sdo.domain.model.SheetIntentStatus
import com.kinderman.sdo.domain.model.SheetSyncStatus
import com.kinderman.sdo.domain.model.allCanonicalAbilitiesSafely
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.presentation.character.CharacterSheetScreen
import com.kinderman.sdo.presentation.dashboard.DashboardScreen
import com.kinderman.sdo.presentation.historian.HistorianDashboardScreen
import com.kinderman.sdo.presentation.journal.JournalScreen
import com.kinderman.sdo.presentation.journal.JournalViewModel
import com.kinderman.sdo.presentation.login.LoginScreen
import com.kinderman.sdo.presentation.session.SessionModeScreen
import com.kinderman.sdo.presentation.settings.SettingsScreen
import com.kinderman.sdo.presentation.sync.AccessDeniedDraftDialog
import com.kinderman.sdo.presentation.sync.CharacterConflictDialog
import com.kinderman.sdo.ui.CyberLoadingMode
import com.kinderman.sdo.ui.CyberLoadingScreen
import com.kinderman.sdo.ui.LocalSdoUseNavigationRail
import com.kinderman.sdo.ui.SdoContentDensity
import com.kinderman.sdo.ui.SdoContentFrame
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.SdoNavigationBar
import com.kinderman.sdo.ui.SdoNavigationItem
import com.kinderman.sdo.ui.SdoNavigationRail
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.SdoTextButton
import kotlinx.coroutines.delay

private enum class AppSurface { DASHBOARD, SHEET, SESSION, HISTORIAN, JOURNAL, LIBRARY, SETTINGS }

@Composable
fun SdoApp(
    activity: MainActivity,
    preferences: SdoPreferences,
    onPreferencesChange: (SdoPreferences) -> Unit,
) {
    val appViewModel: AppViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val journalViewModel: JournalViewModel = viewModel()
    val authenticatedSession by authViewModel.session.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
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
    val sheetSyncStatus by appViewModel.sheetSyncStatus.collectAsStateWithLifecycle()
    val sheetIntents by appViewModel.sheetIntents.collectAsStateWithLifecycle()
    val sheetConflicts by appViewModel.sheetConflicts.collectAsStateWithLifecycle()
    val offlineEditableIds by appViewModel.offlineEditableIds.collectAsStateWithLifecycle()
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
    var reviewingAccessDenied by rememberSaveable { mutableStateOf(false) }
    var lastSeenConfirmedCount by rememberSaveable(appSession?.uid) { mutableStateOf<Int?>(null) }
    var showConfirmedBanner by rememberSaveable(appSession?.uid) { mutableStateOf(false) }

    val openSheetConflicts = remember(sheetConflicts) {
        sheetConflicts.filter { it.resolutionStatus == ConflictResolutionStatus.OPEN }
    }
    val accessDeniedIntents = remember(sheetIntents) {
        sheetIntents.filter { it.status == SheetIntentStatus.ACCESS_DENIED }
    }

    val activeSheetIds = remember(characters) { characters.asSequence().filterNot { it.deleted }.map { it.id }.toSet() }
    val activeSheetConflict = openSheetConflicts.firstOrNull { it.sheetId in activeSheetIds }
    val activeLegacyConflict = conflicts.firstOrNull { it.local.id in activeSheetIds }
    val activeDeniedIntent = accessDeniedIntents.firstOrNull { it.sheetId in activeSheetIds }

    LaunchedEffect(activeDeniedIntent) {
        if (activeDeniedIntent == null) reviewingAccessDenied = false
    }

    LaunchedEffect(activeSheetIds, selectedId) {
        if (selectedId != null && selectedId !in activeSheetIds) {
            selectedId = null
        }
    }

    val isSyncingNow = characterLoadState.syncing ||
        characterLoadState.cloudSyncing ||
        sheetSyncStatus.isSyncing ||
        sheetSyncStatus.sendingCount > 0 ||
        sheetSyncStatus.pendingCount > 0
    var justUpdated by rememberSaveable(appSession?.uid) { mutableStateOf(false) }
    var wasSyncing by remember { mutableStateOf(false) }
    LaunchedEffect(isSyncingNow, sheetSyncStatus.confirmedCount, offlineReadOnly, syncFailure) {
        val previous = lastSeenConfirmedCount
        lastSeenConfirmedCount = sheetSyncStatus.confirmedCount
        val confirmedIncreased = previous != null && sheetSyncStatus.confirmedCount > previous
        val finishedSync = wasSyncing && !isSyncingNow
        wasSyncing = isSyncingNow
        showConfirmedBanner = false
        if ((finishedSync || confirmedIncreased) && !isSyncingNow && !offlineReadOnly && syncFailure == null) {
            justUpdated = true
            delay(2_000L)
            justUpdated = false
        } else if (isSyncingNow || offlineReadOnly || syncFailure != null) {
            justUpdated = false
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
    LaunchedEffect(surface, selectedId, appSession?.uid) {
        appViewModel.setSheetPresenceActive(
            active = surface == AppSurface.SHEET && appSession != null,
            characterId = selectedId,
        )
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        appViewModel.onAppForegrounded()
        appViewModel.setSheetPresenceActive(
            active = surface == AppSurface.SHEET && appSession != null,
            characterId = selectedId,
        )
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        if (surface == AppSurface.SHEET) {
            selectedId?.let { appViewModel.setFieldPresence(it, null, false) }
        }
        appViewModel.setSheetPresenceActive(false)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        appViewModel.onAppBackgrounded(changingConfiguration = activity.isChangingConfigurations)
    }
    LaunchedEffect(surface) {
        snackbar.currentSnackbarData?.dismiss()
    }
    LaunchedEffect(message) {
        message?.let {
            try {
                snackbar.showSnackbar(it)
            } finally {
                appViewModel.dismissMessage()
            }
        }
    }

    when {
        authState.initializing -> CyberLoadingScreen(CyberLoadingMode.AUTH_BOOT)

        authState.loading -> CyberLoadingScreen(CyberLoadingMode.GOOGLE_AUTH)

        authenticatedSession == null && !demo -> SdoContentFrame(maxContentWidth = 640.dp) { LoginScreen(
            activity = activity,
            state = authState,
            allowDemo = !authViewModel.configured,
            onGoogleLogin = { authViewModel.login(activity) },
            onDemo = { demo = true },
            onDevEmailLogin = if (com.kinderman.sdo.BuildConfig.IS_DEV_ENV) {
                { email, password -> authViewModel.loginWithEmailPassword(email, password) }
            } else {
                null
            },
        ) }

        appSession == null ->
            CyberLoadingScreen(CyberLoadingMode.CHARACTERS)

        else -> {
            val selectedCharacter = characters.firstOrNull { it.id == selectedId }
            val activeSheetId = selectedId?.takeIf { surface == AppSurface.SHEET && appSession != null }.orEmpty()
            DisposableEffect(activeSheetId, appSession?.uid) {
                onDispose {
                    if (activeSheetId.isNotBlank()) {
                        appViewModel.setFieldPresence(activeSheetId, null, false)
                    }
                }
            }
            val presenceFlow = remember(activeSheetId, appSession?.uid) {
                appViewModel.observeFieldPresence(activeSheetId)
            }
            val fieldPresence by presenceFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
            val sheetReadOnlyOffline = offlineReadOnly && selectedId !in offlineEditableIds
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
                    add(SdoNavigationItem("Biblioteca", surface == AppSurface.LIBRARY, Icons.AutoMirrored.Filled.MenuBook) { navigate(AppSurface.LIBRARY) })
                })
                Column(Modifier.weight(1f).fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    com.kinderman.sdo.ui.SdoGlobalSyncStatusPill(
                        syncing = isSyncingNow,
                        justUpdated = justUpdated,
                        offline = offlineReadOnly,
                        hasError = sheetSyncStatus.hasActionRequired || syncFailure != null,
                        onClick = {
                            if (activeDeniedIntent != null) reviewingAccessDenied = true else appViewModel.sync()
                        },
                    )
                }
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
                    cloudSyncing = characterLoadState.cloudSyncing,
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
                    fieldPresence = fieldPresence,
                    onFieldFocus = { field, focused ->
                        if (surface == AppSurface.SHEET || !focused) {
                            selectedId?.let { appViewModel.setFieldPresence(it, field, focused) }
                        }
                    },
                    session = appSession,
                    catalog = catalog,
                    readOnly = (archived && appSession?.isAdmin != true) || sheetReadOnlyOffline,
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
                    onDelete = { deletedChar ->
                        val deletedId = deletedChar.id
                        appViewModel.delete(deletedChar)
                        backStack = ArrayList(
                            backStack.map { entry ->
                                val parts = entry.split('|')
                                if (parts.getOrNull(1) == deletedId) {
                                    "${parts.getOrNull(0).orEmpty()}||${parts.getOrNull(2).orEmpty()}"
                                } else {
                                    entry
                                }
                            },
                        )
                        goBack()
                        if (selectedId == deletedId) selectedId = null
                    },
                )

                AppSurface.SESSION -> SessionModeScreen(
                    characters = characters,
                    selectedId = selectedId,
                    compact = preferences.density == SdoContentDensity.COMPACT,
                    readOnly = (archived && appSession?.isAdmin != true) || sheetReadOnlyOffline,
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
                    onInventoryStateChange = appViewModel::changeItemState,
                    onToggleAbilityFavorite = { targetCharacter, abilityId ->
                        appViewModel.autosave(
                            targetCharacter.copy(
                                abilities = targetCharacter.allCanonicalAbilitiesSafely().map { ability ->
                                    if (ability.id == abilityId) ability.copy(favorite = !ability.favorite) else ability
                                },
                            ),
                        )
                    },
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
                    readOnly = offlineReadOnly,
                    offlineEditableIds = offlineEditableIds,
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

                AppSurface.LIBRARY -> {
                    val libraryViewModel: com.kinderman.sdo.presentation.library.LibraryViewModel = viewModel()
                    val libraryCampaignIds = remember(campaigns) {
                        campaigns.filterNot { it.isDeleted }.mapTo(linkedSetOf()) { it.id }
                    }
                    LaunchedEffect(libraryCampaignIds) {
                        libraryViewModel.setAccessibleCampaignIds(libraryCampaignIds)
                    }
                    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()
                    com.kinderman.sdo.presentation.library.LibraryScreen(
                        campaigns = campaigns,
                        characters = characters,
                        catalog = libraryState.catalog,
                        campaignLibrary = libraryState.campaignLibrary,
                        audit = libraryState.audit,
                        onBack = { goBack() },
                    )
                }

                AppSurface.SETTINGS -> SettingsScreen(
                    preferences = preferences,
                    onPreferencesChange = onPreferencesChange,
                    onBack = { goBack() },
                )
            }
                }
                }
                if (!wide && surface != AppSurface.SHEET) SdoNavigationBar(buildList {
                    add(SdoNavigationItem("Painel", surface == AppSurface.DASHBOARD, Icons.Default.Home) { navigate(AppSurface.DASHBOARD, null) })
                    add(SdoNavigationItem("Sessão", surface == AppSurface.SESSION, Icons.Default.PlayCircle) { navigate(AppSurface.SESSION, null) })
                    if (canOpenHistorian) add(SdoNavigationItem("Mestre", surface == AppSurface.HISTORIAN, Icons.Default.Visibility) { navigate(AppSurface.HISTORIAN) })
                    add(SdoNavigationItem("Ajustes", surface == AppSurface.SETTINGS, Icons.Default.Settings) { navigate(AppSurface.SETTINGS) })
                    add(SdoNavigationItem("Biblioteca", surface == AppSurface.LIBRARY, Icons.AutoMirrored.Filled.MenuBook) { navigate(AppSurface.LIBRARY) })
                })
            }
            }
        }
    }

    when {
        activeSheetConflict != null -> CharacterConflictDialog(
            conflict = activeSheetConflict,
            characterName = characters.firstOrNull { it.id == activeSheetConflict.sheetId }?.name.orEmpty(),
            onResolve = { useRemote, customValueJson ->
                appViewModel.resolveIntentConflict(activeSheetConflict.id, useRemote, customValueJson)
            },
            onDiscard = { appViewModel.discardDraft(activeSheetConflict.operationId) },
        )
        activeLegacyConflict != null -> CharacterConflictDialog(
            conflict = activeLegacyConflict,
            onResolve = { remoteFieldIds -> appViewModel.resolveConflict(activeLegacyConflict, remoteFieldIds) },
        )
        reviewingAccessDenied && activeDeniedIntent != null -> AccessDeniedDraftDialog(
            intent = activeDeniedIntent,
            characterName = characters.firstOrNull { it.id == activeDeniedIntent.sheetId }?.name.orEmpty(),
            onRetry = {
                appViewModel.retryAccessDeniedDraft(activeDeniedIntent.operationId)
                reviewingAccessDenied = false
            },
            onDiscard = {
                appViewModel.discardDraft(activeDeniedIntent.operationId)
                reviewingAccessDenied = false
            },
            onDismiss = { reviewingAccessDenied = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SyncStatusBanner(
    offlineReadOnly: Boolean,
    syncFailure: String?,
    status: SheetSyncStatus,
    offlineSheetsAvailable: Boolean,
    hasAccessDeniedDraft: Boolean,
    hasOpenConflict: Boolean,
    onRetry: () -> Unit,
    onReviewAccessDenied: () -> Unit,
    onRetryAccessDenied: () -> Unit,
    onDiscardAccessDenied: () -> Unit,
    onDismissConfirmed: () -> Unit,
) {
    val hasError = status.hasActionRequired || syncFailure != null
    val isConfirmedOnly = !hasError &&
        !offlineReadOnly &&
        status.totalQueuedCount == 0 &&
        !status.isSyncing &&
        status.confirmedCount > 0
    val containerColor = if (!hasError) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (!hasError) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val bannerText = when {
        status.conflictCount > 0 -> stringResource(R.string.sheet_sync_conflict, status.conflictCount)
        status.accessDeniedCount > 0 -> stringResource(
            R.string.sheet_sync_access_denied,
            status.accessDeniedCount,
            status.lastError.orEmpty(),
        ).trim()
        status.rejectedCount > 0 -> stringResource(
            R.string.sheet_sync_rejected,
            status.rejectedCount,
            status.lastError.orEmpty(),
        ).trim()
        offlineReadOnly && offlineSheetsAvailable -> stringResource(R.string.sheet_sync_offline_editable)
        offlineReadOnly -> stringResource(R.string.sheet_sync_offline_unavailable)
        status.pausedCount > 0 -> stringResource(R.string.sheet_sync_paused, status.pausedCount)
        status.sendingCount > 0 || status.isSyncing -> stringResource(
            R.string.sheet_sync_sending,
            maxOf(status.sendingCount, status.pendingCount).coerceAtLeast(1),
        )
        status.pendingCount > 0 -> stringResource(R.string.sheet_sync_pending, status.pendingCount)
        status.draftCount > 0 -> stringResource(R.string.sheet_sync_draft, status.draftCount)
        syncFailure != null -> syncFailure
        status.confirmedCount > 0 -> stringResource(R.string.sheet_sync_confirmed, status.confirmedCount)
        else -> ""
    }
    val stateSummaryItems = buildList {
        if (status.draftCount > 0) add(stringResource(R.string.sheet_sync_summary_draft, status.draftCount))
        if (status.pendingCount > 0) add(stringResource(R.string.sheet_sync_summary_pending, status.pendingCount))
        if (status.sendingCount > 0) add(stringResource(R.string.sheet_sync_summary_sending, status.sendingCount))
        if (status.confirmedCount > 0) add(stringResource(R.string.sheet_sync_summary_confirmed, status.confirmedCount))
        if (status.conflictCount > 0) add(stringResource(R.string.sheet_sync_summary_conflict, status.conflictCount))
        if (status.rejectedCount > 0) add(stringResource(R.string.sheet_sync_summary_rejected, status.rejectedCount))
        if (status.accessDeniedCount > 0) add(stringResource(R.string.sheet_sync_summary_access_denied, status.accessDeniedCount))
        if (status.pausedCount > 0) add(stringResource(R.string.sheet_sync_summary_paused, status.pausedCount))
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!offlineReadOnly && (status.isSyncing || status.sendingCount > 0 || status.pendingCount > 0)) {
                    com.kinderman.sdo.ui.SdoRotatingSyncIcon(
                        syncing = true,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(
                        imageVector = when {
                            offlineReadOnly -> Icons.Default.CloudOff
                            isConfirmedOnly -> Icons.Default.CheckCircle
                            else -> Icons.Default.SyncProblem
                        },
                        contentDescription = null,
                        tint = if (isConfirmedOnly) MaterialTheme.colorScheme.primary else contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = bannerText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            if (stateSummaryItems.size > 1) {
                SdoMetadataText(
                    text = stateSummaryItems.joinToString(" · "),
                    color = contentColor,
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (status.accessDeniedCount > 0 && hasAccessDeniedDraft) {
                    SdoTextButton(onClick = onReviewAccessDenied) {
                        Text(stringResource(R.string.sheet_sync_action_review))
                    }
                    SdoTextButton(onClick = onRetryAccessDenied) {
                        Text(stringResource(R.string.sheet_sync_action_retry_draft))
                    }
                    SdoTextButton(onClick = onDiscardAccessDenied) {
                        Text(stringResource(R.string.sheet_sync_action_discard_draft))
                    }
                } else if (isConfirmedOnly) {
                    SdoTextButton(onClick = onDismissConfirmed) {
                        Text(stringResource(R.string.sheet_sync_action_dismiss))
                    }
                } else if (!hasOpenConflict) {
                    SdoTextButton(onClick = onRetry) {
                        Text(
                            stringResource(
                                if (offlineReadOnly) R.string.sheet_sync_action_reconnect
                                else R.string.sheet_sync_action_retry,
                            ),
                        )
                    }
                }
            }
        }
    }
}
