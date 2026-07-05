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

/**
 * Generates audio tones for sonification.
 */
public class ToneGenerator {
    /**
     * Generate a sine wave tone.
     */
    public fun generateSineWave(frequency: Float, duration: Long, sampleRate: Int = 44100): FloatArray {
        require(frequency > 0f) { "Frequency must be positive, was $frequency" }
        require(duration > 0) { "Duration must be positive, was $duration" }
        require(sampleRate > 0) { "Sample rate must be positive, was $sampleRate" }
        val samples = (sampleRate * duration / 1000).toInt()
        return FloatArray(samples) { i ->
            val t = i.toFloat() / sampleRate
            (2.0 * Math.PI * frequency * t).toFloat().let { Math.sin(it.toDouble()).toFloat() }
        }
    }

    /**
     * Generate a square wave tone.
     */
    public fun generateSquareWave(frequency: Float, duration: Long, sampleRate: Int = 44100): FloatArray {
        require(frequency > 0f) { "Frequency must be positive, was $frequency" }
        require(duration > 0) { "Duration must be positive, was $duration" }
        require(sampleRate > 0) { "Sample rate must be positive, was $sampleRate" }
        val samples = (sampleRate * duration / 1000).toInt()
        return FloatArray(samples) { i ->
            val t = i.toFloat() / sampleRate
            val phase = (2.0 * Math.PI * frequency * t).toFloat()
            if (Math.sin(phase.toDouble()) > 0) 1f else -1f
        }
    }

    /**
     * Generate a sawtooth wave tone.
     */
    public fun generateSawtoothWave(frequency: Float, duration: Long, sampleRate: Int = 44100): FloatArray {
        require(frequency > 0f) { "Frequency must be positive, was $frequency" }
        require(duration > 0) { "Duration must be positive, was $duration" }
        require(sampleRate > 0) { "Sample rate must be positive, was $sampleRate" }
        val samples = (sampleRate * duration / 1000).toInt()
        return FloatArray(samples) { i ->
            val t = i.toFloat() / sampleRate
            val phase = (frequency * t) % 1f
            2f * phase - 1f
        }
    }

    /**
     * Generate a triangle wave tone.
     */
    public fun generateTriangleWave(frequency: Float, duration: Long, sampleRate: Int = 44100): FloatArray {
        require(frequency > 0f) { "Frequency must be positive, was $frequency" }
        require(duration > 0) { "Duration must be positive, was $duration" }
        require(sampleRate > 0) { "Sample rate must be positive, was $sampleRate" }
        val samples = (sampleRate * duration / 1000).toInt()
        return FloatArray(samples) { i ->
            val t = i.toFloat() / sampleRate
            val phase = (frequency * t) % 1f
            4f * kotlin.math.abs(phase - 0.5f) - 1f
        }
    }

    /**
     * Apply amplitude envelope to samples.
     *
     * All parameters are time proportions (0.0-1.0) that must sum to <= 1.0.
     * Note: [sustainDuration] is the time fraction for the sustain phase, NOT an amplitude level.
     * The sustain amplitude is fixed at 0.7.
     *
     * @param samples The audio samples to apply the envelope to.
     * @param attack Time proportion for the attack phase (0.0-1.0).
     * @param decay Time proportion for the decay phase (0.0-1.0).
     * @param sustainDuration Time proportion for the sustain phase (0.0-1.0), NOT an amplitude level.
     * @param release Time proportion for the release phase (0.0-1.0).
     * @return The samples with the ADSR envelope applied.
     */
    public fun applyEnvelope(samples: FloatArray, attack: Float = 0.1f, decay: Float = 0.1f, sustainDuration: Float = 0.6f, release: Float = 0.2f): FloatArray {
        require(attack + decay + sustainDuration + release <= 1.0f) {
            "ADSR parameters must sum to <= 1.0, was ${attack + decay + sustainDuration + release}"
        }
        val totalSamples = samples.size
        val attackSamples = (totalSamples * attack).toInt()
        val decaySamples = (totalSamples * decay).toInt()
        val sustainSamples = (totalSamples * sustainDuration).toInt()
        val releaseSamples = (totalSamples * release).toInt()

        return FloatArray(totalSamples) { i ->
            val envelope = when {
                i < attackSamples -> if (attackSamples > 0) i.toFloat() / attackSamples else 1f
                i < attackSamples + decaySamples -> 1f - if (decaySamples > 0) (i - attackSamples).toFloat() / decaySamples * 0.3f else 0f
                i < attackSamples + decaySamples + sustainSamples -> 0.7f
                else -> 0.7f * if (releaseSamples > 0) (1f - (i - attackSamples - decaySamples - sustainSamples).toFloat() / releaseSamples) else 0f
            }
            samples[i] * envelope.coerceIn(0f, 1f)
        }
    }
}
