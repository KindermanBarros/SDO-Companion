package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.TechCutDark

@Composable
internal fun CatalogPickerDialog(
    title: String,
    entries: List<CatalogEntry>,
    onDismiss: () -> Unit,
    onSelect: (CatalogEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(entries, query) {
        val needle = query.trim()
        if (needle.isEmpty()) entries else entries.filter {
            it.name.contains(needle, true) || it.group.contains(needle, true) || it.summary.contains(needle, true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HudTextField("Buscar no catálogo local", query) { query = it }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                    items(filtered, key = CatalogEntry::id) { entry ->
                        Column(
                            Modifier.fillMaxWidth().clickable { onSelect(entry) },
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(entry.name, color = Ice, style = MaterialTheme.typography.titleSmall)
                            Text(entry.group, color = Acid, style = MaterialTheme.typography.labelSmall)
                            Text(entry.summary, color = Muted, style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(color = TechCutDark)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}
