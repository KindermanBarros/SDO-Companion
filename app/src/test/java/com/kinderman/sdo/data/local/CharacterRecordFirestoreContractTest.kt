package com.kinderman.sdo.data.local

import com.google.firebase.firestore.PropertyName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterRecordFirestoreContractTest {
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
}
