package com.kinderman.sdo.presentation.session

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AbilityCost
import com.kinderman.sdo.domain.model.AbilityKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SpendableResource
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.SdoTheme
import com.kinderman.sdo.ui.SdoVisualMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SessionModeResponsiveTest(private val mode: SdoVisualMode) {
    @get:Rule
    val compose = createComposeRule()

    private val character = Character(
        id = "session-character",
        name = "Ayla",
        abilities = listOf(
            Ability(
                id = "session-power",
                name = "Ímpeto Brutal",
                kind = AbilityKind.POWER,
                cost = AbilityCost.PowerCost(1, SpendableResource.PE),
                effect = "Após mover, cause dano adicional.",
            ),
        ),
    )

    @Test
    fun useIsAvailableWhileDetailsAreCollapsedAndRequiresConfirmation() {
        val commands = mutableListOf<SessionCommand>()
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(character), character.id, true, false, {}, {}, { _, command -> commands += command }, {})
            }
        }

        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Ímpeto Brutal"))
        compose.onNodeWithText("PODER // 1 PE").assertIsDisplayed()
        compose.onNodeWithText("Usar", ignoreCase = true).assertIsDisplayed().performClick()
        compose.onNodeWithText("CONFIRMAR USO").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, commands.size) }
        compose.onNodeWithTag("session_confirm_ability").performClick()
        compose.runOnIdle {
            assertEquals(SessionOperationType.ABILITY_USE, commands.single().type)
            assertEquals("session-power", commands.single().targetId)
        }
    }

    @Test
    fun unavailableAbilityCannotBeUsed() {
        val unavailable = character.copy(abilities = listOf(character.abilities.single().copy(available = false)))
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(unavailable), unavailable.id, true, false, {}, {}, { _, _ -> }, {})
            }
        }

        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Ímpeto Brutal"))
        compose.onNodeWithTag("session_ability_use").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun expandedAbilityKeepsItsFullRules() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(character), character.id, true, false, {}, {}, { _, _ -> }, {})
            }
        }

        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Ímpeto Brutal"))
        compose.onNodeWithTag("session_ability_expand").performClick()
        compose.onNodeWithText("Após mover, cause dano adicional.").assertIsDisplayed()
        compose.onNodeWithText("Usar habilidade", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithTag("session_ability_expand").performClick()
        compose.onNodeWithText("Após mover, cause dano adicional.").assertDoesNotExist()
    }

    @Test
    fun abilityTypeFilterSurvivesStateRestoration() {
        val other = Ability(
            id = "session-spell",
            name = "Faísca Condutora",
            kind = AbilityKind.SPELL,
            cost = AbilityCost.SpellCost(2),
            effect = "Causa dano elétrico.",
        )
        val filteredCharacter = character.copy(abilities = character.abilities + other)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(filteredCharacter), filteredCharacter.id, true, false, {}, {}, { _, _ -> }, {})
            }
        }

        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("session_ability_filter"))
        compose.onNodeWithText("Tipo // Todas", ignoreCase = true).performClick()
        compose.onNodeWithText("Magia", ignoreCase = true).performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Faísca Condutora"))
        compose.onNodeWithText("Ímpeto Brutal").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Faísca Condutora"))
        compose.onNodeWithText("Ímpeto Brutal").assertDoesNotExist()
    }

    @Test
    fun pendingDamageAmountSurvivesStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(character), character.id, true, false, {}, {}, { _, _ -> }, {})
            }
        }

        compose.onNodeWithText("Aplicar dano", ignoreCase = true).performClick()
        compose.onNodeWithContentDescription("Aumentar").performClick()
        compose.onNodeWithTag("session_damage_amount").assertTextEquals("2")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("session_damage_amount").assertIsDisplayed().assertTextEquals("2")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun modes(): List<SdoVisualMode> = SdoVisualMode.entries
    }
}
