package com.kinderman.sdo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CharacterRecord::class,
        OwnerRecord::class,
        CatalogEntryRecord::class,
        CampaignRecord::class,
        CampaignMemberRecord::class,
        CampaignInviteRecord::class,
        SessionOperationRecord::class,
        CampaignLibraryRecord::class,
        CampaignDeliveryRecord::class,
        CampaignAlertSettingsRecord::class,
    ],
    version = 17,
    exportSchema = false,
)
@TypeConverters(CharacterConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun ownerDao(): OwnerDao
    abstract fun catalogDao(): CatalogDao
    abstract fun campaignDao(): CampaignDao
    abstract fun operationsDao(): OperationsDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS owners (
                        uid TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        role TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN personalNotes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN protectionAdjustments TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS catalog_entries (
                        id TEXT NOT NULL PRIMARY KEY,
                        kind TEXT NOT NULL,
                        name TEXT NOT NULL,
                        groupName TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        cost TEXT NOT NULL,
                        action TEXT NOT NULL,
                        range TEXT NOT NULL,
                        duration TEXT NOT NULL,
                        source TEXT NOT NULL,
                        catalogVersion INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN creationCost TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN price INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN load INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN durability TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN region TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN raceAttribute TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN relatedAttribute TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN initialValue INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN prerequisites TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN mechanicalEffect TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN ruleReference TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN keywords TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN repeatable INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE characters SET campaignId = '' WHERE campaignId = 'default'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS campaigns (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        ownerId TEXT NOT NULL,
                        state TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        archivedAt INTEGER DEFAULT NULL,
                        allowPlayerCharacterCreation INTEGER NOT NULL,
                        dirty INTEGER NOT NULL,
                        lastSyncedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS campaign_members (
                        campaignId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        role TEXT NOT NULL,
                        state TEXT NOT NULL,
                        joinedAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        characterIds TEXT NOT NULL,
                        joinedByInviteId TEXT NOT NULL,
                        dirty INTEGER NOT NULL,
                        lastSyncedAt INTEGER NOT NULL,
                        PRIMARY KEY(campaignId, userId)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_campaign_members_userId ON campaign_members(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_campaign_members_campaignId ON campaign_members(campaignId)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS campaign_invites (
                        id TEXT NOT NULL PRIMARY KEY,
                        campaignId TEXT NOT NULL,
                        campaignName TEXT NOT NULL,
                        campaignDescription TEXT NOT NULL,
                        code TEXT NOT NULL,
                        createdBy TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        expiresAt INTEGER DEFAULT NULL,
                        revokedAt INTEGER DEFAULT NULL,
                        generation INTEGER NOT NULL,
                        dirty INTEGER NOT NULL,
                        lastSyncedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_campaign_invites_code ON campaign_invites(code)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_campaign_invites_campaignId ON campaign_invites(campaignId)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE characters SET campaignId = '' WHERE campaignId = 'default'")
                db.execSQL("UPDATE campaign_members SET role = 'HISTORIAN' WHERE role = 'MASTER'")
                db.execSQL("UPDATE owners SET role = 'USER'")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS session_operations (id TEXT NOT NULL PRIMARY KEY, idempotencyKey TEXT NOT NULL, campaignId TEXT NOT NULL, characterId TEXT NOT NULL, actorId TEXT NOT NULL, type TEXT NOT NULL, target TEXT NOT NULL, previousValue TEXT NOT NULL, newValue TEXT NOT NULL, amount INTEGER NOT NULL, reason TEXT NOT NULL, createdAt INTEGER NOT NULL, dirty INTEGER NOT NULL, lastSyncedAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_session_operations_idempotencyKey ON session_operations(idempotencyKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_operations_campaignId ON session_operations(campaignId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_operations_characterId ON session_operations(characterId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS campaign_library_entries (id TEXT NOT NULL PRIMARY KEY, campaignId TEXT NOT NULL, kind TEXT NOT NULL, name TEXT NOT NULL, summary TEXT NOT NULL, payload TEXT NOT NULL, catalogEntryId TEXT NOT NULL, knowledgeBonus INTEGER NOT NULL, archived INTEGER NOT NULL, version INTEGER NOT NULL, createdBy TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, dirty INTEGER NOT NULL, lastSyncedAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_campaign_library_entries_campaignId ON campaign_library_entries(campaignId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS campaign_deliveries (id TEXT NOT NULL PRIMARY KEY, campaignId TEXT NOT NULL, libraryEntryId TEXT NOT NULL, recipientId TEXT NOT NULL, recipientCharacterId TEXT NOT NULL, snapshotKind TEXT NOT NULL, snapshotName TEXT NOT NULL, snapshotSummary TEXT NOT NULL, snapshotPayload TEXT NOT NULL, knowledgeMapping TEXT NOT NULL, knowledgeBonus INTEGER NOT NULL, state TEXT NOT NULL, createdBy TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, dirty INTEGER NOT NULL, lastSyncedAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_campaign_deliveries_campaignId ON campaign_deliveries(campaignId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_campaign_deliveries_recipientId ON campaign_deliveries(recipientId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_campaign_deliveries_recipientCharacterId ON campaign_deliveries(recipientCharacterId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS campaign_alert_settings (campaignId TEXT NOT NULL PRIMARY KEY, lifeThresholdPercent INTEGER NOT NULL, sanityThresholdPercent INTEGER NOT NULL, exhaustionThresholdPercent INTEGER NOT NULL, staleAfterHours INTEGER NOT NULL, alertConditions INTEGER NOT NULL, alertBodyFailures INTEGER NOT NULL)")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("limit", "activationCondition", "enhancements", "deactivationCondition").forEach { column ->
                    db.execSQL("ALTER TABLE catalog_entries ADD COLUMN `$column` TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Campaign authority comes only from campaigns.ownerId. Legacy contextual roles
                // are normalized so the same account can be Mestre in one campaign and jogador in another.
                db.execSQL("UPDATE campaign_members SET role = 'PLAYER' WHERE role != 'PLAYER'")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN appliedDeliveryIds TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
