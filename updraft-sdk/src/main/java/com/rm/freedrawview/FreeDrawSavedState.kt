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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Point::class.java.classLoader, Point::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(Point::class.java.classLoader)
        },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.createTypedArrayList(HistoryPath.CREATOR) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            parcel.createTypedArrayList(HistoryPath.CREATOR) ?: arrayListOf()
        },
        ArrayList<HistoryPath>().apply { parcel.readTypedList(this, HistoryPath.CREATOR) },
        parcel.readFloat(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readSerializable() as ResizeBehaviour,
        parcel.readInt(),
        parcel.readInt()
    )

    fun getCurrentPaint(): Paint {
        val paint = FreeDrawHelper.createPaint()
        FreeDrawHelper.setupStrokePaint(paint)
        FreeDrawHelper.copyFromValues(paint, paintColor, paintAlpha, paintWidth, true)
        return paint
    }

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
