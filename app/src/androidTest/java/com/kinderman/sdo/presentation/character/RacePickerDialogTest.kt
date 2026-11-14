package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.SdoTheme
import com.kinderman.sdo.ui.SdoThemeVariant
import com.kinderman.sdo.ui.SdoVisualMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RacePickerDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun raceChoicesExplainCatalogCharacteristics() {
        val character = Character(race = "Humanos")
        compose.setContent {
            RacePickerDialog(character, {}, { _, _, _, _, _ -> })
        }

        compose.onNodeWithText("RAÇA // HUMANOS").performClick()

        compose.onNodeWithText("+2 em testes de uma tradição, escola ou aplicação de magia escolhida.")
            .assertIsDisplayed()
    }

    @Test fun pendingRaceSurvivesStateRestoration() {
        val restoration = StateRestorationTester(compose)
        val character = Character(race = "Humanos")
        restoration.setContent {
            RacePickerDialog(character, {}, { _, _, _, _, _ -> })
        }

        compose.onNodeWithText("RAÇA // HUMANOS").performClick()
        compose.onNodeWithText("ELFOS").performClick()
        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("RAÇA // ELFOS").assertIsDisplayed()
    }

    @Test fun bodyAndArmorSectionShowsItsNumber() {
        compose.setContent {
            BodySection(Character(), enabled = true, onChange = {})
        }

        compose.onNodeWithText("11", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun selectorTextRemainsOnOneLineOnNarrowScreens() {
        var preferences by mutableStateOf(SdoPreferences())
        compose.setContent {
            SdoTheme(preferences) {
                Box(Modifier.requiredWidth(150.dp)) {
                    ChoiceField("RAÇA", "CRIAS DA NEBLINA", listOf("CRIAS DA NEBLINA"), true) {}
                }
            }
        }

        val themes = SdoThemeVariant.entries.map { SdoPreferences(theme = it) } +
            SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE)
        themes.forEach { theme ->
            compose.runOnIdle { preferences = theme }
            val height = compose.onNodeWithText("RAÇA // CRIAS DA NEBLINA", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot.height
            val maxSingleLineHeight = with(compose.density) { 26.dp.toPx() }

            assertTrue("Selector label wraps in $theme", height <= maxSingleLineHeight)
        }
    }

    @Test fun changingRaceClearsAnIncompatibleSubRace() {
        val character = Character(race = "Elfos", subRace = "Elfos do Crepúsculo")
        compose.setContent {
            RacePickerDialog(character, {}, { _, _, _, _, _ -> })
        }

        compose.onNodeWithText("RAÇA // ELFOS").performClick()
        compose.onNodeWithTag("race-choice-search").performTextInput("Anões")
        compose.onNodeWithText("ANÕES").performClick()

        compose.onNodeWithText("SUB-RAÇA // NENHUMA").assertIsDisplayed()
    }

    @Test fun applyingRaceKeepsItsAttributeAndBothPowers() {
        val character = Character(race = "Humanos")
        var selectedRace = ""
        var selectedAttribute = ""
        var powerNames = emptyList<String>()
        compose.setContent {
            RacePickerDialog(character, {}, { race, _, attribute, powers, _ ->
                selectedRace = race.name
                selectedAttribute = attribute
                powerNames = powers.map { it.name }
            })
        }

        compose.onNodeWithText("RAÇA // HUMANOS").performClick()
        compose.onNodeWithText("ELFOS").performClick()
        compose.onNodeWithText("APLICAR").performClick()

        compose.runOnIdle {
            assertEquals("Elfos", selectedRace)
            assertEquals("POD", selectedAttribute)
            assertEquals(listOf("Afinidade Arcana", "Passo Gracioso"), powerNames)
        }
    }

    @Test fun cancellingRaceDoesNotApplyPendingSelection() {
        val character = Character(race = "Humanos")
        var selectedRace: String? = null
        var dismissed = false
        compose.setContent {
            RacePickerDialog(character, { dismissed = true }, { race, _, _, _, _ -> selectedRace = race.name })
        }

        compose.onNodeWithText("RAÇA // HUMANOS").performClick()
        compose.onNodeWithText("ELFOS").performClick()
        compose.onNodeWithText("CANCELAR").performClick()

        compose.runOnIdle {
            assertTrue(dismissed)
            assertNull(selectedRace)
        }
    }

    @Test fun chosenFlexibleRacialAttributeIsApplied() {
        val character = Character(race = "Humanos")
        var selectedAttribute = ""
        compose.setContent {
            RacePickerDialog(character, {}, { _, _, attribute, _, _ -> selectedAttribute = attribute })
        }

        compose.onNodeWithText("ATRIBUTO // FOR").performScrollTo().performClick()
        compose.onNodeWithText("AGI").performClick()
        compose.onNodeWithText("APLICAR").performClick()

        compose.runOnIdle { assertEquals("AGI", selectedAttribute) }
    }
}
