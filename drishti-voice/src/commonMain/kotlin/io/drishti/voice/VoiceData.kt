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

package io.drishti.voice

import io.drishti.core.ContentType

/**
 * Voice parameter definitions for TTS synthesis.
 *
 * Contains language settings, speech rate/pitch defaults per content type,
 * and Sherpa-ONNX model identifiers for offline TTS/STT.
 *
 * Usage:
 * ```
 * val params = VoiceData.default()
 * val formulaRate = params.rateForContentType(ContentType.Formula)
 * ```
 */
public data class VoiceData(
    /** BCP-47 language tag (e.g., "en-US", "hi-IN"). */
    val language: String = DEFAULT_LANGUAGE,

    /** Base speech rate (1.0 = normal, 0.5 = half speed, 2.0 = double speed). */
    val rate: Float = DEFAULT_RATE,

    /** Base pitch (1.0 = normal, 0.5 = low, 1.5 = high). */
    val pitch: Float = DEFAULT_PITCH,

    /** Speech rate adjustments per content type. */
    val rateAdjustments: Map<ContentType, Float> = defaultRateAdjustments(),

    /** Pitch adjustments per content type. */
    val pitchAdjustments: Map<ContentType, Float> = emptyMap(),

    /** Sherpa-ONNX TTS model identifier (offline synthesis). */
    val ttsModel: String = DEFAULT_TTS_MODEL,

    /** Sherpa-ONNX STT model identifier (offline recognition). */
    val sttModel: String = DEFAULT_STT_MODEL
) {

    /** Speech rate for the given content type, falling back to base rate. */
    public fun rateForContentType(type: ContentType): Float =
        rate * (rateAdjustments[type] ?: 1.0f)

    /** Pitch for the given content type. All content types use the base pitch. */
    public fun pitchForContentType(type: ContentType): Float = pitch

    public companion object {
        public const val DEFAULT_LANGUAGE: String = "en-US"
        public const val DEFAULT_RATE: Float = 1.0f
        public const val DEFAULT_PITCH: Float = 1.0f
        public const val DEFAULT_TTS_MODEL: String = "vits-en-us"
        public const val DEFAULT_STT_MODEL: String = "zipformer-en-us"

        public fun default(): VoiceData = VoiceData()

        public fun withLanguage(language: String): VoiceData =
            VoiceData(language = language)

        private fun defaultRateAdjustments(): Map<ContentType, Float> = mapOf(
            ContentType.Formula to 0.85f,
            ContentType.Graph to 1.0f,
            ContentType.Molecule to 0.95f,
            ContentType.Shape to 1.0f,
            ContentType.Table to 0.9f
        )
    }
}
