package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.SpecialKnowledge

object KnowledgeCatalog {
    const val VERSION = 5
    val entries: List<CatalogEntry> = CanonicalCatalogData.entries.filter {
        it.kind in setOf(CatalogKind.ACQUIRED_KNOWLEDGE, CatalogKind.ARCANE_KNOWLEDGE, CatalogKind.BATTLE_TECHNIQUE)
    }
}

fun CatalogEntry.toSpecialKnowledge(): SpecialKnowledge = SpecialKnowledge(
    name = name,
    attribute = relatedAttribute,
    value = initialValue ?: 0,
    catalogEntryId = id,
    catalogVersion = version,
    category = group,
    description = summary,
    prerequisites = prerequisites,
    mechanicalEffect = mechanicalEffect,
    source = source,
    ruleReference = ruleReference,
    keywords = keywords,
    repeatable = repeatable,
)

fun List<SpecialKnowledge>.canAddCatalogEntry(entry: CatalogEntry): Boolean =
    entry.repeatable || none { it.catalogEntryId == entry.id }
