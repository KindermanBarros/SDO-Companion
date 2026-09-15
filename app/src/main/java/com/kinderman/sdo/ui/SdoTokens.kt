package com.kinderman.sdo.ui

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object SdoSpacingTokens {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val minimumTouchTarget = 48.dp
}

object SdoOpacityTokens {
    const val GRID_MINOR = .16f
    const val GRID_MAJOR = .40f
    const val BORDER = .72f
    const val GRUNGE = .035f
}

object SdoShapeTokens {
    val control: Shape @Composable get() = if (LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE) {
        CutCornerShape(topStart = 2.dp, topEnd = 14.dp, bottomStart = 10.dp)
    } else CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)

    val panel: Shape @Composable get() = if (LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE) {
        CutCornerShape(topStart = 3.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 2.dp)
    } else CutCornerShape(topEnd = 22.dp, bottomStart = 14.dp)
}

object SdoMotionTokens {
    const val RESPONSE = 180
    const val TRANSITION = 300
    const val SIGNAL_PULSE = 700
    const val TELEMETRY_SCAN = 1_100
}
