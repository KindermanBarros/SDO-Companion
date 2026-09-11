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
    val itemModifiers = if (typed.isNotEmpty()) {
        typed.map { audit ->
            ValueModifier(
                sourceType = ModifierSourceType.ITEM,
                sourceId = audit.itemId,
                label = audit.itemName.ifBlank { "Item sem nome" },
                value = audit.value,
            )
        }
    } else {
        val equipped = inventory.filter {
            !it.isBroken && it.inventoryState in setOf(InventoryState.EQUIPPED, InventoryState.WIELDED)
        }
        equipped.filterNot { it.isBroken || it.category.equals("Escudo", true) && it.inventoryState != InventoryState.WIELDED }
            .filter { it.pg != 0 }
            .map { item ->
                ValueModifier(
                    sourceType = ModifierSourceType.ITEM,
                    sourceId = item.id,
                    label = item.name.ifBlank { "Item sem nome" },
                    value = if (item.isScrap) item.pg / 2 else item.pg,
                )
            }
    }
    return CalculatedValue(
        base = 10,
        adjustment = protectionAdjustments["Geral"] ?: 0,
        modifiers = itemModifiers + powerValueModifiers(AbilityModifierTarget.PROTECTION, "Geral"),
    )
}

fun Character.localProtectionBreakdown(region: BodyRegion): CalculatedValue {
    val equippedIds = region.equippedItemIds.toSet()
    return CalculatedValue(
        base = region.localProtection,
        modifiers = inventory
            .filter { it.id in equippedIds && it.pl != 0 && !it.isBroken }
            .map { item ->
                ValueModifier(
                    sourceType = ModifierSourceType.ITEM,
                    sourceId = item.id,
                    label = item.name.ifBlank { "Item sem nome" },
                    value = if (item.isScrap) item.pl / 2 else item.pl,
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
