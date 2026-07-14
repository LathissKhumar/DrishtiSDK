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

package io.drishti.core

import kotlinx.serialization.Serializable

/**
 * Configuration parameters for the scene graph construction pipeline.
 *
 * @property spatialThreshold Maximum distance (pixels) for spatial proximity edges.
 * @property containmentOverlapRatio Minimum overlap ratio for containment detection (0.0-1.0).
 * @property minConfidence Minimum confidence threshold for detected items (0.0-1.0).
 * @property maxItemsPerFrame Maximum number of content items per frame.
 * @property explorationElementLimit Maximum elements in exploration sequences.
 */
@Serializable
public data class PipelineConfig(
    val spatialThreshold: Float = 300f,
    val containmentOverlapRatio: Float = 0.5f,
    val minConfidence: Float = 0.3f,
    val maxItemsPerFrame: Int = 50,
    val explorationElementLimit: Int = 100
) {
    init {
        require(spatialThreshold >= 0f) { "spatialThreshold must be non-negative, was $spatialThreshold" }
        require(containmentOverlapRatio in 0f..1f) { "containmentOverlapRatio must be 0.0-1.0, was $containmentOverlapRatio" }
        require(minConfidence in 0f..1f) { "minConfidence must be 0.0-1.0, was $minConfidence" }
        require(maxItemsPerFrame > 0) { "maxItemsPerFrame must be positive, was $maxItemsPerFrame" }
        require(explorationElementLimit > 0) { "explorationElementLimit must be positive, was $explorationElementLimit" }
    }
}
