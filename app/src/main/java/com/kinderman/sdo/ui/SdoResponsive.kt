package com.kinderman.sdo.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

enum class SdoWindowClass { COMPACT, MEDIUM, EXPANDED }

val LocalSdoWindowClass = compositionLocalOf { SdoWindowClass.COMPACT }

@Composable
fun SdoResponsiveFrame(content: @Composable () -> Unit) {
    BoxWithConstraints {
        val windowClass = when {
            maxWidth < 600.dp -> SdoWindowClass.COMPACT
            maxWidth < 840.dp -> SdoWindowClass.MEDIUM
            else -> SdoWindowClass.EXPANDED
        }
        CompositionLocalProvider(LocalSdoWindowClass provides windowClass, content = content)
    }
}
