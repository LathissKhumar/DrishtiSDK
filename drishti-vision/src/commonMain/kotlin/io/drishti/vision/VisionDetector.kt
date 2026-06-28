package io.drishti.vision

import io.drishti.core.ContentType
import io.drishti.core.ContentItem
import io.drishti.core.DetectorPlugin
import io.drishti.core.Frame
import io.drishti.core.ShapeContent
import io.drishti.core.ShapeType

class VisionDetector : DetectorPlugin {

    override val contentType = ContentType.SHAPE
    override val confidence = 0.85f

    private val extractor = FeatureExtractor()

    override suspend fun detect(frame: Frame): ContentItem? {
        val shapes = extractor.extractShapes(frame)
        return shapes.maxByOrNull { it.confidence }?.toShapeContent()
    }

    fun detectAll(frame: Frame): List<ContentItem> {
        val features = extractor.extractAll(frame)
        return features.shapes.map { it.toShapeContent() }
    }

    fun extractFeatures(frame: Frame): VisionFeatures {
        return extractor.extractAll(frame)
    }

    private fun DetectedShape.toShapeContent(): ShapeContent {
        val shapeType = when (this.type) {
            ShapeKind.TRIANGLE -> ShapeType.TRIANGLE
            ShapeKind.RECTANGLE, ShapeKind.SQUARE -> ShapeType.RECTANGLE
            ShapeKind.CIRCLE -> ShapeType.CIRCLE
            ShapeKind.ELLIPSE -> ShapeType.ELLIPSE
            ShapeKind.LINE_SEGMENT -> ShapeType.LINE
            ShapeKind.POLYGON, ShapeKind.PENTAGON, ShapeKind.HEXAGON -> ShapeType.POLYGON
            ShapeKind.UNKNOWN -> ShapeType.UNKNOWN
        }
        return ShapeContent(
            shapeType = shapeType,
            area = this.area,
            perimeter = this.perimeter
        )
    }
}
