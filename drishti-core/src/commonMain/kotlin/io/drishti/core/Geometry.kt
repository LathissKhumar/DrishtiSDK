package io.drishti.core

import kotlinx.serialization.Serializable

@Serializable
data class Point(val x: Float, val y: Float)

@Serializable
data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    init {
        require(width >= 0f) { "BoundingBox width must be non-negative, got $width" }
        require(height >= 0f) { "BoundingBox height must be non-negative, got $height" }
    }
}

@Serializable
data class Axes(
    val x: Axis = Axis(),
    val y: Axis = Axis()
)

@Serializable
data class Axis(
    val label: String = "",
    val range: ClosedFloatingPointRange<Float> = 0f..100f
)

@Serializable
data class DataPoint(
    val x: Float,
    val y: Float,
    val label: String? = null
)

@Serializable
data class TrendLine(
    val start: Point,
    val end: Point,
    val equation: String? = null
)

@Serializable
data class Geometry(
    val points: List<Point> = emptyList(),
    val boundingBox: BoundingBox? = null
)
