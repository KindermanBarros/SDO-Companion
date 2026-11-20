package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.kinderman.sdo.R
import com.kinderman.sdo.ui.HudTextField

/**
 * Coordinates synchronous commit of any dirty focused sheet text field before explicit Save, Back,
 * Undo, or surface transitions execute.
 */
internal class SheetFieldCommitRegistry {
    private val committers = LinkedHashMap<String, () -> Unit>()

    fun register(key: String, commit: () -> Unit) {
        committers[key] = commit
    }

    fun unregister(key: String) {
        committers.remove(key)
    }

    fun flushAll() {
        committers.values.toList().forEach { it() }
    }
}

internal data class SheetFieldCollaboration(
    val presence: Map<String, String> = emptyMap(),
    val onFocus: (String, Boolean) -> Unit = { _, _ -> },
    val commitRegistry: SheetFieldCommitRegistry = SheetFieldCommitRegistry(),
    val isSheetContext: Boolean = false,
    val commitOnBlur: Boolean = false,
)

internal val LocalSheetFieldCollaboration = staticCompositionLocalOf { SheetFieldCollaboration() }

/**
 * Keeps an active text input stable while other sheet fields receive remote snapshots, and defers
 * emitting edits until the user finishes editing (focus loss, IME Done, disposal, or explicit flush)
 * when [SheetFieldCollaboration.commitOnBlur] is enabled.
 */
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
    Column {
        SheetTextField(
            label = label,
            value = value,
            enabled = enabled,
            multiline = multiline,
            fieldKey = fieldKey,
            onValue = onValue,
        )
        collaboration.presence[fieldKey]?.let { actor ->
            Text(
                text = stringResource(R.string.sheet_field_presence, actor),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
internal fun SheetTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    multiline: Boolean = false,
    placeholder: String? = null,
    fieldKey: String = label,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onValue: (String) -> Unit,
) {
    val collaboration = LocalSheetFieldCollaboration.current
    val focusManager = LocalFocusManager.current
    val currentOnFocus by rememberUpdatedState(collaboration.onFocus)
    val currentOnValue by rememberUpdatedState(onValue)
    val deferCommit = collaboration.isSheetContext && collaboration.commitOnBlur
    var focused by remember(fieldKey) { mutableStateOf(false) }
    var input by remember(fieldKey) { mutableStateOf(value) }
    var committedValue by remember(fieldKey) { mutableStateOf(value) }

    val commitIfDirty = remember(fieldKey) {
        {
            if (input != committedValue) {
                committedValue = input
                currentOnValue(input)
            }
        }
    }

    LaunchedEffect(value, focused) {
        if (!focused) {
            input = value
            committedValue = value
        }
    }

    DisposableEffect(fieldKey, collaboration.commitRegistry) {
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

    val effectiveKeyboardOptions = if (!multiline && keyboardOptions.imeAction == ImeAction.Unspecified) {
        keyboardOptions.copy(imeAction = ImeAction.Done)
    } else {
        keyboardOptions
    }
    val effectiveKeyboardActions = if (!multiline) {
        KeyboardActions(
            onDone = {
                if (deferCommit) commitIfDirty()
                focusManager.clearFocus()
            },
        )
    } else {
        KeyboardActions.Default
    }

    HudTextField(
        label = label,
        value = input,
        enabled = enabled,
        multiline = multiline,
        placeholder = placeholder,
        keyboardOptions = effectiveKeyboardOptions,
        keyboardActions = effectiveKeyboardActions,
        modifier = modifier.onFocusChanged { state ->
            if (focused != state.isFocused) {
                focused = state.isFocused
                if (!focused && deferCommit) {
                    commitIfDirty()
                }
                currentOnFocus(fieldKey, focused)
            }
        },
    ) { next ->
        input = next
        if (!deferCommit) {
            committedValue = next
            currentOnValue(next)
        }
    }
}
