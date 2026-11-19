package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterFieldMergeTest {
    @Test fun independentEditsPreserveBothFieldsEvenWhenDocumentTimesDiffer() {
        val local = Character(id = "shared", name = "Ayla", money = 5, updatedAt = 300,
            fieldUpdatedAt = mapOf("name" to 300L, "money" to 100L))
        val remote = local.copy(name = "Old name", money = 20, updatedAt = 400,
            fieldUpdatedAt = mapOf("name" to 100L, "money" to 250L))

        val conflict = CharacterSyncConflict(local, remote, remote.updatedAt)
        val result = mergeCharacterConflict(local, remote, automaticRemoteFieldIds(conflict))

        assertEquals("Ayla", result.name)
        assertEquals(20, result.money)
        assertEquals(mapOf("name" to 300L, "money" to 250L), result.fieldUpdatedAt)
    }

    @Test fun oneFieldUsesItsLatestEditRatherThanTheLatestDocument() {
        val local = Character(id = "shared", name = "Latest", updatedAt = 300, fieldUpdatedAt = mapOf("name" to 200L))
        val remote = local.copy(name = "Older", updatedAt = 500, fieldUpdatedAt = mapOf("name" to 100L))

        assertTrue(automaticRemoteFieldIds(CharacterSyncConflict(local, remote, remote.updatedAt)).isEmpty())
    }

    @Test fun localEditsStampOnlyChangedFieldsAndKeepMonotonicTime() {
        val before = Character(id = "sheet", name = "Ayla", money = 5, updatedAt = 100,
            fieldUpdatedAt = mapOf("name" to 90L, "money" to 80L))
        val after = before.copy(money = 10).withFieldEditTimes(before, 100)

        assertEquals(90L, after.fieldUpdatedAt["name"])
        assertTrue(after.fieldUpdatedAt.getValue("money") > 100)
        assertTrue(after.updatedAt > before.updatedAt)
    }

    @Test fun equalFieldTimesSelectTheCommittedRemoteValueForEveryAccount() {
        val local = Character(id = "sheet", name = "Local", updatedAt = 200, fieldUpdatedAt = mapOf("name" to 100L))
        val remote = local.copy(name = "Remote", lastEditedBy = "other")
        assertEquals(setOf("name"), automaticRemoteFieldIds(CharacterSyncConflict(local, remote, 200), "owner"))
    }
}
