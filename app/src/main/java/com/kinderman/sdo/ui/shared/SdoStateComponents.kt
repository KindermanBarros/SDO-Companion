package com.kinderman.sdo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp

enum class SdoStateTone { NEUTRAL, OFFLINE, ERROR }

@Composable
fun SdoEmptyState(title: String, message: String, modifier: Modifier = Modifier) =
    SdoStatePanel(title, message, "SEM DADOS", SdoStateTone.NEUTRAL, modifier)

@Composable
fun SdoErrorState(title: String, message: String, modifier: Modifier = Modifier) =
    SdoStatePanel(title, message, "AÇÃO NECESSÁRIA", SdoStateTone.ERROR, modifier)

@Composable
fun SdoOfflineState(title: String, message: String, modifier: Modifier = Modifier) =
    SdoStatePanel(title, message, "DADOS LOCAIS PRESERVADOS", SdoStateTone.OFFLINE, modifier)

@Composable
private fun SdoStatePanel(title: String, message: String, status: String, tone: SdoStateTone, modifier: Modifier) {
    val accent = when (tone) {
        SdoStateTone.NEUTRAL -> MaterialTheme.colorScheme.outline
        SdoStateTone.OFFLINE -> MaterialTheme.colorScheme.tertiary
        SdoStateTone.ERROR -> MaterialTheme.colorScheme.error
    }
    TechPanel(modifier, accent) {
        TelemetryTag(status, accent)
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@PreviewLightDark
@Composable
private fun SdoStateComponentsPreview() {
    SdoTheme {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            SdoEmptyState("VAZIO", "Nenhum registro encontrado.")
            SdoOfflineState("OFFLINE", "Alterações salvas localmente.")
            SdoErrorState("ERRO", "Falha na sincronização.")
        }
    }
}
