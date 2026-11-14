package com.kinderman.sdo.presentation.historian

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinderman.sdo.domain.model.BodyRegionSlot
import com.kinderman.sdo.domain.model.CampaignAlertSettings
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ConditionInstance
import com.kinderman.sdo.domain.model.ConditionKind
import com.kinderman.sdo.domain.model.ConditionPayload
import com.kinderman.sdo.domain.model.Duration
import com.kinderman.sdo.domain.model.DurationKind
import com.kinderman.sdo.domain.model.DurationTimeUnit
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SessionResource
import com.kinderman.sdo.domain.model.TimedDuration
import com.kinderman.sdo.domain.model.canonicalBodyState
import com.kinderman.sdo.domain.model.canonicalConditions
import com.kinderman.sdo.presentation.character.ChoiceField
import com.kinderman.sdo.presentation.character.IntegerField
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.SdoActionButton
import com.kinderman.sdo.ui.SdoActionStyle
import com.kinderman.sdo.ui.SdoCardTitle
import com.kinderman.sdo.ui.SdoFilterChip
import com.kinderman.sdo.ui.SdoInsetCard
import com.kinderman.sdo.ui.SdoMetadataText
import com.kinderman.sdo.ui.SdoResponsiveGrid

internal enum class QuickActionKind(val label: String) {
    DAMAGE("DANO"),
    HEAL("CURA"),
    RESOURCE("RECURSO"),
    CONDITION_ADD("+CONDIÇÃO"),
    CONDITION_REMOVE("-CONDIÇÃO"),
    REGION("FALHA"),
    MONEY("DINHEIRO"),
    DESTINY("DESTINO"),
    USAGE_RESET("REINICIAR USOS"),
    NOTE("ANOTAÇÃO"),
}

internal enum class OperationFilter(val label: String) {
    ALL("TODOS"),
    WITH_ALERTS("COM ALERTA"),
    CRITICAL("CRÍTICOS"),
    CONDITIONS("CONDIÇÕES"),
}

internal data class OperationalTelemetry(
    val alerts: List<String>,
    val isCritical: Boolean,
    val conditionCount: Int,
    val failureCount: Int,
)

internal fun evaluateOperationalTelemetry(
    character: Character,
    settings: CampaignAlertSettings,
): OperationalTelemetry {
    val typedConditions = character.canonicalConditions()
    val body = runCatching { character.canonicalBodyState() }.getOrNull()
    val failedRegions = body?.regions.orEmpty().filter { it.failures > 0 }
    val failedOrgans = body?.organs.orEmpty().filter { it.failures > 0 }
    val lifeCritical = character.lifeMaximum > 0 &&
        character.life.current * 100 <= character.lifeMaximum * settings.lifeThresholdPercent
    val sanityCritical = character.sanityMaximum > 0 &&
        character.sanity.current * 100 <= character.sanityMaximum * settings.sanityThresholdPercent
    val exhaustionCritical = character.exhaustion.maximum > 0 &&
        character.exhaustion.current * 100 >= character.exhaustion.maximum * settings.exhaustionThresholdPercent

    val alerts = buildList {
        if (lifeCritical) add("VIDA CRÍTICA")
        if (sanityCritical) add("SANIDADE CRÍTICA")
        if (exhaustionCritical) add("EXAUSTÃO ALTA")
        if (settings.alertConditions && typedConditions.isNotEmpty()) {
            add("${typedConditions.size} CONDIÇÃO(ÕES)")
        }
        if (settings.alertBodyFailures && (failedRegions.isNotEmpty() || failedOrgans.isNotEmpty())) {
            add("FALHA CORPORAL")
        }
        if (character.lastSyncedAt > 0 &&
            System.currentTimeMillis() - character.lastSyncedAt > settings.staleAfterHours * 3_600_000L
        ) {
            add("SYNC ANTIGO")
        }
        if (character.dirty) add("ALTERAÇÃO LOCAL")
    }
    return OperationalTelemetry(
        alerts = alerts,
        isCritical = lifeCritical || sanityCritical || exhaustionCritical,
        conditionCount = typedConditions.size,
        failureCount = failedRegions.size + failedOrgans.size,
    )
}

internal fun SessionResource.displayLabel(): String = when (this) {
    SessionResource.LIFE -> "Vida"
    SessionResource.SANITY -> "Sanidade"
    SessionResource.ENERGY -> "Energia"
    SessionResource.ARCANE -> "Arcano"
    SessionResource.DESTINY -> "Destino"
    SessionResource.EXHAUSTION -> "Exaustão"
    SessionResource.CORRUPTION -> "Corrupção"
}

internal fun ConditionInstance.summaryLabel(): String = buildString {
    append(name.uppercase())
    intensity?.let { append(" $it") }
    duration?.let { dur ->
        append(" • ")
        append(
            when (dur.kind) {
                DurationKind.Turns -> "${dur.turns ?: 1} turno(s)"
                DurationKind.Timed -> "${dur.timed?.amount ?: 1}h"
                else -> dur.kind.label
            },
        )
    }
}

@Composable
internal fun QuickActionsDialog(
    character: Character,
    campaignCharacters: List<Character>,
    initialAction: QuickActionKind? = null,
    writable: Boolean = true,
    onDismiss: () -> Unit,
    onApply: (List<Pair<Character, SessionCommand>>) -> Unit,
) {
    var selected by rememberSaveable(character.id, initialAction) {
        mutableStateOf(initialAction)
    }
    var amount by rememberSaveable(character.id) { mutableIntStateOf(1) }
    var negativeDelta by rememberSaveable(character.id) { mutableStateOf(false) }
    var label by rememberSaveable(character.id) { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf(ConditionKind.Abalado) }
    var durationKind by remember { mutableStateOf(DurationKind.Scene) }
    var durationAmount by rememberSaveable(character.id) { mutableIntStateOf(1) }
    var conditionIntensity by rememberSaveable(character.id) { mutableIntStateOf(0) }
    var resource by remember { mutableStateOf(SessionResource.ENERGY) }
    var regionIndex by rememberSaveable(character.id) { mutableIntStateOf(0) }
    var selectedTargetIds by remember(character.id) { mutableStateOf(setOf(character.id)) }

    val targets = remember(campaignCharacters, selectedTargetIds, character) {
        val chosen = campaignCharacters.filter { it.id in selectedTargetIds }
        chosen.ifEmpty { listOf(character) }
    }
    val regions = character.canonicalBodyState().regions
    val region = regions.getOrNull(regionIndex)
    val signedAmount = if (negativeDelta) -amount else amount

    val removableConditions = remember(targets) {
        targets.flatMap { it.canonicalConditions() }
            .distinctBy { it.name.lowercase() }
    }
    var conditionToRemoveName by remember(removableConditions) {
        mutableStateOf(removableConditions.firstOrNull()?.name.orEmpty())
    }

    val canBuildCommand = when (selected) {
        null -> false
        QuickActionKind.CONDITION_REMOVE -> conditionToRemoveName.isNotBlank() &&
            targets.any { target ->
                target.canonicalConditions().any { it.name.equals(conditionToRemoveName, ignoreCase = true) }
            }
        else -> true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Text(
                if (targets.size > 1) "AÇÃO RÁPIDA // ${targets.size} FICHAS"
                else "AÇÃO RÁPIDA // ${character.name.uppercase()}",
            )
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("1. ESCOLHA A AÇÃO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                SdoResponsiveGrid(QuickActionKind.entries, minItemWidth = 116.dp) { action, modifier ->
                    SdoFilterChip(action.label, selected == action, { selected = action }, modifier)
                }

                if (campaignCharacters.size > 1) {
                    Text("ALVOS (${targets.size}/${campaignCharacters.size})", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    SdoResponsiveGrid(listOf("CURRENT", "ALL"), minItemWidth = 120.dp) { mode, modifier ->
                        when (mode) {
                            "CURRENT" -> SdoActionButton("SOMENTE ATUAL", { selectedTargetIds = setOf(character.id) }, modifier)
                            else -> SdoActionButton("TODOS (${campaignCharacters.size})", {
                                selectedTargetIds = campaignCharacters.mapTo(linkedSetOf(), Character::id)
                            }, modifier)
                        }
                    }
                    SdoResponsiveGrid(campaignCharacters, minItemWidth = 120.dp) { member, modifier ->
                        val isSelected = member.id in selectedTargetIds
                        SdoFilterChip(
                            label = member.name.ifBlank { "Sem nome" },
                            selected = isSelected,
                            onClick = {
                                selectedTargetIds = if (isSelected && selectedTargetIds.size > 1) {
                                    selectedTargetIds - member.id
                                } else {
                                    selectedTargetIds + member.id
                                }
                            },
                            modifier = modifier,
                        )
                    }
                }

                Text("2. DEFINA OS DADOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                if (selected in listOf(
                        QuickActionKind.DAMAGE,
                        QuickActionKind.HEAL,
                        QuickActionKind.RESOURCE,
                        QuickActionKind.REGION,
                        QuickActionKind.MONEY,
                        QuickActionKind.DESTINY,
                    )
                ) {
                    SdoInsetCard(verticalSpacing = 8.dp) {
                        val supportsSign = selected in listOf(QuickActionKind.RESOURCE, QuickActionKind.MONEY, QuickActionKind.DESTINY)
                        val displayValue = if (supportsSign && negativeDelta) "-$amount" else if (supportsSign) "+$amount" else "$amount"
                        SdoCardTitle("VALOR // $displayValue", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        if (supportsSign) {
                            SdoResponsiveGrid(listOf("ADD", "SUB"), minItemWidth = 126.dp) { sign, modifier ->
                                when (sign) {
                                    "ADD" -> SdoFilterChip("RESTAURAR (+)", !negativeDelta, { negativeDelta = false }, modifier)
                                    else -> SdoFilterChip("GASTAR / REDUZIR (−)", negativeDelta, { negativeDelta = true }, modifier)
                                }
                            }
                        }
                        SdoResponsiveGrid(listOf("DECREASE", "INCREASE"), minItemWidth = 126.dp) { action, modifier ->
                            when (action) {
                                "DECREASE" -> SdoActionButton("DIMINUIR", { amount = (amount - 1).coerceAtLeast(1) }, modifier)
                                else -> SdoActionButton("AUMENTAR", { amount += 1 }, modifier)
                            }
                        }
                    }
                }

                if (selected == QuickActionKind.RESOURCE) {
                    ChoiceField(
                        label = "Recurso",
                        value = resource,
                        options = SessionResource.entries,
                        enabled = writable,
                        display = { it.displayLabel() },
                    ) { resource = it }
                }

                if (selected == QuickActionKind.DAMAGE || selected == QuickActionKind.REGION) {
                    SdoActionButton(
                        label = "REGIÃO // ${region?.name?.uppercase() ?: "NENHUMA"}",
                        onClick = { if (regions.isNotEmpty()) regionIndex = (regionIndex + 1) % regions.size },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = regions.isNotEmpty(),
                    )
                }

                if (selected == QuickActionKind.CONDITION_ADD) {
                    ChoiceField("Condição", selectedCondition, ConditionKind.entries, writable, display = { it.label }) {
                        selectedCondition = it
                    }
                    ChoiceField("Duração", durationKind, DurationKind.entries, writable, display = { it.label }) {
                        durationKind = it
                    }
                    IntegerField("Intensidade (0 = padrão)", conditionIntensity, writable) {
                        conditionIntensity = it.coerceAtLeast(0)
                    }
                    if (durationKind == DurationKind.Turns || durationKind == DurationKind.Timed) {
                        IntegerField(
                            label = if (durationKind == DurationKind.Turns) "Quantidade de turnos" else "Horas",
                            value = durationAmount,
                            enabled = writable,
                        ) { durationAmount = it.coerceAtLeast(1) }
                    }
                }

                if (selected == QuickActionKind.CONDITION_REMOVE) {
                    if (removableConditions.isEmpty()) {
                        SdoMetadataText("Nenhuma condição ativa nos alvos selecionados.")
                    } else {
                        SdoResponsiveGrid(removableConditions, minItemWidth = 132.dp) { cond, modifier ->
                            SdoFilterChip(
                                label = cond.name.uppercase(),
                                selected = cond.name.equals(conditionToRemoveName, ignoreCase = true),
                                onClick = { conditionToRemoveName = cond.name },
                                modifier = modifier,
                            )
                        }
                    }
                }

                HudTextField("Motivo / anotação", label, enabled = writable, onValue = { label = it })
                Text("3. CONFIRME", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = selected?.let { kind ->
                        val targetLabel = if (targets.size > 1) "${targets.size} personagens" else character.name
                        "${kind.label} // $targetLabel"
                    } ?: "Selecione uma ação acima.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            SdoActionButton(
                label = "APLICAR",
                onClick = {
                    val kind = selected ?: return@SdoActionButton
                    val commands = buildQuickActionCommands(
                        kind = kind,
                        targets = targets,
                        amount = amount,
                        signedAmount = signedAmount,
                        resource = resource,
                        region = region?.region,
                        selectedCondition = selectedCondition,
                        conditionIntensity = conditionIntensity,
                        durationKind = durationKind,
                        durationAmount = durationAmount,
                        conditionToRemoveName = conditionToRemoveName,
                        reason = label,
                    )
                    if (commands.isNotEmpty()) onApply(commands)
                },
                enabled = writable && canBuildCommand,
                style = SdoActionStyle.PRIMARY,
            )
        },
        dismissButton = { SdoActionButton("CANCELAR", onDismiss) },
    )
}

private fun buildQuickActionCommands(
    kind: QuickActionKind,
    targets: List<Character>,
    amount: Int,
    signedAmount: Int,
    resource: SessionResource,
    region: BodyRegionSlot?,
    selectedCondition: ConditionKind,
    conditionIntensity: Int,
    durationKind: DurationKind,
    durationAmount: Int,
    conditionToRemoveName: String,
    reason: String,
): List<Pair<Character, SessionCommand>> = targets.mapNotNull { target ->
    val command = when (kind) {
        QuickActionKind.DAMAGE -> SessionCommand(
            type = SessionOperationType.DAMAGE,
            amount = amount,
            bodyRegion = region,
            reason = reason,
        )
        QuickActionKind.HEAL -> SessionCommand(
            type = SessionOperationType.HEAL,
            amount = amount,
            resource = SessionResource.LIFE,
            reason = reason,
        )
        QuickActionKind.RESOURCE -> SessionCommand(
            type = SessionOperationType.RESOURCE,
            amount = signedAmount,
            resource = resource,
            reason = reason,
        )
        QuickActionKind.CONDITION_ADD -> SessionCommand(
            type = SessionOperationType.CONDITION_ADD,
            condition = ConditionPayload(
                kind = selectedCondition,
                intensity = conditionIntensity.takeIf { it > 0 },
                duration = when (durationKind) {
                    DurationKind.Turns -> Duration(durationKind, turns = durationAmount)
                    DurationKind.Timed -> Duration(durationKind, timed = TimedDuration(durationAmount, DurationTimeUnit.Hours))
                    else -> Duration(durationKind)
                },
            ),
            reason = reason,
        )
        QuickActionKind.CONDITION_REMOVE -> {
            val match = target.canonicalConditions().firstOrNull {
                it.name.equals(conditionToRemoveName, ignoreCase = true)
            } ?: return@mapNotNull null
            SessionCommand(
                type = SessionOperationType.CONDITION_REMOVE,
                conditionId = match.instanceId,
                targetId = match.instanceId.value,
                reason = reason.ifBlank { "Removida pelo Historiador" },
            )
        }
        QuickActionKind.REGION -> SessionCommand(
            type = SessionOperationType.REGION_FAILURE,
            amount = amount,
            bodyRegion = region,
            reason = reason,
        )
        QuickActionKind.MONEY -> SessionCommand(
            type = SessionOperationType.MONEY,
            amount = signedAmount,
            reason = reason,
        )
        QuickActionKind.DESTINY -> SessionCommand(
            type = SessionOperationType.DESTINY,
            amount = signedAmount,
            resource = SessionResource.DESTINY,
            reason = reason,
        )
        QuickActionKind.USAGE_RESET -> SessionCommand(
            type = SessionOperationType.USAGE_RESET,
            detail = reason.ifBlank { "Reinício de cena/descanso" },
            reason = reason,
        )
        QuickActionKind.NOTE -> SessionCommand(
            type = SessionOperationType.NOTE,
            label = reason.ifBlank { "Anotação do Historiador" },
            detail = "Registro operacional",
            reason = reason,
        )
    }
    target to command
}
