package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.Power

fun Character.withMigratedCreationRules(): Character {
    if (creationRulesVersion >= 2) return this
    val migratedAttributes = if (raceAttribute.isBlank()) attributes else attributes.map { attribute ->
        if (attribute.acronym.equals(raceAttribute, true) && attribute.value > 0) {
            attribute.copy(value = attribute.value - 1)
        } else attribute
    }
    return copy(attributes = migratedAttributes, creationRulesVersion = 2)
}

fun Character.withRaceSelection(
    raceDefinition: RaceDefinition,
    subRaceDefinition: SubRaceDefinition?,
    attribute: String,
    basePowers: List<RacialPower>,
    subRacePower: RacialPower?,
): Character {
    val oldRace = RaceCatalog.race(race)
    val hadManagedRace = raceAttribute.isNotBlank()
    val selectedAttribute = raceDefinition.attribute.takeUnless { it == "Qualquer" } ?: attribute
    val requiredBasePowers = if (subRaceDefinition == null) 2 else 1
    val selectedBasePowers = basePowers
        .filter { it in raceDefinition.powers }
        .distinct()
        .take(requiredBasePowers)
        .let { selected -> selected + raceDefinition.powers.filterNot { it in selected }.take(requiredBasePowers - selected.size) }
    val selectedSubRacePower = subRaceDefinition?.let { subRace ->
        subRacePower?.takeIf { it in subRace.powers } ?: subRace.powers.first()
    }
    val retainedPowers = powers.filterNot {
        it.origin.startsWith("Raça — ") || it.origin.startsWith("Sub-raça — ")
    }
    val racialPowers = selectedBasePowers.map {
        it.toStructuredPower("Raça — ${raceDefinition.name}")
    } + listOfNotNull(selectedSubRacePower?.let {
        it.toStructuredPower("Sub-raça — ${subRaceDefinition?.name}")
    })
    fun previous(value: Int) = if (hadManagedRace) value else 0

    return copy(
        race = raceDefinition.name,
        subRace = subRaceDefinition?.name.orEmpty(),
        raceAttribute = selectedAttribute,
        creationRulesVersion = maxOf(creationRulesVersion, 2),
        life = life.copy(adjustment = life.adjustment - previous(oldRace?.hp ?: 0) + raceDefinition.hp),
        sanity = sanity.copy(adjustment = sanity.adjustment - previous(oldRace?.sanity ?: 0) + raceDefinition.sanity),
        arcane = arcane.copy(adjustment = arcane.adjustment - previous(oldRace?.arcane ?: 0) + raceDefinition.arcane),
        energy = energy.copy(adjustment = energy.adjustment - previous(oldRace?.energy ?: 0) + raceDefinition.energy),
        powers = retainedPowers + racialPowers,
    )
}

internal fun RacialPower.toStructuredPower(origin: String): Power = Power(
    name = name,
    origin = origin,
    effect = effect,
    cost = cost,
    action = action,
    range = range,
    duration = duration,
    limit = limit,
    category = "Poder racial",
    sourceType = com.kinderman.sdo.domain.model.PowerSourceType.RACE,
    sourceId = origin,
    ruleReference = RaceCatalog.RULE_REFERENCE,
    activationCondition = activationCondition,
    deactivationCondition = deactivationCondition,
    enhancements = "Sem aprimoramento racial publicado",
    catalogVersion = BuiltInCatalog.VERSION,
    canonicalSource = com.kinderman.sdo.domain.model.AbilitySource.RACE,
    costType = costType,
    costValue = costValue,
    destinyCostEligible = destinyCostEligible,
    executionType = executionType,
    rangeType = rangeType,
    durationType = durationType,
)
