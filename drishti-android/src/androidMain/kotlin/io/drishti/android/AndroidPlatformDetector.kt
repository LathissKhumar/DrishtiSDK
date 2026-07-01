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

package io.drishti.android

import android.os.Build
import android.os.VibrationEffect

/**
 * Android-specific platform detection.
 *
 * Queries Android framework APIs to determine haptic, audio, NPU,
 * threading, and memory capabilities of the running device.
 */
public actual class AndroidPlatformDetector : PlatformDetector {
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
            Build.VERSION.SDK_INT >= 30 ->
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
     *
     * API 32+: Full Spatializer with Oboe backend.
     * API 30–31: Basic Spatializer API introduced (no Oboe).
     * API 21–29: Stereo only.
     * API < 21: Mono only.
     */
    private fun detectAudioLevel(): AudioLevel {
        return when {
            Build.VERSION.SDK_INT >= 32 -> AudioLevel.SPATIALIZER_OBOE
            Build.VERSION.SDK_INT >= 30 -> AudioLevel.SPATIALIZER_BASIC
            Build.VERSION.SDK_INT >= 21 -> AudioLevel.STEREO_ONLY
            else -> AudioLevel.MONO_ONLY
        }
    }

    /**
     * Detect whether a neural processing unit (NPU) is available.
     *
     * Uses heuristic hardware string matching against known NPU-capable
     * SoC families. This is best-effort — runtime NNAPI availability
     * may differ from what this static check reports.
     */
    private fun detectNPU(): Boolean {
        val hardware = Build.HARDWARE.lowercase()
        return hardware.contains("qcom") || // Qualcomm Hexagon NPU
               hardware.contains("exynos") || // Samsung NPU
               hardware.contains("tensor") || // Google Tensor TPU
               hardware.contains("kirin") || // HiSilicon Da Vinci NPU
               hardware.contains("mt6") // MediaTek APU (Dimensity series)
    }
}
