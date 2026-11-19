package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import com.kinderman.sdo.ui.sdoClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.kinderman.sdo.ui.SdoTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.addInventoryItem
import com.kinderman.sdo.domain.model.withAddedAsh
import com.kinderman.sdo.domain.catalog.toMysticAbility
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.ConditionKind
import com.kinderman.sdo.domain.model.ConditionInstance
import com.kinderman.sdo.domain.model.ConditionPayload
import com.kinderman.sdo.domain.model.DurationKind
import com.kinderman.sdo.domain.model.Duration
import com.kinderman.sdo.domain.model.DurationTimeUnit
import com.kinderman.sdo.domain.model.TimedDuration
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.InventoryState
import com.kinderman.sdo.domain.model.inventoryState
import com.kinderman.sdo.domain.model.durabilityLabel
import com.kinderman.sdo.domain.model.withInventoryState
import com.kinderman.sdo.domain.model.initialCreationCost
import com.kinderman.sdo.domain.model.participatesInInitialCreation
import com.kinderman.sdo.domain.catalog.ItemCreationRules
import com.kinderman.sdo.domain.catalog.withStructuredPathPreset
import com.kinderman.sdo.domain.catalog.toMysticAbility
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.AbilityCostType
import com.kinderman.sdo.domain.model.AbilityDuration
import com.kinderman.sdo.domain.model.AbilityExecution
import com.kinderman.sdo.domain.model.AbilityRange
import com.kinderman.sdo.domain.model.AbilityResistance
import com.kinderman.sdo.domain.model.AbilitySource
import com.kinderman.sdo.domain.model.AbilityTimeUnit
import com.kinderman.sdo.domain.model.AshPurity
import com.kinderman.sdo.domain.model.AshSource
import com.kinderman.sdo.domain.model.BodyIntegrity
import com.kinderman.sdo.domain.model.BodyRegionState
import com.kinderman.sdo.domain.model.InjuryEvent
import com.kinderman.sdo.domain.model.OrganSlot
import com.kinderman.sdo.domain.model.OrganState
import com.kinderman.sdo.domain.model.ItemInstanceId
import com.kinderman.sdo.domain.model.Source
import com.kinderman.sdo.domain.model.NarrativeSourceId
import com.kinderman.sdo.domain.model.canonicalBodyState
import com.kinderman.sdo.domain.model.withCanonicalBodyState
import com.kinderman.sdo.domain.model.withOrganImplant
import com.kinderman.sdo.domain.model.withCanonicalConditions
import com.kinderman.sdo.domain.model.toCanonicalInstance
import com.kinderman.sdo.domain.model.formattedAbilityCost
import com.kinderman.sdo.domain.model.formattedAbilityExecution
import com.kinderman.sdo.domain.model.withAddedAbility
import com.kinderman.sdo.domain.model.withRemovedAbility
import com.kinderman.sdo.domain.model.withUpdatedAbility
import com.kinderman.sdo.domain.model.withAshDoses
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
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.TechPanel

@Composable
internal fun PathSection(character: Character, catalog: List<CatalogEntry>, enabled: Boolean, onChange: (Character) -> Unit) {
    var selecting by remember { mutableStateOf(false) }
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        SectionHeader("07", "Caminho")
        CollaborativeSheetField("path/name", "Nome do Caminho", character.pathName, enabled = enabled) {
            onChange(character.copy(pathName = it))
        }
        AddButton("Preencher pelo catálogo", enabled && catalog.isNotEmpty()) { selecting = true }
        CollaborativeSheetField("path/motto", "Lema", character.pathMotto, enabled = enabled) { onChange(character.copy(pathMotto = it)) }
        SdoMetadataText("PALAVRAS-CHAVE // 3", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        character.pathKeywords.forEachIndexed { index, keyword ->
            HudTextField("Palavra-chave ${index + 1}", keyword, enabled = enabled) { onChange(character.copy(pathKeywords = character.pathKeywords.replace(index, it))) }
        }
        SdoMetadataText("PILARES // 3", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        character.pathPillars.forEachIndexed { index, pillar ->
            HudTextField("Pilar ${index + 1}", pillar, multiline = true, enabled = enabled) { onChange(character.copy(pathPillars = character.pathPillars.replace(index, it))) }
        }
    }
    if (selecting) CatalogPickerDialog("SELECIONAR CAMINHO", catalog, { selecting = false }) { entry ->
        onChange(character.withStructuredPathPreset(entry))
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
        SectionHeader("10", "Inventário")
        Text("CARGA ${character.currentLoad} / ${character.maximumLoad}", color = if (character.currentLoad > character.maximumLoad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleLarge)
        Text("Máxima = 2 + FOR + capacidade do recipiente. Itens [G] não contam como carregados.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        if (remainingHeritage > 0) {
            SdoMetadataText("CRIAÇÃO INICIAL OBRIGATÓRIA // $remainingHeritage / ${ItemCreationRules.HERITAGE_BUDGET} PH RESTANTES", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
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
            AddButton("Equipamentos iniciais // escolher ou construir", enabled) { dialog = "initial" }
        }
        if (remainingHeritage == 0) {
            AddButton("Catálogo de itens // fora da criação", enabled && catalog.isNotEmpty()) { dialog = "catalog" }
            AddButton("Construtor de item // criação durante o jogo", enabled) { dialog = "builder" }
            AddButton("Adicionar objeto narrativo sem valores mecânicos", enabled) { onChange(character.addInventoryItem(InventoryItem())) }
        }
    }
    when (dialog) {
        "glossary" -> EquipmentGlossaryDialog { dialog = null }
        "initial", "initial_catalog", "initial_builder" -> InventoryItemModal(
            mode = ItemModalMode.Creation(remainingHeritage),
            itemCatalog = catalog.filter { it.kind == com.kinderman.sdo.domain.model.CatalogKind.ITEM },
            ashCatalog = catalog.filter { it.kind == com.kinderman.sdo.domain.model.CatalogKind.ASH },
            regionOptions = character.bodyRegions.map { it.name },
            initialTab = if (dialog == "initial_builder") ItemModalTab.BUILDER else ItemModalTab.CATALOG,
            onDismiss = { dialog = null },
            onAddItem = { item ->
                onChange(character.addInventoryItem(item))
                dialog = null
            },
            onAddAsh = { entry, doses ->
                onChange(character.withAddedAsh(entry.toMysticAbility(), doses, initialCreation = true))
                dialog = null
            },
        )
        "catalog", "builder" -> InventoryItemModal(
            mode = ItemModalMode.Sheet,
            itemCatalog = catalog.filter { it.kind == com.kinderman.sdo.domain.model.CatalogKind.ITEM },
            ashCatalog = catalog.filter { it.kind == com.kinderman.sdo.domain.model.CatalogKind.ASH },
            regionOptions = character.bodyRegions.map { it.name },
            initialTab = if (dialog == "builder") ItemModalTab.BUILDER else ItemModalTab.CATALOG,
            onDismiss = { dialog = null },
            onAddItem = { item ->
                onChange(character.addInventoryItem(item))
                dialog = null
            },
            onAddAsh = { entry, doses ->
                onChange(character.withAddedAsh(entry.toMysticAbility(), doses, initialCreation = false))
                dialog = null
            },
        )
    }
}

@Composable
private fun InventoryEditor(index: Int, item: InventoryItem, enabled: Boolean, onRemove: () -> Unit, onValue: (InventoryItem) -> Unit) {
    SdoInsetCard(contentPadding = 12.dp) {
        Row(Modifier.fillMaxWidth()) {
            Text("ITEM ${(index + 1).toString().padStart(2, '0')}", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            RemoveButton(enabled, "Remover item", onRemove)
        }
        CollaborativeSheetField("inventory/${item.id}/name", "Nome", item.name, enabled = enabled) { onValue(item.copy(name = it)) }
        TwoFields(
            { ChoiceField("Estado", item.inventoryState, InventoryState.entries, enabled, it, InventoryState::label) { value -> onValue(item.withInventoryState(value)) } },
            { IntegerField("Carga", item.load, enabled, it) { value -> onValue(item.copy(load = value.coerceAtLeast(0))) } },
        )
        IntegerField("Durabilidade atual", item.durabilityCurrent, enabled) { value -> onValue(item.copy(durabilityCurrent = value.coerceIn(0, item.durabilityMax))) }
        SdoMetadataText("${item.category.ifBlank { "OBJETO NARRATIVO" }} // ${item.quality.label.uppercase()} // ${item.durabilityLabel}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        SdoMetadataText("REGIÃO ${item.region.ifBlank { "—" }} // PG ${item.pg} // PL ${item.pl} // LA ${item.agilityLimit ?: "—"}", color = MaterialTheme.colorScheme.onSurface)
        if (item.mechanicalEffects.isNotEmpty()) Text(
            "EFEITOS // " + item.mechanicalEffects.joinToString { "${it.type.name} ${if (it.value > 0) "+" else ""}${it.value} ${it.resolvedTargetId.ifBlank { it.target }}" },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
        CollaborativeSheetField("inventory/${item.id}/effect", "Efeito", item.effect, multiline = true, enabled = enabled) { onValue(item.copy(effect = it)) }
    }
}

@Composable
internal fun BodySection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    var equipmentRegionIndex by rememberSaveable(character.id) { mutableStateOf<Int?>(null) }
    val regions = character.canonicalBodyState().regions
    com.kinderman.sdo.ui.CollapsibleSection("Corpo e armadura", index = "11") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("11", "Corpo e armadura")
            regions.forEachIndexed { index, _ ->
                BodyRegionSection(
                    character = character,
                    index = index,
                    enabled = enabled,
                    onChange = onChange,
                    onSelectEquipment = { equipmentRegionIndex = index },
                )
            }
        }
    }
    equipmentRegionIndex?.let { index ->
        val region = regions.getOrNull(index)
        if (region != null) EquipmentPickerDialog(
            regionName = region.name,
            inventory = character.inventory,
            selectedIds = region.equippedItemIds.mapTo(mutableSetOf()) { it.value },
            onDismiss = { equipmentRegionIndex = null },
        ) { selectedIds ->
            onChange(character.equipItems(index, selectedIds))
            equipmentRegionIndex = null
        }
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
    val body = character.canonicalBodyState()
    val region = body.regions[index]
    var expanded by rememberSaveable(character.id, region.region.name) { mutableStateOf(false) }
    fun updateRegion(updated: BodyRegionState) = onChange(
        character.withCanonicalBodyState(body.withUpdatedRegion(region.region) { updated }),
    )
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        Row(
            Modifier.fillMaxWidth().sdoClickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                SdoMetadataText("D10.${region.roll.toString().padStart(2, '0')} // ${region.name.uppercase()}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                SdoMetadataText("${region.state.label.uppercase()} // ${region.failures}/4 FALHAS // PL ${character.localProtection(region)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Recolher ${region.name}" else "Expandir ${region.name}")
        }
        if (!expanded) return@TechPanel
        ChoiceField("Estado", region.state, BodyIntegrity.entries, enabled, display = { it.label }) { state ->
            updateRegion(region.copy(state = state))
        }
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            ChoiceField("Falhas", region.failures.coerceIn(0, 4), (0..4).toList(), enabled, Modifier.weight(1f).fillMaxHeight()) { value ->
                val state = when {
                    value >= 4 -> BodyIntegrity.Destroyed
                    value > 0 -> BodyIntegrity.Damaged
                    region.state in setOf(BodyIntegrity.Damaged, BodyIntegrity.Destroyed) -> BodyIntegrity.Intact
                    else -> region.state
                }
                updateRegion(region.copy(failures = value, state = state))
            }
            IntegerField("Ajuste PL", region.protection.localProtection, enabled, Modifier.weight(1f)) { value -> updateRegion(region.copy(protection = region.protection.copy(localProtection = value.coerceAtLeast(0)))) }
        }
        Text(
            "PL TOTAL ${character.localProtection(region)} // PG DO PERSONAGEM +${character.equippedGeneralProtection}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
        val implantNames = region.implantInstanceIds.mapNotNull { id -> character.inventory.firstOrNull { it.id == id.value }?.name }
        SdoMetadataText("IMPLANTES // ${implantNames.joinToString().ifBlank { "NENHUM" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        val equippedNames = character.equippedItems(region).joinToString { it.name.ifBlank { "Item sem nome" } }
        SdoMetadataText("EQUIPAMENTOS // ${equippedNames.ifBlank { "NENHUM" }}", color = if (equippedNames.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
        if (region.injuries.isNotEmpty()) {
            SdoMetadataText("HISTÓRICO // ${region.injuries.size} evento(s)", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            region.injuries.forEach { injury ->
                val damage = injury.damage?.let { " • ${it.type.label} ${it.amount}" }.orEmpty()
                Text("${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(injury.occurredAt))} • +${injury.failuresAdded} falha(s)$damage", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        AddButton("Registrar lesão", enabled && region.failures < 4) {
            val nextFailures = (region.failures + 1).coerceAtMost(4)
            updateRegion(region.copy(
                failures = nextFailures,
                state = if (nextFailures >= 4) BodyIntegrity.Destroyed else BodyIntegrity.Damaged,
                injuries = region.injuries + InjuryEvent(
                    source = Source.narrative(NarrativeSourceId("manual-injury")),
                    failuresAdded = 1,
                ),
            ))
        }
        AddButton("Selecionar equipamentos do inventário", enabled, onSelectEquipment)
    }
}

@Composable
internal fun OrganSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    val body = character.canonicalBodyState()
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        SectionHeader("12", "Órgãos")
        if (body.organs.isEmpty()) {
            Text(
                "Registre apenas órgãos com dano, implante, parasita ou outra alteração relevante.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        body.organs.forEachIndexed { index, organ ->
            var expanded by rememberSaveable(character.id, "organ", index) { mutableStateOf(false) }
            val implantOptions = listOf("") + character.inventory.filter { it.category.contains("implante", true) }.map { it.id }
            fun updateOrgan(updated: OrganState) {
                onChange(character.withCanonicalBodyState(body.copy(organs = body.organs.mapIndexed { organIndex, current ->
                    if (organIndex == index) updated else current
                })))
            }
            SdoInsetCard(contentPadding = 12.dp) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f).sdoClickable { expanded = !expanded }) {
                        Text(organ.organ.label.uppercase(), color = MaterialTheme.colorScheme.primary)
                        SdoMetadataText("${organ.state.label.uppercase()} // ${organ.failures}/3 FALHAS", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Recolher órgão" else "Expandir órgão", Modifier.sdoClickable { expanded = !expanded })
                    RemoveButton(enabled, "Remover registro de órgão") {
                        onChange(character.withCanonicalBodyState(body.copy(organs = body.organs.filterIndexed { organIndex, _ -> organIndex != index })))
                    }
                }
                if (!expanded) return@SdoInsetCard
                ChoiceField("Órgão", organ.organ, OrganSlot.entries, enabled, display = { it.label }) { slot ->
                    updateOrgan(organ.copy(organ = slot, customName = if (slot == OrganSlot.Other) organ.customName else ""))
                }
                if (organ.organ == OrganSlot.Other) HudTextField("Identificação", organ.customName, enabled = enabled) {
                    updateOrgan(organ.copy(customName = it))
                }
                TwoFields(
                    { ChoiceField("Estado", organ.state, BodyIntegrity.entries, enabled, it, display = { value -> value.label }) { value -> updateOrgan(organ.copy(state = value)) } },
                    { ChoiceField("Falhas", organ.failures.coerceIn(0, 3), (0..3).toList(), enabled, it) { value -> updateOrgan(organ.copy(failures = value)) } },
                )
                ChoiceField("Implante", organ.implantInstanceId?.value.orEmpty(), implantOptions, enabled, display = { id ->
                    character.inventory.firstOrNull { it.id == id }?.name ?: "Nenhum"
                }) { id -> onChange(character.withOrganImplant(index, id.takeIf(String::isNotBlank))) }
            }
            if (index != body.organs.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        AddButton("Adicionar alteração de órgão", enabled) {
            onChange(character.withCanonicalBodyState(body.copy(organs = body.organs + OrganState())))
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
        SectionHeader("09", "Magias, runas e cinzas")
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
                onDosesChange = { doses -> applyChange { character.withAshDoses(ability.id, doses) } },
            )
        }
        AddButton("Selecionar místico", enabled && catalog.isNotEmpty()) { selecting = true }
        AddButton("Adicionar efeito manualmente", enabled) {
            val ability = MysticAbility(type = "Magia")
            expandedAbilityId = ability.id
            applyChange { character.withAddedAbility(ability) }
        }
    }
    if (selecting) CatalogPickerDialog(
        title = "SELECIONAR EFEITO MÍSTICO",
        entries = catalog,
        onDismiss = { selecting = false },
        groupAshVariants = true,
    ) { entry ->
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
    onDosesChange: (Int) -> Unit,
) {
    val effectiveCostType = when {
        ability.type.equals("Cinza", true) -> AbilityCostType.DOSE
        else -> AbilityCostType.ARCANE
    }
    SdoInsetCard(contentPadding = 12.dp) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(ability.name.ifBlank { "EFEITO ${(index + 1).toString().padStart(2, '0')}" }, color = MaterialTheme.colorScheme.onSurface)
                val linkedAsh = character.inventory.firstOrNull {
                    it.id == ability.linkedInventoryItemId || it.linkedAshId == ability.id
                }
                val source = if (ability.type.equals("Cinza", true)) {
                    "${ability.ashSource.label} // ${ability.ashPurity.label} // ESTOQUE ${linkedAsh?.quantity ?: 0}"
                } else ability.canonicalSource.label
                Text(
                    listOf(ability.type.ifBlank { "Magia" }, source, formattedAbilityExecution(ability.executionType, ability.timeValue, ability.timeUnit), formattedAbilityCost(effectiveCostType, ability.costValue)).joinToString(" // "),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            SdoTextButton(onClick = onToggle) { Text(if (expanded) "FECHAR" else "EDITAR") }
            RemoveButton(enabled, "Remover efeito", onRemove)
        }
        if (!expanded) {
            Text(ability.effect.ifBlank { "Sem efeito descrito." }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            return@SdoInsetCard
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
            IntegerField(
                "Doses no inventário",
                character.inventory.firstOrNull { it.id == ability.linkedInventoryItemId || it.linkedAshId == ability.id }?.quantity ?: 0,
                enabled,
            ) { onDosesChange(it.coerceAtLeast(0)) }
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
        CollaborativeSheetField("mysticAbilities/${ability.id}/targetArea", "Alvo / Área (opcional)", ability.targetArea, enabled = enabled) { onValue(ability.copy(targetArea = it)) }
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
        CollaborativeSheetField("mysticAbilities/${ability.id}/effect", "Efeito", ability.effect, multiline = true, enabled = enabled) { onValue(ability.copy(effect = it)) }
        AbilityAvailabilityEditor(ability.favorite, ability.available, enabled) { favorite, available ->
            onValue(ability.copy(favorite = favorite, available = available))
        }
    }
}

@Composable
internal fun ConditionSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    var newlyAddedId by remember(character.id) { mutableStateOf<String?>(null) }
    val instances = character.conditionInstances.ifEmpty {
        character.conditions.mapNotNull { runCatching { it.toCanonicalInstance() }.getOrNull() }
    }
    val descriptions = character.conditions.associate { it.id to it.summary }
    fun updateInstances(values: List<ConditionInstance>, editedId: String? = null, editedDescription: String? = null) {
        val updated = character.withCanonicalConditions(values)
        onChange(updated.copy(conditions = updated.conditions.map { legacy ->
            legacy.copy(summary = if (legacy.id == editedId) editedDescription.orEmpty() else descriptions[legacy.id].orEmpty())
        }))
    }
    TechPanel(accent = MaterialTheme.colorScheme.error) {
        SectionHeader("13", "Condições")
        if (instances.isEmpty()) {
            SdoInsetCard(contentPadding = 14.dp) {
                Text(
                    "Nenhuma condição ativa no momento.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        instances.forEachIndexed { index, condition ->
            androidx.compose.runtime.key(condition.instanceId.value) {
                ConditionEditor(
                    index = index,
                    condition = condition,
                    description = descriptions[condition.instanceId.value].orEmpty(),
                    enabled = enabled,
                    initialEditing = condition.instanceId.value == newlyAddedId,
                    onRemove = {
                        if (newlyAddedId == condition.instanceId.value) newlyAddedId = null
                        updateInstances(instances.filterIndexed { itemIndex, _ -> itemIndex != index })
                    },
                    onValue = { updated, description ->
                        updateInstances(
                            instances.mapIndexed { itemIndex, current -> if (itemIndex == index) updated else current },
                            editedId = updated.instanceId.value,
                            editedDescription = description,
                        )
                    },
                )
            }
        }
        AddButton("Adicionar condição", enabled) {
            val condition = ConditionInstance(
                payload = ConditionPayload(kind = ConditionKind.Abalado, duration = Duration(DurationKind.Instant)),
                source = Source.narrative(NarrativeSourceId("manual-condition")),
            )
            newlyAddedId = condition.instanceId.value
            updateInstances(instances + condition)
        }
    }
}

@Composable
private fun ConditionEditor(
    index: Int,
    condition: ConditionInstance,
    description: String,
    enabled: Boolean,
    initialEditing: Boolean = false,
    onRemove: () -> Unit,
    onValue: (ConditionInstance, String) -> Unit,
) {
    var draftDescription by remember(condition.instanceId.value) { mutableStateOf(description) }
    var editing by rememberSaveable(condition.instanceId.value) { mutableStateOf(initialEditing) }
    val showEditor = enabled && editing
    LaunchedEffect(description) {
        if (description != draftDescription) draftDescription = description
    }
    SdoInsetCard(contentPadding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.kinderman.sdo.ui.SdoMetadataText(
                "CONDIÇÃO ${(index + 1).toString().padStart(2, '0')}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            CompactEditToggleButton(
                editing = showEditor,
                enabled = enabled,
                onClick = { editing = !editing },
            )
            RemoveButton(enabled, "Remover condição", onRemove)
        }
        if (showEditor) {
            ChoiceField("Condição", condition.kind, ConditionKind.entries, enabled, display = { it.label }) {
                onValue(condition.copy(payload = condition.payload.copy(kind = it)), draftDescription)
            }
            val selectedDuration = condition.duration?.kind ?: DurationKind.Instant
            TwoFields(
                { IntegerField("Intensidade", condition.intensity ?: 0, enabled, it) { value -> onValue(condition.copy(payload = condition.payload.copy(intensity = value.takeIf { it > 0 })), draftDescription) } },
                { ChoiceField("Duração", selectedDuration, DurationKind.entries, enabled, it, display = { value -> value.label }) { value ->
                    val duration = when (value) {
                        DurationKind.Instant, DurationKind.Scene, DurationKind.Session -> Duration(value)
                        DurationKind.Turns -> Duration(value, turns = condition.duration?.turns ?: 1)
                        DurationKind.Timed -> Duration(value, timed = condition.duration?.timed ?: TimedDuration(1, DurationTimeUnit.Hours))
                    }
                    onValue(condition.copy(payload = condition.payload.copy(duration = duration)), draftDescription)
                } },
            )
            if (selectedDuration == DurationKind.Turns) IntegerField("Quantidade de turnos", condition.duration?.turns ?: 1, enabled) { value ->
                onValue(condition.copy(payload = condition.payload.copy(duration = Duration(DurationKind.Turns, turns = value.coerceAtLeast(1)))), draftDescription)
            }
            if (selectedDuration == DurationKind.Timed) TwoFields(
                { IntegerField("Tempo", condition.duration?.timed?.amount ?: 1, enabled, it) { value ->
                    onValue(condition.copy(payload = condition.payload.copy(duration = Duration(DurationKind.Timed, timed = TimedDuration(value.coerceAtLeast(1), condition.duration?.timed?.unit ?: DurationTimeUnit.Hours)))), draftDescription)
                } },
                { ChoiceField("Unidade", condition.duration?.timed?.unit ?: DurationTimeUnit.Hours, DurationTimeUnit.entries, enabled, it, display = { unit -> unit.label }) { unit ->
                    onValue(condition.copy(payload = condition.payload.copy(duration = Duration(DurationKind.Timed, timed = TimedDuration(condition.duration?.timed?.amount ?: 1, unit)))), draftDescription)
                } },
            )
            HudTextField("Descrição", draftDescription, multiline = true, enabled = enabled) {
                draftDescription = it
                onValue(condition, it)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (enabled) Modifier.sdoClickable { editing = true } else Modifier),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                com.kinderman.sdo.ui.SdoCardTitle(
                    text = condition.kind.label,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
                val durationText = when (val d = condition.duration?.kind ?: DurationKind.Instant) {
                    DurationKind.Turns -> "${condition.duration?.turns ?: 1} turno(s)"
                    DurationKind.Timed -> "${condition.duration?.timed?.amount ?: 1} ${condition.duration?.timed?.unit?.label ?: ""}".trim()
                    else -> d.label
                }
                val metaItems = buildList {
                    add("DURAÇÃO // $durationText")
                    val intensity = condition.intensity ?: 0
                    if (intensity > 0) add("INTENSIDADE // $intensity")
                }
                com.kinderman.sdo.ui.SdoResponsiveGrid(metaItems, minItemWidth = 132.dp) { item, modifier ->
                    SdoInsetCard(modifier = modifier, contentPadding = 8.dp) {
                        com.kinderman.sdo.ui.SdoMetadataText(item, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                if (draftDescription.isNotBlank()) {
                    Text(
                        text = draftDescription.trim(),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AbilityAvailabilityEditor(
    favorite: Boolean,
    available: Boolean,
    enabled: Boolean,
    onChange: (Boolean, Boolean) -> Unit,
) {
    com.kinderman.sdo.ui.SdoResponsiveGrid(listOf("FAVORITE", "AVAILABLE"), minItemWidth = 140.dp) { kind, modifier ->
        when (kind) {
            "FAVORITE" -> SdoTextButton({ onChange(!favorite, available) }, enabled = enabled, modifier = modifier) {
                com.kinderman.sdo.ui.AdaptiveActionLabel(if (favorite) "★ FAVORITO" else "☆ FAVORITO")
            }
            else -> SdoTextButton({ onChange(favorite, !available) }, enabled = enabled, modifier = modifier) {
                com.kinderman.sdo.ui.AdaptiveActionLabel(if (available) "DISPONÍVEL" else "INDISPONÍVEL")
            }
        }
    }
}

@Composable
internal fun NarrativeSection(character: Character, enabled: Boolean, onChange: (Character) -> Unit) {
    var editing by rememberSaveable(character.id) { mutableStateOf(false) }
    var confirmClear by rememberSaveable(character.id) { mutableStateOf(false) }
    val showEditor = enabled && editing
    TechPanel {
        SectionShowcaseHeader(
            index = "17",
            title = "História",
            subtitle = "ARQUIVO BIOGRÁFICO",
            editing = editing,
            enabled = enabled,
            onToggle = { editing = !editing },
            trailingActions = {
                if (enabled && character.story.isNotBlank()) {
                    RemoveButton(enabled = true, description = "Apagar história") {
                        confirmClear = true
                    }
                }
            },
        )
        if (showEditor) {
            CollaborativeSheetField("story", "História", character.story, multiline = true, enabled = enabled) { onChange(character.copy(story = it)) }
        } else {
            StoryReadOnlyShowcase(character)
        }
    }
    if (confirmClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("APAGAR HISTÓRIA") },
            text = { Text("Deseja apagar todo o texto da história do personagem?") },
            confirmButton = {
                SdoTextButton(
                    onClick = {
                        confirmClear = false
                        onChange(character.copy(story = ""))
                    },
                ) { Text("APAGAR", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                SdoTextButton(onClick = { confirmClear = false }) { Text("CANCELAR") }
            },
        )
    }
}

@Composable
private fun StoryReadOnlyShowcase(character: Character) {
    val identitySummary = listOf(character.name, character.race, character.occupation)
        .filter(String::isNotBlank)
        .joinToString(" · ")
        .ifBlank { "Personagem sem identificação" }
    SdoInsetCard(contentPadding = 12.dp, verticalSpacing = 8.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = identitySummary,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = character.story.trim().ifBlank { "Nenhum registro biográfico inscrito ainda." },
                color = if (character.story.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
