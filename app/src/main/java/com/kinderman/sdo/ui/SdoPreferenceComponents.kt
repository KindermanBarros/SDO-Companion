package com.kinderman.sdo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

data class SdoToggleOption(
    val label: String,
    val selected: Boolean,
    val onChange: (Boolean) -> Unit,
)

@Composable
fun SdoTogglePanel(title: String, options: List<SdoToggleOption>) {
    TechPanel(accent = MaterialTheme.colorScheme.secondary) {
        TelemetryTag(title)
        options.forEach { option ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(option.label, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Switch(checked = option.selected, onCheckedChange = option.onChange)
            }
        }
    }
}

@Composable
fun <T> SdoChoicePanel(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    description: (T) -> String,
    onSelect: (T) -> Unit,
    optionBadge: @Composable (T) -> Unit = {},
) {
    TechPanel(accent = MaterialTheme.colorScheme.primary) {
        TelemetryTag(title)
        options.forEach { option ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = option == selected, onClick = { onSelect(option) })
                Column(Modifier.weight(1f)) {
                    optionBadge(option)
                    Text(label(option), color = MaterialTheme.colorScheme.onSurface)
                    Text(description(option), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
