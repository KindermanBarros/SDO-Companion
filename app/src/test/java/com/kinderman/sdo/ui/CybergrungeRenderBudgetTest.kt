package com.kinderman.sdo.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class CybergrungeRenderBudgetTest {
    @Test
    fun experimentalCorruptionStaysInsideEmergencyCeiling() {
        assertTrue(CyberGrungeTokens.BACKDROP_BLOCKS <= 64)
        assertTrue(CyberGrungeTokens.PANEL_GLITCH_BLOCKS <= 32)
        assertTrue(CyberGrungeTokens.PANEL_EDGE_GLITCH_LINES <= 16)
        assertTrue(CyberGrungeTokens.EMPTY_SIGNAL_BLOCKS <= 32)
    }

    @Test
    fun compositionBoundMotionStaysInsideTimingBudget() {
        // Only explicit loading may loop; interaction feedback is finite and backgrounds are static.
        assertTrue(SdoMotionTokens.RESPONSE <= 200)
        assertTrue(SdoMotionTokens.TELEMETRY_SCAN >= 1_000)
    }
}
