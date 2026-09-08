package com.kinderman.sdo.domain.model

import java.util.UUID

enum class SessionResource { LIFE, SANITY, ENERGY, ARCANE, DESTINY, EXHAUSTION, CORRUPTION }
enum class UsagePeriod { NONE, TURN, SCENE, REST }

data class AbilityUsageLimit(
    val period: UsagePeriod = UsagePeriod.NONE,
    val maximum: Int = 0,
    val used: Int = 0,
    val periodKey: String = "",
) {
    val remaining: Int? get() = maximum.takeIf { it > 0 }?.let { (it - used).coerceAtLeast(0) }
    val available: Boolean get() = maximum <= 0 || used < maximum
}

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
            val region = bodyRegions.firstOrNull { it.idOrName() == command.regionId }
                ?: error("Selecione uma região corporal válida.")
            val absorbed = localProtectionBreakdown(region).total
            val applied = (command.amount.coerceAtLeast(0) - absorbed).coerceAtLeast(0)
            target = region.name
            previous = life.current.toString()
            copy(life = life.withSessionCurrent(life.current - applied, lifeMaximum)).also {
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
            require(command.label.isNotBlank()) { "Informe a condição." }
            target = command.label
            previous = conditions.size.toString()
            copy(
                conditions = conditions + ConditionEffect(
                    name = command.label,
                    intensity = command.amount.takeIf { it != 0 }?.toString().orEmpty(),
                    duration = command.detail,
                    origin = command.reason,
                    summary = command.detail,
                ),
            ).also { next = it.conditions.size.toString() }
        }
        SessionOperationType.CONDITION_REMOVE -> {
            previous = conditions.size.toString()
            target = conditions.firstOrNull { it.id == command.targetId }?.name.orEmpty()
            copy(conditions = conditions.filterNot { it.id == command.targetId }).also { next = it.conditions.size.toString() }
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
            val index = bodyRegions.indexOfFirst { it.idOrName() == command.regionId }
            require(index >= 0) { "Selecione uma região corporal válida." }
            target = bodyRegions[index].name
            previous = bodyRegions[index].failures.toString()
            val regions = bodyRegions.toMutableList()
            regions[index] = regions[index].copy(failures = (regions[index].failures + command.amount).coerceAtLeast(0))
            copy(bodyRegions = regions).also { next = it.bodyRegions[index].failures.toString() }
        }
        SessionOperationType.ABILITY_USE -> useAbility(command).also { result ->
            target = command.targetId
            previous = "READY"
            next = "USED"
        }
        SessionOperationType.USAGE_RESET -> resetUsage(command.detail).also {
            target = command.detail
            previous = "USED"
            next = "READY"
        }
        SessionOperationType.REWARD -> {
            target = command.label
            previous = ""
            next = command.detail
            this
        }
    }.copy(updatedAt = now, dirty = true)
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
        ),
    )
}

private fun Character.useAbility(command: SessionCommand): Character {
    val powerIndex = powers.indexOfFirst { it.id == command.targetId }
    if (powerIndex >= 0) {
        val power = powers[powerIndex]
        require(power.available && power.usage.available) { "Este poder não está disponível agora." }
        val paid = mutateResource(power.costResource, -power.costAmount)
        val updated = paid.powers.toMutableList()
        updated[powerIndex] = power.copy(usage = power.usage.copy(used = power.usage.used + 1))
        return paid.copy(powers = updated)
    }
    val abilityIndex = mysticAbilities.indexOfFirst { it.id == command.targetId }
    require(abilityIndex >= 0) { "Habilidade não encontrada." }
    val ability = mysticAbilities[abilityIndex]
    require(ability.available && ability.usage.available) { "Esta habilidade não está disponível agora." }
    val paid = mutateResource(ability.costResource, -ability.costAmount)
    val updated = paid.mysticAbilities.toMutableList()
    updated[abilityIndex] = ability.copy(usage = ability.usage.copy(used = ability.usage.used + 1))
    return paid.copy(mysticAbilities = updated)
}

private fun Character.resetUsage(periodKey: String): Character = copy(
    powers = powers.map { power ->
        if (periodKey.isBlank() || power.usage.period.name == periodKey) power.copy(usage = power.usage.copy(used = 0, periodKey = periodKey)) else power
    },
    mysticAbilities = mysticAbilities.map { ability ->
        if (periodKey.isBlank() || ability.usage.period.name == periodKey) ability.copy(usage = ability.usage.copy(used = 0, periodKey = periodKey)) else ability
    },
)

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
    else -> resourceValue(resource).maximum
}

private fun ResourceValue.withSessionCurrent(value: Int, maximum: Int) = copy(current = value.coerceIn(0, maximum.coerceAtLeast(0)))
private fun BodyRegion.idOrName(): String = name

