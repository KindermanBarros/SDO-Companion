package com.kinderman.sdo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.characterEditKeys
import com.kinderman.sdo.domain.model.applyCharacterEditKeys
import com.kinderman.sdo.domain.model.preserveEditsAfterUpload
import com.kinderman.sdo.domain.model.preservePendingCharacterEdits
import com.kinderman.sdo.domain.model.pendingCharacterEditKeys
import com.kinderman.sdo.domain.model.withPendingEditKeys
import com.kinderman.sdo.domain.model.withFieldEditTimes
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

    @Query("SELECT * FROM characters WHERE campaignId = :campaignId")
    suspend fun inCampaign(campaignId: String): List<CharacterRecord>

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

    @Transaction
    suspend fun saveEditorChange(candidate: Character) {
        val current = get(candidate.id)?.toDomain()
        val rebased = if (current != null && candidate.editingBase != null) {
            val normalized = candidate.toRecord().toDomain()
            val base = candidate.editingBase.toRecord().toDomain()
            val edited = characterEditKeys(base, normalized)
            applyCharacterEditKeys(current, normalized, edited).copy(
                updatedAt = candidate.updatedAt, lastEditedBy = candidate.lastEditedBy, dirty = true,
            )

        } else candidate
        upsertTracked(rebased.toRecord())
    }

    @Transaction
    suspend fun upsertTracked(character: CharacterRecord) {
        val current = get(character.id)
        if (!character.dirty && current?.dirty == true && current.deleted) return
        if (character.dirty) {
            upsert(character.toDomain().withFieldEditTimes(current?.toDomain()).toRecord())
        } else if (current?.dirty == true) {
            val local = current.toDomain()
            val remote = character.toDomain()
            val marked = local.withPendingEditKeys(pendingCharacterEditKeys(local, remote))
            upsert(preservePendingCharacterEdits(remote, marked).toRecord())
        } else {
            upsert(character)
        }
    }

    @Transaction
    suspend fun acceptSnapshot(snapshot: CharacterRecord, expectedUpdatedAt: Long, uploaded: CharacterRecord? = null) {
        val current = get(snapshot.id)
        if (current?.dirty == true && current.deleted) return
        val clean = snapshot.copy(dirty = false, lastSyncedAt = snapshot.updatedAt, deleted = false)
        if (current?.updatedAt == expectedUpdatedAt) upsert(clean)
        else if (current != null && uploaded != null) upsert(preserveEditsAfterUpload(clean.toDomain(), current.toDomain(), uploaded.toDomain()).toRecord())
        else upsertTracked(clean)
    }

    @Transaction
    suspend fun acknowledgeIfUnchanged(id: String, expectedUpdatedAt: Long, remoteUpdatedAt: Long) {
        val current = get(id)
        if (current?.updatedAt == expectedUpdatedAt && !current.deleted) markSynced(id, remoteUpdatedAt)
    }


    @Query("UPDATE characters SET dirty = 0, lastSyncedAt = :remoteUpdatedAt WHERE id = :id")
    suspend fun markSynced(id: String, remoteUpdatedAt: Long)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun purge(id: String)
}
