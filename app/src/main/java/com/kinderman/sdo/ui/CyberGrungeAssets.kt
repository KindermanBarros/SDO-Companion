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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Static, code-native assets inspired by corrupted field terminals and printed zines. */
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
        repeat(22) { index ->
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
    Text(
        "SIGNAL//UNSTABLE  ◉  REC",
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = .46f),
        style = MaterialTheme.typography.labelSmall,
    )
    Text(
        "ERROR_TRACE  00:00:00  //  SDO FIELD OS",
        modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = .24f),
        style = MaterialTheme.typography.labelSmall,
    )
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
        Text(
            "FILE.404//CORRUPTED",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
        )
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
            drawRect(signal.copy(alpha = .14f))
            val head = Offset(size.width * .5f, size.height * .39f)
            drawCircle(ink.copy(alpha = .7f), size.width * .29f, head, style = Stroke(2.dp.toPx()))
            drawLine(signal, head + Offset(-18.dp.toPx(), -4.dp.toPx()), head + Offset(-5.dp.toPx(), 4.dp.toPx()), 3.dp.toPx())
            drawLine(signal, head + Offset(5.dp.toPx(), 4.dp.toPx()), head + Offset(18.dp.toPx(), -4.dp.toPx()), 3.dp.toPx())
            repeat(6) { tooth ->
                val x = size.width * .3f + tooth * size.width * .08f
                drawLine(ink.copy(alpha = .65f), Offset(x, size.height * .58f), Offset(x, size.height * .72f), 1.dp.toPx())
            }
            repeat(8) { glitch ->
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
            Text("REALITY_FEED // $metadata", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(11) { index ->
                    Box(Modifier.weight(if (index % 4 == 0) 2f else 1f).height(3.dp).background(if (index < 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
                }
            }
        }
    }
}
