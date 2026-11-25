package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.DivinePillarKind
import com.kinderman.sdo.domain.model.DivineFate
import com.kinderman.sdo.domain.model.DivineLink
import com.kinderman.sdo.domain.model.DivineLinkKind
import com.kinderman.sdo.domain.model.changePermanentDivinePillar
import com.kinderman.sdo.domain.model.derivedDivineEffects
import com.kinderman.sdo.domain.model.divineLinkCapacity
import com.kinderman.sdo.domain.model.divineLinkLoad
import com.kinderman.sdo.domain.model.divineLinkOverloadCost
import com.kinderman.sdo.domain.model.restoreDivinePillar
import com.kinderman.sdo.domain.model.settleDivineLinks
import com.kinderman.sdo.domain.model.suppressDivinePillar
import com.kinderman.sdo.domain.model.withDivineFate
import com.kinderman.sdo.domain.model.withDivineLink
import com.kinderman.sdo.domain.model.withRemovedDivineLink
import com.kinderman.sdo.domain.model.currentDivinePillars
import com.kinderman.sdo.ui.SdoCard
import com.kinderman.sdo.ui.SdoField
import com.kinderman.sdo.ui.SdoFilterChip
import com.kinderman.sdo.ui.SdoSwitch

private enum class PowerDialog { LINK, SUPPRESSION, RECOVERY, MILESTONE, SEAL }

@Composable
private fun PowerField(label: Int, value: String, tag: String, onValue: (String) -> Unit) {
    SdoField(
        label = stringResource(label),
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth().testTag("divine_power_$tag"),
        multiline = true,
    )
}

@Composable
private fun PowerPillarPicker(selected: DivinePillarKind, onSelect: (DivinePillarKind) -> Unit) {
    DivinePillarKind.entries.forEach { pillar ->
        SdoFilterChip(
            label = pillar.label,
            selected = selected == pillar,
            onClick = { onSelect(pillar) },
            modifier = Modifier.fillMaxWidth().testTag("divine_power_select_${pillar.name}"),
        )
    }
}

@Composable
private fun PowerLinkEditor(
    onDismiss: () -> Unit,
    error: Boolean,
    onSave: (String, Boolean) -> Unit,
) {
    var target by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableStateOf(false) }
    DivineEditorDialog(
        title = R.string.divine_power_add_link,
        onDismiss = onDismiss,
        canApply = target.isNotBlank(),
        onApply = { onSave(target.trim(), avatar) },
    ) {
        if (error) PowerError()
        PowerField(R.string.divine_power_target, target, "link_target") { target = it }
        listOf(false, true).forEach { isAvatar ->
            SdoFilterChip(
                label = stringResource(
                    if (isAvatar) R.string.divine_power_avatar else R.string.divine_power_anchor,
                ),
                selected = avatar == isAvatar,
                onClick = { avatar = isAvatar },
                modifier = Modifier.fillMaxWidth().testTag("divine_power_link_avatar_$isAvatar"),
            )
        }
        Text(stringResource(R.string.divine_power_capacity_rule))
    }
}

@Composable
private fun PowerSuppressionEditor(
    onDismiss: () -> Unit,
    error: Boolean,
    onSave: (DivinePillarKind, Int, String, Boolean) -> Unit,
) {
    var pillar by rememberSaveable { mutableStateOf(DivinePillarKind.ASPECTO) }
    var amount by rememberSaveable { mutableStateOf(1) }
    var sceneOnly by rememberSaveable { mutableStateOf(false) }
    var cause by rememberSaveable { mutableStateOf("") }
    DivineEditorDialog(
        title = R.string.divine_power_suppression,
        onDismiss = onDismiss,
        canApply = cause.isNotBlank(),
        onApply = { onSave(pillar, amount, cause.trim(), sceneOnly) },
    ) {
        if (error) PowerError()
        Text(stringResource(R.string.divine_power_suppression_rule))
        PowerPillarPicker(pillar) { pillar = it }
        listOf(1, 2).forEach { severity ->
            SdoFilterChip(
                label = stringResource(R.string.divine_power_severity, severity),
                selected = amount == severity,
                onClick = { amount = severity },
                modifier = Modifier.fillMaxWidth().testTag("divine_power_severity_$severity"),
            )
        }
        PowerField(R.string.divine_power_cause, cause, "cause") { cause = it }
        Column(Modifier.testTag("divine_power_scene_only")) {
            Text(stringResource(R.string.divine_power_scene_only))
            SdoSwitch(sceneOnly, { sceneOnly = it })
        }
    }
}

@Composable
private fun PowerMilestoneEditor(
    onDismiss: () -> Unit,
    error: Boolean,
    onSave: (DivinePillarKind, Int, String, String) -> Unit,
) {
    var pillar by rememberSaveable { mutableStateOf(DivinePillarKind.ASPECTO) }
    var delta by rememberSaveable { mutableStateOf(1) }
    var arc by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    DivineEditorDialog(
        title = R.string.divine_power_milestone,
        onDismiss = onDismiss,
        canApply = arc.isNotBlank() && reason.isNotBlank(),
        onApply = { onSave(pillar, delta, arc.trim(), reason.trim()) },
    ) {
        if (error) PowerError()
        Text(stringResource(R.string.divine_power_growth_rule))
        PowerPillarPicker(pillar) { pillar = it }
        listOf(1, -1).forEach { change ->
            SdoFilterChip(
                label = stringResource(
                    if (change > 0) R.string.divine_power_growth else R.string.divine_power_loss,
                ),
                selected = delta == change,
                onClick = { delta = change },
                modifier = Modifier.fillMaxWidth().testTag("divine_power_milestone_$change"),
            )
        }
        PowerField(R.string.divine_power_arc, arc, "arc") { arc = it }
        PowerField(R.string.divine_power_milestone_reason, reason, "milestone_reason") { reason = it }
    }
}

@Composable
private fun PowerRecoveryEditor(
    onDismiss: () -> Unit,
    session: String,
    onSessionChange: (String) -> Unit,
    error: Boolean,
    onSave: (DivinePillarKind, String, String) -> Unit,
) {
    var pillar by rememberSaveable { mutableStateOf(DivinePillarKind.ASPECTO) }
    var scene by rememberSaveable { mutableStateOf("") }
    var causeResolved by rememberSaveable { mutableStateOf(false) }
    DivineEditorDialog(
        title = R.string.divine_power_recover,
        onDismiss = onDismiss,
        canApply = session.isNotBlank() && scene.isNotBlank() && causeResolved,
        onApply = { onSave(pillar, session.trim(), scene.trim()) },
    ) {
        if (error) PowerError()
        Text(stringResource(R.string.divine_power_recovery_rule))
        PowerPillarPicker(pillar) { pillar = it }
        PowerField(R.string.divine_power_session, session, "session", onSessionChange)
        PowerField(R.string.divine_power_restoration, scene, "restoration") { scene = it }
        Column(Modifier.testTag("divine_power_cause_resolved")) {
            Text(stringResource(R.string.divine_power_cause_resolved))
            SdoSwitch(causeResolved, { causeResolved = it })
        }
    }
}



@Composable
internal fun CharacterDivinePowerSection(
    character: Character,
    editable: Boolean,
    canChangeAscension: Boolean,
    onChange: (Character) -> Unit,
) {
    if (!character.derivedDivineEffects().usesConceptualPillars) return
    val canEdit = editable && canChangeAscension
    val runtime = character.divineRuntime
    val current = character.currentDivinePillars()
    var dialog by rememberSaveable(character.id) { mutableStateOf<PowerDialog?>(null) }
    var scene by rememberSaveable(character.id) { mutableStateOf("") }
    var session by rememberSaveable(character.id) { mutableStateOf("") }
    var error by rememberSaveable(character.id) { mutableStateOf(false) }
    val dismiss = { dialog = null; error = false }
    val change: ((Character) -> Character) -> Unit = { transform ->
        if (canEdit) {
            val next = transform(character)
            error = next == character
            if (!error) {
                onChange(next)
                dialog = null
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().testTag("character_divine_power_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.divine_power_title), style = MaterialTheme.typography.titleMedium)
        DivinePillarKind.entries.forEach { pillar ->
            Text(stringResource(
                R.string.divine_power_pillar, pillar.label,
                current.get(pillar), character.divineAscension.pillars.get(pillar),
            ))
        }
        Text(stringResource(
            R.string.divine_power_capacity, character.divineLinkLoad(), character.divineLinkCapacity(),
        ), modifier = Modifier.testTag("divine_power_capacity"))
        Text(stringResource(R.string.divine_power_capacity_rule))
        Text(stringResource(R.string.divine_power_overload, character.divineLinkOverloadCost()))
        Text(stringResource(powerFateLabel(runtime.divineFate)))
        if (runtime.links.isEmpty()) Text(stringResource(R.string.divine_power_no_links))
        runtime.links.forEach { link ->
            SdoCard(modifier = Modifier.fillMaxWidth().testTag("divine_power_link_${link.id}")) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(
                        R.string.divine_power_link, link.id,
                        stringResource(if (link.kind == DivineLinkKind.AVATAR)
                            R.string.divine_power_avatar else R.string.divine_power_anchor),
                        link.kind.capacityCost,
                        stringResource(if (link.active)
                            R.string.divine_power_active else R.string.divine_power_suspended),
                    ))
                    if (canEdit) {
                        DivineAction(
                            if (link.active) R.string.divine_power_suspend else R.string.divine_power_reactivate,
                            "power_toggle_${link.id}", scene.isNotBlank(),
                        ) { change { it.withDivineLink(link.copy(active = !link.active), scene.trim()) } }
                        DivineAction(R.string.divine_power_release, "power_release_${link.id}") {
                            change { it.withRemovedDivineLink(link.id) }
                        }
                    }
                }
            }
        }
        if (canEdit) {
            PowerField(R.string.divine_power_scene, scene, "scene") { scene = it }
            DivineAction(R.string.divine_power_start_scene, "power_settle", scene.isNotBlank()) {
                change { it.settleDivineLinks(scene.trim()) }
            }
            DivineAction(R.string.divine_power_add_link, "power_add_link", scene.isNotBlank()) {
                error = false; dialog = PowerDialog.LINK
            }
            DivineAction(R.string.divine_power_suppression, "power_suppress", scene.isNotBlank()) {
                error = false; dialog = PowerDialog.SUPPRESSION
            }
            DivineAction(R.string.divine_power_recover, "power_restore",
                DivinePillarKind.entries.any { runtime.pillarSuppression.get(it) > 0 },
            ) { error = false; dialog = PowerDialog.RECOVERY }
            DivineAction(R.string.divine_power_milestone, "power_milestone", scene.isNotBlank()) {
                error = false; dialog = PowerDialog.MILESTONE
            }
            DivineAction(R.string.divine_power_fate_title, "power_fate") {
                error = false; dialog = PowerDialog.SEAL
            }
        }
        if (error && dialog == null) PowerError()
    }
    if (!canEdit) return
    // Keep rejected edits and their drafts visible rather than silently discarding input.
    when (dialog) {
        PowerDialog.LINK -> PowerLinkEditor(dismiss, error) { id, avatar ->
            change { it.withDivineLink(
                DivineLink(id, if (avatar) DivineLinkKind.AVATAR else DivineLinkKind.ANCHOR),
                scene.trim(),
            ) }
        }
        PowerDialog.SUPPRESSION -> PowerSuppressionEditor(dismiss, error) { pillar, amount, cause, sceneOnly ->
            change { it.suppressDivinePillar(pillar, amount, cause, scene.trim(), sceneOnly = sceneOnly) }
        }
        PowerDialog.RECOVERY -> PowerRecoveryEditor(dismiss, session, { session = it }, error) {
            pillar, sessionId, restoration ->
            change { it.restoreDivinePillar(pillar, restoration, sessionId, causeResolved = true) }
        }
        PowerDialog.MILESTONE -> PowerMilestoneEditor(dismiss, error) { pillar, delta, arc, reason ->
            change { it.changePermanentDivinePillar(pillar, delta, arc, reason, scene.trim()) }
        }
        PowerDialog.SEAL -> PowerSealEditor(dismiss, error) { fate, cause, vulnerability ->
            change { it.withDivineFate(fate, cause, vulnerability) }
        }
        null -> Unit
    }
}

@Composable
private fun PowerError() {
    Text(stringResource(R.string.divine_power_error), color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("divine_power_error"))
}

@Composable
private fun PowerSealEditor(
    onDismiss: () -> Unit,
    error: Boolean,
    onSave: (DivineFate, String, String) -> Unit,
) {
    var fate by rememberSaveable { mutableStateOf(DivineFate.SEALED) }
    var cause by rememberSaveable { mutableStateOf("") }
    var vulnerability by rememberSaveable { mutableStateOf("") }
    DivineEditorDialog(
        title = R.string.divine_power_fate_title,
        onDismiss = onDismiss,
        canApply = cause.isNotBlank() &&
            (fate != DivineFate.SEALED || vulnerability.isNotBlank()),
        onApply = { onSave(fate, cause.trim(), vulnerability.trim()) },
    ) {
        listOf(DivineFate.ACTIVE, DivineFate.DEFEATED, DivineFate.SEALED).forEach { choice ->
            SdoFilterChip(
                label = stringResource(powerFateLabel(choice)),
                selected = fate == choice,
                onClick = { fate = choice },
                modifier = Modifier.fillMaxWidth().testTag("divine_power_fate_${choice.name}"),
            )
        }
        PowerField(R.string.divine_power_cause, cause, "fate_cause") { cause = it }
        if (fate == DivineFate.SEALED) {
            PowerField(R.string.divine_power_seal_reason, vulnerability, "seal_reason") {
                vulnerability = it
            }
        }
        Text(stringResource(R.string.divine_power_erasure_rule))
        if (error) PowerError()
    }
}

private fun powerFateLabel(fate: DivineFate): Int = when (fate) {
    DivineFate.ACTIVE -> R.string.divine_power_unsealed
    DivineFate.DEFEATED -> R.string.divine_power_defeated
    DivineFate.SEALED -> R.string.divine_power_sealed
    DivineFate.ERASED -> R.string.divine_power_erased
}
