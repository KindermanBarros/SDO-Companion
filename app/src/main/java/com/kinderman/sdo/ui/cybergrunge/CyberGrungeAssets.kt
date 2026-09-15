package com.kinderman.sdo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Code-native assets for the experimental Cybergrunge terminal interface. */
@Composable
internal fun CyberGrungeBackdrop(modifier: Modifier = Modifier) {
    val signal = MaterialTheme.colorScheme.primary
    val telemetry = MaterialTheme.colorScheme.secondary
    val ink = MaterialTheme.colorScheme.onBackground
    Canvas(modifier.fillMaxSize()) {
        val scan = 5.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(ink.copy(alpha = .025f), Offset(0f, y), Offset(size.width, y), 1f)
            y += scan
        }

        val block = 18.dp.toPx()
        repeat(CyberGrungeTokens.BACKDROP_BLOCKS) { index ->
            val column = ((index * 47) % 19) / 19f
            val row = ((index * 83) % 31) / 31f
            val width = block * (1 + index % 4)
            drawRect(
                color = if (index % 3 == 0) signal.copy(alpha = .065f) else ink.copy(alpha = .04f),
                topLeft = Offset(size.width * column, size.height * row),
                size = Size(width.coerceAtMost(size.width * .22f), block * (1 + index % 2)),
            )
        }

        val center = Offset(size.width * .86f, size.height * .16f)
        drawCircle(signal.copy(alpha = .14f), size.minDimension * .115f, center, style = Stroke(1.dp.toPx()))
        drawCircle(telemetry.copy(alpha = .1f), size.minDimension * .08f, center, style = Stroke(1.dp.toPx()))
        drawLine(signal.copy(alpha = .18f), center - Offset(size.minDimension * .14f, 0f), center + Offset(size.minDimension * .14f, 0f), 1.dp.toPx())
        drawLine(signal.copy(alpha = .18f), center - Offset(0f, size.minDimension * .14f), center + Offset(0f, size.minDimension * .14f), 1.dp.toPx())

        val fracture = Path().apply {
            moveTo(size.width * .08f, size.height * .72f)
            lineTo(size.width * .19f, size.height * .62f)
            lineTo(size.width * .23f, size.height * .68f)
            lineTo(size.width * .37f, size.height * .52f)
            lineTo(size.width * .45f, size.height * .57f)
        }
        drawPath(fracture, ink.copy(alpha = .11f), style = Stroke(1.dp.toPx()))
        repeat(5) { branch ->
            val start = Offset(size.width * (.19f + branch * .045f), size.height * (.62f - branch * .025f))
            drawLine(ink.copy(alpha = .08f), start, start + Offset(28.dp.toPx(), (-18 + branch * 8).dp.toPx()), 1f)
        }
    }
}

@Composable
internal fun BoxScope.CyberGrungeEdgeMarks() {
    Box(
        Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp)
            .width(42.dp).height(3.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .46f)),
    )
    Box(
        Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp)
            .width(72.dp).height(2.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha = .24f)),
    )
}

@Composable
internal fun BoxScope.CyberGrungeGhostNumbers() {
    val transition = rememberInfiniteTransition(label = "possessed-background-data")
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_600, easing = LinearEasing), RepeatMode.Reverse),
        label = "possessed-background-drift",
    )
    val numbers = listOf("404", "13", "0XDEAD", "77", "NULL", "666", "//31")
    numbers.forEachIndexed { index, value ->
        Text(
            text = value,
            color = if (index % 3 == 0) MaterialTheme.colorScheme.primary.copy(alpha = .12f)
            else MaterialTheme.colorScheme.onBackground.copy(alpha = .055f),
            fontSize = (42 + index % 3 * 28).sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(if (index % 2 == 0) Alignment.TopEnd else Alignment.BottomStart)
                .padding(
                    top = (24 + index * 67).dp,
                    end = (8 + index * 13).dp,
                    bottom = (18 + index * 39).dp,
                    start = (6 + index * 17).dp,
                )
                .graphicsLayer {
                    translationX = drift * (12f + index * 3f) * if (index % 2 == 0) 1f else -1f
                    translationY = drift * (4f + index)
                    rotationZ = if (index % 2 == 0) -90f else 0f
                },
        )
    }
}

@Composable
internal fun CyberGrungePanelChrome() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(7) { index ->
                Box(
                    Modifier
                        .width(if (index % 3 == 0) 15.dp else 5.dp)
                        .height(4.dp)
                        .background(if (index < 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                )
            }
        }
        Box(Modifier.width(28.dp).height(4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

@Composable
fun ExperimentalBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(Color(0xFFFFD400), SdoShapeTokens.control)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            "EXPERIMENTAL",
            color = Color(0xFF090608),
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun SdoScreenMasthead(
    eyebrow: String,
    title: String,
    metadata: String,
    modifier: Modifier = Modifier,
) {
    if (LocalSdoPreferences.current.visualMode != SdoVisualMode.CYBERGRUNGE) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(eyebrow, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text(title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineLarge)
            Text(metadata, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        return
    }
    val signal = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .background(surface.copy(alpha = .82f), SdoShapeTokens.panel)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(width = 88.dp, height = 108.dp)) {
            drawRect(signal.copy(alpha = .12f))
            repeat(17) { line ->
                val y = (5 + line * 6).dp.toPx()
                val start = if (line % 4 == 0) 0f else ((line * 11) % 26).dp.toPx()
                drawLine(
                    if (line % 3 == 0) signal.copy(alpha = .88f) else ink.copy(alpha = .5f),
                    Offset(start, y),
                    Offset(size.width - ((line * 7) % 31).dp.toPx(), y),
                    if (line % 5 == 0) 3.dp.toPx() else 1.dp.toPx(),
                )
            }
            drawLine(signal, Offset(4.dp.toPx(), 6.dp.toPx()), Offset(size.width - 3.dp.toPx(), size.height - 8.dp.toPx()), 3.dp.toPx())
            drawLine(ink.copy(alpha = .65f), Offset(size.width - 6.dp.toPx(), 4.dp.toPx()), Offset(8.dp.toPx(), size.height - 5.dp.toPx()), 1.dp.toPx())
            repeat(14) { glitch ->
                drawRect(
                    if (glitch % 2 == 0) signal.copy(alpha = .7f) else ink.copy(alpha = .35f),
                    Offset(((glitch * 29) % 80).dp.toPx(), ((glitch * 17) % 104).dp.toPx()),
                    Size((5 + glitch % 3 * 5).dp.toPx(), 3.dp.toPx()),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(eyebrow, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                ExperimentalBadge()
            }
            Text(title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineLarge)
            Text(metadata, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(11) { index ->
                    Box(Modifier.weight(if (index % 4 == 0) 2f else 1f).height(3.dp).background(if (index < 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
                }
            }
        }
    }
}
