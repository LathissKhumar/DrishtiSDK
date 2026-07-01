/*
 * Copyright 2026 DrishtiSTEM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.drishti.audio

import io.drishti.core.Point

/**
 * Maps 2D positions to audio spatial coordinates.
 */
public class AudioSpatialMapper {
    /**
     * Map point to audio coordinates.
     *
     * Normalizes pixel coordinates to the [0, 1] range by dividing by viewport dimensions.
     * The z-axis represents depth proximity: 1.0 at surface, decreasing with depth.
     *
     * @param point Pixel position in viewport coordinates.
     * @param width Viewport width in pixels.
     * @param height Viewport height in pixels.
     * @param depth Depth layer (0 = surface, higher = farther).
     * @return Audio coordinate with x, y in [0, 1] and z in (0, 1].
     */
    public fun mapToAudio(point: Point, width: Float, height: Float, depth: Int = 0): AudioCoordinate {
        require(width > 0f) { "Viewport width must be positive, was $width" }
        require(height > 0f) { "Viewport height must be positive, was $height" }
        require(depth >= 0) { "Depth must be non-negative, was $depth" }
        return AudioCoordinate(
            x = (point.x / width).coerceIn(0f, 1f),
            y = (point.y / height).coerceIn(0f, 1f),
            z = (1.0f / (1.0f + depth)).coerceIn(0f, 1f)
        )
    }

    /**
     * Map bounding box to audio region.
     *
     * Normalizes bounding box coordinates to the [0, 1] range.
     *
     * @param x Left edge of bounding box in pixels.
     * @param y Top edge of bounding box in pixels.
     * @param width Bounding box width in pixels.
     * @param height Bounding box height in pixels.
     * @return Audio region with all edges normalized to [0, 1].
     */
    public fun mapRegion(x: Float, y: Float, width: Float, height: Float): AudioRegion {
        require(width > 0f) { "Region width must be positive, was $width" }
        require(height > 0f) { "Region height must be positive, was $height" }
        return AudioRegion(
            left = (x / width).coerceIn(0f, 1f),
            top = (y / height).coerceIn(0f, 1f),
            right = ((x + width) / width).coerceIn(0f, 1f),
            bottom = ((y + height) / height).coerceIn(0f, 1f)
        )
    }

    /**
     * Calculate distance between two audio coordinates.
     */
    public fun distance(a: AudioCoordinate, b: AudioCoordinate): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Compute intensity/amplitude falloff based on distance using inverse-square law.
     */
    public fun computeFalloff(distance: Float, referenceDistance: Float = 1.0f): Float {
        if (distance <= referenceDistance) return 1.0f
        return (referenceDistance * referenceDistance / (distance * distance)).coerceIn(0f, 1f)
    }
}

public data class AudioCoordinate(
    val x: Float,
    val y: Float,
    val z: Float
)

public data class AudioRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
