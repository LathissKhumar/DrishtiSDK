package io.drishti.android

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import io.drishti.core.AudioSource
import kotlin.math.sin

/**
 * Hardware Abstraction Layer for audio output.
 *
 * Provides spatial, stereo, and mono audio playback with automatic
 * capability detection. Uses Android's Spatializer API when available,
 * falling back to standard stereo or mono playback on older devices.
 *
 * @param context Android context used to obtain system services.
 */
class AudioHAL(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val capabilities = AndroidPlatformDetector().detect()

    /**
     * Play a spatial audio source.
     *
     * Selects the best available playback method based on the device's
     * [AudioLevel] capability.
     *
     * @param source The audio source to play.
     */
    fun playSpatial(source: AudioSource) {
        Thread {
            when (capabilities.audioLevel) {
                AudioLevel.SPATIALIZER_OBOE -> playWithSpatializer(source)
                AudioLevel.SPATIALIZER_BASIC -> playWithSpatializer(source)
                AudioLevel.STEREO_ONLY -> playStereo(source)
                AudioLevel.MONO_ONLY -> playMono(source)
            }
        }.start()
    }

    /**
     * Play audio using the Spatializer API (API 32+).
     *
     * Creates a stereo AudioTrack, writes generated PCM data on a
     * background thread to avoid ANR, and releases resources in a
     * finally block.
     */
    private fun playWithSpatializer(source: AudioSource) {
        val sampleRate = 48000
        val durationMs = 500L

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()

        val buffer = generateStereoSineBuffer(source.frequency, source.amplitude, sampleRate, durationMs)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(buffer.size * 2) // 16-bit = 2 bytes per sample
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            audioTrack.play()
            val writeThread = Thread {
                audioTrack.write(buffer, 0, buffer.size)
            }
            writeThread.start()
            writeThread.join(5000) // 5s timeout to avoid indefinite block
            try { Thread.sleep(durationMs) } catch (_: InterruptedException) { }
        } finally {
            try { audioTrack.stop() } catch (_: IllegalStateException) { }
            audioTrack.release()
        }
    }

    /**
     * Play audio in standard stereo mode.
     *
     * Stereo fallback when Spatializer is unavailable. Writes PCM
     * on a background thread and releases in a finally block.
     */
    private fun playStereo(source: AudioSource) {
        val sampleRate = 44100
        val durationMs = 500L

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()

        val buffer = generateStereoSineBuffer(source.frequency, source.amplitude, sampleRate, durationMs)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            audioTrack.play()
            val writeThread = Thread {
                audioTrack.write(buffer, 0, buffer.size)
            }
            writeThread.start()
            writeThread.join(5000)
            try { Thread.sleep(durationMs) } catch (_: InterruptedException) { }
        } finally {
            try { audioTrack.stop() } catch (_: IllegalStateException) { }
            audioTrack.release()
        }
    }

    /**
     * Play audio in mono fallback mode.
     *
     * Mono fallback for the oldest devices. Writes PCM on a
     * background thread and releases in a finally block.
     */
    private fun playMono(source: AudioSource) {
        val sampleRate = 22050
        val durationMs = 500L

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val buffer = generateMonoSineBuffer(source.frequency, source.amplitude, sampleRate, durationMs)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            audioTrack.play()
            val writeThread = Thread {
                audioTrack.write(buffer, 0, buffer.size)
            }
            writeThread.start()
            writeThread.join(5000)
            try { Thread.sleep(durationMs) } catch (_: InterruptedException) { }
        } finally {
            try { audioTrack.stop() } catch (_: IllegalStateException) { }
            audioTrack.release()
        }
    }

    /**
     * Generate stereo PCM 16-bit sine wave (interleaved L/R frames).
     */
    private fun generateStereoSineBuffer(
        frequency: Float,
        amplitude: Float,
        sampleRate: Int,
        durationMs: Long
    ): ShortArray {
        val totalSamples = (sampleRate * durationMs / 1000).toInt()
        val buffer = ShortArray(totalSamples * 2)
        val vol = amplitude.coerceIn(0f, 1f)
        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val value = (vol * 32767 * sin(2.0 * Math.PI * frequency * t)).toInt()
                .coerceIn(-32768, 32767).toShort()
            buffer[i * 2] = value
            buffer[i * 2 + 1] = value
        }
        return buffer
    }

    /**
     * Generate mono PCM 16-bit sine wave (single channel).
     */
    private fun generateMonoSineBuffer(
        frequency: Float,
        amplitude: Float,
        sampleRate: Int,
        durationMs: Long
    ): ShortArray {
        val totalSamples = (sampleRate * durationMs / 1000).toInt()
        val buffer = ShortArray(totalSamples)
        val vol = amplitude.coerceIn(0f, 1f)
        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val value = (vol * 32767 * sin(2.0 * Math.PI * frequency * t)).toInt()
                .coerceIn(-32768, 32767).toShort()
            buffer[i] = value
        }
        return buffer
    }

    /**
     * Whether the device supports spatial audio output.
     */
    fun hasSpatialAudioSupport(): Boolean =
        capabilities.audioLevel == AudioLevel.SPATIALIZER_OBOE ||
        capabilities.audioLevel == AudioLevel.SPATIALIZER_BASIC

    /**
     * Get the detected audio capability level.
     */
    fun getAudioLevel(): AudioLevel = capabilities.audioLevel
}
