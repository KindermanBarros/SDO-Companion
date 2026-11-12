package com.kinderman.sdo.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Component layout evidence; this is not a production Activity/IME journey or a golden comparison. */
@RunWith(Parameterized::class)
class AdaptiveUiInstrumentedTest(private val mode: SdoVisualMode, private val widthDp: Int) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun navigationSelectionRemainsReachableAndAnnounced() {
        compose.setContent { Fixture(mode, widthDp) }

        val selected = SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)
        compose.onNode(hasText("Painel", ignoreCase = true) and selected)
            .assertIsSelected().assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        compose.onNode(hasText("Ajustes", ignoreCase = true) and selected)
            .assertIsDisplayed().performClick().assertIsSelected()
        compose.onNodeWithTag("selected-page").assertTextEquals("Ajustes")

        saveScreenshot("navigation")
    }

    @Test
    fun enlargedTextReflowsActionsWithoutLeavingViewport() {
        compose.setContent { Fixture(mode, widthDp, fontScale = 1.3f) }

        val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
        for (index in 0..3) {
            val action = compose.onNodeWithTag("action-$index").assertIsDisplayed()
            action.assertHeightIsAtLeast(48.dp)
            val bounds = action.fetchSemanticsNode().boundsInRoot
            assertTrue("Action $index must fit horizontally", bounds.left >= viewport.left - 1f && bounds.right <= viewport.right + 1f)
        }
        compose.onNodeWithTag("action-3").performClick()
        compose.onNodeWithTag("action-result").assertTextEquals("3")

        saveScreenshot("enlarged-text")
    }

    @Test
    fun fieldRetainsEditingAndCybergrungeAccessibleName() {
        compose.setContent { Fixture(mode, widthDp) }

        val input = if (mode == SdoVisualMode.CYBERGRUNGE) {
            compose.onNodeWithContentDescription("Nome do personagem")
        } else {
            compose.onNode(hasSetTextAction())
        }
        input.assertIsDisplayed().performTextReplacement("Kara")
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Kara")))
    }

    @Test
    fun wideReadingSurfaceIsBoundedAndCentered() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                Box(Modifier.width(1400.dp).height(160.dp).testTag("wide-viewport")) {
                    SdoContentFrame {
                        Box(Modifier.fillMaxSize().testTag("reading-surface"))
                    }
                }
            }
        }

        val viewport = compose.onNodeWithTag("wide-viewport").fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("reading-surface").assertWidthIsEqualTo(1120.dp)
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Reading surface must be centered", abs(content.center.x - viewport.center.x) < 1f)
    }

    private fun saveScreenshot(suffix: String) {
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "adaptive-ui")
        check(folder.isDirectory || folder.mkdirs())
        FileOutputStream(File(folder, "${mode.name.lowercase()}-${widthDp}dp-$suffix.png")).use { stream ->
            check(compose.onNodeWithTag("viewport").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-{1}dp")
        fun configurations(): List<Array<Any>> = SdoVisualMode.entries.flatMap { mode ->
            listOf(320, 600, 840).map { width -> arrayOf<Any>(mode, width) }
        }
    }
}

@Composable
private fun Fixture(mode: SdoVisualMode, widthDp: Int, fontScale: Float = 1f) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        SdoTheme(SdoPreferences(visualMode = mode)) {
            Box(Modifier.width(widthDp.dp).height(700.dp).testTag("viewport")) {
                SdoResponsiveFrame {
                    var selected by remember { mutableStateOf("Painel") }
                    var actionResult by remember { mutableStateOf("") }
                    var name by remember { mutableStateOf("") }
                    val destinations = listOf("Painel", "Sessão", "Mestre", "Ajustes").map { label ->
                        SdoNavigationItem(label, selected == label, Icons.Default.Home) { selected = label }
                    }
                    Row {
                        if (LocalSdoUseNavigationRail.current) SdoNavigationRail(destinations)
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            Column(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                                Text(selected, Modifier.testTag("selected-page"))
                                SdoResponsiveGrid(listOf(0, 1, 2, 3), minItemWidth = 132.dp) { action, modifier ->
                                    SdoActionButton("Ação $action", { actionResult = action.toString() }, modifier.testTag("action-$action"))
                                }
                                Text(actionResult, Modifier.testTag("action-result"))
                                SdoField("Nome do personagem", name, { name = it })
                            }
                            if (!LocalSdoUseNavigationRail.current) SdoNavigationBar(destinations)
                        }
                    }
                }
            }
        }
    }
}
