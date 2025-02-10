package com.example.editor_app_intern.customeview

import android.graphics.Path
import android.graphics.PathMeasure

class DrawingPath(var color: Int, var strokeWidth: Int, var path: Path) {
    fun toPathString(): String {
        val pathMeasure = PathMeasure(path, false)
        val points = mutableListOf<String>()
        val length = pathMeasure.length
        var distance = 0f
        val step = 1f

        while (distance < length) {
            val pos = FloatArray(2)
            pathMeasure.getPosTan(distance, pos, null)
            points.add("${pos[0]},${pos[1]}")
            distance += step
        }

        return points.joinToString(";")
    }

    companion object {
        fun fromPathString(pathString: String): Path {
            val path = Path()
            val points = pathString.split(";")

            if (points.isNotEmpty() && points.all { it.isNotEmpty() }) {
                val firstPoint = points[0].split(",")
                if (firstPoint.size == 2) {
                    val startX = firstPoint[0].toFloatOrNull() ?: throw IllegalArgumentException("Invalid float value for X coordinate")
                    val startY = firstPoint[1].toFloatOrNull() ?: throw IllegalArgumentException("Invalid float value for Y coordinate")
                    path.moveTo(startX, startY)

                    for (point in points) {
                        val coords = point.split(",")
                        if (coords.size == 2) {
                            val x = coords[0].toFloatOrNull() ?: throw IllegalArgumentException("Invalid float value for X coordinate")
                            val y = coords[1].toFloatOrNull() ?: throw IllegalArgumentException("Invalid float value for Y coordinate")
                            path.lineTo(x, y)
                        }
                    }
                }
            }

            return path
        }
    }
}