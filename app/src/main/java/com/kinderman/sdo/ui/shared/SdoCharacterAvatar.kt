package com.kinderman.sdo.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.R
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_AVATAR_DIMENSION_PX = 256
private const val MAX_AVATAR_THUMBNAIL_BYTES = 65_536
private val AVATAR_DIMENSION_STEPS = listOf(MAX_AVATAR_DIMENSION_PX, 192, 144, 112, 96)
private val AVATAR_COMPRESSION_STEPS = listOf(82, 70, 58, 46, 34, 24)

enum class SdoAvatarPreset(val uri: String, @StringRes val labelRes: Int) {
    WARRIOR("preset:warrior", R.string.avatar_preset_warrior),
    SCHOLAR("preset:scholar", R.string.avatar_preset_scholar),
    MYSTIC("preset:mystic", R.string.avatar_preset_mystic),
    SCOUT("preset:scout", R.string.avatar_preset_scout),
    GUARDIAN("preset:guardian", R.string.avatar_preset_guardian),
    ALCHEMIST("preset:alchemist", R.string.avatar_preset_alchemist),
}

fun resolveAvatarPreset(photoUri: String): SdoAvatarPreset? {
    val normalized = photoUri.trim().lowercase()
    return SdoAvatarPreset.entries.firstOrNull { it.uri == normalized }
}

fun isAvatarThumbnailWithinSyncBudget(photoUri: String): Boolean =
    photoUri.startsWith("data:image/", ignoreCase = true) &&
        photoUri.toByteArray(Charsets.UTF_8).size <= MAX_AVATAR_THUMBNAIL_BYTES

/**
 * Character portrait frame supporting Standard/Cybergrunge cut-corner geometry,
 * predefined SDO vector presets, local/thumbnail photos, and circular coin-token mode.
 */
@Composable
fun SdoCharacterAvatar(
    photoUri: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    coinToken: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val context = LocalContext.current
    val cybergrunge = LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE
    val corner = (size * 0.22f).coerceIn(6.dp, 16.dp)
    val shape = if (coinToken) CircleShape else CutCornerShape(topEnd = corner, bottomStart = corner)
    val borderWidth = if (coinToken) 2.dp else if (cybergrunge) 1.5.dp else 1.dp
    val preset = remember(photoUri) { resolveAvatarPreset(photoUri) }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photoUri, preset) {
        value = if (photoUri.isBlank() || preset != null) {
            null
        } else {
            withContext(Dispatchers.IO) { decodeAvatarBitmap(context, photoUri) }
        }
    }
    val initials = remember(name) { characterInitials(name) }
    val cleanName = name.ifBlank { stringResource(R.string.avatar_default_character_name) }
    val description = if (preset != null) {
        stringResource(R.string.avatar_portrait_description_preset, cleanName, stringResource(preset.labelRes))
    } else {
        stringResource(R.string.avatar_portrait_description, cleanName)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .border(borderWidth, accent, shape)
            .then(
                if (coinToken) {
                    Modifier.padding(2.dp).border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bitmap
        when {
            preset != null -> SdoPresetEmblem(
                preset = preset,
                accent = accent,
                modifier = Modifier.fillMaxSize(),
            )
            loaded != null -> Image(
                bitmap = loaded,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape),
            )
            else -> {
                val textStyle = when {
                    size >= 64.dp -> MaterialTheme.typography.headlineSmall
                    size >= 44.dp -> MaterialTheme.typography.titleMedium
                    else -> MaterialTheme.typography.labelLarge
                }
                Text(
                    text = initials,
                    color = accent,
                    style = textStyle,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Editable portrait picker offering synced in-app SDO presets and local photo selection
 * compressed within the <= 64 KiB Spark v2 avatar thumbnail budget.
 */
@Composable
fun SdoCharacterPhotoPicker(
    photoUri: String,
    name: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onPhotoChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var processing by remember { mutableStateOf(false) }
    var showCompressionError by remember { mutableStateOf(false) }
    val selectedPreset = remember(photoUri) { resolveAvatarPreset(photoUri) }
    val thumbnailSizeBytes = remember(photoUri) {
        if (photoUri.startsWith("data:image/", ignoreCase = true)) {
            photoUri.toByteArray(Charsets.UTF_8).size
        } else {
            0
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        processing = true
        showCompressionError = false
        scope.launch {
            val encoded = withContext(Dispatchers.IO) { encodePickedAvatar(context, uri) }
            processing = false
            if (encoded != null) {
                onPhotoChange(encoded)
            } else {
                showCompressionError = true
            }
        }
    }

    SdoInsetCard(modifier = modifier.fillMaxWidth(), verticalSpacing = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SdoCharacterAvatar(
                photoUri = photoUri,
                name = name,
                size = 72.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.avatar_picker_title),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                SdoMetadataText(
                    text = when {
                        processing -> stringResource(R.string.avatar_picker_status_processing)
                        selectedPreset != null -> stringResource(R.string.avatar_picker_status_preset, stringResource(selectedPreset.labelRes))
                        thumbnailSizeBytes in 1..MAX_AVATAR_THUMBNAIL_BYTES ->
                            stringResource(R.string.avatar_picker_status_thumbnail, (thumbnailSizeBytes + 1023) / 1024)
                        photoUri.isNotBlank() ->
                            stringResource(R.string.avatar_picker_status_local_photo)
                        !enabled -> stringResource(R.string.avatar_picker_status_read_only)
                        else -> stringResource(R.string.avatar_picker_status_choose)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (showCompressionError) {
            Text(
                text = stringResource(R.string.avatar_picker_compression_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (enabled) {
            Text(
                text = stringResource(R.string.avatar_picker_presets_title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            SdoResponsiveGrid(
                items = SdoAvatarPreset.entries,
                minItemWidth = 128.dp,
                maxColumns = 3,
            ) { preset, itemModifier ->
                val isSelected = selectedPreset == preset
                SdoOutlinedButton(
                    onClick = {
                        showCompressionError = false
                        onPhotoChange(preset.uri)
                    },
                    modifier = itemModifier,
                    enabled = !processing,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SdoCharacterAvatar(
                            photoUri = preset.uri,
                            name = stringResource(preset.labelRes),
                            size = 28.dp,
                        )
                        Text(
                            text = stringResource(preset.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            SdoResponsiveGrid(
                items = if (photoUri.isNotBlank()) listOf("PICK", "REMOVE") else listOf("PICK"),
                minItemWidth = 132.dp,
            ) { action, itemModifier ->
                when (action) {
                    "PICK" -> SdoActionButton(
                        label = stringResource(if (photoUri.isNotBlank()) R.string.avatar_picker_change_photo else R.string.avatar_picker_add_photo),
                        onClick = {
                            launcher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = itemModifier,
                        enabled = !processing,
                    )
                    else -> SdoActionButton(
                        label = stringResource(R.string.avatar_picker_remove_photo),
                        onClick = {
                            showCompressionError = false
                            onPhotoChange("")
                        },
                        modifier = itemModifier,
                        enabled = !processing,
                        style = SdoActionStyle.DESTRUCTIVE,
                    )
                }
            }
        }
    }
}

@Composable
private fun SdoPresetEmblem(
    preset: SdoAvatarPreset,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface
    val emblemColor = when (preset) {
        SdoAvatarPreset.WARRIOR -> primary
        SdoAvatarPreset.SCHOLAR -> secondary
        SdoAvatarPreset.MYSTIC -> tertiary
        SdoAvatarPreset.SCOUT -> secondary
        SdoAvatarPreset.GUARDIAN -> accent
        SdoAvatarPreset.ALCHEMIST -> primary
    }

    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(emblemColor.copy(alpha = 0.28f), surface.copy(alpha = 0.92f)),
                center = center,
                radius = size.minDimension * 0.72f,
            ),
        )
        val stroke = (size.minDimension * 0.065f).coerceIn(1.5f, 5f)
        val r = size.minDimension * 0.34f
        drawCircle(
            color = emblemColor.copy(alpha = 0.32f),
            radius = r * 1.18f,
            center = center,
            style = Stroke(width = stroke * 0.6f),
        )
        when (preset) {
            SdoAvatarPreset.WARRIOR -> drawWarriorEmblem(emblemColor, r, stroke)
            SdoAvatarPreset.SCHOLAR -> drawScholarEmblem(emblemColor, r, stroke)
            SdoAvatarPreset.MYSTIC -> drawMysticEmblem(emblemColor, r, stroke)
            SdoAvatarPreset.SCOUT -> drawScoutEmblem(emblemColor, r, stroke)
            SdoAvatarPreset.GUARDIAN -> drawGuardianEmblem(emblemColor, r, stroke)
            SdoAvatarPreset.ALCHEMIST -> drawAlchemistEmblem(emblemColor, r, stroke)
        }
    }
}

private fun DrawScope.drawWarriorEmblem(color: Color, r: Float, stroke: Float) {
    val cx = center.x
    val cy = center.y
    val blade = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r * 0.22f, cy - r * 0.2f)
        lineTo(cx, cy + r * 0.55f)
        lineTo(cx - r * 0.22f, cy - r * 0.2f)
        close()
    }
    drawPath(blade, color = color, style = Stroke(width = stroke, join = StrokeJoin.Miter))
    drawLine(color, Offset(cx - r * 0.52f, cy + r * 0.3f), Offset(cx + r * 0.52f, cy + r * 0.3f), stroke, StrokeCap.Round)
    drawLine(color, Offset(cx, cy + r * 0.3f), Offset(cx, cy + r * 0.9f), stroke, StrokeCap.Round)
}

private fun DrawScope.drawScholarEmblem(color: Color, r: Float, stroke: Float) {
    val cx = center.x
    val cy = center.y
    val diamond = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r * 0.8f, cy)
        lineTo(cx, cy + r)
        lineTo(cx - r * 0.8f, cy)
        close()
    }
    drawPath(diamond, color = color, style = Stroke(width = stroke))
    drawLine(color, Offset(cx, cy - r * 0.6f), Offset(cx, cy + r * 0.6f), stroke * 0.8f, StrokeCap.Round)
    drawLine(color, Offset(cx - r * 0.42f, cy), Offset(cx + r * 0.42f, cy), stroke * 0.8f, StrokeCap.Round)
}

private fun DrawScope.drawMysticEmblem(color: Color, r: Float, stroke: Float) {
    val cx = center.x
    val cy = center.y
    val up = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r * 0.82f, cy + r * 0.48f)
        lineTo(cx - r * 0.82f, cy + r * 0.48f)
        close()
    }
    val down = Path().apply {
        moveTo(cx, cy + r)
        lineTo(cx + r * 0.82f, cy - r * 0.48f)
        lineTo(cx - r * 0.82f, cy - r * 0.48f)
        close()
    }
    drawPath(up, color = color, style = Stroke(width = stroke * 0.9f))
    drawPath(down, color = color, style = Stroke(width = stroke * 0.9f))
    drawCircle(color = color, radius = r * 0.18f, center = center)
}

private fun DrawScope.drawScoutEmblem(color: Color, r: Float, stroke: Float) {
    val cx = center.x
    val cy = center.y
    val arrow = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r * 0.72f, cy + r * 0.75f)
        lineTo(cx, cy + r * 0.35f)
        lineTo(cx - r * 0.72f, cy + r * 0.75f)
        close()
    }
    drawPath(arrow, color = color, style = Stroke(width = stroke, join = StrokeJoin.Miter))
}

private fun DrawScope.drawGuardianEmblem(color: Color, r: Float, stroke: Float) {
    val cx = center.x
    val cy = center.y
    val shield = Path().apply {
        moveTo(cx - r * 0.78f, cy - r * 0.65f)
        lineTo(cx + r * 0.78f, cy - r * 0.65f)
        lineTo(cx + r * 0.72f, cy + r * 0.18f)
        lineTo(cx, cy + r)
        lineTo(cx - r * 0.72f, cy + r * 0.18f)
        close()
    }
    drawPath(shield, color = color, style = Stroke(width = stroke, join = StrokeJoin.Miter))
    drawLine(color, Offset(cx, cy - r * 0.45f), Offset(cx, cy + r * 0.6f), stroke * 0.85f, StrokeCap.Round)
}

private fun DrawScope.drawAlchemistEmblem(color: Color, r: Float, stroke: Float) {
    val cx = center.x
    val cy = center.y
    val flask = Path().apply {
        moveTo(cx - r * 0.25f, cy - r * 0.85f)
        lineTo(cx + r * 0.25f, cy - r * 0.85f)
        lineTo(cx + r * 0.25f, cy - r * 0.25f)
        lineTo(cx + r * 0.82f, cy + r * 0.78f)
        lineTo(cx - r * 0.82f, cy + r * 0.78f)
        lineTo(cx - r * 0.25f, cy - r * 0.25f)
        close()
    }
    drawPath(flask, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round))
    drawLine(color, Offset(cx - r * 0.45f, cy + r * 0.35f), Offset(cx + r * 0.45f, cy + r * 0.35f), stroke * 0.8f, StrokeCap.Round)
}

private fun characterInitials(name: String): String = name.trim()
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .joinToString("")
    .ifBlank { "?" }

private fun decodeAvatarBitmap(context: Context, photoUri: String): ImageBitmap? = runCatching {
    when {
        photoUri.startsWith("data:image/", ignoreCase = true) -> {
            val commaIndex = photoUri.indexOf(',')
            if (commaIndex < 0) return null
            val payload = photoUri.substring(commaIndex + 1)
            val bytes = Base64.decode(payload, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
        photoUri.startsWith("content://", ignoreCase = true) ||
            photoUri.startsWith("file://", ignoreCase = true) -> {
            context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }
        else -> {
            val file = File(photoUri)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
        }
    }
}.getOrNull()

private fun encodePickedAvatar(context: Context, uri: Uri): String? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_AVATAR_DIMENSION_PX * 2)
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
    } ?: return null

    val square = centerCropSquare(decoded)
    if (square !== decoded) decoded.recycle()

    var encodedWithinBudget: String? = null
    try {
        for (dimension in AVATAR_DIMENSION_STEPS) {
            val targetSide = minOf(dimension, square.width, square.height).coerceAtLeast(1)
            val scaled = if (square.width > targetSide || square.height > targetSide) {
                Bitmap.createScaledBitmap(square, targetSide, targetSide, true)
            } else {
                square
            }
            try {
                for (quality in AVATAR_COMPRESSION_STEPS) {
                    val output = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
                    val candidate = "data:image/jpeg;base64," +
                        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    if (candidate.toByteArray(Charsets.UTF_8).size <= MAX_AVATAR_THUMBNAIL_BYTES) {
                        encodedWithinBudget = candidate
                        break
                    }
                }
            } finally {
                if (scaled !== square) scaled.recycle()
            }
            if (encodedWithinBudget != null) break
        }
    } finally {
        square.recycle()
    }
    encodedWithinBudget
}.getOrNull()

private fun calculateSampleSize(width: Int, height: Int, targetMax: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sample = 1
    while (width / sample > targetMax && height / sample > targetMax) {
        sample *= 2
    }
    return sample.coerceAtLeast(1)
}

private fun centerCropSquare(source: Bitmap): Bitmap {
    val side = minOf(source.width, source.height).coerceAtLeast(1)
    if (source.width == side && source.height == side) return source
    val x = ((source.width - side) / 2).coerceAtLeast(0)
    val y = ((source.height - side) / 2).coerceAtLeast(0)
    return Bitmap.createBitmap(source, x, y, side, side)
}
