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
import io.drishti.core.Frame
import io.drishti.core.FrameFormat
import io.drishti.core.ShapeContent
import io.drishti.core.ShapeType
import kotlin.test.*

class VisionDetectorTest {

    private val detector = VisionDetector()
    private val renderer = VisionRenderer()
    private val plugin = VisionPlugin()
    private val extractor = FeatureExtractor()

    @Test
    fun detectReturnsNullForBlackFrame() = runTest {
        val frame = Frame(width = 100, height = 100, format = FrameFormat.RGB_888, data = ByteArray(30000))
        val result = detector.detect(frame)
        assertNull(result)
    }

    @Test
    fun detectAllReturnsEmptyForBlackFrame() {
        val frame = Frame(width = 100, height = 100, format = FrameFormat.RGB_888, data = ByteArray(30000))
        val results = detector.detectAll(frame)
        assertTrue(results.isEmpty())
    }

    @Test
    fun extractFeaturesReturnsEmptyForBlackFrame() {
        val frame = Frame(width = 100, height = 100, format = FrameFormat.RGB_888, data = ByteArray(30000))
        val features = detector.extractFeatures(frame)
        assertFalse(features.isNotEmpty())
    }

    @Test
    fun extractShapesClassifiesTriangle() {
        val frame = createTriangleFrame()
        val shapes = extractor.extractShapes(frame)
        assertTrue(shapes.isNotEmpty(), "Should detect at least one shape from triangle")

        val triangle = shapes.firstOrNull { it.type == ShapeKind.TRIANGLE }
        assertNotNull(triangle, "Should classify a triangle shape")
        assertEquals(ShapeKind.TRIANGLE, triangle.type)
        assertTrue(triangle.area > 0f, "Triangle area should be positive")
        assertTrue(triangle.perimeter > 0f, "Triangle perimeter should be positive")
        assertEquals(3, triangle.vertices.size, "Triangle should have 3 vertices")
    }

    @Test
    fun extractShapesClassifiesRectangle() {
        val frame = createRectangleFrame()
        val shapes = extractor.extractShapes(frame)
        assertTrue(shapes.isNotEmpty(), "Should detect at least one shape from rectangle")

        val rectangle = shapes.firstOrNull {
            it.type == ShapeKind.RECTANGLE || it.type == ShapeKind.SQUARE
        }
        assertNotNull(rectangle, "Should classify a rectangle/square shape")
        assertTrue(rectangle.area > 0f, "Rectangle area should be positive")
    }

    @Test
    fun extractAllReturnsMultipleFeatures() {
        val frame = createMixedContentFrame()
        val features = extractor.extractAll(frame)
        assertNotNull(features)
    }

    @Test
    fun visionDetectorDetectsShapes() {
        val frame = createTriangleFrame()
        val items = detector.detectAll(frame)
        assertTrue(items.isNotEmpty(), "VisionDetector should detect shapes")

        val shapeContent = items.filterIsInstance<ShapeContent>()
        assertTrue(shapeContent.isNotEmpty(), "Should produce ShapeContent items")
        assertEquals(ContentType.SHAPE, shapeContent.first().contentType)
    }

    @Test
    fun visionPluginDetectsShapes() = runTest {
        val frame = createTriangleFrame()
        val item = plugin.detect(frame)
        assertNotNull(item, "VisionPlugin should detect a shape from the triangle frame")
        assertEquals(ContentType.SHAPE, item.contentType)
    }

    @Test
    fun visionPluginDetectAllReturnsShapes() {
        val frame = createTriangleFrame()
        val items = plugin.detectAll(frame)
        assertTrue(items.isNotEmpty())
    }

    @Test
    fun visionRendererProducesVoiceOutput() {
        val frame = createTriangleFrame()
        val features = detector.extractFeatures(frame)
        val voice = renderer.renderVoice(features)
        assertTrue(voice.speech.text.isNotEmpty(), "Voice output should have text")
    }

    @Test
    fun visionRendererProducesHapticOutput() {
        val frame = createTriangleFrame()
        val features = detector.extractFeatures(frame)
        val haptic = renderer.renderHaptic(features)
        assertNotNull(haptic)
    }

    @Test
    fun visionRendererProducesAudioOutput() {
        val frame = createTriangleFrame()
        val features = detector.extractFeatures(frame)
        val audio = renderer.renderAudio(features)
        assertNotNull(audio)
    }

    @Test
    fun visionRendererHandlesEmptyContent() {
        val voice = renderer.renderVoice(emptyList())
        assertTrue(voice.speech.text.contains("No visual content"))
    }

    @Test
    fun visionFeaturesIsNotEmptyReportsCorrectly() {
        val emptyFeatures = VisionFeatures()
        assertFalse(emptyFeatures.isNotEmpty())

        val nonEmptyFeatures = VisionFeatures(
            shapes = listOf(
                DetectedShape(
                    type = ShapeKind.TRIANGLE,
                    vertices = emptyList(),
                    boundingBox = io.drishti.core.BoundingBox(0f, 0f, 10f, 10f),
                    area = 50f,
                    perimeter = 30f,
                    confidence = 0.8f
                )
            )
        )
        assertTrue(nonEmptyFeatures.isNotEmpty())
    }

    @Test
    fun detectedShapeDataClass() {
        val shape = DetectedShape(
            type = ShapeKind.CIRCLE,
            vertices = emptyList(),
            boundingBox = io.drishti.core.BoundingBox(0f, 0f, 100f, 100f),
            area = 7850f,
            perimeter = 314f,
            confidence = 0.88f
        )
        assertEquals(ShapeKind.CIRCLE, shape.type)
        assertEquals(7850f, shape.area)
    }

    private fun createTriangleFrame(): Frame {
        val width = 200
        val height = 200
        val data = ByteArray(width * height * 3)

        fillTriangle(data, width, height, 100, 30, 30, 170, 170, 170)

        return Frame(width = width, height = height, format = FrameFormat.RGB_888, data = data)
    }

    private fun createRectangleFrame(): Frame {
        val width = 200
        val height = 200
        val data = ByteArray(width * height * 3)

        for (y in 40..160) {
            for (x in 40..160) {
                setPixel(data, width, x, y)
            }
        }

        return Frame(width = width, height = height, format = FrameFormat.RGB_888, data = data)
    }

    private fun createMixedContentFrame(): Frame {
        val width = 300
        val height = 300
        val data = ByteArray(width * height * 3)

        drawLine(data, width, 50, 50, 250, 50)
        drawLine(data, width, 50, 100, 250, 100)
        drawLine(data, width, 50, 150, 250, 150)

        for (y in 200..205) {
            for (x in 50..250 step 2) {
                val offset = (y * width + x) * 3
                if (offset + 2 < data.size) {
                    data[offset] = 0xFF.toByte()
                    data[offset + 1] = 0xFF.toByte()
                    data[offset + 2] = 0xFF.toByte()
                }
            }
        }

        return Frame(width = width, height = height, format = FrameFormat.RGB_888, data = data)
    }

    private fun setPixel(data: ByteArray, imgWidth: Int, x: Int, y: Int) {
        val imgHeight = data.size / (imgWidth * 3)
        if (x in 0 until imgWidth && y in 0 until imgHeight) {
            val offset = (y * imgWidth + x) * 3
            if (offset + 2 < data.size) {
                data[offset] = 0xFF.toByte()
                data[offset + 1] = 0xFF.toByte()
                data[offset + 2] = 0xFF.toByte()
            }
        }
    }

    private fun fillTriangle(
        data: ByteArray, imgWidth: Int, imgHeight: Int,
        x0: Int, y0: Int, x1: Int, y1: Int, x2: Int, y2: Int
    ) {
        val minY = minOf(y0, y1, y2).coerceAtLeast(0)
        val maxY = maxOf(y0, y1, y2).coerceAtMost(imgHeight - 1)

        for (y in minY..maxY) {
            val intersections = mutableListOf<Int>()
            val edges = listOf(
                Pair(x0.toDouble(), y0.toDouble()) to Pair(x1.toDouble(), y1.toDouble()),
                Pair(x1.toDouble(), y1.toDouble()) to Pair(x2.toDouble(), y2.toDouble()),
                Pair(x2.toDouble(), y2.toDouble()) to Pair(x0.toDouble(), y0.toDouble())
            )
            for ((p1, p2) in edges) {
                if ((p1.second <= y && p2.second > y) || (p2.second <= y && p1.second > y)) {
                    val t = (y - p1.second) / (p2.second - p1.second)
                    val x = (p1.first + t * (p2.first - p1.first)).toInt()
                    intersections.add(x)
                }
            }
            intersections.sort()
            var i = 0
            while (i + 1 < intersections.size) {
                val xStart = intersections[i].coerceAtLeast(0)
                val xEnd = intersections[i + 1].coerceAtMost(imgWidth - 1)
                for (x in xStart..xEnd) {
                    setPixel(data, imgWidth, x, y)
                }
                i += 2
            }
        }
    }

    private fun drawLine(data: ByteArray, imgWidth: Int, x0: Int, y0: Int, x1: Int, y1: Int) {
        var x = x0
        var y = y0
        val dx = kotlin.math.abs(x1 - x0)
        val dy = -kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        val imgHeight = data.size / (imgWidth * 3)

        while (true) {
            if (x in 0 until imgWidth && y in 0 until imgHeight) {
                val offset = (y * imgWidth + x) * 3
                if (offset + 2 < data.size) {
                    data[offset] = 0xFF.toByte()
                    data[offset + 1] = 0xFF.toByte()
                    data[offset + 2] = 0xFF.toByte()
                }
            }
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                x += sx
            }
            if (e2 <= dx) {
                err += dx
                y += sy
            }
        }
    }
}

private fun runTest(block: suspend () -> Unit) {
    kotlinx.coroutines.test.runTest { block() }
}
