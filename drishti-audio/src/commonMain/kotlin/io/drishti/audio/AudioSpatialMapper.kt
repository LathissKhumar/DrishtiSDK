package io.drishti.audio

import io.drishti.core.Point

/**
 * Maps 2D positions to audio spatial coordinates.
 */
class AudioSpatialMapper {
    /**
     * Map point to audio coordinates.
     */
    fun mapToAudio(point: Point, width: Float, height: Float, depth: Int = 0): AudioCoordinate {
        return AudioCoordinate(
            x = (point.x / width).coerceIn(-1f, 1f),
            y = (point.y / height).coerceIn(-1f, 1f),
            z = (1.0f / (1.0f + depth)).coerceIn(0f, 1f)
        )
    }

    /**
     * Map bounding box to audio region.
     */
    fun mapRegion(x: Float, y: Float, width: Float, height: Float): AudioRegion {
        return AudioRegion(
            left = (x / width).coerceIn(-1f, 1f),
            top = (y / height).coerceIn(-1f, 1f),
            right = ((x + width) / width).coerceIn(-1f, 1f),
            bottom = ((y + height) / height).coerceIn(-1f, 1f)
        )
    }

    /**
     * Calculate distance between two audio coordinates.
     */
    fun distance(a: AudioCoordinate, b: AudioCoordinate): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Compute intensity/amplitude falloff based on distance using inverse-square law.
     */
    fun computeFalloff(distance: Float, referenceDistance: Float = 1.0f): Float {
        if (distance <= referenceDistance) return 1.0f
        return (referenceDistance * referenceDistance / (distance * distance)).coerceIn(0f, 1f)
    }
}

data class AudioCoordinate(
    val x: Float,
    val y: Float,
    val z: Float
)

data class AudioRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
