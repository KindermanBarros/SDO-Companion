package com.kinderman.sdo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.R

@Composable
fun SdoCompactFilterBar(
    activeFilters: List<String>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SdoOutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.Button },
        ) {
            Text(stringResource(if (expanded) R.string.compact_filters_hide else R.string.compact_filters_show))
        }
        if (activeFilters.isNotEmpty()) {
            Text(activeFilters.joinToString(" · "))
            SdoTextButton(onClick = onClear, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.compact_filters_clear))
            }
        }
        if (expanded) content()
    }
}
