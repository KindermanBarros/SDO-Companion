package com.kinderman.sdo.domain.repository

import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface OwnerRepository {
    fun observe(): Flow<List<UserProfile>>
    suspend fun sync(session: UserSession)
}
