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

package io.drishti.vision

import io.drishti.core.BoundingBox
import io.drishti.core.Point

/**
 * Aggregated features extracted from an image frame.
 *
 * Collects all detected visual elements: contours, lines, text regions,
 * regions of interest, and classified shapes. Produced by
 * [FeatureExtractor.extractAll].
 */
public data class VisionFeatures(
    val contours: List<Contour> = emptyList(),
    val lines: List<Line> = emptyList(),
    val textRegions: List<TextRegion> = emptyList(),
    val regionsOfInterest: List<RegionOfInterest> = emptyList(),
    val shapes: List<DetectedShape> = emptyList()
) {
    /** Whether any features were detected. */
    public fun isNotEmpty(): Boolean =
        contours.isNotEmpty() || lines.isNotEmpty() || textRegions.isNotEmpty() ||
            regionsOfInterest.isNotEmpty() || shapes.isNotEmpty()
}

/**
 * A classified geometric shape detected in an image.
 *
 * Shapes are classified by polygon simplification of contour outlines
 * and circularity analysis.
 *
 * @param type The classified shape type.
 * @param vertices Simplified polygon vertices defining the shape outline.
 * @param boundingBox Axis-aligned bounding rectangle enclosing the shape.
 * @param area Enclosed area in square pixels.
 * @param perimeter Total perimeter length in pixels.
 * @param confidence Detection confidence between 0.0 and 1.0.
 */
public data class DetectedShape(
    val type: ShapeKind,
    val vertices: List<Point>,
    val boundingBox: BoundingBox,
    val area: Float,
    val perimeter: Float,
    val confidence: Float
)

/**
 * Classification of detected geometric shapes.
 *
 * Classification uses vertex count from polygon simplification
 * and circularity ratio (4π × area / perimeter²).
 */
public enum class ShapeKind {
    TRIANGLE,
    RECTANGLE,
    SQUARE,
    PENTAGON,
    HEXAGON,
    POLYGON,
    CIRCLE,
    ELLIPSE,
    LINE_SEGMENT,
    UNKNOWN
}
