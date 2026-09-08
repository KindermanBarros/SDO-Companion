package com.kinderman.sdo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query(
        """
        SELECT DISTINCT ch.* FROM characters ch
        LEFT JOIN campaigns c ON c.id = ch.campaignId
        LEFT JOIN campaign_members m ON m.campaignId = ch.campaignId AND m.userId = :uid
        WHERE ch.deleted = 0
          AND (
            ch.ownerId = :uid
            OR (
              ch.campaignId != ''
              AND (c.ownerId = :uid OR m.state = 'ACTIVE')
            )
          )
        ORDER BY ch.updatedAt DESC
        """,
    )
    fun observe(uid: String): Flow<List<CharacterRecord>>

    @Query("SELECT * FROM characters WHERE deleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CharacterRecord>>

    @Query("SELECT * FROM characters WHERE id = :id AND deleted = 0")
    fun observeOne(id: String): Flow<CharacterRecord?>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CharacterRecord?

    @Query("SELECT * FROM characters WHERE dirty = 1")
    suspend fun dirty(): List<CharacterRecord>

    @Query(
        """
        SELECT DISTINCT ch.* FROM characters ch
        LEFT JOIN campaigns c ON c.id = ch.campaignId
        LEFT JOIN campaign_members m ON m.campaignId = ch.campaignId AND m.userId = :uid
        WHERE ch.ownerId = :uid
           OR (ch.campaignId != '' AND (c.ownerId = :uid OR m.state = 'ACTIVE'))
        """,
    )
    suspend fun visible(uid: String): List<CharacterRecord>

    @Query("SELECT * FROM characters")
    suspend fun all(): List<CharacterRecord>

    @Upsert suspend fun upsert(character: CharacterRecord)

    @Query("UPDATE characters SET dirty = 0, lastSyncedAt = :remoteUpdatedAt WHERE id = :id")
    suspend fun markSynced(id: String, remoteUpdatedAt: Long)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun purge(id: String)
}
