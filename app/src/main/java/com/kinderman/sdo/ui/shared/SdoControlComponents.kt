package com.kinderman.sdo.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SdoActionStyle { PRIMARY, SECONDARY, DESTRUCTIVE }

@Composable
fun SdoActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: SdoActionStyle = SdoActionStyle.SECONDARY,
) {
    if (LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE) {
        CyberGrungeAction(label, onClick, modifier, enabled, style)
        return
    }
    val shape = SdoShapeTokens.control
    when (style) {
        SdoActionStyle.PRIMARY -> Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = SdoSpacingTokens.minimumTouchTarget),
            shape = shape,
        ) { AdaptiveActionLabel(label, color = MaterialTheme.colorScheme.onPrimary) }
        SdoActionStyle.SECONDARY -> OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = SdoSpacingTokens.minimumTouchTarget),
            shape = shape,
        ) { AdaptiveActionLabel(label) }
        SdoActionStyle.DESTRUCTIVE -> OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = SdoSpacingTokens.minimumTouchTarget),
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { AdaptiveActionLabel(label, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun SdoFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE) {
        val accent = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
            border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, accent),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = accent,
                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f) else androidx.compose.ui.graphics.Color.Transparent,
            ),
        ) { AdaptiveActionLabel(if (selected) "■ ${label.uppercase()}" else "□ ${label.uppercase()}", color = accent) }
        return
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { AdaptiveActionLabel(label) },
        modifier = modifier.heightIn(min = 48.dp),
        shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = .18f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
fun <T> SdoResponsiveGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    minItemWidth: Dp = 132.dp,
    maxColumns: Int = 4,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable (T, Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = responsiveColumnCount(maxWidth.value, minItemWidth.value, maxColumns, horizontalSpacing.value)
        androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
            items.chunked(columns).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)) {
                    rowItems.forEach { item -> content(item, Modifier.weight(1f)) }
                    repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

internal fun responsiveColumnCount(
    widthDp: Float,
    minimumItemWidthDp: Float,
    maxColumns: Int = 4,
    spacingDp: Float = 0f,
): Int = ((widthDp + spacingDp) / (minimumItemWidthDp + spacingDp)).toInt()
    .coerceIn(1, maxColumns.coerceAtLeast(1))
