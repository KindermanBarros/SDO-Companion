package com.kinderman.sdo.domain.model

enum class CatalogKind { PATH, POWER, MAGIC, ASH, RUNE }

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
)

