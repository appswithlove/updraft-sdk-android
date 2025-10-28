package com.rm.freedrawview

import android.graphics.Paint
import android.graphics.Path
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class HistoryPath(
    points: ArrayList<Point>,
    paint: Paint
) : Parcelable, Serializable {

    companion object {
        private const val serialVersionUID = 41L

        @JvmField
        val CREATOR: Parcelable.Creator<HistoryPath> = object : Parcelable.Creator<HistoryPath> {
            override fun createFromParcel(parcel: Parcel): HistoryPath = HistoryPath(parcel)
            override fun newArray(size: Int): Array<HistoryPath?> = arrayOfNulls(size)
        }
    }

    var points: ArrayList<Point> = ArrayList(points)
    var paintColor: Int = paint.color
    var paintAlpha: Int = paint.alpha
    var paintWidth: Float = paint.strokeWidth
    var originX: Float = points[0].x
    var originY: Float = points[0].y
    var isPoint: Boolean = FreeDrawHelper.isAPoint(points)

    @Transient
    private var path: Path? = null

    @Transient
    private var paintObj: Paint? = null

    init {
        generatePath()
        generatePaint()
    }

    fun generatePath() {
        path = Path()
        points.forEachIndexed { index, point ->
            if (index == 0) path?.moveTo(point.x, point.y)
            else path?.lineTo(point.x, point.y)
        }
    }

    private fun generatePaint() {
        paintObj =
            FreeDrawHelper.createPaintAndInitialize(paintColor, paintAlpha, paintWidth, isPoint)
    }

    fun getPath(): Path {
        if (path == null) generatePath()
        return path!!
    }

    fun getPaint(): Paint {
        if (paintObj == null) generatePaint()
        return paintObj!!
    }

    private constructor(parcel: Parcel) : this(
        ArrayList<Point>().apply { parcel.readTypedList(this, Point.CREATOR) },
        Paint()
    ) {
        paintColor = parcel.readInt()
        paintAlpha = parcel.readInt()
        paintWidth = parcel.readFloat()
        originX = parcel.readFloat()
        originY = parcel.readFloat()
        isPoint = parcel.readByte() != 0.toByte()

        generatePath()
        generatePaint()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(points)
        parcel.writeInt(paintColor)
        parcel.writeInt(paintAlpha)
        parcel.writeFloat(paintWidth)
        parcel.writeFloat(originX)
        parcel.writeFloat(originY)
        parcel.writeByte(if (isPoint) 1 else 0)
    }

    override fun describeContents(): Int = 0

    override fun toString(): String {
        return "Point: $isPoint\nPoints: $points\nColor: $paintColor\nAlpha: $paintAlpha\nWidth: $paintWidth"
    }
}
