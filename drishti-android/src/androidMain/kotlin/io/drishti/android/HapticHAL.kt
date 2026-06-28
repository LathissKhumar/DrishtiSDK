package io.drishti.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission
import io.drishti.core.HapticPulse

/**
 * Hardware Abstraction Layer for haptic feedback.
 *
 * Provides a unified interface for playing haptic pulses across different
 * Android API levels, automatically selecting the best available vibration
 * mechanism (Composition API, Waveform, or legacy).
 *
 * @param context Android context used to obtain system services.
 */
class HapticHAL(private val context: Context) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= 31) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val capabilities = AndroidPlatformDetector().detect()

    /**
     * Play a single haptic pulse.
     *
     * Selects the appropriate vibration effect based on the device's
     * [HapticLevel] capability.
     *
     * @param pulse The haptic pulse to play.
     */
    @RequiresPermission(Manifest.permission.VIBRATE)
    fun playPulse(pulse: HapticPulse) {
        if (context.checkSelfPermission(Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val effect = when (capabilities.hapticLevel) {
            HapticLevel.FULL_EFFECT_COMPOSITION -> createCompositionEffect(pulse)
            HapticLevel.WAVEFORM_ONLY -> createWaveformEffect(pulse)
            HapticLevel.LEGACY_VIBRATE -> createLegacyEffect(pulse)
            HapticLevel.NONE -> return
        }
        vibrator?.vibrate(effect)
    }

    /**
     * Play a sequence of haptic pulses.
     *
     * @param pulses Ordered list of haptic pulses to play in sequence.
     */
    @RequiresPermission(Manifest.permission.VIBRATE)
    fun playSequence(pulses: List<HapticPulse>) {
        if (pulses.isEmpty()) return
        if (context.checkSelfPermission(Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        when (capabilities.hapticLevel) {
            HapticLevel.FULL_EFFECT_COMPOSITION -> {
                if (Build.VERSION.SDK_INT >= 30) {
                    val composition = VibrationEffect.startComposition()
                    for (pulse in pulses) {
                        composition.addPrimitive(
                            VibrationEffect.Composition.PRIMITIVE_CLICK,
                            pulse.intensity,
                            pulse.delay.toInt()
                        )
                    }
                    vibrator?.vibrate(composition.compose())
                } else {
                    playWaveformSequence(pulses)
                }
            }
            HapticLevel.WAVEFORM_ONLY -> {
                playWaveformSequence(pulses)
            }
            HapticLevel.LEGACY_VIBRATE -> {
                val timings = mutableListOf<Long>()
                for (pulse in pulses) {
                    timings.add(pulse.delay)
                    timings.add(pulse.duration)
                }
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings.toLongArray(), -1)
            }
            HapticLevel.NONE -> return
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun playWaveformSequence(pulses: List<HapticPulse>) {
        if (Build.VERSION.SDK_INT >= 26) {
            val timings = mutableListOf<Long>()
            val amplitudes = mutableListOf<Int>()
            for (pulse in pulses) {
                timings.add(pulse.delay)
                amplitudes.add(0)
                timings.add(pulse.duration)
                amplitudes.add((pulse.intensity * 255).toInt().coerceIn(0, 255))
            }
            val effect = VibrationEffect.createWaveform(timings.toLongArray(), amplitudes.toIntArray(), -1)
            vibrator?.vibrate(effect)
        }
    }

    /**
     * Create a haptic effect using the VibrationEffect.Composition API (API 30+).
     */
    private fun createCompositionEffect(pulse: HapticPulse): VibrationEffect {
        // API 30+ with VibrationEffect.Composition
        return VibrationEffect.startComposition()
            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, pulse.intensity)
            .compose()
    }

    /**
     * Create a haptic effect using createWaveform (API 26-29).
     */
    private fun createWaveformEffect(pulse: HapticPulse): VibrationEffect {
        // API 26-29 with createWaveform
        val timings = longArrayOf(0, pulse.duration)
        val amplitudes = intArrayOf(0, (pulse.intensity * 255).toInt())
        return VibrationEffect.createWaveform(timings, amplitudes, -1)
    }

    /**
     * Create a haptic effect using the legacy vibrate() API.
     */
    private fun createLegacyEffect(pulse: HapticPulse): VibrationEffect {
        // API < 26 with vibrate()
        @Suppress("DEPRECATION")
        return VibrationEffect.createOneShot(pulse.duration, (pulse.intensity * 255).toInt())
    }

    /**
     * Whether the device supports haptic feedback.
     */
    fun hasHapticSupport(): Boolean = capabilities.hapticLevel != HapticLevel.NONE
}
