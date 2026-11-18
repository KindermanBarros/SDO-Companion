package com.kinderman.sdo.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinderman.sdo.data.repository.SyncedCharacterRepository
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
        var repository = SyncedCharacterRepository(database.characterDao(), database.ownerDao(), database.campaignDao())
        assertNotNull(repository.observeOne("character").first()!!.powers.singleOrNull { it.linkedItemId == "weapon" })
        assertEquals(CURRENT_ITEM_DATA_VERSION, database.characterDao().get("character")!!.itemSchemaVersion)
        database.close()

        database = openRoom(name)
        repository = SyncedCharacterRepository(database.characterDao(), database.ownerDao(), database.campaignDao())
        assertNotNull(repository.observeOne("character").first()!!.powers.singleOrNull { it.linkedItemId == "weapon" })
        assertEquals(CURRENT_ITEM_DATA_VERSION, database.characterDao().get("character")!!.itemSchemaVersion)
        database.close()
    }

    @Test fun migration29To30PreservesSheetsAndAddsEmptyFieldTimes() {
        val name = "migration-29-30.db".also(databases::add)
        openSqlite(name, 29, onCreate = { db ->
            db.execSQL("CREATE TABLE characters (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
            db.execSQL("INSERT INTO characters (id, name) VALUES ('sheet', 'Ayla')")
        }).close()
        val migrated = openSqlite(name, 30, onUpgrade = { db, _, _ -> AppDatabase.MIGRATION_29_30.migrate(db) })
        migrated.writableDatabase.query("SELECT name, fieldUpdatedAt FROM characters WHERE id = 'sheet'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Ayla", cursor.getString(0))
            assertEquals("{}", cursor.getString(1))
        }
        migrated.close()
    }

    @Test fun migration30To31PreservesSheetsAndAddsEmptyPhotoUri() {
        val name = "migration-30-31.db".also(databases::add)
        openSqlite(name, 30, onCreate = { db ->
            db.execSQL("CREATE TABLE characters (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
            db.execSQL("INSERT INTO characters (id, name) VALUES ('sheet', 'Ayla')")
        }).close()
        val migrated = openSqlite(name, 31, onUpgrade = { db, _, _ -> AppDatabase.MIGRATION_30_31.migrate(db) })
        migrated.writableDatabase.query("SELECT name, photoUri FROM characters WHERE id = 'sheet'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Ayla", cursor.getString(0))
            assertEquals("", cursor.getString(1))
        }
        migrated.close()
    }

    @Test fun migration31To32CreatesSheetSyncTablesWithoutTouchingCharacters() {
        val name = "migration-31-32.db".also(databases::add)
        openSqlite(name, 31, onCreate = { db ->
            db.execSQL("CREATE TABLE characters (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
            db.execSQL("INSERT INTO characters (id, name) VALUES ('sheet', 'Ayla')")
        }).close()
        val migrated = openSqlite(name, 32, onUpgrade = { db, _, _ -> AppDatabase.MIGRATION_31_32.migrate(db) })
        migrated.writableDatabase.query("SELECT name FROM characters WHERE id = 'sheet'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Ayla", cursor.getString(0))
        }
        listOf("sheet_routes", "sheet_outbox", "sheet_sequences").forEach { table ->
            migrated.writableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$table'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
        migrated.close()
    }

    @Test fun migration32To33CreatesSparkTablesAndPreservesOutboxAndRoutes() {
        val name = "migration-32-33.db".also(databases::add)
        openSqlite(name, 32, onCreate = { db ->
            AppDatabase.MIGRATION_31_32.migrate(db)
            db.execSQL(
                "INSERT INTO sheet_routes (accountId, sheetId, status, accessEpoch, revision, confirmedJson) " +
                    "VALUES ('acc-1', 'sheet-1', 'FIRESTORE_ACTIVE', 2, 7, '{\"id\":\"sheet-1\"}')",
            )
            db.execSQL(
                "INSERT INTO sheet_outbox (operationId, accountId, sheetId, deviceId, sequence, envelopeJson, optimisticJson, status, error) " +
                    "VALUES ('op-pending', 'acc-1', 'sheet-1', 'dev-1', 10, '{\"command\":{\"type\":\"PATCH\"}}', '{\"name\":\"Ayla\"}', 'PENDING', '')",
            )
            db.execSQL(
                "INSERT INTO sheet_outbox (operationId, accountId, sheetId, deviceId, sequence, envelopeJson, optimisticJson, status, error) " +
                    "VALUES ('op-session', 'acc-1', 'sheet-1', 'dev-1', 11, '{\"command\":{\"type\":\"SESSION\"}}', '', 'REJECTED', 'Quota')",
            )
            db.execSQL(
                "INSERT INTO sheet_outbox (operationId, accountId, sheetId, deviceId, sequence, envelopeJson, optimisticJson, status, error) " +
                    "VALUES ('op-done', 'acc-2', 'sheet-2', 'dev-2', 12, '{\"command\":{\"type\":\"PATCH\"}}', '{\"name\":\"B\"}', 'COMMITTED', '')",
            )
        }).close()

        val migrated = openSqlite(name, 33, onUpgrade = { db, oldVersion, newVersion ->
            assertEquals(32, oldVersion)
            assertEquals(33, newVersion)
            AppDatabase.MIGRATION_32_33.migrate(db)
        })
        AppDatabase.MIGRATION_32_33.migrate(migrated.writableDatabase)

        listOf("account_sheets", "sheet_intents", "sheet_conflicts").forEach { table ->
            migrated.writableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$table'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
        migrated.writableDatabase.query(
            "SELECT sheetRevision, confirmedJson FROM account_sheets WHERE accountId = 'acc-1' AND sheetId = 'sheet-1'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(7L, cursor.getLong(0))
            assertEquals("{\"id\":\"sheet-1\"}", cursor.getString(1))
        }
        migrated.writableDatabase.query(
            "SELECT operationId, type, status, baseValueJson, desiredPayloadJson, payloadHash, error FROM sheet_intents ORDER BY createdAt ASC",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("op-pending", cursor.getString(0))
            assertEquals("PATCH_FIELDS", cursor.getString(1))
            assertEquals("PENDING", cursor.getString(2))
            assertEquals("{\"command\":{\"type\":\"PATCH\"}}", cursor.getString(3))
            assertEquals("{\"name\":\"Ayla\"}", cursor.getString(4))
            assertEquals(64, cursor.getString(5).length)

            assertTrue(cursor.moveToNext())
            assertEquals("op-session", cursor.getString(0))
            assertEquals("SESSION_ACTION", cursor.getString(1))
            assertEquals("REJECTED", cursor.getString(2))
            assertEquals("{\"command\":{\"type\":\"SESSION\"}}", cursor.getString(3))
            assertEquals("{\"command\":{\"type\":\"SESSION\"}}", cursor.getString(4))
            assertEquals(64, cursor.getString(5).length)
            assertEquals("Quota", cursor.getString(6))

            assertTrue(cursor.moveToNext())
            assertEquals("op-done", cursor.getString(0))
            assertEquals("CONFIRMED", cursor.getString(2))
            assertEquals(64, cursor.getString(5).length)
        }
        migrated.close()
    }

    @Test fun sparkDaosEnforceStrictAccountIsolation() = runBlocking {
        val name = "spark-account-isolation.db".also(databases::add)
        val database = openRoom(name)
        database.accountSheetDao().upsert(AccountSheetRecord(accountId = "acc-1", sheetId = "shared", sheetRevision = 3L))
        database.accountSheetDao().upsert(AccountSheetRecord(accountId = "acc-2", sheetId = "shared", sheetRevision = 9L))
        database.sheetIntentDao().upsert(
            SheetIntentRecord(
                operationId = "op-1",
                accountId = "acc-1",
                sheetId = "shared",
                type = "PATCH_FIELDS",
                targetKey = "field:name",
                status = "PENDING",
            ),
        )
        database.sheetConflictDao().upsert(
            SheetConflictRecord(
                id = "conf-1",
                accountId = "acc-1",
                sheetId = "shared",
                operationId = "op-1",
                targetKey = "field:name",
                label = "Nome",
            ),
        )

        assertEquals(1, database.accountSheetDao().observeAll("acc-1").first().size)
        assertEquals(3L, database.accountSheetDao().get("acc-1", "shared")?.sheetRevision)
        assertEquals(9L, database.accountSheetDao().get("acc-2", "shared")?.sheetRevision)
        assertEquals(1, database.sheetIntentDao().listForSheet("acc-1", "shared").size)
        assertEquals(0, database.sheetIntentDao().listForSheet("acc-2", "shared").size)
        assertEquals(1, database.sheetConflictDao().observeOpen("acc-1").first().size)
        assertEquals(0, database.sheetConflictDao().observeOpen("acc-2").first().size)
        database.close()
    }

    private fun openRoom(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(
            AppDatabase.MIGRATION_21_22,
            AppDatabase.MIGRATION_30_31,
            AppDatabase.MIGRATION_31_32,
            AppDatabase.MIGRATION_32_33,
            AppDatabase.MIGRATION_33_34,
            AppDatabase.MIGRATION_34_35,
        )
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
