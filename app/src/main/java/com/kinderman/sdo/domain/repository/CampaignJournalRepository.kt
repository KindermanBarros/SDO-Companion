package com.kinderman.sdo.domain.repository

import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignJournalDraft
import com.kinderman.sdo.domain.model.CampaignJournalEntry
import com.kinderman.sdo.domain.model.JournalSaveResult
import com.kinderman.sdo.domain.model.JournalSnapshot
import com.kinderman.sdo.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface CampaignJournalRepository {
    fun observe(session: UserSession, campaign: Campaign): Flow<JournalSnapshot>

    suspend fun save(
        session: UserSession,
        campaign: Campaign,
        draft: CampaignJournalDraft,
        expectedRevision: Long?,
    ): JournalSaveResult

    suspend fun delete(
        session: UserSession,
        campaign: Campaign,
        entry: CampaignJournalEntry,
        operationId: String,
    ): JournalSaveResult

    suspend fun verifyOperation(
        session: UserSession,
        campaignId: String,
        entryId: String,
        operationId: String,
    ): CampaignJournalEntry?

    fun clearAccountCache(uid: String)
}
