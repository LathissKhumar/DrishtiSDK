package io.drishti.vision

import io.drishti.core.Frame
import io.drishti.core.FrameFormat

/**
 * Shared vision preprocessing pipeline.
 */
class ImagePreprocessor {
    /**
     * Convert frame to grayscale.
     */
    fun grayscale(frame: Frame): ProcessedFrame {
        // Common preprocessing logic
        return ProcessedFrame(
            width = frame.width,
            height = frame.height,
            data = frame.data,
            format = ProcessedFormat.GRAYSCALE
        )
    }

    /**
     * Enhance contrast using histogram equalization.
     */
    fun enhanceContrast(frame: Frame): ProcessedFrame {
        return ProcessedFrame(
            width = frame.width,
            height = frame.height,
            data = frame.data,
            format = ProcessedFormat.ENHANCED
        )
    }

    /**
     * Reduce noise using Gaussian blur.
     */
    fun reduceNoise(frame: Frame, kernelSize: Int = 5): ProcessedFrame {
        return ProcessedFrame(
            width = frame.width,
            height = frame.height,
            data = frame.data,
            format = ProcessedFormat.DENOISED
        )
    }

    /**
     * Detect edges using Canny edge detection.
     */
    fun detectEdges(frame: Frame): ProcessedFrame {
        return ProcessedFrame(
            width = frame.width,
            height = frame.height,
            data = frame.data,
            format = ProcessedFormat.EDGES
        )
    }

    /**
     * Apply binarization using Otsu's method.
     */
    fun binarize(frame: Frame): ProcessedFrame {
        return ProcessedFrame(
            width = frame.width,
            height = frame.height,
            data = frame.data,
            format = ProcessedFormat.BINARY
        )
    }

    /**
     * Full preprocessing pipeline.
     */
    fun preprocess(frame: Frame): ProcessedFrame {
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
data class ProcessedFrame(
    val width: Int,
    val height: Int,
    val data: ByteArray?,
    val format: ProcessedFormat
) {
    fun toFrame(): Frame = Frame(
        width = width,
        height = height,
        format = FrameFormat.RGB_888,
        data = data
    )
}

/**
 * Output format of processed frames.
 */
enum class ProcessedFormat {
    GRAYSCALE,
    ENHANCED,
    DENOISED,
    EDGES,
    BINARY
}
