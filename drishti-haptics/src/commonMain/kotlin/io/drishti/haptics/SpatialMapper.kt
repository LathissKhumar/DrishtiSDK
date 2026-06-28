package io.drishti.haptics

import io.drishti.core.Point

/**
 * Maps 2D positions to haptic spatial output.
 */
class SpatialMapper {
    /**
     * Map point to haptic coordinates.
     */
    fun mapToHaptic(point: Point, width: Float, height: Float): HapticCoordinate {
        return HapticCoordinate(
            x = (point.x / width).coerceIn(0f, 1f),
            y = (point.y / height).coerceIn(0f, 1f),
            z = 0.5f
        )
    }

    /**
     * Map bounding box to haptic region.
     */
    fun mapRegion(x: Float, y: Float, width: Float, height: Float): HapticRegion {
        return HapticRegion(
            left = (x / width).coerceIn(0f, 1f),
            top = (y / height).coerceIn(0f, 1f),
            right = ((x + width) / width).coerceIn(0f, 1f),
            bottom = ((y + height) / height).coerceIn(0f, 1f)
        )
    }
}

data class HapticCoordinate(
    val x: Float,
    val y: Float,
    val z: Float
)

data class HapticRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
