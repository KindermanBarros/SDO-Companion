package com.kinderman.sdo.domain.repository

import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignInvite
import com.kinderman.sdo.domain.model.CampaignInvitePreview
import com.kinderman.sdo.domain.model.CampaignMember
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface CampaignRepository {
    fun observe(session: UserSession): Flow<List<Campaign>>
    fun observeMembers(campaignId: String): Flow<List<CampaignMember>>
    fun observeMemberships(session: UserSession): Flow<List<CampaignMember>>
    fun observeInvites(campaignId: String): Flow<List<CampaignInvite>>

    suspend fun create(session: UserSession, name: String, description: String = ""): Campaign
    suspend fun update(session: UserSession, campaign: Campaign)
    suspend fun archive(session: UserSession, campaign: Campaign, archived: Boolean)
    suspend fun leave(session: UserSession, campaign: Campaign)
    suspend fun removeMember(session: UserSession, campaign: Campaign, userId: String)

    suspend fun createInvite(session: UserSession, campaign: Campaign): CampaignInvite
    suspend fun previewInvite(session: UserSession, code: String): CampaignInvitePreview?
    suspend fun joinByCode(session: UserSession, code: String): Campaign

    suspend fun linkCharacter(session: UserSession, character: Character, campaign: Campaign): Character
    suspend fun unlinkCharacter(session: UserSession, character: Character): Character
    suspend fun sync(session: UserSession)
}
