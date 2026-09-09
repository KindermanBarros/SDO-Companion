package com.kinderman.sdo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kinderman.sdo.domain.catalog.BuiltInCatalog
import com.kinderman.sdo.domain.catalog.KnowledgeCatalog
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.presentation.character.CharacterSheetPager
import com.kinderman.sdo.presentation.session.SessionModeScreen
import com.kinderman.sdo.ui.*
import org.junit.Rule
import org.junit.Test

class UiIntegrityTest {
    @get:Rule val compose = createComposeRule()
    private val character = Character(id = "smoke", ownerId = "tester", name = "Teste de integridade")
    private val session = UserSession("tester", "", "Teste", UserRole.USER)

    @Test fun allSheetPagesRenderAndNavigateWithCanonicalCatalog() {
        compose.setContent {
            SdoTheme {
                CharacterSheetPager(character, session, BuiltInCatalog.entries + KnowledgeCatalog.entries,
                    editable = true, showCalculationAudit = true, onChange = {})
            }
        }
        listOf("APTIDÕES", "CAMINHO", "PODERES", "CORPO", "MÍSTICO", "REGISTRO", "ANOTAÇÕES", "PERFIL").forEach { page ->
            compose.onNodeWithText("Ir para:", substring = true).performClick()
            compose.onAllNodesWithText(page).onLast().performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Ir para: $page").assertIsDisplayed()
        }
    }

    @Test fun compactFieldsLoadingAndActionLabelsRenderInEveryTheme() {
        val preferences = mutableStateOf(SdoPreferences())
        compose.setContent {
            SdoTheme(preferences.value) {
                Column(Modifier.width(320.dp)) {
                    HudTextField("Nome", "Teste", onValue = {})
                    HudTextField("Notas", "Texto\ncom várias linhas", multiline = true, onValue = {})
                    AdaptiveActionLabel("+ Descanso")
                    CyberLoadingIndicator("Carregando")
                }
            }
        }
        for (theme in SdoThemeVariant.entries) for (font in SdoFontScale.entries) {
            compose.runOnIdle { preferences.value = SdoPreferences(theme = theme, fontScale = font) }
            compose.onNodeWithText("+ Descanso").assertIsDisplayed()
            compose.onNodeWithContentDescription("Carregando").assertExists()
        }
    }

    @Test fun sessionDamageDialogHandlesCharacterWithoutBodyRegions() {
        compose.setContent {
            SdoTheme {
                SessionModeScreen(listOf(character.copy(bodyRegions = emptyList())), character.id,
                    compact = true, readOnly = false, onSelect = {}, onOpenSheet = {},
                    onCommand = { _, _ -> }, onBack = {})
            }
        }
        compose.onNodeWithText(" DANO").performClick()
        compose.onNodeWithText("›").performClick()
        compose.onNodeWithText("Geral").assertIsDisplayed()
        compose.onNodeWithText("CANCELAR").performClick()
    }
}
