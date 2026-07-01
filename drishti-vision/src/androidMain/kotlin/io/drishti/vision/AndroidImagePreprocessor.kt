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

/**
 * Android-specific image preprocessing using OpenCV.
 *
 * Delegates all operations to the shared [ImagePreprocessor] implementation.
 */
public actual class AndroidImagePreprocessor {
    private val delegate = ImagePreprocessor()

    /**
     * Convert frame to grayscale using OpenCV.
     */
    public actual fun grayscale(frame: Frame): ProcessedFrame = delegate.grayscale(frame)

    /**
     * Enhance contrast using histogram equalization via OpenCV.
     */
    public actual fun enhanceContrast(frame: Frame): ProcessedFrame = delegate.enhanceContrast(frame)

    /**
     * Reduce noise using Gaussian blur via OpenCV.
     */
    public actual fun reduceNoise(frame: Frame): ProcessedFrame = delegate.reduceNoise(frame)

    /**
     * Detect edges using Canny edge detection via OpenCV.
     */
    public actual fun detectEdges(frame: Frame): ProcessedFrame = delegate.detectEdges(frame)

    /**
     * Apply binarization using Otsu's method via OpenCV.
     */
    public actual fun binarize(frame: Frame): ProcessedFrame = delegate.binarize(frame)

    /**
     * Full preprocessing pipeline via OpenCV.
     */
    public actual fun preprocess(frame: Frame): ProcessedFrame = delegate.preprocess(frame)
}
