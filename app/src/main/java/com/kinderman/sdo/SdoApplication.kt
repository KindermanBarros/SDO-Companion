package com.kinderman.sdo

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.kinderman.sdo.data.di.ApplicationScope
import com.kinderman.sdo.data.local.AppDatabase
import com.kinderman.sdo.data.local.CharacterMigrationManager
import com.kinderman.sdo.data.sync.SparkFirestoreSyncEngine
import com.kinderman.sdo.domain.repository.AuthRepository
import com.kinderman.sdo.domain.repository.CampaignJournalRepository
import com.kinderman.sdo.domain.repository.CampaignRepository
import com.kinderman.sdo.domain.repository.CatalogRepository
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.repository.ConnectivityMonitor
import com.kinderman.sdo.domain.repository.OperationsRepository
import com.kinderman.sdo.domain.repository.OwnerRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class SdoApplication : Application() {
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope
    @Inject lateinit var db: AppDatabase
    @Inject lateinit var characterMigrationManager: CharacterMigrationManager
    @Inject lateinit var sparkSyncEngine: SparkFirestoreSyncEngine
    @Inject lateinit var characterRepository: CharacterRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var ownerRepository: OwnerRepository
    @Inject lateinit var catalogRepository: CatalogRepository
    @Inject lateinit var campaignRepository: CampaignRepository
    @Inject lateinit var campaignJournalRepository: CampaignJournalRepository
    @Inject lateinit var operationsRepository: OperationsRepository
    @Inject lateinit var connectivityMonitor: ConnectivityMonitor

    override fun onCreate() {
        runCatching { FirebaseApp.initializeApp(this) }
            .onFailure { Log.e("SDO_FIREBASE", "Firebase initialization failed", it) }
        super.onCreate()
        applicationScope.launch {
            characterMigrationManager.migrateStoredCharacters(db.characterDao())
            catalogRepository.refreshBundledCatalog()
        }
    }
}
