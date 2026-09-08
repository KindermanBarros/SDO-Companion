package com.kinderman.sdo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserRole

@Entity(tableName = "owners")
data class OwnerRecord(
    @PrimaryKey val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = UserRole.USER.name,
)

fun OwnerRecord.toDomain() = UserProfile(
    uid = uid,
    email = email,
    displayName = displayName,
    // Global PLAYER/MASTER values are legacy profile data, never authorization.
    role = UserRole.USER,
)
