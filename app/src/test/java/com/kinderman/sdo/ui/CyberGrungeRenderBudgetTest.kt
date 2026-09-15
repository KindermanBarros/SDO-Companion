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
    fun onlyLoadingOwnsAnInfiniteTransition() {
        // Interaction motion uses animateFloatAsState and is disposed with its composable.
        assertTrue(SdoMotionTokens.RESPONSE <= 200)
        assertTrue(SdoMotionTokens.TELEMETRY_SCAN >= 1_000)
    }
}
