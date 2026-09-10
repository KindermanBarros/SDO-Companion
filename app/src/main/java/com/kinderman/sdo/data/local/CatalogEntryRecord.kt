package com.kinderman.sdo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kinderman.sdo.domain.model.AbilityCostType
import com.kinderman.sdo.domain.model.AbilityDuration
import com.kinderman.sdo.domain.model.AbilityExecution
import com.kinderman.sdo.domain.model.AbilityRange
import com.kinderman.sdo.domain.model.AbilityResistance
import com.kinderman.sdo.domain.model.AbilitySource
import com.kinderman.sdo.domain.model.AbilityTimeUnit
import com.kinderman.sdo.domain.model.AshPurity
import com.kinderman.sdo.domain.model.AshSource
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind

private const val LIST_SEPARATOR = "\u001f"

@Entity(tableName = "catalog_entries")
data class CatalogEntryRecord(
    @PrimaryKey val id: String,
    val kind: String,
    val name: String,
    val groupName: String,
    val summary: String,
    val cost: String,
    val action: String,
    val range: String,
    val duration: String,
    val source: String,
    val catalogVersion: Int,
    val creationCost: String,
    val price: Int,
    val load: Int,
    val durability: String,
    val region: String,
    val relatedAttribute: String,
    val initialValue: Int?,
    val prerequisites: String,
    val mechanicalEffect: String,
    val ruleReference: String,
    val keywords: String,
    val repeatable: Boolean,
    val limit: String,
    val activationCondition: String,
    val enhancements: String,
    val deactivationCondition: String,
    val abilitySource: String?,
    val sourceKnowledge: String,
    val sourceLevel: Int?,
    val abilityCostType: String?,
    val abilityCostValue: Int?,
    val abilityExecution: String?,
    val executionValue: Int,
    val executionUnit: String,
    val abilityRange: String?,
    val targetArea: String,
    val abilityDuration: String?,
    val durationValue: Int,
    val durationUnit: String,
    val abilityResistance: String?,
    val catalogAshSource: String?,
    val catalogAshPurity: String?,
    val runePackage: String,
) {
    fun toDomain() = CatalogEntry(
        id = id,
        kind = CatalogKind.valueOf(kind),
        name = name,
        group = groupName,
        summary = summary,
        cost = cost,
        action = action,
        range = range,
        duration = duration,
        source = source,
        version = catalogVersion,
        creationCost = creationCost,
        price = price,
        load = load,
        durability = durability,
        region = region,
        relatedAttribute = relatedAttribute,
        initialValue = initialValue,
        prerequisites = prerequisites.decodeList(),
        mechanicalEffect = mechanicalEffect,
        ruleReference = ruleReference,
        keywords = keywords.decodeList(),
        repeatable = repeatable,
        limit = limit,
        activationCondition = activationCondition,
        enhancements = enhancements,
        deactivationCondition = deactivationCondition,
        abilitySource = abilitySource.enumOrNull<AbilitySource>(),
        sourceKnowledge = sourceKnowledge,
        sourceLevel = sourceLevel,
        abilityCostType = abilityCostType.enumOrNull<AbilityCostType>(),
        abilityCostValue = abilityCostValue,
        abilityExecution = abilityExecution.enumOrNull<AbilityExecution>(),
        executionValue = executionValue,
        executionUnit = executionUnit.enumOrNull<AbilityTimeUnit>() ?: AbilityTimeUnit.MINUTES,
        abilityRange = abilityRange.enumOrNull<AbilityRange>(),
        targetArea = targetArea,
        abilityDuration = abilityDuration.enumOrNull<AbilityDuration>(),
        durationValue = durationValue,
        durationUnit = durationUnit.enumOrNull<AbilityTimeUnit>() ?: AbilityTimeUnit.HOURS,
        abilityResistance = abilityResistance.enumOrNull<AbilityResistance>(),
        catalogAshSource = catalogAshSource.enumOrNull<AshSource>(),
        catalogAshPurity = catalogAshPurity.enumOrNull<AshPurity>(),
        runePackage = runePackage,
    )
}

fun CatalogEntry.toRecord() = CatalogEntryRecord(
    id = id,
    kind = kind.name,
    name = name,
    groupName = group,
    summary = summary,
    cost = cost,
    action = action,
    range = range,
    duration = duration,
    source = source,
    catalogVersion = version,
    creationCost = creationCost,
    price = price,
    load = load,
    durability = durability,
    region = region,
    relatedAttribute = relatedAttribute,
    initialValue = initialValue,
    prerequisites = prerequisites.encodeList(),
    mechanicalEffect = mechanicalEffect,
    ruleReference = ruleReference,
    keywords = keywords.encodeList(),
    repeatable = repeatable,
    limit = limit,
    activationCondition = activationCondition,
    enhancements = enhancements,
    deactivationCondition = deactivationCondition,
    abilitySource = abilitySource?.name,
    sourceKnowledge = sourceKnowledge,
    sourceLevel = sourceLevel,
    abilityCostType = abilityCostType?.name,
    abilityCostValue = abilityCostValue,
    abilityExecution = abilityExecution?.name,
    executionValue = executionValue,
    executionUnit = executionUnit.name,
    abilityRange = abilityRange?.name,
    targetArea = targetArea,
    abilityDuration = abilityDuration?.name,
    durationValue = durationValue,
    durationUnit = durationUnit.name,
    abilityResistance = abilityResistance?.name,
    catalogAshSource = catalogAshSource?.name,
    catalogAshPurity = catalogAshPurity?.name,
    runePackage = runePackage,
)

private fun List<String>.encodeList(): String = joinToString(LIST_SEPARATOR)
private fun String.decodeList(): List<String> = if (isBlank()) emptyList() else split(LIST_SEPARATOR)
private inline fun <reified T : Enum<T>> String?.enumOrNull(): T? =
    this?.let { value -> enumValues<T>().firstOrNull { it.name == value } }
