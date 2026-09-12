package com.kinderman.sdo.data.local

import com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION
import com.kinderman.sdo.domain.model.CANONICAL_SCHEMA_VERSION
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.BodyRegion
import com.kinderman.sdo.domain.model.DomainError
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ProgressionRecord
import com.kinderman.sdo.domain.model.ProgressionReward
import com.kinderman.sdo.domain.model.ProgressionRewardType
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
            assertEquals(CANONICAL_SCHEMA_VERSION, migrated.canonicalSchemaVersion)
            assertEquals(CURRENT_CREATION_RULES_VERSION, migrated.creationRulesVersion)
            assertTrue(migrated.dirty)
            assertFalse(migrated.requiresStructuredMigration())
            assertEquals(migrated, migrated.migratedStructuredRecord(markDirty = true))
        }

        val custom = fixtures.last().migratedStructuredRecord(markDirty = true)
        assertEquals("Relíquia caseira", custom.inventory.single().name)
        assertEquals(1, custom.toDomain().itemStates.size)
        assertEquals(1, custom.toDomain().customItemCatalog.size)
    }

    @Test fun firestoreMigrationCanPreserveCleanSynchronizationState() {
        val remote = CharacterRecord(id = "remote", ownerId = "account-a", itemSchemaVersion = 0, dirty = false)
        val migrated = remote.migratedStructuredRecord(markDirty = false)

        assertFalse(migrated.dirty)
        assertEquals(CURRENT_ITEM_DATA_VERSION, migrated.itemSchemaVersion)
    }

    @Test fun ambiguousMechanicsBecomeIdempotentReviewRecords() {
        val legacy = CharacterRecord(
            id = "legacy", canonicalSchemaVersion = 0,
            conditions = listOf(com.kinderman.sdo.domain.model.ConditionEffect(id = "condition", name = "Abalado", duration = "até descansar")),
        )
        val migrated = legacy.migratedStructuredRecord(markDirty = true)
        val reloaded = migrated.toDomain()

        assertEquals(1, reloaded.migrationReviews.size)
        assertEquals("até descansar", reloaded.migrationReviews.single().legacyValue)
        assertEquals(migrated, migrated.migratedStructuredRecord(markDirty = true))
        assertEquals(reloaded.migrationReviews, reloaded.toRecord().toDomain().migrationReviews)
    }

    @Test fun missingCanonicalPayloadsMigrateAndUnmappedBodySlotsRemainReviewable() {
        val legacy = CharacterRecord(
            id = "legacy-body",
            canonicalSchemaVersion = CANONICAL_SCHEMA_VERSION,
            itemSchemaVersion = CURRENT_ITEM_DATA_VERSION,
            creationRulesVersion = CURRENT_CREATION_RULES_VERSION,
            bodyRegions = listOf(BodyRegion(roll = 11, name = "Membro protético")),
        )

        val migrated = legacy.migratedStructuredRecord(markDirty = false)

        assertFalse(migrated.requiresStructuredMigration())
        assertTrue(migrated.migrationReviewPayload.isNotBlank())
        assertEquals("Membro protético", migrated.toDomain().migrationReviews.single { it.field.startsWith("body.region:") }.legacyValue)
        assertEquals(migrated, migrated.migratedStructuredRecord(markDirty = false))
    }

    @Test(expected = DomainError.UnsupportedSchemaVersion::class)
    fun newerCanonicalSchemasAreNotDowngraded() {
        CharacterRecord(canonicalSchemaVersion = CANONICAL_SCHEMA_VERSION + 1)
            .migratedStructuredRecord(markDirty = false)
    }

    @Test fun malformedCanonicalPayloadsBecomeReviewItemsInsteadOfCrashingReads() {
        val legacy = CharacterRecord(
            id = "broken-canonical-payload",
            canonicalAbilitiesPayload = "{not-json",
        )

        val migrated = legacy.migratedStructuredRecord(markDirty = false)
        val review = migrated.toDomain().migrationReviews.single { it.field == "canonicalAbilitiesPayload" }

        assertEquals("{not-json", review.legacyValue)
        assertEquals(migrated, migrated.migratedStructuredRecord(markDirty = false))
    }

    @Test fun inventoryAndProgressionBecomeCanonicalAuthoritiesWithoutTextInference() {
        val legacy = CharacterRecord(
            id = "migration-complete",
            inventory = listOf(InventoryItem(
                id = "custom-item", name = "Relíquia sem catálogo", effect = "Escolha da mesa",
                quantity = 2, linkedAshId = "ash-1",
            )),
            progressionHistory = listOf(ProgressionRecord(
                id = "level-2", previousLevel = 1, newLevel = 2, appliedAt = 42,
                rewards = listOf(ProgressionReward(2, ProgressionRewardType.ATTRIBUTE, "AGI")),
            )),
        )

        val migrated = legacy.migratedStructuredRecord(markDirty = false)
        val character = migrated.toDomain()

        assertEquals(2, character.itemStates.single().consumable?.doses)
        assertEquals("Relíquia sem catálogo", character.customItemCatalog.single().name)
        assertEquals("character:migration-complete:custom-item", character.itemStates.single().definition.id.value)
        assertEquals(1, character.progression.choices.size)
        assertTrue(character.migrationReviews.any { it.field == "inventory.mechanics:custom-item" })
        assertEquals(migrated, migrated.migratedStructuredRecord(markDirty = false))
    }

    @Test(expected = DomainError.LegacyWriteRejected::class)
    fun serializationBoundaryRejectsNewLegacyWrites() {
        Character(canonicalSchemaVersion = 0).toRecord()
    }
}
