package com.kinderman.sdo.presentation.sync

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import com.kinderman.sdo.domain.model.OperationType
import com.kinderman.sdo.domain.model.SheetConflictItem
import com.kinderman.sdo.domain.model.SheetIntent
import com.kinderman.sdo.domain.model.SheetIntentStatus
import com.kinderman.sdo.presentation.character.captureReviewScreenshot
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.SdoTheme
import com.kinderman.sdo.ui.SdoVisualMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SparkSyncUiIntegrationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun standardConflictShowsThreeVersionsAndSelectsRemote() {
        verifyConflict(SdoVisualMode.STANDARD, "spark-conflict-standard")
    }

    @Test fun cybergrungeConflictShowsThreeVersionsAndSelectsRemote() {
        verifyConflict(SdoVisualMode.CYBERGRUNGE, "spark-conflict-cybergrunge")
    }

    @Test fun accessDeniedDraftCanBeReviewedAndRetried() {
        var retries = 0
        val intent = SheetIntent(
            operationId = "draft-1", accountId = "account-1", sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS, targetKey = "field:name",
            baseValueJson = "Original name", desiredPayloadJson = "Recovered name",
            status = SheetIntentStatus.ACCESS_DENIED,
        )
        compose.setContent {
            SdoTheme(SdoPreferences()) {
                AccessDeniedDraftDialog(
                    intent = intent, characterName = "Saved character",
                    onRetry = { retries++ }, onDiscard = {}, onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("Original name").assertIsDisplayed()
        compose.onNodeWithText("Recovered name").assertIsDisplayed()
        compose.waitForIdle()
        captureReviewScreenshot("spark-access-denied-standard")
        compose.onNodeWithText("TENTAR NOVAMENTE").performClick()

        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun manualMergeEditsTextAndSuppliesTypedCustomValue() {
        var resolved: Pair<Boolean, String?>? = null
        compose.setContent {
            SdoTheme(SdoPreferences()) {
                CharacterConflictDialog(conflict = manualConflict("\"Local name\""),
                    characterName = "Saved character",
                    onResolve = { remote, custom -> resolved = remote to custom }, onDiscard = {})
            }
        }
        compose.onNodeWithTag("conflict_choose_manual").performScrollTo().performClick()
        compose.onNodeWithTag("conflict_manual_value_0").performScrollTo()
            .performTextReplacement("Merged name")
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        captureReviewScreenshot("spark-conflict-manual-standard")
        compose.onNodeWithTag("conflict_apply").performClick()
        compose.runOnIdle { assertEquals(false to "\"Merged name\"", resolved) }
    }

    @Test fun invalidManualNumberDisablesApply() {
        compose.setContent {
            SdoTheme(SdoPreferences()) {
                CharacterConflictDialog(conflict = manualConflict("10", "field:money"),
                    onResolve = { _, _ -> }, onDiscard = {})
            }
        }
        compose.onNodeWithTag("conflict_choose_manual").performScrollTo().performClick()
        compose.onNodeWithTag("conflict_manual_value_0").performScrollTo()
            .performTextReplacement("invalid")
        compose.onNodeWithTag("conflict_apply").assertIsNotEnabled()
    }

    private fun manualConflict(payload: String, target: String = "field:name") = SheetConflictItem(
        id = "manual-conflict", accountId = "account", sheetId = "sheet", operationId = "op",
        targetKey = target, label = "Campo", baseRevision = 1, remoteRevision = 2,
        baseSummary = "Base", localSummary = "Local", remoteSummary = "Online",
        basePayloadJson = payload, localPayloadJson = payload, remotePayloadJson = payload,
    )

    private fun verifyConflict(mode: SdoVisualMode, screenshot: String) {
        var resolvedRemote: Boolean? = null
        val conflict = SheetConflictItem(
            id = "conflict-1", accountId = "account-1", sheetId = "sheet-1",
            operationId = "operation-1", targetKey = "field:name", label = "Nome",
            baseRevision = 1, remoteRevision = 2,
            baseSummary = "Original name", localSummary = "Local name", remoteSummary = "Remote name",
            basePayloadJson = "\"Original name\"", localPayloadJson = "\"Local name\"",
            remotePayloadJson = "\"Remote name\"",
        )
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = mode)) {
                CharacterConflictDialog(
                    conflict = conflict, characterName = "Saved character",
                    onResolve = { useRemote, _ -> resolvedRemote = useRemote }, onDiscard = {},
                )
            }
        }

        compose.onNodeWithText("Original name").assertIsDisplayed()
        compose.onNodeWithText("Local name").assertIsDisplayed()
        compose.onNodeWithText("Remote name").assertIsDisplayed().performClick()
        compose.waitForIdle()
        captureReviewScreenshot(screenshot)
        compose.onNodeWithText("APLICAR ESCOLHA").performClick()

        compose.runOnIdle { assertEquals(true, resolvedRemote) }
    }
}
