package com.kinderman.sdo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun CyberGrungePanel(
    modifier: Modifier,
    accent: Color,
    compact: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val possessionSeed = remember { System.identityHashCode(Any()) }
    val shape = CutCornerShape(topEnd = CyberGrungeTokens.cut, bottomStart = CyberGrungeTokens.cut)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .94f), shape)
            .border(1.dp, accent.copy(alpha = .78f), shape),
    ) {
        CyberGrungeInterference(seed = possessionSeed)
        Box(Modifier.fillMaxWidth().height(3.dp).background(accent).align(Alignment.TopStart))
        Box(Modifier.width(CyberGrungeTokens.railWidth).fillMaxSize().background(accent.copy(alpha = .78f)))
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                modifier = Modifier.padding(start = if (compact) 13.dp else 18.dp, top = 13.dp, end = 13.dp, bottom = 13.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
            ) {
                CyberGrungePanelChrome()
                content()
            }
        }
    }
}

@Composable
internal fun CyberGrungeAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    style: SdoActionStyle,
) {
    val signal = when (style) {
        SdoActionStyle.PRIMARY -> MaterialTheme.colorScheme.primary
        SdoActionStyle.SECONDARY -> CyberGrungeTokens.TerminalGreen
        SdoActionStyle.DESTRUCTIVE -> MaterialTheme.colorScheme.error
    }
    val shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)
    val interaction = remember { MutableInteractionSource() }
    val content: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(12.dp).height(4.dp).background(signal))
            AdaptiveActionLabel("[ ${label.uppercase()} ]", color = if (style == SdoActionStyle.PRIMARY) MaterialTheme.colorScheme.onPrimary else signal)
        }
    }
    if (style == SdoActionStyle.PRIMARY) {
        Button(
            onClick = onClick, enabled = enabled,
            modifier = modifier.heightIn(min = SdoSpacingTokens.minimumTouchTarget).cyberGrungePress(interaction),
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = signal),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            interactionSource = interaction,
            content = { content() },
        )
    } else {
        OutlinedButton(
            onClick = onClick, enabled = enabled,
            modifier = modifier.heightIn(min = SdoSpacingTokens.minimumTouchTarget).cyberGrungePress(interaction),
            shape = shape,
            border = androidx.compose.foundation.BorderStroke(1.dp, signal),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = signal),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            interactionSource = interaction,
            content = { content() },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun CyberGrungeField(
    label: String,
    value: String,
    modifier: Modifier,
    multiline: Boolean,
    placeholder: String?,
    enabled: Boolean,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions,
    onValue: (String) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = CyberGrungeTokens.TerminalGreen.copy(alpha = .72f),
        focusedContainerColor = CyberGrungeTokens.Panel,
        unfocusedContainerColor = CyberGrungeTokens.Panel,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = CyberGrungeTokens.Paper.copy(alpha = .72f),
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("INPUT//$label", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        androidx.compose.foundation.text.BasicTextField(
            value = value, onValueChange = onValue, enabled = enabled, singleLine = !multiline,
            keyboardOptions = keyboardOptions, interactionSource = interaction,
            minLines = if (multiline) 3 else 1,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium),
            modifier = Modifier.fillMaxWidth().heightIn(min = if (multiline) 88.dp else 48.dp),
            decorationBox = { inner ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value, innerTextField = inner, enabled = enabled, singleLine = !multiline,
                    visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                    interactionSource = interaction,
                    label = null,
                    placeholder = placeholder?.let { hint -> { Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                    colors = colors,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    container = {
                        Box {
                            OutlinedTextFieldDefaults.Container(
                                enabled = enabled, isError = false, interactionSource = interaction, colors = colors,
                                shape = CutCornerShape(topEnd = 16.dp, bottomStart = 10.dp),
                            )
                            if (value.isEmpty() && placeholder == null) CyberGrungeEmptySignal(
                                Modifier.align(Alignment.Center).padding(horizontal = 14.dp)
                                    .testTag("cybergrunge-empty-signal"),
                            )
                        }
                    },
                )
            },
        )
    }
}

@Composable
internal fun CyberGrungeChoiceRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    badge: @Composable () -> Unit,
) {
    val accent = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Row(
        Modifier.fillMaxWidth()
            .border(1.dp, accent, CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
            .clip(CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(18.dp).height(18.dp).border(2.dp, accent).padding(3.dp)) {
            if (selected) Box(Modifier.fillMaxSize().background(accent))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            badge()
            AdaptiveSingleLineText(
                text = label.uppercase(),
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text(if (selected) "LOCK" else "OPEN", color = accent, style = MaterialTheme.typography.labelSmall)
    }
}
