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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.kinderman.sdo.domain.model.inventoryState
import com.kinderman.sdo.domain.model.effectiveLoad
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
) {
    BackHandler(onBack = onBack)
    val character = characters.firstOrNull { it.id == selectedId }
    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(if (character == null) "MODO SESSÃO" else character.name.uppercase()) },
                    navigationIcon = { SdoIconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                    actions = {
                        if (character != null) SdoIconButton({ onOpenSheet(character.id) }) {
                            Icon(Icons.Default.EditNote, "Abrir ficha completa")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
        ) { padding ->
            if (character == null) {
                CharacterSelector(characters, Modifier.padding(padding), onSelect)
            } else {
                SessionContent(character, compact, readOnly, Modifier.padding(padding), onCommand)
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
            TechPanel {
                Text("Escolha uma ficha para a sessão", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            }
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
                    Text(
                        character.name.ifBlank { "?" }.take(2).uppercase(),
                        color = lifeColor,
                        style = MaterialTheme.typography.headlineSmall,
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
    modifier: Modifier,
    onCommand: (Character, SessionCommand) -> Unit,
) {
    var damageDialog by rememberSaveable(character.id) { mutableStateOf(false) }
    var healingDialog by rememberSaveable(character.id) { mutableStateOf(false) }
    var pendingAbilityId by rememberSaveable(character.id) { mutableStateOf<String?>(null) }
    var abilityFilter by rememberSaveable(character.id) { mutableStateOf(ALL_ABILITIES) }
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
    val filteredEntries = remember(entries, abilityFilter) {
        entries.filter { abilityFilter == ALL_ABILITIES || it.ability.kind.name == abilityFilter }
    }
    val allAbilitiesLabel = stringResource(R.string.session_all_abilities)
    val spacing = if (compact) 8.dp else 14.dp
    LazyColumn(
        modifier.fillMaxSize().testTag("session_list"),
        contentPadding = PaddingValues(if (compact) 12.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        item {
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
        item {
            SdoInsetCard(
                accent = if (character.dirty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                contentPadding = 8.dp,
                verticalSpacing = 6.dp,
            ) {
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CharacterActionButton("Aplicar dano", !readOnly, CharacterActionStyle.PRIMARY, Modifier.weight(1f)) { damageDialog = true }
                    CharacterActionButton("Aplicar cura", !readOnly, CharacterActionStyle.SECONDARY, Modifier.weight(1f)) { healingDialog = true }
                }
                if (readOnly) Text(stringResource(R.string.session_controls_locked), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
        item {
            ChoiceField(
                label = stringResource(R.string.session_ability_filter),
                value = abilityFilter,
                options = listOf(ALL_ABILITIES) + AbilityKind.entries.map { it.name },
                enabled = true,
                modifier = Modifier.fillMaxWidth().testTag("session_ability_filter"),
                display = { value -> AbilityKind.entries.firstOrNull { it.name == value }?.label ?: allAbilitiesLabel },
            ) { abilityFilter = it }
        }
        item {
            SectionHeader("01", stringResource(R.string.session_ability_section))
            if (filteredEntries.isEmpty()) {
                Text(
                    text = stringResource(R.string.session_no_abilities),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        items(filteredEntries, key = { "${character.id}:${it.key}" }) { entry ->
            SessionAbilityCard(entry, compact, !readOnly) { pendingAbilityId = entry.ability.id }
        }
        item {
            TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                SectionHeader("02", "Recursos rápidos")
                val resources = listOf(
                    SessionResourceValue("VIDA", character.life.current, character.lifeMaximum, SessionResource.LIFE),
                    SessionResourceValue("SANIDADE", character.sanity.current, character.sanityMaximum, SessionResource.SANITY),
                    SessionResourceValue("ARCANO", character.arcane.current, character.arcaneMaximum, SessionResource.ARCANE),
                    SessionResourceValue("ENERGIA", character.energy.current, character.energyMaximum, SessionResource.ENERGY),
                    SessionResourceValue("DESTINO", character.destiny.current, character.destinyMaximum, SessionResource.DESTINY, SessionOperationType.DESTINY),
                    SessionResourceValue("EXAUSTÃO", character.exhaustion.current, character.exhaustion.maximum, SessionResource.EXHAUSTION),
                    SessionResourceValue("CORRUPÇÃO", character.corruption.current, character.corruption.maximum, SessionResource.CORRUPTION),
                )
                resources.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { resource ->
                            ResourceControl(resource.label, resource.current, resource.maximum, !readOnly, compact, Modifier.weight(1f)) { next ->
                                onCommand(character, SessionCommand(type = resource.operationType, resource = resource.resource, amount = next - resource.current))
                            }
                        }
                        if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
                MoneyControl(character.money, !readOnly, compact) { next ->
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
        item {
            TechPanel {
                SectionHeader("03", "Proteções")
                character.calculatedProtections().forEach { (name, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value.toString(), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item {
            TechPanel(accent = MaterialTheme.colorScheme.error) {
                SectionHeader("04", "Corpo")
                character.canonicalBodyState().regions.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { region ->
                            SdoInsetCard(
                                modifier = Modifier.weight(1f), contentPadding = if (compact) 6.dp else 9.dp,
                                verticalSpacing = 2.dp,
                            ) {
                                Text(region.name.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                                SessionBodyStatus(region.state.label.uppercase(), region.failures)
                                Text("PL ${character.localProtection(region)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            val readyItems = character.inventory.filter {
                it.inventoryState in setOf(InventoryState.WIELDED, InventoryState.EQUIPPED, InventoryState.QUICK_ACCESS) ||
                    it.category.equals("Munição", true) || it.linkedAshId.isNotBlank()
            }
            TechPanel {
                SectionHeader("05", "Equipamento pronto")
                if (readyItems.isEmpty()) Text("Nenhum equipamento operacional.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                readyItems.forEach { item ->
                    SdoInsetCard(contentPadding = 8.dp, verticalSpacing = 4.dp) {
                        SdoCardTitle(
                            item.name.ifBlank { "Item sem nome" }, modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            item.category.ifBlank { "ITEM" }.uppercase(), modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall,
                        )
                        SdoMetadataText(
                            buildList {
                                add(item.inventoryState.label.uppercase())
                                add("CARGA ${item.effectiveLoad()}")
                                if (item.category.equals("Munição", true) || item.linkedAshId.isNotBlank()) add("QTD ${item.quantity}")
                            }.joinToString(" // "),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                SdoMetadataText("Consulta apenas // altere equipamento e munição na ficha completa", style = MaterialTheme.typography.labelSmall)
            }
        }
        item {
            val conditions = character.canonicalConditions()
            TechPanel(accent = if (conditions.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) {
                SectionHeader("06", "Condições // ${conditions.size}")
                if (conditions.isEmpty()) Text("Nenhuma condição ativa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    if (damageDialog) DamageDialog(character, onDismiss = { damageDialog = false }) { amount, region ->
        damageDialog = false
        onCommand(character, SessionCommand(type = SessionOperationType.DAMAGE, amount = amount, bodyRegion = region))
    }
    if (healingDialog) AmountDialog("APLICAR CURA", character.life.current, character.lifeMaximum, { healingDialog = false }) { amount ->
        healingDialog = false
        onCommand(character, SessionCommand(type = SessionOperationType.HEAL, resource = SessionResource.LIFE, amount = amount))
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
private fun SessionAbilityCard(entry: SessionAbilityEntry, compact: Boolean, editable: Boolean, onUse: () -> Unit) {
    var expanded by rememberSaveable(entry.ability.id) { mutableStateOf(false) }
    val accent = if (entry.ability.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val expandedLabel = stringResource(if (expanded) R.string.session_collapse_ability else R.string.session_expand_ability)
    val unavailableLabel = stringResource(R.string.session_unavailable)
    val largeText = LocalDensity.current.fontScale * LocalSdoPreferences.current.fontScale.multiplier > 1.15f
    SdoInsetCard(
        modifier = Modifier.fillMaxWidth(),
        accent = if (entry.ability.available) MaterialTheme.colorScheme.outlineVariant else accent,
        contentPadding = if (compact) 6.dp else 8.dp,
        verticalSpacing = 4.dp,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .testTag("session_ability_expand")
                    .semantics { stateDescription = expandedLabel }
                    .sdoClickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        (if (entry.ability.favorite) "★ " else "") + entry.ability.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (!largeText) SessionAbilityMetadata(entry)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, expandedLabel)
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
        modifier.background(MaterialTheme.colorScheme.surfaceVariant, CutCornerShape(topEnd = 8.dp, bottomStart = 6.dp)).padding(if (compact) 5.dp else 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SdoIconButton({ onChange(current - 1) }, enabled = enabled && current > 0, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Remove, "Reduzir $label") }
            Text("$current/$maximum", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            SdoIconButton({ onChange(current + 1) }, enabled = enabled && current < maximum, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, "Aumentar $label") }
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
private fun DamageDialog(character: Character, onDismiss: () -> Unit, onConfirm: (Int, BodyRegionSlot) -> Unit) {
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
        confirmButton = { SdoTextButton({ onConfirm(amount, region.region) }) { Text("CONFIRMAR") } },
        dismissButton = { SdoTextButton(onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun AmountDialog(title: String, current: Int, maximum: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
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
        confirmButton = { SdoTextButton({ onConfirm(amount) }) { Text("CONFIRMAR") } },
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
