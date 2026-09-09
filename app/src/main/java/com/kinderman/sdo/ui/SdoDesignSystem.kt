package com.kinderman.sdo.ui

import android.app.Activity
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinderman.sdo.R
import androidx.core.view.WindowCompat

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

private val highContrastColors = darkColorScheme(
    primary = Color(0xFF63FFF1),
    onPrimary = Color.Black,
    secondary = Color(0xFFA9CBFF),
    onSecondary = Color.Black,
    tertiary = Color(0xFFFF70AE),
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF101214),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF162B45),
    onSurfaceVariant = Color.White,
    error = Color(0xFFFF70AE),
    onError = Color.Black,
)

private val arcaneColors = darkColorScheme(
    primary = Color(0xFFBCA8FF),
    onPrimary = Color(0xFF100624),
    secondary = Color(0xFF69E6DC),
    onSecondary = Color(0xFF061B1B),
    tertiary = Color(0xFFFF8DBD),
    onTertiary = Color(0xFF2B0014),
    background = Color(0xFF0D0719),
    onBackground = Color(0xFFF8F2FF),
    surface = Color(0xFF21172F),
    onSurface = Color(0xFFF8F2FF),
    surfaceVariant = Color(0xFF33244B),
    onSurfaceVariant = Color(0xFFE5D9FF),
    error = Color(0xFFFF8DBD),
    onError = Color(0xFF2B0014),
)

private val terminalColors = darkColorScheme(
    primary = Color(0xFF70FF9A), onPrimary = Color(0xFF001B08), secondary = Color(0xFFB7FFCA),
    onSecondary = Color(0xFF001B08), tertiary = Color(0xFFFFD166), onTertiary = Color(0xFF241A00),
    background = Color(0xFF030D07), onBackground = Color(0xFFE8FFEE), surface = Color(0xFF0D1C12),
    onSurface = Color(0xFFE8FFEE), surfaceVariant = Color(0xFF183622), onSurfaceVariant = Color(0xFFC9F5D5),
    error = Color(0xFFFF7A9E), onError = Color.Black,
)

private val crimsonColors = darkColorScheme(
    primary = Color(0xFFFF866E), onPrimary = Color(0xFF2D0500), secondary = Color(0xFFFFC06B),
    onSecondary = Color(0xFF281500), tertiary = Color(0xFFFF75AA), onTertiary = Color(0xFF300016),
    background = Color(0xFF150605), onBackground = Color(0xFFFFF1ED), surface = Color(0xFF2A1210),
    onSurface = Color(0xFFFFF1ED), surfaceVariant = Color(0xFF4A211D), onSurfaceVariant = Color(0xFFFFD4CA),
    error = Color(0xFFFF75AA), onError = Color(0xFF300016),
)

private val systemLightColors = lightColorScheme(
    primary = Color(0xFF006A64), onPrimary = Color.White, secondary = Color(0xFF3D6374),
    onSecondary = Color.White, tertiary = Color(0xFF8B1551), onTertiary = Color.White,
    background = Color(0xFFF5FAFA), onBackground = Color(0xFF101C1B), surface = Color.White,
    onSurface = Color(0xFF101C1B), surfaceVariant = Color(0xFFDCE8E6), onSurfaceVariant = Color(0xFF3F4947),
    error = Color(0xFFBA1A1A), onError = Color.White,
)

private val edgerunnersColors = darkColorScheme(
    primary = Color(0xFFFCEE09), onPrimary = Color(0xFF111111),
    secondary = Color(0xFF00F0FF), onSecondary = Color(0xFF001417),
    tertiary = Color(0xFFFF003C), onTertiary = Color.White,
    background = Color(0xFF050A18), onBackground = Color(0xFFF7F7F2),
    surface = Color(0xFF111827), onSurface = Color(0xFFF7F7F2),
    surfaceVariant = Color(0xFF1B2A41), onSurfaceVariant = Color(0xFFD8E5F2),
    error = Color(0xFFFF003C), onError = Color.White,
)

private val magentaDreamColors = darkColorScheme(
    primary = Color(0xFFFF2AA1), onPrimary = Color(0xFF250016),
    secondary = Color(0xFF8F7CFF), onSecondary = Color(0xFF100638),
    tertiary = Color(0xFF5CF7E8), onTertiary = Color(0xFF00201D),
    background = Color(0xFF110713), onBackground = Color(0xFFFFF3FA),
    surface = Color(0xFF261126), onSurface = Color(0xFFFFF3FA),
    surfaceVariant = Color(0xFF42203F), onSurfaceVariant = Color(0xFFF5CCE8),
    error = Color(0xFFFF6B82), onError = Color(0xFF310008),
)

private val apertureWhiteColors = lightColorScheme(
    primary = Color(0xFFF47B20), onPrimary = Color(0xFF241000),
    secondary = Color(0xFF164B73), onSecondary = Color.White,
    tertiary = Color(0xFF00A6D6), onTertiary = Color(0xFF001E2A),
    background = Color(0xFFF4F7F8), onBackground = Color(0xFF101B24),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF101B24),
    surfaceVariant = Color(0xFFE3EAF0), onSurfaceVariant = Color(0xFF344956),
    outline = Color(0xFF607786), outlineVariant = Color(0xFFAAB9C2),
    error = Color(0xFFB3261E), onError = Color.White,
)

val TechInterfaceFont = FontFamily(
    Font(R.font.oxanium_variable, weight = FontWeight.Normal),
)

val RawDisplayFont = FontFamily(
    Font(R.font.mb_forever_raw, weight = FontWeight.Normal),
)

object SdoMotionTokens {
    const val RESPONSE = 180
    const val TRANSITION = 300
    const val SIGNAL_PULSE = 700
    const val TELEMETRY_SCAN = 1_100
}

private fun hudTypography(scale: Float) = Typography(
    displayLarge = TextStyle(
        fontFamily = RawDisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = (42 * scale).sp,
        lineHeight = (50 * scale).sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = RawDisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = (34 * scale).sp,
        lineHeight = (44 * scale).sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = TechInterfaceFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = (18 * scale).sp,
        letterSpacing = 0.5.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = TechInterfaceFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14 * scale).sp,
        letterSpacing = 1.sp,
    ),
    bodyLarge = TextStyle(fontFamily = TechInterfaceFont, fontSize = (16 * scale).sp, lineHeight = (23 * scale).sp),
    bodyMedium = TextStyle(fontFamily = TechInterfaceFont, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp),
    bodySmall = TextStyle(fontFamily = TechInterfaceFont, fontSize = (12 * scale).sp, lineHeight = (17 * scale).sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = (12 * scale).sp,
        letterSpacing = 1.8.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = (11 * scale).sp,
        letterSpacing = 1.sp,
    ),
)

@Composable
fun SdoTheme(
    preferences: SdoPreferences = SdoPreferences(),
    content: @Composable () -> Unit,
) {
    val colors = when (preferences.theme) {
        SdoThemeVariant.CYAN_INDUSTRIAL -> hudColors
        SdoThemeVariant.GREEN_TERMINAL -> terminalColors
        SdoThemeVariant.CRIMSON_ARCANE -> crimsonColors
        SdoThemeVariant.VIOLET_DREAM -> arcaneColors
        SdoThemeVariant.EDGERUNNERS -> edgerunnersColors
        SdoThemeVariant.MAGENTA_DREAM -> magentaDreamColors
        SdoThemeVariant.APERTURE_WHITE -> apertureWhiteColors
        SdoThemeVariant.HIGH_CONTRAST -> highContrastColors
        SdoThemeVariant.SYSTEM -> if (isSystemInDarkTheme()) hudColors else systemLightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = colors.background.toArgb()
        window.navigationBarColor = colors.surface.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = colors.background.luminance() > 0.5f
            isAppearanceLightNavigationBars = colors.surface.luminance() > 0.5f
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = hudTypography(preferences.fontScale.multiplier),
        content = content,
    )
}

@Composable
fun HudBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Canvas(Modifier.fillMaxSize()) {
            val minor = 12.dp.toPx()
            val major = minor * 4
            var x = 0f
            while (x <= size.width) {
                val isMajor = (x % major) < 0.5f
                drawLine(
                    color = gridColor.copy(alpha = if (isMajor) 0.40f else 0.16f),
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
                    color = gridColor.copy(alpha = if (isMajor) 0.40f else 0.16f),
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
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedAccent = accent ?: MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = resolvedAccent.copy(alpha = 0.72f),
                shape = CutCornerShape(topEnd = 22.dp, bottomStart = 14.dp),
            ),
        shape = CutCornerShape(topEnd = 22.dp, bottomStart = 14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
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
    val secondary = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.error
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.primary, CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(index, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
        }
        Text(
            title.uppercase(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Canvas(Modifier
            .weight(1f)
            .height(9.dp)) {
            drawLine(secondary, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2f)
            drawLine(
                error,
                Offset(size.width * .72f, 0f),
                Offset(size.width * .64f, size.height),
                3f
            )
            drawLine(
                error,
                Offset(size.width * .84f, 0f),
                Offset(size.width * .76f, size.height),
                3f
            )
            drawLine(
                error,
                Offset(size.width * .96f, 0f),
                Offset(size.width * .88f, size.height),
                3f
            )
        }
    }
}

@Composable
fun TelemetryTag(text: String, color: Color? = null) {
    val resolved = color ?: MaterialTheme.colorScheme.primary
    Text(
        text = text.uppercase(),
        color = resolved,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .border(1.dp, resolved.copy(alpha = .65f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

@Composable
fun Barcode(seed: String, modifier: Modifier = Modifier) {
    val signal = MaterialTheme.colorScheme.error
    val neutral = MaterialTheme.colorScheme.outline
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
                color = if (index % 7 == 0) signal else neutral,
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
    val primary = MaterialTheme.colorScheme.primary
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(28.dp)) {
            drawCircle(primary, style = Stroke(width = 2.dp.toPx()))
            val p = Path().apply {
                moveTo(size.width * .68f, size.height * .22f)
                lineTo(size.width * .38f, size.height * .5f)
                lineTo(size.width * .68f, size.height * .78f)
            }
            drawPath(p, primary, style = Stroke(width = 2.dp.toPx()))
        }
        Spacer(Modifier.size(6.dp))
        Column {
            Text("CE//SDO", color = primary, style = MaterialTheme.typography.labelLarge)
            Text("CONFORMIDADE ATIVA", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun HudTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    placeholder: String? = null,
    enabled: Boolean = true,
    onValue: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label.uppercase()) },
        placeholder = placeholder?.let { hint -> { Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        minLines = if (multiline) 4 else 1,
        enabled = enabled,
        shape = CutCornerShape(topEnd = 12.dp, bottomStart = 8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}
