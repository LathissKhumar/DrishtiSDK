package io.drishti.android

/**
 * Device capability information.
 *
 * Describes the hardware and software capabilities of the current Android device
 * to enable adaptive behavior across varying API levels and hardware.
 *
 * @property hapticLevel Detected haptic feedback capability level.
 * @property audioLevel Detected audio output capability level.
 * @property npuAvailable Whether a neural processing unit is available on the SoC.
 * @property maxThreads Maximum number of available CPU threads.
 * @property availableMemory Maximum memory the JVM can use, in bytes.
 * @property sdkVersion Android SDK API level of the device.
 */
data class DeviceCapabilities(
    val hapticLevel: HapticLevel,
    val audioLevel: AudioLevel,
    val npuAvailable: Boolean,
    val maxThreads: Int,
    val availableMemory: Long,
    val sdkVersion: Int
)

/**
 * Haptic feedback capability levels, ordered from most to least capable.
 */
enum class HapticLevel {
    /** API 30+ with VibrationEffect.Composition for rich haptic patterns. */
    FULL_EFFECT_COMPOSITION,
    /** API 26-29 with createWaveform for timed vibration patterns. */
    WAVEFORM_ONLY,
    /** API < 26 with the legacy vibrate() API. */
    LEGACY_VIBRATE,
    /** No haptic support detected on the device. */
    NONE
}

/**
 * Audio output capability levels, ordered from most to least capable.
 */
enum class AudioLevel {
    /** API 32+ with Spatializer API and Oboe for low-latency spatial audio. */
    SPATIALIZER_OBOE,
    /** API 32+ with Spatializer API only. */
    SPATIALIZER_BASIC,
    /** Standard stereo output. */
    STEREO_ONLY,
    /** Mono output only. */
    MONO_ONLY
}
