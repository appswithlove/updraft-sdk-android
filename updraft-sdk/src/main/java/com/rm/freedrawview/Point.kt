package com.rm.freedrawview

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class Point() : Parcelable, Serializable {

    var x: Float = -1f
    var y: Float = -1f

    override fun toString(): String {
        return "$x : $y - "
    }

    private constructor(parcel: Parcel) : this() {
        x = parcel.readFloat()
        y = parcel.readFloat()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeFloat(x)
        dest.writeFloat(y)
    }

    companion object CREATOR : Parcelable.Creator<Point> {
        override fun createFromParcel(parcel: Parcel): Point = Point(parcel)
        override fun newArray(size: Int): Array<Point?> = arrayOfNulls(size)
    }
}
