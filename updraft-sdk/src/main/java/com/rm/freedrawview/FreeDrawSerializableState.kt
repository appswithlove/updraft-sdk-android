package com.rm.freedrawview

import java.io.Serializable

class FreeDrawSerializableState(
    canceledPaths: ArrayList<HistoryPath>?,
    paths: ArrayList<HistoryPath>?,
    var paintColor: Int,
    var paintAlpha: Int,
    paintWidth: Float,
    var resizeBehaviour: ResizeBehaviour?,
    lastW: Int,
    lastH: Int
) : Serializable {

    var canceledPaths: ArrayList<HistoryPath> = canceledPaths ?: ArrayList()
    var paths: ArrayList<HistoryPath> = paths ?: ArrayList()
    var paintWidth: Float = if (paintWidth >= 0) paintWidth else 0f
    var lastDimensionW: Int = if (lastW >= 0) lastW else 0
    var lastDimensionH: Int = if (lastH >= 0) lastH else 0
}
