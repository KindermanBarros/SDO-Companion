package com.kinderman.sdo.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVisibilityTest {
    @Test
    fun rotationDoesNotBecomeAnotherForegroundOrBackgroundTrip() {
        val visibility = AppVisibility()
        assertTrue(visibility.started())
        assertFalse(visibility.stopped(changingConfiguration = true))
        assertFalse(visibility.started())
    }

    @Test
    fun genuineBackgroundAndReturnRemainObservable() {
        val visibility = AppVisibility()
        assertTrue(visibility.started())
        assertTrue(visibility.stopped(changingConfiguration = false))
        assertFalse(visibility.stopped(changingConfiguration = false))
        assertTrue(visibility.started())
    }
}
