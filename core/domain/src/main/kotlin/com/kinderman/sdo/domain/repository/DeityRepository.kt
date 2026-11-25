package com.kinderman.sdo.domain.repository

import com.kinderman.sdo.domain.model.DeityDetailView
import com.kinderman.sdo.domain.model.DeityPublicProfile
import com.kinderman.sdo.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface DeityRepository {
    fun observePublicDeities(): Flow<List<DeityPublicProfile>>

    /**
     * Loads a deity detail enforcing GM spoiler access at the data layer.
     * Unauthorized callers receive `gmContent = null` without querying or caching GM secrets.
     */
    suspend fun getDeityDetail(
        session: UserSession?,
        deityId: String,
        campaignId: String? = null,
    ): DeityDetailView?

    suspend fun clearUnauthorizedCaches(activeUid: String? = null)
}
