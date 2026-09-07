package com.kinderman.sdo.domain.repository

import android.app.Activity
import com.kinderman.sdo.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val configured: Boolean
    val session: Flow<UserSession?>
    suspend fun loginWithGoogle(activity: Activity): UserSession?
    fun logout()
}
