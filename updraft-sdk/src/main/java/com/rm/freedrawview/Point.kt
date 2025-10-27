package com.rm.freedrawview

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class Point() : Parcelable, Serializable {

    companion object {
        private const val serialVersionUID = 42L

        @JvmField
        val CREATOR = object : Parcelable.Creator<Point> {
            override fun createFromParcel(parcel: Parcel): Point = Point(parcel)
            override fun newArray(size: Int): Array<Point?> = arrayOfNulls(size)
        }
    }

    var x: Float = -1f
    var y: Float = -1f

    constructor(x: Float, y: Float) : this() {
        this.x = x
        this.y = y
    }

    override fun toString(): String = "$x : $y - "

    private constructor(parcel: Parcel) : this() {
        x = parcel.readFloat()
        y = parcel.readFloat()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeFloat(x)
        dest.writeFloat(y)
    }

    override fun describeContents(): Int = 0
}
