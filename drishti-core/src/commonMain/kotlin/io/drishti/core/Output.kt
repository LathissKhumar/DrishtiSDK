package io.drishti.core

import kotlinx.serialization.Serializable

@Serializable
data class HapticOutput(
    val pulses: List<HapticPulse>,
    val pattern: String = ""
)

@Serializable
data class HapticPulse(
    val intensity: Float, // 0.0f - 1.0f
    val duration: Long,   // milliseconds
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val delay: Long = 0L
) {
    init {
        require(intensity in 0f..1f) { "intensity must be 0.0-1.0, got $intensity" }
        require(duration > 0) { "duration must be positive, got $duration" }
        require(x in 0f..1f) { "x must be 0.0-1.0, got $x" }
        require(y in 0f..1f) { "y must be 0.0-1.0, got $y" }
        require(delay >= 0L) { "delay must be non-negative, got $delay" }
    }
}

@Serializable
data class AudioOutput(
    val sources: List<AudioSource>,
    val spatial: Boolean = true
)

@Serializable
data class AudioSource(
    val frequency: Float,
    val amplitude: Float,
    val spatialX: Float = 0.5f,
    val spatialY: Float = 0.5f,
    val spatialZ: Float = 0.5f
) {
    init {
        require(frequency >= 0f) { "frequency must be non-negative, got $frequency" }
        require(amplitude in 0f..1f) { "amplitude must be 0.0-1.0, got $amplitude" }
    }
}

@Serializable
data class VoiceOutput(
    val speech: SpeechSegment,
    val language: String = "en-US"
)

@Serializable
data class SpeechSegment(
    val text: String,
    val rate: Float = 1.0f,
    val pitch: Float = 1.0f
)

@Serializable
data class TextOutput(
    val text: String
)
