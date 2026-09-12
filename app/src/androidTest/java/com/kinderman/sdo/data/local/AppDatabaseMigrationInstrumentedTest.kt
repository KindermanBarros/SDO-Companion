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
import com.kinderman.sdo.domain.model.DomainError
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

    @Test fun migration24To25AddsTypedCanonicalPayloadsIdempotently() {
        val name = "migration-24-25.db".also(databases::add)
        openSqlite(name, 24, onCreate = { db ->
            db.execSQL("CREATE TABLE characters (id TEXT NOT NULL PRIMARY KEY)")
            db.execSQL("INSERT INTO characters (id) VALUES ('fixture')")
        }).close()
        val migrated = openSqlite(name, 25, onUpgrade = { db, _, _ -> AppDatabase.MIGRATION_24_25.migrate(db) })
        AppDatabase.MIGRATION_24_25.migrate(migrated.writableDatabase)
        migrated.writableDatabase.query(
            "SELECT canonicalAbilitiesPayload, canonicalBodyPayload, canonicalConditionsPayload, activeModifiersPayload FROM characters WHERE id = 'fixture'",
        ).use { cursor ->
            cursor.moveToFirst()
            repeat(4) { assertEquals("", cursor.getString(it)) }
        }
        migrated.close()
    }

    @Test fun sessionContractMigrationsAddStructuredPayloadAndRevisionIdempotently() {
        val name = "migration-25-27.db".also(databases::add)
        openSqlite(name, 25, onCreate = { db ->
            db.execSQL("CREATE TABLE session_operations (id TEXT NOT NULL PRIMARY KEY)")
        }).close()
        val migrated = openSqlite(name, 27, onUpgrade = { db, oldVersion, _ ->
            assertEquals(25, oldVersion)
            AppDatabase.MIGRATION_25_26.migrate(db)
            AppDatabase.MIGRATION_26_27.migrate(db)
        })
        AppDatabase.MIGRATION_25_26.migrate(migrated.writableDatabase)
        AppDatabase.MIGRATION_26_27.migrate(migrated.writableDatabase)
        migrated.writableDatabase.query("PRAGMA table_info(session_operations)").use { cursor ->
            val nameColumn = cursor.getColumnIndex("name")
            val columns = buildSet { while (cursor.moveToNext()) add(cursor.getString(nameColumn)) }
            assertEquals(true, "canonicalPayload" in columns)
            assertEquals(true, "baseCharacterUpdatedAt" in columns)
        }
        migrated.close()
    }

    @Test fun migration27To28AddsRemainingCanonicalPayloadsIdempotently() {
        val name = "migration-27-28.db".also(databases::add)
        openSqlite(name, 27, onCreate = { db ->
            db.execSQL("CREATE TABLE characters (id TEXT NOT NULL PRIMARY KEY)")
            db.execSQL("INSERT INTO characters (id) VALUES ('fixture')")
        }).close()
        val migrated = openSqlite(name, 28, onUpgrade = { db, oldVersion, newVersion ->
            assertEquals(27, oldVersion)
            assertEquals(28, newVersion)
            AppDatabase.MIGRATION_27_28.migrate(db)
        })
        AppDatabase.MIGRATION_27_28.migrate(migrated.writableDatabase)
        migrated.writableDatabase.query(
            "SELECT canonicalItemsPayload, scopedItemCatalogPayload, canonicalProgressionPayload FROM characters WHERE id = 'fixture'",
        ).use { cursor ->
            cursor.moveToFirst()
            repeat(3) { assertEquals("", cursor.getString(it)) }
        }
        migrated.close()
    }

    @Test fun sessionOperationAndCharacterWriteAreAtomicAndRejectStaleRevision() = runBlocking {
        val name = "session-operation-atomicity.db".also(databases::add)
        val database = openRoom(name)
        val character = CharacterRecord(id = "character", updatedAt = 12)
        database.characterDao().upsert(character)

        val stale = SessionOperationRecord(
            id = "stale-operation", idempotencyKey = "stale-key", campaignId = "campaign",
            characterId = "character", baseCharacterUpdatedAt = 11,
        )
        val staleResult = runCatching { database.operationsDao().applyOnce(stale, character.copy(name = "Não deve gravar")) }
        assertTrue(staleResult.exceptionOrNull() is DomainError.RevisionConflict)
        assertEquals("Novo personagem", database.characterDao().get("character")!!.name)
        assertEquals(emptyList<SessionOperationRecord>(), database.operationsDao().observeAudit(setOf("campaign")).first())

        val current = stale.copy(id = "current-operation", idempotencyKey = "current-key", baseCharacterUpdatedAt = 12)
        assertEquals(true, database.operationsDao().applyOnce(current, character.copy(name = "Aplicado")))
        assertEquals("Aplicado", database.characterDao().get("character")!!.name)
        assertEquals(1, database.operationsDao().observeAudit(setOf("campaign")).first().size)
        database.close()
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
