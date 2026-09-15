package com.kinderman.sdo.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun StandardTextAction(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable RowScope.() -> Unit,
) = TextButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)

@Composable
internal fun StandardIconAction(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable () -> Unit,
) = IconButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
