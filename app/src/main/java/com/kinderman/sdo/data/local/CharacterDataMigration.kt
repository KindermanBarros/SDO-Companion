package com.kinderman.sdo.data.local

import com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION

const val CURRENT_CREATION_RULES_VERSION = 2

/** Content-aware migration shared by Room reads and Firestore synchronization. */
fun CharacterRecord.requiresStructuredMigration(): Boolean =
    itemSchemaVersion < CURRENT_ITEM_DATA_VERSION ||
        creationRulesVersion < CURRENT_CREATION_RULES_VERSION ||
        inventory.any { it.dataVersion < CURRENT_ITEM_DATA_VERSION }

fun CharacterRecord.migratedStructuredRecord(markDirty: Boolean): CharacterRecord {
    if (!requiresStructuredMigration()) return this
    val migrationSource = if (itemSchemaVersion < CURRENT_ITEM_DATA_VERSION) {
        copy(inventory = inventory.map { item -> item.copy(dataVersion = minOf(item.dataVersion, itemSchemaVersion)) })
    } else this
    return migrationSource.toDomain().copy(
        itemSchemaVersion = CURRENT_ITEM_DATA_VERSION,
        creationRulesVersion = maxOf(creationRulesVersion, CURRENT_CREATION_RULES_VERSION),
    ).toRecord().copy(
        itemSchemaVersion = CURRENT_ITEM_DATA_VERSION,
        creationRulesVersion = maxOf(creationRulesVersion, CURRENT_CREATION_RULES_VERSION),
        dirty = dirty || markDirty,
        lastSyncedAt = lastSyncedAt,
    )
}
