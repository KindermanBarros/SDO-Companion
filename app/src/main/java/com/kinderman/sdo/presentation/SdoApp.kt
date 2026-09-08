package com.kinderman.sdo.presentation

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.kinderman.sdo.presentation.login.LoginScreen
import com.kinderman.sdo.presentation.sync.CharacterConflictDialog
import com.kinderman.sdo.ui.CyberLoadingMode
import com.kinderman.sdo.ui.CyberLoadingScreen

@Composable
fun SdoApp(activity: MainActivity) {
    val application = LocalContext.current.applicationContext as SdoApplication
    val appViewModel: AppViewModel = viewModel(
        factory = AppViewModelFactory(
            application.characterRepository,
            application.ownerRepository,
            application.catalogRepository,
            application.campaignRepository,
        ),
    )
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application.authRepository))
    val authenticatedSession by authViewModel.session.collectAsStateWithLifecycle()
    val appSession by appViewModel.session.collectAsStateWithLifecycle()
    val characters by appViewModel.characters.collectAsStateWithLifecycle()
    val campaigns by appViewModel.campaigns.collectAsStateWithLifecycle()
    val owners by appViewModel.owners.collectAsStateWithLifecycle()
    val characterLoadState by appViewModel.loadState.collectAsStateWithLifecycle()
    val conflicts by appViewModel.conflicts.collectAsStateWithLifecycle()
    val invitePreview by appViewModel.invitePreview.collectAsStateWithLifecycle()
    val message by appViewModel.message.collectAsStateWithLifecycle()
    val catalog by appViewModel.catalog.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var demo by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(authenticatedSession, demo) {
        when {
            demo -> appViewModel.setDemoSession()
            authenticatedSession != null -> appViewModel.setSession(authenticatedSession!!)
            else -> appViewModel.clearSession()
        }
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

        authenticatedSession == null && !demo -> LoginScreen(
            activity = activity,
            state = authViewModel.state,
            allowDemo = !authViewModel.configured,
            onGoogleLogin = { authViewModel.login(activity) },
            onDemo = { demo = true },
        )

        appSession == null || characterLoadState.initialLoading ->
            CyberLoadingScreen(CyberLoadingMode.CHARACTERS)

        selectedId == null -> DashboardScreen(
            characters = characters,
            campaigns = campaigns,
            owners = owners,
            session = appSession,
            syncing = characterLoadState.syncing,
            invitePreview = invitePreview,
            snackbarHost = { SnackbarHost(snackbar) },
            onAdd = appViewModel::add,
            onAddToCampaign = appViewModel::addToCampaign,
            onOpen = { selectedId = it },
            onOwnerTransfer = appViewModel::transferOwner,
            onCreateCampaign = appViewModel::createCampaign,
            onArchiveCampaign = appViewModel::archiveCampaign,
            onLeaveCampaign = appViewModel::leaveCampaign,
            onCreateInvite = appViewModel::createCampaignInvite,
            onPreviewInvite = appViewModel::previewCampaignInvite,
            onAcceptInvite = appViewModel::acceptCampaignInvite,
            onDismissInvitePreview = appViewModel::dismissInvitePreview,
            onSync = appViewModel::sync,
            onLogout = {
                selectedId = null
                demo = false
                authViewModel.logout()
                appViewModel.clearSession()
            },
        )

        else -> {
            val selectedCharacter = characters.firstOrNull { it.id == selectedId }
            val selectedCampaignId = normalizeCampaignId(selectedCharacter?.campaignId.orEmpty())
            val archived = campaigns.firstOrNull { it.id == selectedCampaignId }?.isArchived == true
            CharacterSheetScreen(
                character = selectedCharacter,
                session = appSession,
                catalog = catalog,
                readOnly = archived,
                snackbarHost = { SnackbarHost(snackbar) },
                onBack = { selectedId = null },
                onSave = appViewModel::save,
                onAutosave = appViewModel::autosave,
                onPlayerLock = appViewModel::setPlayerLocked,
                onHistorianLock = appViewModel::setHistorianLocked,
                onDelete = {
                    appViewModel.delete(it)
                    selectedId = null
                },
            )
        }
    }

    conflicts.firstOrNull()?.let { conflict ->
        CharacterConflictDialog(
            conflict = conflict,
            onResolve = { remoteFieldIds -> appViewModel.resolveConflict(conflict, remoteFieldIds) },
        )
    }
}
