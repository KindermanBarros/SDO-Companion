package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.inventoryState
import com.kinderman.sdo.domain.model.withValidInventoryStates

fun Character.withNormalizedInventory(): Character {
    val normalized = inventory.mapNotNull(InventoryItem::normalized)
    return (if (normalized == inventory) this else copy(inventory = normalized)).withValidInventoryStates()
}

internal fun InventoryItem.normalized(): InventoryItem? {
    val durabilityWasNotDefined = durabilityMax <= 0
    val durabilityNormalized = if (durabilityWasNotDefined) {
        copy(durabilityCurrent = 1, durabilityMax = 1)
    } else copy(durabilityCurrent = durabilityCurrent.coerceIn(0, durabilityMax))
    if (dataVersion >= CURRENT_ITEM_DATA_VERSION) return durabilityNormalized

    val normalizedState = durabilityNormalized.inventoryState.storageCode
    val retainedEffects = (
        ItemCreationRules.componentEffects(modificationIds, gemIds) + mechanicalEffects
    ).distinctBy(ItemEffect::id)
    val resolved = ItemCreationRules.inventoryTemplate(catalogEntryId, name)

    if (resolved == null) {
        return durabilityNormalized.copy(
            state = normalizedState,
            category = "LEGACY_NARRATIVE".takeIf { catalogEntryId.isBlank() && baseId.isBlank() } ?: category,
            mechanicalEffects = retainedEffects,
            dataVersion = CURRENT_ITEM_DATA_VERSION,
        )
    }

    val (entry, template) = resolved
    val canonical = template.toInventoryItem(acquisitionSource == com.kinderman.sdo.domain.model.ItemAcquisitionSource.HERITAGE)
    val finalDurabilityMax = if (durabilityWasNotDefined) canonical.durabilityMax else maxOf(durabilityNormalized.durabilityMax, canonical.durabilityMax)
    val finalDurabilityCurrent = if (durabilityWasNotDefined) canonical.durabilityCurrent else durabilityNormalized.durabilityCurrent.coerceIn(0, finalDurabilityMax)
    return canonical.copy(
        id = id,
        state = normalizedState,
        quantity = quantity,
        linkedAshId = linkedAshId,
        ashPurity = ashPurity,
        acquisitionSource = acquisitionSource,
        heritageCost = heritageCost ?: canonical.heritageCost,
        purchasePrice = purchasePrice ?: canonical.purchasePrice,
        catalogEntryId = entry.id,
        catalogVersion = entry.version,
        canonical = true,
        acquiredAt = acquiredAt,
        durabilityCurrent = finalDurabilityCurrent,
        durabilityMax = finalDurabilityMax,
        mechanicalEffects = (canonical.mechanicalEffects + retainedEffects).distinctBy(ItemEffect::id),
        dataVersion = CURRENT_ITEM_DATA_VERSION,
    )
}
