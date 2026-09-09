package com.kinderman.sdo.presentation.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.CharacterConflictField
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.Carbon
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.LabelFunctional
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechCutDark
import com.kinderman.sdo.ui.Void

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
                Text("CONFLITO DE SINCRONIA", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
                Text(conflict.local.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "A ficha foi alterada neste aparelho e também online. Escolha qual versão fica em cada campo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { remoteFieldIds = emptySet() },
                        modifier = Modifier.weight(1f),
                    ) { Text("TUDO LOCAL") }
                    TextButton(
                        onClick = { remoteFieldIds = conflict.fields.mapTo(mutableSetOf()) { it.id } },
                        modifier = Modifier.weight(1f),
                    ) { Text("TUDO ONLINE") }
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
            Button(
                onClick = { onResolve(remoteFieldIds) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
            ) {
                Text("APLICAR ESCOLHAS")
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
            label = "LOCAL",
            summary = field.localSummary,
            selected = !useRemote,
            onClick = { onUseRemote(false) },
        )
        VersionChoice(
            label = "ONLINE",
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
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
