package com.kinderman.sdo

import android.app.Application
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.kinderman.sdo.data.AppDatabase
import com.kinderman.sdo.data.CharacterRepository

class SdoApplication : Application() {
    lateinit var repository: CharacterRepository
    override fun onCreate() {
        super.onCreate()
        runCatching { FirebaseApp.initializeApp(this) }
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "sdo.db").fallbackToDestructiveMigration().build()
        repository = CharacterRepository(db.characterDao())
    }
}
