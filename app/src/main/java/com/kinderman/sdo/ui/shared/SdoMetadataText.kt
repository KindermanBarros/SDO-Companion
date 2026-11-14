package com.kinderman.sdo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

/** Theme-aware metadata; delimiters retain their literal text and neutral emphasis. */
@Composable
fun SdoMetadataText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    autoSize: TextAutoSize? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val accents = listOf(scheme.primary, scheme.secondary, scheme.tertiary).map { accent ->
        val background = scheme.surface.luminance()
        fun contrast(candidate: Color): Float {
            val foreground = candidate.luminance()
            return (maxOf(foreground, background) + .05f) /
                (minOf(foreground, background) + .05f)
        }
        if (contrast(accent) >= 4.5f) accent else lerp(accent, scheme.onSurface, .7f)
    }
    val annotated = remember(text, accents, color) {
        buildAnnotatedString {
            text.split("//").forEachIndexed { index, chunk ->
                if (index > 0) withStyle(SpanStyle(color = color)) { append("//") }
                withStyle(SpanStyle(color = accents[index % accents.size])) { append(chunk) }
            }
        }
    }
    BasicText(
        text = annotated,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        softWrap = maxLines != 1,
        overflow = overflow,
        autoSize = autoSize,
    )
}
