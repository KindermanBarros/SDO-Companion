package com.kinderman.sdo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

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
            if (LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE) {
                val accent = if (option.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                Row(
                    Modifier.fillMaxWidth()
                        .border(1.dp, accent, CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                        .clip(CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                        .clickable { option.onChange(!option.selected) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (option.selected) "ON//" else "OFF/", color = accent, style = MaterialTheme.typography.labelSmall)
                    Text(option.label.uppercase(), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Box(Modifier.width(34.dp).height(14.dp).border(1.dp, accent).padding(2.dp)) {
                        if (option.selected) Box(Modifier.fillMaxWidth(.68f).height(8.dp).background(accent).align(Alignment.CenterEnd))
                    }
                }
                return@forEach
            }
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
            if (LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE) {
                CyberGrungeChoiceRow(
                    label = label(option), description = description(option), selected = option == selected,
                    onClick = { onSelect(option) }, badge = { optionBadge(option) },
                )
                return@forEach
            }
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
