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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.CompositionLocalProvider
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
val Void: Color @Composable get() = MaterialTheme.colorScheme.background
val VoidDeep: Color @Composable get() = MaterialTheme.colorScheme.background
val Panel: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val ContainmentPanel: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val ArcanePanel: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val MysticPanel: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val Carbon: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val CarbonAlt: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant

// --- Malha Técnica, Divisores e Wireframes (HUD Grid) ---
val Grid: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val GridGuide: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val WireframeNeutral: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val WireframeLight: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val TechCutDark: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val TechCut: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val TechCutCyan: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant

// --- Tipografia e Hierarquia de Leitura ---
val Ice: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val LabelFunctional: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val LabelLight: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val MetaStamp: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val MetalType: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val MetalDeep: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

// --- Módulos de Sistema, Energia e Arcano (Tech Blues & Cyans) ---
val AcidCyan: Color @Composable get() = MaterialTheme.colorScheme.primary
val Acid: Color @Composable get() = MaterialTheme.colorScheme.primary
val Cyan: Color @Composable get() = MaterialTheme.colorScheme.primary
val EnergyBlue: Color @Composable get() = MaterialTheme.colorScheme.secondary
val EnergyLight: Color @Composable get() = MaterialTheme.colorScheme.secondary
val AuraBlue: Color @Composable get() = MaterialTheme.colorScheme.secondary
val AuraLight: Color @Composable get() = MaterialTheme.colorScheme.secondary
val StatHeader: Color @Composable get() = MaterialTheme.colorScheme.secondary
val StatHeaderLight: Color @Composable get() = MaterialTheme.colorScheme.secondary
val ArcanePassive: Color @Composable get() = MaterialTheme.colorScheme.secondary
val ArcaneLatent: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant

// --- Alertas, Dano e Tensão Psicológica (Acid Pinks & Corais) ---
val AcidMagenta: Color @Composable get() = MaterialTheme.colorScheme.error
val Signal: Color @Composable get() = MaterialTheme.colorScheme.error
val NeonCoral: Color @Composable get() = MaterialTheme.colorScheme.error
val PenaltyPink: Color @Composable get() = MaterialTheme.colorScheme.error
val StressPink: Color @Composable get() = MaterialTheme.colorScheme.error
val HostileHeader: Color @Composable get() = MaterialTheme.colorScheme.error
val InsanityPink: Color @Composable get() = MaterialTheme.colorScheme.error
val DamageTrack: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val DamageTrackDeep: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val HazardText: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val hudColors = darkColorScheme(
    primary = Color(0xFF30C0B7),
    onPrimary = Color(0xFF040D1B),
    secondary = Color(0xFF5690DA),
    onSecondary = Color(0xFF040D1B),
    tertiary = Color(0xFFEE227D),
    onTertiary = Color(0xFF040D1B),
    background = Color(0xFF040D1B),
    onBackground = Color(0xFFFCFCFD),
    surface = Color(0xFF191B1C),
    onSurface = Color(0xFFFCFCFD),
    surfaceVariant = Color(0xFF132B49),
    onSurfaceVariant = Color(0xFFCADCF2),
    error = Color(0xFFEE227D),
    onError = Color(0xFF040D1B),
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
    primary = Color(0xFFFF9A00), onPrimary = Color(0xFF2B1700),
    secondary = Color(0xFF27A7D8), onSecondary = Color(0xFF001D2D),
    tertiary = Color(0xFF27A7D8), onTertiary = Color(0xFF001D2D),
    background = Color(0xFFFFFFFF), onBackground = Color(0xFF10202A),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF10202A),
    surfaceVariant = Color(0xFFF3F0F0), onSurfaceVariant = Color(0xFF514A4A),
    outline = Color(0xFF8E8585), outlineVariant = Color(0xFFB5AAAA),
    error = Color(0xFFD45500), onError = Color.White,
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
    val baseColors = when (preferences.theme) {
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
    val colors = baseColors.copy(
        surfaceContainerHigh = baseColors.surface,
        surfaceContainer = baseColors.surface,
        surfaceContainerHighest = baseColors.surfaceVariant,
        outline = baseColors.onSurfaceVariant.copy(alpha = 0.7f),
        outlineVariant = baseColors.onSurfaceVariant.copy(alpha = 0.35f),
    )
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
        shapes = androidx.compose.material3.Shapes(
            extraSmall = CutCornerShape(4.dp), small = CutCornerShape(8.dp),
            medium = CutCornerShape(12.dp), large = CutCornerShape(16.dp),
            extraLarge = CutCornerShape(22.dp),
        ),
        content = {
            androidx.compose.runtime.CompositionLocalProvider(LocalSdoPreferences provides preferences, content = content)
        },
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
    if (LocalFlattenCollapsiblePanel.current) {
        CompositionLocalProvider(LocalFlattenCollapsiblePanel provides false) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
        return
    }

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
        CompositionLocalProvider(LocalCollapsibleSectionTitle provides null) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

@Composable
fun SectionHeader(index: String, title: String, modifier: Modifier = Modifier) {
    if (LocalCollapsibleSectionTitle.current != null) return

    val secondary = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.primary
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
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Canvas(Modifier
            .size(width = 24.dp, height = 9.dp)) {
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
    val signal = MaterialTheme.colorScheme.primary
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HudTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    placeholder: String? = null,
    enabled: Boolean = true,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    onValue: (String) -> Unit,
) {
    val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val compact = LocalSdoWindowClass.current == SdoWindowClass.COMPACT ||
        LocalSdoPreferences.current.density == SdoContentDensity.COMPACT
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    )
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier.fillMaxWidth().then(Modifier.heightIn(min = if (compact) 48.dp else 56.dp)),
        interactionSource = interaction,
        keyboardOptions = keyboardOptions,
        singleLine = !multiline,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        minLines = if (multiline) 3 else 1,
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        decorationBox = { inner ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value, innerTextField = inner, enabled = enabled,
                singleLine = !multiline,
                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                interactionSource = interaction,
                label = { Text(label, maxLines = 1, style = MaterialTheme.typography.bodySmall) },
                placeholder = placeholder?.let { hint -> { Text(hint, style = MaterialTheme.typography.bodyMedium) } },
                colors = colors,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled, isError = false, interactionSource = interaction,
                        colors = colors, shape = CutCornerShape(topEnd = 12.dp, bottomStart = 8.dp),
                    )
                },
            )
        },
    )
}
