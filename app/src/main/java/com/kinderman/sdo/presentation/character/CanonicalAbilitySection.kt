package com.kinderman.sdo.presentation.character

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
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
import com.kinderman.sdo.ui.TechPanel

@Composable
internal fun CanonicalAbilitySection(
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
        SectionHeader(if (powersOnly) "08" else "09", if (powersOnly) "Poderes" else "Habilidades")
        abilities.forEach { ability -> CanonicalAbilityEditor(character, ability, kinds, enabled, onChange) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { selecting = true }, enabled = enabled && catalog.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Text("+ CATÁLOGO")
            }
            TextButton(onClick = {
                val kind = kinds.firstOrNull() ?: AbilityKind.POWER
                val ability = Ability(
                    name = "Nova habilidade", kind = kind,
                    source = Source.narrative(NarrativeSourceId("manual")), cost = defaultCost(kind),
                )
                onChange(character.copy(abilities = character.allCanonicalAbilitiesSafely() + ability))
            }, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text("+ NARRATIVA")
            }
        }
    }
    if (selecting) CatalogPickerDialog("SELECIONAR HABILIDADE", catalog, { selecting = false }) { entry ->
        val result = runCatching {
            when (entry.kind) {
                CatalogKind.POWER -> entry.toStructuredPower(PowerSourceType.NARRATIVE, entry.id).toCanonicalAbility()
                else -> entry.toMysticAbility().toCanonicalAbility()
            }
        }
        result.onSuccess { ability ->
            onChange(character.copy(abilities = character.allCanonicalAbilitiesSafely() + ability))
            selecting = false
        }.onFailure { error ->
            android.widget.Toast.makeText(context, error.message ?: "Entrada inválida no catálogo.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun CanonicalAbilityEditor(character: Character, ability: Ability, allowedKinds: Set<AbilityKind>, enabled: Boolean, onChange: (Character) -> Unit) {
    val published = ability.definition != null
    var expanded by rememberSaveable(ability.id) { mutableStateOf(false) }
    val isRacial = ability.source?.kind == SourceKind.Race
    val mechanicsEditable = enabled && !published && !isRacial
    var nameDraft by rememberSaveable(ability.id, ability.name) { mutableStateOf(ability.name) }
    fun update(value: Ability) = onChange(character.copy(
        abilities = character.allCanonicalAbilitiesSafely().map { if (it.id == ability.id) value.copy(revision = ability.revision + 1) else it },
    ))
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (ability.kind == AbilityKind.POWER) ability.name else ability.kind.label.uppercase(),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
                style = if (ability.kind == AbilityKind.POWER) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            )
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Recolher ${ability.name}" else "Expandir ${ability.name}")
            RemoveButton(enabled && !isRacial, if (isRacial) "Poder racial não pode ser removido" else "Remover habilidade") {
                onChange(character.withRemovedCanonicalAbility(ability.id))
            }
        }
        if (ability.kind != AbilityKind.POWER) {
            Text(ability.name, style = MaterialTheme.typography.titleMedium)
        }
        Text(ability.compactSummary(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        if (isRacial) Text("PODER RACIAL // VINCULADO À RAÇA", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
        if (!expanded) {
            Text(ability.effect.ifBlank { "Sem descrição." }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            return@Column
        }
        else HudTextField("Nome", nameDraft, enabled = mechanicsEditable) { value ->
            nameDraft = value
            if (value.isNotBlank()) update(ability.copy(name = value))
        }
        val durationKind = ability.duration?.kind ?: DurationKind.Instant
        val trigger = ability.mechanicalEffect?.trigger
        TwoFields(
            { field -> ChoiceField("Categoria", ability.kind, allowedKinds.toList(), mechanicsEditable, field, display = AbilityKind::label) { kind ->
                update(ability.copy(kind = kind, cost = defaultCost(kind)))
            } },
            { field -> ChoiceField("Execução", ability.execution.kind, ExecutionKind.entries, mechanicsEditable, field, display = ExecutionKind::label) { kind ->
                update(ability.copy(execution = Execution(kind, if (kind == ExecutionKind.Timed) TimedExecution(1, ExecutionTimeUnit.Minutes) else null)))
            } },
        )
        TwoFields(
            { field -> ChoiceField("Alcance", ability.range.band, RangeBand.entries, mechanicsEditable, field, display = RangeBand::label) { update(ability.copy(range = RangeSpec(it))) } },
            { field -> ChoiceField("Duração", durationKind, DurationKind.entries, mechanicsEditable, field, display = DurationKind::label) { kind ->
                update(ability.copy(duration = when (kind) {
                    DurationKind.Turns -> Duration(kind, turns = 1)
                    DurationKind.Timed -> Duration(kind, timed = TimedDuration(1, DurationTimeUnit.Hours))
                    else -> Duration(kind)
                }))
            } },
        )
        TwoFields(
            { field -> CanonicalCostField(ability, mechanicsEditable, field, ::update) },
            { field -> ChoiceField("Gatilho", triggerLabel(trigger), triggerOptions, mechanicsEditable, field) { selected ->
                val operations = ability.mechanicalEffect?.operations.orEmpty()
                val effect = operations.takeIf { it.isNotEmpty() }?.let { MechanicalEffect(it, ability.mechanicalEffect?.usage, triggerFrom(selected)) }
                update(ability.copy(mechanicalEffect = effect))
            } },
        )
        HudTextField("Descrição", ability.effect, multiline = true, enabled = mechanicsEditable) { update(ability.copy(effect = it)) }
        Text("OPERAÇÕES ESTRUTURADAS", color = MaterialTheme.colorScheme.primary)
        ability.mechanicalEffect?.operations.orEmpty().forEachIndexed { index, operation ->
            CanonicalOperationEditor(operation, mechanicsEditable, { replacement ->
                val operations = ability.mechanicalEffect!!.operations.mapIndexed { current, value -> if (current == index) replacement else value }
                update(ability.copy(mechanicalEffect = MechanicalEffect(operations, ability.mechanicalEffect.usage, ability.mechanicalEffect.trigger)))
            }, {
                val operations = ability.mechanicalEffect!!.operations.filterIndexed { current, _ -> current != index }
                update(ability.copy(mechanicalEffect = operations.takeIf { it.isNotEmpty() }?.let { MechanicalEffect(it, ability.mechanicalEffect.usage, ability.mechanicalEffect.trigger) }))
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
private fun CanonicalOperationEditor(operation: EffectOperation, enabled: Boolean, update: (EffectOperation) -> Unit, remove: () -> Unit) {
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
private fun CanonicalCostField(ability: Ability, enabled: Boolean, modifier: Modifier = Modifier, update: (Ability) -> Unit) {
    val amount = ability.cost.amount
    ChoiceField("Custo", amount.coerceIn(0, 20), (0..20).toList(), enabled, modifier) { value ->
        val cost = when (ability.kind) {
            AbilityKind.POWER -> AbilityCost.PowerCost(value, (ability.cost as? AbilityCost.PowerCost)?.resource ?: SpendableResource.PE)
            AbilityKind.SPELL -> AbilityCost.SpellCost(value.coerceAtLeast(1))
            AbilityKind.RUNE -> AbilityCost.RuneCost(value.coerceAtLeast(1))
            AbilityKind.ASH -> AbilityCost.AshCost(value.coerceAtLeast(1))
        }
        update(ability.copy(cost = cost))
    }
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
