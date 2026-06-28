package io.drishti.vision

import io.drishti.core.Frame

/**
 * Android-specific image preprocessing using OpenCV.
 *
 * Delegates all operations to the shared [ImagePreprocessor] implementation.
 */
actual class AndroidImagePreprocessor {
    private val delegate = ImagePreprocessor()

    /**
     * Convert frame to grayscale using OpenCV.
     */
    actual fun grayscale(frame: Frame): ProcessedFrame = delegate.grayscale(frame)

    /**
     * Enhance contrast using histogram equalization via OpenCV.
     */
    actual fun enhanceContrast(frame: Frame): ProcessedFrame = delegate.enhanceContrast(frame)

    /**
     * Reduce noise using Gaussian blur via OpenCV.
     */
    actual fun reduceNoise(frame: Frame): ProcessedFrame = delegate.reduceNoise(frame)

    /**
     * Detect edges using Canny edge detection via OpenCV.
     */
    actual fun detectEdges(frame: Frame): ProcessedFrame = delegate.detectEdges(frame)

    /**
     * Apply binarization using Otsu's method via OpenCV.
     */
    actual fun binarize(frame: Frame): ProcessedFrame = delegate.binarize(frame)

    /**
     * Full preprocessing pipeline via OpenCV.
     */
    actual fun preprocess(frame: Frame): ProcessedFrame = delegate.preprocess(frame)
}
