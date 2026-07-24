package com.appswithlove.updraft.ui.feedback

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import com.appswithlove.updraft.ui.drawing.DrawnPath
import java.io.ByteArrayOutputStream

actual fun decodePng(bytes: ByteArray): ImageBitmap =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()

actual fun renderAnnotated(base: ByteArray, paths: List<DrawnPath>, canvasSize: IntSize): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(base, 0, base.size)
        .copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)
    paths.forEach { drawn ->
        val paint = Paint().apply {
            color = drawn.color.toArgb()
            strokeWidth = drawn.strokeWidthPx
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        val mappedPoints = drawn.points.map { mapToBitmapSpace(it, canvasSize, bitmap.width, bitmap.height) }
        val path = Path()
        mappedPoints.firstOrNull()?.let { path.moveTo(it.x, it.y) }
        mappedPoints.drop(1).forEach { path.lineTo(it.x, it.y) }
        canvas.drawPath(path, paint)
    }
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}
