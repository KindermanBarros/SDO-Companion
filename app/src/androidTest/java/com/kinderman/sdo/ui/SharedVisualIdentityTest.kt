package com.kinderman.sdo.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SharedVisualIdentityTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun clickableHeaderDoesNotMaskItsContentCorners() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE)) {
                Box(Modifier.size(80.dp).background(Color.Black)) {
                    Box(Modifier.fillMaxSize().testTag("header").sdoClickable {}) {
                        Box(Modifier.fillMaxSize().background(Color.White))
                    }
                }
            }
        }
        val pixels = compose.onNodeWithTag("header").captureToImage().toPixelMap()
        assertTrue("Clickable header must not cut bottom-left text pixels", pixels[1, pixels.height - 2] == Color.White)
    }

    @Test
    fun standardCardAndOutlinedControlUseAngularShapes() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = SdoVisualMode.STANDARD)) {
                androidx.compose.foundation.layout.Column {
                    SdoCard(modifier = Modifier.size(180.dp, 80.dp).testTag("card")) { Text("Item") }
                    SdoOutlinedButton(onClick = {}, modifier = Modifier.testTag("control")) { Text("Filtros") }
                }
            }
        }
        val cardShape = compose.onNodeWithTag("card").fetchSemanticsNode().config[SemanticsProperties.Shape]
        val buttonShape = compose.onNodeWithTag("control").fetchSemanticsNode().config[SemanticsProperties.Shape]
        assertTrue("Standard item cards must have angular corners", cardShape is CutCornerShape)
        assertTrue("Standard filter controls must have angular corners", buttonShape is CutCornerShape)
    }

    @Test
    fun cyberPanelKeepsContentStationaryWhileSignalAnimates() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE)) {
                TechPanel {
                    Text("BÔNUS APLICADOS: NENHUM", modifier = Modifier.testTag("panel_text"))
                }
            }
        }
        compose.mainClock.advanceTimeBy(100)
        val initial = compose.onNodeWithTag("panel_text").fetchSemanticsNode().boundsInRoot
        compose.mainClock.advanceTimeBy(500)
        val later = compose.onNodeWithTag("panel_text").fetchSemanticsNode().boundsInRoot
        assertEquals("Panel decoration must not displace readable content", initial.left, later.left, .25f)
        assertEquals("Panel decoration must not displace readable content", initial.top, later.top, .25f)
    }
}
