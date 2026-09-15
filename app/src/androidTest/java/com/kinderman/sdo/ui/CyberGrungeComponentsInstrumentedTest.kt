package com.kinderman.sdo.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
                CyberGrungeLab()
            }
        }

        compose.onNodeWithText("LABORATÓRIO VISUAL").assertExists()
        compose.onNodeWithText("PRIMÁRIA", substring = true).assertExists()
        compose.onNodeWithText("SEM CONEXÃO").assertExists()
        compose.onNodeWithText("SINAL CORROMPIDO").assertExists()
    }
}
