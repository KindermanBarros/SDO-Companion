package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.R
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.DeityDetailView
import com.kinderman.sdo.domain.model.DivinePillarKind
import com.kinderman.sdo.domain.model.DivineRules
import com.kinderman.sdo.domain.model.DivineTier
import com.kinderman.sdo.domain.model.convertVitalityOrAnchorToDestiny
import com.kinderman.sdo.domain.model.derivedDivineEffects
import com.kinderman.sdo.domain.model.resetDivineScene
import com.kinderman.sdo.domain.model.toDeityPublicProfile
import com.kinderman.sdo.domain.model.violateDivineTabu
import com.kinderman.sdo.domain.model.withAddedDivineMark
import com.kinderman.sdo.domain.model.withDivineAscension
import com.kinderman.sdo.domain.model.withRemovedDivineMark
import com.kinderman.sdo.domain.model.withUpdatedDivineMark
import com.kinderman.sdo.presentation.library.DeityDetailDialog
import com.kinderman.sdo.ui.SdoButton
import com.kinderman.sdo.ui.SdoCard
import com.kinderman.sdo.ui.SdoOutlinedButton
import com.kinderman.sdo.ui.SdoResponsiveGrid
import com.kinderman.sdo.ui.SdoTextButton

private enum class DivineDialog { ASCENSION, MARK, DOMAIN_ACT, TABU, RESET, CORRUPTION, RECOVER }

@Composable
internal fun CharacterDivineSection(
    character: Character,
    catalog: List<CatalogEntry>,
    editable: Boolean,
    canChangeAscension: Boolean,
    onChange: (Character) -> Unit,
) {
    val deities = catalog.filter { it.kind == CatalogKind.DEITY }.distinctBy(CatalogEntry::id)
    val deitiesById = deities.associateBy(CatalogEntry::id)
    val ascension = character.divineAscension
    val runtime = character.divineRuntime
    val effects = character.derivedDivineEffects()
    var dialog by rememberSaveable(character.id) { mutableStateOf<DivineDialog?>(null) }
    var deityId by rememberSaveable(character.id) { mutableStateOf<String?>(null) }
    var markId by rememberSaveable(character.id) { mutableStateOf<String?>(null) }
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

    Column(
        modifier = Modifier.fillMaxWidth().testTag("character_divine_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(effects.tier.label, style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.character_divine_destiny, character.destiny.current, character.destinyMaximum))
        Text(stringResource(R.string.character_divine_session_pd, effects.pdPerSession))
        if (effects.usesConceptualPillars) {
            Text(stringResource(R.string.character_divine_pillars), style = MaterialTheme.typography.titleMedium)
            SdoResponsiveGrid(DivinePillarKind.entries) { pillar, modifier ->
                SdoCard(modifier = modifier) {
                    Text(
                        stringResource(R.string.character_divine_pillar_value, pillar.label, ascension.pillars.get(pillar)),
                        modifier = Modifier.padding(12.dp).testTag("character_divine_pillar_${pillar.name}"),
                    )
                }
            }
            Text(effects.presencaQualitativeSummary)
        }
        if (effects.immuneToMortalMentalEffects) Text(stringResource(R.string.character_divine_mental_immunity))
        if (effects.canReadDarnoScript) Text(stringResource(R.string.character_divine_darno_reading))
        if (effects.hasInstantDarnoRunes) {
            Text(stringResource(R.string.character_divine_darno_runes, DivineRules.DARNO_INSTANT_RUNE_PD_COST))
        }
        if (effects.darnoEmotionalEchoActive) Text(stringResource(R.string.character_divine_darno_echo))
        val patron = ascension.displayPatronLabel(deitiesById)
        if (patron.isNotBlank()) {
            DivineReference(stringResource(R.string.character_divine_patron, patron), deitiesById[ascension.patronDeityId]) {
                deityId = it
            }
        }
        listOf(
            R.string.character_divine_domain to ascension.dominio,
            R.string.character_divine_sanctuary to ascension.santuario,
            R.string.character_divine_posture to ascension.postura,
            R.string.character_divine_thought to ascension.linhaDePensamento,
            R.string.character_divine_tabu to ascension.tabu,
        ).forEach { (label, value) ->
            if (value.isNotBlank()) {
                Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
                Text(value)
            }
        }
        if (effects.hasMaterialRestriction && ascension.materialRestrictionNote.isNotBlank()) {
            Text(ascension.materialRestrictionNote)
        }
        ascension.passiveCapabilities.forEach { passive ->
            Text(passive.name, style = MaterialTheme.typography.titleSmall)
            Text(passive.description)
        }
        if (canChangeAscension) {
            DivineAction(R.string.character_divine_edit_ascension, "ascension") {
                dialog = DivineDialog.ASCENSION
            }
        }

        Text(stringResource(R.string.character_divine_marks), style = MaterialTheme.typography.titleMedium)
        runtime.marks.forEach { mark ->
            val markLabel = stringResource(
                R.string.character_divine_mark_reference, mark.kind.label, mark.displayDeityLabel(deitiesById),
            )
            DivineReference(markLabel, deitiesById[mark.deityId]) { deityId = it }
            Text(stringResource(if (mark.active) R.string.character_divine_anchor else R.string.character_divine_inactive))
            if (mark.giftName.isNotBlank()) {
                Text(stringResource(R.string.character_divine_gift_summary, mark.giftName, mark.executionKind.label))
            }
            if (mark.effect.isNotBlank()) Text(mark.effect)
            if (mark.downside.isNotBlank()) Text(mark.downside)
            if (editable) {
                DivineAction(R.string.character_divine_edit_mark, "edit_mark_${mark.id}") {
                    markId = mark.id
                    dialog = DivineDialog.MARK
                }
            }
        }
        if (editable) {
            DivineAction(R.string.character_divine_add_mark, "add_mark") {
                markId = null
                dialog = DivineDialog.MARK
            }
        }

        if (effects.tier != DivineTier.MORTAL) {
            Text(stringResource(R.string.character_divine_scene_pd, runtime.pdSpentInCurrentScene))
            effects.avatarAutonomyCheckDc?.let { Text(stringResource(R.string.character_divine_autonomy, it)) }
            if (effects.domainActsBlockedByTabu) Text(stringResource(R.string.character_divine_blocked))
            if (editable) {
                if (effects.usesConceptualPillars) {
                    DivineAction(R.string.character_divine_domain_act, "domain_act", !effects.domainActsBlockedByTabu) {
                        dialog = DivineDialog.DOMAIN_ACT
                    }
                }
                if (ascension.tabu.isNotBlank()) {
                    DivineAction(R.string.character_divine_break_tabu, "tabu", !effects.domainActsBlockedByTabu) {
                        dialog = DivineDialog.TABU
                    }
                }
                if (runtime.tabuViolatedInScene || runtime.pdSpentInCurrentScene > 0) {
                    DivineAction(R.string.character_divine_reset, "reset") { dialog = DivineDialog.RESET }
                }
                DivineAction(
                    R.string.character_divine_recover, "recover",
                    effects.canConvertSanityOrAnchorToPd && character.destiny.current < character.destinyMaximum,
                ) { dialog = DivineDialog.RECOVER }
            }
        }

        Text(stringResource(R.string.character_divine_corruption), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.character_divine_corruption_value, character.corruption.current, effects.corruptionStage.label))
        Text(effects.corruptionStage.summary)
        if (editable) {
            DivineAction(R.string.character_divine_resolve_corruption, "corruption") {
                dialog = DivineDialog.CORRUPTION
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
            CharacterAscensionEditor(ascension, deities, close) { next ->
                change { it.withDivineAscension(next) }
            }
        }
        DivineDialog.MARK -> {
            val existing = runtime.marks.firstOrNull { it.id == markId }
            if (markId == null || existing != null) {
                CharacterDivineMarkEditor(
                    existing = existing,
                    deities = deities,
                    onDismiss = close,
                    onRemove = { id -> change { it.withRemovedDivineMark(id) } },
                    onSave = { mark ->
                        change { if (existing == null) it.withAddedDivineMark(mark) else it.withUpdatedDivineMark(mark) }
                    },
                )
            }
        }
        DivineDialog.DOMAIN_ACT -> if (effects.usesConceptualPillars) {
            CharacterDomainActEditor(character, close, change)
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
        DivineDialog.CORRUPTION -> CharacterCorruptionEditor(character, deities, close, change)
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

@Composable
private fun DivineReference(label: String, deity: CatalogEntry?, onOpen: (String) -> Unit) {
    if (deity == null) {
        Text(label)
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
