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
public class AudioHAL(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val capabilities = AndroidPlatformDetector().detect()
    @Volatile private var playbackThread: Thread? = null

    /**
     * Play a spatial audio source.
     *
     * Selects the best available playback method based on the device's
     * [AudioLevel] capability.
     *
     * @param source The audio source to play.
     */
    public fun playSpatial(source: AudioSource) {
        val thread = Thread {
            try {
                when (capabilities.audioLevel) {
                    AudioLevel.SPATIALIZER_OBOE -> playWithSpatializer(source)
                    AudioLevel.SPATIALIZER_BASIC -> playWithSpatializer(source)
                    AudioLevel.STEREO_ONLY -> playStereo(source)
                    AudioLevel.MONO_ONLY -> playMono(source)
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        thread.isDaemon = true
        playbackThread = thread
        thread.start()
    }

    /**
     * Stop playback and release the background thread.
     */
    public fun stop() {
        playbackThread?.interrupt()
        playbackThread = null
    }

    /**
     * Play audio using the Spatializer API (API 32+).
     *
     * Creates a stereo AudioTrack, writes generated PCM data on a
     * background thread to avoid ANR, and releases resources in a
     * finally block.
     */
    private fun playWithSpatializer(source: AudioSource) {
        val buffer = generateStereoSineBuffer(source.frequency, source.amplitude, 48000, 500L, source.spatialX)
        playAudioTrack(buffer, 48000, AudioFormat.CHANNEL_OUT_STEREO)
    }

    private fun playStereo(source: AudioSource) {
        val buffer = generateStereoSineBuffer(source.frequency, source.amplitude, 44100, 500L, source.spatialX)
        playAudioTrack(buffer, 44100, AudioFormat.CHANNEL_OUT_STEREO)
    }

    private fun playMono(source: AudioSource) {
        val buffer = generateMonoSineBuffer(source.frequency, source.amplitude, 22050, 500L)
        playAudioTrack(buffer, 22050, AudioFormat.CHANNEL_OUT_MONO)
    }

    private fun playAudioTrack(buffer: ShortArray, sampleRate: Int, channelMask: Int) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(channelMask)
            .build()

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
        durationMs: Long,
        panning: Float = 0.5f
    ): ShortArray {
        val totalSamples = (sampleRate * durationMs / 1000).toInt()
        val buffer = ShortArray(totalSamples * 2)
        val vol = amplitude.coerceIn(0f, 1f)
        val leftGain = 1.0f - (panning * 0.5f)
        val rightGain = 0.5f + (panning * 0.5f)
        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val value = (vol * 32767 * sin(2.0 * Math.PI * frequency * t)).toInt()
                .coerceIn(-32768, 32767).toShort()
            val leftValue = (value * leftGain).toInt().coerceIn(-32768, 32767).toShort()
            val rightValue = (value * rightGain).toInt().coerceIn(-32768, 32767).toShort()
            buffer[i * 2] = leftValue
            buffer[i * 2 + 1] = rightValue
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
    public fun hasSpatialAudioSupport(): Boolean =
        capabilities.audioLevel == AudioLevel.SPATIALIZER_OBOE ||
        capabilities.audioLevel == AudioLevel.SPATIALIZER_BASIC

    /**
     * Get the detected audio capability level.
     */
    public fun getAudioLevel(): AudioLevel = capabilities.audioLevel
}
