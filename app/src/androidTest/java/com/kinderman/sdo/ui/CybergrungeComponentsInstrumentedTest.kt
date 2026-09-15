package com.kinderman.sdo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CybergrungeComponentsInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptyFieldUsesVisualSignalInsteadOfPlaceholderText() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE)) {
                SdoField(label = "NOME", value = "", onValueChange = {})
            }
        }

        compose.onNodeWithText("INPUT//NOME").assertExists()
        compose.onNodeWithText("SINAL VAZIO", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("cybergrunge-empty-signal", useUnmergedTree = true).assertExists()
    }

    @Test
    fun visualMissingSignalLeavesCompositionWhenFieldHasContent() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE)) {
                SdoField(label = "NOME", value = "Kara", onValueChange = {})
            }
        }

        compose.onNodeWithTag("cybergrunge-empty-signal", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun experimentalLabExposesCoreInteractionStates() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE)) {
                Box(Modifier.size(360.dp, 640.dp).testTag("cybergrunge-viewport")) {
                    Column(
                        Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SdoScreenMasthead("LAB//CG", "LABORATÓRIO VISUAL", "ANDROID VIEWPORT")
                        SdoActionButton("Primária", {}, style = SdoActionStyle.PRIMARY)
                        SdoOfflineState("SEM CONEXÃO", "Dados locais preservados.")
                        SdoErrorState("SINAL CORROMPIDO", "Revise o campo.")
                    }
                }
            }
        }

        compose.onNodeWithText("LABORATÓRIO VISUAL").assertExists()
        compose.onNodeWithText("PRIMÁRIA", substring = true).assertExists()
        compose.onNodeWithText("SEM CONEXÃO").assertExists()
        compose.onNodeWithText("SINAL CORROMPIDO").assertExists()

        val pixels = compose.onNodeWithTag("cybergrunge-viewport").captureToImage().toPixelMap()
        val sampleColors = buildSet {
            val xStep = (pixels.width / 12).coerceAtLeast(1)
            val yStep = (pixels.height / 20).coerceAtLeast(1)
            for (x in 0 until pixels.width step xStep) {
                for (y in 0 until pixels.height step yStep) add(pixels[x, y])
            }
        }
        assertTrue("Cybergrunge deve renderizar contraste e acentos distintos", sampleColors.size >= 8)
    }
}
