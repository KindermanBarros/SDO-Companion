package com.kinderman.sdo.data.local

import com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION
import com.kinderman.sdo.domain.model.InventoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterDataMigrationTest {
    @Test fun fixturesFromDifferentAccountsMigrateIdempotently() {
        val fixtures = listOf(
            CharacterRecord(
                id = "account-a-character", ownerId = "account-a", creationRulesVersion = 1, itemSchemaVersion = 0,
                inventory = listOf(InventoryItem(id = "known", name = "Elmo de Ligas Comuns", state = "E")),
            ),
            CharacterRecord(
                id = "account-b-character", ownerId = "account-b", creationRulesVersion = 2, itemSchemaVersion = 0,
                inventory = listOf(InventoryItem(id = "unknown", name = "Relíquia caseira")),
            ),
        )

        fixtures.forEach { fixture ->
            val migrated = fixture.migratedStructuredRecord(markDirty = true)
            assertEquals(CURRENT_ITEM_DATA_VERSION, migrated.itemSchemaVersion)
            assertEquals(CURRENT_CREATION_RULES_VERSION, migrated.creationRulesVersion)
            assertTrue(migrated.dirty)
            assertFalse(migrated.requiresStructuredMigration())
            assertEquals(migrated, migrated.migratedStructuredRecord(markDirty = true))
        }

        assertTrue(fixtures.last().migratedStructuredRecord(markDirty = true).inventory.isEmpty())
    }

    @Test fun firestoreMigrationCanPreserveCleanSynchronizationState() {
        val remote = CharacterRecord(id = "remote", ownerId = "account-a", itemSchemaVersion = 0, dirty = false)
        val migrated = remote.migratedStructuredRecord(markDirty = false)

        assertFalse(migrated.dirty)
        assertEquals(CURRENT_ITEM_DATA_VERSION, migrated.itemSchemaVersion)
    }
}
