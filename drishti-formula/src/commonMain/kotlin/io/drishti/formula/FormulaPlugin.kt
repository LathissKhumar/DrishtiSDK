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
class FormulaPlugin : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {

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
    fun detectLatex(
        latex: String,
        formulaType: FormulaType? = null
    ): ParsedFormula = detector.detectFromLatex(latex, formulaType)

    /**
     * Detect a formula from Unicode math text.
     *
     * @param text Text with Unicode math symbols (∫, ∑, π, etc.)
     * @return Parsed formula, or null if no formula detected
     */
    fun detectUnicode(text: String): ParsedFormula? = detector.detectFromUnicode(text)

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
    fun renderHaptic(formula: ParsedFormula): HapticOutput = renderer.renderHaptic(formula)

    override fun renderExplorationHaptic(
        item: ContentItem,
        direction: ExplorationDirection
    ): HapticOutput {
        val base = when (item) {
            is ParsedFormula -> renderer.renderHaptic(item)
            is FormulaContent -> renderer.renderHaptic(item)
            else -> return HapticOutput(pulses = emptyList(), pattern = "exploration")
        }
        val pulses = when (direction) {
            ExplorationDirection.NEXT -> base.pulses.takeLast(1).map { it.copy(intensity = (it.intensity * 1.2f).coerceAtMost(1f)) }
            ExplorationDirection.PREVIOUS -> base.pulses.take(1).map { it.copy(intensity = (it.intensity * 1.2f).coerceAtMost(1f)) }
            ExplorationDirection.POSITION -> base.pulses
        }
        return HapticOutput(pulses = pulses, pattern = "formula_explore_${direction.name.lowercase()}")
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
    fun renderAudio(formula: ParsedFormula): AudioOutput = renderer.renderAudio(formula)

    override fun renderExplorationAudio(
        item: ContentItem,
        direction: ExplorationDirection
    ): AudioOutput {
        val base = when (item) {
            is ParsedFormula -> renderer.renderAudio(item)
            is FormulaContent -> renderer.renderAudio(item)
            else -> return AudioOutput(sources = emptyList(), spatial = true)
        }
        val sources = when (direction) {
            ExplorationDirection.NEXT -> base.sources.takeLast(1)
            ExplorationDirection.PREVIOUS -> base.sources.take(1)
            ExplorationDirection.POSITION -> base.sources
        }
        return AudioOutput(sources = sources, spatial = true)
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
    fun renderVoice(formula: ParsedFormula): VoiceOutput = renderer.renderVoice(formula)

    override fun renderExplorationVoice(
        item: ContentItem,
        direction: ExplorationDirection
    ): VoiceOutput = when (item) {
        is ParsedFormula -> {
            val text = when (direction) {
                ExplorationDirection.NEXT -> "Next symbol in formula: ${item.latex}"
                ExplorationDirection.PREVIOUS -> "Previous symbol in formula: ${item.latex}"
                ExplorationDirection.POSITION -> renderer.renderVoice(item).speech.text
            }
            VoiceOutput(speech = SpeechSegment(text = text, rate = 0.9f, pitch = 1.0f), language = "en-US")
        }
        is FormulaContent -> {
            val text = when (direction) {
                ExplorationDirection.NEXT -> "Next symbol: ${item.symbols.lastOrNull()?.value ?: "none"}"
                ExplorationDirection.PREVIOUS -> "Previous symbol: ${item.symbols.firstOrNull()?.value ?: "none"}"
                ExplorationDirection.POSITION -> renderer.renderVoice(item).speech.text
            }
            VoiceOutput(speech = SpeechSegment(text = text, rate = 0.9f, pitch = 1.0f), language = "en-US")
        }
        else -> VoiceOutput(
            speech = SpeechSegment(text = "Exploration", rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }
}
