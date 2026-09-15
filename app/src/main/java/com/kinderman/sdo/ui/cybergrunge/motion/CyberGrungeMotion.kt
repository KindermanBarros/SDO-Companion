package com.kinderman.sdo.ui

import androidx.compose.animation.core.animateFloatAsState
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
