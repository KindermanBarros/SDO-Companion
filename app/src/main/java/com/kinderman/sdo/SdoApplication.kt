package com.kinderman.sdo

import android.app.Application
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.kinderman.sdo.data.auth.FirebaseAuthRepository
import com.kinderman.sdo.data.local.AppDatabase
import com.kinderman.sdo.data.repository.OfflineFirstCharacterRepository
import com.kinderman.sdo.data.repository.OfflineFirstOwnerRepository
import com.kinderman.sdo.data.repository.LocalCatalogRepository
import com.kinderman.sdo.domain.repository.AuthRepository
import com.kinderman.sdo.domain.repository.CatalogRepository
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.repository.OwnerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SdoApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var characterRepository: CharacterRepository
    lateinit var authRepository: AuthRepository
    lateinit var ownerRepository: OwnerRepository
    lateinit var catalogRepository: CatalogRepository

    override fun onCreate() {
        super.onCreate()
        runCatching { FirebaseApp.initializeApp(this) }
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "sdo.db")
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
            )
            .build()
        characterRepository = OfflineFirstCharacterRepository(db.characterDao(), db.ownerDao())
        ownerRepository = OfflineFirstOwnerRepository(db.ownerDao())
        catalogRepository = LocalCatalogRepository(db.catalogDao())
        authRepository = FirebaseAuthRepository()
        applicationScope.launch {
            catalogRepository.refreshBundledCatalog()
        }
    }
}
