package com.kinderman.sdo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class SdoNavigationItem(
    val label: String,
    val selected: Boolean,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun SdoNavigationRail(items: List<SdoNavigationItem>, modifier: Modifier = Modifier) {
    if (LocalSdoPreferences.current.visualMode != SdoVisualMode.CYBERGRUNGE) {
        NavigationRail(modifier) {
            items.forEach { item ->
                NavigationRailItem(
                    selected = item.selected, onClick = item.onClick,
                    icon = { Icon(item.icon, null) }, label = { Text(item.label) },
                )
            }
        }
        return
    }
    Column(
        modifier.fillMaxHeight().width(136.dp)
            .background(CyberGrungeTokens.Void)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .7f))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .verticalScroll(rememberScrollState())
            .selectableGroup()
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("SDO//OS", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(7.dp))
        items.forEachIndexed { index, item ->
            val accent = if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .border(1.dp, accent, CutCornerShape(topEnd = 9.dp, bottomStart = 9.dp))
                    .clip(CutCornerShape(topEnd = 9.dp, bottomStart = 9.dp))
                    .background(if (item.selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f) else CyberGrungeTokens.Panel)
                    .selectable(selected = item.selected, role = Role.Tab, onClick = item.onClick)
                    .padding(horizontal = 9.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(if (item.selected) "▶ 0${index + 1}" else "0${index + 1}", color = accent, style = MaterialTheme.typography.labelSmall)
                    Text(item.label.uppercase(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SdoNavigationBar(items: List<SdoNavigationItem>, modifier: Modifier = Modifier) {
    if (LocalSdoPreferences.current.visualMode != SdoVisualMode.CYBERGRUNGE) {
        NavigationBar(modifier) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = item.selected, onClick = item.onClick,
                    icon = { Icon(item.icon, null) }, label = { Text(item.label) },
                )
            }
        }
        return
    }
    Row(
        modifier.fillMaxWidth().background(CyberGrungeTokens.Void)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .7f))
            .navigationBarsPadding()
            .selectableGroup()
            .padding(horizontal = 5.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items.forEachIndexed { index, item ->
            val shape = CutCornerShape(topEnd = 9.dp, bottomStart = 9.dp)
            val accent = if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Column(
                Modifier.weight(1f).heightIn(min = 64.dp).border(1.dp, accent, shape).clip(shape)
                    .background(if (item.selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f) else CyberGrungeTokens.Panel)
                    .selectable(selected = item.selected, role = Role.Tab, onClick = item.onClick)
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (item.selected) "▶ 0${index + 1}" else "0${index + 1}", color = accent, style = MaterialTheme.typography.labelSmall)
                Text(
                    item.label.uppercase(),
                    color = if (item.selected) accent else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
