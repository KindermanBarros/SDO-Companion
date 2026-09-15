package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.catalog.RaceCatalog
import com.kinderman.sdo.domain.catalog.RaceDefinition
import com.kinderman.sdo.domain.catalog.RacialPower
import com.kinderman.sdo.domain.catalog.SubRaceDefinition
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.ui.SdoActionButton
import com.kinderman.sdo.ui.SdoActionStyle
import com.kinderman.sdo.ui.SdoFilterChip
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoResponsiveGrid
import com.kinderman.sdo.ui.SectionHeader

@Composable
internal fun RacePickerDialog(
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: (RaceDefinition, SubRaceDefinition?, String, List<RacialPower>, RacialPower?) -> Unit,
) {
    val initialRace = RaceCatalog.race(character.race) ?: RaceCatalog.races.firstOrNull()
    if (initialRace == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("CATÁLOGO DE RAÇAS INDISPONÍVEL") },
            text = { Text("Nenhuma raça válida foi carregada. Feche a tela e tente sincronizar novamente.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("FECHAR") } },
        )
        return
    }
    val initialSubRaceName = character.subRace.ifBlank { RaceCatalog.legacySubRace(character.race).orEmpty() }
    val initialSubRace = RaceCatalog.subRacesFor(initialRace).firstOrNull { it.name == initialSubRaceName }
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
    val valid = basePowers.size == (if (needsReplacement) 1 else 2) && (!needsReplacement || subRacePower != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SELECIONAR RAÇA") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("02", "Raça-base")
                SelectionMenu(
                    title = "RAÇA",
                    value = race,
                    options = RaceCatalog.races,
                    optionLabel = { it.name.uppercase() },
                ) { option ->
                    race = option
                    attribute = option.attribute.takeUnless { it == "Qualquer" } ?: "FOR"
                    if (subRace?.organicOnly == true && !option.organic) {
                        subRace = null
                        subRacePower = null
                    }
                    basePowers = if (subRace == null) option.powers.toSet() else option.powers.firstOrNull()?.let { setOf(it) }.orEmpty()
                }
                SdoInsetCard {
                    Text("RECURSOS RACIAIS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    Text("HP +${race.hp} // SAN +${race.sanity} // ARC +${race.arcane} // ENE +${race.energy}", color = MaterialTheme.colorScheme.onSurface)
                    Text("ATRIBUTO // ${race.attribute} +1", color = MaterialTheme.colorScheme.primary)
                }
                if (race.attribute == "Qualquer") {
                    SdoInsetCard(verticalSpacing = 8.dp) {
                        Text("ESCOLHA O ATRIBUTO RACIAL", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        Text("Humanos e Sangue-Vil recebem +1 no atributo selecionado.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        SdoResponsiveGrid(listOf("FOR", "VIG", "AGI", "POD", "INT", "CAR"), minItemWidth = 96.dp, maxColumns = 3) { option, modifier ->
                            SdoFilterChip(option, attribute == option, { attribute = option }, modifier)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SectionHeader("02.A", "Sub-raça opcional")
                SelectionMenu(
                    title = "SUB-RAÇA",
                    value = subRace,
                    options = listOf<SubRaceDefinition?>(null) + RaceCatalog.subRacesFor(race),
                    optionLabel = { it?.name?.uppercase() ?: "NENHUMA" },
                ) { option ->
                    subRace = option
                    basePowers = if (option == null) race.powers.toSet() else race.powers.firstOrNull()?.let { setOf(it) }.orEmpty()
                    subRacePower = option?.powers?.firstOrNull()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SectionHeader("02.B", if (needsReplacement) "Mantenha 1 poder racial" else "Poderes raciais")
                race.powers.forEach { power -> PowerChoice(power, power in basePowers) { checked ->
                    basePowers = if (checked) {
                        if (needsReplacement) setOf(power) else basePowers + power
                    } else basePowers - power
                } }
                subRace?.let { selected ->
                    Text("ESCOLHA 1 PODER DE ${selected.name.uppercase()}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                    selected.powers.forEach { power -> PowerChoice(power, subRacePower == power) { checked ->
                        subRacePower = power.takeIf { checked }
                    } }
                }
                if (!valid) Text("Selecione exatamente dois poderes: dois raciais, ou um racial e um de sub-raça.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { SdoActionButton("APLICAR", { onConfirm(race, subRace, attribute, basePowers.toList(), subRacePower) }, enabled = valid, style = SdoActionStyle.PRIMARY) },
        dismissButton = { SdoActionButton("CANCELAR", onDismiss) },
    )
}

@Composable
private fun PowerChoice(power: RacialPower, checked: Boolean, onChecked: (Boolean) -> Unit) {
    SdoInsetCard(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChecked),
        accent = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(checked, onCheckedChange = null)
            Column(Modifier.padding(top = 10.dp)) {
                Text(power.name, color = MaterialTheme.colorScheme.onSurface)
                Text(power.effect, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun <T> SelectionMenu(
    title: String,
    value: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    ChoiceField(title, value, options, true, display = optionLabel, onValue = onSelect)
}
