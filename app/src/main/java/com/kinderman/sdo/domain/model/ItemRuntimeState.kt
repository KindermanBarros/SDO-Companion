package com.kinderman.sdo.domain.model

import com.kinderman.sdo.domain.catalog.BuiltInCatalog

private val ashCatalogIds by lazy { BuiltInCatalog.entries.filter { it.kind == CatalogKind.ASH }.mapTo(mutableSetOf()) { it.id } }

/** Consumable counters projected from canonical state, with explicit legacy compatibility. */
data class ItemRuntimeState(val kind: ConsumableStateKind, val remaining: Int)

fun Character.isAshItem(item: InventoryItem, catalogKind: CatalogKind? = null): Boolean =
    catalogKind == CatalogKind.ASH || item.catalogEntryId in ashCatalogIds || item.linkedAshId.isNotBlank() || item.category.equals("Cinza", true) ||
        allCanonicalAbilitiesSafely().any { it.kind == AbilityKind.ASH && it.ashData?.linkedItemId?.value == item.id }

fun Character.itemRuntimeStates(item: InventoryItem, catalogKind: CatalogKind? = null): List<ItemRuntimeState> {
    val state = itemStates.firstOrNull { it.id.value == item.id }
    val ash = isAshItem(item, catalogKind)
    return buildList {
        if (ash) add(ItemRuntimeState(ConsumableStateKind.Ash, state?.consumable?.doses ?: item.quantity))
        else if (state?.consumable?.let { it.doses > 0 || it.charges == null } == true || item.category.contains("Consumível", true)) {
            add(ItemRuntimeState(ConsumableStateKind.Dose, state?.consumable?.doses ?: item.quantity))
        }
        if (state?.ammo != null || item.category.equals("Munição", true)) {
            add(ItemRuntimeState(ConsumableStateKind.Ammo, state?.ammo?.loaded ?: item.quantity))
        }
        if (state?.consumable?.charges != null || item.enhancementChargesMax > 0) {
            add(ItemRuntimeState(ConsumableStateKind.Charge, state?.consumable?.charges ?: item.enhancementChargesCurrent))
        }
    }
}

fun Character.consumptionEffects(item: InventoryItem): List<EffectOperation> {
    val state = itemStates.firstOrNull { it.id.value == item.id }
    val itemEffects = state?.let { current ->
        customItemCatalog.firstOrNull { it.reference == current.definition }?.mechanicalEffect
    }?.takeIf { it.trigger == null || it.trigger == TriggerSpec.Manual || it.trigger == TriggerSpec.OnApply }
        ?.operations.orEmpty()
    val ash = allCanonicalAbilitiesSafely().firstOrNull {
        it.kind == AbilityKind.ASH && (it.ashData?.linkedItemId?.value == item.id || it.id == item.linkedAshId)
    }
    if (ash != null && !ash.available) throw DomainError.ValidationError("Esta Cinza está indisponível.")
    return itemEffects + item.mechanicalEffects.mapNotNull { it.onConsume } + ash?.mechanicalEffect?.operations.orEmpty()
}

/** Counter update and all declared effects are one mutation; failure leaves the original intact. */
internal fun Character.consumeRuntimeItem(
    operation: EffectOperation.ConsumeItemState,
    actorId: String,
    idempotencyKey: String,
    reason: String,
    stack: Set<String>,
): Character {
    val item = inventory.firstOrNull { it.id == operation.itemInstanceId.value }
        ?: throw DomainError.InvalidReference("Item não encontrado no inventário.")
    val key = "${item.id}:${operation.state}"
    if (operation.applyEffects && (key in stack || stack.size >= 32)) throw DomainError.ValidationError("Ciclo nos efeitos de consumo do item.")
    val canonical = itemStates.firstOrNull { it.id == operation.itemInstanceId }
    val available = itemRuntimeStates(item).firstOrNull { it.kind == operation.state }
        ?: if (operation.state == ConsumableStateKind.Ash && canonical?.consumable != null) {
            ItemRuntimeState(ConsumableStateKind.Ash, canonical.consumable.doses)
        } else throw DomainError.InvalidReference("O item não possui este estado de consumo.")
    if (available.remaining < operation.amount) throw DomainError.InsufficientResource("Quantidade insuficiente do item.")
    val remaining = available.remaining - operation.amount
    val changedItem = when (operation.state) {
        ConsumableStateKind.Dose, ConsumableStateKind.Ash -> item.copy(quantity = remaining)
        ConsumableStateKind.Ammo -> if (canonical?.ammo == null) item.copy(quantity = remaining) else item
        ConsumableStateKind.Charge -> if (item.enhancementChargesMax > 0) item.copy(enhancementChargesCurrent = remaining) else item
    }
    val changedState = canonical?.let {
        when (operation.state) {
            ConsumableStateKind.Dose, ConsumableStateKind.Ash -> it.copy(
                stack = it.stack.copy(quantity = remaining),
                consumable = (it.consumable ?: ConsumableState()).copy(doses = remaining),
            )
            ConsumableStateKind.Ammo -> if (it.ammo != null) it.copy(
                ammo = it.ammo.copy(loaded = remaining),
                reload = if (remaining == 0) ReloadState(ReloadPhase.Empty) else it.reload,
            ) else it.copy(stack = it.stack.copy(quantity = remaining))
            ConsumableStateKind.Charge -> it.copy(consumable = (it.consumable ?: ConsumableState()).copy(charges = remaining))
        }
    }
    var result = copy(
        inventory = inventory.map { if (it.id == item.id) changedItem else it },
        itemStates = itemStates.map { if (it.id == canonical?.id) changedState!! else it },
    )
    if (operation.applyEffects) {
        val effects = consumptionEffects(item)
        repeat(operation.amount) { dose ->
            effects.forEachIndexed { index, effect ->
                result = result.applyEffectOperation(
                    effect, actorId, "$idempotencyKey:consume:$dose:$index", reason,
                    consumptionStack = stack + key,
                ).character
            }
        }
    }
    return result
}
