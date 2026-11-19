package com.kinderman.sdo.presentation.session

import androidx.activity.compose.BackHandler
import com.kinderman.sdo.ui.sdoClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import com.kinderman.sdo.ui.SdoIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import com.kinderman.sdo.ui.SdoCardTitle
import androidx.compose.material3.Text
import com.kinderman.sdo.ui.SdoTextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinderman.sdo.R
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AbilityCost
import com.kinderman.sdo.domain.model.AbilityKind
import com.kinderman.sdo.domain.model.BodyRegionSlot
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ConditionInstance
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SessionResource
import com.kinderman.sdo.domain.model.allCanonicalAbilitiesSafely
import com.kinderman.sdo.domain.model.canonicalBodyState
import com.kinderman.sdo.domain.model.canonicalConditions
import com.kinderman.sdo.domain.model.localProtectionBreakdown
import com.kinderman.sdo.domain.model.LoadCondition
import com.kinderman.sdo.domain.model.InventoryState
import com.kinderman.sdo.ui.SdoFilterChip
import kotlinx.coroutines.launch
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.LocalSdoPreferences
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.SdoScreenMasthead
import com.kinderman.sdo.presentation.character.CharacterActionButton
import com.kinderman.sdo.presentation.character.CharacterActionStyle
import com.kinderman.sdo.presentation.character.ChoiceField

private const val ALL_ABILITIES = "ALL"

private enum class SessionSection(val label: Int) {
    OVERVIEW(R.string.session_overview),
    RESOURCES(R.string.session_resources),
    ABILITIES(R.string.session_ability_section),
    PROTECTIONS(R.string.session_protections),
    BODY(R.string.session_body),
    EQUIPMENT(R.string.session_equipment),
    CONDITIONS(R.string.session_conditions),
}

private data class SessionAbilityEntry(val ability: Ability, val description: String, val key: String)
private data class SessionResourceValue(
    val label: String,
    val current: Int,
    val maximum: Int,
    val resource: SessionResource,
    val operationType: SessionOperationType = SessionOperationType.RESOURCE,
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SessionModeScreen(
    characters: List<Character>,
    selectedId: String?,
    compact: Boolean,
    readOnly: Boolean,
    onSelect: (String) -> Unit,
    onOpenSheet: (String) -> Unit,
    onCommand: (Character, SessionCommand) -> Unit,
    onBack: () -> Unit,
    canOpenJournal: (Character) -> Boolean = { false },
    onOpenJournal: (Character) -> Unit = {},
    onInventoryStateChange: (Character, String, InventoryState) -> Unit,
    onToggleAbilityFavorite: (Character, String) -> Unit = { _, _ -> },
) {
    BackHandler(onBack = onBack)
    val character = characters.firstOrNull { it.id == selectedId }
    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
        ) { padding ->
            if (character == null) {
                CharacterSelector(characters, Modifier.padding(padding), onSelect)
            } else {
                key(character.id) {
                    SessionContent(
                        character = character,
                        compact = compact,
                        readOnly = readOnly,
                        canOpenJournal = canOpenJournal(character),
                        onOpenJournal = { onOpenJournal(character) },
                        onOpenSheet = { onOpenSheet(character.id) },
                        modifier = Modifier.padding(padding),
                        onCommand = onCommand,
                        onInventoryStateChange = onInventoryStateChange,
                        onToggleAbilityFavorite = onToggleAbilityFavorite,
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterSelector(characters: List<Character>, modifier: Modifier, onSelect: (String) -> Unit) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SdoScreenMasthead(
                eyebrow = "SESSION // PLAY",
                title = "MODO SESSÃO",
                metadata = "ESCOLHA UMA FICHA PARA A SESSÃO",
            )
        }
        items(characters, key = Character::id) { character ->
            val maximum = character.lifeMaximum.coerceAtLeast(1)
            val lifeFraction = (character.life.current.toFloat() / maximum).coerceIn(0f, 1f)
            val lifeColor = when {
                lifeFraction <= .25f -> MaterialTheme.colorScheme.error
                lifeFraction <= .5f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.secondary
            }
            TechPanel(
                modifier = Modifier.fillMaxWidth().sdoClickable { onSelect(character.id) },
                accent = lifeColor,
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 72.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    com.kinderman.sdo.ui.SdoCharacterAvatar(
                        photoUri = character.photoUri,
                        name = character.name,
                        size = 48.dp,
                        accent = lifeColor,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(
                            character.name.ifBlank { "Personagem sem nome" },
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Box(Modifier.fillMaxWidth().height(5.dp).background(MaterialTheme.colorScheme.outlineVariant)) {
                            Box(Modifier.fillMaxWidth(lifeFraction).height(5.dp).background(lifeColor))
                        }
                        Text(
                            "Vida ${character.life.current}/$maximum",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    TelemetryTag(if (character.dirty) "PENDENTE" else "SALVO")
                }
            }
        }
        if (characters.isEmpty()) item { Text("Nenhuma ficha disponível.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SessionContent(
    character: Character,
    compact: Boolean,
    readOnly: Boolean,
    canOpenJournal: Boolean,
    onOpenJournal: () -> Unit,
    onOpenSheet: () -> Unit,
    modifier: Modifier,
    onCommand: (Character, SessionCommand) -> Unit,
    onInventoryStateChange: (Character, String, InventoryState) -> Unit,
    onToggleAbilityFavorite: (Character, String) -> Unit = { _, _ -> },
) {
    var damageDialog by rememberSaveable(character.id) { mutableStateOf(false) }
    var healingDialog by rememberSaveable(character.id) { mutableStateOf(false) }
    var pendingAbilityId by rememberSaveable(character.id) { mutableStateOf<String?>(null) }
    var abilityFilter by rememberSaveable(character.id) { mutableStateOf(ALL_ABILITIES) }
    var favoritesOnly by rememberSaveable(character.id) { mutableStateOf(false) }
    var availableOnly by rememberSaveable(character.id) { mutableStateOf(false) }
    var allResources by rememberSaveable(character.id) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val entries = remember(character.abilities, character.powers, character.mysticAbilities) {
        val occurrences = mutableMapOf<String, Int>()
        character.allCanonicalAbilitiesSafely()
            .sortedWith(compareByDescending<Ability> { it.favorite }.thenBy { it.name })
            .map { ability ->
                val occurrence = occurrences.getOrDefault(ability.id, 0)
                occurrences[ability.id] = occurrence + 1
                SessionAbilityEntry(ability, buildString {
                    if (ability.effect.isNotBlank()) append(ability.effect)
                    ability.mechanicalEffect?.operations.orEmpty().forEach { operation ->
                        if (isNotEmpty()) append('\n')
                        append("• ")
                        append(operation.sessionLabel())
                    }
                }, "ability:${ability.id}:$occurrence")
            }
    }
    val filteredEntries = remember(entries, abilityFilter, favoritesOnly, availableOnly) {
        entries.filter {
            (abilityFilter == ALL_ABILITIES || it.ability.kind.name == abilityFilter) &&
                (!favoritesOnly || it.ability.favorite) && (!availableOnly || it.ability.available)
        }
    }
    val sectionIndices = linkedMapOf(
        SessionSection.OVERVIEW to 0,
        SessionSection.RESOURCES to 2,
        SessionSection.ABILITIES to 3,
        SessionSection.PROTECTIONS to 5 + filteredEntries.size,
        SessionSection.BODY to 6 + filteredEntries.size,
        SessionSection.EQUIPMENT to 7 + filteredEntries.size,
        SessionSection.CONDITIONS to 8 + filteredEntries.size,
    )
    val sectionLabels = SessionSection.entries.associateWith { stringResource(it.label) }
    val currentSection = sectionIndices.entries.lastOrNull { it.value <= listState.firstVisibleItemIndex }?.key
        ?: SessionSection.OVERVIEW
    val allAbilitiesLabel = stringResource(R.string.session_all_abilities)
    val spacing = if (compact) 8.dp else 14.dp
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChoiceField(
                label = stringResource(R.string.session_jump_to),
                value = currentSection,
                options = SessionSection.entries,
                enabled = true,
                modifier = Modifier.weight(1f).testTag("session_section_jump"),
                display = { sectionLabels.getValue(it) },
            ) { section -> scope.launch { listState.scrollToItem(sectionIndices.getValue(section)) } }
            if (canOpenJournal) {
                SdoIconButton(onOpenJournal) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, "Abrir diário da campanha", tint = MaterialTheme.colorScheme.secondary)
                }
            }
            SdoIconButton(onOpenSheet) {
                Icon(Icons.Default.EditNote, "Abrir ficha completa")
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag("session_list"),
            state = listState,
            contentPadding = PaddingValues(if (compact) 12.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            item(key = "session_overview") {
                SdoScreenMasthead(
                    eyebrow = stringResource(R.string.session_eyebrow),
                    title = stringResource(R.string.session_title),
                    metadata = stringResource(
                        when {
                            readOnly -> R.string.session_read_only
                            character.dirty -> R.string.session_pending_sync
                            else -> R.string.session_synced
                        },
                    ),
                )
            }
            item(key = "session_actions") {
                SdoInsetCard(
                    accent = if (character.dirty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                    contentPadding = 12.dp,
                    verticalSpacing = 10.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        com.kinderman.sdo.ui.SdoCharacterAvatar(
                            photoUri = character.photoUri,
                            name = character.name,
                            size = 44.dp,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                character.name.ifBlank { "Personagem sem nome" }.uppercase(),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            SdoMetadataText(
                                text = listOf(character.race, "NV.${character.level}").filter(String::isNotBlank).joinToString(" // "),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    SdoMetadataText(
                        text = stringResource(
                            R.string.session_resources_summary,
                            character.life.current,
                            character.lifeMaximum,
                            character.energy.current,
                            character.energyMaximum,
                            character.arcane.current,
                            character.arcaneMaximum,
                            character.currentLoad,
                            character.maximumLoad,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    when (character.loadCondition) {
                        LoadCondition.OVERLOADED -> {
                            Text(stringResource(R.string.session_overloaded), color = MaterialTheme.colorScheme.error)
                            SdoMetadataText(stringResource(R.string.session_overload_details), style = MaterialTheme.typography.labelSmall)
                        }
                        LoadCondition.IMMOBILE -> {
                            Text(stringResource(R.string.session_immobile), color = MaterialTheme.colorScheme.error)
                            Text(stringResource(R.string.session_immobile_details), color = MaterialTheme.colorScheme.error)
                        }
                        else -> Unit
                    }
                    Text(stringResource(R.string.session_quick_actions), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CharacterActionButton(stringResource(R.string.session_damage), !readOnly, CharacterActionStyle.PRIMARY, Modifier.weight(1f).testTag("session_damage")) { damageDialog = true }
                        CharacterActionButton(stringResource(R.string.session_heal), !readOnly, CharacterActionStyle.SECONDARY, Modifier.weight(1f).testTag("session_heal")) { healingDialog = true }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CharacterActionButton(stringResource(R.string.session_quick_items), true, CharacterActionStyle.SECONDARY, Modifier.weight(1f).testTag("session_quick_items")) {
                            scope.launch { listState.scrollToItem(sectionIndices.getValue(SessionSection.EQUIPMENT)) }
                        }
                        CharacterActionButton(stringResource(R.string.session_quick_conditions), true, CharacterActionStyle.SECONDARY, Modifier.weight(1f).testTag("session_quick_conditions")) {
                            scope.launch { listState.scrollToItem(sectionIndices.getValue(SessionSection.CONDITIONS)) }
                        }
                    }
                    if (readOnly) Text(stringResource(R.string.session_controls_locked), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
            item(key = "session_resources") {
                TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                    SectionHeader("01", stringResource(R.string.session_resources))
                    val resources = listOf(
                        SessionResourceValue(stringResource(R.string.session_life), character.life.current, character.lifeMaximum, SessionResource.LIFE),
                        SessionResourceValue(stringResource(R.string.session_energy), character.energy.current, character.energyMaximum, SessionResource.ENERGY),
                        SessionResourceValue(stringResource(R.string.session_sanity), character.sanity.current, character.sanityMaximum, SessionResource.SANITY),
                        SessionResourceValue(stringResource(R.string.session_arcane), character.arcane.current, character.arcaneMaximum, SessionResource.ARCANE),
                        SessionResourceValue(stringResource(R.string.session_destiny), character.destiny.current, character.destinyMaximum, SessionResource.DESTINY, SessionOperationType.DESTINY),
                        SessionResourceValue(stringResource(R.string.session_exhaustion), character.exhaustion.current, character.exhaustion.maximum, SessionResource.EXHAUSTION),
                        SessionResourceValue(stringResource(R.string.session_corruption), character.corruption.current, character.corruption.maximum, SessionResource.CORRUPTION),
                    )
                    val columns = if (LocalDensity.current.fontScale > 1.2f) 1 else 2
                    (if (allResources) resources else resources.take(2)).chunked(columns).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { resource ->
                                ResourceControl(resource.label, resource.current, resource.maximum, !readOnly, compact, Modifier.weight(1f)) { next ->
                                    onCommand(character, SessionCommand(type = resource.operationType, resource = resource.resource, amount = next - resource.current))
                                }
                            }
                            if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        }
                    }
                    SdoTextButton(
                        onClick = { allResources = !allResources },
                        modifier = Modifier.fillMaxWidth().testTag("session_toggle_resources"),
                    ) { Text(stringResource(if (allResources) R.string.session_fewer_resources else R.string.session_all_resources)) }
                    if (allResources) MoneyControl(character.money, !readOnly, compact) { next ->
                        onCommand(
                            character,
                            SessionCommand(
                                type = SessionOperationType.MONEY,
                                amount = next - character.money,
                            ),
                        )
                    }
                }
            }
            item(key = "session_ability_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceField(
                        label = stringResource(R.string.session_ability_filter),
                        value = abilityFilter,
                        options = listOf(ALL_ABILITIES) + AbilityKind.entries.map { it.name },
                        enabled = true,
                        modifier = Modifier.fillMaxWidth().testTag("session_ability_filter"),
                        display = { value -> AbilityKind.entries.firstOrNull { it.name == value }?.label ?: allAbilitiesLabel },
                    ) { abilityFilter = it }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SdoFilterChip(
                            label = stringResource(R.string.session_favorites),
                            selected = favoritesOnly,
                            onClick = { favoritesOnly = !favoritesOnly },
                            modifier = Modifier.weight(1f).testTag("session_favorites"),
                        )
                        SdoFilterChip(
                            label = stringResource(R.string.session_available),
                            selected = availableOnly,
                            onClick = { availableOnly = !availableOnly },
                            modifier = Modifier.weight(1f).testTag("session_available"),
                        )
                    }
                    if (abilityFilter != ALL_ABILITIES || favoritesOnly || availableOnly) {
                        SdoTextButton(
                            onClick = { abilityFilter = ALL_ABILITIES; favoritesOnly = false; availableOnly = false },
                            modifier = Modifier.testTag("session_clear_filters"),
                        ) { Text(stringResource(R.string.session_clear_filters)) }
                    }
                    SdoMetadataText(
                        stringResource(R.string.session_ability_count, filteredEntries.size, entries.size),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            item(key = "session_ability_header") {
                SectionHeader("02", stringResource(R.string.session_ability_section))
                if (filteredEntries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.session_no_abilities),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            items(filteredEntries, key = { "${character.id}:${it.key}" }) { entry ->
                SessionAbilityCard(
                    entry = entry,
                    compact = compact,
                    editable = !readOnly,
                    onToggleFavorite = { onToggleAbilityFavorite(character, entry.ability.id) },
                    onUse = { pendingAbilityId = entry.ability.id },
                )
            }
            item(key = "session_protections") {
                TechPanel {
                    SectionHeader("03", stringResource(R.string.session_protections))
                    character.calculatedProtections().forEach { (name, value) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value.toString(), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            item(key = "session_body") {
                TechPanel(accent = MaterialTheme.colorScheme.error) {
                    SectionHeader("04", stringResource(R.string.session_body))
                    com.kinderman.sdo.ui.SdoResponsiveGrid(character.canonicalBodyState().regions, minItemWidth = 148.dp) { region, modifier ->
                        SdoInsetCard(
                            modifier = modifier, contentPadding = if (compact) 9.dp else 12.dp,
                            verticalSpacing = 6.dp,
                        ) {
                            SdoCardTitle(region.name.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                            SessionBodyStatus(region.state.label.uppercase(), region.failures)
                            Text("PL ${character.localProtection(region)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item(key = "session_equipment") {
                TechPanel {
                    SectionHeader("05", stringResource(R.string.session_equipment))
                    SessionInventoryActions(character, readOnly, onCommand, onInventoryStateChange)
                }
            }
            item(key = "session_conditions") {
                val conditions = character.canonicalConditions()
                TechPanel(accent = if (conditions.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) {
                    SectionHeader("06", "Condições // ${conditions.size}")
                    if (conditions.isEmpty()) Text("Nenhuma condição ativa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SessionAddCondition(readOnly) { payload ->
                        onCommand(character, SessionCommand(type = SessionOperationType.CONDITION_ADD, condition = payload))
                    }
                    conditions.forEach { condition ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            SdoMetadataText(condition.sessionLabel(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            SdoTextButton(
                                enabled = !readOnly,
                                onClick = {
                                    onCommand(
                                        character,
                                        SessionCommand(
                                            type = SessionOperationType.CONDITION_REMOVE,
                                            conditionId = condition.instanceId,
                                        ),
                                    )
                                },
                            ) { Text("REMOVER") }
                        }
                    }
                }
            }
        }
    }

    if (damageDialog) DamageDialog(character, enabled = !readOnly, onDismiss = { damageDialog = false }) { amount, region ->
        damageDialog = false
        if (!readOnly) onCommand(character, SessionCommand(type = SessionOperationType.DAMAGE, amount = amount, bodyRegion = region))
    }
    if (healingDialog) AmountDialog(stringResource(R.string.session_heal), character.life.current, character.lifeMaximum, !readOnly, { healingDialog = false }) { amount ->
        healingDialog = false
        if (!readOnly) onCommand(character, SessionCommand(type = SessionOperationType.HEAL, resource = SessionResource.LIFE, amount = amount))
    }
    pendingAbilityId?.let { id ->
        val ability = entries.firstOrNull { it.ability.id == id }?.ability
        if (ability != null) {
            AlertDialog(
                onDismissRequest = { pendingAbilityId = null },
                title = { Text("CONFIRMAR USO") },
                text = {
                    SdoMetadataText(
                        "${ability.name} // ${abilityCostLabel(ability.cost)}. O custo será descontado antes da ação e o recurso nunca ficará negativo.",
                    )
                },
                confirmButton = {
                    SdoTextButton(
                        onClick = {
                            pendingAbilityId = null
                            onCommand(character, SessionCommand(type = SessionOperationType.ABILITY_USE, targetId = id))
                        },
                        modifier = Modifier.testTag("session_confirm_ability"),
                        enabled = !readOnly && ability.available,
                    ) { Text("USAR") }
                },
                dismissButton = { SdoTextButton({ pendingAbilityId = null }) { Text("CANCELAR") } },
            )
        }
    }
}

@Composable
private fun SessionAbilityCard(
    entry: SessionAbilityEntry,
    compact: Boolean,
    editable: Boolean,
    onToggleFavorite: () -> Unit,
    onUse: () -> Unit,
) {
    var expanded by rememberSaveable(entry.ability.id) { mutableStateOf(false) }
    val accent = when {
        entry.ability.favorite -> MaterialTheme.colorScheme.tertiary
        entry.ability.available -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val expandedLabel = stringResource(if (expanded) R.string.session_collapse_ability else R.string.session_expand_ability)
    val unavailableLabel = stringResource(R.string.session_unavailable)
    val largeText = LocalDensity.current.fontScale * LocalSdoPreferences.current.fontScale.multiplier > 1.15f
    SdoInsetCard(
        modifier = Modifier.fillMaxWidth(),
        accent = accent,
        contentPadding = if (compact) 11.dp else 14.dp,
        verticalSpacing = 9.dp,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SdoIconButton(
                onClick = onToggleFavorite,
                enabled = editable,
                modifier = Modifier.testTag("session_ability_favorite"),
            ) {
                Icon(
                    imageVector = if (entry.ability.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (entry.ability.favorite) "Desfavoritar ${entry.ability.name}" else "Favoritar ${entry.ability.name}",
                    tint = if (entry.ability.favorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .testTag("session_ability_expand")
                    .semantics { stateDescription = expandedLabel }
                    .sdoClickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.ability.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (!largeText) SessionAbilityMetadata(entry)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, expandedLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!expanded) {
                CharacterActionButton(
                    label = stringResource(if (entry.ability.available) R.string.session_use_short else R.string.session_unavailable_mark),
                    enabled = editable && entry.ability.available,
                    style = CharacterActionStyle.PRIMARY,
                    modifier = Modifier.widthIn(min = 64.dp, max = 96.dp).testTag("session_ability_use")
                        .then(if (entry.ability.available) Modifier else Modifier.semantics { contentDescription = unavailableLabel }),
                    onClick = onUse,
                )
            }
        }
        if (largeText) SessionAbilityMetadata(entry)
        if (expanded) {
            val description = if (entry.description.isBlank()) stringResource(R.string.session_no_ability_details) else entry.description
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            CharacterActionButton(
                label = stringResource(if (entry.ability.available) R.string.session_use_ability else R.string.session_unavailable),
                enabled = editable && entry.ability.available,
                style = CharacterActionStyle.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
                onClick = onUse,
            )
        }
    }
}

@Composable
private fun SessionAbilityMetadata(entry: SessionAbilityEntry) {
    SdoMetadataText(
        "${entry.ability.kind.label.uppercase()} // ${abilityCostLabel(entry.ability.cost)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SessionBodyStatus(label: String, failures: Int) {
    val statusColor = if (failures > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val delimiterColor = MaterialTheme.colorScheme.onSurfaceVariant
    val countColor = MaterialTheme.colorScheme.secondary
    BasicText(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = statusColor)) { append(label) }
            withStyle(SpanStyle(color = delimiterColor)) { append(" // ") }
            withStyle(SpanStyle(color = countColor)) { append("$failures/4") }
        },
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = MaterialTheme.typography.labelSmall.fontSize),
    )
}

@Composable
private fun ResourceControl(
    label: String,
    current: Int,
    maximum: Int,
    enabled: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, CutCornerShape(topEnd = 8.dp, bottomStart = 6.dp))
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 4.dp else 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text("$current/$maximum", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SdoIconButton({ onChange(current - 1) }, enabled = enabled && current > 0, modifier = Modifier.weight(1f).heightIn(min = 36.dp)) {
                Icon(Icons.Default.Remove, stringResource(R.string.session_decrease_resource, label))
            }
            SdoIconButton({ onChange(current + 1) }, enabled = enabled && current < maximum, modifier = Modifier.weight(1f).heightIn(min = 36.dp)) {
                Icon(Icons.Default.Add, stringResource(R.string.session_increase_resource, label))
            }
        }
    }
}

@Composable
private fun MoneyControl(
    money: Int,
    enabled: Boolean,
    compact: Boolean,
    onChange: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, CutCornerShape(topEnd = 8.dp, bottomStart = 6.dp))
            .padding(if (compact) 5.dp else 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("DINHEIRO", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SdoIconButton(
                onClick = { onChange((money - 1).coerceAtLeast(0)) },
                enabled = enabled && money > 0,
                modifier = Modifier.weight(1f),
            ) { Icon(Icons.Default.Remove, "Reduzir dinheiro") }
            Text(money.toString(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            SdoIconButton(
                onClick = { onChange(money + 1) },
                enabled = enabled && money < Int.MAX_VALUE,
                modifier = Modifier.weight(1f),
            ) { Icon(Icons.Default.Add, "Aumentar dinheiro") }
        }
    }
}

@Composable
private fun DamageDialog(character: Character, enabled: Boolean, onDismiss: () -> Unit, onConfirm: (Int, BodyRegionSlot) -> Unit) {
    var amount by rememberSaveable(character.id) { mutableIntStateOf(1) }
    var regionIndex by rememberSaveable(character.id) { mutableIntStateOf(0) }
    val regions = character.canonicalBodyState().regions
    val region = regions.getOrElse(regionIndex) { character.canonicalBodyState().region(BodyRegionSlot.Torso) }
    val protection = character.localProtection(region)
    val applied = (amount - protection).coerceAtLeast(0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PRÉVIA DE DANO") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Informe o dano")
                Stepper(amount, 0, 999, Modifier.testTag("session_damage_amount")) { amount = it }
                Text("2. Escolha a região")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SdoTextButton({ regionIndex = (regionIndex - 1).floorMod(regions.size) }) { Text("‹") }
                    Text(region.name, modifier = Modifier.weight(1f).align(Alignment.CenterVertically))
                    SdoTextButton({ regionIndex = (regionIndex + 1).floorMod(regions.size) }) { Text("›") }
                }
                Text("3. P.L. local: $protection")
                Text("4. Resultado: $amount − $protection = $applied de Vida", color = if (applied > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text("Nada é alterado antes da confirmação.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { SdoTextButton({ if (enabled) onConfirm(amount, region.region) }, enabled = enabled) { Text("CONFIRMAR") } },
        dismissButton = { SdoTextButton(onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun AmountDialog(title: String, current: Int, maximum: Int, enabled: Boolean, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var amount by rememberSaveable { mutableIntStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Stepper(amount, 0, 999) { amount = it }
                Text("Vida: $current → ${(current + amount).coerceAtMost(maximum)} / $maximum")
            }
        },
        confirmButton = { SdoTextButton({ if (enabled) onConfirm(amount) }, enabled = enabled) { Text("CONFIRMAR") } },
        dismissButton = { SdoTextButton(onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun Stepper(value: Int, minimum: Int, maximum: Int, valueModifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SdoIconButton({ onChange((value - 1).coerceAtLeast(minimum)) }) { Icon(Icons.Default.Remove, "Reduzir") }
        Text(value.toString(), modifier = valueModifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleLarge)
        SdoIconButton({ onChange((value + 1).coerceAtMost(maximum)) }) { Icon(Icons.Default.Add, "Aumentar") }
    }
}

private fun Int.floorMod(divisor: Int): Int = if (divisor <= 0) 0 else Math.floorMod(this, divisor)

@Composable
private fun abilityCostLabel(cost: AbilityCost): String = when (cost) {
    is AbilityCost.PowerCost -> if (cost.amount == 0) "SEM CUSTO" else "${cost.amount} ${cost.resource}"
    is AbilityCost.SpellCost -> "${cost.amount} PM"
    is AbilityCost.RuneCost -> "${cost.amount} PM"
    is AbilityCost.AshCost -> pluralStringResource(R.plurals.session_ash_cost, cost.amount, cost.amount)
}

private fun com.kinderman.sdo.domain.model.EffectOperation.sessionLabel(): String = when (this) {
    is com.kinderman.sdo.domain.model.EffectOperation.Damage -> "Dano ${damageType.label} em ${target.kind}"
    is com.kinderman.sdo.domain.model.EffectOperation.Healing -> "Recupera ${amount} de ${resource}"
    is com.kinderman.sdo.domain.model.EffectOperation.ApplyCondition -> "Aplica ${condition.kind.label}"
    is com.kinderman.sdo.domain.model.EffectOperation.AddModifier -> "Modificador ${modifier.target.kind} ${if (modifier.amount > 0) "+" else ""}${modifier.amount}"
    is com.kinderman.sdo.domain.model.EffectOperation.SpendResource -> "Gasta ${amount} ${resource}"
    is com.kinderman.sdo.domain.model.EffectOperation.ConsumeItemState -> "Consome ${amount} ${state}"
}

private fun ConditionInstance.sessionLabel(): String = buildString {
    append(name)
    intensity?.let { append(" // intensidade $it") }
    duration?.let { append(" // ${it.kind.label}"); it.turns?.let { turns -> append(" ($turns turno(s))") } }
}
