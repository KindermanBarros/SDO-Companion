package com.kinderman.sdo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OwnerDao {
    @Query("SELECT * FROM owners ORDER BY displayName COLLATE NOCASE, email COLLATE NOCASE")
    fun observe(): Flow<List<OwnerRecord>>

    @Query("SELECT uid FROM owners")
    suspend fun ids(): List<String>

    @Upsert
    suspend fun upsertAll(owners: List<OwnerRecord>)
}
