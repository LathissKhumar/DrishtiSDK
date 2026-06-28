package io.drishti.haptics

import io.drishti.core.HapticPulse

/**
 * Encodes haptic pulses into vibration effect patterns.
 */
class HapticEncoder {
    /**
     * Encode pulses into timing and amplitude arrays.
     */
    fun encode(pulses: List<HapticPulse>): EncodedPattern {
        val timings = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()

        pulses.forEach { pulse ->
            if (pulse.delay > 0L) {
                timings.add(pulse.delay)
                amplitudes.add(0)
            }
            timings.add(pulse.duration)
            amplitudes.add((pulse.intensity * 255).toInt().coerceIn(0, 255))
        }

        return EncodedPattern(
            timings = timings.toLongArray(),
            amplitudes = amplitudes.toIntArray(),
            repeat = -1
        )
    }

    /**
     * Encode pulses for VibrationEffect.Composition (API 30+).
     */
    fun encodeComposition(pulses: List<HapticPulse>): List<CompositionPrimitive> {
        return pulses.map { pulse ->
            CompositionPrimitive(
                primitiveType = mapToPrimitiveType(pulse),
                scale = pulse.intensity.coerceIn(0f, 1f),
                delay = pulse.delay.toInt()
            )
        }
    }

    /**
     * Encode pulses for haptic SDK format.
     */
    fun encodeSDK(pulses: List<HapticPulse>): HapticSDKPattern {
        return HapticSDKPattern(
            version = 1,
            duration = pulses.sumOf { it.duration },
            channels = pulses.map { pulse ->
                HapticChannel(
                    id = 0,
                    events = listOf(
                        HapticEvent(
                            time = 0,
                            intensity = pulse.intensity,
                            sharpness = 0.5f
                        )
                    )
                )
            }
        )
    }

    private fun mapToPrimitiveType(pulse: HapticPulse): String {
        return when {
            pulse.intensity > 0.8f -> "CLICK"
            pulse.intensity > 0.5f -> "TICK"
            pulse.duration > 100L -> "LOW_TICK"
            else -> "CLICK"
        }
    }
}

data class EncodedPattern(
    val timings: LongArray,
    val amplitudes: IntArray,
    val repeat: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncodedPattern) return false
        return timings.contentEquals(other.timings) && amplitudes.contentEquals(other.amplitudes)
    }

    override fun hashCode(): Int {
        return 31 * timings.contentHashCode() + amplitudes.contentHashCode()
    }
}

data class CompositionPrimitive(
    val primitiveType: String,
    val scale: Float,
    val delay: Int
)

data class HapticSDKPattern(
    val version: Int,
    val duration: Long,
    val channels: List<HapticChannel>
)

data class HapticChannel(
    val id: Int,
    val events: List<HapticEvent>
)

data class HapticEvent(
    val time: Long,
    val intensity: Float,
    val sharpness: Float
)
