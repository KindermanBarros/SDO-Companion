package com.kinderman.sdo

import android.app.Application
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.kinderman.sdo.data.auth.FirebaseAuthRepository
import com.kinderman.sdo.data.local.AppDatabase
import com.kinderman.sdo.data.repository.OfflineFirstCharacterRepository
import com.kinderman.sdo.data.repository.OfflineFirstOwnerRepository
import com.kinderman.sdo.domain.repository.AuthRepository
import com.kinderman.sdo.domain.repository.CharacterRepository
import com.kinderman.sdo.domain.repository.OwnerRepository

class SdoApplication : Application() {
    lateinit var characterRepository: CharacterRepository
    lateinit var authRepository: AuthRepository
    lateinit var ownerRepository: OwnerRepository

    override fun onCreate() {
        super.onCreate()
        runCatching { FirebaseApp.initializeApp(this) }
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "sdo.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
            )
            .build()
        characterRepository = OfflineFirstCharacterRepository(db.characterDao(), db.ownerDao())
        ownerRepository = OfflineFirstOwnerRepository(db.ownerDao())
        authRepository = FirebaseAuthRepository()
    }
}
