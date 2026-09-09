package com.kinderman.sdo.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.MainActivity
import com.kinderman.sdo.presentation.AuthUiState
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.Barcode
import com.kinderman.sdo.ui.ComplianceMark
import com.kinderman.sdo.ui.Grid
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag

@Composable
fun LoginScreen(
    activity: MainActivity,
    state: AuthUiState,
    allowDemo: Boolean,
    onGoogleLogin: () -> Unit,
    onDemo: () -> Unit,
) {
    HudBackground {
        Column(
            Modifier.align(Alignment.Center).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TelemetryTag("SYS.26 // ONLINE")
                TelemetryTag("AUTH_GATE", MaterialTheme.colorScheme.error)
            }
            TechPanel(accent = MaterialTheme.colorScheme.primary) {
                Text("SOLIDÃO DOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text("OPRIMIDOS", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.displayLarge)
                Text("COMPANION // TERMINAL DE PERSONAGEM", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Barcode("SDO-COMPANION-AUTH")
                Text("Identifique-se para acessar arquivos locais e sincronizar a ficha.", color = MaterialTheme.colorScheme.onSurface)
                state.error?.let {
                    Box(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.error).background(MaterialTheme.colorScheme.error.copy(alpha = .12f)).padding(10.dp)) {
                        Text("ERR_AUTH // $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(
                    onClick = onGoogleLogin,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled = !state.loading,
                    shape = CutCornerShape(topEnd = 15.dp, bottomStart = 15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onSurface, strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Default.AccountCircle, null)
                        Spacer(Modifier.size(9.dp))
                        Text("INICIAR COM GOOGLE", style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (allowDemo) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    OutlinedButton(
                        onClick = onDemo,
                        modifier = Modifier.fillMaxWidth(),
                        shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    ) {
                        Text("MODO LOCAL // OFFLINE")
                    }
                }
                ComplianceMark()
            }
        }
    }
}
