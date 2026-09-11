package com.kinderman.sdo.domain.model

enum class CatalogKind {
    PATH,
    POWER,
    MAGIC,
    ASH,
    RUNE,
    ITEM,
    ACQUIRED_KNOWLEDGE,
    ARCANE_KNOWLEDGE,
    BATTLE_TECHNIQUE,
}

data class CatalogEntry(
    val id: String,
    val kind: CatalogKind,
    val name: String,
    val group: String,
    val summary: String,
    val cost: String = "",
    val action: String = "",
    val range: String = "",
    val duration: String = "",
    val source: String = "Catálogo local",
    val version: Int = 1,
    val creationCost: String = "",
    val price: Int = 0,
    val load: Int = 0,
    val backpackCapacity: Int = 0,
    val durability: String = "",
    val region: String = "",
    val relatedAttribute: String = "",
    val initialValue: Int? = null,
    val prerequisites: List<String> = emptyList(),
    val mechanicalEffect: String = "",
    val ruleReference: String = "",
    val keywords: List<String> = emptyList(),
    val repeatable: Boolean = false,
    val limit: String = "",
    val activationCondition: String = "",
    val enhancements: String = "",
    val deactivationCondition: String = "",
    val abilitySource: AbilitySource? = null,
    val sourceKnowledge: String = "",
    val sourceLevel: Int? = null,
    val abilityCostType: AbilityCostType? = null,
    val abilityCostValue: Int? = null,
    val abilityExecution: AbilityExecution? = null,
    val executionValue: Int = 0,
    val executionUnit: AbilityTimeUnit = AbilityTimeUnit.MINUTES,
    val abilityRange: AbilityRange? = null,
    val targetArea: String = "",
    val abilityDuration: AbilityDuration? = null,
    val durationValue: Int = 0,
    val durationUnit: AbilityTimeUnit = AbilityTimeUnit.HOURS,
    val abilityResistance: AbilityResistance? = null,
    val catalogAshSource: AshSource? = null,
    val catalogAshPurity: AshPurity? = null,
    val runePackage: String = "",
) {
    val category: String get() = group
    val description: String get() = summary

    fun searchableText(): String = buildString {
        append(name)
        append(' ')
        append(group)
        append(' ')
        append(summary)
        append(' ')
        append(source)
        append(' ')
        append(relatedAttribute)
        append(' ')
        append(mechanicalEffect)
        append(' ')
        append(ruleReference)
        append(' ')
        append(prerequisites.joinToString(" "))
        append(' ')
        append(keywords.joinToString(" "))
        append(' ')
        append(listOf(cost, action, range, duration, limit, activationCondition, enhancements, deactivationCondition).joinToString(" "))
        append(' ')
        append(listOfNotNull(
            abilitySource?.label, sourceKnowledge, sourceLevel?.toString(), abilityCostType?.label,
            abilityCostValue?.toString(), abilityExecution?.label, abilityRange?.label, targetArea,
            abilityDuration?.label, abilityResistance?.label, catalogAshSource?.label,
            catalogAshPurity?.label, runePackage,
        ).joinToString(" "))
    }
}

fun CatalogEntry.userFacingSource(): String = source
    .replace(Regex("^\\s*\\d+\\s+Exemplos?\\s+de\\s+", RegexOption.IGNORE_CASE), "")
    .replace(Regex("^\\s*Exemplos?\\s+de\\s+", RegexOption.IGNORE_CASE), "")
