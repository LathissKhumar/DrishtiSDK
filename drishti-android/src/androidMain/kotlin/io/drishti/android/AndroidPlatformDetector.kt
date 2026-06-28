package io.drishti.android

import android.os.Build
import android.os.VibrationEffect

/**
 * Android-specific platform detection.
 *
 * Queries Android framework APIs to determine haptic, audio, NPU,
 * threading, and memory capabilities of the running device.
 */
actual class AndroidPlatformDetector : PlatformDetector {
    /**
     * Detect and return the current device capabilities.
     */
    actual override fun detect(): DeviceCapabilities {
        return DeviceCapabilities(
            hapticLevel = detectHapticLevel(),
            audioLevel = detectAudioLevel(),
            npuAvailable = detectNPU(),
            maxThreads = Runtime.getRuntime().availableProcessors(),
            availableMemory = Runtime.getRuntime().maxMemory(),
            sdkVersion = Build.VERSION.SDK_INT
        )
    }

    /**
     * Detect the haptic feedback capability level.
     */
    private fun detectHapticLevel(): HapticLevel {
        return when {
            Build.VERSION.SDK_INT >= 30 && VibrationEffect.Composition::class.java != null ->
                HapticLevel.FULL_EFFECT_COMPOSITION
            Build.VERSION.SDK_INT >= 26 ->
                HapticLevel.WAVEFORM_ONLY
            Build.VERSION.SDK_INT >= 1 ->
                HapticLevel.LEGACY_VIBRATE
            else ->
                HapticLevel.NONE
        }
    }

    /**
     * Detect the audio output capability level.
     */
    private fun detectAudioLevel(): AudioLevel {
        return when {
            Build.VERSION.SDK_INT >= 32 -> AudioLevel.SPATIALIZER_OBOE
            Build.VERSION.SDK_INT >= 21 -> AudioLevel.STEREO_ONLY
            else -> AudioLevel.MONO_ONLY
        }
    }

    /**
     * Detect whether a neural processing unit (NPU) is available.
     */
    private fun detectNPU(): Boolean {
        // Check for Qualcomm Hexagon NPU or similar
        return Build.HARDWARE.contains("qcom", ignoreCase = true) ||
               Build.HARDWARE.contains("exynos", ignoreCase = true)
    }
}
