package com.kinderman.sdo.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class SdoControlComponentsTest {
    @Test
    fun `responsive grid uses one column on narrow phones`() {
        assertEquals(1, responsiveColumnCount(widthDp = 120, minimumItemWidthDp = 128))
    }

    @Test
    fun `responsive grid grows on phones and tablets without overcompressing actions`() {
        assertEquals(2, responsiveColumnCount(widthDp = 360, minimumItemWidthDp = 128))
        assertEquals(4, responsiveColumnCount(widthDp = 900, minimumItemWidthDp = 128))
    }
}
