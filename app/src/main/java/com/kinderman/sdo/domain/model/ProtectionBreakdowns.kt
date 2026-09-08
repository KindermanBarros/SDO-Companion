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
    val equipped = equippedInventoryItemsForBreakdown()
    return CalculatedValue(
        base = 10,
        adjustment = protectionAdjustments["Geral"] ?: 0,
        modifiers = equipped.filter { it.pg != 0 }.map { item ->
            ValueModifier(
                sourceType = ModifierSourceType.ITEM,
                sourceId = item.id,
                label = item.name.ifBlank { "Item sem nome" },
                value = item.pg,
            )
        },
    )
}

fun Character.localProtectionBreakdown(region: BodyRegion): CalculatedValue {
    val equippedIds = region.equippedItemIds.toSet()
    return CalculatedValue(
        base = region.localProtection,
        modifiers = inventory
            .filter { it.id in equippedIds && it.pl != 0 }
            .map { item ->
                ValueModifier(
                    sourceType = ModifierSourceType.ITEM,
                    sourceId = item.id,
                    label = item.name.ifBlank { "Item sem nome" },
                    value = item.pl,
                )
            },
    )
}

fun Character.agilityLimitBreakdown(): LimitBreakdown {
    val contributions = equippedInventoryItemsForBreakdown()
        .mapNotNull { item ->
            item.agilityLimit?.let { limit ->
                LimitContribution(
                    sourceId = item.id,
                    label = item.name.ifBlank { "Item sem nome" },
                    value = limit,
                )
            }
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
