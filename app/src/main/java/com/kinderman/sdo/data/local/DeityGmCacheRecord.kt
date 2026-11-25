package com.kinderman.sdo.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

/**
 * Isolated offline cache for GM-only deity content.
 *
 * Security invariants:
 * - Keyed by `(viewerUid, campaignScopeId, deityId)` so player queries and `catalog_entries` never touch it.
 * - Populated exclusively after `DeityAccessPolicy.canReadGmContent` succeeds for `(viewerUid, campaignScopeId)`.
 * - Purged on sign-out or when unauthorized sessions are cleared.
 */
@Entity(
    tableName = "deity_gm_cache",
    primaryKeys = ["viewerUid", "campaignScopeId", "deityId"],
)
data class DeityGmCacheRecord(
    val viewerUid: String,
    val campaignScopeId: String,
    val deityId: String,
    val payloadJson: String,
    val cachedAt: Long,
)

@Dao
interface DeityGmCacheDao {
    @Query(
        "SELECT * FROM deity_gm_cache WHERE viewerUid = :viewerUid AND campaignScopeId = :campaignScopeId AND deityId = :deityId LIMIT 1",
    )
    suspend fun find(viewerUid: String, campaignScopeId: String, deityId: String): DeityGmCacheRecord?

    @Upsert
    suspend fun upsert(record: DeityGmCacheRecord)

    @Query("DELETE FROM deity_gm_cache WHERE viewerUid != :activeUid")
    suspend fun deleteOtherViewers(activeUid: String)

    @Query("DELETE FROM deity_gm_cache")
    suspend fun deleteAll()
}
