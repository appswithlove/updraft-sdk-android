package com.appswithlove.updraft.ui.drawing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

class DrawnPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidthPx: Float,
)

class DrawingController(
    initialColor: Color = Color.Black,
    initialStrokeWidthPx: Float = 12f,
) {
    private val committedPaths = mutableStateListOf<DrawnPath>()
    private val redoStack = mutableStateListOf<DrawnPath>()
    private val currentPoints = mutableStateListOf<Offset>()

    val paths: List<DrawnPath> get() = committedPaths
    val currentStroke: List<Offset> get() = currentPoints

    var color by mutableStateOf(initialColor)
    var strokeWidthPx by mutableStateOf(initialStrokeWidthPx)

    val canUndo: Boolean get() = committedPaths.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun startPath(point: Offset) {
        currentPoints.clear()
        currentPoints.add(point)
    }

    fun addPoint(point: Offset) {
        currentPoints.add(point)
    }

    fun endPath() {
        if (currentPoints.isNotEmpty()) {
            committedPaths.add(DrawnPath(currentPoints.toList(), color, strokeWidthPx))
            redoStack.clear()
            currentPoints.clear()
        }
    }

    fun undo() {
        if (committedPaths.isNotEmpty()) {
            redoStack.add(committedPaths.removeAt(committedPaths.lastIndex))
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            committedPaths.add(redoStack.removeAt(redoStack.lastIndex))
        }
    }
}
