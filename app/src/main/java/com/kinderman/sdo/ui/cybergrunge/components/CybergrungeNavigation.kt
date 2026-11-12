package com.kinderman.sdo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        modifier.fillMaxHeight().widthIn(min = 104.dp)
            .background(CyberGrungeTokens.Void)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .7f))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("SDO//OS", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(7.dp))
        items.forEachIndexed { index, item ->
            val accent = if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Row(
                Modifier.border(1.dp, accent, CutCornerShape(topEnd = 9.dp, bottomStart = 9.dp))
                    .clip(CutCornerShape(topEnd = 9.dp, bottomStart = 9.dp))
                    .background(if (item.selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f) else CyberGrungeTokens.Panel)
                    .clickable(onClick = item.onClick).padding(horizontal = 9.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("0${index + 1}", color = accent, style = MaterialTheme.typography.labelSmall)
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
            .padding(horizontal = 5.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items.forEachIndexed { index, item ->
            val shape = CutCornerShape(topEnd = 9.dp, bottomStart = 9.dp)
            val accent = if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Column(
                Modifier.weight(1f).border(1.dp, accent, shape).clip(shape)
                    .background(if (item.selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f) else CyberGrungeTokens.Panel)
                    .clickable(onClick = item.onClick).padding(horizontal = 4.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("0${index + 1}", color = accent, style = MaterialTheme.typography.labelSmall)
                AdaptiveSingleLineText(
                    item.label.uppercase(),
                    color = if (item.selected) accent else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                    minimumSize = 7.sp,
                )
            }
        }
    }
}
