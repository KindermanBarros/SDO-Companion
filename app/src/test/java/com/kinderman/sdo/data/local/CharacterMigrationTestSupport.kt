package com.kinderman.sdo.data.local

private val testMigrationManager = CharacterMigrationManager()

internal fun CharacterRecord.migratedRecord(markDirty: Boolean): CharacterRecord =
    testMigrationManager.migrate(this, markDirty)

internal fun CharacterRecord.requiresMigration(): Boolean = testMigrationManager.requiresMigration(this)
