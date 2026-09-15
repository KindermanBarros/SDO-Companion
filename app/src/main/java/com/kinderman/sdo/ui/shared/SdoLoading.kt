package com.kinderman.sdo.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class CyberLoadingMode(
    val code: String,
    val title: String,
    val detail: String,
    val accessibilityLabel: String,
) {
    AUTH_BOOT(
        code = "AUTH.00 // HANDSHAKE",
        title = "INICIALIZANDO IDENTIDADE",
        detail = "VALIDANDO SESSÃO SEGURA",
        accessibilityLabel = "Inicializando autenticação",
    ),
    GOOGLE_AUTH(
        code = "AUTH.01 // GOOGLE",
        title = "AUTENTICANDO OPERADOR",
        detail = "AGUARDANDO CREDENCIAL CRIPTOGRÁFICA",
        accessibilityLabel = "Autenticando com Google",
    ),
    CHARACTERS(
        code = "DATA.13 // FIREBASE",
        title = "CARREGANDO PERSONAGENS",
        detail = "CONCILIANDO CACHE LOCAL E ARQUIVO REMOTO",
        accessibilityLabel = "Carregando personagens",
    ),
}

@Composable
fun CyberLoadingScreen(mode: CyberLoadingMode, modifier: Modifier = Modifier) {
    HudBackground(modifier) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(22.dp)
                .widthIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TelemetryTag(mode.code)
            TechPanel(accent = MaterialTheme.colorScheme.primary) {
                Text(mode.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                CyberLoadingIndicator(mode.accessibilityLabel)
                Text(mode.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Barcode("${mode.code}-${mode.title}")
            }
        }
    }
}

@Composable
fun CyberLoadingIndicator(
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val transition = rememberInfiniteTransition(label = "cyber-loading")
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SdoMotionTokens.TELEMETRY_SCAN, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "telemetry-scan",
    )
    val pulse by transition.animateFloat(
        initialValue = .38f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SdoMotionTokens.SIGNAL_PULSE, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "signal-pulse",
    )

    Canvas(
        modifier
            .fillMaxWidth()
            .height(92.dp)
            .progressSemantics()
            .semantics { contentDescription = accessibilityLabel },
    ) {
        val border = 1.dp.toPx()
        drawRect(outline.copy(alpha = .72f), style = Stroke(border))
        drawLine(outline.copy(alpha = .55f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), border)

        val segmentCount = 15
        val gap = 4.dp.toPx()
        val segmentWidth = (size.width - gap * (segmentCount + 1)) / segmentCount
        repeat(segmentCount) { index ->
            val center = (index + .5f) / segmentCount
            val distance = abs(center - scan)
            val energy = (1f - distance * 5f).coerceIn(.12f, 1f)
            drawRect(
                color = if (index % 5 == 4) error.copy(alpha = energy * pulse) else primary.copy(alpha = energy),
                topLeft = Offset(gap + index * (segmentWidth + gap), size.height * .28f),
                size = Size(segmentWidth, size.height * .44f),
            )
        }

        val scannerX = scan * size.width
        drawLine(
            color = onSurface.copy(alpha = .45f * pulse),
            start = Offset(scannerX, 0f),
            end = Offset(scannerX, size.height),
            strokeWidth = 2.dp.toPx(),
        )
    }
}
