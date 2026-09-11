package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class CharacterCreationWizardResponsiveTest {
    @get:Rule val compose = createComposeRule()

    @Test fun navigationStacksOnNarrowScreensAndSharesARowOnWideScreens() {
        fun positions(width: Int): Pair<Float, Float> {
            compose.setContent {
                Box(Modifier.width(width.dp)) {
                    CharacterCreationWizard(Character(creationStep = 9), emptyList(), true, {})
                }
            }
            val back = compose.onNodeWithTag("creation-back").fetchSemanticsNode().boundsInRoot.top
            val forward = compose.onNodeWithTag("creation-forward").fetchSemanticsNode().boundsInRoot.top
            return back to forward
        }

        val narrow = positions(360)
        assertNotEquals(narrow.first, narrow.second)
        val wide = positions(700)
        assertEquals(wide.first, wide.second, 1f)
    }
}
