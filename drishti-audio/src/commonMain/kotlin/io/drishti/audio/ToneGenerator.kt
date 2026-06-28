package io.drishti.audio

/**
 * Generates audio tones for sonification.
 */
class ToneGenerator {
    /**
     * Generate a sine wave tone.
     */
    fun generateSineWave(frequency: Float, duration: Long, sampleRate: Int = 44100): FloatArray {
        val samples = (sampleRate * duration / 1000).toInt()
        return FloatArray(samples) { i ->
            val t = i.toFloat() / sampleRate
            (2.0 * Math.PI * frequency * t).toFloat().let { Math.sin(it.toDouble()).toFloat() }
        }
    }

    /**
     * Generate a square wave tone.
     */
    fun generateSquareWave(frequency: Float, duration: Long, sampleRate: Int = 44100): FloatArray {
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
    fun generateSawtoothWave(frequency: Float, duration: Long, sampleRate: Int = 44100): FloatArray {
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
    fun generateTriangleWave(frequency: Float, duration: Long, sampleRate: Int = 44100): FloatArray {
        val samples = (sampleRate * duration / 1000).toInt()
        return FloatArray(samples) { i ->
            val t = i.toFloat() / sampleRate
            val phase = (frequency * t) % 1f
            4f * kotlin.math.abs(phase - 0.5f) - 1f
        }
    }

    /**
     * Apply amplitude envelope to samples.
     */
    fun applyEnvelope(samples: FloatArray, attack: Float = 0.1f, decay: Float = 0.1f, sustain: Float = 0.7f, release: Float = 0.2f): FloatArray {
        val totalSamples = samples.size
        val attackSamples = (totalSamples * attack).toInt()
        val decaySamples = (totalSamples * decay).toInt()
        val sustainSamples = (totalSamples * sustain).toInt()
        val releaseSamples = (totalSamples * release).toInt()

        return FloatArray(totalSamples) { i ->
            val envelope = when {
                i < attackSamples -> i.toFloat() / attackSamples
                i < attackSamples + decaySamples -> 1f - (i - attackSamples).toFloat() / decaySamples * 0.3f
                i < attackSamples + decaySamples + sustainSamples -> 0.7f
                else -> 0.7f * (1f - (i - attackSamples - decaySamples - sustainSamples).toFloat() / releaseSamples)
            }
            samples[i] * envelope.coerceIn(0f, 1f)
        }
    }
}
