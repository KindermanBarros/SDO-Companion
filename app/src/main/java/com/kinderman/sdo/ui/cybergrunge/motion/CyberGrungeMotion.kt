package com.kinderman.sdo.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Finite, interaction-bound compression; it stops as soon as the press ends or leaves composition. */
@Composable
internal fun Modifier.cyberGrungePress(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .965f else 1f,
        animationSpec = tween(SdoMotionTokens.RESPONSE),
        label = "cybergrunge-press",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}
