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
import com.kinderman.sdo.presentation.character.CharacterSheetScreen
import com.kinderman.sdo.presentation.dashboard.DashboardScreen
import com.kinderman.sdo.presentation.login.LoginScreen

@Composable
fun SdoApp(activity: MainActivity) {
    val application = LocalContext.current.applicationContext as SdoApplication
    val appViewModel: AppViewModel = viewModel(factory = AppViewModelFactory(application.characterRepository))
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application.authRepository))
    val authenticatedSession by authViewModel.session.collectAsStateWithLifecycle()
    val appSession by appViewModel.session.collectAsStateWithLifecycle()
    val characters by appViewModel.characters.collectAsStateWithLifecycle()
    val message by appViewModel.message.collectAsStateWithLifecycle()
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
        authenticatedSession == null && !demo -> LoginScreen(
            activity = activity,
            state = authViewModel.state,
            allowDemo = !authViewModel.configured,
            onGoogleLogin = { authViewModel.login(activity) },
            onDemo = { demo = true },
        )

        selectedId == null -> DashboardScreen(
            characters = characters,
            session = appSession,
            snackbarHost = { SnackbarHost(snackbar) },
            onAdd = appViewModel::add,
            onOpen = { selectedId = it },
            onSync = appViewModel::sync,
            onLogout = {
                selectedId = null
                demo = false
                authViewModel.logout()
                appViewModel.clearSession()
            },
        )

        else -> CharacterSheetScreen(
            character = characters.firstOrNull { it.id == selectedId },
            session = appSession,
            snackbarHost = { SnackbarHost(snackbar) },
            onBack = { selectedId = null },
            onSave = appViewModel::save,
            onLock = appViewModel::setLocked,
            onDelete = {
                appViewModel.delete(it)
                selectedId = null
            },
        )
    }
}
