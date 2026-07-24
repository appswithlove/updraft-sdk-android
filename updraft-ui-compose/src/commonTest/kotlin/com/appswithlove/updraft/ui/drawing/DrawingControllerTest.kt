package com.appswithlove.updraft.ui.drawing

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawingControllerTest {

    private fun DrawingController.drawStroke(points: List<Offset>) {
        startPath(points.first())
        points.drop(1).forEach { addPoint(it) }
        endPath()
    }

    @Test
    fun stroke_commitsPath() {
        val c = DrawingController()
        c.drawStroke(listOf(Offset(0f, 0f), Offset(10f, 10f)))
        assertEquals(1, c.paths.size)
        assertEquals(2, c.paths.first().points.size)
    }

    @Test
    fun undoRedo_roundTrips() {
        val c = DrawingController()
        c.drawStroke(listOf(Offset(0f, 0f), Offset(1f, 1f)))
        assertTrue(c.canUndo)
        c.undo()
        assertEquals(0, c.paths.size)
        assertTrue(c.canRedo)
        c.redo()
        assertEquals(1, c.paths.size)
    }

    @Test
    fun newStroke_clearsRedoStack() {
        val c = DrawingController()
        c.drawStroke(listOf(Offset(0f, 0f), Offset(1f, 1f)))
        c.undo()
        c.drawStroke(listOf(Offset(2f, 2f), Offset(3f, 3f)))
        assertFalse(c.canRedo)
        assertEquals(1, c.paths.size)
    }
}
