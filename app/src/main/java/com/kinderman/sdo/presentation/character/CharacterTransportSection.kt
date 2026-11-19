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
import com.kinderman.sdo.domain.model.CharacterTransport
import com.kinderman.sdo.domain.model.TransportCategory
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

private val transportSizes = listOf("Pequeno", "Médio", "Grande", "Enorme")

private val transportPresets: List<CharacterTransport> = listOf(
    CharacterTransport(
        id = "preset-cavalo",
        name = "Cavalo",
        category = TransportCategory.MOUNT,
        size = "Grande",
        movement = "16 m",
        loadCapacity = 16,
        hp = 18,
        maxHp = 18,
        generalProtection = 12,
        localProtection = 1,
        attack = "coice, 1d8 + FOR",
        notes = "Montaria ágil de estrada e combate; 10h de jornada segura.",
    ),
    CharacterTransport(
        id = "preset-corta-terra",
        name = "Corta Terra",
        category = TransportCategory.MOUNT,
        size = "Grande",
        movement = "14 m",
        loadCapacity = 12,
        hp = 18,
        maxHp = 18,
        generalProtection = 12,
        localProtection = 2,
        attack = "garras, 1d8 + FOR",
        notes = "Montaria resistente para terreno acidentado.",
    ),
    CharacterTransport(
        id = "preset-warg",
        name = "Warg",
        category = TransportCategory.MOUNT,
        size = "Grande",
        movement = "14 m",
        loadCapacity = 10,
        hp = 18,
        maxHp = 18,
        generalProtection = 13,
        localProtection = 2,
        attack = "mordida, 1d8 + FOR",
        notes = "Predador treinado para perseguição e combate.",
    ),
    CharacterTransport(
        id = "preset-pangolin",
        name = "Pangolin",
        category = TransportCategory.MOUNT,
        size = "Grande",
        movement = "10 m",
        loadCapacity = 16,
        hp = 22,
        maxHp = 22,
        generalProtection = 11,
        localProtection = 4,
        attack = "cauda, 1d8 + FOR",
        notes = "Couraça natural pesada e ótima capacidade de carga.",
    ),
    CharacterTransport(
        id = "preset-coruja",
        name = "Coruja Manto-de-Adaga",
        category = TransportCategory.FLYING_MOUNT,
        size = "Grande",
        movement = "voo 18 m",
        loadCapacity = 6,
        hp = 14,
        maxHp = 14,
        generalProtection = 14,
        localProtection = 1,
        attack = "garras, 1d6 + AGI",
        notes = "Montaria alada silenciosa para reconhecimento e ataques rápidos.",
    ),
    CharacterTransport(
        id = "preset-tartaruga-leao",
        name = "Tartaruga-Leão",
        category = TransportCategory.MOUNT,
        size = "Enorme",
        movement = "8 m",
        loadCapacity = 30,
        hp = 35,
        maxHp = 35,
        generalProtection = 12,
        localProtection = 5,
        attack = "mordida, 1d10 + FOR",
        notes = "Montaria colossal de carga extrema e alta resistência.",
    ),
    CharacterTransport(
        id = "preset-dragonete",
        name = "Dragonete",
        category = TransportCategory.FLYING_MOUNT,
        size = "Grande",
        movement = "voo 16 m",
        loadCapacity = 10,
        hp = 22,
        maxHp = 22,
        generalProtection = 14,
        localProtection = 3,
        attack = "mordida, 1d8 + FOR",
        notes = "Montaria dracônica ágil e blindada.",
    ),
    CharacterTransport(
        id = "preset-carroca",
        name = "Carroça de Viagem",
        category = TransportCategory.VEHICLE,
        size = "Grande",
        movement = "4 km/h (puxada)",
        loadCapacity = 40,
        hp = 30,
        maxHp = 30,
        generalProtection = 10,
        localProtection = 3,
        attack = "—",
        notes = "Veículo de madeira com 2 eixos para suprimentos e até 4 passageiros.",
    ),
    CharacterTransport(
        id = "preset-carruagem",
        name = "Carruagem Blindada",
        category = TransportCategory.VEHICLE,
        size = "Enorme",
        movement = "4 km/h (puxada)",
        loadCapacity = 50,
        hp = 45,
        maxHp = 45,
        generalProtection = 12,
        localProtection = 5,
        attack = "atropelamento, 1d10 + FOR",
        notes = "Cabine reforçada oferecendo cobertura parcial aos ocupantes.",
    ),
    CharacterTransport(
        id = "preset-barco-remo",
        name = "Barco a Remo",
        category = TransportCategory.WATERCRAFT,
        size = "Grande",
        movement = "10 m (água)",
        loadCapacity = 25,
        hp = 24,
        maxHp = 24,
        generalProtection = 10,
        localProtection = 2,
        attack = "—",
        notes = "Embarcação leve fluvial ou costeira para até 4 tripulantes.",
    ),
    CharacterTransport(
        id = "preset-barco-vela",
        name = "Barco à Vela Ligeiro",
        category = TransportCategory.WATERCRAFT,
        size = "Enorme",
        movement = "14 m (água)",
        loadCapacity = 60,
        hp = 50,
        maxHp = 50,
        generalProtection = 11,
        localProtection = 4,
        attack = "abalroamento, 2d8",
        notes = "Navegação fluvial e costeira de médio porte.",
    ),
    CharacterTransport(
        id = "preset-balsa",
        name = "Balsa de Carga",
        category = TransportCategory.WATERCRAFT,
        size = "Enorme",
        movement = "6 m (água)",
        loadCapacity = 80,
        hp = 40,
        maxHp = 40,
        generalProtection = 9,
        localProtection = 3,
        attack = "—",
        notes = "Plataforma estável para travessia de rios, montarias e carga pesada.",
    ),
)

@Composable
internal fun CharacterTransportSection(
    character: Character,
    editable: Boolean,
    onChange: (Character) -> Unit,
) {
    var showPresetsDialog by rememberSaveable { mutableStateOf(false) }

    TechPanel(accent = MaterialTheme.colorScheme.secondary) {
        SectionHeader("14", "Transporte e Montarias")
        SdoMetadataText(
            "Montarias, veículos e embarcações vinculados ao personagem. Carga compartilhada e deslocamento próprio.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (character.transports.isEmpty()) {
            SdoInsetCard(contentPadding = 12.dp) {
                Text(
                    "Nenhum transporte registrado. Adicione um exemplo do livro (montarias, carroças, barcos) ou crie um novo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        character.transports.forEachIndexed { index, transport ->
            TransportCard(
                transport = transport,
                editable = editable,
                onUpdate = { updated ->
                    onChange(
                        character.copy(
                            transports = character.transports.mapIndexed { i, current ->
                                if (i == index) updated else current
                            },
                        ),
                    )
                },
                onDelete = {
                    onChange(
                        character.copy(
                            transports = character.transports.filterIndexed { i, _ -> i != index },
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
                        onClick = {
                            onChange(
                                character.copy(
                                    transports = character.transports + CharacterTransport(
                                        id = UUID.randomUUID().toString(),
                                        name = "Novo Transporte ${character.transports.size + 1}",
                                    ),
                                ),
                            )
                        },
                        modifier = modifier,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Novo transporte")
                    }
                }
            }
        }
    }

    if (showPresetsDialog) {
        TransportPresetDialog(
            onDismiss = { showPresetsDialog = false },
            onSelect = { preset ->
                onChange(
                    character.copy(
                        transports = character.transports + preset.copy(id = UUID.randomUUID().toString()),
                    ),
                )
                showPresetsDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransportCard(
    transport: CharacterTransport,
    editable: Boolean,
    onUpdate: (CharacterTransport) -> Unit,
    onDelete: () -> Unit,
) {
    var editing by rememberSaveable(transport.id) { mutableStateOf(false) }

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
                    TelemetryTag(transport.category.label.uppercase(), MaterialTheme.colorScheme.primary)
                    if (transport.size.isNotBlank()) {
                        TelemetryTag(transport.size.uppercase(), MaterialTheme.colorScheme.secondary)
                    }
                }
                SdoCardTitle(
                    transport.name.ifBlank { "Transporte sem nome" },
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (editable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompactEditToggleButton(editing = editing, enabled = editable, onClick = { editing = !editing })
                    SdoIconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remover transporte",
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
                if (transport.movement.isNotBlank()) {
                    TelemetryTag("MOV ${transport.movement}")
                }
                TelemetryTag("CARGA +${transport.loadCapacity}")
                if (transport.maxHp > 0) {
                    TelemetryTag("PV ${transport.hp}/${transport.maxHp}", MaterialTheme.colorScheme.tertiary)
                }
                if (transport.generalProtection > 0 || transport.localProtection > 0) {
                    TelemetryTag("PG ${transport.generalProtection} / PL ${transport.localProtection}")
                }
            }
            if (transport.attack.isNotBlank() && transport.attack != "—") {
                SdoMetadataText("ATAQUE: ${transport.attack}", color = MaterialTheme.colorScheme.secondary)
            }
            if (transport.notes.isNotBlank()) {
                Text(
                    text = transport.notes,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            HudTextField(
                label = "Nome",
                value = transport.name,
                enabled = editable,
            ) { onUpdate(transport.copy(name = it)) }

            TwoFields(
                first = { mod ->
                    ChoiceField(
                        label = "Tipo",
                        value = transport.category.name,
                        options = TransportCategory.entries.map { it.name },
                        enabled = editable,
                        modifier = mod,
                        display = { name ->
                            TransportCategory.entries.firstOrNull { it.name == name }?.label ?: name
                        },
                    ) { selected ->
                        val cat = runCatching { TransportCategory.valueOf(selected) }
                            .getOrDefault(TransportCategory.OTHER)
                        onUpdate(transport.copy(category = cat))
                    }
                },
                second = { mod ->
                    ChoiceField(
                        label = "Tamanho",
                        value = transport.size,
                        options = transportSizes,
                        enabled = editable,
                        modifier = mod,
                    ) { onUpdate(transport.copy(size = it)) }
                },
            )

            TwoFields(
                first = { mod ->
                    HudTextField(
                        label = "Movimento",
                        value = transport.movement,
                        placeholder = "Ex.: 16 m, voo 18 m",
                        enabled = editable,
                        modifier = mod,
                    ) { onUpdate(transport.copy(movement = it)) }
                },
                second = { mod ->
                    HudTextField(
                        label = "Carga adicional",
                        value = transport.loadCapacity.toString(),
                        enabled = editable,
                        modifier = mod,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    ) { onUpdate(transport.copy(loadCapacity = it.toIntOrNull() ?: 0)) }
                },
            )

            TwoFields(
                first = { mod ->
                    HudTextField(
                        label = "PV atual",
                        value = transport.hp.toString(),
                        enabled = editable,
                        modifier = mod,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    ) { onUpdate(transport.copy(hp = it.toIntOrNull() ?: 0)) }
                },
                second = { mod ->
                    HudTextField(
                        label = "PV máximo",
                        value = transport.maxHp.toString(),
                        enabled = editable,
                        modifier = mod,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    ) { onUpdate(transport.copy(maxHp = it.toIntOrNull() ?: 0)) }
                },
            )

            TwoFields(
                first = { mod ->
                    HudTextField(
                        label = "Proteção Geral (PG)",
                        value = transport.generalProtection.toString(),
                        enabled = editable,
                        modifier = mod,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    ) { onUpdate(transport.copy(generalProtection = it.toIntOrNull() ?: 0)) }
                },
                second = { mod ->
                    HudTextField(
                        label = "Proteção Local (PL)",
                        value = transport.localProtection.toString(),
                        enabled = editable,
                        modifier = mod,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    ) { onUpdate(transport.copy(localProtection = it.toIntOrNull() ?: 0)) }
                },
            )

            HudTextField(
                label = "Ataque / manobra",
                value = transport.attack,
                placeholder = "Ex.: coice, 1d8 + FOR",
                enabled = editable,
            ) { onUpdate(transport.copy(attack = it)) }

            HudTextField(
                label = "Observações",
                value = transport.notes,
                multiline = true,
                enabled = editable,
            ) { onUpdate(transport.copy(notes = it)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransportPresetDialog(
    onDismiss: () -> Unit,
    onSelect: (CharacterTransport) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf<TransportCategory?>(null) }
    val filtered = remember(selectedCategory) {
        if (selectedCategory == null) transportPresets
        else transportPresets.filter { it.category == selectedCategory }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exemplos de Transporte") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SdoFilterChip(
                        label = "Todos",
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                    )
                    listOf(
                        TransportCategory.MOUNT,
                        TransportCategory.FLYING_MOUNT,
                        TransportCategory.VEHICLE,
                        TransportCategory.WATERCRAFT,
                    ).forEach { category ->
                        SdoFilterChip(
                            label = category.label,
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
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
                                "${preset.category.label} • ${preset.size} • ${preset.movement} • +${preset.loadCapacity} Carga",
                                color = MaterialTheme.colorScheme.secondary,
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
