package io.drishti.audio

import io.drishti.core.*

/**
 * Maps content to sonification parameters.
 */
class SonificationMapper {
    /**
     * Map data value to frequency.
     */
    fun mapToFrequency(value: Float, min: Float, max: Float, minFreq: Float = 200f, maxFreq: Float = 1000f): Float {
        val normalized = ((value - min) / (max - min)).coerceIn(0f, 1f)
        return minFreq + normalized * (maxFreq - minFreq)
    }

    /**
     * Map data value to amplitude.
     */
    fun mapToAmplitude(value: Float, min: Float, max: Float): Float {
        return ((value - min) / (max - min)).coerceIn(0f, 1f)
    }

    /**
     * Map data value to panning.
     */
    fun mapToPanning(value: Float, min: Float, max: Float): Float {
        return ((value - min) / (max - min)).coerceIn(-1f, 1f)
    }

    /**
     * Map data value to duration.
     */
    fun mapToDuration(value: Float, min: Float, max: Float, minDuration: Long = 50L, maxDuration: Long = 200L): Long {
        val normalized = ((value - min) / (max - min)).coerceIn(0f, 1f)
        return (minDuration + normalized * (maxDuration - minDuration)).toLong()
    }
}
