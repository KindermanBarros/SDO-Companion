package com.kinderman.sdo.presentation.character

import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AbilityCost
import com.kinderman.sdo.domain.model.AbilityKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.SdoTheme
import com.kinderman.sdo.ui.SdoVisualMode
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Synthetic component captures supplement owner review; they are not pixel goldens. */
@RunWith(Parameterized::class)
class CharacterVisualReviewTest(private val mode: SdoVisualMode) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun bodyAndAttributeHeadersRemainVisible() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                HudBackground {
                    Column(Modifier.requiredWidth(360.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
                        BodyRegionSection(Character(), 6, false, {}, {})
                        AttributeSection(Character(), false, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Atributos e conhecimentos", ignoreCase = true).assertIsDisplayed()
        saveCliCapture("body-attributes")
    }

    @Test
    fun itemAndAbilityCardsRetainReadableContent() {
        val fixture = Character(
            abilities = listOf(Ability(
                name = "Sombra Autônoma", kind = AbilityKind.POWER,
                cost = AbilityCost.PowerCost(), effect = "Interaja com um objeto pela sombra.",
            )),
            inventory = listOf(InventoryItem(name = "Adaga de Ligas Comuns", category = "Arma", load = 1)),
        )
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                HudBackground {
                    Column(Modifier.requiredWidth(360.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
                        AbilitySection(fixture, emptyList(), setOf(AbilityKind.POWER), false, {})
                        PhaseOneStrictInventorySection(fixture, emptyList(), false, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Sombra Autônoma").assertIsDisplayed()
        saveCliCapture("item-ability")
    }

    private fun saveCliCapture(name: String) {
        compose.waitForIdle()
        captureReviewScreenshot("${mode.name.lowercase()}-$name")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun modes(): List<Array<Any>> = SdoVisualMode.entries.map { arrayOf<Any>(it) }
    }
}

internal fun captureReviewScreenshot(name: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "compact-review")
    check(folder.isDirectory || folder.mkdirs())
    val capture = instrumentation.uiAutomation.executeShellCommand("screencap -p")
    ParcelFileDescriptor.AutoCloseInputStream(capture).use { input ->
        File(folder, "$name.png").outputStream().use { output -> input.copyTo(output) }
    }
}
