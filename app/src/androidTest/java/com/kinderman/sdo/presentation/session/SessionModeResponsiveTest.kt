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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import androidx.compose.runtime.mutableStateOf
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AbilityCost
import com.kinderman.sdo.domain.model.AbilityKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SpendableResource
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SessionResource
import com.kinderman.sdo.domain.model.ConditionKind
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.InventoryState
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
    fun favoritesFilterSurvivesRestorationAndCanBeCleared() {
        val restoration = StateRestorationTester(compose)
        val other = character.abilities.single().copy(id = "other", name = "Outra habilidade", favorite = false)
        val filtered = character.copy(abilities = listOf(character.abilities.single().copy(favorite = true), other))
        restoration.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(filtered), filtered.id, true, false, {}, {}, { _, _ -> }, {}, onInventoryStateChange = { _, _, _ -> })
            }
        }
        jumpTo("Habilidades")
        compose.onNodeWithTag("session_favorites").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Ímpeto Brutal"))
        compose.onNodeWithText("Outra habilidade").assertDoesNotExist()
        jumpTo("Habilidades")
        compose.onNodeWithTag("session_clear_filters").performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Outra habilidade"))
        compose.onNodeWithText("Outra habilidade").assertIsDisplayed()
    }

    @Test
    fun playersCanSpendEnergyWithoutOpeningTheSheet() {
        val commands = mutableListOf<SessionCommand>()
        val resourceCharacter = character.copy(energy = ResourceValue(current = 5, maximum = 10))
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(resourceCharacter), resourceCharacter.id, true, false, {}, {}, { _, command -> commands += command }, {}, onInventoryStateChange = { _, _, _ -> })
            }
        }
        jumpTo("Recursos rápidos")
        compose.onNodeWithContentDescription("Reduzir ENERGIA").performClick()
        compose.runOnIdle {
            assertEquals(SessionResource.ENERGY, commands.single().resource)
            assertEquals(-1, commands.single().amount)
        }
    }

    @Test
    fun playersCanAddAConditionWithConfirmation() {
        val commands = mutableListOf<SessionCommand>()
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(character), character.id, true, false, {}, {}, { _, command -> commands += command }, {}, onInventoryStateChange = { _, _, _ -> })
            }
        }
        jumpTo("Condições ativas")
        compose.onNodeWithTag("session_add_condition").performClick()
        compose.runOnIdle { assertEquals(0, commands.size) }
        compose.onNodeWithTag("session_confirm_condition").performClick()
        compose.runOnIdle {
            assertEquals(SessionOperationType.CONDITION_ADD, commands.single().type)
            assertEquals(ConditionKind.Abalado, commands.single().condition!!.kind)
        }
    }

    @Test
    fun playerCanEquipAnItemThroughTheExistingInventoryStates() {
        val equipped = mutableListOf<InventoryState>()
        val inventoryCharacter = character.copy(inventory = listOf(InventoryItem(id = "armor", name = "Colete", category = "Armadura", state = "G")))
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(inventoryCharacter), inventoryCharacter.id, true, false, {}, {}, { _, _ -> }, {}, onInventoryStateChange = { _, _, next -> equipped += next })
            }
        }
        jumpTo("Equipamento pronto")
        compose.onNodeWithTag("session_item_state_armor").performClick()
        compose.onNodeWithText(InventoryState.EQUIPPED.label).performClick()
        compose.runOnIdle { assertEquals(listOf(InventoryState.EQUIPPED), equipped) }
    }

    @Test
    fun capturesPlayerQuickActionsInBothVisualModes() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(character), character.id, true, false, {}, {}, { _, _ -> }, {}, onInventoryStateChange = { _, _, _ -> })
            }
        }
        compose.onNodeWithTag("session_damage").assertIsDisplayed()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = File(instrumentation.targetContext.getExternalFilesDir(null), "session-${mode.name}.png")
        target.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun jumpTo(label: String) {
        compose.onNodeWithTag("session_section_jump").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText(label).onLast().performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun damageConfirmationIsDisabledWhenConnectionBecomesReadOnly() {
        val readOnly = mutableStateOf(false)
        val commands = mutableListOf<SessionCommand>()
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(character), character.id, true, readOnly.value, {}, {}, { _, command -> commands += command }, {}, onInventoryStateChange = { _, _, _ -> })
            }
        }

        compose.onNodeWithText("Aplicar dano", ignoreCase = true).performClick()
        compose.runOnIdle { readOnly.value = true }
        compose.onNodeWithText("CONFIRMAR").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, commands.size) }
    }

    @Test
    fun useIsAvailableWhileDetailsAreCollapsedAndRequiresConfirmation() {
        val commands = mutableListOf<SessionCommand>()
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(character), character.id, true, false, {}, {}, { _, command -> commands += command }, {}, onInventoryStateChange = { _, _, _ -> })
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
                SessionModeScreen(listOf(unavailable), unavailable.id, true, false, {}, {}, { _, _ -> }, {}, onInventoryStateChange = { _, _, _ -> })
            }
        }

        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Ímpeto Brutal"))
        compose.onNodeWithTag("session_ability_use").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun expandedAbilityKeepsItsFullRules() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                SessionModeScreen(listOf(character), character.id, true, false, {}, {}, { _, _ -> }, {}, onInventoryStateChange = { _, _, _ -> })
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
                SessionModeScreen(listOf(filteredCharacter), filteredCharacter.id, true, false, {}, {}, { _, _ -> }, {}, onInventoryStateChange = { _, _, _ -> })
            }
        }

        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("session_ability_filter"))
        compose.onNodeWithText("Tipo // Todas", ignoreCase = true).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Magia", ignoreCase = true).fetchSemanticsNodes().isNotEmpty() }
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
                SessionModeScreen(listOf(character), character.id, true, false, {}, {}, { _, _ -> }, {}, onInventoryStateChange = { _, _, _ -> })
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
