package com.kinderman.sdo.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag

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
                    title = { Text("APARÊNCIA E ACESSIBILIDADE") },
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
                    TechPanel(accent = MaterialTheme.colorScheme.primary) {
                        TelemetryTag("LOCAL_PREFERENCES")
                        Text("Preferências ficam neste aparelho", color = Ice, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "A troca é imediata e não altera a ficha, a campanha ou o Firebase.",
                            color = Muted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                item {
                    ChoicePanel(
                        title = "TEMA",
                        options = SdoThemeVariant.entries,
                        selected = preferences.theme,
                        label = SdoThemeVariant::label,
                        description = SdoThemeVariant::description,
                        onSelect = { onPreferencesChange(preferences.copy(theme = it)) },
                    )
                }
                item {
                    ChoicePanel(
                        title = "DENSIDADE",
                        options = SdoContentDensity.entries,
                        selected = preferences.density,
                        label = SdoContentDensity::label,
                        description = SdoContentDensity::description,
                        onSelect = { onPreferencesChange(preferences.copy(density = it)) },
                    )
                }
                item {
                    ChoicePanel(
                        title = "LEITURA",
                        options = SdoFontScale.entries,
                        selected = preferences.fontScale,
                        label = SdoFontScale::label,
                        description = { if (it == SdoFontScale.LARGE) "Amplia todos os textos da interface." else "Escala original da interface." },
                        onSelect = { onPreferencesChange(preferences.copy(fontScale = it)) },
                    )
                }
                item {
                    TechPanel(accent = Acid) {
                        TelemetryTag("A11Y.CONTRACT")
                        Text("Estados usam texto, ícone e cor", color = Ice)
                        Text("Controles preservam área mínima de toque e seguem a escala de animação do sistema.", color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> ChoicePanel(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    description: (T) -> String,
    onSelect: (T) -> Unit,
) {
    TechPanel(accent = MaterialTheme.colorScheme.primary) {
        TelemetryTag(title)
        options.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option == selected, onClick = { onSelect(option) })
                Column(Modifier.weight(1f)) {
                    Text(label(option), color = MaterialTheme.colorScheme.onSurface)
                    Text(description(option), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
