package com.rm.freedrawview

import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class HistoryPath(
    var points: ArrayList<Point>,
    paint: Paint
) : Parcelable, Serializable {

    companion object {
        private const val serialVersionUID = 41L
        private const val TAG = "HistoryPath"

        @JvmField
        val CREATOR = object : Parcelable.Creator<HistoryPath> {
            override fun createFromParcel(parcel: Parcel): HistoryPath {
                return HistoryPath(parcel)
            }

            override fun newArray(size: Int): Array<HistoryPath?> {
                return arrayOfNulls(size)
            }
        }
    }

    var paintColor: Int = paint.color
    var paintAlpha: Int = paint.alpha
    var paintWidth: Float = paint.strokeWidth
    var originX: Float = points[0].x.toFloat()
    var originY: Float = points[0].y.toFloat()
    var isPoint: Boolean = FreeDrawHelper.isAPoint(points)

    @Transient
    var pathObj: Path? = null

    @Transient
    var paintObj: Paint? = null

    init {
        generatePath()
        generatePaint()
    }

    fun generatePath() {
        pathObj = Path()
        points.let {
            var first = true
            for (point in it) {
                if (first) {
                    pathObj?.moveTo(point.x, point.y)
                    first = false
                } else {
                    pathObj?.lineTo(point.x, point.y)
                }
            }
        }
    }

    private fun generatePaint() {
        paintObj = FreeDrawHelper.createPaintAndInitialize(paintColor, paintAlpha, paintWidth, isPoint)
    }

    fun getPath(): Path = pathObj ?: run {
        generatePath()
        pathObj!!
    }

    fun getPaint(): Paint = paintObj ?: run {
        generatePaint()
        paintObj!!
    }

    // Parcelable constructor
    private constructor(parcel: Parcel) : this(
        ArrayList<Point>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                parcel.readParcelableList(this, Point::class.java.classLoader, Point::class.java)
            } else {
                @Suppress("DEPRECATION")
                parcel.readTypedList(this, Point.CREATOR)
            }
        },
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

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedList(points)
        dest.writeInt(paintColor)
        dest.writeInt(paintAlpha)
        dest.writeFloat(paintWidth)
        dest.writeFloat(originX)
        dest.writeFloat(originY)
        dest.writeByte(if (isPoint) 1 else 0)
    }

    override fun describeContents(): Int = 0

    override fun toString(): String {
        return "Point: $isPoint\nPoints: $points\nColor: $paintColor\nAlpha: $paintAlpha\nWidth: $paintWidth"
    }
}
