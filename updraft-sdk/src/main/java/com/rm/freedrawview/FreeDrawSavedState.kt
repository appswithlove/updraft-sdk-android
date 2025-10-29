package com.rm.freedrawview

import android.graphics.Paint
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IntRange

class FreeDrawSavedState(
    superState: Parcelable?,
    val paths: ArrayList<HistoryPath>,
    val canceledPaths: ArrayList<HistoryPath>,
    val paintWidth: Float,
    @ColorInt val paintColor: Int,
    @IntRange(from = 0, to = 255) val paintAlpha: Int,
    val resizeBehaviour: ResizeBehaviour,
    val lastDimensionW: Int,
    val lastDimensionH: Int
) : View.BaseSavedState(superState) {

    constructor(parcel: Parcel) : this(
        superState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Point::class.java.classLoader, Point::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(Point::class.java.classLoader)
        },
        paths = parcel.createTypedArrayList(HistoryPath.CREATOR) ?: arrayListOf(),
        canceledPaths = parcel.createTypedArrayList(HistoryPath.CREATOR) ?: arrayListOf(),
        paintWidth = parcel.readFloat(),
        paintColor = parcel.readInt(),
        paintAlpha = parcel.readInt(),
        resizeBehaviour = ResizeBehaviour.entries[parcel.readInt()],
        lastDimensionW = parcel.readInt(),
        lastDimensionH = parcel.readInt()
    )

    fun getCurrentPaint(): Paint {
        val paint = FreeDrawHelper.createPaint()
        FreeDrawHelper.setupStrokePaint(paint)
        FreeDrawHelper.copyFromValues(
            to = paint,
            color = paintColor,
            alpha = paintAlpha,
            strokeWidth = paintWidth,
            copyWidth = true
        )
        return paint
    }

    fun getCurrentPaintWidth(): Float = paintWidth

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeTypedList(paths)
        out.writeTypedList(canceledPaths)
        out.writeInt(paintColor)
        out.writeInt(paintAlpha)
        out.writeFloat(paintWidth)
        out.writeSerializable(resizeBehaviour)
        out.writeInt(lastDimensionW)
        out.writeInt(lastDimensionH)
    }

    companion object CREATOR : Parcelable.Creator<FreeDrawSavedState> {
        override fun createFromParcel(parcel: Parcel): FreeDrawSavedState =
            FreeDrawSavedState(parcel)

        override fun newArray(size: Int): Array<FreeDrawSavedState?> = arrayOfNulls(size)
    }
}
