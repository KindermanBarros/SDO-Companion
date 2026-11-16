package com.kinderman.sdo.presentation.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.R
import com.kinderman.sdo.domain.model.CharacterConflictField
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.SheetConflictItem
import com.kinderman.sdo.domain.model.SheetIntent
import com.kinderman.sdo.ui.SdoField
import com.kinderman.sdo.ui.SdoButton
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoOutlinedButton
import com.kinderman.sdo.ui.SdoTextButton

@Composable
fun CharacterConflictDialog(
    conflict: CharacterSyncConflict,
    onResolve: (Set<String>) -> Unit,
) {
    var remoteFieldIds by remember(conflict.local.id, conflict.remoteUpdatedAt) {
        mutableStateOf(emptySet<String>())
    }

    AlertDialog(
        onDismissRequest = {},
        containerColor = MaterialTheme.colorScheme.background,
        shape = CutCornerShape(topEnd = 24.dp, bottomStart = 16.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.conflict_dialog_title), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
                Text(conflict.local.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.conflict_dialog_character_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SdoTextButton(
                        onClick = { remoteFieldIds = emptySet() },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.conflict_dialog_all_local)) }
                    SdoTextButton(
                        onClick = { remoteFieldIds = conflict.fields.mapTo(mutableSetOf()) { it.id } },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.conflict_dialog_all_online)) }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    conflict.fields.forEach { field ->
                        ConflictFieldChoice(
                            field = field,
                            useRemote = field.id in remoteFieldIds,
                            onUseRemote = { useRemote ->
                                remoteFieldIds = if (useRemote) {
                                    remoteFieldIds + field.id
                                } else {
                                    remoteFieldIds - field.id
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            SdoButton(
                onClick = { onResolve(remoteFieldIds) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
            ) {
                Text(stringResource(R.string.conflict_dialog_apply_choices))
            }
        },
    )
}

@Composable
fun CharacterConflictDialog(
    conflict: SheetConflictItem,
    characterName: String = "",
    onResolve: (useRemote: Boolean, customValueJson: String?) -> Unit,
    onDiscard: () -> Unit,
) {
    var useRemote by rememberSaveable(conflict.id, conflict.remoteRevision) { mutableStateOf(false) }
    var manual by rememberSaveable(conflict.id, conflict.remoteRevision) { mutableStateOf(false) }
    val editor = remember(conflict.id, conflict.remoteRevision) { ManualConflictValue.from(conflict) }
    var values by rememberSaveable(conflict.id, conflict.remoteRevision) {
        mutableStateOf(editor?.initialValues.orEmpty())
    }
    val customValueJson = editor?.encode(values)
    val emptyValue = stringResource(R.string.conflict_dialog_empty_value)

    AlertDialog(
        modifier = Modifier.imePadding().testTag("conflict_dialog"),
        onDismissRequest = {},
        containerColor = MaterialTheme.colorScheme.background,
        shape = CutCornerShape(topEnd = 24.dp, bottomStart = 16.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.conflict_dialog_title),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = listOf(characterName.ifBlank { conflict.sheetId }, conflict.label)
                        .filter(String::isNotBlank)
                        .joinToString(stringResource(R.string.conflict_dialog_title_separator)),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.conflict_dialog_field_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                SdoInsetCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MaterialTheme.colorScheme.outlineVariant,
                    contentPadding = 10.dp,
                    verticalSpacing = 4.dp,
                ) {
                    Text(
                        stringResource(R.string.conflict_dialog_base_label),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        conflict.baseSummary.ifBlank { emptyValue },
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                VersionChoice(
                    label = stringResource(R.string.conflict_dialog_local_device_label),
                    summary = conflict.localSummary.ifBlank { emptyValue },
                    selected = !useRemote && !manual,
                    onClick = { useRemote = false; manual = false },
                    modifier = Modifier.testTag("conflict_choose_local"),
                )
                VersionChoice(
                    label = stringResource(R.string.conflict_dialog_online_cloud_label),
                    summary = conflict.remoteSummary.ifBlank { emptyValue },
                    selected = useRemote && !manual,
                    onClick = { useRemote = true; manual = false },
                    modifier = Modifier.testTag("conflict_choose_remote"),
                )
                VersionChoice(
                    label = stringResource(R.string.conflict_dialog_manual_label),
                    summary = stringResource(if (editor != null) R.string.conflict_dialog_manual_hint
                        else R.string.conflict_dialog_manual_unsupported),
                    selected = manual,
                    onClick = { manual = true },
                    enabled = editor != null,
                    modifier = Modifier.testTag("conflict_choose_manual"),
                )
                if (manual && editor != null) {
                    if (editor.kind == ManualConflictValue.Kind.BOOLEAN) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(true, false).forEach { choice ->
                                SdoOutlinedButton(
                                    onClick = { values = listOf(choice.toString()) },
                                    modifier = Modifier.weight(1f).testTag("conflict_manual_" + choice),
                                ) { Text(stringResource(if (choice) R.string.conflict_dialog_manual_yes
                                    else R.string.conflict_dialog_manual_no)) }
                            }
                        }
                    } else {
                        val labels = if (editor.kind == ManualConflictValue.Kind.RESOURCE) listOf(
                            R.string.conflict_dialog_manual_current, R.string.conflict_dialog_manual_maximum,
                            R.string.conflict_dialog_manual_adjustment,
                        ) else listOf(R.string.conflict_dialog_manual_value)
                        labels.forEachIndexed { index, label ->
                            SdoField(
                                label = stringResource(label),
                                value = values[index],
                                onValueChange = { next -> values = values.toMutableList().apply { set(index, next) } },
                                modifier = Modifier.fillMaxWidth().testTag("conflict_manual_value_" + index),
                                multiline = editor.kind == ManualConflictValue.Kind.TEXT,
                                keyboardOptions = KeyboardOptions(keyboardType = when (editor.kind) {
                                    ManualConflictValue.Kind.INTEGER, ManualConflictValue.Kind.RESOURCE -> KeyboardType.Number
                                    ManualConflictValue.Kind.DECIMAL -> KeyboardType.Decimal
                                    else -> KeyboardType.Text
                                }),
                            )
                        }
                    }
                    if (customValueJson == null) Text(
                        stringResource(R.string.conflict_dialog_manual_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            SdoButton(
                onClick = { onResolve(if (manual) false else useRemote, if (manual) customValueJson else null) },
                enabled = !manual || customValueJson != null,
                modifier = Modifier.testTag("conflict_apply"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
            ) {
                Text(stringResource(R.string.conflict_dialog_apply_choice))
            }
        },
        dismissButton = {
            SdoOutlinedButton(
                onClick = onDiscard,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
            ) {
                Text(stringResource(R.string.conflict_dialog_discard))
            }
        },
    )
}

@Composable
fun AccessDeniedDraftDialog(
    intent: SheetIntent,
    characterName: String = "",
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = CutCornerShape(topEnd = 24.dp, bottomStart = 16.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.access_denied_dialog_title),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = characterName.ifBlank { intent.sheetId },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.access_denied_dialog_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (intent.baseValueJson.isNotBlank() || intent.desiredPayloadJson.isNotBlank()) {
                    SdoInsetCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = 10.dp,
                        verticalSpacing = 6.dp,
                    ) {
                        if (intent.baseValueJson.isNotBlank()) {
                            Text(
                                stringResource(R.string.access_denied_dialog_previous_base),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                intent.baseValueJson,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (intent.desiredPayloadJson.isNotBlank()) {
                            Text(
                                stringResource(R.string.access_denied_dialog_local_preserved),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                intent.desiredPayloadJson,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            SdoButton(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
            ) {
                Text(stringResource(R.string.access_denied_dialog_retry))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SdoTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.access_denied_dialog_close))
                }
                SdoOutlinedButton(
                    onClick = onDiscard,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
                ) {
                    Text(stringResource(R.string.conflict_dialog_discard))
                }
            }
        },
    )
}

@Composable
private fun ConflictFieldChoice(
    field: CharacterConflictField,
    useRemote: Boolean,
    onUseRemote: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CutCornerShape(topEnd = 12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(field.label.uppercase(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
        VersionChoice(
            label = stringResource(R.string.conflict_dialog_local_label),
            summary = field.localSummary,
            selected = !useRemote,
            onClick = { onUseRemote(false) },
        )
        VersionChoice(
            label = stringResource(R.string.conflict_dialog_online_label),
            summary = field.remoteSummary,
            selected = useRemote,
            onClick = { onUseRemote(true) },
        )
    }
}

@Composable
private fun VersionChoice(
    label: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SdoOutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = CutCornerShape(topEnd = 9.dp, bottomStart = 9.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(
                summary,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
