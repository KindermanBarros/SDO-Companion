package com.kinderman.sdo.data.local

import com.google.firebase.firestore.PropertyName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
