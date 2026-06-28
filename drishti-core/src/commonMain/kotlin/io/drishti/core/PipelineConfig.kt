package io.drishti.core

import kotlinx.serialization.Serializable

/**
 * Configuration parameters for the scene graph construction pipeline.
 */
@Serializable
data class PipelineConfig(
    val spatialThreshold: Float = 300f,
    val containmentOverlapRatio: Float = 0.5f
)
