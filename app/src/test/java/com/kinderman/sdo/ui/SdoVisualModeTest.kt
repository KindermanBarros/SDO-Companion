package com.kinderman.sdo.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SdoVisualModeTest {
    @Test
    fun cybergrungeHasItsOwnIdentityAndPalette() {
        assertEquals("Interface Cybergrunge", SdoVisualMode.CYBERGRUNGE.label)
        assertFalse(SdoVisualMode.CYBERGRUNGE.allowsThemeSelection)
    }

    @Test
    fun standardInterfaceKeepsThemeSelection() {
        assertEquals("Interface padrão", SdoVisualMode.STANDARD.label)
        assertTrue(SdoVisualMode.STANDARD.allowsThemeSelection)
    }

    @Test
    fun cybergrungeIsOptInAndStandardRemainsDefault() {
        assertEquals(SdoVisualMode.STANDARD, SdoPreferences().visualMode)
        assertEquals(SdoThemeVariant.CYAN_INDUSTRIAL, SdoPreferences().theme)
        assertTrue(SdoVisualMode.entries.first() == SdoVisualMode.STANDARD)
    }
}
