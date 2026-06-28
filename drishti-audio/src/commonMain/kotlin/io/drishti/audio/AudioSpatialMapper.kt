package io.drishti.audio

import io.drishti.core.Point

/**
 * Maps 2D positions to audio spatial coordinates.
 */
class AudioSpatialMapper {
    /**
     * Map point to audio coordinates.
     */
    fun mapToAudio(point: Point, width: Float, height: Float): AudioCoordinate {
        return AudioCoordinate(
            x = (point.x / width).coerceIn(-1f, 1f),
            y = (point.y / height).coerceIn(-1f, 1f),
            z = 0f
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
