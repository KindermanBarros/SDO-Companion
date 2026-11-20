package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import com.kinderman.sdo.ui.SdoButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.kinderman.sdo.ui.SdoIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.kinderman.sdo.ui.SdoOutlinedButton
import com.kinderman.sdo.ui.SdoTextButton
import com.kinderman.sdo.ui.sdoClickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.LocalSdoPreferences
import com.kinderman.sdo.ui.SdoMetadataText
import androidx.compose.foundation.layout.BoxWithConstraints
import com.kinderman.sdo.ui.SdoActionButton
import com.kinderman.sdo.ui.SdoActionStyle
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.Void

@Composable
internal fun IntegerField(
    label: String,
    value: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    fieldKey: String = "int:$label",
    onValue: (Int) -> Unit,
) {
    val collaboration = LocalSheetFieldCollaboration.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val currentOnFocus by androidx.compose.runtime.rememberUpdatedState(collaboration.onFocus)
    val currentOnValue by androidx.compose.runtime.rememberUpdatedState(onValue)
    val deferCommit = collaboration.isSheetContext && collaboration.commitOnBlur
    var focused by remember(fieldKey) { mutableStateOf(false) }
    var input by remember(fieldKey) { mutableStateOf(value.takeUnless { it == 0 }?.toString().orEmpty()) }
    var committedValue by remember(fieldKey) { mutableStateOf(value) }

    val parseInput: (String) -> Int? = { normalized ->
        when {
            normalized.isEmpty() || normalized == "-" -> 0
            else -> normalized.toIntOrNull()
        }
    }

    val commitIfDirty = remember(fieldKey) {
        {
            val parsed = parseInput(input)
            if (parsed != null && parsed != committedValue) {
                committedValue = parsed
                currentOnValue(parsed)
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(value, focused) {
        if (!focused) {
            input = value.takeUnless { it == 0 }?.toString().orEmpty()
            committedValue = value
        }
    }

    androidx.compose.runtime.DisposableEffect(fieldKey, collaboration.commitRegistry) {
        if (collaboration.isSheetContext) {
            collaboration.commitRegistry.register(fieldKey, commitIfDirty)
        }
        onDispose {
            if (collaboration.isSheetContext) {
                collaboration.commitRegistry.unregister(fieldKey)
            }
            if (deferCommit) {
                commitIfDirty()
            }
            if (focused) {
                currentOnFocus(fieldKey, false)
            }
        }
    }

    HudTextField(
        label = label,
        value = input,
        modifier = modifier.onFocusChanged { state ->
            if (focused != state.isFocused) {
                focused = state.isFocused
                if (!focused && deferCommit) {
                    commitIfDirty()
                }
                currentOnFocus(fieldKey, focused)
            }
        },
        placeholder = "0",
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = {
                if (deferCommit) commitIfDirty()
                focusManager.clearFocus()
            },
        ),
        enabled = enabled,
    ) { raw ->
        val normalized = raw.filterIndexed { index, character ->
            character.isDigit() || (index == 0 && character == '-')
        }
        input = normalized
        if (!deferCommit) {
            when {
                normalized.isEmpty() -> {
                    committedValue = 0
                    currentOnValue(0)
                }
                normalized != "-" -> normalized.toIntOrNull()?.let { parsed ->
                    committedValue = parsed
                    currentOnValue(parsed)
                }
            }
        }
    }
}

@Composable
internal fun AddButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    SdoActionButton(label, onClick, Modifier.fillMaxWidth(), enabled, SdoActionStyle.PRIMARY)
}

enum class CharacterActionStyle { PRIMARY, SECONDARY, DESTRUCTIVE }

@Composable
internal fun CharacterActionButton(
    label: String,
    enabled: Boolean,
    style: CharacterActionStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val resolved = when (style) {
        CharacterActionStyle.PRIMARY -> SdoActionStyle.PRIMARY
        CharacterActionStyle.SECONDARY -> SdoActionStyle.SECONDARY
        CharacterActionStyle.DESTRUCTIVE -> SdoActionStyle.DESTRUCTIVE
    }
    SdoActionButton(label, onClick, modifier, enabled, resolved)
}

@Composable
internal fun RemoveButton(enabled: Boolean, description: String, onClick: () -> Unit) {
    SdoIconButton(onClick = onClick, enabled = enabled) { Icon(Icons.Default.Close, description, tint = MaterialTheme.colorScheme.error) }
}

@Composable
internal fun CompactEditToggleButton(
    editing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return
    SdoIconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (editing) Icons.Default.Visibility else Icons.Default.Edit,
            contentDescription = if (editing) "Visualizar" else "Editar",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun SectionShowcaseHeader(
    index: String,
    title: String,
    subtitle: String,
    editing: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    trailingActions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    val collapsedTitle = com.kinderman.sdo.ui.LocalCollapsibleSectionTitle.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (collapsedTitle == null) {
            com.kinderman.sdo.ui.SectionHeader(index, title, Modifier.weight(1f))
        } else {
            SdoMetadataText(
                text = subtitle,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        CompactEditToggleButton(editing = enabled && editing, enabled = enabled, onClick = onToggle)
        trailingActions()
    }
}

@Composable
internal fun TwoFields(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale * LocalSdoPreferences.current.fontScale.multiplier
        val cybergrunge = LocalSdoPreferences.current.visualMode == com.kinderman.sdo.ui.SdoVisualMode.CYBERGRUNGE
        if (maxWidth < (if (cybergrunge) 460.dp else 360.dp) * fontScale) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

internal fun <T> List<T>.replace(index: Int, value: T): List<T> = toMutableList().also { it[index] = value }

@Composable
internal fun <T> ChoiceField(
    label: String,
    value: T,
    options: List<T>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    display: (T) -> String = { it.toString() },
    searchable: Boolean = options.size > 8,
    onValue: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var anchorWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val labelStyle = MaterialTheme.typography.labelLarge
    val minimumLabelSize = (10 * LocalSdoPreferences.current.fontScale.multiplier).sp
    val visibleOptions = if (query.isBlank()) options else options.filter { display(it).contains(query, ignoreCase = true) }
    Box(modifier) {
        SdoOutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).onSizeChanged { anchorWidth = it.width },
            shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        ) {
            SdoMetadataText(
                text = "$label // ${display(value)}",
                color = LocalContentColor.current,
                style = labelStyle,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = if (minimumLabelSize.value <= labelStyle.fontSize.value) minimumLabelSize else labelStyle.fontSize,
                    maxFontSize = labelStyle.fontSize,
                ),
            )
        }
        if (searchable) {
            if (expanded) {
                AlertDialog(
                    onDismissRequest = { expanded = false; query = "" },
                    title = { Text(label.uppercase()) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HudTextField("Buscar em $label", query, Modifier.fillMaxWidth()) { query = it }
                            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                                if (visibleOptions.isEmpty()) {
                                    item(key = "empty") {
                                        Text(
                                            "Nenhuma opção encontrada",
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                } else {
                                    itemsIndexed(visibleOptions, key = { index, option -> "${display(option)}:$index" }) { _, option ->
                                        val selected = option == value
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .sdoClickable {
                                                    onValue(option)
                                                    expanded = false
                                                    query = ""
                                                }
                                                .padding(horizontal = 8.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Text(
                                                text = display(option),
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        SdoTextButton(onClick = { expanded = false; query = "" }) { Text("FECHAR") }
                    },
                )
            }
        } else {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(0.dp, 4.dp),
                modifier = Modifier
                    .then(if (anchorWidth > 0) Modifier.requiredWidth(with(density) { anchorWidth.toDp() }) else Modifier)
                    .heightIn(max = 360.dp),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(display(option)) },
                        onClick = { onValue(option); expanded = false },
                    )
                }
            }
        }
    }
}
