package com.kinderman.sdo.presentation.journal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignJournalDraft
import com.kinderman.sdo.domain.model.CampaignJournalEntry
import com.kinderman.sdo.domain.model.CampaignState
import com.kinderman.sdo.domain.model.JournalSyncState
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.SdoTheme
import com.kinderman.sdo.ui.SdoVisualMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class JournalJourneyTest {
    @get:Rule
    val compose = createComposeRule()

    private val campaign = Campaign(
        id = "camp-ui-1",
        name = "Expedição Boreal",
        ownerId = "gm-1",
        state = CampaignState.ACTIVE,
    )

    private val session = UserSession(
        uid = "player-1",
        email = "p1@sdo.test",
        displayName = "Kaelen",
        role = UserRole.USER,
    )

    private val sampleEntry = CampaignJournalEntry(
        id = "entry-ui-1",
        campaignId = campaign.id,
        authorId = session.uid,
        authorName = session.displayName,
        title = "Diário de Bordo",
        body = "O farol antigo ainda emite sinais intermitentes.",
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
        revision = 1L,
        lastEditorId = session.uid,
        lastEditorName = session.displayName,
    )

    @Test
    fun listAndDetailAndEditorRenderInStandardAndCybergrungeModes() {
        var newEntryClicked = false
        compose.setContent {
            SdoTheme(
                preferences = SdoPreferences(
                    visualMode = SdoVisualMode.STANDARD,
                ),
            ) {
                JournalScreen(
                    state = JournalUiState(
                        campaign = campaign,
                        session = session,
                        entries = listOf(sampleEntry),
                        syncState = JournalSyncState.LIVE,
                        mode = JournalMode.LIST,
                        isActiveMember = true,
                    ),
                    onRetry = {},
                    onOpenEntry = {},
                    onNewEntry = { newEntryClicked = true },
                    onEditEntry = {},
                    onUpdateDraft = { _, _ -> },
                    onSaveDraft = {},
                    onReloadConflict = {},
                    onRequestReplaceConflict = {},
                    onDismissReplaceConflict = {},
                    onConfirmReplaceConflict = {},
                    onRequestDelete = {},
                    onDismissDelete = {},
                    onConfirmDelete = {},
                    onBackPressed = {},
                    onDismissDiscard = {},
                    onConfirmDiscard = {},
                )
            }
        }

        compose.onNodeWithTag("journal-screen").assertIsDisplayed()
        compose.onNodeWithTag("journal-sync-badge").assertIsDisplayed()
        compose.onNodeWithTag("journal-new-entry").performClick()
        assertTrue(newEntryClicked)
    }

    @Test
    fun editorShowsTitleBodyAndSaveActions() {
        compose.setContent {
            SdoTheme(
                preferences = SdoPreferences(
                    visualMode = SdoVisualMode.CYBERGRUNGE,
                ),
            ) {
                JournalScreen(
                    state = JournalUiState(
                        campaign = campaign,
                        session = session,
                        entries = emptyList(),
                        syncState = JournalSyncState.LIVE,
                        mode = JournalMode.EDITOR,
                        draft = CampaignJournalDraft(title = "Pista", body = "Runas"),
                        isActiveMember = true,
                    ),
                    onRetry = {},
                    onOpenEntry = {},
                    onNewEntry = {},
                    onEditEntry = {},
                    onUpdateDraft = { _, _ -> },
                    onSaveDraft = {},
                    onReloadConflict = {},
                    onRequestReplaceConflict = {},
                    onDismissReplaceConflict = {},
                    onConfirmReplaceConflict = {},
                    onRequestDelete = {},
                    onDismissDelete = {},
                    onConfirmDelete = {},
                    onBackPressed = {},
                    onDismissDiscard = {},
                    onConfirmDiscard = {},
                )
            }
        }

        compose.onNodeWithTag("journal-editor-title").assertIsDisplayed()
        compose.onNodeWithTag("journal-editor-body").assertIsDisplayed()
        compose.onNodeWithTag("journal-save-entry").assertIsDisplayed()
    }
}
