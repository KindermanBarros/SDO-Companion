package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.data.local.OwnerDao
import com.kinderman.sdo.data.local.OwnerRecord
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.repository.OwnerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OfflineFirstOwnerRepository(
    private val dao: OwnerDao,
) : OwnerRepository {
    override fun observe(): Flow<List<UserProfile>> =
        dao.observe().map { records -> records.map(OwnerRecord::toDomain) }

    override suspend fun sync(session: UserSession) {
        if (!session.isAdmin) return
        val store = runCatching { Firebase.firestore }.getOrNull() ?: return
        val owners = store.collection("users").get().await().documents.map { document ->
            OwnerRecord(
                uid = document.id,
                email = document.getString("email").orEmpty(),
                displayName = document.getString("displayName").orEmpty(),
                role = document.getString("role").orEmpty(),
            )
        }
        dao.upsertAll(owners)
    }
}
