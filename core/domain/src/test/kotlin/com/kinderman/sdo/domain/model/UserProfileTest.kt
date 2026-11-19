package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileTest {
    @Test
    fun firstNameUsesGoogleDisplayName() {
        val profile = UserProfile("uid", "ana@example.com", "Ana Maria", UserRole.USER)

        assertEquals("Ana", profile.firstName)
    }

    @Test
    fun firstNameFallsBackToEmail() {
        val profile = UserProfile("uid", "operador@example.com", "", UserRole.USER)

        assertEquals("operador", profile.firstName)
    }
}
