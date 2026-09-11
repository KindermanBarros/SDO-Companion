package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.inventoryState

fun Character.withNormalizedInventory(): Character {
    val normalized = inventory.mapNotNull(InventoryItem::normalized)
    return if (normalized == inventory) this else copy(inventory = normalized)
}

internal fun InventoryItem.normalized(): InventoryItem? {
    if (dataVersion >= CURRENT_ITEM_DATA_VERSION) return this

    val normalizedState = inventoryState.storageCode
    val retainedEffects = (
        ItemCreationRules.componentEffects(modificationIds, gemIds) + mechanicalEffects
    ).distinctBy(ItemEffect::id)
    val resolved = ItemCreationRules.inventoryTemplate(catalogEntryId, name)

    if (resolved == null) {
        if (catalogEntryId.isBlank() && baseId.isBlank() && durabilityMax == 0) return null
        return copy(
            state = normalizedState,
            category = "LEGACY_NARRATIVE".takeIf { catalogEntryId.isBlank() && baseId.isBlank() } ?: category,
            mechanicalEffects = retainedEffects,
            dataVersion = CURRENT_ITEM_DATA_VERSION,
        )
    }

    val (entry, template) = resolved
    val canonical = template.toInventoryItem(acquisitionSource == com.kinderman.sdo.domain.model.ItemAcquisitionSource.HERITAGE)
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
        mechanicalEffects = (canonical.mechanicalEffects + retainedEffects).distinctBy(ItemEffect::id),
        dataVersion = CURRENT_ITEM_DATA_VERSION,
    )
}
