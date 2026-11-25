package com.kinderman.sdo.data.di

import android.content.Context
import androidx.room.Room
import com.kinderman.sdo.data.local.AppDatabase
import com.kinderman.sdo.data.local.CampaignDao
import com.kinderman.sdo.data.local.CatalogDao
import com.kinderman.sdo.data.local.CharacterDao
import com.kinderman.sdo.data.local.CharacterMigrationManager
import com.kinderman.sdo.data.local.DeityGmCacheDao
import com.kinderman.sdo.data.local.OperationsDao
import com.kinderman.sdo.data.local.OwnerDao
import com.kinderman.sdo.data.repository.SyncedCampaignRepository
import com.kinderman.sdo.data.repository.SyncedCharacterRepository
import com.kinderman.sdo.data.repository.SyncedOperationsRepository
import com.kinderman.sdo.data.sync.SparkFirestoreSyncEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "sdo.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20,
                AppDatabase.MIGRATION_20_21,
                AppDatabase.MIGRATION_21_22,
                AppDatabase.MIGRATION_22_23,
                AppDatabase.MIGRATION_23_24,
                AppDatabase.MIGRATION_24_25,
                AppDatabase.MIGRATION_25_26,
                AppDatabase.MIGRATION_26_27,
                AppDatabase.MIGRATION_27_28,
                AppDatabase.MIGRATION_28_29,
                AppDatabase.MIGRATION_29_30,
                AppDatabase.MIGRATION_30_31,
                AppDatabase.MIGRATION_31_32,
                AppDatabase.MIGRATION_32_33,
                AppDatabase.MIGRATION_33_34,
                AppDatabase.MIGRATION_34_35,
                AppDatabase.MIGRATION_35_36,
                AppDatabase.MIGRATION_36_37,
            )
            .build()

    @Provides fun provideCharacterDao(db: AppDatabase): CharacterDao = db.characterDao()
    @Provides fun provideOwnerDao(db: AppDatabase): OwnerDao = db.ownerDao()
    @Provides fun provideCatalogDao(db: AppDatabase): CatalogDao = db.catalogDao()
    @Provides fun provideCampaignDao(db: AppDatabase): CampaignDao = db.campaignDao()
    @Provides fun provideOperationsDao(db: AppDatabase): OperationsDao = db.operationsDao()
    @Provides fun provideDeityGmCacheDao(db: AppDatabase): DeityGmCacheDao = db.deityGmCacheDao()

    @Provides
    @Singleton
    fun provideCharacterMigrationManager(): CharacterMigrationManager = CharacterMigrationManager()

    @Provides
    @Singleton
    fun provideSparkFirestoreSyncEngine(
        @ApplicationContext context: Context,
        db: AppDatabase,
        @ApplicationScope scope: CoroutineScope,
    ): SparkFirestoreSyncEngine = SparkFirestoreSyncEngine(context, db, scope)

    @Provides
    @Singleton
    fun provideSyncedCharacterRepository(
        dao: CharacterDao,
        ownerDao: OwnerDao,
        campaignDao: CampaignDao,
        migrationManager: CharacterMigrationManager,
        syncEngine: SparkFirestoreSyncEngine,
    ): SyncedCharacterRepository = SyncedCharacterRepository(
        dao = dao,
        ownerDao = ownerDao,
        campaignDao = campaignDao,
        migrationManager = migrationManager,
        syncEngine = syncEngine,
    )

    @Provides
    @Singleton
    fun provideSyncedCampaignRepository(
        dao: CampaignDao,
        characterDao: CharacterDao,
        syncEngine: SparkFirestoreSyncEngine,
    ): SyncedCampaignRepository = SyncedCampaignRepository(
        dao = dao,
        characterDao = characterDao,
        syncEngine = syncEngine,
    )

    @Provides
    @Singleton
    fun provideSyncedOperationsRepository(
        dao: OperationsDao,
        campaignDao: CampaignDao,
        migrationManager: CharacterMigrationManager,
        syncEngine: SparkFirestoreSyncEngine,
    ): SyncedOperationsRepository = SyncedOperationsRepository(
        dao = dao,
        campaignDao = campaignDao,
        migrationManager = migrationManager,
        syncEngine = syncEngine,
    )
}
