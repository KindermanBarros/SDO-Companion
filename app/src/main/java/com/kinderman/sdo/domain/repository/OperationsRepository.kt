package com.kinderman.sdo.domain.repository

import com.kinderman.sdo.domain.model.CampaignAlertSettings
import com.kinderman.sdo.domain.model.CampaignDelivery
import com.kinderman.sdo.domain.model.CampaignLibraryEntry
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperation
import com.kinderman.sdo.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface OperationsRepository {
    fun observeAudit(campaignIds: Set<String>): Flow<List<SessionOperation>>
    fun observeLibrary(campaignIds: Set<String>): Flow<List<CampaignLibraryEntry>>
    fun observeDeliveries(session: UserSession, campaignIds: Set<String>): Flow<List<CampaignDelivery>>
    fun observeAlertSettings(campaignIds: Set<String>): Flow<List<CampaignAlertSettings>>

    suspend fun apply(session: UserSession, character: Character, command: SessionCommand): Boolean
    suspend fun saveLibrary(session: UserSession, entry: CampaignLibraryEntry)
    suspend fun duplicateLibrary(session: UserSession, entry: CampaignLibraryEntry)
    suspend fun archiveLibrary(session: UserSession, entry: CampaignLibraryEntry, archived: Boolean)
    suspend fun deliver(session: UserSession, entry: CampaignLibraryEntry, recipients: List<Character>, knowledgeMappings: Map<String, String>)
    suspend fun respondToDelivery(session: UserSession, delivery: CampaignDelivery, accept: Boolean)
    suspend fun saveAlertSettings(settings: CampaignAlertSettings)
    suspend fun sync(session: UserSession, manageableCampaignIds: Set<String>)
}
