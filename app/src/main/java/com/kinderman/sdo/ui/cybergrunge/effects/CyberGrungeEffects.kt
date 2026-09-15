package com.kinderman.sdo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Static, allocation-light panel interference. Stateful motion is reserved for meaningful states. */
@Composable
internal fun CyberGrungeInterference(modifier: Modifier = Modifier, seed: Int = 17) {
    Canvas(modifier.fillMaxSize()) {
        repeat(CyberGrungeTokens.PANEL_GLITCH_BLOCKS) { index ->
            val x = ((seed + index * 43) % 101) / 101f * size.width
            val y = ((seed * 3 + index * 67) % 97) / 97f * size.height
            drawRect(
                color = if (index % 4 == 0) CyberGrungeTokens.SignalRed.copy(alpha = .11f)
                else Color.White.copy(alpha = .045f),
                topLeft = Offset(x, y),
                size = Size((4 + index % 3 * 8).dp.toPx(), (2 + index % 2 * 2).dp.toPx()),
            )
        }
    }
}

/** Visual-only missing-signal state. The field label remains the accessible description. */
@Composable
internal fun CyberGrungeEmptySignal(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "empty-signal")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SdoMotionTokens.TELEMETRY_SCAN),
            repeatMode = RepeatMode.Restart,
        ),
        label = "empty-signal-phase",
    )
    Canvas(modifier.fillMaxWidth().height(22.dp).testTag("cybergrunge-empty-signal")) {
        repeat(CyberGrungeTokens.EMPTY_SIGNAL_BLOCKS) { index ->
            val direction = if (index % 2 == 0) 1f else -1f
            val drift = phase * size.width * .18f * direction
            val start = ((((index * 31) % 97) / 97f * size.width) + drift + size.width) % size.width
            val width = size.width * (.025f + (index % 4) * .018f)
            drawRect(
                color = when (index % 3) {
                    0 -> CyberGrungeTokens.SignalRed.copy(alpha = .78f)
                    1 -> CyberGrungeTokens.TerminalGreen.copy(alpha = .58f)
                    else -> Color.White.copy(alpha = .48f)
                },
                topLeft = Offset(start, ((index + (phase * 4).toInt()) % 4) * 4.dp.toPx()),
                size = Size(width, (2 + index % 3).dp.toPx()),
            )
        }
    }
}
