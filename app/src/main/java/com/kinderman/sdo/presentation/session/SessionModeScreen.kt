package com.kinderman.sdo.presentation.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AbilityCost
import com.kinderman.sdo.domain.model.BodyRegionSlot
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ConditionInstance
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SessionResource
import com.kinderman.sdo.domain.model.allCanonicalAbilitiesSafely
import com.kinderman.sdo.domain.model.canonicalBodyState
import com.kinderman.sdo.domain.model.canonicalConditions
import com.kinderman.sdo.domain.model.localProtectionBreakdown
import com.kinderman.sdo.domain.model.LoadCondition
import com.kinderman.sdo.domain.model.InventoryState
import com.kinderman.sdo.domain.model.inventoryState
import com.kinderman.sdo.domain.model.effectiveLoad
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.AcidCyan
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.presentation.character.CharacterActionButton
import com.kinderman.sdo.presentation.character.CharacterActionStyle

private data class SessionAbilityEntry(val ability: Ability, val description: String)
private data class SessionResourceValue(
    val label: String,
    val current: Int,
    val maximum: Int,
    val resource: SessionResource,
    val operationType: SessionOperationType = SessionOperationType.RESOURCE,
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SessionModeScreen(
    characters: List<Character>,
    selectedId: String?,
    compact: Boolean,
    readOnly: Boolean,
    onSelect: (String) -> Unit,
    onOpenSheet: (String) -> Unit,
    onCommand: (Character, SessionCommand) -> Unit,
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
                SessionContent(character, compact, readOnly, Modifier.padding(padding), onCommand)
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
                Text("Escolha uma ficha para a sessão", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                Text("Ajustes rápidos são salvos neste aparelho e entram na fila de sincronização.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(characters, key = Character::id) { character ->
            TechPanel(modifier = Modifier.fillMaxWidth().clickable { onSelect(character.id) }) {
                Column(Modifier.fillMaxWidth()) {
                    Text(character.name.ifBlank { "Personagem sem nome" })
                    Text(
                        "VIDA ${character.life.current}/${character.lifeMaximum}  //  ${if (character.dirty) "LOCAL_DELTA" else "SYNC_OK"}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        if (characters.isEmpty()) item { Text("Nenhuma ficha disponível.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SessionContent(
    character: Character,
    compact: Boolean,
    readOnly: Boolean,
    modifier: Modifier,
    onCommand: (Character, SessionCommand) -> Unit,
) {
    var damageDialog by remember { mutableStateOf(false) }
    var healingDialog by remember { mutableStateOf(false) }
    var pendingAbilityId by remember { mutableStateOf<String?>(null) }
    val spacing = if (compact) 8.dp else 14.dp
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(if (compact) 12.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        item {
            TechPanel(accent = if (character.dirty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) {
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
                SectionHeader("00", "Resumo operacional")
                Text(
                    "VIDA ${character.life.current}/${character.lifeMaximum} // ENERGIA ${character.energy.current}/${character.energyMaximum} // CARGA ${character.currentLoad}/${character.maximumLoad}",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                when (character.loadCondition) {
                    LoadCondition.OVERLOADED -> Text("SOBRECARREGADO // MOVIMENTO −5 m // ESQUIVA −2 // CORRIDA +1 PE // DESVANTAGEM: Movimento, Furtividade e Atletismo", color = MaterialTheme.colorScheme.error)
                    LoadCondition.IMMOBILE -> Text("IMÓVEL // MOVIMENTO E ESQUIVA INDISPONÍVEIS", color = MaterialTheme.colorScheme.error)
                    else -> Unit
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CharacterActionButton("Aplicar dano", !readOnly, CharacterActionStyle.PRIMARY, Modifier.weight(1f)) { damageDialog = true }
                    CharacterActionButton("Aplicar cura", !readOnly, CharacterActionStyle.SECONDARY, Modifier.weight(1f)) { healingDialog = true }
                }
                if (readOnly) Text("SOMENTE LEITURA // controles operacionais bloqueados", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
        item {
            TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                SectionHeader("01", "Recursos rápidos")
                val resources = listOf(
                    SessionResourceValue("VIDA", character.life.current, character.lifeMaximum, SessionResource.LIFE),
                    SessionResourceValue("SANIDADE", character.sanity.current, character.sanityMaximum, SessionResource.SANITY),
                    SessionResourceValue("ARCANO", character.arcane.current, character.arcaneMaximum, SessionResource.ARCANE),
                    SessionResourceValue("ENERGIA", character.energy.current, character.energyMaximum, SessionResource.ENERGY),
                    SessionResourceValue("DESTINO", character.destiny.current, character.destinyMaximum, SessionResource.DESTINY, SessionOperationType.DESTINY),
                    SessionResourceValue("EXAUSTÃO", character.exhaustion.current, character.exhaustion.maximum, SessionResource.EXHAUSTION),
                    SessionResourceValue("CORRUPÇÃO", character.corruption.current, character.corruption.maximum, SessionResource.CORRUPTION),
                )
                resources.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { resource ->
                            ResourceControl(resource.label, resource.current, resource.maximum, !readOnly, compact, Modifier.weight(1f)) { next ->
                                onCommand(character, SessionCommand(type = resource.operationType, resource = resource.resource, amount = next - resource.current))
                            }
                        }
                        if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            TechPanel {
                SectionHeader("02", "Proteções")
                character.calculatedProtections().forEach { (name, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value.toString(), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item {
            TechPanel(accent = MaterialTheme.colorScheme.error) {
                SectionHeader("03", "Corpo")
                character.canonicalBodyState().regions.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { region ->
                            Column(
                                Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant).padding(if (compact) 6.dp else 9.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(region.name.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                                Text("${region.state.label.uppercase()} // ${region.failures}/4", style = MaterialTheme.typography.labelSmall, color = if (region.failures > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                Text("PL ${character.localProtection(region)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            val readyItems = character.inventory.filter {
                it.inventoryState in setOf(InventoryState.WIELDED, InventoryState.EQUIPPED, InventoryState.QUICK_ACCESS) ||
                    it.category.equals("Munição", true) || it.linkedAshId.isNotBlank()
            }
            TechPanel {
                SectionHeader("04", "Equipamento pronto")
                if (readyItems.isEmpty()) Text("Nenhum equipamento operacional.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                readyItems.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name.ifBlank { "Item sem nome" }, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
                            Text(
                                buildList {
                                    add(item.inventoryState.label.uppercase())
                                    add("CARGA ${item.effectiveLoad()}")
                                    if (item.category.equals("Munição", true) || item.linkedAshId.isNotBlank()) add("QTD ${item.quantity}")
                                }.joinToString(" // "),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        TelemetryTag(item.category.ifBlank { "ITEM" }.uppercase())
                    }
                }
                Text("Consulta apenas // altere equipamento e munição na ficha completa", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
        item {
            val entries = character.allCanonicalAbilitiesSafely()
                .sortedWith(compareByDescending<Ability> { it.favorite }.thenBy { it.name })
                .map { ability -> SessionAbilityEntry(ability, buildString {
                    append(if (ability.favorite) "★ " else "")
                    append(ability.name)
                    append(" // ")
                    append(abilityCostLabel(ability.cost))
                    if (ability.effect.isNotBlank()) append("\n${ability.effect}")
                    ability.mechanicalEffect?.operations.orEmpty().forEach { operation ->
                        append("\n• ")
                        append(operation.sessionLabel())
                    }
                }) }
            TechPanel(accent = MaterialTheme.colorScheme.secondary) {
                SectionHeader("05", "Poderes // Magias // Runas // Cinzas")
                if (entries.isEmpty()) Text("Nenhuma habilidade cadastrada na ficha.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                entries.forEach { entry ->
                    SessionAbilityCard(entry, compact, !readOnly) { pendingAbilityId = entry.ability.id }
                }
            }
        }
        item {
            val conditions = character.canonicalConditions()
            TechPanel(accent = if (conditions.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) {
                SectionHeader("06", "Condições // ${conditions.size}")
                if (conditions.isEmpty()) Text("Nenhuma condição ativa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                conditions.forEach { condition ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(condition.sessionLabel(), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        TextButton(
                            enabled = !readOnly,
                            onClick = {
                                onCommand(
                                    character,
                                    SessionCommand(
                                        type = SessionOperationType.CONDITION_REMOVE,
                                        conditionId = condition.instanceId,
                                    ),
                                )
                            },
                        ) { Text("REMOVER") }
                    }
                }
            }
        }
    }

    if (damageDialog) DamageDialog(character, onDismiss = { damageDialog = false }) { amount, region ->
        damageDialog = false
        onCommand(character, SessionCommand(type = SessionOperationType.DAMAGE, amount = amount, bodyRegion = region))
    }
    if (healingDialog) AmountDialog("APLICAR CURA", character.life.current, character.lifeMaximum, { healingDialog = false }) { amount ->
        healingDialog = false
        onCommand(character, SessionCommand(type = SessionOperationType.HEAL, resource = SessionResource.LIFE, amount = amount))
    }
    pendingAbilityId?.let { id ->
        val ability = character.allCanonicalAbilitiesSafely().firstOrNull { it.id == id }
        val name = ability?.name.orEmpty()
        val cost = ability?.let { abilityCostLabel(it.cost) }.orEmpty()
        AlertDialog(
            onDismissRequest = { pendingAbilityId = null },
            title = { Text("CONFIRMAR USO") },
            text = { Text("$name // ${cost.ifBlank { "sem custo" }}. O custo será descontado antes da ação e o recurso nunca ficará negativo.") },
            confirmButton = { TextButton({ pendingAbilityId = null; onCommand(character, SessionCommand(type = SessionOperationType.ABILITY_USE, targetId = id)) }) { Text("USAR") } },
            dismissButton = { TextButton({ pendingAbilityId = null }) { Text("CANCELAR") } },
        )
    }
}

@Composable
private fun SessionAbilityCard(entry: SessionAbilityEntry, compact: Boolean, editable: Boolean, onUse: () -> Unit) {
    var expanded by rememberSaveable(entry.ability.id) { mutableStateOf(false) }
    val accent = if (entry.ability.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = .7f), CutCornerShape(topEnd = 10.dp, bottomStart = 8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(if (compact) 7.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 7.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text((if (entry.ability.favorite) "★ " else "") + entry.ability.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
                Text("${entry.ability.kind.label.uppercase()} // ${abilityCostLabel(entry.ability.cost)}", color = accent, style = MaterialTheme.typography.labelSmall)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Recolher habilidade" else "Ver detalhes")
        }
        if (expanded) {
            Text(entry.description.substringAfter('\n', entry.ability.effect.ifBlank { "Sem descrição adicional." }), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            CharacterActionButton(
                label = if (entry.ability.available) "Usar habilidade" else "Indisponível",
                enabled = editable && entry.ability.available,
                style = CharacterActionStyle.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
                onClick = onUse,
            )
        }
    }
}

@Composable
private fun ResourceControl(
    label: String,
    current: Int,
    maximum: Int,
    enabled: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    Column(
        modifier.background(MaterialTheme.colorScheme.surfaceVariant, CutCornerShape(topEnd = 8.dp, bottomStart = 6.dp)).padding(if (compact) 5.dp else 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ onChange(current - 1) }, enabled = enabled && current > 0, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Remove, "Reduzir $label") }
            Text("$current/$maximum", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            IconButton({ onChange(current + 1) }, enabled = enabled && current < maximum, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, "Aumentar $label") }
        }
    }
}

@Composable
private fun DamageDialog(character: Character, onDismiss: () -> Unit, onConfirm: (Int, BodyRegionSlot) -> Unit) {
    var amount by remember { mutableIntStateOf(1) }
    var regionIndex by remember { mutableIntStateOf(0) }
    val regions = character.canonicalBodyState().regions
    val region = regions.getOrElse(regionIndex) { character.canonicalBodyState().region(BodyRegionSlot.Torso) }
    val protection = character.localProtection(region)
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
                    TextButton({ regionIndex = (regionIndex - 1).floorMod(regions.size) }) { Text("‹") }
                    Text(region.name, modifier = Modifier.weight(1f).align(Alignment.CenterVertically))
                    TextButton({ regionIndex = (regionIndex + 1).floorMod(regions.size) }) { Text("›") }
                }
                Text("3. P.L. local: $protection")
                Text("4. Resultado: $amount − $protection = $applied de Vida", color = if (applied > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text("Nada é alterado antes da confirmação.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton({ onConfirm(amount, region.region) }) { Text("CONFIRMAR") } },
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

private fun Int.floorMod(divisor: Int): Int = if (divisor <= 0) 0 else Math.floorMod(this, divisor)

private fun abilityCostLabel(cost: AbilityCost): String = when (cost) {
    is AbilityCost.PowerCost -> if (cost.amount == 0) "SEM CUSTO" else "${cost.amount} ${cost.resource}"
    is AbilityCost.SpellCost -> "${cost.amount} PM"
    is AbilityCost.RuneCost -> "${cost.amount} PM"
    is AbilityCost.AshCost -> "${cost.amount} dose(s)"
}

private fun com.kinderman.sdo.domain.model.EffectOperation.sessionLabel(): String = when (this) {
    is com.kinderman.sdo.domain.model.EffectOperation.Damage -> "Dano ${damageType.label} em ${target.kind}"
    is com.kinderman.sdo.domain.model.EffectOperation.Healing -> "Recupera ${amount} de ${resource}"
    is com.kinderman.sdo.domain.model.EffectOperation.ApplyCondition -> "Aplica ${condition.kind.label}"
    is com.kinderman.sdo.domain.model.EffectOperation.AddModifier -> "Modificador ${modifier.target.kind} ${if (modifier.amount > 0) "+" else ""}${modifier.amount}"
    is com.kinderman.sdo.domain.model.EffectOperation.SpendResource -> "Gasta ${amount} ${resource}"
    is com.kinderman.sdo.domain.model.EffectOperation.ConsumeItemState -> "Consome ${amount} ${state}"
}

private fun ConditionInstance.sessionLabel(): String = buildString {
    append(name)
    intensity?.let { append(" // intensidade $it") }
    duration?.let { append(" // ${it.kind.label}"); it.turns?.let { turns -> append(" ($turns turno(s))") } }
}
