package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.ItemPart
import com.kinderman.sdo.domain.model.inventoryState
import com.kinderman.sdo.domain.model.withValidInventoryStates

fun Character.withNormalizedInventory(): Character {
    val normalized = inventory.mapNotNull(InventoryItem::normalized)
    return (if (normalized == inventory) this else copy(inventory = normalized)).withValidInventoryStates()
}

internal fun InventoryItem.normalized(): InventoryItem? {
    val knownMaterialIds = (ItemCreationRules.weaponMaterials + ItemCreationRules.armorMaterials).mapTo(hashSetOf(), ItemPart::id)
    val normalizedSecondaryMaterialId = secondaryMaterialId.takeIf { it != materialId && it in knownMaterialIds }.orEmpty()
    val durabilityWasNotDefined = durabilityMax <= 0
    val validEnhancementIds = CanonicalItemCatalog.enhancements.mapTo(hashSetOf()) { it.part.id }
    val cleanedEnhancements = installedEnhancements.filter {
        it.catalogEntryId in validEnhancementIds && it.durabilityMax > 0 && it.durabilityCurrent > 0
    }.distinctBy { it.catalogEntryId }
    val cleanedEffects = mechanicalEffects.filterNot { effect ->
        effect.id.startsWith("gema_") || effect.id.startsWith("tech_") ||
            effect.id in gemIds || effect.id in technologyIds
    }
    val durabilityNormalized = if (durabilityWasNotDefined) {
        copy(durabilityCurrent = 1, durabilityMax = 1, secondaryMaterialId = normalizedSecondaryMaterialId)
    } else copy(durabilityCurrent = durabilityCurrent.coerceIn(0, durabilityMax), secondaryMaterialId = normalizedSecondaryMaterialId)
    val cleaned = durabilityNormalized.copy(
        gemIds = emptyList(), technologyIds = emptyList(), gemSlots = 0, technologySlots = 0,
        installedEnhancements = cleanedEnhancements,
        enhancementSlots = enhancementSlots.coerceAtLeast(cleanedEnhancements.size),
        mechanicalEffects = (cleanedEffects + ItemCreationRules.enhancementEffects(cleanedEnhancements)).distinctBy(ItemEffect::id),
    )
    val resolved = ItemCreationRules.inventoryTemplate(catalogEntryId, name)
    if (dataVersion >= CURRENT_ITEM_DATA_VERSION) {
        val (entry, template) = resolved ?: return cleaned
        return cleaned.copy(
            category = category.ifBlank { template.category },
            catalogEntryId = catalogEntryId.ifBlank { entry.id },
            catalogVersion = maxOf(catalogVersion, entry.version),
            canonical = true,
            backpackCapacity = backpackCapacity.takeIf { it > 0 } ?: template.backpackCapacity,
        )
    }

    val normalizedState = durabilityNormalized.inventoryState.storageCode
    val retainedEffects = (
        ItemCreationRules.componentEffects(modificationIds, emptyList(), emptyList()) + cleaned.mechanicalEffects
    ).distinctBy(ItemEffect::id)
    if (resolved == null) {
        return cleaned.copy(
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
        secondaryMaterialId = normalizedSecondaryMaterialId,
        durabilityCurrent = finalDurabilityCurrent,
        durabilityMax = finalDurabilityMax,
        mechanicalEffects = (canonical.mechanicalEffects + retainedEffects).distinctBy(ItemEffect::id),
        dataVersion = CURRENT_ITEM_DATA_VERSION,
    )
}
