package com.kinderman.sdo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

enum class CyberGrungeIntensity(val shaderAmount: Float) {
    LATENT(.18f), INTERFERENCE(.52f), RUPTURE(1f),
}

val LocalCyberGrungeIntensity = compositionLocalOf { CyberGrungeIntensity.LATENT }

/** Coordinated scan/RGB tear layer compatible with every supported Android API. */
@Composable
internal fun CyberGrungeShaderLayer(
    modifier: Modifier = Modifier,
    intensity: CyberGrungeIntensity = LocalCyberGrungeIntensity.current,
) {
    val phase = .37f
    val red = CyberGrungeTokens.SignalRed
    val cyan = CyberGrungeTokens.TerminalGreen
    Canvas(modifier.fillMaxSize()) {
        val amount = intensity.shaderAmount
        drawRect(red.copy(alpha = .018f * amount), Offset(0f, phase * size.height), Size(size.width, 2.dp.toPx()))
        repeat(6) { index ->
            val drift = phase * if (index % 2 == 0) .11f else -.07f
            val y = ((((index * .173f) + drift) % 1f + 1f) % 1f) * size.height
            val x = ((index * 37) % 100) / 100f * size.width
            val width = size.width * (.04f + (index % 3) * .025f)
            drawRect(
                if (index % 2 == 0) red.copy(alpha = .035f * amount) else cyan.copy(alpha = .028f * amount),
                Offset(x, y), Size(width, (1 + index % 2).dp.toPx()),
            )
        }
    }
}
