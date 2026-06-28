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
data class VisionFeatures(
    val contours: List<Contour> = emptyList(),
    val lines: List<Line> = emptyList(),
    val textRegions: List<TextRegion> = emptyList(),
    val regionsOfInterest: List<RegionOfInterest> = emptyList(),
    val shapes: List<DetectedShape> = emptyList()
) {
    /** Whether any features were detected. */
    fun isNotEmpty(): Boolean =
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
data class DetectedShape(
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
enum class ShapeKind {
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
