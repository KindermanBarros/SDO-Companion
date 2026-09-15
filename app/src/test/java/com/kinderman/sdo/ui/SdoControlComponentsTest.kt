package com.kinderman.sdo.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SdoControlComponentsTest {
    @Test
    fun `responsive grid uses one column on narrow phones`() {
        assertEquals(1, responsiveColumnCount(widthDp = 120f, minimumItemWidthDp = 128f))
    }

    @Test
    fun `responsive grid grows on phones and tablets without overcompressing actions`() {
        assertEquals(2, responsiveColumnCount(widthDp = 360f, minimumItemWidthDp = 128f))
        assertEquals(4, responsiveColumnCount(widthDp = 900f, minimumItemWidthDp = 128f))
    }

    @Test
    fun `responsive grid reserves horizontal spacing before adding a column`() {
        assertEquals(1, responsiveColumnCount(widthDp = 264f, minimumItemWidthDp = 128f, spacingDp = 12f))
        assertEquals(2, responsiveColumnCount(widthDp = 268f, minimumItemWidthDp = 128f, spacingDp = 12f))
    }
}
