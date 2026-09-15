package com.kinderman.sdo.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SdoVisualModeTest {
    @Test
    fun kaltochIsExplicitlyExperimental() {
        assertTrue(SdoVisualMode.KALTOCH.label.contains("EXPERIMENTAL"))
        assertFalse(SdoVisualMode.STANDARD.label.contains("EXPERIMENTAL"))
    }
}
