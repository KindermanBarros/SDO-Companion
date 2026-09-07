package com.kinderman.sdo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind

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
    )
}

fun CatalogEntry.toRecord() = CatalogEntryRecord(
    id, kind.name, name, group, summary, cost, action, range, duration, source, version,
    creationCost, price, load, durability, region,
)
