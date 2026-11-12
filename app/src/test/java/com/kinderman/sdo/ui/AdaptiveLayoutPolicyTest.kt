package com.kinderman.sdo.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutPolicyTest {
    @Test
    fun widthClassesChangeAtTabletAndExpandedBoundaries() {
        assertEquals(SdoWindowClass.COMPACT, sdoWindowClass(599f))
        assertEquals(SdoWindowClass.MEDIUM, sdoWindowClass(600f))
        assertEquals(SdoWindowClass.MEDIUM, sdoWindowClass(839f))
        assertEquals(SdoWindowClass.EXPANDED, sdoWindowClass(840f))
    }

    @Test
    fun shortLandscapeWindowsKeepReachableBottomNavigation() {
        assertFalse(sdoUsesNavigationRail(599f, 900f))
        assertFalse(sdoUsesNavigationRail(840f, 479f))
        assertTrue(sdoUsesNavigationRail(600f, 480f))
        assertTrue(sdoUsesNavigationRail(1200f, 900f))
    }

    @Test
    fun enlargedTextReservesEnoughRoomInsteadOfCompressingActions() {
        assertEquals(2, responsiveColumnCount(360f, 132f, fontScale = 1f))
        assertEquals(1, responsiveColumnCount(360f, 132f, fontScale = 2f))
        assertEquals(2, responsiveColumnCount(600f, 132f, fontScale = 2f))
        assertEquals(3, responsiveColumnCount(840f, 132f, fontScale = 2f))
    }

    @Test
    fun scalingNeverAddsColumnsAndNarrowWindowsNeverLoseTheirOnlyColumn() {
        for (width in listOf(0f, 120f, 320f, 360f, 600f, 840f, 1200f)) {
            val normal = responsiveColumnCount(width, 132f)
            val large = responsiveColumnCount(width, 132f, fontScale = 1.3f)
            val largest = responsiveColumnCount(width, 132f, fontScale = 2f)
            assertTrue("At $width dp", normal in 1..4)
            assertTrue("At $width dp", large in 1..normal)
            assertTrue("At $width dp", largest in 1..large)
        }
    }
}
