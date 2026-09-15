package com.kinderman.sdo.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SdoVisualModeTest {
    @Test
    fun cybergrungeIsExplicitlyExperimental() {
        assertTrue(SdoVisualMode.CYBERGRUNGE.label.contains("EXPERIMENTAL"))
        assertFalse(SdoVisualMode.STANDARD.label.contains("EXPERIMENTAL"))
    }
}
