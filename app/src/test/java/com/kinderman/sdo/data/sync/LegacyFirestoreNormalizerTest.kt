package com.kinderman.sdo.data.sync

import com.google.firebase.Timestamp
import com.kinderman.sdo.data.local.CharacterMigrationManager
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.domain.model.CURRENT_CHARACTER_SCHEMA_VERSION
import com.kinderman.sdo.domain.model.ItemQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyFirestoreNormalizerTest {

    @Test
    fun normalizesLegacyMonolithicDocCoercesTypesAndPreservesUnknownFields() {
        val rawLegacy = mapOf<String, Any?>(
            "id" to "legacy-sheet-1",
            "ownerId" to "uid-player",
            "campaignId" to "default",
            "name" to "Kael",
            "level" to "4",
            "money" to 150.0,
            "notes" to "Anotação antiga do jogador",
            "updatedAt" to Timestamp(1_700_000_000L, 0),
            "life" to mapOf("current" to "12", "maximum" to 20, "adjustment" to "-1"),
            "inventory" to listOf(
                mapOf("id" to "item-1", "name" to "Adaga", "quantity" to 1, "quality" to "Aprimorada"),
            ),
            "legacyCustomAura" to "violet",
            "legacyNestedExtra" to mapOf("tier" to 2, "tag" to "relic"),
        )

        val record = LegacyFirestoreNormalizer.normalizeToRecord("legacy-sheet-1", rawLegacy)
        assertEquals("", record.campaignId)
        assertEquals(4, record.level)
        assertEquals(150, record.money)
        assertEquals(CURRENT_CHARACTER_SCHEMA_VERSION, record.canonicalSchemaVersion)
        assertEquals(12, record.life.current)
        assertEquals(-1, record.life.adjustment)
        assertEquals(ItemQuality.IMPROVED, record.inventory.single().quality)
        assertEquals("Anotação antiga do jogador", record.personalNotes.single().text)
        assertTrue(record.unrecognizedFieldsJson.contains("legacyCustomAura"))
        assertTrue(record.unrecognizedFieldsJson.contains("legacyNestedExtra"))

        // Round-trip through domain -> record -> SparkWireCodec -> record
        val domain = record.toDomain()
        assertEquals(record.unrecognizedFieldsJson, domain.unrecognizedFieldsJson)
        val wireDocs = SparkWireCodec.toSparkDocuments(domain.toRecord())
        assertEquals(
            record.unrecognizedFieldsJson,
            wireDocs.fields["unrecognizedFieldsPayload"]?.get("value"),
        )
        val decoded = SparkWireCodec.fromSparkDocuments(wireDocs)
        assertEquals(record.unrecognizedFieldsJson, decoded.unrecognizedFieldsJson)
    }

    @Test
    fun repeatedMigrationsRemainIdempotentWithoutCorruptingUnknownFields() {
        val rawLegacy = mapOf<String, Any?>(
            "ownerId" to "uid-player",
            "name" to "Lyra",
            "legacyFlag" to true,
        )
        val manager = CharacterMigrationManager()
        val first = LegacyFirestoreNormalizer.normalizeToRecord("legacy-sheet-2", rawLegacy)
        val second = manager.migrate(first, markDirty = false)
        val third = manager.migrate(second, markDirty = false)

        assertFalse(manager.requiresMigration(first))
        assertEquals(first, second)
        assertEquals(second, third)
        assertTrue(third.unrecognizedFieldsJson.contains("legacyFlag"))
    }
}
