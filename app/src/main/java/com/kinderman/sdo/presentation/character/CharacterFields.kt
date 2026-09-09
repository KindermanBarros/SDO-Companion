package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        colors = ButtonDefaults.buttonColors(containerColor = Acid, contentColor = MaterialTheme.colorScheme.onPrimary),
    ) {
        Icon(Icons.Default.Add, null)
        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun RemoveButton(enabled: Boolean, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(Icons.Default.Close, description, tint = Signal) }
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
