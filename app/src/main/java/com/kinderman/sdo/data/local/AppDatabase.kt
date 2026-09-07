package com.kinderman.sdo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CharacterRecord::class], version = 3, exportSchema = false)
@TypeConverters(CharacterConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN height TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN sex TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN size TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN learnedKnowledges TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN arcaneKnowledges TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN battleTechniques TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN pathKeywords TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN pathPillars TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN containerCapacity INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN bodyRegions TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN agilityLimit TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN organs TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN mysticAbilities TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN conditions TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN lockedBy TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN lockedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE characters ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN lockType TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("UPDATE characters SET lockType = 'HISTORIAN' WHERE isLocked = 1")
            }
        }
    }
}
