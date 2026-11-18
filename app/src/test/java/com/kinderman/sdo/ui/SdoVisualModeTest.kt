package com.kinderman.sdo.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SdoVisualModeTest {
    @Test
    fun cybergrungeHasItsOwnIdentityAndSupportsThemes() {
        assertEquals("Interface Cybergrunge", SdoVisualMode.CYBERGRUNGE.label)
        assertTrue(SdoVisualMode.CYBERGRUNGE.allowsThemeSelection)
        assertTrue(SdoThemeVariant.CYBERGRUNGE_SIGNAL in SdoThemeVariant.entries)
        SdoThemeVariant.entries.forEach { variant ->
            val prefs = SdoPreferences(visualMode = SdoVisualMode.CYBERGRUNGE, theme = variant)
            assertEquals(SdoVisualMode.CYBERGRUNGE, prefs.visualMode)
            assertEquals(variant, prefs.theme)
        }
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
