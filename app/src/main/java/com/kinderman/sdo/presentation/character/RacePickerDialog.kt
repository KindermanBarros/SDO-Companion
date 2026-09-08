package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.kinderman.sdo.domain.catalog.RaceCatalog
import com.kinderman.sdo.domain.catalog.RaceDefinition
import com.kinderman.sdo.domain.catalog.RacialPower
import com.kinderman.sdo.domain.catalog.SubRaceDefinition
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechCutDark

@Composable
internal fun RacePickerDialog(
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: (RaceDefinition, SubRaceDefinition?, String, List<RacialPower>, RacialPower?) -> Unit,
) {
    val initialRace = RaceCatalog.race(character.race) ?: RaceCatalog.races.first()
    val initialSubRace = RaceCatalog.subRacesFor(initialRace).firstOrNull { it.name == character.subRace }
    val initialBaseCount = if (initialSubRace == null) 2 else 1
    val storedBasePowers = initialRace.powers.filter { option ->
        character.powers.any { it.name == option.name && it.origin == "Raça — ${initialRace.name}" }
    }
    val initialBasePowers = storedBasePowers.takeIf { it.size == initialBaseCount }
        ?: initialRace.powers.take(initialBaseCount)
    val initialSubRacePower = initialSubRace?.powers?.firstOrNull { option ->
        character.powers.any { it.name == option.name && it.origin == "Sub-raça — ${initialSubRace.name}" }
    } ?: initialSubRace?.powers?.firstOrNull()
    var race by remember { mutableStateOf(initialRace) }
    var subRace by remember { mutableStateOf(initialSubRace) }
    var attribute by remember { mutableStateOf(character.raceAttribute.ifBlank { race.attribute.takeUnless { it == "Qualquer" } ?: "FOR" }) }
    var basePowers by remember { mutableStateOf(initialBasePowers.toSet()) }
    var subRacePower by remember { mutableStateOf(initialSubRacePower) }
    val needsReplacement = subRace != null
    val valid = basePowers.size == if (needsReplacement) 1 else 2 && (!needsReplacement || subRacePower != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SELECIONAR RAÇA") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("RAÇA-BASE", color = Acid, style = MaterialTheme.typography.labelLarge)
                SelectionMenu(
                    label = race.name.uppercase(),
                    options = RaceCatalog.races,
                    optionLabel = { it.name.uppercase() },
                ) { option ->
                    race = option
                    attribute = option.attribute.takeUnless { it == "Qualquer" } ?: "FOR"
                    if (subRace?.organicOnly == true && !option.organic) {
                        subRace = null
                        subRacePower = null
                    }
                    basePowers = if (subRace == null) option.powers.toSet() else setOf(option.powers.first())
                }
                Text("HP +${race.hp} // SAN +${race.sanity} // ARC +${race.arcane} // ENE +${race.energy}", color = Ice)
                Text("ATRIBUTO // ${race.attribute} +1", color = Acid)
                if (race.attribute == "Qualquer") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("FOR", "VIG", "AGI", "POD", "INT", "CAR").forEach { option ->
                            TextButton(onClick = { attribute = option }) { Text(if (attribute == option) "[$option]" else option) }
                        }
                    }
                }
                HorizontalDivider(color = TechCutDark)
                Text("SUB-RAÇA // ADICIONAL OPCIONAL", color = Acid, style = MaterialTheme.typography.labelLarge)
                SelectionMenu(
                    label = subRace?.name?.uppercase() ?: "NENHUMA",
                    options = listOf<SubRaceDefinition?>(null) + RaceCatalog.subRacesFor(race),
                    optionLabel = { it?.name?.uppercase() ?: "NENHUMA" },
                ) { option ->
                    subRace = option
                    basePowers = if (option == null) race.powers.toSet() else setOf(race.powers.first())
                    subRacePower = option?.powers?.first()
                }
                HorizontalDivider(color = TechCutDark)
                Text(if (needsReplacement) "MANTENHA 1 PODER RACIAL" else "PODERES RACIAIS", color = Acid, style = MaterialTheme.typography.labelLarge)
                race.powers.forEach { power -> PowerChoice(power, power in basePowers) { checked ->
                    basePowers = if (checked) {
                        if (needsReplacement) setOf(power) else (basePowers + power).takeLast(2).toSet()
                    } else basePowers - power
                } }
                subRace?.let { selected ->
                    Text("ESCOLHA 1 PODER DE ${selected.name.uppercase()}", color = Signal, style = MaterialTheme.typography.labelLarge)
                    selected.powers.forEach { power -> PowerChoice(power, subRacePower == power) { checked ->
                        subRacePower = power.takeIf { checked }
                    } }
                }
                if (!valid) Text("Selecione exatamente dois poderes: dois raciais, ou um racial e um de sub-raça.", color = Signal)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(race, subRace, attribute, basePowers.toList(), subRacePower) }, enabled = valid) { Text("APLICAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun PowerChoice(power: RacialPower, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(vertical = 4.dp)) {
        Checkbox(checked, onCheckedChange = null)
        Column(Modifier.padding(top = 8.dp)) {
            Text(power.name, color = Ice)
            Text(power.effect, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun <T> SelectionMenu(
    label: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("[ $label ▾ ]")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
