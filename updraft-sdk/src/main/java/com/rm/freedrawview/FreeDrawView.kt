package com.rm.freedrawview

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.os.Parcelable
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
import kotlinx.coroutines.Job

class FreeDrawView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), View.OnTouchListener {

    private var mCurrentPaint: Paint = FreeDrawHelper.createPaint()
    private var mCurrentPath: Path? = null

    private var mResizeBehaviour: ResizeBehaviour? = null

    private var mPoints = arrayListOf<Point>()
    private var mPaths = arrayListOf<HistoryPath>()
    private var mCanceledPaths = arrayListOf<HistoryPath>()

    @ColorInt
    private var mPaintColor: Int = DEFAULT_COLOR

    @IntRange(from = 0, to = 255)
    private var mPaintAlpha: Int = DEFAULT_ALPHA

    private var mLastDimensionW = -1
    private var mLastDimensionH = -1

    private var mFinishPath = false

    private var mPathDrawnListener: PathDrawnListener? = null
    private var mPathRedoUndoCountChangeListener: PathRedoUndoCountChangeListener? = null

    val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    init {
        setOnTouchListener(this)
        var a: TypedArray? = null
        try {
            a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.FreeDrawView,
                defStyleAttr,
                0
            )
            initPaints(a)
        } finally {
            a?.recycle()
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val resizeBehaviour = getResizeBehaviour() ?: return null
        if (mPoints.isNotEmpty()) {
            createHistoryPathFromPoints()
        }
        return FreeDrawSavedState(
            superState,
            mPaths,
            mCanceledPaths,
            getPaintWidth(),
            getPaintColor(),
            getPaintAlpha(),
            resizeBehaviour,
            mLastDimensionW,
            mLastDimensionH
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is FreeDrawSavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        mPaths = state.paths
        mCanceledPaths = state.canceledPaths
        mCurrentPaint = state.getCurrentPaint()
        setPaintWidthPx(state.getCurrentPaintWidth())
        setPaintColor(state.paintColor)
        setPaintAlpha(state.paintAlpha)
        setResizeBehaviour(state.resizeBehaviour)
        mLastDimensionW = state.lastDimensionW
        mLastDimensionH = state.lastDimensionH
        notifyRedoUndoCountChanged()
    }

    fun setPaintColor(@ColorInt color: Int) {
        invalidate()
        mPaintColor = color
        mCurrentPaint.color = mPaintColor
        mCurrentPaint.alpha = mPaintAlpha
    }

    @ColorInt
    fun getPaintColor(): Int = mPaintColor

//    @ColorInt
//    fun getPaintColorWithAlpha(): Int = mCurrentPaint.color

    fun setPaintWidthPx(@FloatRange(from = 0.0) widthPx: Float) {
        if (widthPx > 0) {
            invalidate()
            mCurrentPaint.strokeWidth = widthPx
        }
    }

//    fun setPaintWidthDp(dp: Float) {
//        setPaintWidthPx(FreeDrawHelper.convertDpToPixels(dp))
//    }

    @FloatRange(from = 0.0)
    fun getPaintWidth(inDp: Boolean = false): Float =
        if (inDp) FreeDrawHelper.convertPixelsToDp(mCurrentPaint.strokeWidth)
        else mCurrentPaint.strokeWidth

    fun setPaintAlpha(@IntRange(from = 0, to = 255) alpha: Int) {
        invalidate()
        mPaintAlpha = alpha
        mCurrentPaint.alpha = mPaintAlpha
    }

    @IntRange(from = 0, to = 255)
    fun getPaintAlpha(): Int = mPaintAlpha

    fun setResizeBehaviour(newBehaviour: ResizeBehaviour?) {
        mResizeBehaviour = newBehaviour
    }

    fun getResizeBehaviour(): ResizeBehaviour? = mResizeBehaviour

    fun undoLast() {
        if (mPaths.isNotEmpty()) {
            mFinishPath = true
            invalidate()
            mCanceledPaths.add(mPaths.last())
            mPaths.removeAt(mPaths.size - 1)
            invalidate()
            notifyRedoUndoCountChanged()
        }
    }

//    fun redoLast() {
//        if (mCanceledPaths.isNotEmpty()) {
//            mPaths.add(mCanceledPaths.last())
//            mCanceledPaths.removeAt(mCanceledPaths.size - 1)
//            invalidate()
//            notifyRedoUndoCountChanged()
//        }
//    }
//
//    fun undoAll() {
//        mPaths.reverse()
//        mCanceledPaths.addAll(mPaths)
//        mPaths.clear()
//        invalidate()
//        notifyRedoUndoCountChanged()
//    }
//
//    fun redoAll() {
//        if (mCanceledPaths.isNotEmpty()) {
//            mPaths.addAll(mCanceledPaths)
//            mCanceledPaths.clear()
//            invalidate()
//            notifyRedoUndoCountChanged()
//        }
//    }

    fun getUndoCount(): Int = mPaths.size
    fun getRedoCount(): Int = mCanceledPaths.size

//    fun getPathCount(includeCurrentlyDrawingPath: Boolean): Int {
//        var size = mPaths.size
//        if (includeCurrentlyDrawingPath && mPoints.isNotEmpty()) size++
//        return size
//    }
//
//    fun setOnPathDrawnListener(listener: PathDrawnListener?) {
//        mPathDrawnListener = listener
//    }
//
//    fun removePathDrawnListener() {
//        mPathDrawnListener = null
//    }
//
//    fun clearDrawAndHistory() {
//        clearDraw(false)
//        clearHistory(true)
//    }
//
//    fun clearDraw() {
//        clearDraw(true)
//    }

//    private fun clearDraw(invalidate: Boolean) {
//        mPoints = arrayListOf()
//        mPaths = arrayListOf()
//        notifyRedoUndoCountChanged()
//        if (invalidate) invalidate()
//    }
//
//    fun clearHistory() {
//        clearHistory(true)
//    }

//    private fun clearHistory(invalidate: Boolean) {
//        mCanceledPaths = arrayListOf()
//        notifyRedoUndoCountChanged()
//        if (invalidate) invalidate()
//    }
//
//    fun setPathRedoUndoCountChangeListener(listener: PathRedoUndoCountChangeListener?) {
//        mPathRedoUndoCountChangeListener = listener
//    }
//
//    fun removePathRedoUndoCountChangeListener() {
//        mPathRedoUndoCountChangeListener = null
//    }
//
//    fun getCurrentViewStateAsSerializable(): FreeDrawSerializableState {
//        return FreeDrawSerializableState(
//            mCanceledPaths, mPaths, getPaintColor(),
//            getPaintAlpha(), getPaintWidth(), getResizeBehaviour(),
//            mLastDimensionW, mLastDimensionH
//        )
//    }
//
//    fun restoreStateFromSerializable(state: FreeDrawSerializableState?) {
//        state ?: return
//        state.canceledPaths.let { mCanceledPaths = it }
//        state.paths.let { mPaths = it }
//        mPaintColor = state.paintColor
//        mPaintAlpha = state.paintAlpha
//        mCurrentPaint.color = state.paintColor
//        mCurrentPaint.alpha = state.paintAlpha
//        setPaintWidthPx(state.paintWidth)
//        mResizeBehaviour = state.resizeBehaviour
//        if (state.lastDimensionW >= 0) mLastDimensionW = state.lastDimensionW
//        if (state.lastDimensionH >= 0) mLastDimensionH = state.lastDimensionH
//        notifyRedoUndoCountChanged()
//        invalidate()
//    }

    fun getDrawScreenshot(listener: DrawCreatorListener) {
        takeScreenshot(listener)
    }

    private fun notifyPathStart() {
        mPathDrawnListener?.onPathStart()
    }

    private fun notifyPathDrawn() {
        mPathDrawnListener?.onNewPathDrawn()
    }

    private fun notifyRedoUndoCountChanged() {
        mPathRedoUndoCountChangeListener?.onRedoCountChanged(getRedoCount())
        mPathRedoUndoCountChangeListener?.onUndoCountChanged(getUndoCount())
    }

    private fun initPaints(a: TypedArray?) {
        mCurrentPaint = FreeDrawHelper.createPaint()
        mCurrentPaint.color =
            a?.getColor(R.styleable.FreeDrawView_paintColor, mPaintColor) ?: mPaintColor
        mCurrentPaint.alpha =
            a?.getInt(R.styleable.FreeDrawView_paintAlpha, mPaintAlpha) ?: mPaintAlpha
        mCurrentPaint.strokeWidth = a?.getDimensionPixelSize(
            R.styleable.FreeDrawView_paintWidth,
            FreeDrawHelper.convertDpToPixels(DEFAULT_STROKE_WIDTH).toInt()
        )?.toFloat() ?: FreeDrawHelper.convertDpToPixels(DEFAULT_STROKE_WIDTH)
        FreeDrawHelper.setupStrokePaint(mCurrentPaint)

        a?.let {
            val resizeBehaviour = it.getInt(R.styleable.FreeDrawView_resizeBehaviour, -1)
            mResizeBehaviour = when (resizeBehaviour) {
                0 -> ResizeBehaviour.CLEAR
                1 -> ResizeBehaviour.FIT_XY
                2 -> ResizeBehaviour.CROP
                else -> ResizeBehaviour.CROP
            }
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

    override fun onDraw(canvas: Canvas) {
        if (mPaths.isEmpty() && mPoints.isEmpty()) return

        val finishedPath = mFinishPath
        mFinishPath = false

        for (currentPath in mPaths) {
            if (currentPath.isPoint) {
                canvas.drawCircle(
                    currentPath.originX,
                    currentPath.originY,
                    currentPath.getPaint().strokeWidth / 2,
                    currentPath.getPaint()
                )
            } else {
                canvas.drawPath(currentPath.getPath(), currentPath.getPaint())
            }
        }

        if (mCurrentPath == null) mCurrentPath = Path() else mCurrentPath!!.rewind()

        when {
            mPoints.size == 1 || FreeDrawHelper.isAPoint(mPoints) -> {
                canvas.drawCircle(
                    mPoints[0].x, mPoints[0].y,
                    mCurrentPaint.strokeWidth / 2,
                    createAndCopyColorAndAlphaForFillPaint(mCurrentPaint, false)
                )
            }

            mPoints.isNotEmpty() -> {
                var first = true
                for (point in mPoints) {
                    if (first) {
                        mCurrentPath!!.moveTo(point.x, point.y)
                        first = false
                    } else {
                        mCurrentPath!!.lineTo(point.x, point.y)
                    }
                }
                canvas.drawPath(mCurrentPath!!, mCurrentPaint)
            }
        }

        if (finishedPath && mPoints.isNotEmpty()) {
            createHistoryPathFromPoints()
        }
    }

    private fun createHistoryPathFromPoints() {
        mPaths.add(HistoryPath(mPoints, Paint(mCurrentPaint)))
        mPoints = arrayListOf()
        notifyPathDrawn()
        notifyRedoUndoCountChanged()
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) notifyPathStart()
        parent?.requestDisallowInterceptTouchEvent(true)
        mCanceledPaths = arrayListOf()

        mFinishPath =
            motionEvent.action == MotionEvent.ACTION_UP || motionEvent.action == MotionEvent.ACTION_CANCEL

        if (!mFinishPath) {
            for (i in 0 until motionEvent.historySize) {
                val point = Point()
                point.x = motionEvent.getHistoricalX(i)
                point.y = motionEvent.getHistoricalY(i)
                mPoints.add(point)
            }
            val point = Point()
            point.x = motionEvent.x
            point.y = motionEvent.y
            mPoints.add(point)
        }

        invalidate()
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        var xFactor = 1f
        var yFactor = 1f
        if (mLastDimensionW == -1) mLastDimensionW = w
        if (mLastDimensionH == -1) mLastDimensionH = h
        if (w >= 0 && w != oldw && w != mLastDimensionW) xFactor = w.toFloat() / mLastDimensionW
        if (h >= 0 && h != oldh && h != mLastDimensionH) yFactor = h.toFloat() / mLastDimensionH
        multiplyPathsAndPoints(xFactor, yFactor)
    }

    private fun multiplyPathsAndPoints(xFactor: Float, yFactor: Float) {
        if ((xFactor == 1f && yFactor == 1f) || xFactor <= 0 || yFactor <= 0 ||
            (mPaths.isEmpty() && mCanceledPaths.isEmpty() && mPoints.isEmpty())
        ) return

        if (mResizeBehaviour == ResizeBehaviour.CLEAR) {
            mPaths.clear()
            mCanceledPaths.clear()
            mPoints.clear()
            return
        }

        val xf = if (mResizeBehaviour == ResizeBehaviour.CROP) 1f else xFactor
        val yf = if (mResizeBehaviour == ResizeBehaviour.CROP) 1f else yFactor

        for (historyPath in mPaths + mCanceledPaths) {
            if (historyPath.isPoint) {
                historyPath.originX *= xf
                historyPath.originY *= yf
            } else {
                for (point in historyPath.points) {
                    point.x *= xf
                    point.y *= yf
                }
            }
            historyPath.generatePath()
        }

        for (point in mPoints) {
            point.x *= xf
            point.y *= yf
        }
    }

    interface DrawCreatorListener {
        fun onDrawCreated(draw: Bitmap)
        fun onDrawCreationError()
    }

    private fun takeScreenshot(listener: DrawCreatorListener) {
        val width = width
        val height = height

        coroutineScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                try {
                    val bm = createBitmap(width, height)
                    val canvas = Canvas(bm)
                    canvas
                    bm
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            if (bitmap != null) {
                val canvas = Canvas(bitmap)
                draw(canvas)
                listener.onDrawCreated(bitmap)
            } else {
                listener.onDrawCreationError()
            }
        }
    }

    companion object {
        private const val DEFAULT_STROKE_WIDTH = 4f
        private const val DEFAULT_COLOR = Color.BLACK
        private const val DEFAULT_ALPHA = 255
    }
}
