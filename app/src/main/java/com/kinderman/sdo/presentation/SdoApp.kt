package com.kinderman.sdo.presentation

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kinderman.sdo.MainActivity
import com.kinderman.sdo.SdoApplication
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.presentation.character.CharacterSheetScreen
import com.kinderman.sdo.presentation.dashboard.DashboardScreen
import com.kinderman.sdo.presentation.historian.HistorianDashboardScreen
import com.kinderman.sdo.presentation.login.LoginScreen
import com.kinderman.sdo.presentation.session.SessionModeScreen
import com.kinderman.sdo.presentation.settings.SettingsScreen
import com.kinderman.sdo.presentation.sync.CharacterConflictDialog
import com.kinderman.sdo.ui.CyberLoadingMode
import com.kinderman.sdo.ui.CyberLoadingScreen
import com.kinderman.sdo.ui.SdoContentDensity
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.LocalSdoWindowClass
import com.kinderman.sdo.ui.SdoWindowClass

private enum class AppSurface { DASHBOARD, SHEET, SESSION, HISTORIAN, SETTINGS }

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
        ),
    )
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application.authRepository))
    val authenticatedSession by authViewModel.session.collectAsStateWithLifecycle()
    val appSession by appViewModel.session.collectAsStateWithLifecycle()
    val characters by appViewModel.characters.collectAsStateWithLifecycle()
    val campaigns by appViewModel.campaigns.collectAsStateWithLifecycle()
    val memberships by appViewModel.memberships.collectAsStateWithLifecycle()
    val campaignMembers by appViewModel.campaignMembers.collectAsStateWithLifecycle()
    val owners by appViewModel.owners.collectAsStateWithLifecycle()
    val characterLoadState by appViewModel.loadState.collectAsStateWithLifecycle()
    val conflicts by appViewModel.conflicts.collectAsStateWithLifecycle()
    val invitePreview by appViewModel.invitePreview.collectAsStateWithLifecycle()
    val message by appViewModel.message.collectAsStateWithLifecycle()
    val catalog by appViewModel.catalog.collectAsStateWithLifecycle()
    val audit by appViewModel.audit.collectAsStateWithLifecycle()
    val campaignLibrary by appViewModel.library.collectAsStateWithLifecycle()
    val deliveries by appViewModel.deliveries.collectAsStateWithLifecycle()
    val alertSettings by appViewModel.alertSettings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var demo by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var surface by rememberSaveable { mutableStateOf(AppSurface.DASHBOARD) }

    LaunchedEffect(authenticatedSession, demo) {
        when {
            demo -> appViewModel.setDemoSession()
            authenticatedSession != null -> appViewModel.setSession(authenticatedSession!!)
            else -> appViewModel.clearSession()
        }
    }
    LaunchedEffect(preferences.autoSync) { appViewModel.setAutomaticSync(preferences.autoSync) }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            appViewModel.dismissMessage()
        }
    }

    when {
        authViewModel.state.initializing -> CyberLoadingScreen(CyberLoadingMode.AUTH_BOOT)

        authViewModel.state.loading -> CyberLoadingScreen(CyberLoadingMode.GOOGLE_AUTH)

        authenticatedSession == null && !demo -> LoginScreen(
            activity = activity,
            state = authViewModel.state,
            allowDemo = !authViewModel.configured,
            onGoogleLogin = { authViewModel.login(activity) },
            onDemo = { demo = true },
        )

        appSession == null || characterLoadState.initialLoading ->
            CyberLoadingScreen(CyberLoadingMode.CHARACTERS)

        else -> {
            val selectedCharacter = characters.firstOrNull { it.id == selectedId }
            val selectedCampaignId = normalizeCampaignId(selectedCharacter?.campaignId.orEmpty())
            val archived = campaigns.firstOrNull { it.id == selectedCampaignId }?.isArchived == true
            val selectedCampaign = campaigns.firstOrNull { it.id == selectedCampaignId }
            val membership = memberships.firstOrNull { it.campaignId == selectedCampaignId }
            val isCampaignHistorian = appSession?.isAdmin == true || selectedCampaign?.ownerId == appSession?.uid
            val isCampaignResponsible = appSession?.isAdmin == true || selectedCampaign?.ownerId == appSession?.uid
            val wide = LocalSdoWindowClass.current == SdoWindowClass.EXPANDED
            val canOpenHistorian = appSession?.isAdmin == true || campaigns.any { it.ownerId == appSession?.uid }
            Row(Modifier.fillMaxSize()) {
                if (wide) NavigationRail {
                    NavigationRailItem(surface == AppSurface.DASHBOARD, { selectedId = null; surface = AppSurface.DASHBOARD }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Painel") })
                    NavigationRailItem(surface == AppSurface.SESSION, { selectedId = null; surface = AppSurface.SESSION }, icon = { Icon(Icons.Default.PlayCircle, null) }, label = { Text("Sessão") })
                    if (canOpenHistorian) NavigationRailItem(surface == AppSurface.HISTORIAN, { surface = AppSurface.HISTORIAN }, icon = { Icon(Icons.Default.Visibility, null) }, label = { Text("Mestre") })
                    NavigationRailItem(surface == AppSurface.SETTINGS, { surface = AppSurface.SETTINGS }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Ajustes") })
                }
                Box(Modifier.weight(1f).fillMaxSize()) {
            when (surface) {
                AppSurface.DASHBOARD -> DashboardScreen(
                    characters = characters,
                    campaigns = campaigns,
                    memberships = memberships,
                    campaignMembers = campaignMembers,
                    owners = owners,
                    session = appSession,
                    syncing = characterLoadState.syncing,
                    compactCards = preferences.compactCards,
                    showArchivedCampaigns = preferences.showArchivedCampaigns,
                    invitePreview = invitePreview,
                    snackbarHost = { SnackbarHost(snackbar) },
                    onAdd = appViewModel::add,
                    onAddToCampaign = appViewModel::addToCampaign,
                    onOpen = {
                        selectedId = it
                        surface = AppSurface.SHEET
                    },
                    onOwnerTransfer = appViewModel::transferOwner,
                    onCreateCampaign = appViewModel::createCampaign,
                    onArchiveCampaign = appViewModel::archiveCampaign,
                    onLeaveCampaign = appViewModel::leaveCampaign,
                    onCreateInvite = appViewModel::createCampaignInvite,
                    onPreviewInvite = appViewModel::previewCampaignInvite,
                    onAcceptInvite = appViewModel::acceptCampaignInvite,
                    onDismissInvitePreview = appViewModel::dismissInvitePreview,
                    onSync = appViewModel::sync,
                    onOpenSession = {
                        selectedId = null
                        surface = AppSurface.SESSION
                    },
                    onOpenHistorian = { surface = AppSurface.HISTORIAN },
                    onOpenSettings = { surface = AppSurface.SETTINGS },
                    deliveries = if (preferences.notifications) deliveries.filter { it.recipientId == appSession?.uid } else emptyList(),
                    onRespondDelivery = appViewModel::respondDelivery,
                    onLogout = {
                        selectedId = null
                        surface = AppSurface.DASHBOARD
                        demo = false
                        authViewModel.logout()
                        appViewModel.clearSession()
                    },
                )

                AppSurface.SHEET -> CharacterSheetScreen(
                    character = selectedCharacter,
                    session = appSession,
                    catalog = catalog,
                    readOnly = archived,
                    isCampaignHistorian = isCampaignHistorian,
                    isCampaignResponsible = isCampaignResponsible,
                    snackbarHost = { SnackbarHost(snackbar) },
                    onBack = {
                        selectedId = null
                        surface = AppSurface.DASHBOARD
                    },
                    onOpenSession = {
                        selectedId = it
                        surface = AppSurface.SESSION
                    },
                    onSave = appViewModel::save,
                    onAutosave = appViewModel::autosave,
                    onPlayerLock = appViewModel::setPlayerLocked,
                    onHistorianLock = appViewModel::setHistorianLocked,
                    onDelete = {
                        appViewModel.delete(it)
                        selectedId = null
                        surface = AppSurface.DASHBOARD
                    },
                )

                AppSurface.SESSION -> SessionModeScreen(
                    characters = characters,
                    selectedId = selectedId,
                    compact = preferences.density == SdoContentDensity.COMPACT || preferences.compactCards,
                    readOnly = archived,
                    onSelect = { selectedId = it },
                    onOpenSheet = {
                        selectedId = it
                        surface = AppSurface.SHEET
                    },
                    onCommand = appViewModel::applySessionCommand,
                    onBack = {
                        selectedId = null
                        surface = AppSurface.DASHBOARD
                    },
                )

                AppSurface.HISTORIAN -> HistorianDashboardScreen(
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
                        selectedId = it
                        surface = AppSurface.SESSION
                    },
                    onOpenSheet = {
                        selectedId = it
                        surface = AppSurface.SHEET
                    },
                    onBack = { surface = AppSurface.DASHBOARD },
                )

                AppSurface.SETTINGS -> SettingsScreen(
                    preferences = preferences,
                    onPreferencesChange = onPreferencesChange,
                    onBack = { surface = AppSurface.DASHBOARD },
                )
            }
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
