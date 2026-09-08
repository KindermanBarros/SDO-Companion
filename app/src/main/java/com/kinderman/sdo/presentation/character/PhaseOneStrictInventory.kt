package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemBonus
import com.kinderman.sdo.domain.model.ItemBonusType
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.initialCreationCost
import com.kinderman.sdo.domain.model.participatesInInitialCreation
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.Carbon
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechPanel

@Composable
internal fun PhaseOneStrictInventorySection(
    character: Character,
    catalog: List<CatalogEntry>,
    enabled: Boolean,
    onChange: (Character) -> Unit,
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val spentHeritage = character.inventory.sumOf { it.initialCreationCost() }
    val hasInitialShopping = character.inventory.any { it.participatesInInitialCreation() }
    val remainingHeritage = when {
        hasInitialShopping || character.inventory.isEmpty() -> (ItemCreationRules.HERITAGE_BUDGET - spentHeritage).coerceAtLeast(0)
        else -> 0
    }
    val acquiredTargets = character.learnedKnowledges
        .map(SpecialKnowledge::name)
        .filter(String::isNotBlank)
        .distinct()

    TechPanel {
        SectionHeader("09", "Inventário")
        Text(
            "CARGA ${character.currentLoad} / ${character.maximumLoad}",
            color = if (character.currentLoad > character.maximumLoad) Signal else Acid,
            style = MaterialTheme.typography.titleLarge,
        )
        Text("Bônus mecânicos usam somente seletores controlados de tipo e destino.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        IntegerField("Capacidade do recipiente equipado", character.containerCapacity, enabled) {
            onChange(character.copy(containerCapacity = it.coerceAtLeast(0)))
        }

        character.inventory.forEachIndexed { index, item ->
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text(item.name.ifBlank { "ITEM ${(index + 1).toString().padStart(2, '0')}" }, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    RemoveButton(enabled, "Remover item") { onChange(character.removeInventoryItem(item.id)) }
                }
                Text("${item.category.ifBlank { "OBJETO" }} // ${item.quality} // PG ${item.pg} // PL ${item.pl}", color = Acid, style = MaterialTheme.typography.labelSmall)
                Text("REGIÃO ${item.region.ifBlank { "—" }} // CARGA ${item.load} // LA ${item.agilityLimit ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (item.bonuses.isNotEmpty()) {
                    item.bonuses.forEach { bonus ->
                        Text(
                            "${bonus.type.label}: ${if (bonus.value >= 0) "+" else ""}${bonus.value} ${bonus.displayTarget()}",
                            color = Acid,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                HudTextField("Estado E/R/M/G", item.state, enabled = enabled) { value ->
                    onChange(character.copy(inventory = character.inventory.replace(index, item.copy(state = value.uppercase().take(1)))))
                }
                HudTextField("Efeito", item.effect, multiline = true, enabled = enabled) { value ->
                    onChange(character.copy(inventory = character.inventory.replace(index, item.copy(effect = value))))
                }
            }
        }

        AddButton("Glossário de itens e materiais", true) { dialog = "glossary" }
        if (remainingHeritage > 0) {
            Text("CRIAÇÃO INICIAL // $remainingHeritage / ${ItemCreationRules.HERITAGE_BUDGET} PH RESTANTES", color = Acid)
            AddButton("Selecionar item pronto", enabled && catalog.isNotEmpty()) { dialog = "initial_catalog" }
            AddButton("Construir item com PH", enabled) { dialog = "initial_builder" }
        } else {
            AddButton("Selecionar item do catálogo", enabled && catalog.isNotEmpty()) { dialog = "catalog" }
            AddButton("Construtor de item", enabled) { dialog = "builder" }
            AddButton("Adicionar objeto narrativo", enabled) {
                onChange(character.copy(inventory = character.inventory + InventoryItem()))
            }
        }
    }

    when (dialog) {
        "glossary" -> EquipmentGlossaryDialog { dialog = null }
        "initial_catalog" -> ItemCatalogDialog(
            title = "LOJA INICIAL // ITENS PRONTOS",
            entries = catalog,
            remainingHeritage = remainingHeritage,
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.copy(inventory = character.inventory + item))
            dialog = null
        }
        "catalog" -> ItemCatalogDialog(
            title = "CATÁLOGO DE ITENS",
            entries = catalog,
            remainingHeritage = null,
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.copy(inventory = character.inventory + item))
            dialog = null
        }
        "initial_builder" -> StrictItemBuilderDialog(
            remainingHeritage = remainingHeritage,
            acquiredKnowledgeTargets = acquiredTargets,
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.copy(inventory = character.inventory + item))
            dialog = null
        }
        "builder" -> StrictItemBuilderDialog(
            remainingHeritage = null,
            acquiredKnowledgeTargets = acquiredTargets,
            onDismiss = { dialog = null },
        ) { item ->
            onChange(character.copy(inventory = character.inventory + item))
            dialog = null
        }
    }
}

@Composable
private fun StrictItemBuilderDialog(
    remainingHeritage: Int?,
    acquiredKnowledgeTargets: List<String>,
    onDismiss: () -> Unit,
    onAdd: (InventoryItem) -> Unit,
) {
    var weapon by remember { mutableStateOf(true) }
    var base by remember { mutableStateOf(ItemCreationRules.weaponBases.first()) }
    var material by remember { mutableStateOf(ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" }) }
    var modifications by remember { mutableStateOf(emptyList<ItemPart>()) }
    var gemSlots by remember { mutableIntStateOf(0) }
    var technologySlots by remember { mutableIntStateOf(0) }
    var customName by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf(ItemQuality.COMMON) }
    var bonuses by remember { mutableStateOf(emptyList<ItemBonus>()) }
    var gems by remember { mutableStateOf(emptyList<ItemPart>()) }
    var manualPrice by remember { mutableStateOf("") }

    val initialCreation = remainingHeritage != null
    val bases = if (weapon) ItemCreationRules.weaponBases else ItemCreationRules.armorBases
    val materials = when {
        weapon -> ItemCreationRules.weaponMaterials
        base.id == "gibao" -> ItemCreationRules.armorMaterials.filter { it.id == "organico" }
        else -> ItemCreationRules.armorMaterials
    }.let { list -> if (initialCreation) list.filter { it.creationCost != null } else list }
    val availableModifications = if (weapon) ItemCreationRules.weaponModifications else ItemCreationRules.armorModifications
    val built = ItemCreationRules.build(
        base = base,
        material = material,
        modifications = modifications,
        gemSlots = gemSlots,
        technologySlots = technologySlots,
        customName = customName,
        quality = quality,
        bonuses = bonuses,
        components = gems,
        priceOverride = manualPrice.toIntOrNull().takeIf { !initialCreation },
    )
    val allowedByBudget = remainingHeritage == null || (built.creationCost != null && built.creationCost <= remainingHeritage)
    val requiresPrice = !initialCreation && built.creationCost == null && manualPrice.isBlank()
    val bonusesComplete = bonuses.all(ItemBonus::isComplete)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCreation) "CONSTRUTOR // CRIAÇÃO INICIAL" else "CONSTRUTOR DE ITEM") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 590.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                remainingHeritage?.let { Text("PH RESTANTES // $it", color = Acid) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = {
                        weapon = true
                        base = ItemCreationRules.weaponBases.first()
                        material = ItemCreationRules.weaponMaterials.first { it.id == "ligas_comuns" }
                        modifications = emptyList()
                    }) { Text(if (weapon) "[ ARMA ]" else "ARMA") }
                    TextButton(onClick = {
                        weapon = false
                        base = ItemCreationRules.armorBases.first()
                        material = ItemCreationRules.armorMaterials.first { it.id == "ligas_comuns" }
                        modifications = emptyList()
                    }) { Text(if (!weapon) "[ ARMADURA / ACESSÓRIO ]" else "ARMADURA / ACESSÓRIO") }
                }
                HudTextField("Nome personalizado", customName) { customName = it }
                CyclePartButton("TIPO", base, bases) { selected ->
                    base = selected
                    if (!weapon && selected.id == "gibao") {
                        material = ItemCreationRules.armorMaterials.first { it.id == "organico" }
                    }
                }
                CyclePartButton("MATERIAL", material, materials) { material = it }
                TextButton(
                    onClick = { quality = ItemQuality.entries[(quality.ordinal + 1) % ItemQuality.entries.size] },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("QUALIDADE // ${quality.label}") }

                Text("MODIFICAÇÕES", color = Acid, style = MaterialTheme.typography.labelLarge)
                availableModifications.forEach { modification ->
                    val checked = modification in modifications
                    Row(Modifier.fillMaxWidth().clickable { modifications = strictToggleModification(modifications, modification) }) {
                        Checkbox(checked, onCheckedChange = { modifications = strictToggleModification(modifications, modification) })
                        Column(Modifier.padding(top = 8.dp)) {
                            Text(modification.name, color = Ice)
                            if (modification.effect.isNotBlank()) Text(modification.effect, color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                TwoFields(
                    { IntegerField("Espaços de Gema", gemSlots, true, it) { value -> gemSlots = value.coerceIn(gems.size, 5) } },
                    { IntegerField("Espaços de Tecnologia", technologySlots, true, it) { value -> technologySlots = value.coerceIn(0, 5) } },
                )
                Text("GEMAS", color = Acid, style = MaterialTheme.typography.labelLarge)
                ItemCreationRules.gemComponents.forEach { gem ->
                    val checked = gem in gems
                    Row(Modifier.fillMaxWidth().clickable(enabled = quality != ItemQuality.MUNDANE) {
                        gems = if (checked) gems - gem else if (gems.size < 5) gems + gem else gems
                        gemSlots = gemSlots.coerceAtLeast(gems.size)
                    }) {
                        Checkbox(checked, enabled = quality != ItemQuality.MUNDANE, onCheckedChange = {
                            gems = if (checked) gems - gem else if (gems.size < 5) gems + gem else gems
                            gemSlots = gemSlots.coerceAtLeast(gems.size)
                        })
                        Text(gem.name, color = Ice, modifier = Modifier.padding(top = 10.dp))
                    }
                }

                Text("BÔNUS CONCEDIDOS AO EQUIPAR", color = Acid, style = MaterialTheme.typography.labelLarge)
                bonuses.forEachIndexed { index, bonus ->
                    val targets = strictBonusTargets(bonus.type, acquiredKnowledgeTargets)
                    Column(Modifier.fillMaxWidth().background(Carbon).padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = {
                                val nextType = ItemBonusType.entries[(bonus.type.ordinal + 1) % ItemBonusType.entries.size]
                                val target = strictBonusTargets(nextType, acquiredKnowledgeTargets).firstOrNull().orEmpty()
                                bonuses = bonuses.replace(index, bonus.copy(type = nextType, target = target))
                            }) { Text("TIPO // ${bonus.type.label.uppercase()}") }
                            TextButton(onClick = { bonuses = bonuses.filterIndexed { itemIndex, _ -> itemIndex != index } }) {
                                Text("REMOVER", color = Signal)
                            }
                        }
                        TextButton(
                            enabled = targets.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val current = targets.indexOf(bonus.target)
                                val next = targets[(current.coerceAtLeast(-1) + 1) % targets.size]
                                bonuses = bonuses.replace(index, bonus.copy(target = next))
                            },
                        ) {
                            Text("APLICAR EM // ${bonus.displayTarget().ifBlank { if (targets.isEmpty()) "SEM OPÇÕES" else "SELECIONAR" }}")
                        }
                        IntegerField("Valor (-5 a +5)", bonus.value, true) { value ->
                            bonuses = bonuses.replace(index, bonus.copy(value = value.coerceIn(-5, 5)))
                        }
                        if (bonus.target.isBlank()) {
                            Text(
                                if (bonus.type == ItemBonusType.ACQUIRED_KNOWLEDGE) "A ficha não possui Conhecimentos Adquiridos disponíveis para este bônus."
                                else "Selecione um destino para o bônus.",
                                color = Signal,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { bonuses = bonuses + ItemBonus(type = ItemBonusType.ATTRIBUTE, target = "FOR") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("+ ADICIONAR BÔNUS") }

                if (!initialCreation) {
                    HudTextField("Preço final em E$", manualPrice) { manualPrice = it.filter(Char::isDigit) }
                }
                Text("CUSTO // ${built.creationCost ?: "#"} PH // PREÇO ${built.price} E$", color = Acid)
                Text("PG ${built.pg} // PL ${built.pl} // LA ${built.agilityLimit ?: "—"}", color = Ice)
                Text("CARGA ${built.load} // DURABILIDADE ${built.durability}", color = Muted)
                if (!allowedByBudget) Text("Custo acima dos PH restantes ou item # não disponível na criação inicial.", color = Signal)
                if (requiresPrice) Text("Este material exige preço manual.", color = Signal)
                if (!bonusesComplete) Text("Todos os bônus precisam de um destino válido.", color = Signal)
            }
        },
        confirmButton = {
            TextButton(
                enabled = allowedByBudget && !requiresPrice && bonusesComplete,
                onClick = { onAdd(built.toInventoryItem(initialCreation = initialCreation)) },
            ) { Text("ADICIONAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun CyclePartButton(label: String, current: ItemPart, options: List<ItemPart>, onSelected: (ItemPart) -> Unit) {
    TextButton(
        enabled = options.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            if (options.isEmpty()) return@TextButton
            val currentIndex = options.indexOfFirst { it.id == current.id }
            onSelected(options[(currentIndex.coerceAtLeast(-1) + 1) % options.size])
        },
    ) { Text("$label // ${current.name}") }
}

private fun strictBonusTargets(type: ItemBonusType, acquiredKnowledgeTargets: List<String>): List<String> = when (type) {
    ItemBonusType.ATTRIBUTE -> listOf("FOR", "VIG", "AGI", "POD", "INT", "CAR")
    ItemBonusType.BASIC_KNOWLEDGE -> listOf(
        "FOR" to listOf("Atletismo", "Brutalidade", "Luta", "Arremesso"),
        "VIG" to listOf("Energia", "Vitalidade", "Tolerância", "Regeneração"),
        "AGI" to listOf("Furtividade", "Reflexos", "Movimento", "Pontaria"),
        "POD" to listOf("Arcano", "Sentidos", "Controle", "Recuperação"),
        "INT" to listOf("Sanidade", "Intuição", "Religião", "Raciocínio"),
        "CAR" to listOf("Política", "Lábia", "Enganação", "Intimidação"),
    ).flatMap { (attribute, skills) -> skills.map { ItemBonus.basicKnowledgeTarget(attribute, it) } }
    ItemBonusType.ACQUIRED_KNOWLEDGE -> acquiredKnowledgeTargets
}

private fun strictToggleModification(current: List<ItemPart>, item: ItemPart): List<ItemPart> {
    if (item in current) return current - item
    var next = current + item
    if (item.id == "nobre") next = next.filterNot { it.id == "chamativa" }
    if (item.id == "chamativa") next = next.filterNot { it.id == "nobre" }
    if (item.id == "sob_medida" && next.none { it.id == "ajustada" }) {
        next = next + ItemCreationRules.armorModifications.first { it.id == "ajustada" }
    }
    if (item.id == "ajustada" && current.any { it.id == "sob_medida" }) return current
    return next
}
