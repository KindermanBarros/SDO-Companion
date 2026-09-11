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
    val backpackCapacity: Int = 0,
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
    val manualPrice: String = "",
    val commonName: String = "",
    val commonEffect: String = "",
    val commonLoad: Int = 0,
    val commonQuantity: Int = 1,
    val commonCategory: String = "Item",
    val commonRegion: String = "",
    val commonDurability: Int = 0,
    val commonPg: Int = 0,
    val commonPl: Int = 0,
    val commonAgilityLimit: Int? = null,
    val commonAttack: Int = 0,
    val commonDamage: Int = 0,
    val commonRange: Int = 0,
)

enum class ItemEffectType {
    ATTRIBUTE,
    KNOWLEDGE,
    ATTACK,
    PHYSICAL_DAMAGE,
    MAGIC_DAMAGE,
    PG,
    PL,
    AGILITY_LIMIT,
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
    val resolvedTargetId: String = "",
)

data class ActiveItemEffect(
    val itemId: String,
    val itemName: String,
    val effect: ItemEffect,
)

fun Character.activeItemEffects(): List<ActiveItemEffect> = inventory.flatMap { item ->
    if (item.isBroken) return@flatMap emptyList()
    item.mechanicalEffects.filter { effect ->
        if (item.isScrap && (effect.id in item.modificationIds || effect.type in setOf(ItemEffectType.ATTACK, ItemEffectType.GEM_POWER))) return@filter false
        when (effect.condition) {
            ItemEffectCondition.EQUIPPED -> item.inventoryState in setOf(InventoryState.EQUIPPED, InventoryState.WIELDED)
            ItemEffectCondition.WIELDED -> item.inventoryState == InventoryState.WIELDED
        }
    }.map { effect ->
        val effective = if (item.isScrap && effect.type in setOf(ItemEffectType.PG, ItemEffectType.PL)) {
            effect.copy(value = effect.value / 2)
        } else effect
        ActiveItemEffect(item.id, item.name, effective)
    }
}

val Character.hasScrapAttackDisadvantage: Boolean get() = inventory.any {
    it.inventoryState == InventoryState.WIELDED && it.isScrap && it.category.contains("arma", true)
}

/** Number of damage-die categories lost by the active weapon because it is Scrap. */
val Character.scrapDamageDieCategoryPenalty: Int get() = if (hasScrapAttackDisadvantage) 1 else 0

data class EquipmentEffectAudit(
    val itemId: String,
    val itemName: String,
    val effectId: String,
    val type: ItemEffectType,
    val targetId: String,
    val value: Int,
    val description: String,
)

data class EquipmentEffectResolution(val entries: List<EquipmentEffectAudit>) {
    fun total(type: ItemEffectType, targetId: String? = null): Int = entries
        .filter { it.type == type && (targetId == null || it.targetId.equals(targetId, true)) }
        .sumOf(EquipmentEffectAudit::value)

    val attackBonus: Int get() = total(ItemEffectType.ATTACK)
    val physicalDamageBonus: Int get() = total(ItemEffectType.PHYSICAL_DAMAGE)
    val magicDamageBonus: Int get() = total(ItemEffectType.MAGIC_DAMAGE)
    val agilityLimit: Int? get() = entries.filter { it.type == ItemEffectType.AGILITY_LIMIT }.map { it.value }.minOrNull()
    fun durabilityBonus(itemId: String): Int = entries.filter { it.type == ItemEffectType.DURABILITY && it.itemId == itemId }.sumOf { it.value }
    val activeRules: List<EquipmentEffectAudit> get() = entries.filter { it.type == ItemEffectType.RULE }
}

object EquipmentEffectEngine {
    fun resolve(character: Character): EquipmentEffectResolution = EquipmentEffectResolution(
        character.activeItemEffects().map { active ->
            EquipmentEffectAudit(
                itemId = active.itemId,
                itemName = active.itemName,
                effectId = active.effect.id,
                type = active.effect.type,
                targetId = active.effect.resolvedTargetId.ifBlank { active.effect.target },
                value = active.effect.value,
                description = active.effect.description,
            )
        },
    )
}

fun Character.addInventoryItem(item: InventoryItem): Character {
    val targets = (attributes.flatMap { attribute -> attribute.skills.map { "${attribute.acronym}:${it.name}" } } +
        learnedKnowledges.map { it.id } + arcaneKnowledges.map { it.id } + battleTechniques.map { it.id })
        .filter(String::isNotBlank).sorted()
    val resolved = item.copy(mechanicalEffects = item.mechanicalEffects.map { effect ->
        if (effect.target != "*" || effect.resolvedTargetId.isNotBlank() || targets.isEmpty()) effect
        else effect.copy(resolvedTargetId = targets[Math.floorMod("${item.id}:${effect.id}".hashCode(), targets.size)])
    })
    return copy(inventory = inventory + resolved).synchronizeItemPowers()
}

fun Character.withItemInventoryState(itemId: String, state: InventoryState): Character {
    val item = inventory.firstOrNull { it.id == itemId } ?: return this
    if (item.isBroken && state in setOf(InventoryState.EQUIPPED, InventoryState.WIELDED)) return this
    if (state == InventoryState.QUICK_ACCESS && (item.effectiveLoad() > 1 || inventory.count {
            it.id != itemId && it.inventoryState == InventoryState.QUICK_ACCESS
        } >= 2)) return this
    if (state == InventoryState.BACKPACK && backpackCapacity == 0) return this
    if (state == InventoryState.WIELDED && inventory.count {
            it.id != itemId && it.inventoryState == InventoryState.WIELDED
        } >= 2) return this
    return copy(inventory = inventory.map { if (it.id == itemId) it.withInventoryState(state) else it })
        .synchronizeItemPowers()
}

/** Applies durability loss following the equipment rules: zero creates Scrap and
 * any further loss dealt to Scrap makes the item Broken. */
fun Character.damageInventoryItem(itemId: String, amount: Int = 1): Character {
    if (amount <= 0) return this
    val current = inventory.firstOrNull { it.id == itemId } ?: return this
    val damaged = when {
        current.isBroken -> current
        current.isScrap -> current.copy(itemCondition = ItemCondition.BROKEN, durabilityCurrent = 0)
        else -> {
            val durability = (current.durabilityCurrent - amount).coerceAtLeast(0)
            current.copy(
                durabilityCurrent = durability,
                itemCondition = if (durability == 0) ItemCondition.SCRAP else ItemCondition.NORMAL,
            )
        }
    }
    return copy(inventory = inventory.map { item -> if (item.id == itemId) damaged else item })
        .synchronizeItemPowers()
}

fun Character.synchronizeItemPowers(): Character {
    val gemEffects = activeItemEffects().filter { it.effect.type == ItemEffectType.GEM_POWER }
    val activeKeys = gemEffects.mapTo(hashSetOf()) { "${it.itemId}:${it.effect.id}" }
    val retained = powers.filterNot { it.sourceType == PowerSourceType.ITEM && it.linkedItemId.isNotBlank() && "${it.linkedItemId}:${it.catalogEntryId}" !in activeKeys }
    val existingKeys = retained.mapTo(hashSetOf()) { "${it.linkedItemId}:${it.catalogEntryId}" }
    val granted = gemEffects.filter { "${it.itemId}:${it.effect.id}" !in existingKeys }.map { active ->
        Power(
            id = "item:${active.itemId}:${active.effect.id}",
            name = active.effect.description.substringBefore('.').ifBlank { "Poder de gema" },
            effect = active.effect.description,
            sourceType = PowerSourceType.ITEM,
            sourceId = active.itemId,
            catalogEntryId = active.effect.id,
            canonicalSource = AbilitySource.ITEM,
            linkedItemId = active.itemId,
        )
    }
    return copy(powers = retained + granted)
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
    val backpackCapacity: Int = 0,
    val durability: Int,
    val region: String,
    val effect: String,
    val pg: Int = 0,
    val pl: Int = 0,
    val agilityLimit: Int? = null,
    val quality: ItemQuality = ItemQuality.COMMON,
    val baseId: String = "",
    val materialId: String = "",
    val modificationIds: List<String> = emptyList(),
    val gemIds: List<String> = emptyList(),
    val mechanicalEffects: List<ItemEffect> = emptyList(),
) {
    fun toInventoryItem(initialCreation: Boolean = false) = InventoryItem(
        name = name,
        load = load,
        backpackCapacity = backpackCapacity,
        durabilityCurrent = durability,
        durabilityMax = durability,
        itemCondition = if (durability == 0) ItemCondition.SCRAP else ItemCondition.NORMAL,
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
        quality = quality,
        baseId = baseId,
        materialId = materialId,
        modificationIds = modificationIds,
        gemIds = gemIds,
        mechanicalEffects = mechanicalEffects,
        dataVersion = CURRENT_ITEM_DATA_VERSION,
        acquisitionSource = if (initialCreation) ItemAcquisitionSource.HERITAGE else ItemAcquisitionSource.PURCHASE,
        heritageCost = creationCost.takeIf { initialCreation },
        purchasePrice = price.takeIf { !initialCreation },
    )
}

const val CURRENT_ITEM_DATA_VERSION = 5

fun InventoryItem.initialCreationCost(): Int = heritageCost.takeIf { acquisitionSource == ItemAcquisitionSource.HERITAGE } ?: 0

fun InventoryItem.participatesInInitialCreation(): Boolean = acquisitionSource == ItemAcquisitionSource.HERITAGE
