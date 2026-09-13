package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
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
    RECORD("13", "ESTADO"),
    NOTES("14", "ANOTAÇÕES"),
    HISTORY("15", "HISTÓRIA"),
}

@Composable
internal fun CharacterSheetPager(
    character: Character,
    session: UserSession,
    catalog: List<CatalogEntry>,
    editable: Boolean,
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
        rememberLazyListState(),
    )
    val scope = rememberCoroutineScope()
    var pageMenu by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        SheetPageNavigator(
            pages = pages,
            currentPage = pagerState.currentPage,
            menuExpanded = pageMenu,
            onMenuExpandedChange = { pageMenu = it },
            onNavigate = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
        )
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
private fun SheetPageNavigator(
    pages: List<SheetPage>,
    currentPage: Int,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onNavigate: (Int) -> Unit,
) {
    val page = pages[currentPage]
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .72f),
                shape = CutCornerShape(topEnd = 16.dp, bottomStart = 12.dp),
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = CutCornerShape(topEnd = 16.dp, bottomStart = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onNavigate(currentPage - 1) },
                enabled = currentPage > 0,
            ) {
                Icon(Icons.Default.ChevronLeft, "Página anterior")
            }
            Box(
                Modifier
                    .weight(1f)
                    .clickable { onMenuExpandedChange(true) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp),
                    ) {
                        Text(page.code, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(page.label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "SEÇÃO ${currentPage + 1} DE ${pages.size}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Icon(Icons.Default.ExpandMore, "Escolher seção", tint = MaterialTheme.colorScheme.primary)
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { onMenuExpandedChange(false) },
                ) {
                    pages.forEachIndexed { index, option ->
                        androidx.compose.material3.DropdownMenuItem(
                            leadingIcon = { Text(option.code, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) },
                            text = { Text(option.label) },
                            onClick = {
                                onMenuExpandedChange(false)
                                onNavigate(index)
                            },
                        )
                    }
                }
            }
            IconButton(
                onClick = { onNavigate(currentPage + 1) },
                enabled = currentPage < pages.lastIndex,
            ) {
                Icon(Icons.Default.ChevronRight, "Próxima página")
            }
        }
    }
}

@Composable
private fun SheetPageContent(
    page: SheetPage,
    character: Character,
    session: UserSession,
    catalog: List<CatalogEntry>,
    editable: Boolean,
    onChange: (Character) -> Unit,
    scrollState: LazyListState,
    saveError: String?,
) {
    val compactCards = com.kinderman.sdo.ui.LocalSdoPreferences.current.compactCards
    LazyColumn(
        Modifier.fillMaxSize(),
        state = scrollState,
        contentPadding = PaddingValues(if (compactCards) 9.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (compactCards) 8.dp else 13.dp),
    ) {
        when (page) {
            SheetPage.PROFILE -> {
                item("hero") { SheetHero(character, session, saveError) }
                item("identity") { com.kinderman.sdo.ui.CollapsibleSection("Identidade", "01") { IdentitySection(character, catalog, editable, onChange) } }
                item("resources") { com.kinderman.sdo.ui.CollapsibleSection("Recursos", "02") { ResourceSection(character, editable, onChange) } }
                item("traits") { com.kinderman.sdo.ui.CollapsibleSection("Traços", "03") { TraitSection(character, editable, onChange) } }
            }

            SheetPage.APTITUDES -> {
                item("attributes") { com.kinderman.sdo.ui.CollapsibleSection("Atributos e conhecimentos", "04") { AttributeSection(character, editable, onChange) } }
                item("knowledge") { com.kinderman.sdo.ui.CollapsibleSection("Conhecimentos especiais", "05") { PhaseOneKnowledgeSection(character, catalog, editable, onChange) } }
                item("protection") { com.kinderman.sdo.ui.CollapsibleSection("Proteções", "06") { ProtectionSection(character, editable, onChange) } }
            }

            SheetPage.PATH -> item("path") {
                com.kinderman.sdo.ui.CollapsibleSection("Caminho", "07") { PhaseOnePathSection(character, catalog.filter { it.kind == CatalogKind.PATH }, editable, onChange) }
            }

            SheetPage.POWERS -> item("powers") {
                com.kinderman.sdo.ui.CollapsibleSection("Poderes", "08") {
                    CanonicalAbilitySection(character, catalog.filter { it.kind == CatalogKind.POWER }, setOf(AbilityKind.POWER), editable, onChange)
                }
            }

            SheetPage.MYSTIC -> item("mystic") {
                com.kinderman.sdo.ui.CollapsibleSection("Magias // Runas // Cinzas", "09") {
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
                com.kinderman.sdo.ui.CollapsibleSection("Inventário", "10") { PhaseOneInventoryWithBonusSection(character, catalog.filter { it.kind == CatalogKind.ITEM || it.kind == CatalogKind.ASH }, editable, onChange) }
            }

            SheetPage.BODY -> {
                item("body") { BodySection(character, editable, onChange) }
                item("organs") { com.kinderman.sdo.ui.CollapsibleSection("Órgãos", "12") { OrganSection(character, editable, onChange) } }
            }

            SheetPage.RECORD -> {
                item("conditions") { com.kinderman.sdo.ui.CollapsibleSection("Condições", "13") { ConditionSection(character, editable, onChange) } }
            }

            SheetPage.NOTES -> item("notes") { com.kinderman.sdo.ui.CollapsibleSection("Registros pessoais", "14") { NotesSection(character, editable, onChange) } }

            SheetPage.HISTORY -> item("narrative") {
                com.kinderman.sdo.ui.CollapsibleSection("História", "15") { NarrativeSection(character, editable, onChange) }
            }
        }
    }
}
