package com.kinderman.sdo.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader

enum class CyberGrungeIntensity(val shaderAmount: Float) {
    LATENT(.18f),
    INTERFERENCE(.52f),
    RUPTURE(1f),
}

val LocalCyberGrungeIntensity = compositionLocalOf { CyberGrungeIntensity.LATENT }

/**
 * One coordinated render layer for scan, RGB separation and horizontal framebuffer tears.
 * RuntimeShader is used where Android supports AGSL; older supported devices keep the Canvas fallback.
 */
@Composable
internal fun CyberGrungeShaderLayer(
    modifier: Modifier = Modifier,
    intensity: CyberGrungeIntensity = LocalCyberGrungeIntensity.current,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslCorruption(modifier, intensity)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslCorruption(modifier: Modifier, intensity: CyberGrungeIntensity) {
    val transition = rememberInfiniteTransition(label = "cybergrunge-engine")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cybergrunge-engine-time",
    )
    val shader = remember { RuntimeShader(CORRUPTION_SHADER) }
    val brush = remember(shader) { ShaderBrush(shader.asComposeShader()) }

    Canvas(modifier.fillMaxSize()) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("amount", intensity.shaderAmount)
        drawRect(brush)
    }
}

private const val CORRUPTION_SHADER = """
uniform float2 resolution;
uniform float time;
uniform float amount;

float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;
    float band = floor(uv.y * 54.0);
    float tearGate = step(0.84 - amount * 0.12, hash(float2(band, floor(time * 5.0))));
    float tear = tearGate * (hash(float2(band + 7.0, floor(time * 7.0))) - 0.5) * amount;
    float scan = 0.5 + 0.5 * sin((fragCoord.y + time * 46.0) * 1.75);
    float phosphor = 0.5 + 0.5 * sin(time * 5.0 + uv.y * 19.0);
    float red = step(0.985 - amount * 0.012, hash(float2(floor((uv.x + tear) * 92.0), band)));
    float cyan = step(0.988 - amount * 0.009, hash(float2(floor((uv.x - tear) * 86.0), band + 13.0)));
    float alpha = (scan * 0.025 + tearGate * 0.055 + phosphor * 0.018) * amount;
    half3 color = half3(red * 0.92, cyan * 0.7, cyan * 0.76);
    return half4(color, half(clamp(alpha + (red + cyan) * 0.035 * amount, 0.0, 0.16)));
}
"""
