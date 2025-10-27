package com.rm.freedrawview

import android.content.res.Resources
import android.graphics.CornerPathEffect
import android.graphics.ComposePathEffect
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Paint.Join
import android.graphics.Paint.Style

object FreeDrawHelper {

    /**
     * Check if a list of points represents a single point (all points have same coordinates)
     */
    fun isAPoint(points: List<Point>): Boolean {
        if (points.isEmpty()) return false
        if (points.size == 1) return true

        for (i in 1 until points.size) {
            if (points[i - 1].x != points[i].x || points[i - 1].y != points[i].y) return false
        }
        return true
    }

    /**
     * Create, initialize, and setup a Paint
     */
    fun createPaintAndInitialize(
        paintColor: Int,
        paintAlpha: Int,
        paintWidth: Float,
        fill: Boolean
    ): Paint {
        val paint = createPaint()
        initializePaint(paint, paintColor, paintAlpha, paintWidth, fill)
        return paint
    }

    fun createPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun initializePaint(
        paint: Paint,
        paintColor: Int,
        paintAlpha: Int,
        paintWidth: Float,
        fill: Boolean
    ) {
        if (fill) setupFillPaint(paint) else setupStrokePaint(paint)
        paint.strokeWidth = paintWidth
        paint.color = paintColor
        paint.alpha = paintAlpha
    }

    fun setupFillPaint(paint: Paint) {
        paint.style = Style.FILL
    }

    fun setupStrokePaint(paint: Paint) {
        paint.strokeJoin = Join.ROUND
        paint.strokeCap = Cap.ROUND
        paint.pathEffect = ComposePathEffect(
            CornerPathEffect(100f),
            CornerPathEffect(100f)
        )
        paint.style = Style.STROKE
    }

    fun copyFromPaint(from: Paint, to: Paint, copyWidth: Boolean) {
        to.color = from.color
        to.alpha = from.alpha
        if (copyWidth) to.strokeWidth = from.strokeWidth
    }

    fun copyFromValues(to: Paint, color: Int, alpha: Int, strokeWidth: Float, copyWidth: Boolean) {
        to.color = color
        to.alpha = alpha
        if (copyWidth) to.strokeWidth = strokeWidth
    }

    /** Convert dp to pixels **/
    fun convertDpToPixels(dp: Float): Float =
        dp * Resources.getSystem().displayMetrics.density

    /** Convert pixels to dp **/
    fun convertPixelsToDp(px: Float): Float =
        px / Resources.getSystem().displayMetrics.density
}
