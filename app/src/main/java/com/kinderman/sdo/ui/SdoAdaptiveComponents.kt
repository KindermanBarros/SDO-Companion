package com.kinderman.sdo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LocalFlattenCollapsiblePanel = compositionLocalOf { false }
internal val LocalCollapsibleSectionTitle = compositionLocalOf<String?> { null }

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
    if (!collapsible) {
        content()
        return
    }

    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    TechPanel(
        modifier = Modifier.animateContentSize(),
        accent = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    stateDescription = if (expanded) "Expandido" else "Recolhido"
                }
                .toggleable(
                    value = expanded,
                    role = Role.Button,
                    onValueChange = { expanded = it },
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Recolher $title" else "Expandir $title",
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            CompositionLocalProvider(
                LocalFlattenCollapsiblePanel provides true,
                LocalCollapsibleSectionTitle provides title,
            ) {
                content()
            }
        }
    }
}
