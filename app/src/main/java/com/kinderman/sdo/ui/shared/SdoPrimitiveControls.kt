package com.kinderman.sdo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Drop-in contract for low-emphasis actions previously declared directly in screens. */
@Composable
fun SdoTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    if (LocalSdoPreferences.current.visualMode != SdoVisualMode.CYBERGRUNGE) {
        StandardTextAction(onClick, modifier, enabled, content)
        return
    }
    val interaction = remember { MutableInteractionSource() }
    val shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.clip(shape).cyberGrungePress(interaction),
        enabled = enabled,
        shape = shape,
        interactionSource = interaction,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .72f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        content = content,
    )
}

@Composable
fun SdoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    val cyber = LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE
    val resolvedShape = if (cyber) CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp) else shape
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick, modifier = modifier.clip(resolvedShape).then(if (cyber) Modifier.cyberGrungePress(interaction) else Modifier),
        enabled = enabled, shape = resolvedShape, colors = colors, interactionSource = interaction, content = content,
    )
}

@Composable
fun SdoOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val cyber = LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE
    val resolvedShape = if (cyber) CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp) else shape
    val resolvedBorder = if (cyber) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .72f))
    else border ?: ButtonDefaults.outlinedButtonBorder(enabled)
    val interaction = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick, modifier = modifier.clip(resolvedShape).then(if (cyber) Modifier.cyberGrungePress(interaction) else Modifier),
        enabled = enabled, shape = resolvedShape, colors = colors, border = resolvedBorder,
        contentPadding = contentPadding, interactionSource = interaction, content = content,
    )
}

@Composable
fun SdoCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE) {
        CyberGrungePanel(modifier, MaterialTheme.colorScheme.primary, LocalSdoPreferences.current.compactCards, content)
    } else Card(modifier = modifier, shape = shape, colors = colors, content = content)
}

@Composable
fun SdoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE) {
        val resolved = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)
        androidx.compose.foundation.layout.Box(
            modifier.clip(resolved).clickable(enabled = enabled, onClick = onClick),
        ) { CyberGrungePanel(Modifier, MaterialTheme.colorScheme.primary, LocalSdoPreferences.current.compactCards, content) }
    } else Card(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors, content = content)
}

@Composable
fun SdoSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    if (LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE) {
        val accent = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        val shape = CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp)
        androidx.compose.foundation.layout.Box(
            Modifier.clip(shape).clickable(enabled = enabled) { onCheckedChange?.invoke(!checked) }
                .border(1.dp, accent, shape).padding(8.dp),
        ) { androidx.compose.material3.Text(if (checked) "ON//■" else "OFF/□", color = accent, style = MaterialTheme.typography.labelSmall) }
    } else Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
}

@Composable
fun SdoIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (LocalSdoPreferences.current.visualMode != SdoVisualMode.CYBERGRUNGE) {
        StandardIconAction(onClick, modifier, enabled, content)
        return
    }
    val interaction = remember { MutableInteractionSource() }
    val shape = CutCornerShape(topEnd = 9.dp, bottomStart = 9.dp)
    IconButton(
        onClick = onClick,
        modifier = modifier.clip(shape).cyberGrungePress(interaction),
        enabled = enabled,
        interactionSource = interaction,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.outline,
        ),
        content = content,
    )
}
