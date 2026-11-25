package com.kinderman.sdo.presentation.character

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.heightIn
import com.kinderman.sdo.domain.model.withKind
import com.kinderman.sdo.R
import com.kinderman.sdo.ui.SdoSwitch


import com.kinderman.sdo.ui.SdoCardTitle
import androidx.compose.foundation.background
import com.kinderman.sdo.ui.sdoClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.kinderman.sdo.ui.SdoTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.catalog.toMysticAbility
import com.kinderman.sdo.domain.catalog.toStructuredPower
import com.kinderman.sdo.domain.model.*
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.TechPanel

@Composable
internal fun AbilitySection(
    character: Character,
    catalog: List<CatalogEntry>,
    kinds: Set<AbilityKind>,
    enabled: Boolean,
    onChange: (Character) -> Unit,
) {
    var selecting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val abilities = character.allCanonicalAbilitiesSafely().filter { it.kind in kinds }
    TechPanel(accent = MaterialTheme.colorScheme.primary) {
        val powersOnly = kinds == setOf(AbilityKind.POWER)
        SectionHeader(if (powersOnly) "08" else "09", if (powersOnly) "Poderes" else "Magias // Runas // Cinzas")
        abilities.forEach { ability -> AbilityEditor(character, ability, kinds, enabled, onChange) }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AddButton(
                if (powersOnly) "Selecionar poder" else "Selecionar místico",
                enabled && catalog.isNotEmpty(),
            ) { selecting = true }
            SdoTextButton(onClick = {
                val kind = kinds.firstOrNull() ?: AbilityKind.POWER
                val defaultPowerCost = if (kind == AbilityKind.POWER && character.derivedDivineEffects().usesConceptualPillars) {
                    AbilityCost.PowerCost(1, SpendableResource.PD, isDivineOrLuckBased = true)
                } else {
                    defaultCost(kind)
                }
                val ability = Ability(
                    name = if (character.derivedDivineEffects().usesConceptualPillars && kind == AbilityKind.POWER) "Novo Ato ou Capacidade Divina" else "Nova habilidade",
                    kind = kind,
                    source = Source.narrative(NarrativeSourceId("manual")),
                    cost = defaultPowerCost,
                )
                onChange(character.copy(abilities = character.allCanonicalAbilitiesSafely() + ability))
            }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(if (powersOnly) "+ CRIAR PODER" else "+ CRIAR EFEITO MÍSTICO")
            }
        }
    }
    if (selecting) CatalogPickerDialog(
        title = if (kinds == setOf(AbilityKind.POWER)) "SELECIONAR PODER" else "CATÁLOGO MÍSTICO",
        entries = catalog,
        onDismiss = { selecting = false },
        groupAshVariants = kinds != setOf(AbilityKind.POWER),
    ) { entry ->
        val result = runCatching {
            when (entry.kind) {
                CatalogKind.POWER -> entry.toStructuredPower(PowerSourceType.NARRATIVE, entry.id).toCanonicalAbility()
                else -> entry.toMysticAbility(character).toCanonicalAbility()
            }
        }
        result.onSuccess { ability ->
            val updated = if (entry.kind == CatalogKind.ASH) {
                character.withAddedAsh(entry.toMysticAbility(), doses = 1, initialCreation = character.isInCreation)
            } else {
                character.copy(abilities = character.allCanonicalAbilitiesSafely() + ability)
            }
            onChange(updated)
            selecting = false
        }.onFailure { error ->
            android.widget.Toast.makeText(context, error.message ?: "Entrada inválida no catálogo.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun AbilityEditor(character: Character, ability: Ability, allowedKinds: Set<AbilityKind>, enabled: Boolean, onChange: (Character) -> Unit) {
    val published = ability.definition != null
    var expanded by rememberSaveable(ability.id) { mutableStateOf(false) }
    val isRacial = ability.source?.kind == SourceKind.Race
    val mechanicsEditable = enabled && !published && !isRacial
    var nameDraft by rememberSaveable(ability.id, ability.name) { mutableStateOf(ability.name) }
    fun update(value: Ability) = onChange(character.copy(
        abilities = character.allCanonicalAbilitiesSafely().map { if (it.id == ability.id) value.copy(revision = ability.revision + 1) else it },
    ))
    SdoInsetCard(contentPadding = 12.dp) {
        Row(
            Modifier.fillMaxWidth().sdoClickable { expanded = !expanded },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                SdoMetadataText(
                    text = ability.kind.label.uppercase(),
                    color = MaterialTheme.colorScheme.secondary,
                )
                SdoCardTitle(
                    text = ability.name.ifBlank { "Habilidade sem nome" },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Recolher ${ability.name}" else "Expandir ${ability.name}")
            RemoveButton(enabled && !isRacial, if (isRacial) "Poder racial não pode ser removido" else "Remover habilidade") {
                onChange(character.withRemovedCanonicalAbility(ability.id))
            }
        }
        if (ability.kind != AbilityKind.POWER) {
            SdoCardTitle(ability.name, style = MaterialTheme.typography.titleMedium)
        }
        SdoMetadataText(ability.compactSummary(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        if (isRacial) SdoMetadataText("PODER RACIAL // VINCULADO À RAÇA", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
        if (!expanded) {
            Text(ability.effect.ifBlank { "Sem descrição." }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            return@SdoInsetCard
        }
        else CollaborativeSheetField("abilities/${ability.id}/name", "Nome", nameDraft, enabled = mechanicsEditable) { value ->
            nameDraft = value
            if (value.isNotBlank()) update(ability.copy(name = value))
        }
        val durationKind = ability.duration?.kind ?: DurationKind.Instant
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics(mergeDescendants = true) {}
                .testTag("ability_channeling_" + ability.id),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.sustained_channeling_label), Modifier.weight(1f))
            SdoSwitch(
                checked = ability.duration?.requiresChanneling == true,
                enabled = mechanicsEditable && ability.execution.kind != ExecutionKind.Passive,
                onCheckedChange = { required ->
                    update(ability.copy(duration = (ability.duration ?: Duration(DurationKind.Scene)).copy(requiresChanneling = required)))
                },
            )
        }
        val trigger = ability.mechanicalEffect?.trigger
        TwoFields(
            { field -> ChoiceField("Categoria", ability.kind, allowedKinds.toList(), mechanicsEditable, field, display = AbilityKind::label) { kind ->
                update(ability.copy(kind = kind, cost = defaultCost(kind)))
            } },
            { field -> ChoiceField("Execução", ability.execution.kind, ExecutionKind.entries, mechanicsEditable, field, display = ExecutionKind::label) { kind ->
                val nextDuration = if (kind == ExecutionKind.Passive) null else (ability.duration ?: Duration(DurationKind.Instant))
                val nextCost = if (kind == ExecutionKind.Passive && ability.kind == AbilityKind.POWER) {
                    abilityCostWithAmount(ability, 0)
                } else if (kind != ExecutionKind.Passive && ability.kind == AbilityKind.POWER && ability.cost.amount == 0 && (ability.cost as? AbilityCost.PowerCost)?.resource != SpendableResource.PD) {
                    abilityCostWithAmount(ability, 1)
                } else {
                    ability.cost
                }
                val nextPowerData = if (kind == ExecutionKind.Passive && ability.kind == AbilityKind.POWER) {
                    (ability.powerData ?: PowerData()).copy(passiveState = ability.powerData?.passiveState ?: PassiveState.ManualToggle(active = true))
                } else {
                    ability.powerData
                }
                update(
                    ability.copy(
                        execution = Execution(kind, if (kind == ExecutionKind.Timed) TimedExecution(1, ExecutionTimeUnit.Minutes) else null),
                        duration = nextDuration,
                        cost = nextCost,
                        powerData = nextPowerData,
                    ),
                )
            } },
        )
        if (ability.execution.kind != ExecutionKind.Passive) {
            TwoFields(
                { field -> ChoiceField("Alcance", ability.range.band, RangeBand.entries, mechanicsEditable, field, display = RangeBand::label) { update(ability.copy(range = RangeSpec(it))) } },
                { field -> ChoiceField("Duração", durationKind, DurationKind.entries, mechanicsEditable, field, display = DurationKind::label) { kind ->
                    update(ability.copy(duration = ability.duration.withKind(kind)))
                } },
            )
        } else {
            ChoiceField("Alcance", ability.range.band, RangeBand.entries, mechanicsEditable, display = RangeBand::label) {
                update(ability.copy(range = RangeSpec(it)))
            }
        }
        if (ability.kind == AbilityKind.POWER) {
            val powerResources = listOf(SpendableResource.PE, SpendableResource.PD, SpendableResource.PS, SpendableResource.PV)
            val currentResource = (ability.cost as? AbilityCost.PowerCost)?.resource ?: SpendableResource.PE
            TwoFields(
                { field ->
                    ChoiceField(
                        "Recurso do custo",
                        currentResource,
                        powerResources,
                        mechanicsEditable,
                        field,
                        display = { res -> if (res == SpendableResource.PD) "PD (Destino / Divino)" else res.name },
                    ) { res ->
                        val nextAmt = if (ability.execution.kind == ExecutionKind.Passive) 0 else ability.cost.amount
                        update(
                            ability.copy(
                                cost = AbilityCost.PowerCost(
                                    amount = nextAmt,
                                    resource = res,
                                    isDivineOrLuckBased = res == SpendableResource.PD || (ability.cost as? AbilityCost.PowerCost)?.isDivineOrLuckBased == true,
                                ),
                            ),
                        )
                    }
                },
                { field -> AbilityCostField(ability, mechanicsEditable && ability.execution.kind != ExecutionKind.Passive, field, ::update) },
            )
            ChoiceField("Gatilho", triggerLabel(trigger), triggerOptions, mechanicsEditable) { selected ->
                val operations = ability.mechanicalEffect?.operations.orEmpty()
                val effect = operations.takeIf { it.isNotEmpty() }?.let { MechanicalEffect(it, ability.mechanicalEffect?.usage, triggerFrom(selected)) }
                update(ability.copy(mechanicalEffect = effect))
            }
        } else {
            TwoFields(
                { field -> AbilityCostField(ability, mechanicsEditable, field, ::update) },
                { field -> ChoiceField("Gatilho", triggerLabel(trigger), triggerOptions, mechanicsEditable, field) { selected ->
                    val operations = ability.mechanicalEffect?.operations.orEmpty()
                    val effect = operations.takeIf { it.isNotEmpty() }?.let { MechanicalEffect(it, ability.mechanicalEffect?.usage, triggerFrom(selected)) }
                    update(ability.copy(mechanicalEffect = effect))
                } },
            )
        }
        CollaborativeSheetField("abilities/${ability.id}/effect", "Descrição", ability.effect, multiline = true, enabled = mechanicsEditable) { update(ability.copy(effect = it)) }
        Text("OPERAÇÕES ESTRUTURADAS", color = MaterialTheme.colorScheme.primary)
        ability.mechanicalEffect?.operations.orEmpty().forEachIndexed { index, operation ->
            AbilityOperationEditor(operation, mechanicsEditable, { replacement ->
                val effect = ability.mechanicalEffect!!
                val operations = effect.operations.mapIndexed { current, value -> if (current == index) replacement else value }
                update(ability.copy(mechanicalEffect = MechanicalEffect(operations, effect.usage, effect.trigger)))
            }, {
                val effect = ability.mechanicalEffect!!
                val operations = effect.operations.filterIndexed { current, _ -> current != index }
                update(ability.copy(mechanicalEffect = operations.takeIf { it.isNotEmpty() }?.let { MechanicalEffect(it, effect.usage, effect.trigger) }))
            })
        }
        if (!published && !isRacial) AddButton("Adicionar operação", enabled) {
            val operations = ability.mechanicalEffect?.operations.orEmpty() + EffectOperation.Damage()
            update(ability.copy(mechanicalEffect = MechanicalEffect(operations, ability.mechanicalEffect?.usage, ability.mechanicalEffect?.trigger)))
        }
        if (!published) Text("Descrição não é interpretada como regra.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

private fun defaultCost(kind: AbilityKind): AbilityCost = when (kind) {
    AbilityKind.POWER -> AbilityCost.PowerCost()
    AbilityKind.SPELL -> AbilityCost.SpellCost()
    AbilityKind.RUNE -> AbilityCost.RuneCost()
    AbilityKind.ASH -> AbilityCost.AshCost()
}

private fun Ability.compactSummary(): String {
    val costLabel = when (val value = cost) {
        is AbilityCost.PowerCost -> "${value.amount} ${value.resource.name}"
        is AbilityCost.SpellCost -> "${value.amount} PM"
        is AbilityCost.RuneCost -> "${value.amount} PM"
        is AbilityCost.AshCost -> "${value.amount} dose(s)"
    }
    val durationLabel = duration?.kind?.label ?: DurationKind.Instant.label
    return "${execution.kind.label} // $costLabel // ${range.band.label} // $durationLabel"
}

@Composable
private fun AbilityOperationEditor(operation: EffectOperation, enabled: Boolean, update: (EffectOperation) -> Unit, remove: () -> Unit) {
    val kinds = EffectOperationKind.entries
    Column(Modifier.fillMaxWidth().padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        ChoiceField("Tipo", operation.kind, kinds, enabled) { kind -> update(when (kind) {
            EffectOperationKind.Damage -> EffectOperation.Damage()
            EffectOperationKind.Healing -> EffectOperation.Healing()
            EffectOperationKind.ApplyCondition -> EffectOperation.ApplyCondition(ConditionPayload())
            EffectOperationKind.AddModifier -> EffectOperation.AddModifier(ActiveModifier(target = BonusTarget.AttributeTarget(Attribute.FOR), amount = 1, source = Source.narrative(NarrativeSourceId("manual-operation"))))
            EffectOperationKind.SpendResource -> EffectOperation.SpendResource(SpendableResource.PE, 1)
            EffectOperationKind.ConsumeItemState -> EffectOperation.ConsumeItemState(ItemInstanceId("selecione-item"))
        }) }
        when (operation) {
            is EffectOperation.Damage -> {
                ChoiceField("Dano", operation.damageType, DamageType.entries, enabled, display = DamageType::label) { update(operation.copy(damageType = it)) }
                FixedAmountField(operation.amount, enabled) { update(operation.copy(amount = it)) }
            }
            is EffectOperation.Healing -> {
                ChoiceField("Recurso", operation.resource, SpendableResource.entries, enabled) { update(operation.copy(resource = it)) }
                FixedAmountField(operation.amount, enabled) { update(operation.copy(amount = it)) }
            }
            is EffectOperation.ApplyCondition -> ChoiceField("Condição", operation.condition.kind, ConditionKind.entries, enabled, display = ConditionKind::label) {
                update(operation.copy(condition = operation.condition.copy(kind = it)))
            }
            is EffectOperation.AddModifier -> ChoiceField("Bônus", operation.modifier.amount.coerceIn(-10, 10), (-10..10).toList(), enabled) {
                update(operation.copy(modifier = operation.modifier.copy(amount = it)))
            }
            is EffectOperation.SpendResource -> {
                ChoiceField("Recurso", operation.resource, SpendableResource.entries, enabled) { update(operation.copy(resource = it)) }
                ChoiceField("Quantidade", operation.amount.coerceIn(1, 20), (1..20).toList(), enabled) { update(operation.copy(amount = it)) }
            }
            is EffectOperation.ConsumeItemState -> ChoiceField("Quantidade", operation.amount.coerceIn(1, 20), (1..20).toList(), enabled) { update(operation.copy(amount = it)) }
        }
        RemoveButton(enabled, "Remover operação", remove)
    }
}

@Composable
private fun FixedAmountField(amount: DiceOrNumber, enabled: Boolean, update: (DiceOrNumber) -> Unit) {
    val fixed = (amount as? DiceOrNumber.Fixed)?.value ?: 1
    ChoiceField("Quantidade fixa", fixed.coerceIn(0, 20), (0..20).toList(), enabled) { update(DiceOrNumber.Fixed(it)) }
    if (amount is DiceOrNumber.Roll) Text("O dado existente será mantido até selecionar uma quantidade fixa: ${amount.dice}", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun AbilityCostField(ability: Ability, enabled: Boolean, modifier: Modifier = Modifier, update: (Ability) -> Unit) {
    val amount = ability.cost.amount
    val minimum = if (ability.kind == AbilityKind.ASH) 1 else 0
    ChoiceField("Custo", amount.coerceIn(minimum, 20), (minimum..20).toList(), enabled, modifier) { value ->
        update(ability.copy(cost = abilityCostWithAmount(ability, value)))
    }
}

internal fun abilityCostWithAmount(ability: Ability, value: Int): AbilityCost = when (ability.kind) {
    AbilityKind.POWER -> {
        val res = (ability.cost as? AbilityCost.PowerCost)?.resource ?: SpendableResource.PE
        AbilityCost.PowerCost(
            value.coerceAtLeast(0),
            res,
            res == SpendableResource.PD || (ability.cost as? AbilityCost.PowerCost)?.isDivineOrLuckBased == true,
        )
    }
    AbilityKind.SPELL -> AbilityCost.SpellCost(value.coerceAtLeast(0))
    AbilityKind.RUNE -> AbilityCost.RuneCost(value.coerceAtLeast(0))
    AbilityKind.ASH -> AbilityCost.AshCost(value.coerceAtLeast(1))
}

private val triggerOptions = listOf("Nenhum", "Manual", "Ao aplicar", "Início do turno", "Fim do turno")
private fun triggerLabel(value: TriggerSpec?): String = when (value) {
    null -> "Nenhum"
    TriggerSpec.Manual -> "Manual"
    TriggerSpec.OnApply -> "Ao aplicar"
    TriggerSpec.StartOfTurn -> "Início do turno"
    TriggerSpec.EndOfTurn -> "Fim do turno"
    else -> "Condicional"
}
private fun triggerFrom(value: String): TriggerSpec? = when (value) {
    "Manual" -> TriggerSpec.Manual
    "Ao aplicar" -> TriggerSpec.OnApply
    "Início do turno" -> TriggerSpec.StartOfTurn
    "Fim do turno" -> TriggerSpec.EndOfTurn
    else -> null
}
