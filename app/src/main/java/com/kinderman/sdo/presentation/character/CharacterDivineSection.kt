package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.R
import com.kinderman.sdo.domain.creation.DivineAscensionCreation
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.DeityDetailView
import com.kinderman.sdo.domain.model.DivineFate
import com.kinderman.sdo.domain.model.DivinePillarKind
import com.kinderman.sdo.domain.model.DivineRules
import com.kinderman.sdo.domain.model.DivineTier
import com.kinderman.sdo.domain.model.convertVitalityOrAnchorToDestiny
import com.kinderman.sdo.domain.model.currentDivinePillars
import com.kinderman.sdo.domain.model.derivedDivineEffects
import com.kinderman.sdo.domain.model.displayDeityLabel
import com.kinderman.sdo.domain.model.displayPatronLabel
import com.kinderman.sdo.domain.model.resetDivineScene
import com.kinderman.sdo.domain.model.resolveAvailableDeityOptions
import com.kinderman.sdo.domain.model.toDeityPublicProfile
import com.kinderman.sdo.domain.model.violateDivineTabu
import com.kinderman.sdo.domain.model.withAddedDivineMark
import com.kinderman.sdo.domain.model.withDivineAscension
import com.kinderman.sdo.domain.model.withRemovedDivineMark
import com.kinderman.sdo.domain.model.withUpdatedDivineMark
import com.kinderman.sdo.presentation.library.DeityDetailDialog
import com.kinderman.sdo.ui.SdoButton
import com.kinderman.sdo.ui.SdoCardTitle
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.SdoOutlinedButton
import com.kinderman.sdo.ui.SdoResponsiveGrid
import com.kinderman.sdo.ui.SdoTextButton
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.sdoClickable

internal val DivinePinkAccent = Color(0xFFEE227D)

private enum class DivineDialog { ASCENSION, TABU, RESET, CORRUPTION, RECOVER }

/**
 * Profile-embedded divine card (pink accent) matching the Race & Level pattern.
 * Hidden from non-GM players while [Character.divineAscension] is [DivineTier.MORTAL];
 * GMs/Admins always see the card so they can promote a mortal sheet into Avatar/Semideus/Deus.
 */
@Composable
internal fun CharacterDivineSection(
    character: Character,
    catalog: List<CatalogEntry>,
    editable: Boolean,
    canChangeAscension: Boolean,
    onChange: (Character) -> Unit,
    patronPresenca: Int? = null,
    characters: List<Character> = emptyList(),
) {
    val ascension = character.divineAscension
    val runtime = character.divineRuntime
    val effects = character.derivedDivineEffects(patronPresenca)
    val isDivine = effects.tier != DivineTier.MORTAL
    if (!isDivine && !canChangeAscension) return

    val deities = catalog.filter { it.kind == CatalogKind.DEITY }.distinctBy(CatalogEntry::id)
    val deitiesById = deities.associateBy(CatalogEntry::id)
    val charactersById = characters.associateBy(Character::id)
    val deityOptions = resolveAvailableDeityOptions(character, catalog, characters)
    val currentPillars = character.currentDivinePillars()

    var dialog by rememberSaveable(character.id) { mutableStateOf<DivineDialog?>(null) }
    var deityId by rememberSaveable(character.id) { mutableStateOf<String?>(null) }
    var showGmPowerControls by rememberSaveable(character.id) { mutableStateOf(false) }
    var error by rememberSaveable(character.id) { mutableStateOf<String?>(null) }
    val close = { dialog = null }
    val change: ((Character) -> Character) -> Unit = { transform ->
        if (editable) {
            try {
                val next = transform(character)
                if (next != character) onChange(next)
                error = null
                close()
            } catch (failure: IllegalArgumentException) {
                error = failure.message
                close()
            }
        }
    }

    TechPanel(
        modifier = Modifier.fillMaxWidth().testTag("character_divine_section"),
        accent = DivinePinkAccent,
    ) {
        SectionHeader("DV", stringResource(R.string.character_divine_title))
        SdoInsetCard(accent = DivinePinkAccent, contentPadding = 12.dp, verticalSpacing = 6.dp) {
            SdoMetadataText(
                "CATEGORIA DIVINA // ${effects.mechanicalScale.label.uppercase()}",
                color = DivinePinkAccent,
            )
            SdoCardTitle(
                text = effects.tier.label.uppercase(),
                color = DivinePinkAccent,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "${stringResource(R.string.character_divine_destiny, character.destiny.current, character.destinyMaximum)} // ${stringResource(R.string.character_divine_session_pd, effects.pdPerSession)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            val patron = ascension.displayPatronLabel(deitiesById, charactersById)
            if (patron.isNotBlank()) {
                DivineReference(
                    label = stringResource(R.string.character_divine_patron, patron),
                    deity = deitiesById[ascension.patronDeityId],
                ) { deityId = it }
            }
        }

        if (canChangeAscension) {
            if (effects.tier in setOf(DivineTier.MORTAL, DivineTier.AVATAR, DivineTier.DEMIGOD)) {
                DivineAction(
                    label = R.string.character_divine_unlock_lesser,
                    tag = "unlock_lesser_god",
                ) {
                    change { DivineAscensionCreation.unlockPromotion(it, DivineTier.LESSER_GOD) }
                }
            } else if (effects.tier == DivineTier.LESSER_GOD) {
                DivineAction(
                    label = R.string.character_divine_unlock_greater,
                    tag = "unlock_greater_god",
                ) {
                    change { DivineAscensionCreation.unlockPromotion(it, DivineTier.GREATER_GOD) }
                }
            }
            DivineAction(
                label = if (isDivine) R.string.character_divine_edit_ascension else R.string.character_divine_awaken_ascension,
                tag = "ascension",
            ) {
                dialog = DivineDialog.ASCENSION
            }
        }

        if (effects.usesConceptualPillars) {
            SdoMetadataText(
                stringResource(R.string.character_divine_pillars).uppercase(),
                color = DivinePinkAccent,
            )
            SdoResponsiveGrid(DivinePillarKind.entries, minItemWidth = 132.dp) { pillar, modifier ->
                SdoInsetCard(
                    modifier = modifier.testTag("character_divine_pillar_${pillar.name}"),
                    accent = DivinePinkAccent.copy(alpha = 0.65f),
                    contentPadding = 10.dp,
                    verticalSpacing = 2.dp,
                ) {
                    SdoMetadataText(pillar.label.uppercase(), color = DivinePinkAccent)
                    Text(
                        "${currentPillars.get(pillar)} ATUAL // ${ascension.pillars.get(pillar)} PERM.",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Text(
                effects.presencaQualitativeSummary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            if (runtime.divineFate == DivineFate.DEFEATED) {
                Text(stringResource(R.string.character_divine_defeated), color = DivinePinkAccent)
            }
            if (runtime.divineFate == DivineFate.SEALED || runtime.divineFate == DivineFate.ERASED) {
                Text(stringResource(R.string.character_divine_fate_blocked), color = MaterialTheme.colorScheme.error)
            }
        }

        val traits = buildList {
            if (effects.immuneToMortalMentalEffects) add(stringResource(R.string.character_divine_mental_immunity))
            if (effects.canReadDarnoScript) add(stringResource(R.string.character_divine_darno_reading))
            if (effects.hasInstantDarnoRunes) add(stringResource(R.string.character_divine_darno_runes, DivineRules.DARNO_INSTANT_RUNE_PD_COST))
            if (effects.darnoEmotionalEchoActive) add(stringResource(R.string.character_divine_darno_echo))
            if (effects.hasMaterialRestriction && ascension.materialRestrictionNote.isNotBlank()) add(ascension.materialRestrictionNote)
        }
        if (traits.isNotEmpty()) {
            SdoInsetCard(accent = DivinePinkAccent.copy(alpha = 0.45f), contentPadding = 10.dp, verticalSpacing = 4.dp) {
                traits.forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        val identityDetails = listOf(
            R.string.character_divine_domain to ascension.dominio,
            R.string.character_divine_sanctuary to ascension.santuario,
            R.string.character_divine_posture to ascension.postura,
            R.string.character_divine_thought to ascension.linhaDePensamento,
            R.string.character_divine_tabu to ascension.tabu,
        ).filter { it.second.isNotBlank() }
        if (identityDetails.isNotEmpty() || ascension.heraldGift.name.isNotBlank()) {
            SdoInsetCard(accent = DivinePinkAccent.copy(alpha = 0.45f), contentPadding = 10.dp, verticalSpacing = 6.dp) {
                identityDetails.forEach { (label, value) ->
                    SdoMetadataText(stringResource(label).uppercase(), color = DivinePinkAccent)
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
                if (ascension.heraldGift.name.isNotBlank()) {
                    SdoMetadataText(
                        stringResource(
                            R.string.character_divine_herald_gift_profile,
                            ascension.heraldGift.name,
                            ascension.heraldGift.executionKind.label,
                        ),
                        color = DivinePinkAccent,
                    )
                    if (ascension.heraldGift.effect.isNotBlank()) {
                        Text(ascension.heraldGift.effect, style = MaterialTheme.typography.bodySmall)
                    }
                    if (ascension.heraldGift.downside.isNotBlank()) {
                        Text(
                            "Contrapartida: ${ascension.heraldGift.downside}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DivinePinkAccent,
                        )
                    }
                }
            }
        }

        if (isDivine) {
            SdoInsetCard(accent = DivinePinkAccent.copy(alpha = 0.45f), contentPadding = 10.dp, verticalSpacing = 6.dp) {
                SdoMetadataText(
                    "${stringResource(R.string.character_divine_scene_pd, runtime.pdSpentInCurrentScene)} // ${stringResource(R.string.character_divine_corruption_value, character.corruption.current, effects.corruptionStage.label)}",
                    color = DivinePinkAccent,
                )
                Text(
                    stringResource(R.string.character_divine_powers_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (effects.tier == DivineTier.AVATAR && patronPresenca == null &&
                    (runtime.pdSpentInCurrentScene >= 2 || runtime.tabuViolatedInScene)
                ) {
                    Text(stringResource(R.string.character_divine_autonomy_unknown), color = DivinePinkAccent)
                }
                effects.avatarAutonomyCheckDc?.let {
                    Text(stringResource(R.string.character_divine_autonomy, it), color = DivinePinkAccent)
                }
                if (effects.domainActsBlockedByTabu) {
                    Text(stringResource(R.string.character_divine_blocked), color = MaterialTheme.colorScheme.error)
                }
                if (editable) {
                    if (ascension.tabu.isNotBlank()) {
                        DivineAction(R.string.character_divine_break_tabu, "tabu", !effects.domainActsBlockedByTabu) {
                            dialog = DivineDialog.TABU
                        }
                    }
                    if (runtime.tabuViolatedInScene || runtime.pdSpentInCurrentScene > 0 || runtime.channeledInCurrentScene) {
                        DivineAction(R.string.character_divine_reset, "reset") { dialog = DivineDialog.RESET }
                    }
                    DivineAction(
                        R.string.character_divine_recover,
                        "recover",
                        effects.canConvertSanityOrAnchorToPd && character.destiny.current < character.destinyMaximum,
                    ) { dialog = DivineDialog.RECOVER }
                    DivineAction(R.string.character_divine_resolve_corruption, "corruption") {
                        dialog = DivineDialog.CORRUPTION
                    }
                }
            }
        }

        if (effects.usesConceptualPillars) {
            SdoInsetCard(accent = DivinePinkAccent.copy(alpha = 0.55f), contentPadding = 10.dp, verticalSpacing = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().sdoClickable { showGmPowerControls = !showGmPowerControls },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SdoMetadataText(
                        stringResource(R.string.divine_power_title).uppercase(),
                        color = DivinePinkAccent,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (showGmPowerControls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = DivinePinkAccent,
                    )
                }
                if (showGmPowerControls) {
                    CharacterDivinePowerSection(
                        character = character,
                        editable = editable,
                        canChangeAscension = canChangeAscension,
                        onChange = onChange,
                    )
                }
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    deitiesById[deityId]?.let { deity ->
        DeityDetailDialog(
            deityId = deity.id,
            campaignId = null,
            session = null,
            canShowGm = false,
            load = { _, _ -> DeityDetailView(deity.toDeityPublicProfile()) },
            onDismiss = { deityId = null },
        )
    }

    if (!editable) return
    when (dialog) {
        DivineDialog.ASCENSION -> if (canChangeAscension) {
            CharacterAscensionEditor(ascension, deityOptions, close) { next ->
                change { it.withDivineAscension(next) }
            }
        }
        DivineDialog.TABU -> DivineConfirmation(
            R.string.character_divine_break_tabu,
            stringResource(R.string.character_divine_tabu_confirm, DivineRules.TABU_VIOLATION_PD_COST),
            close,
        ) { change { if (it.divineRuntime.tabuViolatedInScene) it else it.violateDivineTabu() } }
        DivineDialog.RESET -> DivineConfirmation(
            R.string.character_divine_reset,
            stringResource(R.string.character_divine_reset_confirm),
            close,
        ) { change(Character::resetDivineScene) }
        DivineDialog.CORRUPTION -> CharacterCorruptionEditor(character, deityOptions, close, change)
        DivineDialog.RECOVER -> DivineConfirmation(
            R.string.character_divine_recover,
            stringResource(
                if (effects.usesConceptualPillars) R.string.character_divine_recover_anchor
                else R.string.character_divine_recover_sanity,
            ),
            close,
        ) { change(Character::convertVitalityOrAnchorToDestiny) }
        null -> Unit
    }
}

/**
 * Divine Marks (Arauto / Marcado) shared inside the Conditions tab (`13 CONDIÇÕES`),
 * since carrying a divine mark is mechanically a divine condition on the character.
 */
@Composable
internal fun CharacterDivineMarksConditionPanel(
    character: Character,
    catalog: List<CatalogEntry>,
    characters: List<Character>,
    enabled: Boolean,
    onChange: (Character) -> Unit,
) {
    val deities = catalog.filter { it.kind == CatalogKind.DEITY }.distinctBy(CatalogEntry::id)
    val deitiesById = deities.associateBy(CatalogEntry::id)
    val charactersById = characters.associateBy(Character::id)
    val deityOptions = resolveAvailableDeityOptions(character, catalog, characters)
    val marks = character.divineRuntime.marks
    var editingMarkId by rememberSaveable(character.id) { mutableStateOf<String?>(null) }
    var addingMark by rememberSaveable(character.id) { mutableStateOf(false) }
    var openDeityId by rememberSaveable(character.id) { mutableStateOf<String?>(null) }

    TechPanel(
        modifier = Modifier.fillMaxWidth().testTag("character_divine_marks_section"),
        accent = DivinePinkAccent,
    ) {
        SectionHeader("13B", stringResource(R.string.character_divine_marks))
        if (marks.isEmpty()) {
            SdoInsetCard(accent = DivinePinkAccent.copy(alpha = 0.45f), contentPadding = 12.dp) {
                Text(
                    stringResource(R.string.character_divine_no_marks),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        marks.forEachIndexed { index, mark ->
            val deityLabel = mark.displayDeityLabel(deitiesById, charactersById)
            SdoInsetCard(
                accent = DivinePinkAccent.copy(alpha = if (mark.active) 0.85f else 0.35f),
                contentPadding = 12.dp,
                verticalSpacing = 6.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SdoMetadataText(
                        "MARCA ${(index + 1).toString().padStart(2, '0')} // ${if (mark.active) "ÂNCORA ATIVA" else "DORMENTE"}",
                        color = DivinePinkAccent,
                        modifier = Modifier.weight(1f),
                    )
                    if (enabled) {
                        SdoTextButton(
                            onClick = { editingMarkId = mark.id },
                            modifier = Modifier.testTag("character_divine_edit_mark_${mark.id}"),
                        ) {
                            Text("EDITAR", color = DivinePinkAccent)
                        }
                    }
                }
                SdoCardTitle(
                    text = stringResource(R.string.character_divine_mark_reference, mark.kind.label, deityLabel),
                    color = DivinePinkAccent,
                    style = MaterialTheme.typography.titleMedium,
                )
                deitiesById[mark.deityId]?.let { catalogDeity ->
                    DivineReference(
                        label = stringResource(R.string.deity_open_detail),
                        deity = catalogDeity,
                    ) { openDeityId = it }
                }
                if (mark.giftName.isNotBlank()) {
                    SdoMetadataText(
                        stringResource(R.string.character_divine_gift_summary, mark.giftName, mark.executionKind.label),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                if (mark.effect.isNotBlank()) {
                    Text(mark.effect, style = MaterialTheme.typography.bodyMedium)
                }
                if (mark.downside.isNotBlank()) {
                    Text(
                        "Contrapartida: ${mark.downside}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DivinePinkAccent,
                    )
                }
            }
        }
        if (enabled) {
            DivineAction(R.string.character_divine_add_mark, "add_mark") {
                addingMark = true
            }
        }
    }

    deitiesById[openDeityId]?.let { deity ->
        DeityDetailDialog(
            deityId = deity.id,
            campaignId = null,
            session = null,
            canShowGm = false,
            load = { _, _ -> DeityDetailView(deity.toDeityPublicProfile()) },
            onDismiss = { openDeityId = null },
        )
    }

    if (!enabled) return
    if (addingMark) {
        CharacterDivineMarkEditor(
            existing = null,
            deities = deityOptions,
            onDismiss = { addingMark = false },
            onRemove = { addingMark = false },
            onSave = { mark ->
                onChange(character.withAddedDivineMark(mark))
                addingMark = false
            },
        )
    } else if (editingMarkId != null) {
        val existing = marks.firstOrNull { it.id == editingMarkId }
        if (existing != null) {
            CharacterDivineMarkEditor(
                existing = existing,
                deities = deityOptions,
                onDismiss = { editingMarkId = null },
                onRemove = { id ->
                    onChange(character.withRemovedDivineMark(id))
                    editingMarkId = null
                },
                onSave = { mark ->
                    onChange(character.withUpdatedDivineMark(mark))
                    editingMarkId = null
                },
            )
        }
    }
}

@Composable
private fun DivineReference(label: String, deity: CatalogEntry?, onOpen: (String) -> Unit) {
    if (deity == null) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
    } else {
        SdoOutlinedButton(
            onClick = { onOpen(deity.id) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("character_divine_reference_${deity.id}"),
        ) { Text(label) }
    }
}

@Composable
internal fun DivineAction(label: Int, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    SdoOutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("character_divine_$tag"),
        enabled = enabled,
    ) { Text(stringResource(label)) }
}

@Composable
private fun DivineConfirmation(title: Int, message: String, dismiss: () -> Unit, apply: () -> Unit) {
    DivineEditorDialog(title, dismiss, onApply = apply) { Text(message) }
}

@Composable
internal fun DivineEditorDialog(
    title: Int,
    onDismiss: () -> Unit,
    canApply: Boolean = true,
    onApply: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { content() }
        },
        confirmButton = {
            SdoButton(
                onClick = onApply,
                enabled = canApply,
                modifier = Modifier.heightIn(min = 48.dp).testTag("character_divine_apply"),
            ) { Text(stringResource(R.string.character_divine_apply)) }
        },
        dismissButton = {
            SdoTextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.character_divine_cancel))
            }
        },
    )
}
