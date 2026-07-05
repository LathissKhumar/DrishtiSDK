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

package io.drishti.formula

import io.drishti.core.AudioOutput
import io.drishti.core.ContentItem
import io.drishti.core.ContentType
import io.drishti.core.DetectorPlugin
import io.drishti.core.ExplorationDirection
import io.drishti.core.FormulaContent
import io.drishti.core.FormulaType
import io.drishti.core.Frame
import io.drishti.core.HapticOutput
import io.drishti.core.HapticsRenderer
import io.drishti.core.AudioRenderer
import io.drishti.core.SpeechSegment
import io.drishti.core.VoiceOutput
import io.drishti.core.VoiceOutputRenderer

/**
 * Complete formula plugin combining detection and rendering.
 *
 * Provides LaTeX-first entry points for formula processing, with
 * backward-compatible support for [FormulaContent] inputs.
 *
 * Usage:
 * ```
 * val plugin = FormulaPlugin()
 *
 * // LaTeX-first (preferred)
 * val formula = plugin.detectLatex("\\frac{1}{2}")
 * val haptic = plugin.renderHaptic(formula)
 *
 * // Legacy API still works
 * val items = listOf(formulaContent)
 * val output = plugin.renderHaptic(items)
 * ```
 */
public class FormulaPlugin : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {

    private val detector = FormulaDetector()
    private val renderer = FormulaRenderer()

    override val name: String = "formula"
    override val contentType: ContentType = ContentType.FORMULA
    override val confidence: Float = detector.confidence

    // ── DetectorPlugin ────────────────────────────────────────────────

    override suspend fun detect(frame: Frame): ContentItem? {
        return detector.detect(frame)
    }

    /**
     * Detect a formula from a LaTeX string.
     *
     * @param latex LaTeX math expression
     * @param formulaType Optional formula type hint
     * @return Parsed formula with AST, evaluation, and speech text
     */
    public fun detectLatex(
        latex: String,
        formulaType: FormulaType? = null
    ): ParsedFormula = detector.detectFromLatex(latex, formulaType)

    /**
     * Detect a formula from Unicode math text.
     *
     * @param text Text with Unicode math symbols (∫, ∑, π, etc.)
     * @return Parsed formula, or null if no formula detected
     */
    public fun detectUnicode(text: String): ParsedFormula? = detector.detectFromUnicode(text)

    // ── HapticsRenderer ───────────────────────────────────────────────

    override fun renderHaptic(items: List<ContentItem>, focusIndex: Int): HapticOutput {
        val pulses = items.mapIndexedNotNull { index, item ->
            if (focusIndex in items.indices && index != focusIndex) return@mapIndexedNotNull null
            when (item) {
                is ParsedFormula -> renderer.renderHaptic(item).pulses
                is FormulaContent -> renderer.renderHaptic(item).pulses
                else -> emptyList()
            }
        }.flatten()
        val pattern = if (items.size > 1 && focusIndex in items.indices) {
            "formula_haptic_focus_$focusIndex"
        } else {
            "formula_haptic"
        }
        return HapticOutput(pulses = pulses, pattern = pattern)
    }

    /**
     * Render a single [ParsedFormula] as haptic output.
     */
    public fun renderHaptic(formula: ParsedFormula): HapticOutput = renderer.renderHaptic(formula)

    private fun computeExplorationIndex(
        elementIndex: Int,
        direction: ExplorationDirection,
        symbolCount: Int
    ): Int {
        if (elementIndex >= 0) return elementIndex
        return when (direction) {
            ExplorationDirection.NEXT -> 0
            ExplorationDirection.PREVIOUS -> symbolCount - 1
            ExplorationDirection.POSITION -> 0
        }
    }

    override fun renderExplorationHaptic(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): HapticOutput {
        val base = when (item) {
            is ParsedFormula -> {
                val idx = computeExplorationIndex(elementIndex, direction, item.symbols.size)
                renderer.renderHaptic(item).let { baseOutput ->
                    val singlePulse = baseOutput.pulses.getOrNull(idx)
                    HapticOutput(
                        pulses = if (singlePulse != null) listOf(singlePulse.copy(intensity = (singlePulse.intensity * 1.2f).coerceAtMost(1f))) else emptyList(),
                        pattern = "formula_explore_symbol_$idx"
                    )
                }
            }
            is FormulaContent -> {
                val idx = computeExplorationIndex(elementIndex, direction, item.symbols.size)
                renderer.renderHaptic(item).let { baseOutput ->
                    val singlePulse = baseOutput.pulses.getOrNull(idx)
                    HapticOutput(
                        pulses = if (singlePulse != null) listOf(singlePulse.copy(intensity = (singlePulse.intensity * 1.2f).coerceAtMost(1f))) else emptyList(),
                        pattern = "formula_explore_symbol_$idx"
                    )
                }
            }
            else -> return HapticOutput(pulses = emptyList(), pattern = "exploration")
        }
        return base
    }

    // ── AudioRenderer ─────────────────────────────────────────────────

    override fun renderAudio(items: List<ContentItem>, focusIndex: Int): AudioOutput {
        val sources = items.mapIndexedNotNull { index, item ->
            if (focusIndex in items.indices && index != focusIndex) return@mapIndexedNotNull null
            when (item) {
                is ParsedFormula -> renderer.renderAudio(item).sources
                is FormulaContent -> renderer.renderAudio(item).sources
                else -> emptyList()
            }
        }.flatten()
        return AudioOutput(sources = sources, spatial = true)
    }

    /**
     * Render a single [ParsedFormula] as audio output.
     */
    public fun renderAudio(formula: ParsedFormula): AudioOutput = renderer.renderAudio(formula)

    override fun renderExplorationAudio(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): AudioOutput {
        val base = when (item) {
            is ParsedFormula -> {
                val idx = computeExplorationIndex(elementIndex, direction, item.symbols.size)
                renderer.renderAudio(item).let { baseOutput ->
                    val singleSource = baseOutput.sources.getOrNull(idx)
                    AudioOutput(
                        sources = if (singleSource != null) listOf(singleSource) else emptyList(),
                        spatial = true
                    )
                }
            }
            is FormulaContent -> {
                val idx = computeExplorationIndex(elementIndex, direction, item.symbols.size)
                renderer.renderAudio(item).let { baseOutput ->
                    val singleSource = baseOutput.sources.getOrNull(idx)
                    AudioOutput(
                        sources = if (singleSource != null) listOf(singleSource) else emptyList(),
                        spatial = true
                    )
                }
            }
            else -> return AudioOutput(sources = emptyList(), spatial = true)
        }
        return base
    }

    // ── VoiceOutputRenderer ───────────────────────────────────────────

    override fun renderVoice(items: List<ContentItem>, focusIndex: Int): VoiceOutput {
        if (items.isEmpty()) {
            return VoiceOutput(
                speech = SpeechSegment(text = "No formula content", rate = 1.0f, pitch = 1.0f),
                language = "en-US"
            )
        }
        val speeches = items.mapIndexedNotNull { index, item ->
            if (focusIndex in items.indices && index != focusIndex) return@mapIndexedNotNull null
            val speech = when (item) {
                is ParsedFormula -> renderer.renderVoice(item).speech
                is FormulaContent -> renderer.renderVoice(item).speech
                else -> null
            }
            if (speech != null && focusIndex in items.indices && index == focusIndex) {
                SpeechSegment(
                    text = "Formula ${index + 1} of ${items.size}. ${speech.text}",
                    rate = speech.rate,
                    pitch = speech.pitch
                )
            } else {
                speech
            }
        }
        val combinedText = speeches.joinToString(" ") { it.text }
        return VoiceOutput(
            speech = SpeechSegment(text = combinedText, rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }

    /**
     * Render a single [ParsedFormula] as voice output.
     */
    public fun renderVoice(formula: ParsedFormula): VoiceOutput = renderer.renderVoice(formula)

    override fun renderExplorationVoice(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): VoiceOutput = when (item) {
        is ParsedFormula -> {
            val idx = computeExplorationIndex(elementIndex, direction, item.symbols.size)
            val symbol = item.symbols.getOrNull(idx)
            val text = if (symbol != null) {
                "Symbol ${idx + 1}: ${symbol.value}"
            } else {
                when (direction) {
                    ExplorationDirection.NEXT -> "Next symbol in formula: ${item.latex}"
                    ExplorationDirection.PREVIOUS -> "Previous symbol in formula: ${item.latex}"
                    ExplorationDirection.POSITION -> renderer.renderVoice(item).speech.text
                }
            }
            VoiceOutput(speech = SpeechSegment(text = text, rate = 0.9f, pitch = 1.0f), language = "en-US")
        }
        is FormulaContent -> {
            val idx = computeExplorationIndex(elementIndex, direction, item.symbols.size)
            val symbol = item.symbols.getOrNull(idx)
            val text = if (symbol != null) {
                "Symbol ${idx + 1}: ${symbol.value}"
            } else {
                when (direction) {
                    ExplorationDirection.NEXT -> "Next symbol: ${item.symbols.firstOrNull()?.value ?: "none"}"
                    ExplorationDirection.PREVIOUS -> "Previous symbol: ${item.symbols.lastOrNull()?.value ?: "none"}"
                    ExplorationDirection.POSITION -> renderer.renderVoice(item).speech.text
                }
            }
            VoiceOutput(speech = SpeechSegment(text = text, rate = 0.9f, pitch = 1.0f), language = "en-US")
        }
        else -> VoiceOutput(
            speech = SpeechSegment(text = "Exploration", rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }
}
