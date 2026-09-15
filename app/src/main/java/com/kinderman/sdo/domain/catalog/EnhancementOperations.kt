package com.kinderman.sdo.domain.catalog

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.EnhancementKind
import com.kinderman.sdo.domain.model.InstalledEnhancement
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemAcquisitionSource
import com.kinderman.sdo.domain.model.ItemCondition
import com.kinderman.sdo.domain.model.synchronizeItemPowers

/** Creates a loose enhancement. Installation and removal move this same instance in and out of a host item. */
fun enhancementInventoryItem(catalogEntryId: String, initialCreation: Boolean = false): InventoryItem {
    val definition = BundledItemCatalog.enhancements.first { it.part.id == catalogEntryId }
    return InventoryItem(
        name = definition.part.name,
        category = if (definition.kind == "TECHNOLOGY") "Tecnologia" else "Gema",
        durabilityCurrent = definition.part.durability.coerceAtLeast(1),
        durabilityMax = definition.part.durability.coerceAtLeast(1),
        effect = definition.effect.description,
        catalogEntryId = definition.part.id,
        catalogVersion = 1,
        canonical = true,
        acquisitionSource = if (initialCreation) ItemAcquisitionSource.HERITAGE else ItemAcquisitionSource.NARRATIVE,
        heritageCost = definition.part.creationCost.takeIf { initialCreation },
        purchasePrice = definition.part.price,
        quantity = 1,
        enhancementChargesCurrent = definition.maxCharges,
        enhancementChargesMax = definition.maxCharges,
    )
}

fun Character.installEnhancement(sourceItemId: String, hostItemId: String): Character {
    val source = inventory.first { it.id == sourceItemId && it.category in setOf("Gema", "Tecnologia") }
    val host = inventory.first { it.id == hostItemId }
    require(host.installedEnhancements.size < host.enhancementSlots) { "O item não possui espaço livre." }
    val definition = BundledItemCatalog.enhancements.first { it.part.id == source.catalogEntryId }
    val base = (ItemCreationRules.weaponBases + ItemCreationRules.armorBases).firstOrNull { it.id == host.baseId }
    val compatibleIds = base?.let { ItemCreationRules.compatibleEnhancements(it, initialCreation = false).mapTo(hashSetOf()) { part -> part.id } }.orEmpty()
    require(source.catalogEntryId in compatibleIds) { "O aprimoramento não é compatível com este item." }
    require(definition.kind != "TECHNOLOGY" || host.installedEnhancements.none { it.catalogEntryId == source.catalogEntryId }) {
        "A mesma Tecnologia não pode ser instalada duas vezes."
    }
    val installed = InstalledEnhancement(
        id = source.id,
        catalogEntryId = source.catalogEntryId,
        kind = if (definition.kind == "TECHNOLOGY") EnhancementKind.TECHNOLOGY else EnhancementKind.GEM,
        durabilityCurrent = source.durabilityCurrent,
        durabilityMax = source.durabilityMax,
        chargesCurrent = source.enhancementChargesCurrent.coerceAtMost(source.enhancementChargesMax),
        chargesMax = source.enhancementChargesMax,
    )
    val updatedHost = ItemCreationRules.installEnhancement(host, source.catalogEntryId).let { generated ->
        generated.copy(installedEnhancements = generated.installedEnhancements.dropLast(1) + installed)
    }
    return copy(inventory = inventory.filterNot { it.id == sourceItemId }.map { if (it.id == hostItemId) updatedHost else it })
        .synchronizeItemPowers()
}

fun Character.removeEnhancement(hostItemId: String, installationId: String): Character {
    val host = inventory.first { it.id == hostItemId }
    val installed = host.installedEnhancements.first { it.id == installationId }
    val definition = BundledItemCatalog.enhancements.first { it.part.id == installed.catalogEntryId }
    val loose = enhancementInventoryItem(installed.catalogEntryId).copy(
        id = installed.id,
        durabilityCurrent = installed.durabilityCurrent,
        durabilityMax = installed.durabilityMax,
        enhancementChargesCurrent = installed.chargesCurrent,
        enhancementChargesMax = installed.chargesMax,
    )
    val updatedHost = ItemCreationRules.removeEnhancement(host, installationId)
    return copy(inventory = inventory.map { if (it.id == hostItemId) updatedHost else it } + loose).synchronizeItemPowers()
}

/** Applies the catalogued consequence after a failed Ourivesaria, Engenharia, or Ofício test. */
fun Character.failEnhancementOperation(hostItemId: String, enhancementInstanceId: String): Character {
    val host = inventory.first { it.id == hostItemId }
    val installed = host.installedEnhancements.firstOrNull { it.id == enhancementInstanceId }
    val loose = inventory.firstOrNull { it.id == enhancementInstanceId }
    val catalogId = installed?.catalogEntryId ?: loose?.catalogEntryId ?: error("Aprimoramento não encontrado.")
    val definition = BundledItemCatalog.enhancements.first { it.part.id == catalogId }
    val damagedInstalled = installed?.copy(
        durabilityCurrent = (installed.durabilityCurrent - definition.failureEnhancementDamage).coerceAtLeast(0),
    )
    val remainingInstalled = host.installedEnhancements.filterNot { it.id == enhancementInstanceId } +
        listOfNotNull(damagedInstalled?.takeIf { it.durabilityCurrent > 0 })
    val hostDurability = (host.durabilityCurrent - definition.failureItemDamage).coerceAtLeast(0)
    val updatedHost = host.copy(
        durabilityCurrent = hostDurability,
        itemCondition = if (hostDurability == 0) ItemCondition.BROKEN else host.itemCondition,
        installedEnhancements = remainingInstalled,
        mechanicalEffects = ItemCreationRules.enhancementEffects(remainingInstalled) + host.mechanicalEffects.filterNot { effect ->
            BundledItemCatalog.enhancements.any { it.effect.id == effect.id }
        },
    )
    val damagedLoose = loose?.copy(durabilityCurrent = (loose.durabilityCurrent - definition.failureEnhancementDamage).coerceAtLeast(0))
    return copy(inventory = inventory.mapNotNull { item ->
        when (item.id) {
            hostItemId -> updatedHost
            enhancementInstanceId -> damagedLoose?.takeIf { it.durabilityCurrent > 0 }
            else -> item
        }
    }).synchronizeItemPowers()
}

fun Character.spendEnhancementCharge(hostItemId: String, installationId: String): Character =
    updateEnhancement(hostItemId, installationId) { installed ->
        require(installed.chargesCurrent > 0) { "O aprimoramento está sem cargas." }
        installed.copy(chargesCurrent = installed.chargesCurrent - 1)
    }

fun Character.rechargeEnhancement(hostItemId: String, installationId: String): Character =
    updateEnhancement(hostItemId, installationId) { installed ->
        installed.copy(chargesCurrent = (installed.chargesCurrent + 1).coerceAtMost(installed.chargesMax))
    }

private fun Character.updateEnhancement(
    hostItemId: String,
    installationId: String,
    transform: (InstalledEnhancement) -> InstalledEnhancement,
): Character = copy(inventory = inventory.map { item ->
    if (item.id != hostItemId) item else {
        val updated = item.installedEnhancements.map { if (it.id == installationId) transform(it) else it }
        item.copy(
            installedEnhancements = updated,
            mechanicalEffects = item.mechanicalEffects.filterNot { effect ->
                BundledItemCatalog.enhancements.any { it.effect.id == effect.id }
            } + ItemCreationRules.enhancementEffects(updated),
        )
    }
}).synchronizeItemPowers()
