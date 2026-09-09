package com.kinderman.sdo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kinderman.sdo.data.local.AppDatabase
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.repository.LocalCatalogRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CatalogMigrationTest {
    @Test fun upgradeFrom15PreservesOfflineCharacterAndLoadsFullCatalog() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-integrity.db"
        context.deleteDatabase(name)
        try {
            // Reconstruct v15: the only schema delta in v16 is these four catalog columns.
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().use { db ->
                db.characterDao().upsert(CharacterRecord(id = "offline", name = "Ficha preservada", dirty = true))
            }
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                listOf("limit", "activationCondition", "enhancements", "deactivationCondition").forEach {
                    db.execSQL("ALTER TABLE catalog_entries DROP COLUMN `$it`")
                }
                db.execSQL("DELETE FROM room_master_table")
                db.version = 15
            }
            Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.MIGRATION_15_16).build().use { db ->
                    val character = db.characterDao().get("offline")!!
                    assertEquals("Ficha preservada", character.name)
                    assertTrue(character.dirty)
                    val repository = LocalCatalogRepository(db.catalogDao())
                    repository.refreshBundledCatalog()
                    val entries = repository.observe().first()
                    assertTrue(entries.size >= 400)
                    assertEquals(entries.size, entries.map { it.id }.distinct().size)
                    assertTrue(entries.any { it.activationCondition.isNotBlank() })
                }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
