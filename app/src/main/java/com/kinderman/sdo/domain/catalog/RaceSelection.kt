package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.Power

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
    val updatedAttributes = attributes.map { value ->
        val withoutOld = if (value.acronym == raceAttribute) value.value - 1 else value.value
        value.copy(value = (withoutOld + if (value.acronym == selectedAttribute) 1 else 0).coerceAtLeast(0))
    }
    val retainedPowers = powers.filterNot {
        it.origin.startsWith("Raça — ") || it.origin.startsWith("Sub-raça — ")
    }
    val racialPowers = selectedBasePowers.map {
        Power(name = it.name, origin = "Raça — ${raceDefinition.name}", effect = it.effect)
    } + listOfNotNull(selectedSubRacePower?.let {
        Power(name = it.name, origin = "Sub-raça — ${subRaceDefinition?.name}", effect = it.effect)
    })
    fun previous(value: Int) = if (hadManagedRace) value else 0

    return copy(
        race = raceDefinition.name,
        subRace = subRaceDefinition?.name.orEmpty(),
        raceAttribute = selectedAttribute,
        attributes = updatedAttributes,
        life = life.copy(adjustment = life.adjustment - previous(oldRace?.hp ?: 0) + raceDefinition.hp),
        sanity = sanity.copy(adjustment = sanity.adjustment - previous(oldRace?.sanity ?: 0) + raceDefinition.sanity),
        arcane = arcane.copy(adjustment = arcane.adjustment - previous(oldRace?.arcane ?: 0) + raceDefinition.arcane),
        energy = energy.copy(adjustment = energy.adjustment - previous(oldRace?.energy ?: 0) + raceDefinition.energy),
        powers = retainedPowers + racialPowers,
    )
}
