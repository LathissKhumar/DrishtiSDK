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
import kotlin.math.sqrt

/**
 * Shared vision preprocessing pipeline.
 */
public class ImagePreprocessor {

    /**
     * Extracts grayscale bytes from a frame regardless of source format.
     * Handles RGB_888 (3 bytes/pixel), YUV_420_888 (Y plane), and already-grayscale data.
     */
    private fun toGrayscaleBytes(frame: Frame): ByteArray {
        val data = frame.data ?: return ByteArray(0)
        val pixelCount = frame.width * frame.height

        // Already 1 byte per pixel (e.g., from prior grayscale step)
        if (data.size == pixelCount) return data

        return when (frame.format) {
            FrameFormat.RGB_888 -> {
                val gray = ByteArray(pixelCount)
                for (i in 0 until pixelCount) {
                    val r = data[i * 3].toInt() and 0xFF
                    val g = data[i * 3 + 1].toInt() and 0xFF
                    val b = data[i * 3 + 2].toInt() and 0xFF
                    gray[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt().toByte()
                }
                gray
            }
            FrameFormat.YUV_420_888 -> {
                // Y plane is the first width*height bytes — already grayscale
                data.copyOfRange(0, pixelCount.coerceAtMost(data.size))
            }
            else -> data.copyOfRange(0, pixelCount.coerceAtMost(data.size))
        }
    }

    /**
     * Convert frame to grayscale.
     *
     * RGB_888 is converted using ITU-R BT.601 luminance weights (0.299R + 0.587G + 0.114B).
     * YUV_420_888 Y plane is extracted directly.
     * Output is 1 byte per pixel.
     */
    public fun grayscale(frame: Frame): ProcessedFrame {
        val gray = toGrayscaleBytes(frame)
        return ProcessedFrame(
            width = frame.width,
            height = frame.height,
            data = gray,
            format = ProcessedFormat.GRAYSCALE
        )
    }

    /**
     * Enhance contrast using histogram equalization on grayscale data.
     *
     * Computes the cumulative distribution function of pixel intensities
     * and remaps values to achieve a uniform histogram.
     */
    public fun enhanceContrast(frame: Frame): ProcessedFrame {
        val gray = toGrayscaleBytes(frame)
        if (gray.isEmpty()) {
            return ProcessedFrame(frame.width, frame.height, null, ProcessedFormat.ENHANCED)
        }

        // Build histogram
        val histogram = IntArray(256)
        for (b in gray) {
            histogram[b.toInt() and 0xFF]++
        }

        // Compute cumulative distribution function
        val cdf = IntArray(256)
        cdf[0] = histogram[0]
        for (i in 1..255) {
            cdf[i] = cdf[i - 1] + histogram[i]
        }

        val cdfMin = cdf.first { it > 0 }
        val totalPixels = gray.size
        val range = totalPixels - cdfMin

        // Build equalization lookup table
        val lut = ByteArray(256) { i ->
            if (range == 0) {
                // All pixels identical — no equalization possible
                0.toByte()
            } else {
                ((cdf[i] - cdfMin).toFloat() / range.toFloat() * 255f)
                    .toInt().coerceIn(0, 255).toByte()
            }
        }

        // Apply LUT
        val enhanced = ByteArray(totalPixels)
        for (i in 0 until totalPixels) {
            enhanced[i] = lut[gray[i].toInt() and 0xFF]
        }

        return ProcessedFrame(
            width = frame.width,
            height = frame.height,
            data = enhanced,
            format = ProcessedFormat.ENHANCED
        )
    }

    /**
     * Reduce noise using a box blur with configurable kernel size.
     *
     * Each output pixel is the average of its neighborhood.
     * Edge pixels are clamped to the image boundary.
     */
    public fun reduceNoise(frame: Frame, kernelSize: Int = 3): ProcessedFrame {
        require(kernelSize >= 1 && kernelSize % 2 == 1) {
            "kernelSize must be a positive odd number, was $kernelSize"
        }
        val gray = toGrayscaleBytes(frame)
        if (gray.isEmpty()) {
            return ProcessedFrame(frame.width, frame.height, null, ProcessedFormat.DENOISED)
        }

        val w = frame.width
        val h = frame.height
        val half = kernelSize / 2
        val result = ByteArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0
                var count = 0
                for (dy in -half..half) {
                    for (dx in -half..half) {
                        val nx = (x + dx).coerceIn(0, w - 1)
                        val ny = (y + dy).coerceIn(0, h - 1)
                        sum += gray[ny * w + nx].toInt() and 0xFF
                        count++
                    }
                }
                result[y * w + x] = (sum / count).toByte()
            }
        }

        return ProcessedFrame(
            width = w,
            height = h,
            data = result,
            format = ProcessedFormat.DENOISED
        )
    }

    /**
     * Detect edges using Sobel operators on grayscale data.
     *
     * Applies 3x3 horizontal (Gx) and vertical (Gy) Sobel kernels,
     * then computes gradient magnitude for each pixel.
     * Border pixels are left as zero.
     */
    public fun detectEdges(frame: Frame): ProcessedFrame {
        val gray = toGrayscaleBytes(frame)
        if (gray.isEmpty()) {
            return ProcessedFrame(frame.width, frame.height, null, ProcessedFormat.EDGES)
        }

        val w = frame.width
        val h = frame.height
        val edges = ByteArray(w * h)

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val tl = gray[(y - 1) * w + (x - 1)].toInt() and 0xFF
                val tc = gray[(y - 1) * w + x].toInt() and 0xFF
                val tr = gray[(y - 1) * w + (x + 1)].toInt() and 0xFF
                val ml = gray[y * w + (x - 1)].toInt() and 0xFF
                val mr = gray[y * w + (x + 1)].toInt() and 0xFF
                val bl = gray[(y + 1) * w + (x - 1)].toInt() and 0xFF
                val bc = gray[(y + 1) * w + x].toInt() and 0xFF
                val br = gray[(y + 1) * w + (x + 1)].toInt() and 0xFF

                // Sobel Gx: horizontal edges
                val gx = -tl + tr - 2 * ml + 2 * mr - bl + br
                // Sobel Gy: vertical edges
                val gy = -tl - 2 * tc - tr + bl + 2 * bc + br

                val magnitude = sqrt((gx * gx + gy * gy).toDouble()).toInt()
                edges[y * w + x] = magnitude.coerceIn(0, 255).toByte()
            }
        }

        return ProcessedFrame(
            width = w,
            height = h,
            data = edges,
            format = ProcessedFormat.EDGES
        )
    }

    /**
     * Binarize grayscale data using Otsu's automatic thresholding.
     *
     * Computes the optimal threshold by maximizing inter-class variance
     * between foreground and background pixel populations.
     */
    public fun binarize(frame: Frame): ProcessedFrame {
        val gray = toGrayscaleBytes(frame)
        if (gray.isEmpty()) {
            return ProcessedFrame(frame.width, frame.height, null, ProcessedFormat.BINARY)
        }

        val totalPixels = gray.size

        // Build histogram
        val histogram = IntArray(256)
        for (b in gray) {
            histogram[b.toInt() and 0xFF]++
        }

        // Sum of all pixel values
        var sumAll = 0L
        for (i in 0..255) {
            sumAll += i.toLong() * histogram[i]
        }

        // Find threshold that maximizes inter-class variance
        var sumB = 0L
        var wB = 0
        var maxVariance = 0.0
        var threshold = 0

        for (t in 0..255) {
            wB += histogram[t]
            if (wB == 0) continue
            val wF = totalPixels - wB
            if (wF == 0) break

            sumB += t.toLong() * histogram[t]
            val meanB = sumB.toFloat() / wB
            val meanF = (sumAll - sumB).toFloat() / wF
            val variance = wB.toDouble() * wF.toDouble() * (meanB - meanF) * (meanB - meanF)

            if (variance > maxVariance) {
                maxVariance = variance
                threshold = t
            }
        }

        // Apply threshold
        val binary = ByteArray(totalPixels)
        for (i in 0 until totalPixels) {
            binary[i] = if ((gray[i].toInt() and 0xFF) >= threshold) 255.toByte() else 0.toByte()
        }

        return ProcessedFrame(
            width = frame.width,
            height = frame.height,
            data = binary,
            format = ProcessedFormat.BINARY
        )
    }

    /**
     * Full preprocessing pipeline: grayscale, contrast enhancement, noise reduction, binarization.
     *
     * For unknown formats the frame is returned unchanged to avoid garbled conversions.
     */
    public fun preprocess(frame: Frame): ProcessedFrame {
        val isKnownFormat = frame.format == FrameFormat.RGB_888 ||
            frame.format == FrameFormat.YUV_420_888 ||
            frame.format == FrameFormat.GRAYSCALE
        if (!isKnownFormat) {
            return ProcessedFrame(
                width = frame.width,
                height = frame.height,
                data = frame.data,
                format = ProcessedFormat.GRAYSCALE
            )
        }
        return binarize(
            reduceNoise(
                enhanceContrast(
                    grayscale(frame).toFrame()
                ).toFrame()
            ).toFrame()
        )
    }
}

/**
 * Represents a frame after image preprocessing.
 */
public data class ProcessedFrame(
    val width: Int,
    val height: Int,
    val data: ByteArray?,
    val format: ProcessedFormat
) {
    public fun toFrame(): Frame {
        val frameFormat = FrameFormat.GRAYSCALE
        return Frame(
            width = width,
            height = height,
            format = frameFormat,
            data = data
        )
    }
}

/**
 * Output format of processed frames.
 */
public enum class ProcessedFormat {
    GRAYSCALE,
    ENHANCED,
    DENOISED,
    EDGES,
    BINARY
}
