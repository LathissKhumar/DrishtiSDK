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

import io.drishti.core.Frame
import io.drishti.core.FrameFormat
import kotlin.test.*

class FeatureExtractorTest {

    private val extractor = FeatureExtractor()
    private val emptyFrame = Frame(width = 1, height = 1, format = FrameFormat.RGB_888, data = null)

    /** Frame with all-zero (black) pixel data — uniform, no edges. */
    private val blackFrame = Frame(
        width = 640, height = 480,
        format = FrameFormat.RGB_888,
        data = ByteArray(640 * 480 * 3)
    )

    // --- Existing tests (uniform black → empty results) ---

    @Test
    fun extractContoursReturnsEmptyForBlackFrame() {
        val contours = extractor.extractContours(blackFrame)
        assertNotNull(contours)
        assertTrue(contours.isEmpty())
    }

    @Test
    fun extractLinesReturnsEmptyForBlackFrame() {
        val lines = extractor.extractLines(blackFrame)
        assertNotNull(lines)
        assertTrue(lines.isEmpty())
    }

    @Test
    fun extractTextRegionsReturnsEmptyForBlackFrame() {
        val regions = extractor.extractTextRegions(blackFrame)
        assertNotNull(regions)
        assertTrue(regions.isEmpty())
    }

    @Test
    fun extractROIsReturnsEmptyForBlackFrame() {
        val rois = extractor.extractROIs(blackFrame)
        assertNotNull(rois)
        assertTrue(rois.isEmpty())
    }

    // --- Empty frame (null data) tests ---

    @Test
    fun extractContoursWorksWithNullData() {
        val contours = extractor.extractContours(emptyFrame)
        assertTrue(contours.isEmpty())
    }

    @Test
    fun extractLinesWorksWithNullData() {
        val lines = extractor.extractLines(emptyFrame)
        assertTrue(lines.isEmpty())
    }

    @Test
    fun extractTextRegionsWorksWithNullData() {
        val regions = extractor.extractTextRegions(emptyFrame)
        assertTrue(regions.isEmpty())
    }

    @Test
    fun extractROIsWorksWithNullData() {
        val rois = extractor.extractROIs(emptyFrame)
        assertTrue(rois.isEmpty())
    }

    // --- New tests: real feature extraction from frames with actual content ---

    @Test
    fun extractContoursFromFrameWithVerticalEdge() {
        // Create a frame with a sharp vertical edge at x=160: left half white, right half black
        val width = 320
        val height = 240
        val data = ByteArray(width * height * 3)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = (y * width + x) * 3
                val brightness: Byte = if (x < 160) 0xFF.toByte() else 0x00
                data[offset] = brightness
                data[offset + 1] = brightness
                data[offset + 2] = brightness
            }
        }
        val frame = Frame(width = width, height = height, format = FrameFormat.RGB_888, data = data)

        val contours = extractor.extractContours(frame)
        // A strong vertical edge should produce at least one contour
        assertTrue(contours.isNotEmpty(), "Should detect contours from vertical edge")
    }

    @Test
    fun extractLinesFromFrameWithDiagonalLine() {
        // Create a frame with a diagonal white line on black background
        val width = 200
        val height = 200
        val data = ByteArray(width * height * 3)
        // Draw a diagonal line
        for (i in 0 until 200) {
            val offset = (i * width + i) * 3
            if (offset + 2 < data.size) {
                data[offset] = 0xFF.toByte()
                data[offset + 1] = 0xFF.toByte()
                data[offset + 2] = 0xFF.toByte()
            }
        }
        val frame = Frame(width = width, height = height, format = FrameFormat.RGB_888, data = data)

        val lines = extractor.extractLines(frame)
        // Diagonal line should produce at least one detected line segment
        assertTrue(lines.isNotEmpty(), "Should detect lines from diagonal pattern")
    }

    @Test
    fun extractTextRegionsFromFrameWithHorizontalBands() {
        // Create a frame with horizontal text-like bands (high edge density rows)
        val width = 300
        val height = 100
        val data = ByteArray(width * height * 3)

        // Row 20-25: dense edges (text-like)
        for (y in 20..25) {
            for (x in 0 until width step 2) {
                val offset = (y * width + x) * 3
                if (offset + 2 < data.size) {
                    data[offset] = 0xFF.toByte()
                    data[offset + 1] = 0xFF.toByte()
                    data[offset + 2] = 0xFF.toByte()
                }
            }
        }

        val frame = Frame(width = width, height = height, format = FrameFormat.RGB_888, data = data)
        val regions = extractor.extractTextRegions(frame)
        // Horizontal bands of edges should be detected as text regions
        assertTrue(regions.isNotEmpty(), "Should detect text regions from horizontal bands")
    }

    @Test
    fun extractROIsFromFrameWithHighContrastRegion() {
        val width = 200
        val height = 200
        val data = ByteArray(width * height * 3)

        for (i in data.indices step 3) {
            data[i] = 128.toByte()
            data[i + 1] = 128.toByte()
            data[i + 2] = 128.toByte()
        }

        for (y in 96..103) {
            for (x in 96..103) {
                val offset = (y * width + x) * 3
                data[offset] = 0xFF.toByte()
                data[offset + 1] = 0xFF.toByte()
                data[offset + 2] = 0xFF.toByte()
            }
        }
        val frame = Frame(width = width, height = height, format = FrameFormat.RGB_888, data = data)

        val rois = extractor.extractROIs(frame)
        assertNotNull(rois)
    }

    @Test
    fun extractContoursFromFrameWithGradient() {
        // Create a frame with a horizontal gradient (smooth transition)
        val width = 200
        val height = 100
        val data = ByteArray(width * height * 3)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = (y * width + x) * 3
                val gray = (x * 255 / width).toByte()
                data[offset] = gray
                data[offset + 1] = gray
                data[offset + 2] = gray
            }
        }
        val frame = Frame(width = width, height = height, format = FrameFormat.RGB_888, data = data)

        val contours = extractor.extractContours(frame)
        // Smooth gradient has subtle edges; may or may not produce contours
        // Just verify it doesn't crash and returns a list
        assertNotNull(contours)
    }

    // --- Data class tests ---

    @Test
    fun contourDataClass() {
        val contour = Contour(
            points = listOf(
                io.drishti.core.Point(0f, 0f),
                io.drishti.core.Point(10f, 0f),
                io.drishti.core.Point(10f, 10f)
            ),
            area = 100f
        )
        assertEquals(3, contour.points.size)
        assertEquals(100f, contour.area)
    }

    @Test
    fun lineDataClass() {
        val line = Line(
            start = io.drishti.core.Point(0f, 0f),
            end = io.drishti.core.Point(10f, 10f),
            angle = 45f
        )
        assertEquals(0f, line.start.x)
        assertEquals(10f, line.end.x)
        assertEquals(45f, line.angle)
    }

    @Test
    fun textRegionDataClass() {
        val region = TextRegion(
            boundingBox = io.drishti.core.BoundingBox(x = 10f, y = 20f, width = 100f, height = 30f),
            text = "Hello"
        )
        assertEquals("Hello", region.text)
        assertEquals(10f, region.boundingBox.x)
    }

    @Test
    fun regionOfInterestDataClass() {
        val roi = RegionOfInterest(
            boundingBox = io.drishti.core.BoundingBox(x = 5f, y = 5f, width = 50f, height = 50f),
            confidence = 0.95f
        )
        assertEquals(0.95f, roi.confidence)
        assertEquals(50f, roi.boundingBox.width)
    }

    @Test
    fun extractContoursEqualityEngine() {
        val frame = Frame(width = 320, height = 240, format = FrameFormat.RGB_888)
        val contours = extractor.extractContours(frame)
        assertTrue(contours.isEmpty())
    }
}
