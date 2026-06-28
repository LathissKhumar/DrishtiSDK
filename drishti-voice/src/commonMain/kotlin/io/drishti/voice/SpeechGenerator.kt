package io.drishti.voice

import io.drishti.core.SpeechSegment

/**
 * Generates speech segments from text.
 */
class SpeechGenerator {
    /**
     * Generate speech segment from text.
     */
    fun generate(text: String, rate: Float = 1.0f, pitch: Float = 1.0f): SpeechSegment {
        return SpeechSegment(text = text, rate = rate, pitch = pitch)
    }

    /**
     * Generate number speech.
     */
    fun generateNumber(value: Float, decimalPlaces: Int = 1): SpeechSegment {
        val formatted = "%.${decimalPlaces}f".format(value)
        return SpeechSegment(text = formatted, rate = 1.0f, pitch = 1.0f)
    }

    /**
     * Generate coordinate speech.
     */
    fun generateCoordinate(x: Float, y: Float): SpeechSegment {
        return SpeechSegment(
            text = "Position: x=${"%.1f".format(x)}, y=${"%.1f".format(y)}.",
            rate = 1.0f,
            pitch = 1.0f
        )
    }

    /**
     * Generate list speech.
     */
    fun generateList(items: List<String>): SpeechSegment {
        val text = when {
            items.isEmpty() -> "Empty list."
            items.size == 1 -> "One item: ${items[0]}."
            else -> "${items.size} items: ${items.joinToString(", ")}."
        }
        return SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f)
    }
}
