package com.rm.freedrawview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Riccardo Moro on 9/10/2016.
 */
public class FreeDrawView extends View implements View.OnTouchListener {
    private static final String TAG = FreeDrawView.class.getSimpleName();

    private static final float DEFAULT_STROKE_WIDTH = 4;
    private static final int DEFAULT_COLOR = Color.BLACK;
    private static final int DEFAULT_ALPHA = 255;

    private Paint mCurrentPaint;
    private Path mCurrentPath;

    private ResizeBehaviour mResizeBehaviour;

    private ArrayList<Point> mPoints = new ArrayList<>();
    private ArrayList<HistoryPath> mPaths = new ArrayList<>();
    private ArrayList<HistoryPath> mCanceledPaths = new ArrayList<>();

    @ColorInt
    private int mPaintColor = DEFAULT_COLOR;
    @IntRange(from = 0, to = 255)
    private int mPaintAlpha = DEFAULT_ALPHA;

    private int mLastDimensionW = -1;
    private int mLastDimensionH = -1;

    private boolean mFinishPath = false;

    private PathDrawnListener mPathDrawnListener;
    private PathRedoUndoCountChangeListener mPathRedoUndoCountChangeListener;

    public FreeDrawView(Context context) {
        this(context, null);
    }

    public FreeDrawView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FreeDrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOnTouchListener(this);

        TypedArray a = null;
        try {

            a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.FreeDrawView,
                    defStyleAttr, 0);

            initPaints(a);
        } finally {
            if (a != null) {
                a.recycle();
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {

        // Get the superclass parcelable state
        Parcelable superState = super.onSaveInstanceState();

        if (mPoints.size() > 0) {// Currently doing a line, save it's current path
            createHistoryPathFromPoints();
        }

        return new FreeDrawSavedState(superState, mPaths, mCanceledPaths,
                getPaintWidth(), getPaintColor(), getPaintAlpha(),
                getResizeBehaviour(), mLastDimensionW, mLastDimensionH);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {

        // If not instance of my state, let the superclass handle it
        if (!(state instanceof FreeDrawSavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        FreeDrawSavedState savedState = (FreeDrawSavedState) state;
        // Superclass restore state
        super.onRestoreInstanceState(savedState.getSuperState());

        // My state restore
        mPaths = savedState.getPaths();
        mCanceledPaths = savedState.getCanceledPaths();
        mCurrentPaint = savedState.getCurrentPaint();

        setPaintWidthPx(savedState.getCurrentPaintWidth());
        setPaintColor(savedState.getPaintColor());
        setPaintAlpha(savedState.getPaintAlpha());

        setResizeBehaviour(savedState.getResizeBehaviour());

        // Restore the last dimensions, so that in onSizeChanged i can calculate the
        // height and width change factor and multiply every point x or y to it, so that if the
        // View is resized, it adapt automatically it's points to the new width/height
        mLastDimensionW = savedState.getLastDimensionW();
        mLastDimensionH = savedState.getLastDimensionH();

        notifyRedoUndoCountChanged();
    }

    /**
     * Set the paint color
     *
     * @param color The now color to be applied to the
     */
    public void setPaintColor(@ColorInt int color) {

        invalidate();

        mPaintColor = color;

        mCurrentPaint.setColor(mPaintColor);
        mCurrentPaint.setAlpha(mPaintAlpha);// Restore the previous alpha
    }

    /**
     * Get the current paint color without it's alpha
     */
    @ColorInt
    public int getPaintColor() {
        return mPaintColor;
    }

    /**
     * Get the current color with the current alpha
     */
    @ColorInt
    public int getPaintColorWithAlpha() {
        return mCurrentPaint.getColor();
    }


    /**
     * Set the paint width in px
     *
     * @param widthPx The new weight in px, must be > 0
     */
    public void setPaintWidthPx(@FloatRange(from = 0) float widthPx) {
        if (widthPx > 0) {

            invalidate();

            mCurrentPaint.setStrokeWidth(widthPx);
        }
    }

    /**
     * Set the paint width in dp
     *
     * @param dp The new weight in dp, must be > 0
     */
    public void setPaintWidthDp(float dp) {
        setPaintWidthPx(FreeDrawHelper.convertDpToPixels(dp));
    }

    /**
     * {@link #getPaintWidth(boolean)}
     */
    @FloatRange(from = 0)
    public float getPaintWidth() {
        return getPaintWidth(false);
    }

    /**
     * Get the current paint with in dp or pixel
     */
    @FloatRange(from = 0)
    public float getPaintWidth(boolean inDp) {
        if (inDp) {
            return FreeDrawHelper.convertPixelsToDp(mCurrentPaint.getStrokeWidth());
        } else {
            return mCurrentPaint.getStrokeWidth();
        }
    }


    /**
     * Set the paint opacity, must be between 0 and 1
     *
     * @param alpha The alpha to apply to the paint
     */
    public void setPaintAlpha(@IntRange(from = 0, to = 255) int alpha) {

        // Finish current path and redraw, so that the new setting is applied only to the next path
        invalidate();

        mPaintAlpha = alpha;
        mCurrentPaint.setAlpha(mPaintAlpha);
    }

    /**
     * Get the current paint alpha
     */
    @IntRange(from = 0, to = 255)
    public int getPaintAlpha() {
        return mPaintAlpha;
    }


    /**
     * Set what to do when the view is resized (on rotation if its dimensions are not fixed)
     * {@link ResizeBehaviour}
     */
    public void setResizeBehaviour(ResizeBehaviour newBehaviour) {
        mResizeBehaviour = newBehaviour;
    }

    /**
     * Get the current behaviour on view resize
     */
    public ResizeBehaviour getResizeBehaviour() {
        return mResizeBehaviour;
    }


    /**
     * Cancel the last drawn segment
     */
    public void undoLast() {

        if (mPaths.size() > 0) {
            // End current path
            mFinishPath = true;
            invalidate();

            // Cancel the last one and redraw
            mCanceledPaths.add(mPaths.get(mPaths.size() - 1));
            mPaths.remove(mPaths.size() - 1);
            invalidate();

            notifyRedoUndoCountChanged();
        }
    }

    /**
     * Re-add the first removed path and redraw
     */
    public void redoLast() {

        if (mCanceledPaths.size() > 0) {
            mPaths.add(mCanceledPaths.get(mCanceledPaths.size() - 1));
            mCanceledPaths.remove(mCanceledPaths.size() - 1);
            invalidate();

            notifyRedoUndoCountChanged();
        }
    }

    /**
     * Remove all the paths and redraw (can be undone with {@link #redoLast()})
     */
    public void undoAll() {
        Collections.reverse(mPaths);
        mCanceledPaths.addAll(mPaths);
        mPaths = new ArrayList<>();
        invalidate();

        notifyRedoUndoCountChanged();
    }

    /**
     * Re-add all the removed paths and redraw
     */
    public void redoAll() {

        if (mCanceledPaths.size() > 0) {
            mPaths.addAll(mCanceledPaths);
            mCanceledPaths = new ArrayList<>();
            invalidate();

            notifyRedoUndoCountChanged();
        }
    }

    /**
     * Get how many undo operations are available
     */
    public int getUndoCount() {
        return mPaths.size();
    }

    /**
     * Get how many redo operations are available
     */
    public int getRedoCount() {
        return mCanceledPaths.size();
    }

    /**
     * Get how many paths are drawn on this FreeDrawView
     *
     * @param includeCurrentlyDrawingPath Include the path that is currently been drawn
     * @return The number of paths drawn
     */
    public int getPathCount(boolean includeCurrentlyDrawingPath) {
        int size = mPaths.size();

        if (includeCurrentlyDrawingPath && mPoints.size() > 0) {
            size++;
        }
        return size;
    }

    /**
     * Set a path drawn listener, will be called every time a new path is drawn
     */
    public void setOnPathDrawnListener(PathDrawnListener listener) {
        mPathDrawnListener = listener;
    }

    /**
     * Remove the path drawn listener
     */
    public void removePathDrawnListener() {
        mPathDrawnListener = null;
    }

    /**
     * Clear the current draw and the history
     */
    public void clearDrawAndHistory() {

        clearDraw(false);
        clearHistory(true);
    }

    /**
     * Clear the current draw
     */
    public void clearDraw() {

        clearDraw(true);
    }

    private void clearDraw(boolean invalidate) {
        mPoints = new ArrayList<>();
        mPaths = new ArrayList<>();

        notifyRedoUndoCountChanged();

        if (invalidate) {
            invalidate();
        }
    }

    /**
     * Clear the history (paths that can be redone)
     */
    public void clearHistory() {
        clearHistory(true);
    }

    private void clearHistory(boolean invalidate) {
        mCanceledPaths = new ArrayList<>();

        notifyRedoUndoCountChanged();

        if (invalidate) {
            invalidate();
        }
    }

    /**
     * Set a redo-undo count change listener, this will be called every time undo or redo count
     * changes
     */
    public void setPathRedoUndoCountChangeListener(PathRedoUndoCountChangeListener listener) {
        mPathRedoUndoCountChangeListener = listener;
    }

    /**
     * Remove the redo-undo count listener
     */
    public void removePathRedoUndoCountChangeListener() {
        mPathRedoUndoCountChangeListener = null;
    }

    /**
     * Get a serializable object with all the needed info about the current draw and state
     *
     * @return A {@link FreeDrawSerializableState} containing all the needed data
     */
    public FreeDrawSerializableState getCurrentViewStateAsSerializable() {

        return new FreeDrawSerializableState(mCanceledPaths, mPaths, getPaintColor(),
                getPaintAlpha(), getPaintWidth(), getResizeBehaviour(),
                mLastDimensionW, mLastDimensionH);
    }

    /**
     * Restore the state of the draw from the given serializable state
     *
     * @param state A {@link FreeDrawSerializableState} containing all the draw and paint info,
     *              if null, nothing will be restored. Null sub fields will be ignored
     */
    public void restoreStateFromSerializable(FreeDrawSerializableState state) {

        if (state != null) {

            if (state.getCanceledPaths() != null) {
                mCanceledPaths = state.getCanceledPaths();
            }

            if (state.getPaths() != null) {
                mPaths = state.getPaths();
            }

            mPaintColor = state.getPaintColor();
            mPaintAlpha = state.getPaintAlpha();

            mCurrentPaint.setColor(state.getPaintColor());
            mCurrentPaint.setAlpha(state.getPaintAlpha());
            setPaintWidthPx(state.getPaintWidth());

            mResizeBehaviour = state.getResizeBehaviour();

            if (state.getLastDimensionW() >= 0) {
                mLastDimensionW = state.getLastDimensionW();
            }

            if (state.getLastDimensionH() >= 0) {
                mLastDimensionH = state.getLastDimensionH();
            }

            notifyRedoUndoCountChanged();
            invalidate();
        }
    }

    /**
     * Create a Bitmap with the content drawn inside the view
     */
    public void getDrawScreenshot(@NonNull final DrawCreatorListener listener) {
        new TakeScreenShotAsyncTask(listener).execute();
    }


    // Internal methods
    private void notifyPathStart() {
        if (mPathDrawnListener != null) {
            mPathDrawnListener.onPathStart();
        }
    }

    private void notifyPathDrawn() {
        if (mPathDrawnListener != null) {
            mPathDrawnListener.onNewPathDrawn();
        }
    }

    private void notifyRedoUndoCountChanged() {
        if (mPathRedoUndoCountChangeListener != null) {
            mPathRedoUndoCountChangeListener.onRedoCountChanged(getRedoCount());
            mPathRedoUndoCountChangeListener.onUndoCountChanged(getUndoCount());
        }
    }

    private void initPaints(TypedArray a) {
        mCurrentPaint = FreeDrawHelper.createPaint();

        mCurrentPaint.setColor(a != null ? a.getColor(R.styleable.FreeDrawView_paintColor,
                mPaintColor) : mPaintColor);
        mCurrentPaint.setAlpha(a != null ?
                a.getInt(R.styleable.FreeDrawView_paintAlpha, mPaintAlpha)
                : mPaintAlpha);
        mCurrentPaint.setStrokeWidth(a != null ?
                a.getDimensionPixelSize(R.styleable.FreeDrawView_paintWidth,
                        (int) FreeDrawHelper.convertDpToPixels(DEFAULT_STROKE_WIDTH))
                : FreeDrawHelper.convertDpToPixels(DEFAULT_STROKE_WIDTH));

        FreeDrawHelper.setupStrokePaint(mCurrentPaint);

        if (a != null) {
            int resizeBehaviour = a.getInt(R.styleable.FreeDrawView_resizeBehaviour, -1);
            mResizeBehaviour =
                    resizeBehaviour == 0 ? ResizeBehaviour.CLEAR :
                            resizeBehaviour == 1 ? ResizeBehaviour.FIT_XY :
                                    resizeBehaviour == 2 ? ResizeBehaviour.CROP :
                                            ResizeBehaviour.CROP;
        }
    }

    private Paint createAndCopyColorAndAlphaForFillPaint(Paint from, boolean copyWidth) {
        Paint paint = FreeDrawHelper.createPaint();
        FreeDrawHelper.setupFillPaint(paint);
        paint.setColor(from.getColor());
        paint.setAlpha(from.getAlpha());
        if (copyWidth) {
            paint.setStrokeWidth(from.getStrokeWidth());
        }
        return paint;
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if (mPaths.size() == 0 && mPoints.size() == 0) {
            return;
        }

        // Avoid concurrency errors by first setting the finished path variable to false
        final boolean finishedPath = mFinishPath;
        mFinishPath = false;

        for (HistoryPath currentPath : mPaths) {

            // If the path is just a single point, draw as a point
            if (currentPath.isPoint()) {

                canvas.drawCircle(currentPath.getOriginX(), currentPath.getOriginY(),
                        currentPath.getPaint().getStrokeWidth() / 2, currentPath.getPaint());
            } else {// Else draw the complete path

                canvas.drawPath(currentPath.getPath(), currentPath.getPaint());
            }
        }

        // Initialize the current path
        if (mCurrentPath == null)
            mCurrentPath = new Path();
        else
            mCurrentPath.rewind();

        // If a single point, add a circle to the path
        if (mPoints.size() == 1 || FreeDrawHelper.isAPoint(mPoints)) {

            canvas.drawCircle(mPoints.get(0).x, mPoints.get(0).y,
                    mCurrentPaint.getStrokeWidth() / 2,
                    createAndCopyColorAndAlphaForFillPaint(mCurrentPaint, false));
        } else if (mPoints.size() != 0) {// Else draw the complete series of points

            boolean first = true;

            for (Point point : mPoints) {

                if (first) {
                    mCurrentPath.moveTo(point.x, point.y);
                    first = false;
                } else {
                    mCurrentPath.lineTo(point.x, point.y);
                }
            }

            canvas.drawPath(mCurrentPath, mCurrentPaint);
        }

        // If the path is finished, add it to the history
        if (finishedPath && mPoints.size() > 0) {
            createHistoryPathFromPoints();
        }
    }

    // Create a path from the current points
    private void createHistoryPathFromPoints() {
        mPaths.add(new HistoryPath(mPoints, new Paint(mCurrentPaint)));

        mPoints = new ArrayList<>();

        notifyPathDrawn();
        notifyRedoUndoCountChanged();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            notifyPathStart();
        }
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        // Clear all the history when restarting to draw
        mCanceledPaths = new ArrayList<>();

        if ((motionEvent.getAction() != MotionEvent.ACTION_UP) &&
                (motionEvent.getAction() != MotionEvent.ACTION_CANCEL)) {
            Point point;
            for (int i = 0; i < motionEvent.getHistorySize(); i++) {
                point = new Point();
                point.x = motionEvent.getHistoricalX(i);
                point.y = motionEvent.getHistoricalY(i);
                mPoints.add(point);
            }
            point = new Point();
            point.x = motionEvent.getX();
            point.y = motionEvent.getY();
            mPoints.add(point);
            mFinishPath = false;
        } else
            mFinishPath = true;

        invalidate();
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float xMultiplyFactor = 1;
        float yMultiplyFactor = 1;


        if (mLastDimensionW == -1) {
            mLastDimensionW = w;
        }

        if (mLastDimensionH == -1) {
            mLastDimensionH = h;
        }

        if (w >= 0 && w != oldw && w != mLastDimensionW) {
            xMultiplyFactor = (float) w / mLastDimensionW;
            mLastDimensionW = w;
        }

        if (h >= 0 && h != oldh && h != mLastDimensionH) {
            yMultiplyFactor = (float) h / mLastDimensionH;
            mLastDimensionH = h;
        }

        multiplyPathsAndPoints(xMultiplyFactor, yMultiplyFactor);
    }

    // Translate all the paths, used every time that this view size is changed
    @SuppressWarnings("SuspiciousNameCombination")
    private void multiplyPathsAndPoints(float xMultiplyFactor, float yMultiplyFactor) {

        // If both factors == 1 or <= 0 or no paths/points to apply things, just return
        if ((xMultiplyFactor == 1 && yMultiplyFactor == 1)
                || (xMultiplyFactor <= 0 || yMultiplyFactor <= 0) ||
                (mPaths.size() == 0 && mCanceledPaths.size() == 0 && mPoints.size() == 0)) {
            return;
        }

        if (mResizeBehaviour == ResizeBehaviour.CLEAR) {// If clear, clear all and return
            mPaths = new ArrayList<>();
            mCanceledPaths = new ArrayList<>();
            mPoints = new ArrayList<>();
            return;
        } else if (mResizeBehaviour == ResizeBehaviour.CROP) {
            xMultiplyFactor = yMultiplyFactor = 1;
        }

        // Adapt drawn paths
        for (HistoryPath historyPath : mPaths) {

            if (historyPath.isPoint()) {
                historyPath.setOriginX(historyPath.getOriginX() * xMultiplyFactor);
                historyPath.setOriginY(historyPath.getOriginY() * yMultiplyFactor);
            } else {
                for (Point point : historyPath.getPoints()) {
                    point.x *= xMultiplyFactor;
                    point.y *= yMultiplyFactor;
                }
            }

            historyPath.generatePath();
        }

        // Adapt canceled paths
        for (HistoryPath historyPath : mCanceledPaths) {

            if (historyPath.isPoint()) {
                historyPath.setOriginX(historyPath.getOriginX() * xMultiplyFactor);
                historyPath.setOriginY(historyPath.getOriginY() * yMultiplyFactor);
            } else {
                for (Point point : historyPath.getPoints()) {
                    point.x *= xMultiplyFactor;
                    point.y *= yMultiplyFactor;
                }
            }

            historyPath.generatePath();
        }

        // Adapt drawn points
        for (Point point : mPoints) {
            point.x *= xMultiplyFactor;
            point.y *= yMultiplyFactor;
        }
    }

    public interface DrawCreatorListener {
        void onDrawCreated(Bitmap draw);

        void onDrawCreationError();
    }


    private class TakeScreenShotAsyncTask extends AsyncTask<Void, Void, Void> {
        private int mWidth, mHeight;
        private Canvas mCanvas;
        private Bitmap mBitmap;
        private DrawCreatorListener mListener;

        public TakeScreenShotAsyncTask(@NonNull DrawCreatorListener listener) {
            mListener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mWidth = getWidth();
            mHeight = getHeight();
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                mBitmap = Bitmap.createBitmap(
                        mWidth, mHeight, Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(mBitmap);
            } catch (Exception e) {
                e.printStackTrace();
                cancel(true);
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            if (mListener != null) {
                mListener.onDrawCreationError();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            draw(mCanvas);

            if (mListener != null) {
                mListener.onDrawCreated(mBitmap);
            }
        }
    }
}
