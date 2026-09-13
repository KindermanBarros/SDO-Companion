package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.Void

@Composable
internal fun IntegerField(
    label: String,
    value: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onValue: (Int) -> Unit,
) {
    var input by remember(value) { mutableStateOf(value.takeUnless { it == 0 }?.toString().orEmpty()) }
    HudTextField(
        label = label,
        value = input,
        modifier = modifier,
        placeholder = "0",
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        enabled = enabled,
    ) { raw ->
        val normalized = raw.filterIndexed { index, character ->
            character.isDigit() || (index == 0 && character == '-')
        }
        input = normalized
        when {
            normalized.isEmpty() -> onValue(0)
            normalized != "-" -> normalized.toIntOrNull()?.let(onValue)
        }
    }
}

@Composable
internal fun AddButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
    ) {
        Icon(Icons.Default.Add, null)
        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
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
    when (style) {
        CharacterActionStyle.PRIMARY -> Button(
            onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
            shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
        ) { Text(label.uppercase()) }
        CharacterActionStyle.SECONDARY -> OutlinedButton(
            onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
            shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
        ) { Text(label.uppercase()) }
        CharacterActionStyle.DESTRUCTIVE -> OutlinedButton(
            onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
            shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text(label.uppercase()) }
    }
}

@Composable
internal fun RemoveButton(enabled: Boolean, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(Icons.Default.Close, description, tint = MaterialTheme.colorScheme.error) }
}

@Composable
internal fun TwoFields(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        first(Modifier.weight(1f))
        second(Modifier.weight(1f))
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
    val visibleOptions = if (query.isBlank()) options else options.filter { display(it).contains(query, ignoreCase = true) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).onSizeChanged { anchorWidth = it.width },
            shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
        ) { Text("$label // ${display(value)}", maxLines = 2) }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false; query = "" },
            offset = DpOffset(0.dp, 4.dp),
            modifier = Modifier
                .then(if (anchorWidth > 0) Modifier.requiredWidth(with(density) { anchorWidth.toDp() }) else Modifier)
                .heightIn(max = if (searchable) 320.dp else 360.dp),
        ) {
            Column {
                if (searchable) HudTextField("Buscar em $label", query, Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { query = it }
                visibleOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(display(option)) },
                        onClick = { onValue(option); expanded = false; query = "" },
                    )
                }
                if (visibleOptions.isEmpty()) Text("Nenhuma opção encontrada", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
