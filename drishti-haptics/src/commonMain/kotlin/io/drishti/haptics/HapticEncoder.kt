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

package io.drishti.haptics

import io.drishti.core.HapticPulse

/**
 * Encodes haptic pulses into vibration effect patterns.
 */
public class HapticEncoder {
    /**
     * Encode pulses into timing and amplitude arrays.
     */
    public fun encode(pulses: List<HapticPulse>): EncodedPattern {
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
    public fun encodeComposition(pulses: List<HapticPulse>): List<CompositionPrimitive> {
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
     *
     * Accumulates all events on a single channel with cumulative timing.
     * Each event's [HapticEvent.time] is the offset from pattern start in milliseconds.
     */
    public fun encodeSDK(pulses: List<HapticPulse>): HapticSDKPattern {
        var currentTime = 0L
        val events = mutableListOf<HapticEvent>()

        pulses.forEach { pulse ->
            if (pulse.delay > 0L) {
                currentTime += pulse.delay
            }
            events.add(
                HapticEvent(
                    time = currentTime,
                    intensity = pulse.intensity,
                    sharpness = when {
                        pulse.intensity > 0.8f -> 0.9f  // sharp, high-intensity
                        pulse.duration > 100L -> 0.3f   // dull, long vibration
                        else -> 0.5f
                    }
                )
            )
            currentTime += pulse.duration
        }

        return HapticSDKPattern(
            version = 1,
            duration = currentTime,
            channels = listOf(
                HapticChannel(
                    id = 0,
                    events = events
                )
            )
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

public data class EncodedPattern(
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

public data class CompositionPrimitive(
    val primitiveType: String,
    val scale: Float,
    val delay: Int
)

public data class HapticSDKPattern(
    val version: Int,
    val duration: Long,
    val channels: List<HapticChannel>
)

public data class HapticChannel(
    val id: Int,
    val events: List<HapticEvent>
)

public data class HapticEvent(
    val time: Long,
    val intensity: Float,
    val sharpness: Float
)
