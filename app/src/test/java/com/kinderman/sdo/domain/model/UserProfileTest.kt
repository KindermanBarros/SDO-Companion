package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileTest {
    @Test
    fun firstNameUsesGoogleDisplayName() {
        val profile = UserProfile("uid", "ana@example.com", "Ana Maria", UserRole.PLAYER)

        assertEquals("Ana", profile.firstName)
    }

    @Test
    fun firstNameFallsBackToEmail() {
        val profile = UserProfile("uid", "operador@example.com", "", UserRole.PLAYER)

        assertEquals("operador", profile.firstName)
    }
}
