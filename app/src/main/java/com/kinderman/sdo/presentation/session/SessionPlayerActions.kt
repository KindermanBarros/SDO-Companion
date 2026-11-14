package com.kinderman.sdo.presentation.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
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
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ConditionKind
import com.kinderman.sdo.domain.model.ConditionPayload
import com.kinderman.sdo.domain.model.Duration
import com.kinderman.sdo.domain.model.DurationKind
import com.kinderman.sdo.domain.model.DurationTimeUnit
import com.kinderman.sdo.domain.model.EffectOperation
import com.kinderman.sdo.domain.model.InventoryState
import com.kinderman.sdo.domain.model.ItemInstanceId
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.TimedDuration
import com.kinderman.sdo.domain.model.inventoryState
import com.kinderman.sdo.domain.model.validInventoryStates
import com.kinderman.sdo.presentation.character.ChoiceField
import com.kinderman.sdo.presentation.character.IntegerField
import com.kinderman.sdo.ui.SdoCardTitle
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.SdoTextButton

@Composable
internal fun SessionInventoryActions(
    character: Character,
    readOnly: Boolean,
    onCommand: (Character, SessionCommand) -> Unit,
    onInventoryStateChange: (Character, String, InventoryState) -> Unit,
) {
    var pendingItemId by rememberSaveable(character.id) { mutableStateOf<String?>(null) }
    if (character.inventory.isEmpty()) Text(stringResource(R.string.session_inventory_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
    character.inventory.forEach { item ->
        val consumable = character.itemStates.firstOrNull { it.id.value == item.id }?.consumable
        SdoInsetCard(contentPadding = 8.dp, verticalSpacing = 4.dp) {
            SdoCardTitle(item.name, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleSmall)
            SdoMetadataText(item.inventoryState.label, style = MaterialTheme.typography.labelSmall)
            if (item.effect.isNotBlank()) Text(item.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            ChoiceField(
                label = stringResource(R.string.session_equipment_state),
                value = item.inventoryState,
                options = character.validInventoryStates(item),
                enabled = !readOnly,
                modifier = Modifier.testTag("session_item_state_${item.id}"),
                display = InventoryState::label,
            ) { next -> if (!readOnly) onInventoryStateChange(character, item.id, next) }
            if (consumable != null) {
                SdoTextButton(
                    onClick = { pendingItemId = item.id },
                    enabled = !readOnly && consumable.doses > 0,
                    modifier = Modifier.fillMaxWidth().testTag("session_consume_${item.id}"),
                ) { Text(stringResource(R.string.session_consume_dose, consumable.doses)) }
            }
        }
    }
    val item = character.inventory.firstOrNull { it.id == pendingItemId }
    if (item != null) {
        val doses = character.itemStates.firstOrNull { it.id.value == item.id }?.consumable?.doses ?: 0
        AlertDialog(
            onDismissRequest = { pendingItemId = null },
            title = { Text(stringResource(R.string.session_consume_title)) },
            text = { Text(stringResource(R.string.session_consume_confirmation, item.name)) },
            confirmButton = {
                SdoTextButton(
                    enabled = !readOnly && doses > 0,
                    modifier = Modifier.testTag("session_confirm_consume"),
                    onClick = {
                        pendingItemId = null
                        if (!readOnly && doses > 0) onCommand(
                            character,
                            SessionCommand(
                                type = SessionOperationType.EFFECT_OPERATION,
                                effectOperation = EffectOperation.ConsumeItemState(ItemInstanceId(item.id)),
                            ),
                        )
                    },
                ) { Text(stringResource(R.string.session_confirm)) }
            },
            dismissButton = {
                SdoTextButton(onClick = { pendingItemId = null }) { Text(stringResource(R.string.session_cancel)) }
            },
        )
    }
}

@Composable
internal fun SessionAddCondition(readOnly: Boolean, onAdd: (ConditionPayload) -> Unit) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    SdoTextButton(
        onClick = { showDialog = true },
        enabled = !readOnly,
        modifier = Modifier.fillMaxWidth().testTag("session_add_condition"),
    ) { Text(stringResource(R.string.session_add_condition)) }
    if (showDialog) {
        var kindName by rememberSaveable { mutableStateOf(ConditionKind.Abalado.name) }
        var durationName by rememberSaveable { mutableStateOf(DurationKind.Scene.name) }
        var intensity by rememberSaveable { mutableStateOf(0) }
        var amount by rememberSaveable { mutableStateOf(1) }
        val kind = ConditionKind.entries.first { it.name == kindName }
        val durationKind = DurationKind.entries.first { it.name == durationName }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.session_add_condition)) },
            modifier = Modifier.imePadding(),
            text = {
                Column(
                    Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChoiceField(stringResource(R.string.session_condition_kind), kind, ConditionKind.entries, !readOnly, display = { it.label }) { kindName = it.name }
                    ChoiceField(stringResource(R.string.session_condition_duration), durationKind, DurationKind.entries, !readOnly, display = { it.label }) { durationName = it.name }
                    IntegerField(stringResource(R.string.session_condition_intensity), intensity, !readOnly) { intensity = it.coerceAtLeast(0) }
                    if (durationKind == DurationKind.Turns || durationKind == DurationKind.Timed) {
                        IntegerField(
                            stringResource(if (durationKind == DurationKind.Turns) R.string.session_condition_turns else R.string.session_condition_hours),
                            amount, !readOnly,
                        ) { amount = it.coerceAtLeast(1) }
                    }
                }
            },
            confirmButton = {
                SdoTextButton(
                    enabled = !readOnly,
                    modifier = Modifier.testTag("session_confirm_condition"),
                    onClick = {
                        showDialog = false
                        if (!readOnly) onAdd(
                            ConditionPayload(
                                kind = kind,
                                intensity = intensity.takeIf { it > 0 },
                                duration = when (durationKind) {
                                    DurationKind.Turns -> Duration(durationKind, turns = amount)
                                    DurationKind.Timed -> Duration(durationKind, timed = TimedDuration(amount, DurationTimeUnit.Hours))
                                    else -> Duration(durationKind)
                                },
                            ),
                        )
                    },
                ) { Text(stringResource(R.string.session_confirm)) }
            },
            dismissButton = { SdoTextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.session_cancel)) } },
        )
    }
}
