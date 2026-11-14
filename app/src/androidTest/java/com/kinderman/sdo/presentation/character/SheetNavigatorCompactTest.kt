package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.SdoTheme
import com.kinderman.sdo.ui.SdoVisualMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SheetNavigatorCompactTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun shortChoiceValueRemainsReadableInsideNarrowSheet() {
        compose.setContent {
            SdoTheme {
                Box(Modifier.requiredWidth(240.dp)) {
                    TwoFields(
                        { ChoiceField("Categoria", "Cinza", listOf("Cinza"), true, it) {} },
                        { ChoiceField("Execução", "Ação", listOf("Ação"), true, it) {} },
                    )
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Categoria // Cinza").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult,
        ) { it(layouts) }
        assertFalse("Short choice value must be fully visible", layouts.single().isLineEllipsized(0))
    }

    @Test
    fun cyberBodyNavigationKeepsPositionOnOneLine() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE)) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                  Box(Modifier.requiredWidth(320.dp)) {
                    CharacterSheetPager(
                        character = Character(),
                        session = UserSession("fixture", "test@example.invalid", "Test", UserRole.ADMIN),
                        catalog = emptyList(),
                        editable = false,
                        onChange = {},
                    )
                  }
                }
            }
        }
        compose.onNodeWithContentDescription("Escolher seção").performClick()
        compose.onNodeWithText("CORPO").performClick()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("SEÇÃO 7 DE 10").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult,
        ) { it(layouts) }
        assertEquals("Navigation position must not wrap", 1, layouts.single().lineCount)
        assertFalse("Navigation position must remain fully readable", layouts.single().isLineEllipsized(0))
    }
}
