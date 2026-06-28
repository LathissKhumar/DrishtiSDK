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
 * val formulaRate = params.rateForContentType(ContentType.FORMULA)
 * ```
 */
data class VoiceData(
    /** BCP-47 language tag (e.g., "en-US", "hi-IN"). */
    val language: String = DEFAULT_LANGUAGE,

    /** Base speech rate (1.0 = normal, 0.5 = half speed, 2.0 = double speed). */
    val rate: Float = DEFAULT_RATE,

    /** Base pitch (1.0 = normal, 0.5 = low, 1.5 = high). */
    val pitch: Float = DEFAULT_PITCH,

    /** Speech rate adjustments per content type. */
    val rateAdjustments: Map<ContentType, Float> = defaultRateAdjustments(),

    /** Pitch adjustments per content type. */
    val pitchAdjustments: Map<ContentType, Float> = defaultPitchAdjustments(),

    /** Sherpa-ONNX TTS model identifier (offline synthesis). */
    val ttsModel: String = DEFAULT_TTS_MODEL,

    /** Sherpa-ONNX STT model identifier (offline recognition). */
    val sttModel: String = DEFAULT_STT_MODEL
) {

    /** Speech rate for the given content type, falling back to base rate. */
    fun rateForContentType(type: ContentType): Float =
        rate * (rateAdjustments[type] ?: 1.0f)

    /** Pitch for the given content type, falling back to base pitch. */
    fun pitchForContentType(type: ContentType): Float =
        pitch * (pitchAdjustments[type] ?: 1.0f)

    companion object {
        const val DEFAULT_LANGUAGE = "en-US"
        const val DEFAULT_RATE = 1.0f
        const val DEFAULT_PITCH = 1.0f
        const val DEFAULT_TTS_MODEL = "vits-en-us"
        const val DEFAULT_STT_MODEL = "zipformer-en-us"

        fun default(): VoiceData = VoiceData()

        fun withLanguage(language: String): VoiceData =
            VoiceData(language = language)

        private fun defaultRateAdjustments(): Map<ContentType, Float> = mapOf(
            ContentType.FORMULA to 0.85f,
            ContentType.GRAPH to 1.0f,
            ContentType.MOLECULE to 0.95f,
            ContentType.SHAPE to 1.0f,
            ContentType.TABLE to 0.9f,
            ContentType.CUSTOM to 1.0f
        )

        private fun defaultPitchAdjustments(): Map<ContentType, Float> = mapOf(
            ContentType.FORMULA to 1.0f,
            ContentType.GRAPH to 1.0f,
            ContentType.MOLECULE to 1.0f,
            ContentType.SHAPE to 1.0f,
            ContentType.TABLE to 1.0f,
            ContentType.CUSTOM to 1.0f
        )
    }
}
