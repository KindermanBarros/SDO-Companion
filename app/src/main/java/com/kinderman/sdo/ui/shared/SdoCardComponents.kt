package com.kinderman.sdo.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CyberPanel(
    title: String,
    modifier: Modifier = Modifier,
    code: String? = null,
    summary: String? = null,
    accent: Color? = null,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    TechPanel(modifier = modifier, accent = accent) {
        code?.let { TelemetryTag(it) }
        AdaptiveSingleLineText(title, style = MaterialTheme.typography.titleLarge)
        summary?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        content()
        actions?.invoke(this)
    }
}

@Composable
fun CyberSectionHeader(index: String, title: String, modifier: Modifier = Modifier) =
    SectionHeader(index, title, modifier)

/** Compact label/value modules shared by catalog, library and operational summaries. */
@Composable
fun SdoDataModules(
    fields: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    val visible = fields.filter { it.second.isNotBlank() }
    if (visible.isEmpty()) return
    SdoResponsiveGrid(
        items = visible,
        modifier = modifier,
        minItemWidth = 144.dp,
        maxColumns = 2,
        horizontalSpacing = 7.dp,
        verticalSpacing = 7.dp,
    ) { (label, value), itemModifier ->
        SdoInsetCard(
            modifier = itemModifier,
            accent = if (LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE)
                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            contentPadding = 9.dp,
            verticalSpacing = 4.dp,
        ) {
            AdaptiveSingleLineText(
                text = label.uppercase(),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                minimumSize = 9.sp,
            )
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
        }
    }
}
