package com.kinderman.sdo.presentation.dashboard

import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.Character
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.kinderman.sdo.ui.SdoNavigationBar
import com.kinderman.sdo.ui.SdoNavigationItem
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Assert.assertEquals
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.presentation.character.captureReviewScreenshot
import com.kinderman.sdo.presentation.historian.HistorianDashboardScreen
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.SdoTheme
import com.kinderman.sdo.ui.SdoVisualMode
import org.junit.Rule
import org.junit.Test

class CompactDashboardLibraryTest {
    @get:Rule
    val compose = createComposeRule()

    private val admin = UserSession("admin", "admin@example.invalid", "Admin", UserRole.ADMIN)

    @Test
    fun dashboardFiltersCommitAndCancelPendingStatus() {
        compose.setContent {
            SdoTheme(SdoPreferences()) {
                Box(Modifier.width(320.dp).height(800.dp)) {
                    DashboardScreen(
                        characters = emptyList(), campaigns = emptyList(), memberships = emptyList(),
                        campaignInvites = emptyList(), campaignMembers = emptyList(), owners = emptyList(),
                        session = admin, syncing = false, compactCards = true, collapseCampaignCards = true,
                        showArchivedCampaigns = false, invitePreview = null, snackbarHost = {},
                        onAdd = {}, onAddToCampaign = { _, _ -> }, onLinkCharacter = { _, _ -> },
                        onUnlinkCharacter = {}, onOpen = {}, onOwnerTransfer = { _, _ -> },
                        onCreateCampaign = { _, _ -> }, onArchiveCampaign = { _, _ -> }, onDeleteCampaign = {},
                        onLeaveCampaign = {}, onPreviewInvite = {}, onAcceptInvite = { _, _, _ -> },
                        onDismissInvitePreview = {}, onSync = {}, onOpenSession = {}, onOpenHistorian = {},
                        onOpenSettings = {}, deliveries = emptyList(), onRespondDelivery = { _, _ -> }, onLogout = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("dashboard_quick_session").assertDoesNotExist()
        compose.onNodeWithTag("dashboard_quick_historian").assertDoesNotExist()
        compose.onNodeWithTag("dashboard_quick_settings").assertExists()
        val settings = compose.onNodeWithTag("dashboard_quick_settings").fetchSemanticsNode().boundsInRoot
        val sync = compose.onNodeWithContentDescription("Sincronizar").fetchSemanticsNode().boundsInRoot
        assertEquals(sync.center.y, settings.center.y, 1f)

        compose.onNodeWithText("OWNER // TODOS OS OWNERS", ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithTag("dashboard_filter_trigger").performScrollTo().performClick()
        compose.onNodeWithTag("dashboard_filter_status").performClick()
        compose.onNodeWithTag("dashboard_filter_option_PENDING").performClick()
        compose.onNodeWithTag("dashboard_filters_apply").performClick()
        compose.onNodeWithTag("dashboard_filter_trigger").performClick()
        compose.onNodeWithTag("dashboard_filter_status").assertTextContains("Pendentes", ignoreCase = true)
        compose.onNodeWithTag("dashboard_filter_status").performClick()
        compose.onNodeWithTag("dashboard_filter_option_LOCKED").performClick()
        compose.onNodeWithTag("dashboard_filters_cancel").performClick()
        compose.onNodeWithTag("dashboard_filter_trigger").performClick()
        compose.onNodeWithTag("dashboard_filter_status").assertTextContains("Pendentes", ignoreCase = true)
    }

    @Test
    fun libraryOriginAndCategoryUseModalChoicesInCybergrunge() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE)) {
                Box(Modifier.width(320.dp).height(800.dp)) {
                    HistorianDashboardScreen(
                        session = admin, campaigns = emptyList(), memberships = emptyList(), characters = emptyList(),
                        catalog = listOf(CatalogEntry("knowledge", CatalogKind.ACQUIRED_KNOWLEDGE, "Atuação", "Arte", "Descrição")),
                        audit = emptyList(), library = emptyList(), deliveries = emptyList(),
                        alertSettings = emptyList(), onApplyCommand = { _, _ -> }, onSaveLibrary = {},
                        onDuplicateLibrary = {}, onArchiveLibrary = { _, _ -> }, onDeliverLibrary = { _, _, _ -> },
                        onSaveAlertSettings = {}, onOpenSession = {}, onOpenSheet = {}, onBack = {},
                    )
                }
            }
        }

        compose.onNodeWithText("BIBLIOTECA", substring = true).performClick()
        compose.onNodeWithText("HABILIDADES", substring = true).assertDoesNotExist()
        captureReviewScreenshot("cybergrunge-library")
        compose.onNodeWithTag("library_origin_trigger").performScrollTo().performClick()
        compose.onNodeWithTag("library_choice_CATALOG").assertIsDisplayed().performClick()
        compose.onNodeWithTag("library_origin_trigger").assertTextContains("CATÁLOGO", substring = true, ignoreCase = true)
        compose.onNodeWithTag("library_pending_count").performScrollTo().assertWidthIsAtLeast(100.dp)
        compose.onNodeWithTag("historian_screen_list").performScrollToNode(hasText("CONHECIMENTO ADQUIRIDO", ignoreCase = true))
        compose.onNodeWithText("CONHECIMENTO ADQUIRIDO", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("ACQUIRED_KNOWLEDGE").assertDoesNotExist()
        compose.onNodeWithTag("historian_screen_list").performScrollToNode(hasTestTag("library_category_trigger"))
        compose.onNodeWithTag("library_category_trigger").performClick()
        compose.onNodeWithTag("library_choice_ABILITY").assertIsDisplayed().performClick()
        compose.onNodeWithTag("library_category_trigger").assertTextContains("HABILIDADES", substring = true, ignoreCase = true)
        compose.onNodeWithTag("library_category_trigger").performClick()
        compose.onNodeWithTag("library_choice_ABILITY").assertIsSelected()
        captureReviewScreenshot("cybergrunge-library-selection")
    }

    @Test
    fun masterQuickActionIsVisibleWithoutExpandingCharacter() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE)) {
                Box(Modifier.width(320.dp).height(800.dp)) {
                    HistorianDashboardScreen(
                        session = admin, campaigns = listOf(Campaign(id = "fixture", ownerId = admin.uid)), memberships = emptyList(), characters = listOf(Character(id = "hero", name = "Teste", campaignId = "fixture")),
                        catalog = listOf(CatalogEntry("knowledge", CatalogKind.ACQUIRED_KNOWLEDGE, "Atuação", "Arte", "Descrição")),
                        audit = emptyList(), library = emptyList(), deliveries = emptyList(),
                        alertSettings = emptyList(), onApplyCommand = { _, _ -> }, onSaveLibrary = {},
                        onDuplicateLibrary = {}, onArchiveLibrary = { _, _ -> }, onDeliverLibrary = { _, _, _ -> },
                        onSaveAlertSettings = {}, onOpenSession = {}, onOpenSheet = {}, onBack = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("historian_screen_list").performScrollToNode(hasText("AÇÃO RÁPIDA"))
        compose.onNodeWithText("AÇÃO RÁPIDA").assertIsDisplayed()
        captureReviewScreenshot("cybergrunge-operation")
        compose.onNodeWithText("AÇÃO RÁPIDA").performClick()
    }
    @Test
    fun compactCyberNavigationRetainsVisibleLabels() {
        compose.setContent {
            SdoTheme(SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE)) {
                Box(Modifier.width(360.dp)) {
                    SdoNavigationBar(listOf("Painel", "Sessão", "Mestre", "Ajustes").map {
                        SdoNavigationItem(it, it == "Mestre", Icons.Default.Settings) {}
                    })
                }
            }
        }
        compose.onNodeWithText("MESTRE").assertIsDisplayed()
        captureReviewScreenshot("cybergrunge-compact-navbar")
    }}
