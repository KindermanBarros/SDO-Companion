package com.kinderman.sdo.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CyberGrungeComponentsInstrumentedTest {
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
    }

    @Test
    fun experimentalLabExposesCoreInteractionStates() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE)) {
                CyberGrungeLab(Modifier.testTag("cybergrunge-lab"))
            }
        }

        compose.onNodeWithText("LABORATÓRIO VISUAL").assertExists()
        compose.onNodeWithText("PRIMÁRIA", substring = true).assertExists()
        compose.onNodeWithText("SEM CONEXÃO").assertExists()
        compose.onNodeWithText("SINAL CORROMPIDO").assertExists()

        val pixels = compose.onNodeWithTag("cybergrunge-lab").captureToImage().toPixelMap()
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
