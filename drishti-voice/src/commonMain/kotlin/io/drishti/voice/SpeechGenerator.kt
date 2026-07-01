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

import io.drishti.core.SpeechSegment

/**
 * Generates speech segments from text.
 */
public class SpeechGenerator {

    private companion object {
        private val VALID_RATE_RANGE = 0.1f..3.0f
        private val VALID_PITCH_RANGE = 0.1f..3.0f
    }

    /**
     * Generate speech segment from text.
     */
    public fun generate(text: String, rate: Float = 1.0f, pitch: Float = 1.0f): SpeechSegment {
        return SpeechSegment(
            text = text,
            rate = rate.coerceIn(VALID_RATE_RANGE),
            pitch = pitch.coerceIn(VALID_PITCH_RANGE)
        )
    }

    /**
     * Generate number speech.
     */
    public fun generateNumber(value: Float, decimalPlaces: Int = 1): SpeechSegment {
        val formatted = "%.${decimalPlaces}f".format(value)
        return SpeechSegment(text = formatted, rate = 1.0f, pitch = 1.0f)
    }

    /**
     * Generate coordinate speech.
     */
    public fun generateCoordinate(x: Float, y: Float): SpeechSegment {
        return SpeechSegment(
            text = "Position: x=${"%.1f".format(x)}, y=${"%.1f".format(y)}.",
            rate = 1.0f,
            pitch = 1.0f
        )
    }

    /**
     * Generate list speech.
     */
    public fun generateList(items: List<String>): SpeechSegment {
        val text = when {
            items.isEmpty() -> "Empty list."
            items.size == 1 -> "One item: ${items[0]}."
            else -> "${items.size} items: ${items.joinToString(", ")}."
        }
        return SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f)
    }
}
