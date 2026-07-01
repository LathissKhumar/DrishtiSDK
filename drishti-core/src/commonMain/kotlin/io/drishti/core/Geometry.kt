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

package io.drishti.core

import kotlinx.serialization.Serializable

@Serializable
public data class Point(val x: Float, val y: Float)

@Serializable
public data class BoundingBox(
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
public data class Axes(
    val x: Axis = Axis(),
    val y: Axis = Axis()
)

@Serializable
public data class Axis(
    val label: String = "",
    val range: ClosedFloatingPointRange<Float> = 0f..100f
)

@Serializable
public data class DataPoint(
    val x: Float,
    val y: Float,
    val label: String? = null
)

@Serializable
public data class TrendLine(
    val start: Point,
    val end: Point,
    val equation: String? = null
)

@Serializable
public data class Geometry(
    val points: List<Point> = emptyList(),
    val boundingBox: BoundingBox? = null
)
