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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserSession
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
    BODY("08—10", "CORPO"),
    MYSTIC("11", "MÍSTICO"),
    RECORD("12—13", "REGISTRO"),
    NOTES("14", "ANOTAÇÕES"),
}

@Composable
internal fun CharacterSheetPager(
    character: Character,
    session: UserSession,
    editable: Boolean,
    onChange: (Character) -> Unit,
    modifier: Modifier = Modifier,
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
    )
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Void,
            contentColor = Acid,
            edgePadding = 8.dp,
            divider = { HorizontalDivider(color = TechCutDark) },
        ) {
            pages.forEachIndexed { index, page ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(page.label, style = MaterialTheme.typography.labelLarge)
                            Text(page.code, color = if (pagerState.currentPage == index) Acid else Muted, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    selectedContentColor = Acid,
                    unselectedContentColor = Ice,
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
                editable = editable,
                onChange = onChange,
                scrollState = pageScrollStates[pageIndex],
            )
        }
    }
}

@Composable
private fun SheetPageContent(
    page: SheetPage,
    character: Character,
    session: UserSession,
    editable: Boolean,
    onChange: (Character) -> Unit,
    scrollState: LazyListState,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        state = scrollState,
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        when (page) {
            SheetPage.PROFILE -> {
                item("hero") { SheetHero(character, session) }
                item("identity") { IdentitySection(character, editable, onChange) }
                item("resources") { ResourceSection(character, editable, onChange) }
                item("traits") { TraitSection(character, editable, onChange) }
            }

            SheetPage.APTITUDES -> {
                item("attributes") { AttributeSection(character, editable, onChange) }
                item("knowledge") { SpecialKnowledgeSection(character, editable, onChange) }
                item("protection") { ProtectionSection(character, editable, onChange) }
            }

            SheetPage.PATH -> item("path") { PathSection(character, editable, onChange) }

            SheetPage.BODY -> {
                item("inventory") { InventorySection(character, editable, onChange) }
                item("body") { BodySection(character, editable, onChange) }
                item("organs") { OrganSection(character, editable, onChange) }
            }

            SheetPage.MYSTIC -> item("mystic") { MysticSection(character, editable, onChange) }

            SheetPage.RECORD -> {
                item("conditions") { ConditionSection(character, editable, onChange) }
                item("narrative") { NarrativeSection(character, editable, onChange) }
            }

            SheetPage.NOTES -> item("notes") { NotesSection(character, editable, onChange) }
        }
    }
}
