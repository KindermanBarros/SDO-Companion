package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.addInventoryItem
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.InventoryState
import com.kinderman.sdo.domain.model.inventoryState
import com.kinderman.sdo.domain.model.durabilityLabel
import com.kinderman.sdo.domain.model.withInventoryState
import com.kinderman.sdo.domain.model.initialCreationCost
import com.kinderman.sdo.domain.model.participatesInInitialCreation
import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.catalog.withPathPreset
import com.kinderman.sdo.domain.catalog.toMysticAbility
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.OrganStatus
import com.kinderman.sdo.domain.model.AbilityCostType
import com.kinderman.sdo.domain.model.AbilityDuration
import com.kinderman.sdo.domain.model.AbilityExecution
import com.kinderman.sdo.domain.model.AbilityRange
import com.kinderman.sdo.domain.model.AbilityResistance
import com.kinderman.sdo.domain.model.AbilitySource
import com.kinderman.sdo.domain.model.AbilityTimeUnit
import com.kinderman.sdo.domain.model.AshPurity
import com.kinderman.sdo.domain.model.AshSource
import com.kinderman.sdo.domain.model.formattedAbilityCost
import com.kinderman.sdo.domain.model.formattedAbilityExecution
import com.kinderman.sdo.domain.model.withAddedAbility
import com.kinderman.sdo.domain.model.withRemovedAbility
import com.kinderman.sdo.domain.model.withUpdatedAbility
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.AcidCyan
import com.kinderman.sdo.ui.ArcanePanel
import com.kinderman.sdo.ui.Carbon
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.LabelFunctional
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.TechCutDark
import com.kinderman.sdo.ui.TechPanel

@Composable
internal fun PathSection(character: Character, catalog: List<CatalogEntry>, enabled: Boolean, onChange: (Character) -> Unit) {
    var selecting by remember { mutableStateOf(false) }
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        SectionHeader("07", "Caminho")
        HudTextField("Nome do Caminho", character.pathName, enabled = enabled) {
            onChange(character.copy(pathName = it))
        }
        AddButton("Preencher pelo catálogo", enabled && catalog.isNotEmpty()) { selecting = true }
        HudTextField("Lema", character.pathMotto, enabled = enabled) { onChange(character.copy(pathMotto = it)) }
        Text("PALAVRAS-CHAVE // 3", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        character.pathKeywords.forEachIndexed { index, keyword ->
            HudTextField("Palavra-chave ${index + 1}", keyword, enabled = enabled) { onChange(character.copy(pathKeywords = character.pathKeywords.replace(index, it))) }
        }
        Text("PILARES // 3", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        character.pathPillars.forEachIndexed { index, pillar ->
            HudTextField("Pilar ${index + 1}", pillar, multiline = true, enabled = enabled) { onChange(character.copy(pathPillars = character.pathPillars.replace(index, it))) }
        }
    }
    if (selecting) CatalogPickerDialog("SELECIONAR CAMINHO", catalog, { selecting = false }) { entry ->
        onChange(character.withPathPreset(entry))
        selecting = false
    }
}

@Composable
internal fun InventorySection(character: Character, catalog: List<CatalogEntry>, enabled: Boolean, onChange: (Character) -> Unit) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val spentHeritage = character.inventory.sumOf { it.initialCreationCost() }
    val hasInitialShopping = character.inventory.any { it.participatesInInitialCreation() }
    val remainingHeritage = when {
        hasInitialShopping || character.inventory.isEmpty() -> (ItemCreationRules.HERITAGE_BUDGET - spentHeritage).coerceAtLeast(0)
        else -> 0 // Personagens anteriores ao fluxo de PH permanecem válidos.
    }
    LaunchedEffect(remainingHeritage) {
        if (remainingHeritage > 0 && dialog == null) dialog = "initial"
    }
    TechPanel {
        SectionHeader("09", "Inventário")
        Text("CARGA ${character.currentLoad} / ${character.maximumLoad}", color = if (character.currentLoad > character.maximumLoad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleLarge)
        Text("Máxima = 2 + FOR + capacidade do recipiente. Itens [G] não contam como carregados.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        if (remainingHeritage > 0) {
            Text("CRIAÇÃO INICIAL OBRIGATÓRIA // $remainingHeritage / ${ItemCreationRules.HERITAGE_BUDGET} PH RESTANTES", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("Finalize os PH para liberar o catálogo comum, o construtor livre e itens manuais.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        character.inventory.forEachIndexed { index, item ->
            InventoryEditor(index, item, enabled,
                onRemove = { onChange(character.removeInventoryItem(item.id)) },
                onValue = { onChange(character.copy(inventory = character.inventory.replace(index, it))) },
            )
        }
        AddButton("Glossário de itens e materiais", true) { dialog = "glossary" }
        if (remainingHeritage > 0) {
            AddButton("Loja inicial // comprar ou construir com PH", enabled) { dialog = "initial" }
        }
        if (remainingHeritage == 0) {
            AddButton("Catálogo de itens // fora da criação", enabled && catalog.isNotEmpty()) { dialog = "catalog" }
            AddButton("Construtor de item // criação durante o jogo", enabled) { dialog = "builder" }
            AddButton("Adicionar objeto narrativo sem valores mecânicos", enabled) { onChange(character.addInventoryItem(InventoryItem())) }
        }
    }
    when (dialog) {
        "glossary" -> EquipmentGlossaryDialog { dialog = null }
        "initial" -> InitialShopDialog(
            remainingHeritage = remainingHeritage,
            catalogAvailable = catalog.isNotEmpty(),
            onDismiss = { dialog = null },
            onCatalog = { dialog = "initial_catalog" },
            onBuilder = { dialog = "initial_builder" },
        )
        "initial_builder" -> ItemBuilderDialog(remainingHeritage, { dialog = null }) { item ->
            onChange(character.addInventoryItem(item))
            dialog = null
        }
        "builder" -> ItemBuilderDialog(null, { dialog = null }) { item ->
            onChange(character.addInventoryItem(item))
            dialog = null
        }
        "initial_catalog" -> ItemCatalogDialog("LOJA INICIAL // ITENS PRONTOS", catalog, remainingHeritage, { dialog = "initial" }) { item ->
            onChange(character.addInventoryItem(item))
            dialog = null
        }
        "catalog" -> ItemCatalogDialog("CATÁLOGO DE ITENS", catalog, null, { dialog = null }) { item ->
            onChange(character.addInventoryItem(item))
            dialog = null
        }
    }
}

@Composable
private fun InventoryEditor(index: Int, item: InventoryItem, enabled: Boolean, onRemove: () -> Unit, onValue: (InventoryItem) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("ITEM ${(index + 1).toString().padStart(2, '0')}", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            RemoveButton(enabled, "Remover item", onRemove)
        }
        HudTextField("Nome", item.name, enabled = enabled) { onValue(item.copy(name = it)) }
        TwoFields(
            { ChoiceField("Estado", item.inventoryState, InventoryState.entries, enabled, it, InventoryState::label) { value -> onValue(item.withInventoryState(value)) } },
            { IntegerField("Carga", item.load, enabled, it) { value -> onValue(item.copy(load = value.coerceAtLeast(0))) } },
        )
        IntegerField("Durabilidade atual", item.durabilityCurrent, enabled) { value -> onValue(item.copy(durabilityCurrent = value.coerceIn(0, item.durabilityMax))) }
        Text("${item.category.ifBlank { "OBJETO NARRATIVO" }} // ${item.quality.label.uppercase()} // ${item.durabilityLabel}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        Text("REGIÃO ${item.region.ifBlank { "—" }} // PG ${item.pg} // PL ${item.pl} // LA ${item.agilityLimit ?: "—"}", color = MaterialTheme.colorScheme.onSurface)
        if (item.mechanicalEffects.isNotEmpty()) Text(
            "EFEITOS // " + item.mechanicalEffects.joinToString { "${it.type.name} ${if (it.value > 0) "+" else ""}${it.value} ${it.resolvedTargetId.ifBlank { it.target }}" },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
        HudTextField("Efeito", item.effect, multiline = true, enabled = enabled) { onValue(item.copy(effect = it)) }
    }
}

@Composable
internal fun BodySection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        SectionHeader("10", "Corpo e armadura")
        Text("LA DOS EQUIPAMENTOS // ${character.equippedAgilityLimit ?: "—"}", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun BodyRegionSection(
    character: Character,
    index: Int,
    enabled: Boolean,
    onChange: (Character) -> Unit,
    onSelectEquipment: () -> Unit,
) {
    val region = character.bodyRegions[index]
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        Text("D10.${region.roll.toString().padStart(2, '0')} // ${region.name.uppercase()}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntegerField("Falhas", region.failures, enabled, Modifier.weight(1f)) { value -> onChange(character.copy(bodyRegions = character.bodyRegions.replace(index, region.copy(failures = value.coerceIn(0, 4))))) }
            IntegerField("Ajuste PL", region.localProtection, enabled, Modifier.weight(1f)) { value -> onChange(character.copy(bodyRegions = character.bodyRegions.replace(index, region.copy(localProtection = value.coerceAtLeast(0))))) }
        }
        Text(
            "PL TOTAL ${character.localProtection(region)} // PG DO PERSONAGEM +${character.equippedGeneralProtection}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
        HudTextField("Danos", region.damage, enabled = enabled) { onChange(character.copy(bodyRegions = character.bodyRegions.replace(index, region.copy(damage = it)))) }
        HudTextField("Implantes", region.implants, enabled = enabled) { onChange(character.copy(bodyRegions = character.bodyRegions.replace(index, region.copy(implants = it)))) }
        val equippedNames = character.equippedItems(region).joinToString { it.name.ifBlank { "Item sem nome" } }
        Text("EQUIPAMENTOS // ${equippedNames.ifBlank { "NENHUM" }}", color = if (equippedNames.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
        AddButton("Selecionar equipamentos do inventário", enabled, onSelectEquipment)
        HudTextField("Observações de equipamento", region.equipment, enabled = enabled) { onChange(character.copy(bodyRegions = character.bodyRegions.replace(index, region.copy(equipment = it)))) }
    }
}

@Composable
internal fun OrganSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        SectionHeader("11", "Órgãos")
        if (character.organs.isEmpty()) {
            Text(
                "Registre apenas órgãos com dano, implante, parasita ou outra alteração relevante.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        character.organs.forEachIndexed { index, organ ->
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("ALTERAÇÃO ${(index + 1).toString().padStart(2, '0')}", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                    RemoveButton(enabled, "Remover registro de órgão") {
                        onChange(character.copy(organs = character.organs.filterNot { it.id == organ.id }))
                    }
                }
                HudTextField("Órgão", organ.name, enabled = enabled) { onChange(character.copy(organs = character.organs.replace(index, organ.copy(name = it)))) }
                IntegerField("Falhas", organ.failures, enabled) { onChange(character.copy(organs = character.organs.replace(index, organ.copy(failures = it.coerceIn(0, 3))))) }
                HudTextField("Implante ou parasita", organ.implant, enabled = enabled) { onChange(character.copy(organs = character.organs.replace(index, organ.copy(implant = it)))) }
                HudTextField("Dano / efeito", organ.effect, multiline = true, enabled = enabled) { onChange(character.copy(organs = character.organs.replace(index, organ.copy(effect = it)))) }
            }
            if (index != character.organs.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        AddButton("Adicionar alteração de órgão", enabled) {
            onChange(character.copy(organs = character.organs + OrganStatus()))
        }
    }
}

@Composable
internal fun MysticSection(character: Character, catalog: List<CatalogEntry>, enabled: Boolean, onChange: (Character) -> Unit) {
    var selecting by remember { mutableStateOf(false) }
    var expandedAbilityId by remember(character.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    fun applyChange(block: () -> Character) {
        runCatching(block).onSuccess(onChange).onFailure { android.widget.Toast.makeText(context, it.message, android.widget.Toast.LENGTH_SHORT).show() }
    }
    TechPanel(accent = MaterialTheme.colorScheme.secondary) {
        SectionHeader("12", "Magias, runas e cinzas")
        Text(
            "CATÁLOGO EXPANSÍVEL // exemplos adicionais podem ser incluídos continuamente. Os procedimentos completos estão em Regras Arcanas Expandidas.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        character.mysticAbilities.forEachIndexed { index, ability ->
            MysticEditor(index, ability, character, enabled,
                expanded = expandedAbilityId == ability.id,
                onToggle = { expandedAbilityId = ability.id.takeUnless { it == expandedAbilityId } },
                onRemove = {
                    if (expandedAbilityId == ability.id) expandedAbilityId = null
                    applyChange { character.withRemovedAbility(ability.id) }
                },
                onValue = { value -> applyChange { character.withUpdatedAbility(value.copy(revision = ability.revision + 1)) } },
            )
        }
        AddButton("Selecionar magia, cinza ou runa", enabled && catalog.isNotEmpty()) { selecting = true }
        AddButton("Adicionar efeito manualmente", enabled) {
            val ability = MysticAbility(type = "Magia")
            expandedAbilityId = ability.id
            applyChange { character.withAddedAbility(ability) }
        }
    }
    if (selecting) CatalogPickerDialog("SELECIONAR EFEITO MÍSTICO", catalog, { selecting = false }) { entry ->
        val ability = runCatching { entry.toMysticAbility(character) }
            .getOrElse {
                android.widget.Toast.makeText(context, it.message, android.widget.Toast.LENGTH_SHORT).show()
                return@CatalogPickerDialog
            }
        expandedAbilityId = ability.id
        applyChange { character.withAddedAbility(ability, reuseExistingAsh = true) }
        selecting = false
    }
}

@Composable
private fun MysticEditor(
    index: Int,
    ability: MysticAbility,
    character: Character,
    enabled: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onValue: (MysticAbility) -> Unit,
) {
    val effectiveCostType = when {
        ability.type.equals("Cinza", true) -> AbilityCostType.DOSE
        else -> AbilityCostType.ARCANE
    }
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(ability.name.ifBlank { "EFEITO ${(index + 1).toString().padStart(2, '0')}" }, color = MaterialTheme.colorScheme.onSurface)
                val source = if (ability.type.equals("Cinza", true)) "${ability.ashSource.label} // ${ability.ashPurity.label}" else ability.canonicalSource.label
                Text(
                    listOf(ability.type.ifBlank { "Magia" }, source, formattedAbilityExecution(ability.executionType, ability.timeValue, ability.timeUnit), formattedAbilityCost(effectiveCostType, ability.costValue)).joinToString(" // "),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            TextButton(onClick = onToggle) { Text(if (expanded) "FECHAR" else "EDITAR") }
            RemoveButton(enabled, "Remover efeito", onRemove)
        }
        if (!expanded) {
            Text(ability.effect.ifBlank { "Sem efeito descrito." }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            return@Column
        }
        TwoFields(
            { ChoiceField("Tipo", ability.type.ifBlank { "Magia" }, listOf("Magia", "Runa", "Cinza"), enabled, it) { value ->
                val costType = when (value) {
                    "Cinza" -> AbilityCostType.DOSE
                    else -> AbilityCostType.ARCANE
                }
                onValue(ability.copy(type = value, costType = costType))
            } },
            { HudTextField("Nome", ability.name, it, enabled = enabled) { value -> onValue(ability.copy(name = value)) } },
        )
        if (ability.type.equals("Cinza", true)) {
            TwoFields(
                { ChoiceField("Fonte", ability.ashSource, AshSource.entries, enabled, it, display = { value -> value.label }) { value -> onValue(ability.copy(ashSource = value)) } },
                { ChoiceField("Pureza", ability.ashPurity, AshPurity.entries, enabled, it, display = { value -> value.label }) { value -> onValue(ability.copy(ashPurity = value)) } },
            )
        } else {
            val knowledges = character.learnedKnowledges + character.arcaneKnowledges + character.battleTechniques
            val sourceOptions = AbilitySource.entries.filterNot { it == AbilitySource.KNOWLEDGE && knowledges.isEmpty() }
            ChoiceField("Fonte", ability.canonicalSource, sourceOptions, enabled, display = { it.label }) { source ->
                val selectedKnowledge = knowledges.firstOrNull()
                onValue(ability.copy(
                    canonicalSource = source,
                    knowledgeId = selectedKnowledge?.id.orEmpty().takeIf { source == AbilitySource.KNOWLEDGE }.orEmpty(),
                    knowledgeLevel = if (source == AbilitySource.KNOWLEDGE) 0 else null,
                ))
            }
            if (ability.canonicalSource == AbilitySource.KNOWLEDGE) {
                if (knowledges.isEmpty()) Text("Adicione um Conhecimento à ficha antes de selecionar esta fonte.", color = MaterialTheme.colorScheme.error)
                else {
                    val selected = knowledges.firstOrNull { it.id == ability.knowledgeId } ?: knowledges.first()
                    ChoiceField("Conhecimento", selected, knowledges, enabled, display = { it.name }) {
                        onValue(ability.copy(knowledgeId = it.id, knowledgeLevel = (ability.knowledgeLevel ?: 0).coerceIn(0, it.value)))
                    }
                    ChoiceField("Nível", (ability.knowledgeLevel ?: 0).coerceIn(0, selected.value), (0..selected.value).toList(), enabled) {
                        onValue(ability.copy(knowledgeId = selected.id, knowledgeLevel = it))
                    }
                }
            }
        }
        TwoFields(
            { when {
                ability.type.equals("Cinza", true) -> ChoiceField("Custo", AbilityCostType.DOSE, listOf(AbilityCostType.DOSE), false, it, display = { value -> value.label }) { }
                ability.type.equals("Runa", true) -> ChoiceField("Custo", AbilityCostType.ARCANE, listOf(AbilityCostType.ARCANE), false, it, display = { value -> value.label }) { }
                else -> ChoiceField("Custo", AbilityCostType.ARCANE, listOf(AbilityCostType.ARCANE), false, it, display = { value -> value.label }) { }
            } },
            { if (effectiveCostType != AbilityCostType.NONE) IntegerField("Valor do custo", ability.costValue, enabled, it) { value -> onValue(ability.copy(costType = effectiveCostType, costValue = value.coerceAtLeast(0))) } },
        )
        TwoFields(
            { ChoiceField("Execução", ability.executionType, AbilityExecution.entries.filterNot { value -> value == AbilityExecution.PASSIVE }, enabled, it, display = { value -> value.label }) { value -> onValue(ability.copy(executionType = value, action = value.label)) } },
            { ChoiceField("Alcance", ability.rangeType, AbilityRange.entries, enabled, it, display = { value -> value.label }) { value -> onValue(ability.copy(rangeType = value, range = value.label)) } },
        )
        if (ability.executionType == AbilityExecution.TIME) TwoFields(
            { IntegerField(if (ability.type.equals("Runa", true)) "Tempo de inscrição" else "Tempo de execução", ability.timeValue, enabled, it) { value -> onValue(ability.copy(timeValue = value.coerceAtLeast(0))) } },
            { ChoiceField("Unidade", ability.timeUnit, AbilityTimeUnit.entries, enabled, it, display = { value -> value.label }) { value -> onValue(ability.copy(timeUnit = value)) } },
        )
        HudTextField("Alvo / Área (opcional)", ability.targetArea, enabled = enabled) { onValue(ability.copy(targetArea = it)) }
        TwoFields(
            { ChoiceField("Duração", ability.durationType, AbilityDuration.entries, enabled, it, display = { value -> value.label }) { value -> onValue(ability.copy(durationType = value, duration = value.label)) } },
            { ChoiceField("Resistência", ability.resistance, AbilityResistance.entries, enabled, it, display = { value -> value.label }) { value -> onValue(ability.copy(resistance = value)) } },
        )
        if (ability.durationType == AbilityDuration.TURNS) {
            IntegerField("Quantidade de turnos", ability.durationValue, enabled) { value -> onValue(ability.copy(durationValue = value.coerceAtLeast(0))) }
        }
        if (ability.durationType == AbilityDuration.TIME) TwoFields(
            { IntegerField("Tempo de duração", ability.durationValue, enabled, it) { value -> onValue(ability.copy(durationValue = value.coerceAtLeast(0))) } },
            { ChoiceField("Unidade da duração", ability.durationUnit, listOf(AbilityTimeUnit.HOURS, AbilityTimeUnit.DAYS), enabled, it, display = { value -> value.label }) { value -> onValue(ability.copy(durationUnit = value)) } },
        )
        HudTextField("Efeito", ability.effect, multiline = true, enabled = enabled) { onValue(ability.copy(effect = it)) }
        AbilityAvailabilityEditor(ability.favorite, ability.available, enabled) { favorite, available ->
            onValue(ability.copy(favorite = favorite, available = available))
        }
    }
}

@Composable
internal fun ConditionSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        SectionHeader("13", "Condições")
        character.conditions.forEachIndexed { index, condition ->
            ConditionEditor(index, condition, enabled,
                onRemove = { onChange(character.copy(conditions = character.conditions.filterIndexed { itemIndex, _ -> itemIndex != index })) },
                onValue = { onChange(character.copy(conditions = character.conditions.replace(index, it))) },
            )
        }
        AddButton("Adicionar condição", enabled) { onChange(character.copy(conditions = character.conditions + ConditionEffect())) }
    }
}

@Composable
internal fun MigrationReviewSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        SectionHeader("!", "Correções da migração")
        Text(
            "Campos mecânicos antigos não foram interpretados automaticamente. Corrija o registro correspondente e confirme cada pendência.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        character.migrationReviews.forEachIndexed { index, review ->
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(review.field, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(review.legacyValue, color = MaterialTheme.colorScheme.onSurface)
                Text(review.reason, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                TextButton(
                    onClick = {
                        onChange(character.copy(migrationReviews = character.migrationReviews.filterIndexed { itemIndex, _ -> itemIndex != index }))
                    },
                    enabled = enabled,
                ) { Text("CONFIRMAR CORREÇÃO") }
            }
        }
    }
}

@Composable
private fun ConditionEditor(index: Int, condition: ConditionEffect, enabled: Boolean, onRemove: () -> Unit, onValue: (ConditionEffect) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("CONDIÇÃO ${(index + 1).toString().padStart(2, '0')}", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            RemoveButton(enabled, "Remover condição", onRemove)
        }
        HudTextField("Condição", condition.name, enabled = enabled) { onValue(condition.copy(name = it)) }
        TwoFields(
            { HudTextField("Intensidade", condition.intensity, it, enabled = enabled) { value -> onValue(condition.copy(intensity = value)) } },
            { HudTextField("Duração", condition.duration, it, enabled = enabled) { value -> onValue(condition.copy(duration = value)) } },
        )
        HudTextField("Origem", condition.origin, enabled = enabled) { onValue(condition.copy(origin = it)) }
        HudTextField("Resumo do efeito", condition.summary, multiline = true, enabled = enabled) { onValue(condition.copy(summary = it)) }
    }
}

@Composable
internal fun AbilityAvailabilityEditor(
    favorite: Boolean,
    available: Boolean,
    enabled: Boolean,
    onChange: (Boolean, Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton({ onChange(!favorite, available) }, enabled = enabled, modifier = Modifier.weight(1f)) { Text(if (favorite) "★ FAVORITO" else "☆ FAVORITO") }
        TextButton({ onChange(favorite, !available) }, enabled = enabled, modifier = Modifier.weight(1f)) { Text(if (available) "DISPONÍVEL" else "INDISPONÍVEL") }
    }
}

@Composable
internal fun NarrativeSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    TechPanel {
        SectionHeader("14", "História")
        HudTextField("História", character.story, multiline = true, enabled = enabled) { onChange(character.copy(story = it)) }
    }
}
