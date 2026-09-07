package com.kinderman.sdo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Fundo de Tela e Superfícies Estruturais (Dark Canvas) ---
val Void = Color(0xFF040D1B)
val VoidDeep = Color(0xFF061424)
val Panel = Color(0xFF191B1C)
val ContainmentPanel = Color(0xFF1A060F)
val ArcanePanel = Color(0xFF261E3C)
val MysticPanel = Color(0xFF3B0855)
val Carbon = Color(0xFF132B49)
val CarbonAlt = Color(0xFF183451)

// --- Malha Técnica, Divisores e Wireframes (HUD Grid) ---
val Grid = Color(0xFF383B3D)
val GridGuide = Color(0xFF595F61)
val WireframeNeutral = Color(0xFF7B8285)
val WireframeLight = Color(0xFF9BA3A8)
val TechCutDark = Color(0xFF274D7D)
val TechCut = Color(0xFF2C5784)
val TechCutCyan = Color(0xFF498099)

// --- Tipografia e Hierarquia de Leitura ---
val Ice = Color(0xFFFCFCFD)
val TextPrimary = Color(0xFFEDEFF0)
val LabelFunctional = Color(0xFFCADCF2)
val LabelLight = Color(0xFFD8E6F6)
val Muted = Color(0xFFC2C9CC)
val MetaStamp = Color(0xFFD5D1E5)
val MetalType = Color(0xFFB0A8CE)
val MetalDeep = Color(0xFF8F82BA)

// --- Módulos de Sistema, Energia e Arcano (Tech Blues & Cyans) ---
val AcidCyan = Color(0xFF30C0B7)
val Acid = AcidCyan
val Cyan = AcidCyan
val EnergyBlue = Color(0xFF5690DA)
val EnergyLight = Color(0xFF5E9CDE)
val AuraBlue = Color(0xFF91B6E6)
val AuraLight = Color(0xFF9CC1EA)
val StatHeader = Color(0xFF3B6FB0)
val StatHeaderLight = Color(0xFF407AB7)
val ArcanePassive = Color(0xFF6E5BA2)
val ArcaneLatent = Color(0xFF483B6D)

// --- Alertas, Dano e Tensão Psicológica (Acid Pinks & Corais) ---
val AcidMagenta = Color(0xFFEE227D)
val Signal = AcidMagenta
val NeonCoral = Color(0xFFFD8083)
val PenaltyPink = Color(0xFFD85E99)
val StressPink = Color(0xFFE59BBA)
val HostileHeader = Color(0xFF852467)
val InsanityPink = Color(0xFFA84876)
val DamageTrack = Color(0xFF773153)
val DamageTrackDeep = Color(0xFF46192F)
val HazardText = Color(0xFFF2D1DD)

private val hudColors = darkColorScheme(
    primary = Acid,
    onPrimary = Void,
    secondary = EnergyBlue,
    onSecondary = Void,
    tertiary = Signal,
    onTertiary = Void,
    background = Void,
    onBackground = Ice,
    surface = Panel,
    onSurface = Ice,
    surfaceVariant = Carbon,
    onSurfaceVariant = LabelFunctional,
    error = Signal,
    onError = Void,
)

private val hudTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        fontSize = 42.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        fontSize = 30.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.8).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 0.5.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
    ),
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.8.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        letterSpacing = 1.2.sp,
    ),
)

@Composable
fun SdoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = hudColors, typography = hudTypography, content = content)
}

@Composable
fun HudBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier
        .fillMaxSize()
        .background(Void)) {
        Canvas(Modifier.fillMaxSize()) {
            val minor = 12.dp.toPx()
            val major = minor * 4
            var x = 0f
            while (x <= size.width) {
                val isMajor = (x % major) < 0.5f
                drawLine(
                    color = Grid.copy(alpha = if (isMajor) 0.42f else 0.16f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (isMajor) 1.2f else 0.6f,
                )
                x += minor
            }
            var y = 0f
            while (y <= size.height) {
                val isMajor = (y % major) < 0.5f
                drawLine(
                    color = Grid.copy(alpha = if (isMajor) 0.42f else 0.16f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = if (isMajor) 1.2f else 0.6f,
                )
                y += minor
            }
        }
        content()
    }
}

@Composable
fun TechPanel(
    modifier: Modifier = Modifier,
    accent: Color = Acid,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.72f),
                shape = CutCornerShape(topEnd = 22.dp, bottomStart = 14.dp),
            ),
        shape = CutCornerShape(topEnd = 22.dp, bottomStart = 14.dp),
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = 0.96f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun SectionHeader(index: String, title: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .background(Acid, CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(index, color = Void, style = MaterialTheme.typography.labelLarge)
        }
        Text(
            title.uppercase(),
            color = Ice,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Canvas(Modifier
            .weight(1f)
            .height(9.dp)) {
            drawLine(Acid, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2f)
            drawLine(
                Signal,
                Offset(size.width * .72f, 0f),
                Offset(size.width * .64f, size.height),
                3f
            )
            drawLine(
                Signal,
                Offset(size.width * .84f, 0f),
                Offset(size.width * .76f, size.height),
                3f
            )
            drawLine(
                Signal,
                Offset(size.width * .96f, 0f),
                Offset(size.width * .88f, size.height),
                3f
            )
        }
    }
}

@Composable
fun TelemetryTag(text: String, color: Color = Acid) {
    Text(
        text = text.uppercase(),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = .65f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

@Composable
fun Barcode(seed: String, modifier: Modifier = Modifier) {
    Canvas(modifier
        .fillMaxWidth()
        .height(28.dp)) {
        val safeSeed = seed.ifEmpty { "SDO" }
        var cursor = 0f
        var index = 0
        while (cursor < size.width) {
            val code = safeSeed[index % safeSeed.length].code
            val width = ((code % 4) + 1) * 1.2.dp.toPx()
            drawRect(
                color = if (index % 7 == 0) Signal else Ice,
                topLeft = Offset(cursor, 0f),
                size = androidx.compose.ui.geometry.Size(width, size.height),
            )
            cursor += width + (((code / 3) % 3) + 1) * 0.9.dp.toPx()
            index++
        }
    }
}

@Composable
fun ComplianceMark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(28.dp)) {
            drawCircle(Acid, style = Stroke(width = 2.dp.toPx()))
            val p = Path().apply {
                moveTo(size.width * .68f, size.height * .22f)
                lineTo(size.width * .38f, size.height * .5f)
                lineTo(size.width * .68f, size.height * .78f)
            }
            drawPath(p, Acid, style = Stroke(width = 2.dp.toPx()))
        }
        Spacer(Modifier.size(6.dp))
        Column {
            Text("CE//SDO", color = Acid, style = MaterialTheme.typography.labelLarge)
            Text("CONFORMIDADE ATIVA", color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun HudTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    onValue: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label.uppercase()) },
        minLines = if (multiline) 4 else 1,
        shape = CutCornerShape(topEnd = 12.dp, bottomStart = 8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Acid,
            unfocusedBorderColor = Grid,
            focusedLabelColor = Acid,
            unfocusedLabelColor = Muted,
            focusedContainerColor = Carbon,
            unfocusedContainerColor = Carbon,
            cursorColor = Acid,
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}
