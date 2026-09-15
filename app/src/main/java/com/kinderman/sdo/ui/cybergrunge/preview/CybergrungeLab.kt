package com.kinderman.sdo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** In-app component lab so compact Android states can be reviewed without a separate debug build. */
@Composable
fun CyberGrungeLab(modifier: Modifier = Modifier) {
    var field by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf("FREQUÊNCIA A") }
    var enabled by remember { mutableStateOf(true) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SdoScreenMasthead("LAB//CG", "LABORATÓRIO VISUAL", "COMPACT VIEW // INTERACTION STATES")
        TechPanel(accent = MaterialTheme.colorScheme.primary) {
            SectionHeader("01", "Ações")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SdoActionButton("Primária", {}, Modifier.weight(1f), style = SdoActionStyle.PRIMARY)
                SdoActionButton("Secundária", {}, Modifier.weight(1f))
            }
            SdoActionButton("Desabilitada", {}, Modifier.fillMaxWidth(), enabled = false)
        }
        TechPanel(accent = CyberGrungeTokens.TerminalGreen) {
            SectionHeader("02", "Campos")
            SdoField("CAMPO VAZIO", field, { field = it })
            SdoField("CAMPO BLOQUEADO", "DADO PRESERVADO", {}, enabled = false)
        }
        SdoChoicePanel(
            title = "CAPTURA DE FREQUÊNCIA",
            options = listOf("FREQUÊNCIA A", "FREQUÊNCIA B"), selected = selected,
            label = { it }, description = { if (it == selected) "Sinal capturado" else "Sinal disponível" },
            onSelect = { selected = it },
        )
        SdoTogglePanel("ESTADOS", listOf(SdoToggleOption("Canal ativo", enabled) { enabled = it }))
        SdoOfflineState("SEM CONEXÃO", "A cópia local permanece disponível para edição.")
        SdoErrorState("SINAL CORROMPIDO", "Revise os campos destacados antes de continuar.")
        SdoEmptyState("ARQUIVO VAZIO", "Nenhum registro corresponde aos filtros atuais.")
    }
}
