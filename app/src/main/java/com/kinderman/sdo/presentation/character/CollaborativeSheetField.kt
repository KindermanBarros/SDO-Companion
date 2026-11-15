package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import com.kinderman.sdo.ui.HudTextField

internal data class SheetFieldCollaboration(
    val presence: Map<String, String> = emptyMap(),
    val onFocus: (String, Boolean) -> Unit = { _, _ -> },
)

internal val LocalSheetFieldCollaboration = staticCompositionLocalOf { SheetFieldCollaboration() }

/** Keeps an active text input stable while other sheet fields receive remote snapshots. */
@Composable
internal fun CollaborativeSheetField(
    fieldKey: String,
    label: String,
    value: String,
    enabled: Boolean,
    multiline: Boolean = false,
    onValue: (String) -> Unit,
) {
    val collaboration = LocalSheetFieldCollaboration.current
    val onFocus by rememberUpdatedState(collaboration.onFocus)
    var focused by remember(fieldKey) { mutableStateOf(false) }
    var input by remember(fieldKey) { mutableStateOf(value) }
    LaunchedEffect(value, focused) { if (!focused) input = value }
    DisposableEffect(fieldKey) {
        onDispose { if (focused) onFocus(fieldKey, false) }
    }
    Column {
        HudTextField(
            label = label,
            value = input,
            enabled = enabled,
            multiline = multiline,
            modifier = Modifier.onFocusChanged {
                if (focused != it.isFocused) {
                    focused = it.isFocused
                    onFocus(fieldKey, focused)
                }
            },
        ) {
            input = it
            onValue(it)
        }
        collaboration.presence[fieldKey]?.let { actor ->
            Text(
                text = androidx.compose.ui.res.stringResource(com.kinderman.sdo.R.string.sheet_field_presence, actor),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
