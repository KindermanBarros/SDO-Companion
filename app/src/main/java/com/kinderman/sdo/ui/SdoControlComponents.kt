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
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { AdaptiveActionLabel(label) },
        modifier = modifier.heightIn(min = 48.dp),
        shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
        val columns = responsiveColumnCount(maxWidth.value, minItemWidth.value, maxColumns)
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

internal fun responsiveColumnCount(widthDp: Float, minimumItemWidthDp: Float, maxColumns: Int = 4): Int =
    (widthDp / minimumItemWidthDp).toInt().coerceIn(1, maxColumns.coerceAtLeast(1))
