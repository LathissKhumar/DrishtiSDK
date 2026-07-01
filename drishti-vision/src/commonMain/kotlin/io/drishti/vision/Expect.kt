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
 * Platform-specific image preprocessor using OpenCV.
 *
 * Common declarations for the expect/actual pattern.
 */
public expect class AndroidImagePreprocessor {
    /**
     * Convert frame to grayscale.
     */
    public fun grayscale(frame: Frame): ProcessedFrame

    /**
     * Enhance contrast using histogram equalization.
     */
    public fun enhanceContrast(frame: Frame): ProcessedFrame

    /**
     * Reduce noise using box blur.
     */
    public fun reduceNoise(frame: Frame): ProcessedFrame

    /**
     * Detect edges using Sobel edge detection.
     */
    public fun detectEdges(frame: Frame): ProcessedFrame

    /**
     * Apply binarization using Otsu's method.
     */
    public fun binarize(frame: Frame): ProcessedFrame

    /**
     * Full preprocessing pipeline.
     */
    public fun preprocess(frame: Frame): ProcessedFrame
}
