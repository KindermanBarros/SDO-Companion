package com.kinderman.sdo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdaptiveActionLabel(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    BasicText(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(color = color, letterSpacing = 0.sp),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = MaterialTheme.typography.labelLarge.fontSize),
    )
}

@Composable
fun CollapsibleSection(title: String, content: @Composable () -> Unit) {
    val collapsible = LocalSdoPreferences.current.collapseLongSections
    var expanded by rememberSaveable { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (collapsible) TechPanel(
            modifier = Modifier.clickable { expanded = !expanded },
            accent = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    if (expanded) "Recolher $title" else "Expandir $title",
                )
            }
        }
        AnimatedVisibility(visible = !collapsible || expanded) { content() }
    }
}
