package com.kinderman.sdo.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterCreationStatus
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.presentation.character.CharacterSheetScreen
import org.junit.Rule
import org.junit.Test

class CharacterSheetAccessInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun cybergrungePreservesArchivedReadOnlyExplanation() {
        mountArchivedSheet(SdoVisualMode.CYBERGRUNGE)
        compose.onNodeWithText("Campanha arquivada · somente leitura").assertExists()
    }

    @Test
    fun standardPreservesArchivedReadOnlyExplanation() {
        mountArchivedSheet(SdoVisualMode.STANDARD)
        compose.onNodeWithText("Campanha arquivada · somente leitura").assertExists()
    }

    private fun mountArchivedSheet(mode: SdoVisualMode) {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                CharacterSheetScreen(
                    character = Character(
                        id = "archived-sheet",
                        ownerId = "owner",
                        name = "Archived character",
                        creationStatus = CharacterCreationStatus.COMPLETED,
                    ),
                    session = UserSession("owner", "", "Owner", UserRole.USER),
                    catalog = emptyList(),
                    readOnly = true,
                    snackbarHost = {},
                    onBack = {},
                    onClose = {},
                    onOpenSession = {},
                    onSave = {},
                    onAutosave = {},
                    onPlayerLock = { _, _ -> },
                    onHistorianLock = { _, _ -> },
                    onDelete = {},
                )
            }
        }
    }
}
