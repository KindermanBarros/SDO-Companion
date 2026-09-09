package com.kinderman.sdo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query(
        """
        SELECT DISTINCT c.* FROM campaigns c
        LEFT JOIN campaign_members m ON m.campaignId = c.id
        WHERE c.ownerId = :uid OR (m.userId = :uid AND m.state = 'ACTIVE')
        ORDER BY c.updatedAt DESC
        """,
    )
    fun observeForUser(uid: String): Flow<List<CampaignRecord>>

    @Query("SELECT * FROM campaigns ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CampaignRecord>>

    @Query("SELECT * FROM campaign_members WHERE campaignId = :campaignId ORDER BY joinedAt ASC")
    fun observeMembers(campaignId: String): Flow<List<CampaignMemberRecord>>

    @Query("SELECT * FROM campaign_members WHERE userId = :userId AND state = 'ACTIVE'")
    fun observeMemberships(userId: String): Flow<List<CampaignMemberRecord>>

    @Query("SELECT * FROM campaign_invites WHERE campaignId = :campaignId ORDER BY createdAt DESC")
    fun observeInvites(campaignId: String): Flow<List<CampaignInviteRecord>>

    @Query("SELECT * FROM campaign_invites ORDER BY createdAt ASC")
    fun observeAllInvites(): Flow<List<CampaignInviteRecord>>

    @Query("SELECT * FROM campaigns WHERE id = :id LIMIT 1")
    suspend fun campaign(id: String): CampaignRecord?

    @Query("SELECT * FROM campaign_members WHERE campaignId = :campaignId AND userId = :userId LIMIT 1")
    suspend fun member(campaignId: String, userId: String): CampaignMemberRecord?

    @Query("SELECT * FROM campaign_invites WHERE code = :code LIMIT 1")
    suspend fun inviteByCode(code: String): CampaignInviteRecord?

    @Query("SELECT * FROM campaign_invites WHERE campaignId = :campaignId ORDER BY createdAt ASC LIMIT 1")
    suspend fun canonicalInvite(campaignId: String): CampaignInviteRecord?

    @Query("SELECT * FROM campaigns WHERE dirty = 1")
    suspend fun dirtyCampaigns(): List<CampaignRecord>

    @Query("SELECT * FROM campaign_members WHERE dirty = 1")
    suspend fun dirtyMembers(): List<CampaignMemberRecord>

    @Query("SELECT * FROM campaign_invites WHERE dirty = 1")
    suspend fun dirtyInvites(): List<CampaignInviteRecord>

    @Query("SELECT * FROM campaigns")
    suspend fun allCampaigns(): List<CampaignRecord>

    @Query("SELECT * FROM campaign_members")
    suspend fun allMembers(): List<CampaignMemberRecord>

    @Query("SELECT * FROM campaigns WHERE ownerId = :userId")
    suspend fun ownedCampaigns(userId: String): List<CampaignRecord>

    @Query("SELECT * FROM campaign_invites")
    suspend fun allInvites(): List<CampaignInviteRecord>

    @Upsert suspend fun upsertCampaign(value: CampaignRecord)
    @Upsert suspend fun upsertMember(value: CampaignMemberRecord)
    @Upsert suspend fun upsertInvite(value: CampaignInviteRecord)

    @Query("UPDATE campaigns SET dirty = 0, lastSyncedAt = :updatedAt WHERE id = :id")
    suspend fun markCampaignSynced(id: String, updatedAt: Long)

    @Query("UPDATE campaign_members SET dirty = 0, lastSyncedAt = :updatedAt WHERE campaignId = :campaignId AND userId = :userId")
    suspend fun markMemberSynced(campaignId: String, userId: String, updatedAt: Long)

    @Query("UPDATE campaign_invites SET dirty = 0, lastSyncedAt = :updatedAt WHERE id = :id")
    suspend fun markInviteSynced(id: String, updatedAt: Long)

    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun purgeCampaign(id: String)

    @Query("DELETE FROM campaign_members WHERE campaignId = :campaignId AND userId = :userId")
    suspend fun purgeMember(campaignId: String, userId: String)

    @Query("DELETE FROM campaign_members WHERE campaignId = :campaignId")
    suspend fun purgeMembersForCampaign(campaignId: String)

    @Query("DELETE FROM campaign_invites WHERE id = :id")
    suspend fun purgeInvite(id: String)

    @Query("DELETE FROM campaign_invites WHERE campaignId = :campaignId")
    suspend fun purgeInvitesForCampaign(campaignId: String)
}
