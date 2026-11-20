package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterFamiliar
import com.kinderman.sdo.domain.model.FamiliarRarity
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.SdoButton
import com.kinderman.sdo.ui.SdoCardTitle
import com.kinderman.sdo.ui.SdoFilterChip
import com.kinderman.sdo.ui.SdoIconButton
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.SdoOutlinedButton
import com.kinderman.sdo.ui.SdoResponsiveGrid
import com.kinderman.sdo.ui.SdoTextButton
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag
import com.kinderman.sdo.ui.sdoClickable
import java.util.UUID

private val familiarSizes = listOf("Pequeno", "Médio")

private val familiarPresets: List<CharacterFamiliar> = listOf(
    CharacterFamiliar(
        id = "fam-corvo",
        name = "Corvo Mensageiro",
        species = "Corvo",
        rarity = FamiliarRarity.COMMON,
        size = "Pequeno",
        movement = "voo 14 m",
        hp = 6,
        maxHp = 6,
        generalProtection = 12,
        senses = "Visão aguçada diurna",
        specialTrait = "Memoriza rotas simples e entrega pequenos bilhetes.",
        attack = "bicada, 1d4 + AGI",
    ),
    CharacterFamiliar(
        id = "fam-gato",
        name = "Gato de Beco",
        species = "Gato",
        rarity = FamiliarRarity.COMMON,
        size = "Pequeno",
        movement = "12 m",
        hp = 8,
        maxHp = 8,
        generalProtection = 13,
        senses = "Visão na penumbra e audição sensível",
        specialTrait = "Furtivo; excelente vigia noturno contra intrusos.",
        attack = "garras, 1d4 + AGI",
    ),
    CharacterFamiliar(
        id = "fam-cao",
        name = "Cão Rastreador",
        species = "Cão",
        rarity = FamiliarRarity.COMMON,
        size = "Médio",
        movement = "12 m",
        hp = 12,
        maxHp = 12,
        generalProtection = 11,
        senses = "Faro apurado (+2 para rastrear)",
        specialTrait = "Identifica odores familiares e alerta o grupo em emboscadas.",
        attack = "mordida, 1d6 + FOR",
    ),
    CharacterFamiliar(
        id = "fam-furao",
        name = "Furão Ladrão",
        species = "Furão",
        rarity = FamiliarRarity.COMMON,
        size = "Pequeno",
        movement = "10 m / escalada 6 m",
        hp = 6,
        maxHp = 6,
        generalProtection = 13,
        senses = "Faro e percepção tátil",
        specialTrait = "Passa por frestas estreitas e recupera objetos pequenos.",
        attack = "mordida, 1d4",
    ),
    CharacterFamiliar(
        id = "fam-rato",
        name = "Rato de Esgoto",
        species = "Rato",
        rarity = FamiliarRarity.COMMON,
        size = "Pequeno",
        movement = "8 m",
        hp = 4,
        maxHp = 4,
        generalProtection = 12,
        senses = "Faro e orientação subterrânea",
        specialTrait = "Detecta gases ou água contaminada e explora túneis.",
        attack = "mordida, 1d3",
    ),
    CharacterFamiliar(
        id = "fam-salamandra",
        name = "Salamandra de Cinza",
        species = "Salamandra Ígnea",
        rarity = FamiliarRarity.EXOTIC,
        size = "Pequeno",
        movement = "10 m",
        hp = 10,
        maxHp = 10,
        generalProtection = 12,
        senses = "Percepção térmica",
        specialTrait = "Imune a chamas comuns; aquece acampamentos e reage a Cinzas de Fogo.",
        attack = "toque ígneo, 1d4 + POD",
    ),
    CharacterFamiliar(
        id = "fam-mariposa",
        name = "Mariposa Onírica",
        species = "Inseto Planar",
        rarity = FamiliarRarity.EXOTIC,
        size = "Pequeno",
        movement = "voo 10 m",
        hp = 6,
        maxHp = 6,
        generalProtection = 13,
        senses = "Sente Energia Onírica próxima",
        specialTrait = "Emite brilho suave que acalma o sono e sinaliza distorções arcanas.",
        attack = "pó atordoante (visual)",
    ),
    CharacterFamiliar(
        id = "fam-homunculo",
        name = "Homúnculo de Sucata",
        species = "Construto Minúsculo",
        rarity = FamiliarRarity.EXOTIC,
        size = "Pequeno",
        movement = "8 m",
        hp = 10,
        maxHp = 10,
        generalProtection = 13,
        senses = "Visão mecânica",
        specialTrait = "Manipula ferramentas leves, travas simples e peças de engenharia.",
        attack = "pinça, 1d4 + FOR",
    ),
    CharacterFamiliar(
        id = "fam-coruja-espectral",
        name = "Coruja Espectral",
        species = "Ave Etérea",
        rarity = FamiliarRarity.EXOTIC,
        size = "Pequeno",
        movement = "voo 16 m",
        hp = 8,
        maxHp = 8,
        generalProtection = 14,
        senses = "Visão noturna e percepção espiritual",
        specialTrait = "Voo completamente silencioso através de sombras densas.",
        attack = "garras gélidas, 1d4 + POD",
    ),
    CharacterFamiliar(
        id = "fam-lagarto-cristal",
        name = "Lagarto de Cristal",
        species = "Réptil Mineral",
        rarity = FamiliarRarity.EXOTIC,
        size = "Pequeno",
        movement = "10 m / escalada 8 m",
        hp = 10,
        maxHp = 10,
        generalProtection = 14,
        senses = "Vibração mineral",
        specialTrait = "Camuflagem em rochas e cavernas; escamas refletem faíscas arcanas.",
        attack = "cauda cortante, 1d4 + AGI",
    ),
)

@Composable
internal fun CharacterFamiliarSection(
    character: Character,
    editable: Boolean,
    onChange: (Character) -> Unit,
) {
    var showPresetsDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomDialog by rememberSaveable { mutableStateOf(false) }

    TechPanel(accent = MaterialTheme.colorScheme.tertiary) {
        SectionHeader("15", "Familiares e Companheiros")
        SdoMetadataText(
            "Criaturas comuns ou exóticas que acompanham o personagem (não-montarias), com sentidos, traços e atributos básicos.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (character.familiars.isEmpty()) {
            SdoInsetCard(contentPadding = 12.dp) {
                Text(
                    "Nenhum familiar registrado. Escolha um modelo comum/exótico ou crie um familiar personalizado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        character.familiars.forEachIndexed { index, familiar ->
            FamiliarCard(
                familiar = familiar,
                editable = editable,
                onUpdate = { updated ->
                    onChange(
                        character.copy(
                            familiars = character.familiars.mapIndexed { i, current ->
                                if (i == index) updated else current
                            },
                        ),
                    )
                },
                onDelete = {
                    onChange(
                        character.copy(
                            familiars = character.familiars.filterIndexed { i, _ -> i != index },
                        ),
                    )
                },
            )
        }

        if (editable) {
            SdoResponsiveGrid(
                items = listOf("preset", "custom"),
                minItemWidth = 156.dp,
            ) { action, modifier ->
                if (action == "preset") {
                    SdoButton(
                        onClick = { showPresetsDialog = true },
                        modifier = modifier,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Adicionar do catálogo")
                    }
                } else {
                    SdoOutlinedButton(
                        onClick = { showCustomDialog = true },
                        modifier = modifier,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Criar familiar")
                    }
                }
            }
        }
    }

    if (showPresetsDialog) {
        FamiliarPresetDialog(
            onDismiss = { showPresetsDialog = false },
            onSelect = { preset ->
                onChange(
                    character.copy(
                        familiars = character.familiars + preset.copy(id = UUID.randomUUID().toString()),
                    ),
                )
                showPresetsDialog = false
            },
        )
    }

    if (showCustomDialog) {
        CustomFamiliarDialog(
            onDismiss = { showCustomDialog = false },
            onCreate = { created ->
                onChange(character.copy(familiars = character.familiars + created))
                showCustomDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FamiliarCard(
    familiar: CharacterFamiliar,
    editable: Boolean,
    onUpdate: (CharacterFamiliar) -> Unit,
    onDelete: () -> Unit,
) {
    var editing by rememberSaveable(familiar.id) { mutableStateOf(false) }

    SdoInsetCard(contentPadding = 12.dp, verticalSpacing = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TelemetryTag(familiar.rarity.label.uppercase(), MaterialTheme.colorScheme.primary)
                    if (familiar.size.isNotBlank()) {
                        TelemetryTag(familiar.size.uppercase(), MaterialTheme.colorScheme.secondary)
                    }
                    if (familiar.species.isNotBlank()) {
                        TelemetryTag(familiar.species.uppercase())
                    }
                }
                SdoCardTitle(
                    familiar.name.ifBlank { "Familiar sem nome" },
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (editable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompactEditToggleButton(editing = editing, enabled = editable, onClick = { editing = !editing })
                    SdoIconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remover familiar",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        if (!editing || !editable) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (familiar.movement.isNotBlank()) {
                    TelemetryTag("MOV ${familiar.movement}")
                }
                TelemetryTag("PV ${familiar.hp}/${familiar.maxHp}", MaterialTheme.colorScheme.tertiary)
                TelemetryTag("PG ${familiar.generalProtection}")
            }
            if (familiar.senses.isNotBlank()) {
                SdoMetadataText("SENTIDOS: ${familiar.senses}", color = MaterialTheme.colorScheme.secondary)
            }
            if (familiar.specialTrait.isNotBlank()) {
                Text(
                    text = familiar.specialTrait,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (familiar.attack.isNotBlank() && familiar.attack != "—") {
                SdoMetadataText("ATAQUE: ${familiar.attack}", color = MaterialTheme.colorScheme.primary)
            }
            if (familiar.notes.isNotBlank()) {
                Text(
                    text = familiar.notes,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            FamiliarEditorFields(
                familiar = familiar,
                enabled = editable,
                onUpdate = onUpdate,
            )
        }
    }
}

@Composable
private fun FamiliarEditorFields(
    familiar: CharacterFamiliar,
    enabled: Boolean,
    onUpdate: (CharacterFamiliar) -> Unit,
) {
    TwoFields(
        first = { mod ->
            SheetTextField(
                label = "Nome",
                value = familiar.name,
                enabled = enabled,
                modifier = mod,
                fieldKey = "familiar/${familiar.id}/name",
            ) { onUpdate(familiar.copy(name = it)) }
        },
        second = { mod ->
            SheetTextField(
                label = "Espécie / tipo",
                value = familiar.species,
                placeholder = "Ex.: Corvo, Salamandra",
                enabled = enabled,
                modifier = mod,
                fieldKey = "familiar/${familiar.id}/species",
            ) { onUpdate(familiar.copy(species = it)) }
        },
    )

    TwoFields(
        first = { mod ->
            ChoiceField(
                label = "Categoria",
                value = familiar.rarity.name,
                options = FamiliarRarity.entries.map { it.name },
                enabled = enabled,
                modifier = mod,
                display = { name ->
                    FamiliarRarity.entries.firstOrNull { it.name == name }?.label ?: name
                },
            ) { selected ->
                val rarity = runCatching { FamiliarRarity.valueOf(selected) }
                    .getOrDefault(FamiliarRarity.CUSTOM)
                onUpdate(familiar.copy(rarity = rarity))
            }
        },
        second = { mod ->
            ChoiceField(
                label = "Tamanho",
                value = familiar.size,
                options = familiarSizes,
                enabled = enabled,
                modifier = mod,
            ) { onUpdate(familiar.copy(size = it)) }
        },
    )

    TwoFields(
        first = { mod ->
            SheetTextField(
                label = "Movimento",
                value = familiar.movement,
                placeholder = "Ex.: 12 m, voo 14 m",
                enabled = enabled,
                modifier = mod,
                fieldKey = "familiar/${familiar.id}/movement",
            ) { onUpdate(familiar.copy(movement = it)) }
        },
        second = { mod ->
            IntegerField(
                label = "Proteção Geral (PG)",
                value = familiar.generalProtection,
                enabled = enabled,
                modifier = mod,
                fieldKey = "familiar/${familiar.id}/generalProtection",
            ) { onUpdate(familiar.copy(generalProtection = it)) }
        },
    )

    TwoFields(
        first = { mod ->
            IntegerField(
                label = "PV atual",
                value = familiar.hp,
                enabled = enabled,
                modifier = mod,
                fieldKey = "familiar/${familiar.id}/hp",
            ) { onUpdate(familiar.copy(hp = it)) }
        },
        second = { mod ->
            IntegerField(
                label = "PV máximo",
                value = familiar.maxHp,
                enabled = enabled,
                modifier = mod,
                fieldKey = "familiar/${familiar.id}/maxHp",
            ) { onUpdate(familiar.copy(maxHp = it)) }
        },
    )

    SheetTextField(
        label = "Sentidos",
        value = familiar.senses,
        placeholder = "Ex.: Visão noturna, faro apurado",
        enabled = enabled,
        fieldKey = "familiar/${familiar.id}/senses",
    ) { onUpdate(familiar.copy(senses = it)) }

    SheetTextField(
        label = "Característica especial",
        value = familiar.specialTrait,
        multiline = true,
        enabled = enabled,
        fieldKey = "familiar/${familiar.id}/specialTrait",
    ) { onUpdate(familiar.copy(specialTrait = it)) }

    SheetTextField(
        label = "Ataque / ação",
        value = familiar.attack,
        placeholder = "Ex.: bicada, 1d4 + AGI",
        enabled = enabled,
        fieldKey = "familiar/${familiar.id}/attack",
    ) { onUpdate(familiar.copy(attack = it)) }

    SheetTextField(
        label = "Observações",
        value = familiar.notes,
        multiline = true,
        enabled = enabled,
        fieldKey = "familiar/${familiar.id}/notes",
    ) { onUpdate(familiar.copy(notes = it)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FamiliarPresetDialog(
    onDismiss: () -> Unit,
    onSelect: (CharacterFamiliar) -> Unit,
) {
    var selectedRarity by remember { mutableStateOf<FamiliarRarity?>(null) }
    val filtered = remember(selectedRarity) {
        if (selectedRarity == null) familiarPresets
        else familiarPresets.filter { it.rarity == selectedRarity }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exemplos de Familiares") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SdoFilterChip(
                        label = "Todos",
                        selected = selectedRarity == null,
                        onClick = { selectedRarity = null },
                    )
                    listOf(FamiliarRarity.COMMON, FamiliarRarity.EXOTIC).forEach { rarity ->
                        SdoFilterChip(
                            label = rarity.label,
                            selected = selectedRarity == rarity,
                            onClick = { selectedRarity = rarity },
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { preset ->
                        SdoInsetCard(
                            modifier = Modifier.fillMaxWidth().sdoClickable { onSelect(preset) },
                            contentPadding = 10.dp,
                            verticalSpacing = 4.dp,
                        ) {
                            SdoCardTitle(preset.name, color = MaterialTheme.colorScheme.onSurface)
                            SdoMetadataText(
                                "${preset.rarity.label} • ${preset.species} • ${preset.movement} • PV ${preset.maxHp}",
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                text = preset.specialTrait,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            SdoTextButton(onClick = onDismiss) { Text("Fechar") }
        },
    )
}

@Composable
private fun CustomFamiliarDialog(
    onDismiss: () -> Unit,
    onCreate: (CharacterFamiliar) -> Unit,
) {
    var draft by remember {
        mutableStateOf(
            CharacterFamiliar(
                id = UUID.randomUUID().toString(),
                rarity = FamiliarRarity.CUSTOM,
                size = "Pequeno",
                movement = "10 m",
                hp = 6,
                maxHp = 6,
                generalProtection = 12,
            ),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar Familiar Personalizado") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalSheetFieldCollaboration provides SheetFieldCollaboration(isSheetContext = false),
                ) {
                    FamiliarEditorFields(
                        familiar = draft,
                        enabled = true,
                        onUpdate = { draft = it },
                    )
                }
            }
        },
        confirmButton = {
            SdoTextButton(
                onClick = {
                    onCreate(
                        draft.copy(
                            name = draft.name.ifBlank {
                                draft.species.ifBlank { "Familiar Personalizado" }
                            },
                        ),
                    )
                },
            ) { Text("Salvar") }
        },
        dismissButton = {
            SdoTextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
