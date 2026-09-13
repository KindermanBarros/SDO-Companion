package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.abilityDuplicates
import com.kinderman.sdo.domain.model.resolveAbilityDuplicate
import com.kinderman.sdo.domain.model.AbilityKind
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.TechCutDark
import com.kinderman.sdo.ui.Void
import kotlinx.coroutines.launch

internal enum class SheetPage(val code: String, val label: String) {
    PROFILE("01—03", "PERFIL"),
    APTITUDES("04—06", "APTIDÕES"),
    PATH("07", "CAMINHO"),
    POWERS("08", "PODERES"),
    MYSTIC("09", "MÍSTICO"),
    INVENTORY("10", "INVENTÁRIO"),
    BODY("11—12", "CORPO"),
    RECORD("13—14", "REGISTRO"),
    NOTES("15", "ANOTAÇÕES"),
}

@Composable
internal fun CharacterSheetPager(
    character: Character,
    session: UserSession,
    catalog: List<CatalogEntry>,
    editable: Boolean,
    showCalculationAudit: Boolean,
    onChange: (Character) -> Unit,
    modifier: Modifier = Modifier,
    saveError: String? = null,
) {
    val pages = SheetPage.entries
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val pageScrollStates = listOf(
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
    )
    val scope = rememberCoroutineScope()
    var pageMenu by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Box {
            androidx.compose.material3.TextButton(onClick = { pageMenu = true }) {
                Text("Ir para: ${pages[pagerState.currentPage].label}", style = MaterialTheme.typography.labelLarge)
            }
            androidx.compose.material3.DropdownMenu(expanded = pageMenu, onDismissRequest = { pageMenu = false }) {
                pages.forEachIndexed { index, page ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(page.label) },
                        onClick = { pageMenu = false; scope.launch { pagerState.animateScrollToPage(index) } },
                    )
                }
            }
        }
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 8.dp,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
        ) {
            pages.forEachIndexed { index, page ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(page.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            key = { pages[it].name },
            beyondViewportPageCount = 0,
        ) { pageIndex ->
            SheetPageContent(
                page = pages[pageIndex],
                character = character,
                session = session,
                catalog = catalog,
                editable = editable,
                showCalculationAudit = showCalculationAudit,
                onChange = onChange,
                scrollState = pageScrollStates[pageIndex],
                saveError = saveError,
            )
        }
    }
    character.abilityDuplicates().firstOrNull()?.let { duplicate ->
        var keepId by remember(duplicate.key) { mutableStateOf(duplicate.entries.first().first) }
        AlertDialog(
            onDismissRequest = {},
            title = { Text("DUPLICATA // ${duplicate.type.uppercase()}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Escolha qual registro manter. Cada duplicata precisa ser resolvida separadamente.")
                    duplicate.entries.forEachIndexed { index, (id, name) ->
                        TextButton(onClick = { keepId = id }) {
                            Text("${if (keepId == id) "[ MANTER ]" else "MANTER"} ${index + 1}. $name")
                        }
                    }
                    Text("Ao confirmar, os outros registros e estoques vinculados serão apagados definitivamente. Essa exclusão não pode ser desfeita.", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = { onChange(character.resolveAbilityDuplicate(duplicate, keepId)) }) { Text("CONFIRMAR EXCLUSÃO") }
            },
            dismissButton = {},
        )
    }
}

@Composable
private fun SheetPageContent(
    page: SheetPage,
    character: Character,
    session: UserSession,
    catalog: List<CatalogEntry>,
    editable: Boolean,
    showCalculationAudit: Boolean,
    onChange: (Character) -> Unit,
    scrollState: LazyListState,
    saveError: String?,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        state = scrollState,
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        when (page) {
            SheetPage.PROFILE -> {
                item("hero") { SheetHero(character, session, saveError) }
                item("identity") { com.kinderman.sdo.ui.CollapsibleSection("Identidade") { IdentitySection(character, catalog, editable, onChange) } }
                item("resources") { com.kinderman.sdo.ui.CollapsibleSection("Recursos") { ResourceSection(character, editable, onChange) } }
                item("traits") { com.kinderman.sdo.ui.CollapsibleSection("Traços") { TraitSection(character, editable, onChange) } }
            }

            SheetPage.APTITUDES -> {
                item("attributes") { com.kinderman.sdo.ui.CollapsibleSection("Atributos") { AttributeSection(character, editable, onChange) } }
                item("knowledge") { com.kinderman.sdo.ui.CollapsibleSection("Conhecimentos") { PhaseOneKnowledgeSection(character, catalog, editable, onChange) } }
                item("protection") { com.kinderman.sdo.ui.CollapsibleSection("Proteções") { ProtectionSection(character, editable, onChange) } }
                if (showCalculationAudit) item("calculation-audit") { com.kinderman.sdo.ui.CollapsibleSection("Auditoria de valores") { CalculatedValuesAuditSection(character) } }
            }

            SheetPage.PATH -> item("path") {
                com.kinderman.sdo.ui.CollapsibleSection("Caminho") { PhaseOnePathSection(character, catalog.filter { it.kind == CatalogKind.PATH }, editable, onChange) }
            }

            SheetPage.POWERS -> item("powers") {
                com.kinderman.sdo.ui.CollapsibleSection("Poderes") {
                    CanonicalAbilitySection(character, catalog.filter { it.kind == CatalogKind.POWER }, setOf(AbilityKind.POWER), editable, onChange)
                }
            }

            SheetPage.MYSTIC -> item("mystic") {
                com.kinderman.sdo.ui.CollapsibleSection("Habilidades — Místicas") {
                    CanonicalAbilitySection(
                        character,
                        catalog.filter { it.kind == CatalogKind.MAGIC || it.kind == CatalogKind.ASH || it.kind == CatalogKind.RUNE },
                        setOf(AbilityKind.SPELL, AbilityKind.RUNE, AbilityKind.ASH),
                        editable,
                        onChange,
                    )
                }
            }

            SheetPage.INVENTORY -> item("inventory") {
                com.kinderman.sdo.ui.CollapsibleSection("Inventário") { PhaseOneInventoryWithBonusSection(character, catalog.filter { it.kind == CatalogKind.ITEM || it.kind == CatalogKind.ASH }, editable, onChange) }
            }

            SheetPage.BODY -> {
                item("body") { BodySection(character, editable, onChange) }
                item("organs") { com.kinderman.sdo.ui.CollapsibleSection("Órgãos") { OrganSection(character, editable, onChange) } }
            }

            SheetPage.RECORD -> {
                item("conditions") { com.kinderman.sdo.ui.CollapsibleSection("Condições") { ConditionSection(character, editable, onChange) } }
                item("narrative") { com.kinderman.sdo.ui.CollapsibleSection("História") { NarrativeSection(character, editable, onChange) } }
            }

            SheetPage.NOTES -> item("notes") { com.kinderman.sdo.ui.CollapsibleSection("Anotações") { NotesSection(character, editable, onChange) } }
        }
    }
}
