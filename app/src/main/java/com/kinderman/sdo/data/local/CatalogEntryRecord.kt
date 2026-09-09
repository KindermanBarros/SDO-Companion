package com.kinderman.sdo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
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
)

private fun List<String>.encodeList(): String = joinToString(LIST_SEPARATOR)
private fun String.decodeList(): List<String> = if (isBlank()) emptyList() else split(LIST_SEPARATOR)
