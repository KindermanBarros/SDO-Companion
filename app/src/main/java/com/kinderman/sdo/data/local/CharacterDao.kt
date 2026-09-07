package com.kinderman.sdo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters WHERE deleted = 0 AND (ownerId = :uid OR :isMaster = 1) ORDER BY updatedAt DESC")
    fun observe(uid: String, isMaster: Boolean): Flow<List<CharacterRecord>>

    @Query("SELECT * FROM characters WHERE id = :id AND deleted = 0")
    fun observeOne(id: String): Flow<CharacterRecord?>

    @Query("SELECT * FROM characters WHERE dirty = 1")
    suspend fun dirty(): List<CharacterRecord>

    @Query("SELECT * FROM characters WHERE ownerId = :uid OR :isMaster = 1")
    suspend fun visible(uid: String, isMaster: Boolean): List<CharacterRecord>

    @Upsert suspend fun upsert(character: CharacterRecord)

    @Query("UPDATE characters SET dirty = 0, lastSyncedAt = :remoteUpdatedAt WHERE id = :id")
    suspend fun markSynced(id: String, remoteUpdatedAt: Long)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun purge(id: String)
}
