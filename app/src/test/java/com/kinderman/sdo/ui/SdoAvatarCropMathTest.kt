package com.kinderman.sdo.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SdoAvatarCropMathTest {
    @Test
    fun `landscape image centered at 1x crops middle square`() {
        val rect = computeSquareCropRect(
            bitmapWidth = 800,
            bitmapHeight = 400,
            viewportSizePx = 400f,
            zoom = 1f,
            offsetX = 0f,
            offsetY = 0f,
        )
        assertEquals(SquareCropRect(left = 200, top = 0, side = 400), rect)
    }

    @Test
    fun `portrait image panned beyond edges clamps to valid bounds`() {
        val topRect = computeSquareCropRect(
            bitmapWidth = 400,
            bitmapHeight = 800,
            viewportSizePx = 400f,
            zoom = 1f,
            offsetX = 0f,
            offsetY = 999f,
        )
        val bottomRect = computeSquareCropRect(
            bitmapWidth = 400,
            bitmapHeight = 800,
            viewportSizePx = 400f,
            zoom = 1f,
            offsetX = 0f,
            offsetY = -999f,
        )
        assertEquals(SquareCropRect(left = 0, top = 0, side = 400), topRect)
        assertEquals(SquareCropRect(left = 0, top = 400, side = 400), bottomRect)
    }

    @Test
    fun `zoom halves crop side and centers correctly`() {
        val rect = computeSquareCropRect(
            bitmapWidth = 800,
            bitmapHeight = 400,
            viewportSizePx = 400f,
            zoom = 2f,
            offsetX = 0f,
            offsetY = 0f,
        )
        assertEquals(SquareCropRect(left = 300, top = 100, side = 200), rect)
    }
}
