package com.kinderman.sdo.presentation.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.BodyRegion
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.localProtectionBreakdown
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.AcidCyan
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SessionModeScreen(
    characters: List<Character>,
    selectedId: String?,
    compact: Boolean,
    readOnly: Boolean,
    onSelect: (String) -> Unit,
    onOpenSheet: (String) -> Unit,
    onChange: (Character) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val character = characters.firstOrNull { it.id == selectedId }
    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(if (character == null) "MODO SESSÃO" else character.name.uppercase()) },
                    navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                    actions = {
                        if (character != null) IconButton({ onOpenSheet(character.id) }) {
                            Icon(Icons.Default.EditNote, "Abrir ficha completa")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
        ) { padding ->
            if (character == null) {
                CharacterSelector(characters, Modifier.padding(padding), onSelect)
            } else {
                SessionContent(character, compact, readOnly, Modifier.padding(padding), onChange)
            }
        }
    }
}

@Composable
private fun CharacterSelector(characters: List<Character>, modifier: Modifier, onSelect: (String) -> Unit) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TechPanel {
                TelemetryTag("SELECT_OPERATIVE")
                Text("Escolha uma ficha para a sessão", color = Ice, style = MaterialTheme.typography.titleLarge)
                Text("Os ajustes rápidos usam a mesma ficha offline-first.", color = Muted)
            }
        }
        items(characters, key = Character::id) { character ->
            OutlinedButton(onClick = { onSelect(character.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Text(character.name.ifBlank { "Personagem sem nome" })
                    Text(
                        "VIDA ${character.life.current}/${character.lifeMaximum}  //  ${if (character.dirty) "LOCAL_DELTA" else "SYNC_OK"}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        if (characters.isEmpty()) item { Text("Nenhuma ficha disponível.", color = Muted) }
    }
}

@Composable
private fun SessionContent(
    character: Character,
    compact: Boolean,
    readOnly: Boolean,
    modifier: Modifier,
    onChange: (Character) -> Unit,
) {
    var damageDialog by remember { mutableStateOf(false) }
    var healingDialog by remember { mutableStateOf(false) }
    val spacing = if (compact) 8.dp else 14.dp
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(if (compact) 12.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        item {
            TechPanel(accent = if (character.dirty) Signal else Acid) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TelemetryTag("SESSION.MODE")
                    TelemetryTag(
                        when {
                            readOnly -> "ARCHIVE.READ_ONLY"
                            character.dirty -> "OFFLINE_READY"
                            else -> "SYNC_OK"
                        },
                    )
                }
                Text("AÇÕES DE COMBATE", color = Ice, style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { damageDialog = true }, enabled = !readOnly, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Shield, null)
                        Text(" DANO")
                    }
                    OutlinedButton(onClick = { healingDialog = true }, enabled = !readOnly, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Healing, null)
                        Text(" CURA")
                    }
                }
            }
        }
        item {
            TechPanel(accent = AcidCyan) {
                TelemetryTag("RESOURCES.QUICK")
                ResourceControl("VIDA", character.life.current, character.lifeMaximum, !readOnly) {
                    onChange(character.copy(life = character.life.withCurrent(it, character.lifeMaximum)))
                }
                ResourceControl("SANIDADE", character.sanity.current, character.sanityMaximum, !readOnly) {
                    onChange(character.copy(sanity = character.sanity.withCurrent(it, character.sanityMaximum)))
                }
                ResourceControl("ARCANO", character.arcane.current, character.arcaneMaximum, !readOnly) {
                    onChange(character.copy(arcane = character.arcane.withCurrent(it, character.arcaneMaximum)))
                }
                ResourceControl("ENERGIA", character.energy.current, character.energyMaximum, !readOnly) {
                    onChange(character.copy(energy = character.energy.withCurrent(it, character.energyMaximum)))
                }
                ResourceControl("DESTINO", character.destiny.current, character.destiny.maximum, !readOnly) {
                    onChange(character.copy(destiny = character.destiny.withCurrent(it, character.destiny.maximum)))
                }
                ResourceControl("EXAUSTÃO", character.exhaustion.current, character.exhaustion.maximum, !readOnly) {
                    onChange(character.copy(exhaustion = character.exhaustion.withCurrent(it, character.exhaustion.maximum)))
                }
                ResourceControl("CORRUPÇÃO", character.corruption.current, character.corruption.maximum, !readOnly) {
                    onChange(character.copy(corruption = character.corruption.withCurrent(it, character.corruption.maximum)))
                }
            }
        }
        item {
            TechPanel {
                TelemetryTag("DEFENSE.SNAPSHOT")
                Text("PROTEÇÕES", color = Ice, style = MaterialTheme.typography.titleMedium)
                character.calculatedProtections().forEach { (name, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, color = Muted)
                        Text(value.toString(), color = Ice)
                    }
                }
            }
        }
        item {
            val entries = buildList {
                character.powers.forEach { add("PODER" to "${it.name} // ${it.cost.ifBlank { "SEM CUSTO" }}\n${it.effect}") }
                character.mysticAbilities.forEach { add(it.type.ifBlank { "ARCANO" }.uppercase() to "${it.name} // ${it.cost.ifBlank { "SEM CUSTO" }}\n${it.effect}") }
            }
            TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                TelemetryTag("ABILITIES.READY")
                Text("PODERES, MAGIAS, CINZAS E RUNAS", color = Ice, style = MaterialTheme.typography.titleMedium)
                if (entries.isEmpty()) Text("Nenhuma habilidade cadastrada na ficha.", color = Muted)
                entries.forEach { (kind, description) ->
                    Column(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CutCornerShape(6.dp)).padding(10.dp)) {
                        Text(kind, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        Text(description, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item {
            TechPanel(accent = if (character.conditions.isEmpty()) Acid else Signal) {
                TelemetryTag("CONDITIONS.${character.conditions.size}")
                Text("CONDIÇÕES", color = Ice, style = MaterialTheme.typography.titleMedium)
                if (character.conditions.isEmpty()) Text("Nenhuma condição ativa.", color = Muted)
                character.conditions.forEach { condition ->
                    Text("${condition.name} // ${condition.intensity} // ${condition.duration}", color = Ice)
                }
            }
        }
    }

    if (damageDialog) DamageDialog(character, onDismiss = { damageDialog = false }) { amount, region ->
        damageDialog = false
        val protection = character.localProtectionBreakdown(region).total
        val applied = (amount - protection).coerceAtLeast(0)
        onChange(character.copy(life = character.life.withCurrent(character.life.current - applied, character.lifeMaximum)))
    }
    if (healingDialog) AmountDialog("APLICAR CURA", character.life.current, character.lifeMaximum, { healingDialog = false }) { amount ->
        healingDialog = false
        onChange(character.copy(life = character.life.withCurrent(character.life.current + amount, character.lifeMaximum)))
    }
}

@Composable
private fun ResourceControl(label: String, current: Int, maximum: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
            Text("$current / $maximum", color = Ice, style = MaterialTheme.typography.titleMedium)
        }
        IconButton({ onChange(current - 1) }, enabled = enabled && current > 0) { Icon(Icons.Default.Remove, "Reduzir $label") }
        IconButton({ onChange(current + 1) }, enabled = enabled && current < maximum) { Icon(Icons.Default.Add, "Aumentar $label") }
    }
}

@Composable
private fun DamageDialog(character: Character, onDismiss: () -> Unit, onConfirm: (Int, BodyRegion) -> Unit) {
    var amount by remember { mutableIntStateOf(1) }
    var regionIndex by remember { mutableIntStateOf(0) }
    val region = character.bodyRegions.getOrElse(regionIndex) { BodyRegion(name = "Geral") }
    val protection = character.localProtectionBreakdown(region).total
    val applied = (amount - protection).coerceAtLeast(0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PRÉVIA DE DANO") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Informe o dano")
                Stepper(amount, 0, 999) { amount = it }
                Text("2. Escolha a região")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton({ regionIndex = (regionIndex - 1).floorMod(character.bodyRegions.size) }) { Text("‹") }
                    Text(region.name, modifier = Modifier.weight(1f).align(Alignment.CenterVertically))
                    TextButton({ regionIndex = (regionIndex + 1).floorMod(character.bodyRegions.size) }) { Text("›") }
                }
                Text("3. P.L. local: $protection")
                Text("4. Resultado: $amount − $protection = $applied de Vida", color = if (applied > 0) Signal else Acid)
                Text("Nada é alterado antes da confirmação.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton({ onConfirm(amount, region) }) { Text("CONFIRMAR") } },
        dismissButton = { TextButton(onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun AmountDialog(title: String, current: Int, maximum: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var amount by remember { mutableIntStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Stepper(amount, 0, 999) { amount = it }
                Text("Vida: $current → ${(current + amount).coerceAtMost(maximum)} / $maximum")
            }
        },
        confirmButton = { TextButton({ onConfirm(amount) }) { Text("CONFIRMAR") } },
        dismissButton = { TextButton(onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun Stepper(value: Int, minimum: Int, maximum: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton({ onChange((value - 1).coerceAtLeast(minimum)) }) { Icon(Icons.Default.Remove, "Reduzir") }
        Text(value.toString(), modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleLarge)
        IconButton({ onChange((value + 1).coerceAtMost(maximum)) }) { Icon(Icons.Default.Add, "Aumentar") }
    }
}

private fun ResourceValue.withCurrent(value: Int, limit: Int): ResourceValue = copy(current = value.coerceIn(0, limit.coerceAtLeast(0)))

private fun Int.floorMod(divisor: Int): Int = if (divisor <= 0) 0 else Math.floorMod(this, divisor)
