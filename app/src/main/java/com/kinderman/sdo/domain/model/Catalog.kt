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
    }
}
