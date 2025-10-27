package com.rm.freedrawview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.appswithlove.updraft.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap

class FreeDrawView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), View.OnTouchListener {

    companion object {
        private const val DEFAULT_STROKE_WIDTH = 4f
        private const val DEFAULT_COLOR = Color.BLACK
        private const val DEFAULT_ALPHA = 255
    }

    private var currentPaint: Paint = FreeDrawHelper.createPaint()
    private var currentPath: Path = Path()

    private var resizeBehaviour: ResizeBehaviour = ResizeBehaviour.CROP

    private var points = arrayListOf<Point>()
    private var paths = arrayListOf<HistoryPath>()
    private var canceledPaths = arrayListOf<HistoryPath>()

    @ColorInt
    private var paintColor = DEFAULT_COLOR
    @IntRange(from = 0, to = 255)
    private var paintAlpha = DEFAULT_ALPHA

    private var lastDimensionW = -1
    private var lastDimensionH = -1

    private var finishPath = false

    private var pathDrawnListener: PathDrawnListener? = null
    private var pathRedoUndoCountChangeListener: PathRedoUndoCountChangeListener? = null

    init {
        setOnTouchListener(this)
        attrs?.let {
            val a = context.theme.obtainStyledAttributes(attrs, R.styleable.FreeDrawView, defStyleAttr, 0)
            try {
                initPaints(a)
            } finally {
                a.recycle()
            }
        }
    }

    /** Paint setters/getters **/
    fun setPaintColor(@ColorInt color: Int) {
        paintColor = color
        currentPaint.color = paintColor
        currentPaint.alpha = paintAlpha
        invalidate()
    }

    @ColorInt
    fun getPaintColor(): Int = paintColor

    @ColorInt
    fun getPaintColorWithAlpha(): Int = currentPaint.color

    fun setPaintWidthPx(@FloatRange(from = 0.0) widthPx: Float) {
        if (widthPx > 0f) {
            currentPaint.strokeWidth = widthPx
            invalidate()
        }
    }

    fun setPaintWidthDp(dp: Float) {
        setPaintWidthPx(FreeDrawHelper.convertDpToPixels(dp))
    }

    fun getPaintWidth(inDp: Boolean = false): Float {
        return if (inDp) FreeDrawHelper.convertPixelsToDp(currentPaint.strokeWidth) else currentPaint.strokeWidth
    }

    fun setPaintAlpha(@IntRange(from = 0, to = 255) alpha: Int) {
        paintAlpha = alpha
        currentPaint.alpha = paintAlpha
        invalidate()
    }

    @IntRange(from = 0, to = 255)
    fun getPaintAlpha(): Int = paintAlpha

    fun setResizeBehaviour(newBehaviour: ResizeBehaviour) {
        resizeBehaviour = newBehaviour
    }

    fun getResizeBehaviour(): ResizeBehaviour = resizeBehaviour

    /** Undo / Redo **/
    fun undoLast() {
        if (paths.isNotEmpty()) {
            finishPath = true
            canceledPaths.add(paths.removeAt(paths.size - 1))
            invalidate()
            notifyRedoUndoCountChanged()
        }
    }

    fun redoLast() {
        if (canceledPaths.isNotEmpty()) {
            paths.add(canceledPaths.removeAt(canceledPaths.size - 1))
            invalidate()
            notifyRedoUndoCountChanged()
        }
    }

    fun undoAll() {
        canceledPaths.addAll(paths.asReversed())
        paths.clear()
        invalidate()
        notifyRedoUndoCountChanged()
    }

    fun redoAll() {
        paths.addAll(canceledPaths)
        canceledPaths.clear()
        invalidate()
        notifyRedoUndoCountChanged()
    }

    fun getUndoCount(): Int = paths.size
    fun getRedoCount(): Int = canceledPaths.size
    fun getPathCount(includeCurrentlyDrawingPath: Boolean = false): Int =
        paths.size + if (includeCurrentlyDrawingPath && points.isNotEmpty()) 1 else 0

    /** Path listeners **/
    fun setOnPathDrawnListener(listener: PathDrawnListener?) {
        pathDrawnListener = listener
    }

    fun removePathDrawnListener() {
        pathDrawnListener = null
    }

    fun setPathRedoUndoCountChangeListener(listener: PathRedoUndoCountChangeListener?) {
        pathRedoUndoCountChangeListener = listener
    }

    fun removePathRedoUndoCountChangeListener() {
        pathRedoUndoCountChangeListener = null
    }

    /** Clear methods **/
    fun clearDrawAndHistory() {
        clearDraw(false)
        clearHistory(true)
    }

    fun clearDraw() = clearDraw(true)
    private fun clearDraw(invalidateView: Boolean) {
        points.clear()
        paths.clear()
        notifyRedoUndoCountChanged()
        if (invalidateView) invalidate()
    }

    fun clearHistory() = clearHistory(true)
    private fun clearHistory(invalidateView: Boolean) {
        canceledPaths.clear()
        notifyRedoUndoCountChanged()
        if (invalidateView) invalidate()
    }

    /** Touch events **/
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        event ?: return false

        if (event.action == MotionEvent.ACTION_DOWN) notifyPathStart()
        parent?.requestDisallowInterceptTouchEvent(true)
        canceledPaths.clear()

        when (event.action) {
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                for (i in 0 until event.historySize) {
                    points.add(Point(event.getHistoricalX(i), event.getHistoricalY(i)))
                }
                points.add(Point(event.x, event.y))
                finishPath = false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> finishPath = true
        }

        invalidate()
        return true
    }

    /** Drawing **/
    override fun onDraw(canvas: Canvas) {
        if (paths.isEmpty() && points.isEmpty()) return

        val finishedPath = finishPath
        finishPath = false

        paths.forEach { pathObj ->
            if (pathObj.isPoint) {
                canvas.drawCircle(pathObj.originX, pathObj.originY, pathObj.getPaint().strokeWidth / 2, pathObj.getPaint())
            } else {
                canvas.drawPath(pathObj.getPath(), pathObj.getPaint())
            }
        }

        currentPath.rewind()

        if (points.size == 1 || FreeDrawHelper.isAPoint(points)) {
            val p = points[0]
            canvas.drawCircle(p.x, p.y, currentPaint.strokeWidth / 2, createAndCopyColorAndAlphaForFillPaint(currentPaint, false))
        } else if (points.isNotEmpty()) {
            var first = true
            points.forEach { p ->
                if (first) {
                    currentPath.moveTo(p.x, p.y)
                    first = false
                } else {
                    currentPath.lineTo(p.x, p.y)
                }
            }
            canvas.drawPath(currentPath, currentPaint)
        }

        if (finishedPath && points.isNotEmpty()) createHistoryPathFromPoints()
    }

    private fun createHistoryPathFromPoints() {
        paths.add(HistoryPath(ArrayList(points), Paint(currentPaint)))
        points.clear()
        notifyPathDrawn()
        notifyRedoUndoCountChanged()
    }

    private fun notifyPathStart() = pathDrawnListener?.onPathStart()
    private fun notifyPathDrawn() = pathDrawnListener?.onNewPathDrawn()
    private fun notifyRedoUndoCountChanged() {
        pathRedoUndoCountChangeListener?.let {
            it.onRedoCountChanged(getRedoCount())
            it.onUndoCountChanged(getUndoCount())
        }
    }

    private fun initPaints(a: android.content.res.TypedArray) {
        currentPaint = FreeDrawHelper.createPaint()
        currentPaint.color = a.getColor(R.styleable.FreeDrawView_paintColor, paintColor)
        currentPaint.alpha = a.getInt(R.styleable.FreeDrawView_paintAlpha, paintAlpha)
        currentPaint.strokeWidth = a.getDimensionPixelSize(
            R.styleable.FreeDrawView_paintWidth,
            FreeDrawHelper.convertDpToPixels(DEFAULT_STROKE_WIDTH).toInt()
        ).toFloat()
        FreeDrawHelper.setupStrokePaint(currentPaint)

        val resizeValue = a.getInt(R.styleable.FreeDrawView_resizeBehaviour, -1)
        resizeBehaviour = when (resizeValue) {
            0 -> ResizeBehaviour.CLEAR
            1 -> ResizeBehaviour.FIT_XY
            2 -> ResizeBehaviour.CROP
            else -> ResizeBehaviour.CROP
        }
    }

    private fun createAndCopyColorAndAlphaForFillPaint(from: Paint, copyWidth: Boolean): Paint {
        val paint = FreeDrawHelper.createPaint()
        FreeDrawHelper.setupFillPaint(paint)
        paint.color = from.color
        paint.alpha = from.alpha
        if (copyWidth) paint.strokeWidth = from.strokeWidth
        return paint
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        var xFactor = 1f
        var yFactor = 1f

        if (lastDimensionW == -1) lastDimensionW = w
        if (lastDimensionH == -1) lastDimensionH = h

        if (w >= 0 && w != oldw && w != lastDimensionW) {
            xFactor = w.toFloat() / lastDimensionW
            lastDimensionW = w
        }
        if (h >= 0 && h != oldh && h != lastDimensionH) {
            yFactor = h.toFloat() / lastDimensionH
            lastDimensionH = h
        }

        multiplyPathsAndPoints(xFactor, yFactor)
    }

    private fun multiplyPathsAndPoints(xFactor: Float, yFactor: Float) {
        var xMul = xFactor
        var yMul = yFactor

        if ((xMul == 1f && yMul == 1f) || (xMul <= 0 || yMul <= 0) ||
            (paths.isEmpty() && canceledPaths.isEmpty() && points.isEmpty())) return

        if (resizeBehaviour == ResizeBehaviour.CLEAR) {
            paths.clear()
            canceledPaths.clear()
            points.clear()
            return
        } else if (resizeBehaviour == ResizeBehaviour.CROP) {
            xMul = 1f
            yMul = 1f
        }

        paths.forEach { adaptHistoryPath(it, xMul, yMul) }
        canceledPaths.forEach { adaptHistoryPath(it, xMul, yMul) }
        points.forEach { p -> p.x *= xMul; p.y *= yMul }
    }

    private fun adaptHistoryPath(historyPath: HistoryPath, xMul: Float, yMul: Float) {
        if (historyPath.isPoint) {
            historyPath.originX *= xMul
            historyPath.originY *= yMul
        } else {
            historyPath.points.forEach { it.x *= xMul; it.y *= yMul }
        }
        historyPath.generatePath()
    }

    /** Screenshot **/
    fun getDrawScreenshot(listener: DrawCreatorListener) {
        CoroutineScope(Dispatchers.Main).launch {
            val bitmap = withContext(Dispatchers.Default) {
                try {
                    val bmp = createBitmap(width, height)
                    val canvas = Canvas(bmp)
                    draw(canvas)
                    bmp
                } catch (_: Exception) {
                    null
                }
            }

            if (bitmap != null) listener.onDrawCreated(bitmap) else listener.onDrawCreationError()
        }
    }

    /** Interfaces **/
    interface DrawCreatorListener {
        fun onDrawCreated(draw: Bitmap)
        fun onDrawCreationError()
    }

    interface PathDrawnListener {
        fun onPathStart()
        fun onNewPathDrawn()
    }

    interface PathRedoUndoCountChangeListener {
        fun onRedoCountChanged(count: Int)
        fun onUndoCountChanged(count: Int)
    }
}
