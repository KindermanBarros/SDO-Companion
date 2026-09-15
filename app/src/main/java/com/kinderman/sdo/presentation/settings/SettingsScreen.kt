package com.kinderman.sdo.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.SdoContentDensity
import com.kinderman.sdo.ui.SdoFontScale
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.SdoThemeVariant
import com.kinderman.sdo.ui.SdoVisualMode
import com.kinderman.sdo.ui.SdoChoicePanel
import com.kinderman.sdo.ui.SdoToggleOption
import com.kinderman.sdo.ui.SdoTogglePanel
import com.kinderman.sdo.ui.CyberPanel
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag
import com.kinderman.sdo.ui.ExperimentalBadge

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: SdoPreferences,
    onPreferencesChange: (SdoPreferences) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("CONFIGURAÇÕES") },
                    navigationIcon = {
                        IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    CyberPanel(
                        title = "Preferências ficam neste aparelho",
                        code = "LOCAL_PREFERENCES",
                        summary = "A troca é imediata e não altera a ficha, a campanha ou o Firebase.",
                        accent = MaterialTheme.colorScheme.primary,
                    )
                }
                item {
                    SdoTogglePanel("INTERFACE", listOf(
                        SdoToggleOption("Resumir cartões", preferences.compactCards) { onPreferencesChange(preferences.copy(compactCards = it)) },
                        SdoToggleOption("Recolher seções extensas", preferences.collapseLongSections) { onPreferencesChange(preferences.copy(collapseLongSections = it)) },
                    ))
                }
                item {
                    SdoTogglePanel("SINCRONIZAÇÃO E NOTIFICAÇÕES", listOf(
                        SdoToggleOption("Sincronização automática", preferences.autoSync) { onPreferencesChange(preferences.copy(autoSync = it)) },
                        SdoToggleOption("Alertas e entregas", preferences.notifications) { onPreferencesChange(preferences.copy(notifications = it)) },
                    ))
                }
                item {
                    SdoTogglePanel("CAMPANHAS", listOf(
                        SdoToggleOption("Mostrar campanhas arquivadas", preferences.showArchivedCampaigns) { onPreferencesChange(preferences.copy(showArchivedCampaigns = it)) },
                    ))
                }
                item {
                    CyberPanel(
                        title = "Conta e sessão",
                        code = "ACCOUNT.SESSION",
                        summary = "Identidade, saída e permissões continuam centralizadas no painel principal.",
                        accent = MaterialTheme.colorScheme.secondary,
                    )
                }
                item {
                    SdoChoicePanel(
                        title = "MODO VISUAL",
                        options = SdoVisualMode.entries,
                        selected = preferences.visualMode,
                        label = SdoVisualMode::label,
                        description = SdoVisualMode::description,
                        onSelect = { onPreferencesChange(preferences.copy(visualMode = it)) },
                        optionBadge = { option ->
                            if (option == SdoVisualMode.KALTOCH) ExperimentalBadge()
                        },
                    )
                }
                if (preferences.visualMode == SdoVisualMode.STANDARD) item {
                    SdoChoicePanel(
                        title = "TEMA",
                        options = SdoThemeVariant.entries,
                        selected = preferences.theme,
                        label = SdoThemeVariant::label,
                        description = SdoThemeVariant::description,
                        onSelect = { onPreferencesChange(preferences.copy(theme = it)) },
                    )
                }
                item {
                    SdoChoicePanel(
                        title = "DENSIDADE",
                        options = SdoContentDensity.entries,
                        selected = preferences.density,
                        label = SdoContentDensity::label,
                        description = SdoContentDensity::description,
                        onSelect = { onPreferencesChange(preferences.copy(density = it)) },
                    )
                }
                item {
                    SdoChoicePanel(
                        title = "LEITURA",
                        options = SdoFontScale.entries,
                        selected = preferences.fontScale,
                        label = SdoFontScale::label,
                        description = { if (it == SdoFontScale.LARGE) "Amplia todos os textos da interface." else "Escala original da interface." },
                        onSelect = { onPreferencesChange(preferences.copy(fontScale = it)) },
                    )
                }

            }
        }
    }
}
