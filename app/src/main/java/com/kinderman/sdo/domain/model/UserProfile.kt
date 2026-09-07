package com.kinderman.sdo.domain.model

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: UserRole,
) {
    val firstName: String
        get() = displayName.trim().substringBefore(' ').ifBlank {
            email.substringBefore('@').ifBlank { "Operador" }
        }
}
