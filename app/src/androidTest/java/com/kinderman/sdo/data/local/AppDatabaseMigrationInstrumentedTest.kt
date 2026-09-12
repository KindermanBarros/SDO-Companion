package com.kinderman.sdo.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinderman.sdo.data.repository.OfflineFirstCharacterRepository
import com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databases = mutableSetOf<String>()

    @Before fun setUp() = databases.clear()

    @After fun tearDown() {
        databases.forEach(context::deleteDatabase)
    }

    @Test fun migration21To22RunsAgainstARealSqliteDatabase() {
        val name = "migration-21-22.db".also(databases::add)
        openSqlite(name, 21, onCreate = { db ->
            db.execSQL("CREATE TABLE characters (id TEXT NOT NULL PRIMARY KEY)")
            db.execSQL("INSERT INTO characters (id) VALUES ('fixture')")
        }).close()

        val migrated = openSqlite(name, 22, onUpgrade = { db, oldVersion, newVersion ->
            assertEquals(21, oldVersion)
            assertEquals(22, newVersion)
            AppDatabase.MIGRATION_21_22.migrate(db)
        })

        migrated.writableDatabase.query("SELECT itemSchemaVersion FROM characters WHERE id = 'fixture'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test fun migration22To23MarksInventoryForTypedMigration() {
        val name = "migration-22-23.db".also(databases::add)
        openSqlite(name, 22, onCreate = { db ->
            db.execSQL("CREATE TABLE characters (id TEXT NOT NULL PRIMARY KEY, itemSchemaVersion INTEGER NOT NULL DEFAULT 4)")
            db.execSQL("INSERT INTO characters (id, itemSchemaVersion) VALUES ('fixture', 4)")
        }).close()
        val migrated = openSqlite(name, 23, onUpgrade = { db, _, _ -> AppDatabase.MIGRATION_22_23.migrate(db) })
        migrated.writableDatabase.query("SELECT itemSchemaVersion FROM characters WHERE id = 'fixture'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test fun migration23To24AddsCanonicalSchemaAndReviewQueue() {
        val name = "migration-23-24.db".also(databases::add)
        openSqlite(name, 23, onCreate = { db ->
            db.execSQL("CREATE TABLE characters (id TEXT NOT NULL PRIMARY KEY)")
            db.execSQL("INSERT INTO characters (id) VALUES ('fixture')")
        }).close()
        val migrated = openSqlite(name, 24, onUpgrade = { db, _, _ -> AppDatabase.MIGRATION_23_24.migrate(db) })
        migrated.writableDatabase.query("SELECT canonicalSchemaVersion, migrationReviewPayload FROM characters WHERE id = 'fixture'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
            assertEquals("", cursor.getString(1))
        }
        migrated.close()
    }

    @Test fun structuredMigrationAndDerivedEffectsSurviveDatabaseRestart() = runBlocking {
        val name = "restart-acceptance.db".also(databases::add)
        val effect = ItemEffect(
            id = "gem-power",
            type = ItemEffectType.GEM_POWER,
            condition = ItemEffectCondition.WIELDED,
            description = "Pulso rúnico.",
        )
        var database = openRoom(name)
        database.characterDao().upsert(
            CharacterRecord(
                id = "character",
                ownerId = "player",
                itemSchemaVersion = 0,
                inventory = listOf(InventoryItem(id = "weapon", name = "Lâmina", state = "W", mechanicalEffects = listOf(effect))),
            ),
        )
        var repository = OfflineFirstCharacterRepository(database.characterDao(), database.ownerDao(), database.campaignDao())
        assertNotNull(repository.observeOne("character").first()!!.powers.singleOrNull { it.linkedItemId == "weapon" })
        assertEquals(CURRENT_ITEM_DATA_VERSION, database.characterDao().get("character")!!.itemSchemaVersion)
        database.close()

        database = openRoom(name)
        repository = OfflineFirstCharacterRepository(database.characterDao(), database.ownerDao(), database.campaignDao())
        assertNotNull(repository.observeOne("character").first()!!.powers.singleOrNull { it.linkedItemId == "weapon" })
        assertEquals(CURRENT_ITEM_DATA_VERSION, database.characterDao().get("character")!!.itemSchemaVersion)
        database.close()
    }

    private fun openRoom(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(AppDatabase.MIGRATION_21_22)
        .build()

    private fun openSqlite(
        name: String,
        version: Int,
        onCreate: (SupportSQLiteDatabase) -> Unit = {},
        onUpgrade: (SupportSQLiteDatabase, Int, Int) -> Unit = { _, _, _ -> },
    ): SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = onCreate(db)
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = onUpgrade(db, oldVersion, newVersion)
            })
            .build(),
    ).also { it.writableDatabase }
}
