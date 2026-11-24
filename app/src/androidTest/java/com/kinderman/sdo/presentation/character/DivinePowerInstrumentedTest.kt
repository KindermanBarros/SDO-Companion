package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterDivineAscension
import com.kinderman.sdo.domain.model.CharacterDivineRuntime
import com.kinderman.sdo.domain.model.DivineLink
import com.kinderman.sdo.domain.model.DivineLinkKind
import com.kinderman.sdo.domain.model.DivinePillars
import com.kinderman.sdo.domain.model.DivineTier
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.SdoTheme
import com.kinderman.sdo.ui.SdoVisualMode
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DivinePowerInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun standardPlayerAccessAndGmCapacitySettlement() =
        verifyAccessAndSettlement(SdoVisualMode.STANDARD)

    @Test
    fun cybergrungePlayerAccessAndGmCapacitySettlement() =
        verifyAccessAndSettlement(SdoVisualMode.CYBERGRUNGE)

    private fun verifyAccessAndSettlement(mode: SdoVisualMode) {
        var gm by mutableStateOf(false)
        var character by mutableStateOf(Character(
            divineAscension = CharacterDivineAscension(
                tier = DivineTier.LESSER_GOD,
                pillars = DivinePillars(2, 2, 2, 1),
            ),
            destiny = ResourceValue(current = 4),
            divineRuntime = CharacterDivineRuntime(links = listOf(
                DivineLink("avatar", DivineLinkKind.AVATAR),
                DivineLink("anchor-old"),
                DivineLink("anchor-new"),
            )),
        ))
        var changes = 0
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    CharacterDivinePowerSection(
                        character = character,
                        editable = true,
                        canChangeAscension = gm,
                        onChange = { character = it; changes += 1 },
                    )
                }
                }
            }
        }

        compose.onNodeWithTag("divine_power_capacity").assertIsDisplayed()
        compose.onNodeWithTag("divine_power_link_avatar").performScrollTo().assertIsDisplayed()
        listOf("settle", "add_link", "suppress", "restore", "milestone", "fate",
            "toggle_avatar", "release_avatar").forEach { action ->
            compose.onNodeWithTag("character_divine_power_$action").assertDoesNotExist()
        }
        compose.onNodeWithTag("divine_power_scene").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, changes)
            gm = true
        }

        compose.onNode(
            hasSetTextAction() and (
                hasTestTag("divine_power_scene") or
                    hasAnyAncestor(hasTestTag("divine_power_scene"))
            ),
            useUnmergedTree = true,
        ).performScrollTo().performTextInput("scene-overload")
        closeSoftKeyboard()
        compose.onNodeWithTag("character_divine_power_settle").performScrollTo().performClick()

        compose.runOnIdle {
            assertEquals(2, character.destiny.current)
            assertEquals(2, character.divineRuntime.pdSpentInCurrentScene)
            assertEquals(1, changes)
        }
        compose.onNodeWithTag("character_divine_power_settle").performScrollTo().performClick()
        compose.onNodeWithTag("divine_power_capacity").performScrollTo()
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val capture = instrumentation.uiAutomation.takeScreenshot()
        val destination = File(instrumentation.targetContext.getExternalFilesDir(null), "divine-power-" + mode.name + ".png")
        destination.outputStream().use { capture.compress(Bitmap.CompressFormat.PNG, 100, it) }
        capture.recycle()
        compose.runOnIdle {
            assertEquals(2, character.destiny.current)
            assertEquals(2, character.divineRuntime.pdSpentInCurrentScene)
            assertEquals(1, changes)
        }
    }
}
