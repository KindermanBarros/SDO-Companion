package com.kinderman.sdo.presentation.dashboard

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Panel
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechCutDark
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag

@Composable
internal fun OwnerPickerDialog(
    character: Character,
    owners: List<UserProfile>,
    onDismiss: () -> Unit,
    onSelect: (UserProfile) -> Unit,
) {
    var query by rememberSaveable(character.id) { mutableStateOf("") }
    val filteredOwners = owners.filter { owner ->
        query.isBlank() || listOf(owner.firstName, owner.displayName, owner.email)
            .any { it.contains(query.trim(), ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            TechPanel(
                modifier = Modifier.widthIn(max = 520.dp),
                accent = MaterialTheme.colorScheme.primary,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TelemetryTag("OWNER.ROUTE")
                    TelemetryTag("${owners.size.toString().padStart(2, '0')} OPERADORES", MaterialTheme.colorScheme.error)
                }
                SectionHeader("ID", "Transferir ${character.name}")
                Text(
                    "SELECIONE O NOVO RESPONSÁVEL PELA FICHA",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
                HudTextField(
                    label = "Buscar por nome ou e-mail",
                    value = query,
                    onValue = { query = it },
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (filteredOwners.isEmpty()) {
                        item("empty-owner-search") {
                            Text(
                                "NO_SIGNAL // NENHUM OPERADOR ENCONTRADO",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(vertical = 18.dp),
                            )
                        }
                    }
                    items(filteredOwners, key = UserProfile::uid) { owner ->
                        OwnerOption(
                            owner = owner,
                            selected = owner.uid == character.ownerId,
                            onClick = { if (owner.uid != character.ownerId) onSelect(owner) },
                        )
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
                ) {
                    Text("CANCELAR // FECHAR ROTA", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun OwnerOption(
    owner: UserProfile,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent, CutCornerShape(topEnd = 14.dp, bottomStart = 9.dp)),
        shape = CutCornerShape(topEnd = 14.dp, bottomStart = 9.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(owner.firstName.uppercase(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                Text(owner.email, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text("UID.${owner.uid.take(8)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            TelemetryTag(if (selected) "OWNER.ATUAL" else owner.role.name)
        }
    }
}
