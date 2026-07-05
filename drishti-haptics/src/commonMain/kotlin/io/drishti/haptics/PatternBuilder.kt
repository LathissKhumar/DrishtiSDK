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

package io.drishti.haptics

import io.drishti.core.ContentType

/**
 * Haptic primitive types for VibrationEffect.Composition (API 30+).
 *
 * Maps to Android VibrationEffect.Composition primitives:
 * - CLICK → sharp, instantaneous tap
 * - TICK → short, light tap
 * - LOW_TICK → low-frequency gentle tap
 * - SWEEP → amplitude ramp
 * - QUICK_RISE → fast amplitude increase
 * - SLOW_RISE → gradual amplitude increase
 * - QUICK_FALL → fast amplitude decrease
 * - SLOW_FALL → gradual amplitude decrease
 * - SPIN → rotating vibration
 * - THUD → heavy, low-frequency impact
 */
public enum class HapticPrimitiveType {
    CLICK,
    TICK,
    LOW_TICK,
    SWEEP,
    QUICK_RISE,
    SLOW_RISE,
    QUICK_FALL,
    SLOW_FALL,
    SPIN,
    THUD
}

/**
 * A single haptic primitive in a composition pattern.
 *
 * Represents one vibration primitive with timing and intensity parameters
 * for VibrationEffect.Composition (API 30+).
 *
 * @param type The primitive vibration type
 * @param intensity Amplitude 0.0-1.0 (maps to VibrationEffect scale 0.0-1.0)
 * @param frequency Frequency in Hz (platform-specific mapping)
 * @param duration Duration in milliseconds
 * @param delay Delay before this primitive in milliseconds
 */
public data class HapticPrimitive(
    val type: HapticPrimitiveType,
    val intensity: Float,
    val frequency: Float,
    val duration: Long,
    val delay: Long
)

/**
 * A complete haptic pattern composed of sequenced primitives.
 *
 * @param id Unique pattern identifier
 * @param duration Total pattern duration in milliseconds
 * @param primitives Ordered list of haptic primitives
 */
public data class HapticPattern(
    val id: String,
    val duration: Long,
    val primitives: List<HapticPrimitive>
)

/**
 * Builds haptic patterns for different content types.
 *
 * Supports both the legacy string-based API (for backward compatibility
 * with [HapticRenderer]) and the new builder API for constructing
 * real haptic patterns with timing, intensity, and frequency data.
 *
 * Usage (new API):
 * ```
 * val pattern = PatternBuilder.forContentType(ContentType.GRAPH)
 *     .addItem()
 *     .setFocus(0)
 *     .addPrimitive(HapticPrimitiveType.CLICK, intensity = 0.8f)
 *     .addPrimitive(HapticPrimitiveType.TICK, intensity = 0.5f, delay = 50)
 *     .build()
 * ```
 *
 * Usage (legacy API, backward compatible):
 * ```
 * val builder = PatternBuilder()
 * val patternId = builder.buildPattern(itemCount = 3, focusIndex = 1)
 * ```
 */
public class PatternBuilder private constructor(
    private val contentType: ContentType?
) {
    /**
     * Data class for accumulating haptic primitive parameters during build.
     */
    private data class PrimitiveData(
        val type: HapticPrimitiveType,
        val intensity: Float,
        val frequency: Float,
        val duration: Long,
        val delay: Long
    )

    private val primitives = mutableListOf<PrimitiveData>()
    private var itemCount = 0
    private var focusIndex = 0

    /**
     * Create a PatternBuilder with no content type (legacy mode).
     */
    public constructor() : this(contentType = null)

    public companion object {
        /**
         * Create a PatternBuilder for a specific content type.
         *
         * @param contentType The content type this pattern will represent
         * @return A new PatternBuilder configured for the content type
         */
        public fun forContentType(contentType: ContentType): PatternBuilder {
            return PatternBuilder(contentType = contentType)
        }
    }

    // ── Builder API ──────────────────────────────────────────────────

    /**
     * Increment the item count for this pattern.
     *
     * @return This builder for chaining
     */
    public fun addItem(): PatternBuilder {
        itemCount++
        return this
    }

    /**
     * Set the focus index for this pattern.
     *
     * @param index The index of the focused item
     * @return This builder for chaining
     */
    public fun setFocus(index: Int): PatternBuilder {
        focusIndex = index
        return this
    }

    /**
     * Add a haptic primitive to the pattern.
     *
     * @param type The primitive vibration type
     * @param intensity Amplitude 0.0-1.0, defaults to 0.5
     * @param frequency Frequency in Hz, defaults to 100
     * @param duration Duration in milliseconds, defaults to 50
     * @param delay Delay before this primitive in milliseconds, defaults to 0
     * @return This builder for chaining
     */
    public fun addPrimitive(
        type: HapticPrimitiveType,
        intensity: Float = 0.5f,
        frequency: Float = 100f,
        duration: Long = 50,
        delay: Long = 0
    ): PatternBuilder {
        primitives.add(
            PrimitiveData(
                type = type,
                intensity = intensity.coerceIn(0f, 1f),
                frequency = frequency.coerceAtLeast(0f),
                duration = duration.coerceAtLeast(0),
                delay = delay.coerceAtLeast(0)
            )
        )
        return this
    }

    /**
     * Build the haptic pattern from accumulated primitives.
     *
     * Note: This builder is single-use. After build(), internal state is cleared
     * and the builder cannot be reused. Create a new PatternBuilder for the next pattern.
     *
     * @return The constructed [HapticPattern].
     * @throws IllegalStateException if no primitives have been added
     */
    public fun build(): HapticPattern {
        val totalDuration = primitives.sumOf { it.duration + it.delay }
        val hapticPrimitives = primitives.map { p ->
            HapticPrimitive(
                type = p.type,
                intensity = p.intensity,
                frequency = p.frequency,
                duration = p.duration,
                delay = p.delay
            )
        }
        val contentTypeName = contentType?.name?.lowercase() ?: "default"
        val pattern = HapticPattern(
            id = "pattern_${contentTypeName}_${itemCount}_focus_${focusIndex}",
            duration = totalDuration,
            primitives = hapticPrimitives
        )
        primitives.clear()
        itemCount = 0
        focusIndex = 0
        return pattern
    }

    // ── Legacy API (backward compatible) ─────────────────────────────

    /**
     * Build pattern string for content.
     *
     * This is the legacy API preserved for backward compatibility with
     * [io.drishti.haptics.HapticRenderer].
     *
     * @param itemCount Number of items in the content
     * @param focusIndex Index of the focused item
     * @return Pattern identifier string
     */
    public fun buildPattern(itemCount: Int, focusIndex: Int): String {
        return buildString {
            append("content_${itemCount}_focus_${focusIndex}")
        }
    }

    /**
     * Build exploration pattern.
     *
     * @param direction Exploration direction (e.g., "next", "previous")
     * @return Pattern identifier string
     */
    public fun buildExplorationPattern(direction: String): String {
        return "exploration_$direction"
    }

    /**
     * Build notification pattern.
     *
     * @param type Notification type (e.g., "alert", "warning")
     * @return Pattern identifier string
     */
    public fun buildNotificationPattern(type: String): String {
        return "notification_$type"
    }
}
