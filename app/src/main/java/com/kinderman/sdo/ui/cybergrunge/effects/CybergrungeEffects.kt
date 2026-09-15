package com.kinderman.sdo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Static, allocation-light panel interference. Stateful motion is reserved for meaningful states. */
@Composable
internal fun CyberGrungeInterference(modifier: Modifier = Modifier, seed: Int = 17) {
    val phase = ((seed * 37 % 101) / 50f) - 1f
    Canvas(modifier.fillMaxSize()) {
        repeat(CyberGrungeTokens.PANEL_GLITCH_BLOCKS) { index ->
            val tear = if (index % 5 == 0) phase * size.width * .17f else phase * (index % 3) * 2.dp.toPx()
            val x = ((((seed + index * 43) % 101) / 101f * size.width) + tear + size.width) % size.width
            val y = ((seed * 3 + index * 67) % 97) / 97f * size.height
            drawRect(
                color = if (index % 4 == 0) CyberGrungeTokens.SignalRed.copy(alpha = .11f)
                else Color.White.copy(alpha = .045f),
                topLeft = Offset(x, y),
                size = Size((4 + index % 3 * 8).dp.toPx(), (2 + index % 2 * 2).dp.toPx()),
            )
        }
        repeat(3) { slice ->
            val y = ((((seed + slice * 31) % 89) / 89f) * size.height + phase * 9.dp.toPx())
                .coerceIn(0f, size.height)
            drawRect(
                color = if (slice == 1) CyberGrungeTokens.SignalRed.copy(alpha = .22f) else Color.White.copy(alpha = .08f),
                topLeft = Offset(if (slice % 2 == 0) 0f else size.width * .38f, y),
                size = Size(size.width * if (slice % 2 == 0) .62f else .55f, (1 + slice).dp.toPx()),
            )
        }
    }
}

/** Visual-only missing-signal state. The field label remains the accessible description. */
@Composable
internal fun CyberGrungeEmptySignal(modifier: Modifier = Modifier) {
    val phase = .43f
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
