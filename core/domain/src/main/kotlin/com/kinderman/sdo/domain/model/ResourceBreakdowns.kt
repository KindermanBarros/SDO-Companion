package com.kinderman.sdo.domain.model

fun Character.lifeMaximumBreakdown(): CalculatedValue {
    val vitality = basicKnowledgeCalculation("VIG", "Vitalidade")
    return CalculatedValue(
        base = 10 + vitality.base,
        adjustment = life.adjustment + vitality.adjustment,
        modifiers = vitality.modifiers + powerValueModifiers(AbilityModifierTarget.RESOURCE_MAXIMUM, "LIFE"),
    )
}

fun Character.sanityMaximumBreakdown(): CalculatedValue {
    val sanitySkill = basicKnowledgeCalculation("INT", "Sanidade")
    return CalculatedValue(
        base = 10 + sanitySkill.base,
        adjustment = sanity.adjustment + sanitySkill.adjustment,
        modifiers = sanitySkill.modifiers + powerValueModifiers(AbilityModifierTarget.RESOURCE_MAXIMUM, "SANITY"),
    )
}

fun Character.arcaneMaximumBreakdown(): CalculatedValue {
    val power = attributeCalculation("POD")
    val arcaneSkill = basicKnowledgeCalculation("POD", "Arcano")
    return CalculatedValue(
        base = power.base + arcaneSkill.base,
        adjustment = arcane.adjustment + power.adjustment + arcaneSkill.adjustment,
        modifiers = power.modifiers + arcaneSkill.modifiers + powerValueModifiers(AbilityModifierTarget.RESOURCE_MAXIMUM, "ARCANE"),
    )
}

fun Character.energyMaximumBreakdown(): CalculatedValue {
    val vigor = attributeCalculation("VIG")
    val energySkill = basicKnowledgeCalculation("VIG", "Energia")
    return CalculatedValue(
        base = vigor.base + energySkill.base,
        adjustment = energy.adjustment + vigor.adjustment + energySkill.adjustment,
        modifiers = vigor.modifiers + energySkill.modifiers + powerValueModifiers(AbilityModifierTarget.RESOURCE_MAXIMUM, "ENERGY"),
    )
}

fun Character.loadCapacityBreakdown(): CalculatedValue {
    val strength = attributeCalculation("FOR")
    return CalculatedValue(
        base = 2 + strength.base,
        adjustment = backpackCapacity + strength.adjustment,
        modifiers = strength.modifiers,
    )
}
