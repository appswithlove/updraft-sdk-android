package com.appswithlove.updraft.ui.feedback

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import com.appswithlove.updraft.ui.drawing.DrawnPath
import kotlin.math.min

expect fun decodePng(bytes: ByteArray): ImageBitmap
expect fun renderAnnotated(base: ByteArray, paths: List<DrawnPath>, canvasSize: IntSize): ByteArray

/**
 * Maps a point drawn on the displayed (ContentScale.Fit) image back to the coordinate space of
 * the full-resolution bitmap it was drawn over.
 */
fun mapToBitmapSpace(point: Offset, canvasSize: IntSize, bitmapWidth: Int, bitmapHeight: Int): Offset {
    if (canvasSize.width <= 0 || canvasSize.height <= 0 || bitmapWidth <= 0 || bitmapHeight <= 0) {
        return point
    }
    val scale = min(
        canvasSize.width.toFloat() / bitmapWidth.toFloat(),
        canvasSize.height.toFloat() / bitmapHeight.toFloat(),
    )
    val scaledWidth = bitmapWidth * scale
    val scaledHeight = bitmapHeight * scale
    val offsetX = (canvasSize.width - scaledWidth) / 2f
    val offsetY = (canvasSize.height - scaledHeight) / 2f
    return Offset((point.x - offsetX) / scale, (point.y - offsetY) / scale)
}
