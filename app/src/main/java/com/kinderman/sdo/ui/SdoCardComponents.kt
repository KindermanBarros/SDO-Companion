package com.kinderman.sdo.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

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
        Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
        summary?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        content()
        actions?.invoke(this)
    }
}

@Composable
fun CyberSectionHeader(index: String, title: String, modifier: Modifier = Modifier) =
    SectionHeader(index, title, modifier)
