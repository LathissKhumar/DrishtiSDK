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

class ImagePreprocessorTest {

    private val preprocessor = ImagePreprocessor()
    private val testFrame = Frame(width = 100, height = 100, format = FrameFormat.RGB_888, data = ByteArray(30000))

    @Test
    fun grayscaleReturnsProcessedFrame() {
        val result = preprocessor.grayscale(testFrame)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
        assertEquals(ProcessedFormat.GRAYSCALE, result.format)
        assertNotNull(result.data)
        assertEquals(100 * 100, result.data!!.size)
    }

    @Test
    fun enhanceContrastReturnsProcessedFrame() {
        val result = preprocessor.enhanceContrast(testFrame)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
        assertEquals(ProcessedFormat.ENHANCED, result.format)
    }

    @Test
    fun reduceNoiseReturnsProcessedFrame() {
        val result = preprocessor.reduceNoise(testFrame)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
        assertEquals(ProcessedFormat.DENOISED, result.format)
    }

    @Test
    fun reduceNoiseRespectsKernelSize() {
        val result = preprocessor.reduceNoise(testFrame, kernelSize = 3)
        assertEquals(ProcessedFormat.DENOISED, result.format)
    }

    @Test
    fun detectEdgesReturnsProcessedFrame() {
        val result = preprocessor.detectEdges(testFrame)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
        assertEquals(ProcessedFormat.EDGES, result.format)
    }

    @Test
    fun binarizeReturnsProcessedFrame() {
        val result = preprocessor.binarize(testFrame)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
        assertEquals(ProcessedFormat.BINARY, result.format)
    }

    @Test
    fun preprocessChainsOperations() {
        val result = preprocessor.preprocess(testFrame)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
        assertEquals(ProcessedFormat.BINARY, result.format)
    }

    @Test
    fun processedFrameToFrameConvertsCorrectly() {
        val processed = ProcessedFrame(width = 200, height = 150, data = testFrame.data, format = ProcessedFormat.GRAYSCALE)
        val frame = processed.toFrame()
        assertEquals(200, frame.width)
        assertEquals(150, frame.height)
        assertEquals(FrameFormat.GRAYSCALE, frame.format)
        assertEquals(testFrame.data, frame.data)
    }

    @Test
    fun processedFrameToFramePreservesNullData() {
        val processed = ProcessedFrame(width = 50, height = 50, data = null, format = ProcessedFormat.EDGES)
        val frame = processed.toFrame()
        assertNull(frame.data)
        assertEquals(50, frame.width)
        assertEquals(50, frame.height)
    }

    @Test
    fun processedImageCanBeReprocessed() {
        val grayscale = preprocessor.grayscale(testFrame)
        val enhanced = preprocessor.enhanceContrast(grayscale.toFrame())
        val denoised = preprocessor.reduceNoise(enhanced.toFrame())
        val edges = preprocessor.detectEdges(denoised.toFrame())
        assertEquals(ProcessedFormat.EDGES, edges.format)
    }

    @Test
    fun processedFormatValues() {
        val formats = ProcessedFormat.values()
        assertEquals(5, formats.size)
        assertTrue(formats.contains(ProcessedFormat.GRAYSCALE))
        assertTrue(formats.contains(ProcessedFormat.ENHANCED))
        assertTrue(formats.contains(ProcessedFormat.DENOISED))
        assertTrue(formats.contains(ProcessedFormat.EDGES))
        assertTrue(formats.contains(ProcessedFormat.BINARY))
    }
}
