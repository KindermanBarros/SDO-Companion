package com.kinderman.sdo.data.auth

import com.kinderman.sdo.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test

class AdministratorAccountTest {
    @Test
    fun `configured verified identity is administrator`() {
        assertEquals(
            UserRole.ADMIN,
            AdministratorAccount.roleFor("kindbarros@gmail.com", emailVerified = true),
        )
    }

    @Test
    fun `same unverified email is a generic account`() {
        assertEquals(
            UserRole.USER,
            AdministratorAccount.roleFor("kindbarros@gmail.com", emailVerified = false),
        )
    }

    @Test
    fun `other identities are generic accounts`() {
        assertEquals(
            UserRole.USER,
            AdministratorAccount.roleFor("other@example.com", emailVerified = true),
        )
    }
}
