package com.kinderman.sdo.domain.model

import java.util.UUID
import kotlinx.serialization.Serializable

enum class SessionResource { LIFE, SANITY, ENERGY, ARCANE, DESTINY, EXHAUSTION, CORRUPTION }

enum class SessionOperationType {
    DAMAGE,
    HEAL,
    RESOURCE,
    CONDITION_ADD,
    CONDITION_REMOVE,
    MONEY,
    DESTINY,
    REWARD,
    NOTE,
    REGION_FAILURE,
    ABILITY_USE,
    USAGE_RESET,
    EFFECT_OPERATION,
    MODIFIER_APPLY,
    ITEM_CONSUME,
}

data class SessionCommand(
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val type: SessionOperationType,
    val amount: Int = 0,
    val resource: SessionResource? = null,
    val regionId: String = "",
    val targetId: String = "",
    val label: String = "",
    val detail: String = "",
    val reason: String = "",
    val effectOperation: EffectOperation? = null,
    val condition: ConditionPayload? = null,
    val conditionId: ConditionInstanceId? = null,
    val bodyRegion: BodyRegionSlot? = null,
)

@Serializable
data class CanonicalSessionPayload(
    val schemaVersion: Int = CANONICAL_SCHEMA_VERSION,
    val effectOperations: List<EffectOperation> = emptyList(),
    val condition: ConditionPayload? = null,
    val conditionInstanceId: ConditionInstanceId? = null,
    val bodyRegion: BodyRegionSlot? = null,
    val abilityId: String? = null,
    val abilityCost: AbilityCost? = null,
)

data class SessionOperation(
    val id: String = UUID.randomUUID().toString(),
    val idempotencyKey: String = "",
    val campaignId: String = "",
    val characterId: String = "",
    val actorId: String = "",
    val type: SessionOperationType = SessionOperationType.RESOURCE,
    val target: String = "",
    val previousValue: String = "",
    val newValue: String = "",
    val amount: Int = 0,
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = true,
    val lastSyncedAt: Long = 0,
    val canonicalPayload: CanonicalSessionPayload? = null,
    val baseCharacterUpdatedAt: Long = 0,
)

data class SessionMutation(
    val character: Character,
    val operation: SessionOperation,
)

fun Character.applySessionCommand(command: SessionCommand, actorId: String): SessionMutation {
    val now = System.currentTimeMillis()
    var target = command.resource?.name.orEmpty()
    var previous = ""
    var next = ""
    val changed = when (command.type) {
        SessionOperationType.DAMAGE -> {
            val slot = command.bodyRegion ?: BodyRegionSlot.fromName(command.regionId)
                ?: throw DomainError.InvalidReference("Selecione uma região corporal válida.")
            val region = canonicalBodyState().region(slot)
            val absorbed = localProtection(region)
            val applied = (command.amount.coerceAtLeast(0) - absorbed).coerceAtLeast(0)
            target = slot.label
            previous = life.current.toString()
            val injuredBody = canonicalBodyState().withInjury(
                slot,
                InjuryEvent(
                    source = sessionSource(command),
                    damage = DamageAmount(DamageType.Physical, command.amount.coerceAtLeast(0)),
                ),
            )
            withCanonicalBodyState(injuredBody).copy(life = life.withSessionCurrent(life.current - applied, lifeMaximum)).also {
                next = it.life.current.toString()
            }
        }
        SessionOperationType.HEAL -> mutateResource(
            resource = command.resource ?: SessionResource.LIFE,
            delta = command.amount.coerceAtLeast(0),
        ).also {
            val resource = command.resource ?: SessionResource.LIFE
            target = resource.name
            previous = resourceValue(resource).current.toString()
            next = it.resourceValue(resource).current.toString()
        }
        SessionOperationType.RESOURCE, SessionOperationType.DESTINY -> {
            val resource = command.resource ?: if (command.type == SessionOperationType.DESTINY) SessionResource.DESTINY
            else error("Selecione um recurso.")
            target = resource.name
            previous = resourceValue(resource).current.toString()
            mutateResource(resource, command.amount).also { next = it.resourceValue(resource).current.toString() }
        }
        SessionOperationType.CONDITION_ADD -> {
            val payload = command.condition ?: throw DomainError.ValidationError("Selecione uma condição tipada.")
            target = payload.kind.label
            previous = canonicalConditions().size.toString()
            withCanonicalConditions(canonicalConditions() + ConditionInstance(payload = payload, source = sessionSource(command)))
                .also { next = it.canonicalConditions().size.toString() }
        }
        SessionOperationType.CONDITION_REMOVE -> {
            val before = canonicalConditions()
            previous = before.size.toString()
            val removedId = command.conditionId?.value ?: command.targetId
            val removed = before.firstOrNull { it.instanceId.value == removedId }
                ?: throw DomainError.InvalidReference("Condição ativa não encontrada.")
            target = removed.name
            withCanonicalConditions(before.filterNot { it.instanceId.value == removedId })
                .also { next = it.canonicalConditions().size.toString() }
        }
        SessionOperationType.MONEY -> {
            target = "MONEY"
            previous = money.toString()
            copy(money = (money + command.amount).coerceAtLeast(0)).also { next = it.money.toString() }
        }
        SessionOperationType.NOTE -> {
            target = "NOTE"
            previous = personalNotes.size.toString()
            copy(personalNotes = personalNotes + PersonalNote(title = command.label.ifBlank { nextPersonalNoteTitle(personalNotes) }, text = command.detail))
                .also { next = it.personalNotes.size.toString() }
        }
        SessionOperationType.REGION_FAILURE -> {
            val slot = command.bodyRegion ?: BodyRegionSlot.fromName(command.regionId)
                ?: throw DomainError.InvalidReference("Selecione uma região corporal válida.")
            require(command.amount >= 0) { "Falhas adicionadas não podem ser negativas." }
            val before = canonicalBodyState().region(slot)
            target = slot.label
            previous = before.failures.toString()
            val changedBody = canonicalBodyState().withInjury(
                slot,
                InjuryEvent(source = sessionSource(command), failuresAdded = command.amount),
            )
            withCanonicalBodyState(changedBody).also { next = it.canonicalBodyState().region(slot).failures.toString() }
        }
        SessionOperationType.ABILITY_USE -> useAbility(command).also { result ->
            target = abilityName(command.targetId)
            val changedResources = SessionResource.entries.filter { resourceValue(it).current != result.resourceValue(it).current }
            previous = changedResources.joinToString(", ") { "${it.name}=${resourceValue(it).current}" }.ifBlank { "SEM CUSTO" }
            next = changedResources.joinToString(", ") { "${it.name}=${result.resourceValue(it).current}" }.ifBlank { "SEM ALTERAÇÃO" }
        }
        SessionOperationType.USAGE_RESET -> this.also {
            target = command.detail.ifBlank { "LEGADO" }
            previous = "SEM ALTERAÇÃO"
            next = "SEM ALTERAÇÃO"
        }
        SessionOperationType.EFFECT_OPERATION -> {
            val op = command.effectOperation ?: error("Operação canônica ausente.")
            return applyEffectOperation(
                operation = op,
                actorId = actorId,
                idempotencyKey = command.idempotencyKey,
                reason = command.reason.ifBlank { "Operação Canônica" },
            )
        }
        SessionOperationType.REWARD -> {
            target = command.label
            previous = ""
            next = command.detail
            this
        }
        SessionOperationType.MODIFIER_APPLY,
        SessionOperationType.ITEM_CONSUME -> throw DomainError.ValidationError(
            "Este tipo é gerado somente pela execução de uma EffectOperation estruturada.",
        )
    }.let { result ->
        if (command.type == SessionOperationType.USAGE_RESET || result == this) result
        else result.copy(updatedAt = now, dirty = true)
    }
    return SessionMutation(
        character = changed,
        operation = SessionOperation(
            id = command.idempotencyKey,
            idempotencyKey = command.idempotencyKey,
            campaignId = normalizeCampaignId(campaignId),
            characterId = id,
            actorId = actorId,
            type = command.type,
            target = target,
            previousValue = previous,
            newValue = next,
            amount = command.amount,
            reason = command.reason,
            createdAt = now,
            baseCharacterUpdatedAt = updatedAt,
            canonicalPayload = when (command.type) {
                SessionOperationType.CONDITION_ADD -> CanonicalSessionPayload(condition = command.condition)
                SessionOperationType.CONDITION_REMOVE -> CanonicalSessionPayload(
                    condition = canonicalConditions().firstOrNull { it.instanceId.value == (command.conditionId?.value ?: command.targetId) }?.payload,
                    conditionInstanceId = command.conditionId ?: command.targetId.takeIf(String::isNotBlank)?.let(::ConditionInstanceId),
                )
                SessionOperationType.DAMAGE, SessionOperationType.REGION_FAILURE -> CanonicalSessionPayload(bodyRegion = command.bodyRegion ?: BodyRegionSlot.fromName(command.regionId))
                SessionOperationType.EFFECT_OPERATION -> command.effectOperation?.let { CanonicalSessionPayload(effectOperations = listOf(it)) }
                SessionOperationType.ABILITY_USE -> allCanonicalAbilitiesSafely().firstOrNull { it.id == command.targetId }?.let { ability ->
                    CanonicalSessionPayload(
                        effectOperations = ability.mechanicalEffect?.operations.orEmpty(),
                        abilityId = ability.id,
                        abilityCost = ability.cost,
                    )
                }
                else -> null
            },
        ),
    )
}

private fun Character.useAbility(command: SessionCommand): Character {
    allCanonicalAbilitiesSafely().firstOrNull { it.id == command.targetId }?.let { ability ->
        require(ability.available) { "Esta habilidade não está disponível agora." }
        val paid = payAbilityCost(ability)
        return ability.mechanicalEffect?.operations.orEmpty().foldIndexed(paid) { index, current, operation ->
            current.applyEffectOperation(
                operation,
                idempotencyKey = "${command.idempotencyKey}:$index",
                reason = "Uso de habilidade: ${ability.id}",
            ).character
        }
    }
    val powerIndex = powers.indexOfFirst { it.id == command.targetId }
    if (powerIndex >= 0) {
        val power = powers[powerIndex]
        require(power.available) { "Este poder não está disponível agora." }
        return payCanonicalAbilityCost(power.costType, power.costValue, power.id)
    }
    val abilityIndex = mysticAbilities.indexOfFirst { it.id == command.targetId }
    require(abilityIndex >= 0) { "Habilidade não encontrada." }
    val ability = mysticAbilities[abilityIndex]
    require(ability.available) { "Esta habilidade não está disponível agora." }
    val prepared = if (ability.type.equals("Runa", true)) copy(
        mysticAbilities = mysticAbilities.replaceAbility(
            abilityIndex,
            ability.copy(
                inscriberId = id,
                inscriberPower = attributeTotal("POD"),
                inscriberRunicKnowledge = maxOf(acquiredKnowledgeValue("Rúnico"), acquiredKnowledgeValue("Runas")),
                revision = ability.revision + 1,
            ),
        ),
    ) else this
    val costType = if (ability.type.equals("Runa", true)) AbilityCostType.ARCANE else ability.costType
    return prepared.payCanonicalAbilityCost(costType, ability.costValue, ability.id)
}

private fun Character.payAbilityCost(ability: Ability): Character = when (val cost = ability.cost) {
    is AbilityCost.PowerCost -> payCanonicalAbilityCost(
        when (cost.resource) {
            SpendableResource.PV -> AbilityCostType.LIFE
            SpendableResource.PS -> AbilityCostType.SANITY
            SpendableResource.PE -> AbilityCostType.ENERGY
            SpendableResource.PM -> throw DomainError.ValidationError("Poder não pode gastar PM.")
            SpendableResource.PD -> AbilityCostType.DESTINY
        }, cost.amount, ability.id,
    )
    is AbilityCost.SpellCost -> mutateResource(SessionResource.ARCANE, -cost.amount)
    is AbilityCost.RuneCost -> mutateResource(SessionResource.ARCANE, -cost.amount)
    is AbilityCost.AshCost -> {
        val itemId = ability.ashData?.linkedItemId?.value
            ?: throw DomainError.InvalidReference("Cinza sem vínculo tipado com item.")
        val item = inventory.firstOrNull { it.id == itemId }
            ?: throw DomainError.InvalidReference("Item de Cinza não encontrado.")
        if (item.quantity < cost.amount) throw DomainError.InsufficientResource("Doses de Cinza insuficientes.")
        copy(inventory = inventory.map { if (it.id == item.id) it.copy(quantity = it.quantity - cost.amount) else it })
    }
}

private fun List<MysticAbility>.replaceAbility(index: Int, value: MysticAbility) = toMutableList().also { it[index] = value }

private fun Character.abilityName(id: String): String =
    allCanonicalAbilitiesSafely().firstOrNull { it.id == id }?.name
        ?: powers.firstOrNull { it.id == id }?.name
        ?: mysticAbilities.firstOrNull { it.id == id }?.name.orEmpty()

internal fun Character.payCanonicalAbilityCost(type: AbilityCostType, amount: Int, abilityId: String = ""): Character {
    val value = amount.coerceAtLeast(0)
    if (type == AbilityCostType.NONE || value == 0) return this
    if (type == AbilityCostType.DOSE) {
        val item = inventory.firstOrNull { it.linkedAshId == abilityId }
        require(item != null && item.quantity >= value) { "Sem cinzas necessárias" }
        return copy(inventory = inventory.map { if (it.id == item.id) it.copy(quantity = it.quantity - value) else it })
    }
    val resource = when (type) {
        AbilityCostType.ARCANE -> SessionResource.ARCANE
        AbilityCostType.ENERGY -> SessionResource.ENERGY
        AbilityCostType.DESTINY -> SessionResource.DESTINY
        AbilityCostType.LIFE -> SessionResource.LIFE
        AbilityCostType.SANITY -> SessionResource.SANITY
        AbilityCostType.NONE, AbilityCostType.DOSE -> error("Tipo de custo não consumível")
    }
    return mutateResource(resource, -value)
}

private fun Character.mutateResource(resource: SessionResource, delta: Int): Character {
    val value = resourceValue(resource)
    val maximum = resourceMaximum(resource)
    if (delta < 0) require(value.current + delta >= 0) { "Recurso insuficiente para esta ação." }
    val changed = value.withSessionCurrent(value.current + delta, maximum)
    return when (resource) {
        SessionResource.LIFE -> copy(life = changed)
        SessionResource.SANITY -> copy(sanity = changed)
        SessionResource.ENERGY -> copy(energy = changed)
        SessionResource.ARCANE -> copy(arcane = changed)
        SessionResource.DESTINY -> copy(destiny = changed)
        SessionResource.EXHAUSTION -> copy(exhaustion = changed)
        SessionResource.CORRUPTION -> copy(corruption = changed)
    }
}

fun Character.resourceValue(resource: SessionResource): ResourceValue = when (resource) {
    SessionResource.LIFE -> life
    SessionResource.SANITY -> sanity
    SessionResource.ENERGY -> energy
    SessionResource.ARCANE -> arcane
    SessionResource.DESTINY -> destiny
    SessionResource.EXHAUSTION -> exhaustion
    SessionResource.CORRUPTION -> corruption
}

fun Character.resourceMaximum(resource: SessionResource): Int = when (resource) {
    SessionResource.LIFE -> lifeMaximum
    SessionResource.SANITY -> sanityMaximum
    SessionResource.ENERGY -> energyMaximum
    SessionResource.ARCANE -> arcaneMaximum
    SessionResource.DESTINY -> destinyMaximum
    SessionResource.CORRUPTION -> 100
    SessionResource.EXHAUSTION -> resourceValue(resource).maximum
}

private fun ResourceValue.withSessionCurrent(value: Int, maximum: Int) = copy(current = value.coerceIn(0, maximum.coerceAtLeast(0)))
private fun sessionSource(command: SessionCommand): Source = Source.narrative(
    NarrativeSourceId(command.idempotencyKey.ifBlank { UUID.randomUUID().toString() }),
)

fun Character.applyEffectOperation(
    operation: EffectOperation,
    actorId: String = "",
    idempotencyKey: String = UUID.randomUUID().toString(),
    reason: String = "Operação Canônica",
): SessionMutation {
    val now = System.currentTimeMillis()
    var target = ""
    var previous = ""
    var next = ""

    val updated = when (operation) {
        is EffectOperation.Damage -> {
            val amount = when (val a = operation.amount) {
                is DiceOrNumber.Fixed -> a.value
                is DiceOrNumber.Roll -> a.dice.count * a.dice.die / 2 + a.dice.bonus
            }.coerceAtLeast(0)
            validateSessionTarget(operation.target)
            target = operation.target.kind
            previous = life.current.toString()
            val state = canonicalBodyState()
            val absorbed = if (operation.damageType == DamageType.True) 0 else when (val t = operation.target) {
                is TargetSpec.TargetRegion -> localProtection(state.region(t.slot))
                is TargetSpec.TargetOrgan, TargetSpec.Self, is TargetSpec.TargetCharacter -> equippedGeneralProtection
                is TargetSpec.TargetArea -> throw DomainError.ValidationError("Dano em área exige resolução explícita dos personagens atingidos.")
            }
            val applied = (amount - absorbed).coerceAtLeast(0)
            val newCurrent = (life.current - applied).coerceIn(0, lifeMaximum)
            next = newCurrent.toString()
            val event = InjuryEvent(
                source = Source.narrative(NarrativeSourceId(idempotencyKey)),
                damage = DamageAmount(operation.damageType, amount),
            )
            val injured = when (val t = operation.target) {
                is TargetSpec.TargetRegion -> state.withInjury(t.slot, event)
                is TargetSpec.TargetOrgan -> {
                    val found = state.organs.any { it.organ == t.slot }
                    state.copy(organs = if (found) state.organs.map { organ ->
                        if (organ.organ == t.slot) organ.copy(injuries = organ.injuries + event) else organ
                    } else state.organs + OrganState(organ = t.slot, injuries = listOf(event)))
                }
                else -> state
            }
            withCanonicalBodyState(injured).copy(life = life.copy(current = newCurrent))
        }
        is EffectOperation.Healing -> {
            validateSessionTarget(operation.target)
            val amount = when (val a = operation.amount) {
                is DiceOrNumber.Fixed -> a.value
                is DiceOrNumber.Roll -> a.dice.count * a.dice.die / 2 + a.dice.bonus
            }.coerceAtLeast(0)
            val sessionRes = when (operation.resource) {
                SpendableResource.PV -> SessionResource.LIFE
                SpendableResource.PS -> SessionResource.SANITY
                SpendableResource.PE -> SessionResource.ENERGY
                SpendableResource.PM -> SessionResource.ARCANE
                SpendableResource.PD -> SessionResource.DESTINY
            }
            target = sessionRes.name
            previous = resourceValue(sessionRes).current.toString()
            val res = mutateResource(sessionRes, amount)
            next = res.resourceValue(sessionRes).current.toString()
            res
        }
        is EffectOperation.ApplyCondition -> {
            validateSessionTarget(operation.target)
            target = operation.condition.kind.label
            val before = canonicalConditions()
            previous = before.size.toString()
            val payload = operation.condition.withTarget(operation.target)
            val res = withCanonicalConditions(before + ConditionInstance(payload = payload, source = Source.narrative(NarrativeSourceId(idempotencyKey))))
            next = res.canonicalConditions().size.toString()
            res
        }
        is EffectOperation.AddModifier -> {
            target = operation.modifier.target.kind
            previous = activeModifiers.size.toString()
            next = operation.modifier.amount.toString()
            val updated = when (operation.modifier.stacking) {
                StackingRule.Add -> activeModifiers + operation.modifier
                StackingRule.HighestOnly -> {
                    val highestOnly = activeModifiers.filter {
                        it.target == operation.modifier.target && it.stacking == StackingRule.HighestOnly
                    }
                    val candidates = highestOnly + operation.modifier
                    activeModifiers.filterNot { it in highestOnly } + candidates.maxBy { it.amount }
                }
                StackingRule.ReplaceBySource -> activeModifiers.filterNot {
                    it.target == operation.modifier.target && it.source == operation.modifier.source
                } + operation.modifier
            }
            next = updated.size.toString()
            copy(activeModifiers = updated)
        }
        is EffectOperation.SpendResource -> {
            val sessionRes = when (operation.resource) {
                SpendableResource.PV -> SessionResource.LIFE
                SpendableResource.PS -> SessionResource.SANITY
                SpendableResource.PE -> SessionResource.ENERGY
                SpendableResource.PM -> SessionResource.ARCANE
                SpendableResource.PD -> SessionResource.DESTINY
            }
            target = sessionRes.name
            previous = resourceValue(sessionRes).current.toString()
            val res = mutateResource(sessionRes, -operation.amount)
            next = res.resourceValue(sessionRes).current.toString()
            res
        }
        is EffectOperation.ConsumeItemState -> {
            if (operation.state != ConsumableStateKind.Dose) {
                throw DomainError.ValidationError("Este item ainda não possui estado canônico de carga/munição; operação recusada.")
            }
            val item = inventory.firstOrNull { it.id == operation.itemInstanceId.value }
                ?: throw DomainError.InvalidReference("Item não encontrado no inventário.")
            val canonicalItem = itemStates.firstOrNull { it.id == operation.itemInstanceId }
                ?: throw DomainError.InvalidReference("Estado canônico do item não encontrado; migre a ficha antes do consumo.")
            val consumable = canonicalItem.consumable
                ?: throw DomainError.InvalidReference("Item não possui doses estruturadas.")
            target = item.name
            previous = consumable.doses.toString()
            if (consumable.doses < operation.amount) throw DomainError.InsufficientResource("Doses insuficientes do item.")
            val nextQuantity = consumable.doses - operation.amount
            next = nextQuantity.toString()
            copy(
                inventory = inventory.map { if (it.id == item.id) it.copy(quantity = nextQuantity) else it },
                itemStates = itemStates.map {
                    if (it.id == canonicalItem.id) it.copy(
                        stack = it.stack.copy(quantity = nextQuantity),
                        consumable = consumable.copy(doses = nextQuantity),
                    ) else it
                },
            )
        }
    }.copy(updatedAt = now, dirty = true)

    return SessionMutation(
        character = updated,
        operation = SessionOperation(
            id = idempotencyKey,
            idempotencyKey = idempotencyKey,
            campaignId = normalizeCampaignId(campaignId),
            characterId = id,
            actorId = actorId,
            type = when (operation) {
                is EffectOperation.Damage -> SessionOperationType.DAMAGE
                is EffectOperation.Healing -> SessionOperationType.HEAL
                is EffectOperation.ApplyCondition -> SessionOperationType.CONDITION_ADD
                is EffectOperation.AddModifier -> SessionOperationType.MODIFIER_APPLY
                is EffectOperation.SpendResource -> SessionOperationType.RESOURCE
                is EffectOperation.ConsumeItemState -> SessionOperationType.ITEM_CONSUME
            },
            target = target,
            previousValue = previous,
            newValue = next,
            amount = when (operation) {
                is EffectOperation.SpendResource -> operation.amount
                is EffectOperation.ConsumeItemState -> operation.amount
                else -> 0
            },
            reason = reason,
            createdAt = now,
            canonicalPayload = CanonicalSessionPayload(effectOperations = listOf(operation)),
            baseCharacterUpdatedAt = updatedAt,
        ),
    )
}

private fun ConditionPayload.withTarget(target: TargetSpec): ConditionPayload = when (target) {
    TargetSpec.Self -> this
    is TargetSpec.TargetCharacter -> this
    is TargetSpec.TargetRegion -> copy(targetRegion = target.slot)
    is TargetSpec.TargetOrgan -> copy(targetOrgan = target.slot)
    is TargetSpec.TargetArea -> throw DomainError.ValidationError("Condição em área precisa ser aplicada por resolução de alvos da sessão.")
}

private fun Character.validateSessionTarget(target: TargetSpec) {
    when (target) {
        TargetSpec.Self -> Unit
        is TargetSpec.TargetCharacter -> if (target.characterId.value != id) {
            throw DomainError.InvalidReference("A operação aponta para outro personagem; selecione o alvo antes de aplicar.")
        }
        is TargetSpec.TargetRegion, is TargetSpec.TargetOrgan -> Unit
        is TargetSpec.TargetArea -> throw DomainError.ValidationError("Alvos em área precisam ser resolvidos explicitamente na sessão.")
    }
}
