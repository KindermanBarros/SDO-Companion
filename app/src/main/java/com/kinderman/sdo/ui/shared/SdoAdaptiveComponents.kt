package com.kinderman.sdo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LocalFlattenCollapsiblePanel = compositionLocalOf { false }
internal val LocalCollapsibleSectionTitle = compositionLocalOf<String?> { null }

@Composable
fun AdaptiveActionLabel(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    AdaptiveSingleLineText(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.sp),
    )
}

/** Prefers one line; wraps complete words rather than truncating when readable text needs more room. */
@Composable
fun AdaptiveSingleLineText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    minimumSize: androidx.compose.ui.unit.TextUnit = 11.sp,
) {
    val measurer = rememberTextMeasurer()
    val baseSize = if (style.fontSize.isSp) style.fontSize else MaterialTheme.typography.bodyLarge.fontSize
    BoxWithConstraints(modifier) {
        val availableWidth = constraints.maxWidth.coerceAtLeast(1)
        val fittedSize = remember(text, style, baseSize, availableWidth, measurer) {
            val words = text.split(Regex("\\s+")).filter(String::isNotEmpty)
            fun widestWord(size: Float): Int = words.maxOfOrNull { word ->
                measurer.measure(word, style = style.copy(fontSize = size.sp), softWrap = false).size.width
            } ?: 0
            var size = baseSize.value
            val width = widestWord(size)
            if (width > availableWidth) {
                size = (size * availableWidth / width).coerceAtLeast(1f)
                while (size > 1f && widestWord(size) > availableWidth) size = (size - .5f).coerceAtLeast(1f)
            }
            size.sp
        }
        val minimum = minOf(minimumSize.value, fittedSize.value).sp
        val fitsOneLine = !text.contains('\n') && measurer.measure(
            text, style = style.copy(fontSize = minimum), softWrap = false,
        ).size.width <= availableWidth
        BasicText(
            text = text,
            style = style.copy(color = color, fontSize = fittedSize),
            maxLines = if (fitsOneLine || text.none(Char::isWhitespace)) 1 else Int.MAX_VALUE,
            softWrap = !fitsOneLine && text.any(Char::isWhitespace),
            autoSize = TextAutoSize.StepBased(minFontSize = minimum, maxFontSize = fittedSize),
        )
    }
}
@Composable
fun CollapsibleSection(title: String, index: String = "", content: @Composable () -> Unit) {
    val collapsible = LocalSdoPreferences.current.collapseLongSections
    if (!collapsible) {
        content()
        return
    }

    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    TechPanel(
        accent = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    stateDescription = if (expanded) "Expandido" else "Recolhido"
                }
                .toggleable(value = expanded, role = Role.Button, onValueChange = { expanded = it })
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (index.isNotBlank()) {
                TelemetryTag(
                    index,
                    if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(10.dp))
            }
            AdaptiveSingleLineText(
                title.uppercase(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Recolher $title" else "Expandir $title",
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(150), expandFrom = Alignment.Top) +
                    fadeIn(animationSpec = tween(90)),
            exit = shrinkVertically(animationSpec = tween(130), shrinkTowards = Alignment.Top) +
                    fadeOut(animationSpec = tween(80)),
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
