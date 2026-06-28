package io.drishti.formula

import io.drishti.core.AudioOutput
import io.drishti.core.AudioSource
import io.drishti.core.FormulaContent
import io.drishti.core.FormulaSymbol
import io.drishti.core.HapticOutput
import io.drishti.core.HapticPulse
import io.drishti.core.SpeechSegment
import io.drishti.core.SymbolType
import io.drishti.core.VoiceOutput

/**
 * Renders formula content as haptic, audio, and voice outputs.
 *
 * Supports both [ParsedFormula] (AST-based rendering) and [FormulaContent]
 * (legacy symbol-based rendering for backward compatibility).
 */
class FormulaRenderer {

    /**
     * Render a [ParsedFormula] as haptic output using AST traversal.
     *
     * Each AST node produces a haptic pulse with intensity and duration
     * proportional to the node's structural significance.
     */
    fun renderHaptic(formula: ParsedFormula): HapticOutput {
        val pulses = mutableListOf<HapticPulse>()
        formula.ast.visit(0.5f, 0.3f) { node, x, y ->
            val intensity = nodeIntensity(node)
            val duration = nodeDuration(node)
            pulses.add(HapticPulse(intensity = intensity, duration = duration, x = x, y = y))
        }
        return HapticOutput(pulses = pulses, pattern = "formula_ast")
    }

    /**
     * Render a [FormulaContent] as haptic output (backward compatibility).
     */
    fun renderHaptic(formula: FormulaContent): HapticOutput {
        if (formula.symbols.isEmpty() && formula.expression.isNotEmpty()) {
            val parsed = try {
                ParsedFormula.fromFormulaContent(formula)
            } catch (_: Exception) {
                null
            }
            if (parsed != null) {
                return renderHaptic(parsed)
            }
        }
        val pulses = formula.symbols.map { symbol ->
            HapticPulse(
                intensity = symbolIntensity(symbol.type),
                duration = symbolDuration(symbol.type),
                x = normalizePosition(symbol.position.x),
                y = normalizePosition(symbol.position.y)
            )
        }
        return HapticOutput(pulses = pulses, pattern = "formula_exploration")
    }

    /**
     * Render a [ParsedFormula] as audio output using AST traversal.
     */
    fun renderAudio(formula: ParsedFormula): AudioOutput {
        val sources = mutableListOf<AudioSource>()
        formula.ast.visit(0.5f, 0.5f) { node, x, y ->
            val freq = nodeFrequency(node)
            val amp = nodeAmplitude(node)
            sources.add(AudioSource(frequency = freq, amplitude = amp, spatialX = x, spatialY = y, spatialZ = 0.5f))
        }
        return AudioOutput(sources = sources, spatial = true)
    }

    /**
     * Render a [FormulaContent] as audio output (backward compatibility).
     */
    fun renderAudio(formula: FormulaContent): AudioOutput {
        if (formula.symbols.isEmpty() && formula.expression.isNotEmpty()) {
            val parsed = try {
                ParsedFormula.fromFormulaContent(formula)
            } catch (_: Exception) {
                null
            }
            if (parsed != null) {
                return renderAudio(parsed)
            }
        }
        val sources = formula.symbols.map { symbol ->
            AudioSource(
                frequency = symbolFrequency(symbol.type),
                amplitude = symbolAmplitude(symbol.type),
                spatialX = normalizePosition(symbol.position.x),
                spatialY = normalizePosition(symbol.position.y),
                spatialZ = 0.5f
            )
        }
        return AudioOutput(sources = sources, spatial = true)
    }

    /**
     * Render a [ParsedFormula] as voice output using the pre-computed speech text.
     */
    fun renderVoice(formula: ParsedFormula): VoiceOutput {
        val speech = if (formula.speechText.isNotEmpty()) {
            SpeechSegment(text = formula.speechText, rate = 0.9f, pitch = 1.0f)
        } else {
            SpeechSegment(
                text = "Formula: ${formula.latex}",
                rate = 0.9f,
                pitch = 1.0f
            )
        }
        return VoiceOutput(speech = speech, language = "en-US")
    }

    /**
     * Render a [FormulaContent] as voice output (backward compatibility).
     */
    fun renderVoice(formula: FormulaContent): VoiceOutput {
        val speech = when (formula.formulaType) {
            io.drishti.core.FormulaType.CALCULUS -> describeCalculus(formula)
            io.drishti.core.FormulaType.TRIGONOMETRIC -> describeTrigonometric(formula)
            io.drishti.core.FormulaType.ALGEBRAIC -> describeAlgebraic(formula)
            io.drishti.core.FormulaType.MATHEMATICAL -> describeMathematical(formula)
            io.drishti.core.FormulaType.NOTATION -> describeNotation(formula)
        }
        return VoiceOutput(speech = speech, language = "en-US")
    }

    // ── Node → haptic parameters ─────────────────────────────────────

    private fun nodeIntensity(node: FormulaNode): Float = when (node) {
        is FormulaNode.Integral -> 0.95f
        is FormulaNode.Summation -> 0.95f
        is FormulaNode.Product -> 0.95f
        is FormulaNode.Limit -> 0.9f
        is FormulaNode.Fraction -> 0.85f
        is FormulaNode.BinaryOp -> 0.8f
        is FormulaNode.FunctionCall -> 0.8f
        is FormulaNode.Power -> 0.75f
        is FormulaNode.SquareRoot -> 0.75f
        is FormulaNode.Matrix -> 0.85f
        is FormulaNode.Binomial -> 0.8f
        is FormulaNode.Cases -> 0.75f
        is FormulaNode.Accent -> 0.65f
        is FormulaNode.NamedSymbol -> 0.7f
        is FormulaNode.Number -> 0.65f
        is FormulaNode.Variable -> 0.6f
        is FormulaNode.UnaryMinus -> 0.5f
        is FormulaNode.Subscript -> 0.5f
        is FormulaNode.Group -> 0.4f
        is FormulaNode.AbsoluteValue -> 0.5f
    }

    private fun nodeDuration(node: FormulaNode): Long = when (node) {
        is FormulaNode.Integral -> 160L
        is FormulaNode.Summation -> 160L
        is FormulaNode.Product -> 160L
        is FormulaNode.Limit -> 140L
        is FormulaNode.Fraction -> 120L
        is FormulaNode.BinaryOp -> 80L
        is FormulaNode.FunctionCall -> 100L
        is FormulaNode.Power -> 70L
        is FormulaNode.SquareRoot -> 100L
        is FormulaNode.Matrix -> 140L
        is FormulaNode.Binomial -> 100L
        is FormulaNode.Cases -> 80L
        is FormulaNode.Accent -> 40L
        is FormulaNode.NamedSymbol -> 60L
        is FormulaNode.Number -> 50L
        is FormulaNode.Variable -> 50L
        is FormulaNode.UnaryMinus -> 40L
        is FormulaNode.Subscript -> 40L
        is FormulaNode.Group -> 30L
        is FormulaNode.AbsoluteValue -> 40L
    }

    private fun nodeFrequency(node: FormulaNode): Float = when (node) {
        is FormulaNode.Integral -> 700f
        is FormulaNode.Summation -> 700f
        is FormulaNode.Product -> 700f
        is FormulaNode.Limit -> 650f
        is FormulaNode.Fraction -> 600f
        is FormulaNode.BinaryOp -> 300f
        is FormulaNode.FunctionCall -> 500f
        is FormulaNode.Power -> 450f
        is FormulaNode.SquareRoot -> 450f
        is FormulaNode.Matrix -> 600f
        is FormulaNode.Binomial -> 500f
        is FormulaNode.Cases -> 450f
        is FormulaNode.Accent -> 350f
        is FormulaNode.NamedSymbol -> 400f
        is FormulaNode.Number -> 400f
        is FormulaNode.Variable -> 350f
        is FormulaNode.UnaryMinus -> 300f
        is FormulaNode.Subscript -> 300f
        is FormulaNode.Group -> 250f
        is FormulaNode.AbsoluteValue -> 300f
    }

    private fun nodeAmplitude(node: FormulaNode): Float = when (node) {
        is FormulaNode.Integral -> 0.85f
        is FormulaNode.Summation -> 0.85f
        is FormulaNode.Product -> 0.85f
        is FormulaNode.Limit -> 0.8f
        is FormulaNode.Fraction -> 0.75f
        is FormulaNode.BinaryOp -> 0.7f
        is FormulaNode.FunctionCall -> 0.7f
        is FormulaNode.Power -> 0.6f
        is FormulaNode.SquareRoot -> 0.65f
        is FormulaNode.Matrix -> 0.75f
        is FormulaNode.Binomial -> 0.7f
        is FormulaNode.Cases -> 0.65f
        is FormulaNode.Accent -> 0.5f
        is FormulaNode.NamedSymbol -> 0.6f
        is FormulaNode.Number -> 0.55f
        is FormulaNode.Variable -> 0.5f
        is FormulaNode.UnaryMinus -> 0.4f
        is FormulaNode.Subscript -> 0.4f
        is FormulaNode.Group -> 0.3f
        is FormulaNode.AbsoluteValue -> 0.4f
    }

    // ── Legacy symbol-based parameters ────────────────────────────────

    private fun symbolIntensity(type: SymbolType): Float = when (type) {
        SymbolType.OPERATOR -> 0.9f
        SymbolType.FUNCTION -> 0.8f
        SymbolType.NUMBER -> 0.7f
        SymbolType.VARIABLE -> 0.6f
        SymbolType.BRACKET -> 0.4f
        SymbolType.SUBSCRIPT -> 0.5f
        SymbolType.SUPERSCRIPT -> 0.5f
        SymbolType.FRACTION -> 0.8f
        SymbolType.SUMMATION -> 0.9f
        SymbolType.INTEGRAL -> 0.9f
        SymbolType.GREEK_LETTER -> 0.7f
        SymbolType.EQUALS -> 0.8f
        SymbolType.RELATION -> 0.8f
    }

    private fun symbolDuration(type: SymbolType): Long = when (type) {
        SymbolType.OPERATOR -> 80L
        SymbolType.FUNCTION -> 100L
        SymbolType.NUMBER -> 50L
        SymbolType.VARIABLE -> 50L
        SymbolType.BRACKET -> 30L
        SymbolType.SUBSCRIPT -> 40L
        SymbolType.SUPERSCRIPT -> 40L
        SymbolType.FRACTION -> 120L
        SymbolType.SUMMATION -> 150L
        SymbolType.INTEGRAL -> 150L
        SymbolType.GREEK_LETTER -> 60L
        SymbolType.EQUALS -> 80L
        SymbolType.RELATION -> 80L
    }

    private fun symbolFrequency(type: SymbolType): Float = when (type) {
        SymbolType.OPERATOR -> 300f
        SymbolType.FUNCTION -> 500f
        SymbolType.NUMBER -> 400f
        SymbolType.VARIABLE -> 350f
        SymbolType.BRACKET -> 250f
        SymbolType.SUBSCRIPT -> 300f
        SymbolType.SUPERSCRIPT -> 450f
        SymbolType.FRACTION -> 600f
        SymbolType.SUMMATION -> 700f
        SymbolType.INTEGRAL -> 700f
        SymbolType.GREEK_LETTER -> 400f
        SymbolType.EQUALS -> 350f
        SymbolType.RELATION -> 350f
    }

    private fun symbolAmplitude(type: SymbolType): Float = when (type) {
        SymbolType.OPERATOR -> 0.8f
        SymbolType.FUNCTION -> 0.7f
        SymbolType.NUMBER -> 0.6f
        SymbolType.VARIABLE -> 0.5f
        SymbolType.BRACKET -> 0.3f
        SymbolType.SUBSCRIPT -> 0.4f
        SymbolType.SUPERSCRIPT -> 0.4f
        SymbolType.FRACTION -> 0.7f
        SymbolType.SUMMATION -> 0.8f
        SymbolType.INTEGRAL -> 0.8f
        SymbolType.GREEK_LETTER -> 0.6f
        SymbolType.EQUALS -> 0.7f
        SymbolType.RELATION -> 0.7f
    }

    // ── Legacy voice descriptions ─────────────────────────────────────

    private fun describeCalculus(formula: FormulaContent): SpeechSegment {
        return SpeechSegment(
            text = "Calculus expression: ${formula.expression}. Contains ${formula.symbols.size} symbols.",
            rate = 0.9f,
            pitch = 1.0f
        )
    }

    private fun describeTrigonometric(formula: FormulaContent): SpeechSegment {
        return SpeechSegment(
            text = "Trigonometric expression: ${formula.expression}. Contains ${formula.symbols.size} symbols.",
            rate = 0.9f,
            pitch = 1.0f
        )
    }

    private fun describeAlgebraic(formula: FormulaContent): SpeechSegment {
        return SpeechSegment(
            text = "Algebraic expression: ${formula.expression}. Contains ${formula.symbols.size} symbols.",
            rate = 1.0f,
            pitch = 1.0f
        )
    }

    private fun describeMathematical(formula: FormulaContent): SpeechSegment {
        return SpeechSegment(
            text = "Mathematical expression: ${formula.expression}. Contains ${formula.symbols.size} symbols.",
            rate = 1.0f,
            pitch = 1.0f
        )
    }

    private fun describeNotation(formula: FormulaContent): SpeechSegment {
        return SpeechSegment(
            text = "Mathematical notation: ${formula.expression}. Contains ${formula.symbols.size} symbols.",
            rate = 1.0f,
            pitch = 1.0f
        )
    }

    private fun normalizePosition(value: Float): Float {
        return (value / 200f).coerceIn(0.05f, 0.95f)
    }
}
