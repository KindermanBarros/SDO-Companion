package com.kinderman.sdo.domain.repository

import com.kinderman.sdo.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val configured: Boolean
    val session: Flow<UserSession?>
    suspend fun loginWithGoogleIdToken(idToken: String): UserSession?
    suspend fun loginWithEmailPassword(email: String, password: String): UserSession? = null
    fun logout()
}
