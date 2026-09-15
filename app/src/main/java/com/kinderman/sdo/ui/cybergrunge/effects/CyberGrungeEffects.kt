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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Animated framebuffer tears inside a panel, driven by the same clock as its shake. */
@Composable
internal fun CyberGrungeInterference(
    motion: CyberGrungePanelMotion,
    modifier: Modifier = Modifier,
    seed: Int = 17,
) {
    Canvas(modifier.fillMaxSize()) {
        repeat(CyberGrungeTokens.PANEL_GLITCH_BLOCKS) { index ->
            val tear = motion.translationX * (if (index % 5 == 0) size.width * .05f else (index % 3) * 2.dp.toPx())
            val x = ((((seed + index * 43) % 101) / 101f * size.width) + tear + size.width) % size.width
            val y = (((seed * 3 + index * 67 + (motion.translationY * 11).toInt()) % 97 + 97) % 97) / 97f * size.height
            drawRect(
                color = if (index % 4 == 0) CyberGrungeTokens.SignalRed.copy(alpha = .11f)
                else Color.White.copy(alpha = .045f),
                topLeft = Offset(x, y),
                size = Size((4 + index % 3 * 8).dp.toPx(), (2 + index % 2 * 2).dp.toPx()),
            )
        }
        repeat(3) { slice ->
            val y = ((((seed + slice * 31) % 89) / 89f) * size.height + motion.translationY * 9.dp.toPx())
                .coerceIn(0f, size.height)
            drawRect(
                color = if (slice == 1) CyberGrungeTokens.SignalRed.copy(alpha = .22f) else Color.White.copy(alpha = .08f),
                topLeft = Offset(if (slice % 2 == 0) 0f else size.width * .38f, y),
                size = Size(size.width * if (slice % 2 == 0) .62f else .55f, (1 + slice).dp.toPx()),
            )
        }

        // A narrow directional tear: left while travelling left, right while travelling right.
        val edge = if (motion.direction < 0) 0f else size.width
        val inward = if (motion.direction < 0) 1f else -1f
        val strength = .24f + motion.rupture * .76f
        repeat(CyberGrungeTokens.PANEL_EDGE_GLITCH_LINES) { line ->
            val lineSeed = kotlin.math.abs(seed + line * 53)
            val y = (lineSeed % 101) / 101f * size.height
            val width = (3 + lineSeed % 19).dp.toPx() * strength
            val x = edge + inward * if (motion.direction < 0) 0f else width
            drawRect(
                color = when (line % 3) {
                    0 -> CyberGrungeTokens.SignalRed.copy(alpha = .2f + .45f * strength)
                    1 -> CyberGrungeTokens.TerminalGreen.copy(alpha = .13f + .3f * strength)
                    else -> Color.White.copy(alpha = .1f + .24f * strength)
                },
                topLeft = Offset(x, y),
                size = Size(width, (1 + line % 4).dp.toPx()),
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
            repeatMode = RepeatMode.Reverse,
        ),
        label = "empty-signal-phase",
    )
    Canvas(modifier.fillMaxWidth().height(22.dp)) {
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
