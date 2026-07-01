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

package io.drishti.haptics

import io.drishti.core.Point

/**
 * Maps 2D positions to haptic spatial output.
 */
public class SpatialMapper {
    /**
     * Map point to haptic coordinates.
     */
    public fun mapToHaptic(point: Point, width: Float, height: Float, depth: Int = 0): HapticCoordinate {
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
    public fun computeFalloff(distance: Float, referenceDistance: Float = 1.0f): Float {
        if (distance <= referenceDistance) return 1.0f
        return (referenceDistance * referenceDistance / (distance * distance)).coerceIn(0f, 1f)
    }

    /**
     * Map bounding box to haptic region.
     */
    public fun mapRegion(x: Float, y: Float, width: Float, height: Float): HapticRegion {
        return HapticRegion(
            left = (x / width).coerceIn(0f, 1f),
            top = (y / height).coerceIn(0f, 1f),
            right = ((x + width) / width).coerceIn(0f, 1f),
            bottom = ((y + height) / height).coerceIn(0f, 1f)
        )
    }
}

public data class HapticCoordinate(
    val x: Float,
    val y: Float,
    val z: Float
)

public data class HapticRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
