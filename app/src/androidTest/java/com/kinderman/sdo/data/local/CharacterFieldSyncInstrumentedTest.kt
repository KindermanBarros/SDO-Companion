package com.kinderman.sdo.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinderman.sdo.domain.model.Character
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CharacterFieldSyncInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val dao = database.characterDao()

    @After fun close() = database.close()

    @Test fun staleEditorDoesNotOverwriteAnUnrelatedRemoteField() = runBlocking {
        val base = Character(id = "sheet", name = "Ayla", money = 5, dirty = false, updatedAt = 100)
        dao.upsert(base.toRecord())
        dao.upsert(base.copy(money = 20, updatedAt = 200, fieldUpdatedAt = mapOf("money" to 200L)).toRecord())

        dao.saveEditorChange(base.copy(name = "New name", editingBase = base, updatedAt = 300, dirty = true))

        val result = dao.get(base.id)!!.toDomain()
        assertEquals("New name", result.name)
        assertEquals(20, result.money)
        assertEquals(200L, result.fieldUpdatedAt["money"])
        assertEquals(300L, result.fieldUpdatedAt["name"])
    }

    @Test fun responseToAnOlderWriteCannotClearOrReplaceANewerLocalEdit() = runBlocking {
        val current = Character(id = "sheet", name = "New local", money = 5, updatedAt = 300,
            fieldUpdatedAt = mapOf("name" to 300L, "money" to 100L))
        dao.upsert(current.toRecord())
        dao.acknowledgeIfUnchanged(current.id, 100, 200)
        assertTrue(dao.get(current.id)!!.dirty)
        val remote = current.copy(name = "Old local", money = 20, updatedAt = 250, dirty = false,
            fieldUpdatedAt = mapOf("name" to 100L, "money" to 250L))
        dao.acceptSnapshot(remote.toRecord(), 100)
        val result = dao.get(current.id)!!
        assertEquals("New local", result.name)
        assertEquals(20, result.money)
        assertTrue(result.dirty)
    }

    @Test fun matchingAcknowledgementClearsOnlyTheUploadedRevision() = runBlocking {
        val character = Character(id = "sheet", updatedAt = 100)
        dao.upsert(character.toRecord())
        dao.acknowledgeIfUnchanged(character.id, 100, 200)
        assertFalse(dao.get(character.id)!!.dirty)
        assertEquals(200L, dao.get(character.id)!!.lastSyncedAt)
    }
}
