package com.kinderman.sdo.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CyberGrungeMotionTest {
    @Test
    fun edgeGlitchFollowsHorizontalMovement() {
        assertEquals(-1, cyberGrungeDirectionFor(-0.01f))
        assertEquals(1, cyberGrungeDirectionFor(0f))
        assertEquals(1, cyberGrungeDirectionFor(3.5f))
    }
}
