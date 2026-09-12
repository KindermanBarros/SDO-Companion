package com.kinderman.sdo.domain.model

import com.google.firebase.firestore.PropertyName

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
    val id: String = "",
    val type: ItemEffectType = ItemEffectType.RULE,
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

private val TWO_HANDED_WEAPON_BASES = setOf(
    "espada_bastarda", "guarda_dupla", "montante", "clava", "glaive", "arco_composto",
    "besta_pesada", "fuzil_vapor", "estilhacadora", "fuzil_tesla",
)

/** Canonical hand requirement extracted from the Pesado characteristic. */
fun InventoryItem.handsRequired(): Int = if (baseId in TWO_HANDED_WEAPON_BASES && materialId != "ossos_comuns") 2 else 1

val Character.wieldedHandsUsed: Int get() = inventory.filter { it.inventoryState == InventoryState.WIELDED }
    .sumOf(InventoryItem::handsRequired)

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
    if (state == InventoryState.WIELDED && inventory.filter {
            it.id != itemId && it.inventoryState == InventoryState.WIELDED
        }.sumOf(InventoryItem::handsRequired) + item.handsRequired() > 2) return this
    return copy(inventory = inventory.map { if (it.id == itemId) it.withInventoryState(state) else it })
        .synchronizeItemPowers()
}

/** Repairs invalid persisted states without changing the item order or deleting valid items. */
fun Character.withValidInventoryStates(): Character {
    val usableContainerId = inventory
        .filter { !it.isBroken && it.catalogEntryId.isNotBlank() && it.inventoryState == InventoryState.EQUIPPED && it.category.equals("Recipiente de Carga", true) }
        .maxByOrNull(InventoryItem::backpackCapacity)?.id
    var quickSlots = 0
    var hands = 0
    val hasBackpack = usableContainerId != null
    val sanitized = inventory.map { item ->
        val state = item.inventoryState
        val valid = when {
            item.isBroken && state in setOf(InventoryState.EQUIPPED, InventoryState.WIELDED) -> false
            state == InventoryState.EQUIPPED && item.category.equals("Recipiente de Carga", true) -> item.catalogEntryId.isNotBlank() && item.id == usableContainerId
            state == InventoryState.BACKPACK -> hasBackpack
            state == InventoryState.QUICK_ACCESS -> item.effectiveLoad() <= 1 && quickSlots++ < 2
            state == InventoryState.WIELDED -> (hands + item.handsRequired() <= 2).also { if (it) hands += item.handsRequired() }
            else -> true
        }
        if (valid) item.withInventoryState(state) else item.withInventoryState(InventoryState.STORED)
    }
    val activeEquipmentIds = sanitized.asSequence()
        .filterNot { it.isBroken }
        .filter { it.inventoryState in setOf(InventoryState.EQUIPPED, InventoryState.WIELDED) }
        .mapTo(hashSetOf(), InventoryItem::id)
    val sanitizedRegions = bodyRegions.map { region ->
        region.copy(equippedItemIds = region.equippedItemIds.filter(activeEquipmentIds::contains))
    }
    return if (sanitized == inventory && sanitizedRegions == bodyRegions) this
    else copy(inventory = sanitized, bodyRegions = sanitizedRegions).synchronizeItemPowers()
}

/** Applies durability loss following the equipment rules: zero creates Scrap and
 * any further loss dealt to Scrap makes the item Broken. */
fun Character.damageInventoryItem(itemId: String, amount: Int = 1): Character {
    if (amount <= 0) return this
    val current = inventory.firstOrNull { it.id == itemId } ?: return this
    val damaged = when {
        current.isBroken -> current
        current.isScrap -> current.copy(
            itemCondition = ItemCondition.BROKEN,
            durabilityCurrent = 0,
            state = InventoryState.STORED.storageCode,
        )
        else -> {
            val durability = (current.durabilityCurrent - amount).coerceAtLeast(0)
            current.copy(
                durabilityCurrent = durability,
                itemCondition = if (durability == 0) ItemCondition.SCRAP else ItemCondition.NORMAL,
            )
        }
    }
    val brokenItemIds = setOfNotNull(itemId.takeIf { damaged.isBroken })
    return copy(
        inventory = inventory.map { item -> if (item.id == itemId) damaged else item },
        bodyRegions = bodyRegions.map { region ->
            region.copy(equippedItemIds = region.equippedItemIds.filterNot(brokenItemIds::contains))
        },
    )
        .synchronizeItemPowers()
}

val InventoryItem.salvageValueEstribos: Int
    get() = ((purchasePrice ?: 0) / 2).coerceAtLeast(1)

/** Recycles a destroyed item into the amount of Sucata defined by its creation cost. */
fun Character.recycleBrokenItem(itemId: String): Character {
    val broken = inventory.firstOrNull { it.id == itemId && it.isBroken } ?: return this
    val recoveredValue = broken.salvageValueEstribos
    val withoutBroken = removeInventoryItem(itemId)
    return withoutBroken.addInventoryItem(
        InventoryItem(
            name = "Sucata recuperada de ${broken.name.ifBlank { "item quebrado" }}",
            category = "Material",
            quantity = 1,
            load = 1,
            purchasePrice = recoveredValue,
            acquisitionSource = ItemAcquisitionSource.NARRATIVE,
        ),
    )
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
    @PropertyName("Mundana") MUNDANE("Mundana", -1, 0.25),
    @PropertyName("Comum") COMMON("Comum", 0, 1.0),
    @PropertyName("Aprimorada") IMPROVED("Aprimorada", 3, 1.5),
    @PropertyName("Icônica") ICONIC("Icônica", 5, 2.0),
    @PropertyName("Obra-Prima") MASTERPIECE("Obra-Prima", 8, 5.0),
    @PropertyName("Artefato") ARTIFACT("Artefato", 15, 100.0),
    @PropertyName("Anciã") ANCIENT("Anciã", 25, 10_000.0),
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
        purchasePrice = price,
    )
}

const val CURRENT_ITEM_DATA_VERSION = 5

fun InventoryItem.initialCreationCost(): Int = heritageCost.takeIf { acquisitionSource == ItemAcquisitionSource.HERITAGE } ?: 0

fun InventoryItem.participatesInInitialCreation(): Boolean = acquisitionSource == ItemAcquisitionSource.HERITAGE
