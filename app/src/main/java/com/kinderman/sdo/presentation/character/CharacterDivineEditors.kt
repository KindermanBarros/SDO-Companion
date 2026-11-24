package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.kinderman.sdo.R
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterDivineAscension
import com.kinderman.sdo.domain.model.DivineMark
import com.kinderman.sdo.domain.model.DivineMarkKind
import com.kinderman.sdo.domain.model.DivinePassiveCapability
import com.kinderman.sdo.domain.model.DivinePillarKind
import com.kinderman.sdo.domain.model.DivineRules
import com.kinderman.sdo.domain.model.DivineScale
import com.kinderman.sdo.domain.model.DivineTier
import com.kinderman.sdo.domain.model.ExecutionKind
import com.kinderman.sdo.domain.model.applyDivineCorruption
import com.kinderman.sdo.domain.model.currentDivinePillars
import com.kinderman.sdo.domain.model.performDomainAct
import com.kinderman.sdo.domain.model.toDeityPublicProfile
import com.kinderman.sdo.ui.SdoField
import com.kinderman.sdo.ui.SdoFilterChip
import com.kinderman.sdo.ui.SdoResponsiveGrid
import kotlinx.serialization.json.Json

private inline fun <reified T : Any> divineDraftSaver(): Saver<T, String> =
    Saver(save = { Json.encodeToString(it) }, restore = { Json.decodeFromString<T>(it) })

@Composable
internal fun CharacterAscensionEditor(
    ascension: CharacterDivineAscension,
    deities: List<CatalogEntry>,
    onDismiss: () -> Unit,
    onSave: (CharacterDivineAscension) -> Unit,
) {
    var draft by rememberSaveable(ascension, stateSaver = divineDraftSaver<CharacterDivineAscension>()) {
        mutableStateOf(ascension)
    }
    val conceptual = draft.tier.scale == DivineScale.CONCEPTUAL_PILLARS
    val usesPatron = draft.tier == DivineTier.AVATAR || draft.tier == DivineTier.DEMIGOD
    DivineEditorDialog(
        R.string.character_divine_edit_ascension,
        onDismiss,
        canApply = DivineRules.validAscensionPillars(ascension, draft) && draft.tier.playableOnCharacterSheet &&
            (draft.tier != DivineTier.AVATAR || draft.patronDeityId.isNotBlank()),
        onApply = { onSave(draft) },
    ) {
        DivineChoices(
            DivineTier.entries.filter(DivineTier::playableOnCharacterSheet),
            draft.tier, { it.label },
        ) { tier ->
            draft = draft.copy(
                tier = tier,
                pillars = if (tier == ascension.tier) ascension.pillars else draft.pillars,
            )
        }
        if (usesPatron) {
            DivineDeityPicker(R.string.character_divine_choose_patron, draft.patronDeityId, deities) { deity ->
                draft = draft.copy(patronDeityId = deity?.id.orEmpty(), patronName = deity?.name.orEmpty())
            }
        }
        if (conceptual) {
            val maximum = if (draft.tier == DivineTier.LESSER_GOD) 4 else 5
            Text(stringResource(R.string.character_divine_pillar_range, maximum))
            Text(stringResource(R.string.character_divine_pillar_budget, draft.pillars.totalPoints))
            if (draft.tier == ascension.tier) Text(stringResource(R.string.character_divine_pillar_milestones_only))
            SdoResponsiveGrid(DivinePillarKind.entries, maxColumns = 2) { pillar, modifier ->
                SdoField(
                    label = pillar.label,
                    value = draft.pillars.get(pillar).toString(),
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toIntOrNull()?.let { it in 0..maximum } == true) {
                            draft = draft.copy(pillars = draft.pillars.with(pillar, input.toIntOrNull() ?: 0))
                        }
                    },
                    modifier = modifier.testTag("character_divine_edit_pillar_${pillar.name}"),
                    enabled = draft.tier != ascension.tier,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            DivineField(R.string.character_divine_sanctuary, draft.santuario) { draft = draft.copy(santuario = it) }
        }
        if (conceptual || draft.tier == DivineTier.DEMIGOD) {
            DivineField(R.string.character_divine_domain, draft.dominio) { draft = draft.copy(dominio = it) }
            DivineField(R.string.character_divine_posture, draft.postura) { draft = draft.copy(postura = it) }
            DivineField(R.string.character_divine_thought, draft.linhaDePensamento) {
                draft = draft.copy(linhaDePensamento = it)
            }
            DivineField(R.string.character_divine_tabu, draft.tabu) { draft = draft.copy(tabu = it) }
        }
        if (draft.tier == DivineTier.GREATER_GOD) {
            DivineField(R.string.character_divine_restriction, draft.materialRestrictionNote) {
                draft = draft.copy(materialRestrictionNote = it)
            }
        }
        if (conceptual) {
            Text(stringResource(R.string.character_divine_passives), style = MaterialTheme.typography.titleSmall)
            draft.passiveCapabilities.forEach { passive ->
                fun update(next: DivinePassiveCapability) {
                    draft = draft.copy(passiveCapabilities = draft.passiveCapabilities.map {
                        if (it.id == next.id) next else it
                    })
                }
                DivineField(R.string.character_divine_passive_name, passive.name) { update(passive.copy(name = it)) }
                DivineField(R.string.character_divine_effect, passive.description) {
                    update(passive.copy(description = it))
                }
                DivineAction(R.string.character_divine_remove_passive, "remove_passive_${passive.id}") {
                    draft = draft.copy(passiveCapabilities = draft.passiveCapabilities.filterNot { it.id == passive.id })
                }
            }
            DivineAction(R.string.character_divine_add_passive, "add_passive") {
                draft = draft.copy(passiveCapabilities = draft.passiveCapabilities + DivinePassiveCapability())
            }
        }
    }
}

@Composable
internal fun CharacterDivineMarkEditor(
    existing: DivineMark?,
    deities: List<CatalogEntry>,
    onDismiss: () -> Unit,
    onRemove: (String) -> Unit,
    onSave: (DivineMark) -> Unit,
) {
    var draft by rememberSaveable(existing, stateSaver = divineDraftSaver<DivineMark>()) {
        mutableStateOf(existing ?: DivineMark())
    }
    var removing by rememberSaveable(existing?.id) { mutableStateOf(false) }
    val title = if (existing == null) R.string.character_divine_add_mark else R.string.character_divine_edit_mark
    if (removing && existing != null) {
        DivineEditorDialog(
            R.string.character_divine_remove_mark,
            onDismiss = { removing = false },
            onApply = { onRemove(existing.id) },
        ) { Text(stringResource(R.string.character_divine_remove_mark_confirm)) }
        return
    }
    DivineEditorDialog(
        title, onDismiss,
        canApply = draft.deityId.isNotBlank(),
        onApply = { onSave(draft) },
    ) {
        DivineDeityPicker(R.string.character_divine_mark_deity, draft.deityId, deities) { deity ->
            draft = draft.copy(deityId = deity?.id.orEmpty(), deityName = deity?.name.orEmpty())
        }
        DivineChoices(DivineMarkKind.entries, draft.kind, { it.label }) { draft = draft.copy(kind = it) }
        DivineField(R.string.character_divine_gift, draft.giftName) { draft = draft.copy(giftName = it) }
        DivineChoices(
            listOf(ExecutionKind.Free, ExecutionKind.Reaction, ExecutionKind.Passive, ExecutionKind.Action),
            draft.executionKind, { it.label },
        ) { draft = draft.copy(executionKind = it) }
        DivineField(R.string.character_divine_effect, draft.effect) { draft = draft.copy(effect = it) }
        DivineField(R.string.character_divine_downside, draft.downside) { draft = draft.copy(downside = it) }
        SdoFilterChip(
            label = stringResource(R.string.character_divine_anchor),
            selected = draft.active,
            onClick = { draft = draft.copy(active = !draft.active) },
            modifier = Modifier.testTag("character_divine_mark_active"),
        )
        if (existing != null) {
            DivineAction(R.string.character_divine_remove_mark, "remove_mark") { removing = true }
        }
    }
}

@Composable
internal fun CharacterDomainActEditor(
    character: Character,
    onDismiss: () -> Unit,
    onChange: ((Character) -> Character) -> Unit,
) {
    var pillar by rememberSaveable { mutableStateOf(DivinePillarKind.DOMINIO) }
    var base by rememberSaveable { mutableStateOf("0") }
    var extra by rememberSaveable { mutableStateOf("0") }
    val basePd = base.toIntOrNull()
    val extraPd = extra.toIntOrNull()
    val available = character.destiny.current
    val canApply = basePd != null && extraPd != null && basePd <= available && extraPd <= available - basePd &&
        character.corruption.current < 100 && !character.divineRuntime.tabuViolatedInScene
    DivineEditorDialog(
        R.string.character_divine_domain_act, onDismiss, canApply,
        onApply = {
            if (basePd != null && extraPd != null) onChange { it.performDomainAct(basePd, extraPd) }
        },
    ) {
        DivineChoices(DivinePillarKind.entries, pillar, { it.label }) { pillar = it }
        DivineNumberField(R.string.character_divine_base_pd, base) { base = it }
        DivineNumberField(R.string.character_divine_extra_pd, extra) { extra = it }
        val dc = DivineRules.calculateDirectResistanceDc(
            character.currentDivinePillars().get(pillar), (extraPd ?: 0).coerceAtMost(available.coerceAtLeast(0)),
        )
        Text(stringResource(R.string.character_divine_resistance, dc))
        Text(stringResource(R.string.character_divine_available_pd, available))
        if (character.divineRuntime.tabuViolatedInScene) Text(stringResource(R.string.character_divine_blocked))
    }
}

@Composable
internal fun CharacterCorruptionEditor(
    character: Character,
    deities: List<CatalogEntry>,
    onDismiss: () -> Unit,
    onChange: ((Character) -> Character) -> Unit,
) {
    var sourceId by rememberSaveable { mutableStateOf("") }
    var sourceTier by rememberSaveable { mutableStateOf<DivineTier?>(null) }
    var purify by rememberSaveable { mutableStateOf(false) }
    var amount by rememberSaveable { mutableStateOf(DivineRules.DEFAULT_CORRUPTION_GAIN.toString()) }
    val raw = amount.toIntOrNull()
    val delta = if (purify) -(raw ?: 0) else raw ?: 0
    val projected = character.applyDivineCorruption(sourceTier, sourceId, delta).corruption.current
    val valid = raw != null && raw in 1..100
    DivineEditorDialog(
        R.string.character_divine_resolve_corruption, onDismiss, valid,
        onApply = { onChange { it.applyDivineCorruption(sourceTier, sourceId, delta) } },
    ) {
        DivineChoices(
            listOf(false, true), purify,
            { if (it) stringResource(R.string.character_divine_purify) else stringResource(R.string.character_divine_gain) },
        ) {
            purify = it
            amount = (if (it) -DivineRules.DEFAULT_CORRUPTION_PURIFICATION else DivineRules.DEFAULT_CORRUPTION_GAIN).toString()
        }
        if (!purify) {
            DivineDeityPicker(R.string.character_divine_source, sourceId, deities) { deity ->
                sourceId = deity?.id.orEmpty()
                sourceTier = deity?.toDeityPublicProfile()?.tier
            }
            if (sourceId.isBlank()) {
                DivineChoices(
                    listOf(null) + DivineTier.entries, sourceTier,
                    { it?.label ?: stringResource(R.string.character_divine_unknown_source) },
                ) { sourceTier = it }
            }
        }
        DivineNumberField(R.string.character_divine_amount, amount, maximum = 100) { amount = it }
        if (valid) Text(stringResource(R.string.character_divine_corruption_preview, character.corruption.current, projected))
    }
}

@Composable
private fun DivineDeityPicker(
    title: Int,
    selectedId: String,
    deities: List<CatalogEntry>,
    onSelect: (CatalogEntry?) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = deities.firstOrNull { it.id == selectedId }
    DivineAction(title, "deity_picker") { expanded = !expanded }
    if (selected != null) Text(selected.name)
    else if (selectedId.isNotBlank()) Text(selectedId)
    if (expanded) {
        DivineChoices(
            listOf(null) + deities, selected,
            { it?.name ?: stringResource(R.string.character_divine_no_deity) },
        ) {
            onSelect(it)
            expanded = false
        }
    }
}

@Composable
private fun <T> DivineChoices(
    values: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    SdoResponsiveGrid(values, maxColumns = 2) { choice, modifier ->
        SdoFilterChip(label(choice), choice == selected, { onSelect(choice) }, modifier)
    }
}

@Composable
private fun DivineField(label: Int, value: String, onChange: (String) -> Unit) {
    SdoField(stringResource(label), value, onChange, Modifier.fillMaxWidth(), multiline = true)
}

@Composable
private fun DivineNumberField(label: Int, value: String, maximum: Int = Int.MAX_VALUE, onChange: (String) -> Unit) {
    SdoField(
        label = stringResource(label),
        value = value,
        onValueChange = { if (it.isEmpty() || it.toIntOrNull()?.let { value -> value in 0..maximum } == true) onChange(it) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
