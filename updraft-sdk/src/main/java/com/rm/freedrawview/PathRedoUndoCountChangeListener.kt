package com.rm.freedrawview

interface PathRedoUndoCountChangeListener {
    fun onUndoCountChanged(undoCount: Int)

    fun onRedoCountChanged(redoCount: Int)
}
