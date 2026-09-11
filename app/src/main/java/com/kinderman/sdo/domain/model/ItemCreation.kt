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
    val agilityLimit: Int? = null,
)

data class ItemCreationDraft(
    val step: Int = 1,
    val category: String = "Arma",
    val baseId: String = "",
    val materialId: String = "",
    val quality: ItemQuality = ItemQuality.COMMON,
    val modificationIds: List<String> = emptyList(),
    val gemIds: List<String> = emptyList(),
    val gemSlots: Int = 0,
    val technologySlots: Int = 0,
    val customName: String = "",
)

enum class ItemEffectType {
    ATTRIBUTE,
    KNOWLEDGE,
    MAGIC_DAMAGE,
    DURABILITY,
    GEM_POWER,
    RULE,
}

enum class ItemEffectCondition { EQUIPPED, WIELDED }

data class ItemEffect(
    val id: String,
    val type: ItemEffectType,
    val value: Int = 0,
    val target: String = "",
    val condition: ItemEffectCondition = ItemEffectCondition.WIELDED,
    val description: String = "",
)

enum class ItemBonusType(val label: String, val heritageCost: Int) {
    ATTRIBUTE("Atributo", 3),
    BASIC_KNOWLEDGE("Conhecimento básico", 2),
    ACQUIRED_KNOWLEDGE("Conhecimento adquirido", 2),
}

data class ItemBonus(
    val type: ItemBonusType = ItemBonusType.ATTRIBUTE,
    val target: String = "",
    val value: Int = 0,
) {
    val creationCost: Int get() = value.coerceAtLeast(0) * type.heritageCost
    val isComplete: Boolean get() = target.isNotBlank()

    fun displayTarget(): String = when (type) {
        ItemBonusType.BASIC_KNOWLEDGE -> target.substringAfter(':', target)
        else -> target
    }

    companion object {
        fun basicKnowledgeTarget(attributeAcronym: String, skillName: String): String =
            "${attributeAcronym.uppercase()}:$skillName"
    }
}

enum class ItemQuality(
    val label: String,
    val creationAdjustment: Int,
    val priceMultiplier: Double,
) {
    MUNDANE("Mundana", -1, 0.25),
    COMMON("Comum", 0, 1.0),
    IMPROVED("Aprimorada", 3, 1.5),
    ICONIC("Icônica", 5, 2.0),
    MASTERPIECE("Obra-Prima", 8, 5.0),
    ARTIFACT("Artefato", 15, 100.0),
    ANCIENT("Anciã", 25, 10_000.0),
}

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
    val agilityLimit: Int? = null,
    val quality: ItemQuality = ItemQuality.COMMON,
    val bonuses: List<ItemBonus> = emptyList(),
    val baseId: String = "",
    val materialId: String = "",
    val modificationIds: List<String> = emptyList(),
    val gemIds: List<String> = emptyList(),
    val mechanicalEffects: List<ItemEffect> = emptyList(),
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
        ).joinToString("\n"),
        pg = pg,
        pl = pl,
        category = category,
        agilityLimit = agilityLimit,
        quality = quality.label,
        bonuses = bonuses,
        baseId = baseId,
        materialId = materialId,
        modificationIds = modificationIds,
        gemIds = gemIds,
        mechanicalEffects = mechanicalEffects,
        acquisitionSource = if (initialCreation) ItemAcquisitionSource.HERITAGE else ItemAcquisitionSource.PURCHASE,
        heritageCost = creationCost.takeIf { initialCreation },
        purchasePrice = price.takeIf { !initialCreation },
    )
}

fun InventoryItem.initialCreationCost(): Int = heritageCost.takeIf { acquisitionSource == ItemAcquisitionSource.HERITAGE } ?: 0

fun InventoryItem.participatesInInitialCreation(): Boolean = acquisitionSource == ItemAcquisitionSource.HERITAGE
