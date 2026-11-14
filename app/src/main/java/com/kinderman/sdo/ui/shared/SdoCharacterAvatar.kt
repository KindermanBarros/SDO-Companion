package com.kinderman.sdo.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_AVATAR_DIMENSION_PX = 256
private const val AVATAR_JPEG_QUALITY = 82

/**
 * Character portrait frame supporting Standard/Cybergrunge cut-corner geometry
 * and a circular coin-token mode for future battle-grid tokens.
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
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photoUri) {
        value = if (photoUri.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) { decodeAvatarBitmap(context, photoUri) }
        }
    }
    val initials = remember(name) { characterInitials(name) }
    val description = remember(name) {
        val cleanName = name.ifBlank { "Personagem" }
        "Retrato de $cleanName"
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
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape),
            )
        } else {
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

/**
 * Editable portrait picker that center-crops and scales selected images to <= 256x256 JPEG Base64 data URIs.
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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        processing = true
        errorMessage = null
        scope.launch {
            val encoded = withContext(Dispatchers.IO) { encodePickedAvatar(context, uri) }
            processing = false
            if (encoded != null) {
                onPhotoChange(encoded)
            } else {
                errorMessage = "Não foi possível carregar a imagem selecionada."
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
                    text = "FOTO DE PERFIL",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                SdoMetadataText(
                    text = when {
                        processing -> "Processando imagem..."
                        photoUri.isNotBlank() -> "Imagem salva na ficha (256x256)."
                        !enabled -> "Sem foto definida (modo somente leitura)."
                        else -> "Adicione um retrato para fichas, listas e modo sessão."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (enabled) {
            SdoResponsiveGrid(
                items = if (photoUri.isNotBlank()) listOf("PICK", "REMOVE") else listOf("PICK"),
                minItemWidth = 132.dp,
            ) { action, itemModifier ->
                when (action) {
                    "PICK" -> SdoActionButton(
                        label = if (photoUri.isNotBlank()) "Trocar foto" else "Adicionar foto",
                        onClick = {
                            launcher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = itemModifier,
                        enabled = !processing,
                    )
                    else -> SdoActionButton(
                        label = "Remover foto",
                        onClick = {
                            errorMessage = null
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

    val scaled = if (square.width > MAX_AVATAR_DIMENSION_PX || square.height > MAX_AVATAR_DIMENSION_PX) {
        Bitmap.createScaledBitmap(square, MAX_AVATAR_DIMENSION_PX, MAX_AVATAR_DIMENSION_PX, true).also {
            if (it !== square) square.recycle()
        }
    } else {
        square
    }

    val output = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, output)
    scaled.recycle()
    "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
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
