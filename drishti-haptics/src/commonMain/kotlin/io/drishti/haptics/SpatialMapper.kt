package io.drishti.haptics

import io.drishti.core.Point

/**
 * Maps 2D positions to haptic spatial output.
 */
class SpatialMapper {
    /**
     * Map point to haptic coordinates.
     */
    fun mapToHaptic(point: Point, width: Float, height: Float, depth: Int = 0): HapticCoordinate {
        val zVal = if (depth == 0) 0.5f else (0.5f / (1.0f + depth)).coerceIn(0f, 1f)
        return HapticCoordinate(
            x = (point.x / width).coerceIn(0f, 1f),
            y = (point.y / height).coerceIn(0f, 1f),
            z = zVal
        )
    }

    /**
     * Compute intensity falloff based on distance using inverse-square law.
     */
    fun computeFalloff(distance: Float, referenceDistance: Float = 1.0f): Float {
        if (distance <= referenceDistance) return 1.0f
        return (referenceDistance * referenceDistance / (distance * distance)).coerceIn(0f, 1f)
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
