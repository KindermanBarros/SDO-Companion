package com.kinderman.sdo.domain.model

data class ItemPart(
    val id: String,
    val name: String,
    val group: String,
    val creationCost: Int?,
    val price: Int,
    val load: Int = 0,
    val durability: Int = 0,
    val region: String = "",
    val effect: String = "",
    val pg: Int = 0,
    val pl: Int = 0,
)

data class ItemMaterialPart(
    val name: String,
    val material: ItemPart,
)

data class BuiltItem(
    val name: String,
    val category: String,
    val creationCost: Int?,
    val price: Int,
    val load: Int,
    val durability: Int,
    val region: String,
    val effect: String,
    val pg: Int = 0,
    val pl: Int = 0,
) {
    fun toInventoryItem(initialCreation: Boolean = false) = InventoryItem(
        name = name,
        load = load,
        durability = durability.takeIf { it > 0 }?.let { "$it/$it" }.orEmpty(),
        region = region,
        effect = listOfNotNull(
            "Categoria: $category",
            "Preço de referência: ${price} E$".takeIf { price > 0 },
            effect.takeIf(String::isNotBlank),
            creationCost?.takeIf { initialCreation }?.let { initialCreationMarker(it) },
        ).joinToString("\n"),
        pg = pg,
        pl = pl,
    )
}

fun CatalogEntry.toInventoryItem(initialCreation: Boolean = false) = InventoryItem(
    name = name,
    load = load,
    durability = durability,
    region = region,
    effect = listOfNotNull(
        "Categoria: $group".takeIf { group.isNotBlank() },
        "Preço de referência: ${price} E$".takeIf { price > 0 },
        summary.takeIf(String::isNotBlank),
        creationCost.toIntOrNull()?.takeIf { initialCreation }?.let(::initialCreationMarker),
    ).joinToString("\n"),
    pg = protectionValue("PG"),
    pl = protectionValue("PL"),
)

private fun CatalogEntry.protectionValue(label: String): Int =
    Regex("(?:^|[;\\n]\\s*)$label\\s+(\\d+)").find(summary)?.groupValues?.get(1)?.toIntOrNull() ?: 0

fun InventoryItem.initialCreationCost(): Int =
    Regex("\\[Criação inicial: (\\d+) PH]").find(effect)?.groupValues?.get(1)?.toIntOrNull() ?: 0

private fun initialCreationMarker(cost: Int) = "[Criação inicial: $cost PH]"
