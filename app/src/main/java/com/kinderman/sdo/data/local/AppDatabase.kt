package com.kinderman.sdo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.security.MessageDigest

@Database(
    entities = [
        CharacterRecord::class,
        SheetRouteRecord::class,
        SheetOutboxRecord::class,
        SheetSequenceRecord::class,
        AccountSheetRecord::class,
        SheetIntentRecord::class,
        SheetConflictRecord::class,
        OwnerRecord::class,
        CatalogEntryRecord::class,
        CampaignRecord::class,
        CampaignMemberRecord::class,
        CampaignInviteRecord::class,
        SessionOperationRecord::class,
        CampaignLibraryRecord::class,
        CampaignDeliveryRecord::class,
        CampaignAlertSettingsRecord::class,
        DeityGmCacheRecord::class,
    ],
    version = 37,
    exportSchema = true,
)
@TypeConverters(CharacterConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sheetSyncDao(): SheetSyncDao
    abstract fun accountSheetDao(): AccountSheetDao
    abstract fun sheetIntentDao(): SheetIntentDao
    abstract fun sheetConflictDao(): SheetConflictDao
    abstract fun characterDao(): CharacterDao
    abstract fun ownerDao(): OwnerDao
    abstract fun catalogDao(): CatalogDao
    abstract fun campaignDao(): CampaignDao
    abstract fun operationsDao(): OperationsDao
    abstract fun deityGmCacheDao(): DeityGmCacheDao

    companion object {
        private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean =
            db.query("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(tableName))
                .use { cursor -> cursor.moveToFirst() }

        val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = db.query("PRAGMA table_info(characters)").use { cursor ->
                    val nameColumn = cursor.getColumnIndex("name")
                    buildSet { while (cursor.moveToNext()) add(cursor.getString(nameColumn)) }
                }
                if ("divineAscensionPayload" !in columns) {
                    db.execSQL("ALTER TABLE characters ADD COLUMN divineAscensionPayload TEXT NOT NULL DEFAULT ''")
                }
                if ("divineRuntimePayload" !in columns) {
                    db.execSQL("ALTER TABLE characters ADD COLUMN divineRuntimePayload TEXT NOT NULL DEFAULT ''")
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS deity_gm_cache (
                        viewerUid TEXT NOT NULL,
                        campaignScopeId TEXT NOT NULL,
                        deityId TEXT NOT NULL,
                        payloadJson TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL,
                        PRIMARY KEY(viewerUid, campaignScopeId, deityId)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN unrecognizedFieldsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN familiars TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN transports TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS account_sheets (
                        accountId TEXT NOT NULL,
                        sheetId TEXT NOT NULL,
                        sheetRevision INTEGER NOT NULL,
                        fieldRevisionsJson TEXT NOT NULL,
                        confirmedJson TEXT NOT NULL,
                        localPhotoUri TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(accountId, sheetId)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sheet_intents (
                        operationId TEXT NOT NULL,
                        accountId TEXT NOT NULL,
                        sheetId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        targetKey TEXT NOT NULL,
                        dependsOnOperationId TEXT,
                        baseRevision INTEGER NOT NULL,
                        baseValueJson TEXT NOT NULL,
                        desiredPayloadJson TEXT NOT NULL,
                        payloadHash TEXT NOT NULL,
                        status TEXT NOT NULL,
                        error TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(operationId)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sheet_intents_accountId_sheetId_status ON sheet_intents (accountId, sheetId, status)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sheet_intents_accountId_status ON sheet_intents (accountId, status)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sheet_conflicts (
                        id TEXT NOT NULL,
                        accountId TEXT NOT NULL,
                        sheetId TEXT NOT NULL,
                        operationId TEXT NOT NULL,
                        targetKey TEXT NOT NULL,
                        label TEXT NOT NULL,
                        baseRevision INTEGER NOT NULL,
                        remoteRevision INTEGER NOT NULL,
                        baseSummary TEXT NOT NULL,
                        localSummary TEXT NOT NULL,
                        remoteSummary TEXT NOT NULL,
                        basePayloadJson TEXT NOT NULL,
                        localPayloadJson TEXT NOT NULL,
                        remotePayloadJson TEXT NOT NULL,
                        resolutionStatus TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sheet_conflicts_accountId_sheetId_resolutionStatus ON sheet_conflicts (accountId, sheetId, resolutionStatus)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sheet_conflicts_accountId_operationId ON sheet_conflicts (accountId, operationId)",
                )

                if (tableExists(db, "sheet_routes")) {
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO account_sheets (
                            accountId, sheetId, sheetRevision, fieldRevisionsJson, confirmedJson, localPhotoUri, updatedAt
                        )
                        SELECT accountId, sheetId, revision, '{}', confirmedJson, '', 0
                        FROM sheet_routes
                        """.trimIndent(),
                    )
                }

                if (tableExists(db, "sheet_outbox")) {
                    val digest = MessageDigest.getInstance("SHA-256")
                    db.query(
                        "SELECT operationId, accountId, sheetId, sequence, envelopeJson, optimisticJson, status, error FROM sheet_outbox",
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            val operationId = cursor.getString(0)
                            val accountId = cursor.getString(1)
                            val sheetId = cursor.getString(2)
                            val sequence = cursor.getLong(3)
                            val envelopeJson = cursor.getString(4)
                            val optimisticJson = cursor.getString(5)
                            val rawStatus = cursor.getString(6)
                            val error = cursor.getString(7)

                            val type = if (envelopeJson.contains("\"SESSION\"")) {
                                "SESSION_ACTION"
                            } else {
                                "PATCH_FIELDS"
                            }
                            val mappedStatus = when (rawStatus) {
                                "COMMITTED" -> "CONFIRMED"
                                "REJECTED" -> "REJECTED"
                                "SENDING" -> "SENDING"
                                "DRAFT" -> "DRAFT"
                                "CONFLICT" -> "CONFLICT"
                                "ACCESS_DENIED" -> "ACCESS_DENIED"
                                "PAUSED_DEPENDENCY" -> "PAUSED_DEPENDENCY"
                                else -> "PENDING"
                            }
                            val desiredPayload = optimisticJson.ifBlank { envelopeJson }
                            val hashInput = "$operationId|$sheetId|$type|$envelopeJson|$optimisticJson"
                            val payloadHash = digest.digest(hashInput.toByteArray(Charsets.UTF_8))
                                .joinToString("") { byte -> "%02x".format(byte) }

                            db.execSQL(
                                """
                                INSERT OR IGNORE INTO sheet_intents (
                                    operationId, accountId, sheetId, type, targetKey, dependsOnOperationId,
                                    baseRevision, baseValueJson, desiredPayloadJson, payloadHash, status, error, createdAt, updatedAt
                                ) VALUES (?, ?, ?, ?, 'sheet', NULL, 0, ?, ?, ?, ?, ?, ?, ?)
                                """.trimIndent(),
                                arrayOf<Any>(
                                    operationId,
                                    accountId,
                                    sheetId,
                                    type,
                                    envelopeJson,
                                    desiredPayload,
                                    payloadHash,
                                    mappedStatus,
                                    error,
                                    sequence,
                                    sequence,
                                ),
                            )
                        }
                    }
                }
            }
        }

        val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS sheet_routes (accountId TEXT NOT NULL, sheetId TEXT NOT NULL, status TEXT NOT NULL, accessEpoch INTEGER NOT NULL, revision INTEGER NOT NULL, confirmedJson TEXT NOT NULL, PRIMARY KEY(accountId, sheetId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS sheet_outbox (operationId TEXT NOT NULL PRIMARY KEY, accountId TEXT NOT NULL, sheetId TEXT NOT NULL, deviceId TEXT NOT NULL, sequence INTEGER NOT NULL, envelopeJson TEXT NOT NULL, optimisticJson TEXT NOT NULL, status TEXT NOT NULL, error TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS sheet_sequences (accountId TEXT NOT NULL, sheetId TEXT NOT NULL, deviceId TEXT NOT NULL, nextSequence INTEGER NOT NULL, PRIMARY KEY(accountId, sheetId))")
            }
        }
        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN fieldUpdatedAt TEXT NOT NULL DEFAULT '{}'")
            }
        }

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

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN abilitySource TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN sourceKnowledge TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN sourceLevel INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN abilityCostType TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN abilityCostValue INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN abilityExecution TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN executionValue INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN executionUnit TEXT NOT NULL DEFAULT 'MINUTES'")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN abilityRange TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN targetArea TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN abilityDuration TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN durationValue INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN durationUnit TEXT NOT NULL DEFAULT 'HOURS'")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN abilityResistance TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN catalogAshSource TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN catalogAshPurity TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE catalog_entries ADD COLUMN runePackage TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN progressionLifeBonus INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN progressionSanityBonus INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN progressionArcaneBonus INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN progressionEnergyBonus INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN progressionHistory TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN creationStatus TEXT NOT NULL DEFAULT 'COMPLETED'")
                db.execSQL("ALTER TABLE characters ADD COLUMN creationStep INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE characters ADD COLUMN creationCompletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE characters ADD COLUMN creationRulesVersion INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN itemCreationDraft TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // The marker is intentionally zero. The repository performs the content-aware,
                // idempotent conversion with the canonical catalogs on first read.
                db.execSQL("ALTER TABLE characters ADD COLUMN itemSchemaVersion INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Forces the content-aware converter to rebuild typed durability, quality,
                // inventory states and canonical container capacity on the first read.
                db.execSQL("UPDATE characters SET itemSchemaVersion = 0")
            }
        }

        /**
         * Requirement: persist the canonical contract version and human-review queue.
         * Legacy handling: version zero triggers the pure, idempotent content migration.
         * Validation: repositories only mark the current version after conversion.
         * Rollback: the two additive columns can be ignored safely by version 23 code.
         */
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN canonicalSchemaVersion INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN migrationReviewPayload TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Requirement: persist canonical ability, body, condition and modifier state.
         * Legacy handling: empty payloads leave existing legacy columns available to the idempotent converter.
         * Validation: the content migration writes schema 2 only after typed payload serialization succeeds.
         * Rollback: the additive payload columns are ignored by version 24 clients.
         */
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val existingColumns = db.query("PRAGMA table_info(characters)").use { cursor ->
                    val nameColumn = cursor.getColumnIndex("name")
                    buildSet {
                        while (cursor.moveToNext()) add(cursor.getString(nameColumn))
                    }
                }
                listOf(
                    "canonicalAbilitiesPayload" to "TEXT NOT NULL DEFAULT ''",
                    "canonicalBodyPayload" to "TEXT NOT NULL DEFAULT ''",
                    "canonicalConditionsPayload" to "TEXT NOT NULL DEFAULT ''",
                    "activeModifiersPayload" to "TEXT NOT NULL DEFAULT ''",
                ).filterNot { (column, _) -> column in existingColumns }
                    .forEach { (column, declaration) ->
                        db.execSQL("ALTER TABLE characters ADD COLUMN $column $declaration")
                    }
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = db.query("PRAGMA table_info(session_operations)").use { cursor ->
                    val nameColumn = cursor.getColumnIndex("name")
                    buildSet {
                        while (cursor.moveToNext()) add(cursor.getString(nameColumn))
                    }
                }
                if ("canonicalPayload" !in columns) {
                    db.execSQL("ALTER TABLE session_operations ADD COLUMN canonicalPayload TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = db.query("PRAGMA table_info(session_operations)").use { cursor ->
                    val nameColumn = cursor.getColumnIndex("name")
                    buildSet {
                        while (cursor.moveToNext()) add(cursor.getString(nameColumn))
                    }
                }
                if ("baseCharacterUpdatedAt" !in columns) {
                    db.execSQL("ALTER TABLE session_operations ADD COLUMN baseCharacterUpdatedAt INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        /** Adds the remaining canonical authorities without deleting compatibility projections. */
        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = db.query("PRAGMA table_info(characters)").use { cursor ->
                    val nameColumn = cursor.getColumnIndex("name")
                    buildSet { while (cursor.moveToNext()) add(cursor.getString(nameColumn)) }
                }
                listOf("canonicalItemsPayload", "scopedItemCatalogPayload", "canonicalProgressionPayload")
                    .filterNot(columns::contains)
                    .forEach { column -> db.execSQL("ALTER TABLE characters ADD COLUMN $column TEXT NOT NULL DEFAULT ''") }
            }
        }

        /** Tracks the last editor account on each character for same-account conflict convergence. */
        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = db.query("PRAGMA table_info(characters)").use { cursor ->
                    val nameColumn = cursor.getColumnIndex("name")
                    buildSet { while (cursor.moveToNext()) add(cursor.getString(nameColumn)) }
                }
                if ("lastEditedBy" !in columns) {
                    db.execSQL("ALTER TABLE characters ADD COLUMN lastEditedBy TEXT NOT NULL DEFAULT ''")
                }
            }
        }
    }
}
