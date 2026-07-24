package com.appswithlove.updraft.ui.feedback

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageCodecTest {

    @Test
    fun mapToBitmapSpace_letterboxedLandscape_mapsPointAccountingForVerticalOffset() {
        // 200x100 bitmap (2:1) fit into a 100x100 square canvas: scale = 0.5, vertical bars top/bottom.
        val canvasSize = IntSize(100, 100)
        val bitmapWidth = 200
        val bitmapHeight = 100

        // Top-left corner of the scaled (letterboxed) image.
        val topLeft = mapToBitmapSpace(Offset(0f, 25f), canvasSize, bitmapWidth, bitmapHeight)
        assertEquals(Offset(0f, 0f), topLeft)

        // Center of the canvas maps to the center of the bitmap.
        val center = mapToBitmapSpace(Offset(50f, 50f), canvasSize, bitmapWidth, bitmapHeight)
        assertEquals(Offset(100f, 50f), center)

        // Bottom-right corner of the scaled image.
        val bottomRight = mapToBitmapSpace(Offset(100f, 75f), canvasSize, bitmapWidth, bitmapHeight)
        assertEquals(Offset(200f, 100f), bottomRight)
    }

    @Test
    fun mapToBitmapSpace_pillarboxedPortrait_mapsPointAccountingForHorizontalOffset() {
        // 100x200 bitmap (1:2) fit into a 100x100 square canvas: scale = 0.5, horizontal bars left/right.
        val canvasSize = IntSize(100, 100)
        val bitmapWidth = 100
        val bitmapHeight = 200

        // Top-left corner of the scaled (pillarboxed) image.
        val topLeft = mapToBitmapSpace(Offset(25f, 0f), canvasSize, bitmapWidth, bitmapHeight)
        assertEquals(Offset(0f, 0f), topLeft)

        // Center of the canvas maps to the center of the bitmap.
        val center = mapToBitmapSpace(Offset(50f, 50f), canvasSize, bitmapWidth, bitmapHeight)
        assertEquals(Offset(50f, 100f), center)

        // Bottom-right corner of the scaled image.
        val bottomRight = mapToBitmapSpace(Offset(75f, 100f), canvasSize, bitmapWidth, bitmapHeight)
        assertEquals(Offset(100f, 200f), bottomRight)
    }

    @Test
    fun mapToBitmapSpace_exactFit_isIdentityMapping() {
        val canvasSize = IntSize(100, 200)
        val point = Offset(40f, 60f)
        assertEquals(point, mapToBitmapSpace(point, canvasSize, bitmapWidth = 100, bitmapHeight = 200))
    }

    @Test
    fun mapToBitmapSpace_zeroCanvasSize_returnsPointUnchanged() {
        val point = Offset(10f, 20f)
        assertEquals(point, mapToBitmapSpace(point, IntSize.Zero, bitmapWidth = 100, bitmapHeight = 100))
    }
}
