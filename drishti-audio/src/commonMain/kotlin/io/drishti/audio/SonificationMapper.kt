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

package io.drishti.audio

import io.drishti.core.*

/**
 * Maps content to sonification parameters.
 */
public class SonificationMapper {
    /**
     * Map data value to frequency.
     *
     * Linearly interpolates between [minFreq] and [maxFreq] based on where [value]
     * falls in the [min, max] range. When [min] equals [max], returns the midpoint frequency.
     *
     * @param value The data value to map.
     * @param min Minimum data value (may equal max for degenerate datasets).
     * @param max Maximum data value.
     * @param minFreq Minimum output frequency in Hz.
     * @param maxFreq Maximum output frequency in Hz.
     * @return Frequency in Hz within [minFreq, maxFreq].
     */
    public fun mapToFrequency(value: Float, min: Float, max: Float, minFreq: Float = 200f, maxFreq: Float = 1000f): Float {
        if (min == max) return (minFreq + maxFreq) / 2f
        val normalized = ((value - min) / (max - min)).coerceIn(0f, 1f)
        return minFreq + normalized * (maxFreq - minFreq)
    }

    /**
     * Map data value to amplitude.
     *
     * @param value The data value to map.
     * @param min Minimum data value.
     * @param max Maximum data value.
     * @return Amplitude in [0, 1]. Returns 0.5f when [min] equals [max].
     */
    public fun mapToAmplitude(value: Float, min: Float, max: Float): Float {
        if (min == max) return 0.5f
        return ((value - min) / (max - min)).coerceIn(0f, 1f)
    }

    /**
     * Map data value to panning.
     *
     * @param value The data value to map.
     * @param min Minimum data value.
     * @param max Maximum data value.
     * @return Panning in [-1, 1] where 0 is center. Returns 0f when [min] equals [max].
     */
    public fun mapToPanning(value: Float, min: Float, max: Float): Float {
        if (min == max) return 0f
        return (((value - min) / (max - min)) * 2f - 1f).coerceIn(-1f, 1f)
    }

    /**
     * Map data value to duration.
     *
     * @param value The data value to map.
     * @param min Minimum data value.
     * @param max Maximum data value.
     * @param minDuration Minimum output duration in milliseconds.
     * @param maxDuration Maximum output duration in milliseconds.
     * @return Duration in milliseconds within [minDuration, maxDuration].
     */
    public fun mapToDuration(value: Float, min: Float, max: Float, minDuration: Long = 50L, maxDuration: Long = 200L): Long {
        if (min == max) return (minDuration + maxDuration) / 2
        val normalized = ((value - min) / (max - min)).coerceIn(0f, 1f)
        return (minDuration + normalized * (maxDuration - minDuration)).toLong()
    }
}
