package com.kinderman.sdo.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class CyberGrungeRenderBudgetTest {
    @Test
    fun experimentalCorruptionStaysInsideEmergencyCeiling() {
        assertTrue(CyberGrungeTokens.BACKDROP_BLOCKS <= 64)
        assertTrue(CyberGrungeTokens.PANEL_GLITCH_BLOCKS <= 32)
        assertTrue(CyberGrungeTokens.EMPTY_SIGNAL_BLOCKS <= 32)
    }

    @Test
    fun compositionBoundMotionStaysInsideTimingBudget() {
        // Every continuous corruption loop is composition-bound and disposed with its visual mode.
        assertTrue(SdoMotionTokens.RESPONSE <= 200)
        assertTrue(SdoMotionTokens.TELEMETRY_SCAN >= 1_000)
    }
}
