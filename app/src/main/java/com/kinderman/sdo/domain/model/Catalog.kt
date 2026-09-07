package com.kinderman.sdo.domain.model

enum class CatalogKind { PATH, POWER, MAGIC, ASH, RUNE, ITEM }

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
    val durability: String = "",
    val region: String = "",
)
