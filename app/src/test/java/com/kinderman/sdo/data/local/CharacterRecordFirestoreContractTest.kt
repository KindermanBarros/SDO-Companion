package com.kinderman.sdo.data.local

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.kinderman.sdo.domain.model.defaultAttributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterRecordFirestoreContractTest {
    @Test
    fun lastSyncedRevisionStaysOnlyInTheLocalDatabase() {
        val getterAnnotation = CharacterRecord::class.java
            .getDeclaredMethod("getLastSyncedAt")
            .getAnnotation(Exclude::class.java)
        val fieldAnnotation = CharacterRecord::class.java
            .getDeclaredField("lastSyncedAt")
            .getAnnotation(Exclude::class.java)

        assertNotNull(getterAnnotation)
        assertNotNull(fieldAnnotation)
        assertEquals(456L, CharacterRecord(lastSyncedAt = 456L).toDomain().lastSyncedAt)
        assertEquals(456L, CharacterRecord(lastSyncedAt = 456L).toDomain().toRecord().lastSyncedAt)
    }

    @Test
    fun isLockedKeepsTheSecurityRulesFieldName() {
        val getterAnnotation = CharacterRecord::class.java
            .getDeclaredMethod("isLocked")
            .getAnnotation(PropertyName::class.java)
        val fieldAnnotation = CharacterRecord::class.java
            .getDeclaredField("isLocked")
            .getAnnotation(PropertyName::class.java)

        assertNotNull(getterAnnotation)
        assertNotNull(fieldAnnotation)
        assertEquals("isLocked", getterAnnotation.value)
        assertEquals("isLocked", fieldAnnotation.value)
    }

    @Test
    fun legacyNotesAreMigratedToTheFirstPersonalRecord() {
        val character = CharacterRecord(id = "character-1", notes = "Pista antiga").toDomain()

        assertEquals(1, character.personalNotes.size)
        assertEquals("Registro Pessoal 1", character.personalNotes.single().title)
        assertEquals("Pista antiga", character.personalNotes.single().text)
        assertEquals("", character.toRecord().notes)
    }

    @Test
    fun resourceConverterReadsTheLegacyFormatWithoutAnAdjustment() {
        val resource = CharacterConverters().stringToResource("4|12")

        assertEquals(4, resource.current)
        assertEquals(12, resource.maximum)
        assertEquals(0, resource.adjustment)
        assertTrue(CharacterConverters().resourceToString(resource).endsWith("|0"))
    }

    @Test
    fun defaultLegacyProtectionsAdoptAutomaticCalculations() {
        val attributes = defaultAttributes().map { attribute ->
            if (attribute.acronym == "AGI") attribute.copy(value = 2) else attribute
        }
        val character = CharacterRecord(attributes = attributes).toDomain()

        assertEquals(0, character.protectionAdjustments.getValue("Esquiva"))
        assertEquals(12, character.protectionTotal("Esquiva"))
    }

    @Test
    fun customizedLegacyProtectionTotalsArePreservedAsAdjustments() {
        val attributes = defaultAttributes().map { attribute ->
            if (attribute.acronym == "AGI") {
                attribute.copy(
                    value = 2,
                    skills = attribute.skills.map { if (it.name == "Reflexos") it.copy(value = 2) else it },
                )
            } else {
                attribute
            }
        }
        val character = CharacterRecord(
            attributes = attributes,
            protections = linkedMapOf(
                "Geral" to 19,
                "Esquiva" to 23,
                "Postura" to 10,
                "Mental" to 10,
                "Arcana" to 10,
            ),
        ).toDomain()

        assertEquals(9, character.protectionAdjustments.getValue("Geral"))
        assertEquals(0, character.protectionAdjustments.getValue("Esquiva"))
        assertEquals(23, character.protectionTotal("Esquiva"))
        assertEquals(character.calculatedProtections(), character.toRecord().protections)
    }

    @Test
    fun convertersPreserveEquipmentProtectionAndBodyAssignments() {
        val converters = CharacterConverters()
        val item = com.kinderman.sdo.domain.model.InventoryItem(id = "armor-1", pg = 4, pl = 3)
        val region = com.kinderman.sdo.domain.model.BodyRegion(name = "Braço", equippedItemIds = listOf(item.id))

        assertEquals(item, converters.stringToInventory(converters.inventoryToString(listOf(item))).single())
        assertEquals(region, converters.stringToBody(converters.bodyToString(listOf(region))).single())
    }

    @Test
    fun raceSelectionMetadataSurvivesLocalAndFirestoreMapping() {
        val record = CharacterRecord(race = "Humanos", subRace = "Oráculo", raceAttribute = "AGI")

        assertEquals("AGI", record.toDomain().raceAttribute)
        assertEquals("AGI", record.toDomain().toRecord().raceAttribute)
    }
}
