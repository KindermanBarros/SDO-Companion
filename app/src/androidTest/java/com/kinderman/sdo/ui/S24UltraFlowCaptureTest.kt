package com.kinderman.sdo.ui

import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kinderman.sdo.domain.catalog.BuiltInCatalog
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AbilityCost
import com.kinderman.sdo.domain.model.AbilityKind
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignMember
import com.kinderman.sdo.domain.model.CampaignRole
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterConflictField
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.PersonalNote
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SpendableResource
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.presentation.character.CharacterSheetScreen
import com.kinderman.sdo.presentation.character.ConditionSection
import com.kinderman.sdo.presentation.character.NarrativeSection
import com.kinderman.sdo.presentation.character.NotesSection
import com.kinderman.sdo.presentation.character.PathSection
import com.kinderman.sdo.presentation.character.PhaseOneStrictInventorySection
import com.kinderman.sdo.presentation.dashboard.DashboardScreen
import com.kinderman.sdo.presentation.historian.HistorianDashboardScreen
import com.kinderman.sdo.presentation.library.LibraryScreen
import com.kinderman.sdo.presentation.session.SessionModeScreen
import com.kinderman.sdo.presentation.settings.SettingsScreen
import com.kinderman.sdo.presentation.sync.CharacterConflictDialog
import java.io.File
import org.junit.Rule
import org.junit.Test

class S24UltraFlowCaptureTest {
    @get:Rule
    val compose = createComposeRule()

    private val session = UserSession(
        uid = "gm-1",
        email = "mestre@sdo.rpg",
        displayName = "Historiador",
        role = UserRole.ADMIN,
    )

    private val campaign = Campaign(
        id = "camp-1",
        name = "Solidão dos Oprimidos",
        description = "Crônicas da Era Muda e dos Ecos do Véu.",
        ownerId = session.uid,
    )

    private val membership = CampaignMember(
        campaignId = campaign.id,
        userId = session.uid,
        role = CampaignRole.HISTORIAN,
    )

    private val character = Character(
        id = "char-1",
        ownerId = session.uid,
        campaignId = campaign.id,
        name = "Kaelen Vane",
        race = "Humano",
        occupation = "Artífice do Sol Esquecido",
        story = "Sobrevivente das oficinas de bronze de Essalhen, carrega um cronômetro que marca eclipses esquecidos.",
        life = ResourceValue(current = 18, maximum = 24),
        energy = ResourceValue(current = 11, maximum = 15),
        sanity = ResourceValue(current = 14, maximum = 16),
        arcane = ResourceValue(current = 6, maximum = 8),
        destiny = ResourceValue(current = 2, maximum = 3),
        money = 420,
        abilities = listOf(
            Ability(
                id = "ab-1",
                name = "Ímpeto Brutal",
                kind = AbilityKind.POWER,
                cost = AbilityCost.PowerCost(1, SpendableResource.PE),
                effect = "Após mover, cause dano adicional com arma corpo a corpo.",
            ),
            Ability(
                id = "ab-2",
                name = "Selo de Bronze",
                kind = AbilityKind.RUNE,
                cost = AbilityCost.PowerCost(2, SpendableResource.PE),
                effect = "Reforça a proteção local de uma peça de armadura até o fim da cena.",
            ),
        ),
        inventory = listOf(
            InventoryItem(
                id = "item-1",
                name = "Lâmina de Bronze Rúnico",
                category = "Arma",
                quantity = 1,
                load = 2,
                effect = "Forjada antes do grande silêncio.",
            ),
            InventoryItem(
                id = "item-2",
                name = "Frasco de Cinza Refinada",
                category = "Consumível",
                quantity = 3,
                load = 1,
                effect = "Dose catalisadora para rituais de campo.",
            ),
        ),
        conditions = listOf(
            ConditionEffect(
                id = "cond-1",
                name = "Sangramento",
                summary = "Perde 1 de Vida por turno até ser estancado.",
                duration = "Scene",
                intensity = "1",
                origin = "Combate",
            ),
        ),
        personalNotes = listOf(
            PersonalNote(
                id = "note-1",
                title = "Pista na Torre de Sal",
                text = "O símbolo gravado na porta coincide com o selo de Essalhen.",
            ),
        ),
    )

    private val conflict = CharacterSyncConflict(
        local = character.copy(life = ResourceValue(current = 15, maximum = 24)),
        remote = character.copy(life = ResourceValue(current = 18, maximum = 24)),
        remoteUpdatedAt = 1_763_500_000_000L,
        fields = listOf(
            CharacterConflictField(
                id = "life",
                label = "Vida",
                localSummary = "15 / 24",
                remoteSummary = "18 / 24",
            ),
        ),
    )

    private enum class FlowStep(val fileName: String) {
        DASHBOARD("01_painel"),
        SESSION("02_sessao"),
        HISTORIAN("03_mestre"),
        LIBRARY("04_biblioteca"),
        SETTINGS("05_ajustes"),
        SHEET("06_ficha_perfil_tracos"),
        PATH_INVENTORY("07_ficha_caminho_inventario"),
        CONDITIONS_NOTES("08_ficha_condicoes_notas_historia"),
        CONFLICT("09_modal_conflito"),
    }

    private val themeConfigs = listOf(
        "standard" to SdoPreferences(visualMode = SdoVisualMode.STANDARD, theme = SdoThemeVariant.CYAN_INDUSTRIAL),
        "cybergrunge" to SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE, theme = SdoThemeVariant.CYBERGRUNGE_SIGNAL),
        "cybergrunge_edgerunners" to SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE, theme = SdoThemeVariant.EDGERUNNERS),
    )

    @Test
    fun captureFullS24UltraFlowAcrossThemes() {
        var currentPrefs by mutableStateOf(themeConfigs.first().second)
        var currentStep by mutableStateOf(FlowStep.DASHBOARD)
        val catalog = BuiltInCatalog.entries.take(40)

        compose.setContent {
            SdoTheme(currentPrefs) {
                SdoResponsiveFrame {
                    val showFooter = currentStep in setOf(
                        FlowStep.DASHBOARD,
                        FlowStep.SESSION,
                        FlowStep.HISTORIAN,
                        FlowStep.LIBRARY,
                        FlowStep.SETTINGS,
                    )
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            when (currentStep) {
                                FlowStep.DASHBOARD -> DashboardScreen(
                                    characters = listOf(character),
                                    campaigns = listOf(campaign),
                                    memberships = listOf(membership),
                                    campaignInvites = emptyList(),
                                    campaignMembers = listOf(membership),
                                    owners = listOf(UserProfile(session.uid, session.email, session.displayName)),
                                    session = session,
                                    syncing = false,
                                    compactCards = false,
                                    collapseCampaignCards = false,
                                    showArchivedCampaigns = true,
                                    invitePreview = null,
                                    snackbarHost = {},
                                    onAdd = {},
                                    onAddToCampaign = { _, _ -> },
                                    onLinkCharacter = { _, _ -> },
                                    onUnlinkCharacter = {},
                                    onOpen = {},
                                    onOwnerTransfer = { _, _ -> },
                                    onCreateCampaign = { _, _ -> },
                                    onArchiveCampaign = { _, _ -> },
                                    onDeleteCampaign = {},
                                    onLeaveCampaign = {},
                                    onPreviewInvite = {},
                                    onAcceptInvite = { _, _, _ -> },
                                    onDismissInvitePreview = {},
                                    onSync = {},
                                    onOpenSession = {},
                                    onOpenHistorian = {},
                                    onOpenSettings = {},
                                    deliveries = emptyList(),
                                    onRespondDelivery = { _, _ -> },
                                    onLogout = {},
                                )

                                FlowStep.SESSION -> SessionModeScreen(
                                    characters = listOf(character),
                                    selectedId = character.id,
                                    compact = false,
                                    readOnly = false,
                                    onSelect = {},
                                    onOpenSheet = {},
                                    onCommand = { _, _ -> },
                                    onBack = {},
                                    onInventoryStateChange = { _, _, _ -> },
                                )

                                FlowStep.HISTORIAN -> HistorianDashboardScreen(
                                    session = session,
                                    campaigns = listOf(campaign),
                                    memberships = listOf(membership),
                                    characters = listOf(character),
                                    catalog = catalog,
                                    audit = emptyList(),
                                    library = emptyList(),
                                    deliveries = emptyList(),
                                    alertSettings = emptyList(),
                                    onApplyCommand = { _, _ -> },
                                    onSaveLibrary = {},
                                    onDuplicateLibrary = {},
                                    onArchiveLibrary = { _, _ -> },
                                    onDeliverLibrary = { _, _, _ -> },
                                    onSaveAlertSettings = {},
                                    onOpenSession = {},
                                    onOpenSheet = {},
                                    onBack = {},
                                )

                                FlowStep.LIBRARY -> LibraryScreen(
                                    campaigns = listOf(campaign),
                                    characters = listOf(character),
                                    catalog = catalog,
                                    campaignLibrary = emptyList(),
                                    audit = emptyList(),
                                    onBack = {},
                                )

                                FlowStep.SETTINGS -> SettingsScreen(
                                    preferences = currentPrefs,
                                    onPreferencesChange = {},
                                    onBack = {},
                                )

                                FlowStep.SHEET -> CharacterSheetScreen(
                                    character = character,
                                    session = session,
                                    catalog = catalog,
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

                                FlowStep.PATH_INVENTORY -> HudBackground {
                                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                        PathSection(character, catalog, true, {})
                                        PhaseOneStrictInventorySection(character, catalog, true, {})
                                    }
                                }

                                FlowStep.CONDITIONS_NOTES -> HudBackground {
                                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                        ConditionSection(character, true, {})
                                        NotesSection(character, true, {})
                                        NarrativeSection(character, true, {})
                                    }
                                }

                                FlowStep.CONFLICT -> HudBackground {
                                    CharacterConflictDialog(conflict = conflict, onResolve = {})
                                }
                            }
                        }
                        if (showFooter) {
                            SdoNavigationBar(
                                listOf(
                                    SdoNavigationItem("Painel", currentStep == FlowStep.DASHBOARD, Icons.Default.Home) {},
                                    SdoNavigationItem("Sessão", currentStep == FlowStep.SESSION, Icons.Default.PlayCircle) {},
                                    SdoNavigationItem("Mestre", currentStep == FlowStep.HISTORIAN, Icons.Default.Visibility) {},
                                    SdoNavigationItem("Biblioteca", currentStep == FlowStep.LIBRARY, Icons.AutoMirrored.Filled.MenuBook) {},
                                    SdoNavigationItem("Ajustes", currentStep == FlowStep.SETTINGS, Icons.Default.Settings) {},
                                ),
                            )
                        }
                    }
                }
            }
        }

        compose.mainClock.autoAdvance = false
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val baseDir = File(instrumentation.targetContext.getExternalFilesDir(null), "s24-flow")
        for ((themeDirName, prefs) in themeConfigs) {
            val outDir = File(baseDir, themeDirName)
            check(outDir.isDirectory || outDir.mkdirs())
            instrumentation.runOnMainSync { currentPrefs = prefs }
            for (step in FlowStep.entries) {
                instrumentation.runOnMainSync { currentStep = step }
                compose.mainClock.advanceTimeBy(350L)
                instrumentation.waitForIdleSync()
                Thread.sleep(250L)
                val capture = instrumentation.uiAutomation.executeShellCommand("screencap -p")
                ParcelFileDescriptor.AutoCloseInputStream(capture).use { input ->
                    File(outDir, "${step.fileName}.png").outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}
