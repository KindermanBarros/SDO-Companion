package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import com.kinderman.sdo.ui.SdoTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.kinderman.sdo.R
import com.kinderman.sdo.domain.catalog.RaceCatalog
import com.kinderman.sdo.domain.catalog.RaceDefinition
import com.kinderman.sdo.domain.catalog.RacialPower
import com.kinderman.sdo.domain.catalog.SubRaceDefinition
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.ui.SdoActionButton
import com.kinderman.sdo.ui.SdoActionStyle
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.SdoOutlinedButton
import com.kinderman.sdo.ui.HudTextField

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
            confirmButton = { SdoTextButton(onClick = onDismiss) { Text("FECHAR") } },
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
    var raceName by rememberSaveable(character.id) { mutableStateOf(initialRace.name) }
    var subRaceName by rememberSaveable(character.id) { mutableStateOf(initialSubRace?.name.orEmpty()) }
    var attribute by rememberSaveable(character.id) {
        mutableStateOf(character.raceAttribute.ifBlank { initialRace.attribute.takeUnless { it == "Qualquer" } ?: "FOR" })
    }
    var basePowerNames by rememberSaveable(character.id) { mutableStateOf(initialBasePowers.map { it.name }) }
    var subRacePowerName by rememberSaveable(character.id) { mutableStateOf(initialSubRacePower?.name.orEmpty()) }
    var choosingRace by rememberSaveable(character.id) { mutableStateOf(false) }
    var raceQuery by rememberSaveable(character.id) { mutableStateOf("") }
    val race = RaceCatalog.race(raceName) ?: initialRace
    val subRace = RaceCatalog.subRacesFor(race).firstOrNull { it.name == subRaceName }
    val basePowers = race.powers.filter { it.name in basePowerNames }
    val subRacePower = subRace?.powers?.firstOrNull { it.name == subRacePowerName }
    val needsReplacement = subRace != null
    val valid = basePowers.size == (if (needsReplacement) 1 else 2) && (!needsReplacement || subRacePower != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 560.dp).fillMaxWidth()
            .testTag("race-picker-dialog"),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("SELECIONAR RAÇA") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SdoOutlinedButton(
                    onClick = { choosingRace = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("race-selection-trigger"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    SdoMetadataText(
                        stringResource(R.string.race_picker_current_race, race.name.uppercase()),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                race.powers.firstOrNull()?.effect?.let { description ->
                    Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                SdoInsetCard {
                    SdoMetadataText("HP +${race.hp} // SAN +${race.sanity} // ARC +${race.arcane} // ENE +${race.energy}")
                    SdoMetadataText("ATRIBUTO // ${race.attribute} +1")
                }
                if (race.attribute == "Qualquer") {
                    SdoInsetCard(verticalSpacing = 8.dp) {
                        ChoiceField(
                            label = stringResource(R.string.race_picker_attribute),
                            value = attribute,
                            options = listOf("FOR", "VIG", "AGI", "POD", "INT", "CAR"),
                            enabled = true,
                            modifier = Modifier.fillMaxWidth(),
                            onValue = { attribute = it },
                        )
                    }
                }
                SelectionMenu(
                    title = "SUB-RAÇA",
                    value = subRace,
                    options = listOf<SubRaceDefinition?>(null) + RaceCatalog.subRacesFor(race),
                    optionLabel = { it?.name?.uppercase() ?: "NENHUMA" },
                ) { option ->
                    subRaceName = option?.name.orEmpty()
                    basePowerNames = if (option == null) race.powers.map { it.name } else race.powers.take(1).map { it.name }
                    subRacePowerName = option?.powers?.firstOrNull()?.name.orEmpty()
                }
                Text(
                    text = stringResource(if (needsReplacement) R.string.race_picker_keep_one else R.string.race_picker_powers),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                race.powers.forEach { power -> PowerChoice(power, power in basePowers) { checked ->
                    basePowerNames = if (checked) {
                        if (needsReplacement) listOf(power.name) else basePowerNames + power.name
                    } else basePowerNames - power.name
                } }
                subRace?.let { selected ->
                    Text("ESCOLHA 1 PODER DE ${selected.name.uppercase()}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                    selected.powers.forEach { power -> PowerChoice(power, subRacePower == power) { checked ->
                        subRacePowerName = if (checked) power.name else ""
                    } }
                }
                if (!valid) Text("Selecione exatamente dois poderes: dois raciais, ou um racial e um de sub-raça.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { SdoActionButton("APLICAR", { onConfirm(race, subRace, attribute, basePowers, subRacePower) }, enabled = valid, style = SdoActionStyle.PRIMARY) },
        dismissButton = { SdoActionButton("CANCELAR", onDismiss) },
    )
    if (choosingRace) {
        RaceChoiceDialog(
            selectedRace = race,
            query = raceQuery,
            onQueryChange = { raceQuery = it },
            onDismiss = { choosingRace = false; raceQuery = "" },
            onSelect = { option ->
                if (option.name != race.name) {
                    raceName = option.name
                    attribute = option.attribute.takeUnless { it == "Qualquer" } ?: "FOR"
                    if (RaceCatalog.subRacesFor(option).none { it.name == subRaceName }) {
                        subRaceName = ""
                        subRacePowerName = ""
                    }
                    basePowerNames = option.powers.take(if (subRaceName.isBlank()) 2 else 1).map { it.name }
                }
                choosingRace = false
                raceQuery = ""
            },
        )
    }
}

@Composable
private fun RaceChoiceDialog(
    selectedRace: RaceDefinition,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (RaceDefinition) -> Unit,
) {
    val choices = RaceCatalog.races.filter { it.name.contains(query.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 560.dp).fillMaxWidth()
            .testTag("race-choice-dialog"),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.race_picker_choose_race)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HudTextField(
                    label = stringResource(R.string.race_picker_search),
                    value = query,
                    modifier = Modifier.fillMaxWidth().testTag("race-choice-search"),
                    onValue = onQueryChange,
                )
                if (choices.isEmpty()) {
                    Text(stringResource(R.string.race_picker_no_results))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(choices, key = { it.name }) { option ->
                            val selected = option.name == selectedRace.name
                            SdoInsetCard(
                                modifier = Modifier.heightIn(min = 48.dp)
                                    .selectable(selected = selected, role = Role.RadioButton) { onSelect(option) },
                                accent = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(option.name.uppercase())
                                        option.powers.firstOrNull()?.effect?.let { description ->
                                            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    RadioButton(selected = selected, onClick = null)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            SdoTextButton(onClick = onDismiss) { Text(stringResource(R.string.race_picker_close)) }
        },
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
