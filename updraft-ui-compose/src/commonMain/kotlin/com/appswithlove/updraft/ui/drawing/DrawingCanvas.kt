package com.appswithlove.updraft.ui.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun DrawingCanvas(controller: DrawingController, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.pointerInput(controller) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: continue
                    controller.startPath(down.position)
                    down.consume()
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                break
                            }
                            controller.addPoint(change.position)
                            change.consume()
                        }
                    } finally {
                        controller.endPath()
                    }
                }
            }
        },
    ) {
        controller.paths.forEach { drawStroke(it.points, it.color, it.strokeWidthPx) }
        drawStroke(controller.currentStroke, controller.color, controller.strokeWidthPx)
    }
}

private fun DrawScope.drawStroke(points: List<Offset>, color: Color, strokeWidthPx: Float) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color, radius = strokeWidthPx / 2f, center = points.first())
        return
    }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(path, color, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
