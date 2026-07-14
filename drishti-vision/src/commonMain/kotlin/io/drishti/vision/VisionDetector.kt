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

import io.drishti.core.ContentType
import io.drishti.core.ContentItem
import io.drishti.core.DetectorPlugin
import io.drishti.core.Frame
import io.drishti.core.ShapeContent
import io.drishti.core.ShapeType

/** Vision-based detector for shapes using feature extraction. */
public class VisionDetector : DetectorPlugin {

    override val contentType: ContentType = ContentType.Shape
    override val confidence: Float = 0.85f

    private val extractor = FeatureExtractor()

    /** Detect a single shape from a frame. */
    override suspend fun detect(frame: Frame): ContentItem? {
        val shapes = extractor.extractShapes(frame)
        return shapes.maxByOrNull { it.confidence }?.toShapeContent()
    }

    /** Detect all shapes from a frame. */
    public fun detectAll(frame: Frame): List<ContentItem> {
        val features = extractor.extractAll(frame)
        return features.shapes.map { it.toShapeContent() }
    }

    public fun extractFeatures(frame: Frame): VisionFeatures {
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
            perimeter = this.perimeter,
            x = this.boundingBox.x,
            y = this.boundingBox.y,
            width = this.boundingBox.width,
            height = this.boundingBox.height,
            confidence = this.confidence
        )
    }
}
