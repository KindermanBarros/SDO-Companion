package com.kinderman.sdo.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SdoField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    multiline: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    if (LocalSdoPreferences.current.visualMode == SdoVisualMode.CYBERGRUNGE) {
        CyberGrungeField(label, value, modifier, multiline, supportingText, enabled, keyboardOptions, onValueChange)
        return
    }
    HudTextField(
        label = label,
        value = value,
        modifier = modifier,
        multiline = multiline,
        placeholder = supportingText,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        onValue = onValueChange,
    )
}
