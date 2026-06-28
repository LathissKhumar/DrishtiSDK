package io.drishti.audio

import io.drishti.core.*

/**
 * Spatial audio parameters for Android Spatializer API (API 32+).
 *
 * Maps SceneGraph nodes to 3D audio positions using spherical coordinates
 * consumed by the Spatializer engine. The Android HAL layer reads these
 * parameters to configure AudioTrack spatial audio output via Oboe.
 */

/**
 * Sound type determines the timbre character of a spatial source.
 *
 * - [MUSICAL_TONE] – data points and numerical values
 * - [SPEECH] – text labels, formulas, and descriptions
 * - [AMBIENT] – background references, axes, and structural elements
 */
enum class SoundType {
    MUSICAL_TONE,
    SPEECH,
    AMBIENT
}

/**
 * Spherical position for spatial audio rendering.
 *
 * @param azimuth Horizontal angle in degrees (-180° to 180°).
 *        0° = directly ahead, negative = left, positive = right.
 * @param elevation Vertical angle in degrees (-90° to 90°).
 *        0° = ear level, negative = above, positive = below.
 * @param distance Distance from listener in meters (0.1 to 10.0).
 *        Closer = louder, used with distance attenuation model.
 */
data class SpatialPosition(
    val azimuth: Float,
    val elevation: Float,
    val distance: Float
) {
    init {
        require(azimuth in -180f..180f) { "Azimuth must be in [-180, 180]: $azimuth" }
        require(elevation in -90f..90f) { "Elevation must be in [-90, 90]: $elevation" }
        require(distance in 0.1f..10.0f) { "Distance must be in [0.1, 10.0]: $distance" }
    }
}

/**
 * A single spatial audio source with full 3D positioning and timbre.
 *
 * This extends the core [AudioSource] with spatial audio parameters
 * required by the Android Spatializer API.
 *
 * @param position Spherical 3D position.
 * @param volume Volume level (0.0 to 1.0) derived from edge weights.
 * @param soundType Timbre character based on content type.
 * @param frequency Base frequency for musical tones (Hz).
 * @param speechText Text to synthesize via TTS if [soundType] is [SoundType.SPEECH].
 * @param nodeId Source SceneGraph node id for tracking.
 */
data class SpatialAudioSource(
    val position: SpatialPosition,
    val volume: Float,
    val soundType: SoundType,
    val frequency: Float = 0f,
    val speechText: String = "",
    val nodeId: String = ""
)

/**
 * Speech description for a content element.
 *
 * Generated for each content item to provide verbal context
 * alongside spatial audio rendering.
 *
 * @param text The spoken text.
 * @param sourceNodeId The SceneGraph node this description relates to.
 * @param position Where to place the speech source in 3D space.
 */
data class SpeechDescription(
    val text: String,
    val sourceNodeId: String,
    val position: SpatialPosition
)

/**
 * Complete spatial audio scene description.
 *
 * Produced by [SpatialRenderer] from a [SceneGraph]. Contains all
 * spatial audio sources and speech descriptions that the Android HAL
 * layer feeds to the Spatializer API and Oboe renderer.
 *
 * @param sources Spatial audio sources positioned in 3D space.
 * @param speechDescriptions Speech descriptions for content elements.
 * @param sceneBounds The spatial bounds of the audio scene.
 */
data class SpatialAudioScene(
    val sources: List<SpatialAudioSource>,
    val speechDescriptions: List<SpeechDescription>,
    val sceneBounds: SceneBounds
)

/** Minimum distance from listener (meters). */
const val MIN_DISTANCE = 0.1f

/** Maximum distance from listener (meters). */
const val MAX_DISTANCE = 10.0f

/** Default reference distance for inverse-distance volume model. */
const val REFERENCE_DISTANCE = 1.0f

/** Maximum frequency for musical tone mapping (Hz). */
const val MAX_FREQUENCY = 1000f

/** Minimum frequency for musical tone mapping (Hz). */
const val MIN_FREQUENCY = 200f
