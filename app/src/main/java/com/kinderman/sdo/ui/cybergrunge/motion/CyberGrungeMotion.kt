package com.kinderman.sdo.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
import kotlin.math.sin

/** Cybergrunge uses channel displacement and compression for touch feedback, never Material ripples. */
internal object CyberGrungeNoRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = NoRippleNode()
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = javaClass.name.hashCode()

    private class NoRippleNode : Modifier.Node(), DrawModifierNode {
        override fun ContentDrawScope.draw() = drawContent()
    }
}

/** Finite, interaction-bound compression; it stops as soon as the press ends or leaves composition. */
@Composable
internal fun Modifier.cyberGrungePress(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .965f else 1f,
        animationSpec = tween(SdoMotionTokens.RESPONSE),
        label = "cybergrunge-press",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationX = if (pressed) 2f else 0f
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.18f, .5f)
    }
}

internal data class CyberGrungePanelMotion(
    val translationX: Float,
    val translationY: Float,
    val rotation: Float,
    val direction: Int,
    val rupture: Float,
)

internal fun cyberGrungeDirectionFor(translationX: Float): Int = if (translationX < 0f) -1 else 1

/** One motion clock drives both the card transform and its directional edge corruption. */
@Composable
internal fun rememberCyberGrungePanelMotion(seed: Int): CyberGrungePanelMotion {
    val transition = rememberInfiniteTransition(label = "possessed-panel-$seed")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_350 + kotlin.math.abs(seed % 700), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "possessed-panel-phase-$seed",
    )
    val rupture by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(110 + kotlin.math.abs(seed % 90), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "possessed-panel-rupture-$seed",
    )
    val slow = sin(phase + seed * .013f)
    val spike = if (sin(phase * 3.7f + seed) > .9f) rupture else 0f
    val x = slow * 1.7f + spike * 4.2f
    return CyberGrungePanelMotion(
        translationX = x,
        translationY = sin(phase * 1.43f + seed * .021f) * 1.15f,
        rotation = slow * .11f + spike * .18f,
        direction = cyberGrungeDirectionFor(x),
        rupture = abs(spike),
    )
}

internal fun Modifier.cyberGrungePossessed(motion: CyberGrungePanelMotion): Modifier = graphicsLayer {
    translationX = motion.translationX
    translationY = motion.translationY
    rotationZ = motion.rotation
    scaleX = 1f + motion.rupture * .0025f
}
