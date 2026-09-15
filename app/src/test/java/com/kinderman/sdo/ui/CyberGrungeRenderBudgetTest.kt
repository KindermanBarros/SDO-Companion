package com.kinderman.sdo.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class CyberGrungeRenderBudgetTest {
    @Test
    fun staticLayersStayInsideMobileRenderBudget() {
        assertTrue(CyberGrungeTokens.BACKDROP_BLOCKS <= 24)
        assertTrue(CyberGrungeTokens.PANEL_GLITCH_BLOCKS <= 10)
        assertTrue(CyberGrungeTokens.EMPTY_SIGNAL_BLOCKS <= 16)
    }

    @Test
    fun compositionBoundMotionStaysInsideTimingBudget() {
        // Press motion is finite; latent and missing-signal loops are disposed with their composables.
        assertTrue(SdoMotionTokens.RESPONSE <= 200)
        assertTrue(SdoMotionTokens.TELEMETRY_SCAN >= 1_000)
    }
}
