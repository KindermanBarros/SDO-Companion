package com.kinderman.sdo.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** Keeps a single-word Cybergrunge card heading intact by fitting its font to the available width. */
@Composable
fun SdoCardTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    style: TextStyle = LocalTextStyle.current,
) {
    if (LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE &&
        text.trim().none(Char::isWhitespace)
    ) {
        AdaptiveSingleLineText(text, modifier, color, style, minimumSize = 1.sp)
    } else {
        Text(text, modifier = modifier, color = color, style = style)
    }
}
