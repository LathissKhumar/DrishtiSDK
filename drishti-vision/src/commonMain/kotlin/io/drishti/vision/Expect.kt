package io.drishti.vision

import io.drishti.core.Frame

/**
 * Platform-specific image preprocessor using OpenCV.
 *
 * Common declarations for the expect/actual pattern.
 */
expect class AndroidImagePreprocessor {
    /**
     * Convert frame to grayscale.
     */
    fun grayscale(frame: Frame): ProcessedFrame

    /**
     * Enhance contrast using histogram equalization.
     */
    fun enhanceContrast(frame: Frame): ProcessedFrame

    /**
     * Reduce noise using Gaussian blur.
     */
    fun reduceNoise(frame: Frame): ProcessedFrame

    /**
     * Detect edges using Canny edge detection.
     */
    fun detectEdges(frame: Frame): ProcessedFrame

    /**
     * Apply binarization using Otsu's method.
     */
    fun binarize(frame: Frame): ProcessedFrame

    /**
     * Full preprocessing pipeline.
     */
    fun preprocess(frame: Frame): ProcessedFrame
}
