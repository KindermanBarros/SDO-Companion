package com.kinderman.sdo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SdoWindowClass { COMPACT, MEDIUM, EXPANDED }

val LocalSdoWindowClass = compositionLocalOf { SdoWindowClass.COMPACT }
val LocalSdoUseNavigationRail = compositionLocalOf { false }

internal fun sdoWindowClass(widthDp: Float): SdoWindowClass = when {
    widthDp < 600f -> SdoWindowClass.COMPACT
    widthDp < 840f -> SdoWindowClass.MEDIUM
    else -> SdoWindowClass.EXPANDED
}

internal fun sdoUsesNavigationRail(widthDp: Float, heightDp: Float): Boolean =
    widthDp >= 600f && heightDp >= 480f

@Composable
fun SdoResponsiveFrame(content: @Composable () -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        CompositionLocalProvider(
            LocalSdoWindowClass provides sdoWindowClass(maxWidth.value),
            LocalSdoUseNavigationRail provides sdoUsesNavigationRail(maxWidth.value, maxHeight.value),
            content = content,
        )
    }
}

/** Keep reading surfaces bounded without constraining navigation or the window background. */
@Composable
fun SdoContentFrame(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = 1120.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.widthIn(max = maxContentWidth).fillMaxSize()) {
            content()
        }
    }
}
