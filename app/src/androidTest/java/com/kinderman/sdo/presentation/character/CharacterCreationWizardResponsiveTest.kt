package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class CharacterCreationWizardResponsiveTest {
    @get:Rule val compose = createComposeRule()

    @Test fun navigationStacksOnNarrowScreens() {
        compose.setContent {
            Box(Modifier.requiredWidth(360.dp)) {
                CharacterCreationWizard(Character(creationStep = 9), emptyList(), true, {})
            }
        }
        val back = compose.onNodeWithTag("creation-back").fetchSemanticsNode().boundsInRoot.top
        val forward = compose.onNodeWithTag("creation-forward").fetchSemanticsNode().boundsInRoot.top

        assertNotEquals(back, forward)
    }

    @Test fun navigationSharesARowOnWideScreens() {
        compose.setContent {
            Box(Modifier.requiredWidth(700.dp)) {
                CharacterCreationWizard(Character(creationStep = 9), emptyList(), true, {})
            }
        }
        val back = compose.onNodeWithTag("creation-back").fetchSemanticsNode().boundsInRoot.top
        val forward = compose.onNodeWithTag("creation-forward").fetchSemanticsNode().boundsInRoot.top

        assertEquals(back, forward, 1f)
    }
}
