package com.kinderman.sdo.domain.model

data class LimitContribution(
    val sourceId: String,
    val label: String,
    val value: Int,
)

data class LimitBreakdown(
    val total: Int?,
    val contributions: List<LimitContribution>,
)

fun Character.generalProtectionBreakdown(): CalculatedValue {
    val typed = EquipmentEffectEngine.resolve(this).entries.filter { it.type == ItemEffectType.PG }
    val itemModifiers = typed.map { audit ->
        ValueModifier(
            sourceType = ModifierSourceType.ITEM,
            sourceId = audit.itemId,
            label = audit.itemName.ifBlank { "Item sem nome" },
            value = audit.value,
        )
    }
    return CalculatedValue(
        base = 10,
        adjustment = protectionAdjustments["Geral"] ?: 0,
        modifiers = itemModifiers + powerValueModifiers(AbilityModifierTarget.PROTECTION, "Geral"),
    )
}

fun Character.localProtectionBreakdown(region: BodyRegion): CalculatedValue {
    val equippedIds = region.equippedItemIds.toSet()
    val typed = EquipmentEffectEngine.resolve(this).entries.filter {
        it.type == ItemEffectType.PL && it.itemId in equippedIds
    }
    return CalculatedValue(
        base = region.localProtection,
        modifiers = typed.map { item ->
            ValueModifier(
                sourceType = ModifierSourceType.ITEM,
                sourceId = item.itemId,
                label = item.itemName.ifBlank { "Item sem nome" },
                value = item.value,
            )
        },
    )
}

fun Character.agilityLimitBreakdown(): LimitBreakdown {
    val contributions = EquipmentEffectEngine.resolve(this).entries
        .filter { it.type == ItemEffectType.AGILITY_LIMIT }
        .map { item ->
            LimitContribution(
                sourceId = item.itemId,
                label = item.itemName.ifBlank { "Item sem nome" },
                value = item.value,
            )
        }
    return LimitBreakdown(
        total = contributions.minOfOrNull(LimitContribution::value),
        contributions = contributions,
    )
}

private fun Character.equippedInventoryItemsForBreakdown(): List<InventoryItem> {
    val equippedIds = bodyRegions.flatMap(BodyRegion::equippedItemIds).toSet()
    return inventory.filter { it.id in equippedIds }
}
