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

import io.drishti.core.ContentItem
import io.drishti.core.ContentType
import io.drishti.core.DetectorPlugin
import io.drishti.core.FormulaType
import io.drishti.core.Frame
import kotlin.coroutines.cancellation.CancellationException

/**
 * Detects mathematical formula content using LaTeX parsing.
 *
 * Primary entry point: [detectFromLatex] for direct LaTeX string input.
 * Secondary entry point: [detect] for camera frame input (attempts to identify
 * LaTeX patterns from OCR text).
 * Also supports Unicode math symbols via [detectFromUnicode].
 *
 * This detector does NOT depend on the vision module. All detection
 * is performed through text analysis and LaTeX parsing.
 */
public class FormulaDetector : DetectorPlugin {

    override val contentType: ContentType = ContentType.Formula

    override val confidence: Float = 0.95f

    /**
     * Detect formula content from a camera frame.
     *
     * This is a fallback path. Prefer [detectFromLatex] when the LaTeX
     * string is already known.
     *
     * @param frame The input image frame
     * @return Detected formula, or null if no formula detected
     */
    override suspend fun detect(frame: Frame): ContentItem? {
        val text = try {
            frame.data?.decodeToString()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return null
        if (text.isBlank()) return null
        return detectFromOcrText(text)
    }

    /**
     * Detect formula content from a LaTeX string.
     *
     * This is the primary entry point. Parses the LaTeX into an AST,
     * evaluates if possible, and generates accessible speech text.
     *
     * @param latex LaTeX math expression (e.g. `\frac{1}{2}`, `\int_{0}^{1} x\,dx`)
     * @param formulaType Optional formula type hint
     * @return Parsed formula with AST, evaluation, and speech text
     * @throws IllegalArgumentException if the LaTeX cannot be parsed
     */
    public fun detectFromLatex(
        latex: String,
        formulaType: FormulaType? = null
    ): ParsedFormula {
        val type = formulaType ?: classifyFormulaType(latex)
        return ParsedFormula.fromLatex(latex, type)
    }

    /**
     * Detect formula content from a Unicode math string.
     *
     * Converts Unicode math symbols to LaTeX, then parses.
     * Supported Unicode symbols: ∫, ∑, √, π, θ, α, β, γ, ∞, ±, ×, ÷, ≤, ≥, ∂, ∇
     *
     * @param text Text containing Unicode math symbols
     * @return Parsed formula, or null if no formula detected
     */
    public fun detectFromUnicode(text: String): ParsedFormula? {
        val latex = unicodeToLatex(text)
        if (latex.isBlank()) return null
        return try {
            detectFromLatex(latex)
        } catch (e: FormulaParseException) {
            null
        }
    }

    /**
     * Detect formula content from OCR-extracted text.
     *
     * Attempts to identify mathematical patterns in the text and convert
     * them to LaTeX for parsing.
     *
     * @param ocrText Text extracted by OCR
     * @return Parsed formula, or null if no formula detected
     */
    public fun detectFromOcrText(ocrText: String): ParsedFormula? {
        // Try Unicode conversion first
        val fromUnicode = detectFromUnicode(ocrText)
        if (fromUnicode != null) return fromUnicode

        // Try direct LaTeX parsing (OCR might have captured LaTeX-like syntax)
        return try {
            detectFromLatex(ocrText)
        } catch (e: FormulaParseException) {
            null
        }
    }

    /**
     * Classify the formula type based on LaTeX content heuristics.
     */
    private fun classifyFormulaType(latex: String): FormulaType {
        val lower = latex.lowercase()
        return when {
            CALCULUS_PATTERN.containsMatchIn(lower) -> FormulaType.CALCULUS
            TRIG_PATTERN.containsMatchIn(lower) -> FormulaType.TRIGONOMETRIC
            ALGEBRA_PATTERN.containsMatchIn(lower) -> FormulaType.ALGEBRAIC
            NOTATION_PATTERN.containsMatchIn(lower) -> FormulaType.NOTATION
            else -> FormulaType.MATHEMATICAL
        }
    }

    /**
     * Convert Unicode math symbols to LaTeX commands.
     */
    internal fun unicodeToLatex(text: String): String {
        var result = text
        for ((unicode, latex) in UNICODE_TO_LATEX) {
            result = result.replace(unicode, latex)
        }
        return result.trim()
    }

    public companion object {
        private val CALCULUS_PATTERN = Regex(
            "\\\\int|\\\\sum|\\\\prod|\\\\lim|\\\\partial|\\\\nabla|∫|∑|∏|∂|∇"
        )
        private val TRIG_PATTERN = Regex(
            "\\\\sin|\\\\cos|\\\\tan|\\\\cot|\\\\sec|\\\\csc|sin|cos|tan|θ|φ"
        )
        private val ALGEBRA_PATTERN = Regex(
            "\\\\frac|\\\\sqrt|\\\\cdot|\\\\times|\\\\div|=|\\+|-|\\^|_|√"
        )
        private val NOTATION_PATTERN = Regex(
            "\\\\alpha|\\\\beta|\\\\gamma|\\\\delta|\\\\pi|\\\\sigma|\\\\omega|" +
                "α|β|γ|δ|π|σ|ω|∞"
        )

        internal val UNICODE_TO_LATEX = linkedMapOf(
            // Operators
            "×" to "\\times",
            "÷" to "\\div",
            "±" to "\\pm",
            "∓" to "\\mp",
            "·" to "\\cdot",
            "≤" to "\\leq",
            "≥" to "\\geq",
            "≠" to "\\neq",
            "≈" to "\\approx",
            "≡" to "\\equiv",
            "∈" to "\\in",
            "∉" to "\\notin",
            "⊂" to "\\subset",
            "⊃" to "\\supset",
            "∪" to "\\cup",
            "∩" to "\\cap",
            "∅" to "\\emptyset",
            "∀" to "\\forall",
            "∃" to "\\exists",
            "¬" to "\\neg",
            "∧" to "\\land",
            "∨" to "\\lor",
            "→" to "\\rightarrow",
            "←" to "\\leftarrow",
            "⇒" to "\\Rightarrow",
            "⇐" to "\\Leftarrow",
            "⇔" to "\\Leftrightarrow",
            "↦" to "\\mapsto",
            "ℝ" to "\\mathbb{R}",
            "ℂ" to "\\mathbb{C}",
            "ℕ" to "\\mathbb{N}",
            "ℤ" to "\\mathbb{Z}",
            "ℚ" to "\\mathbb{Q}",
            "=" to "=",
            "<" to "<",
            ">" to ">",
            // Structures
            "√" to "\\sqrt",
            "∫" to "\\int",
            "∑" to "\\sum",
            "∏" to "\\prod",
            "∂" to "\\partial",
            "∇" to "\\nabla",
            "∞" to "\\infty",
            // Greek lowercase
            "α" to "\\alpha",
            "β" to "\\beta",
            "γ" to "\\gamma",
            "δ" to "\\delta",
            "ε" to "\\epsilon",
            "ζ" to "\\zeta",
            "η" to "\\eta",
            "θ" to "\\theta",
            "ι" to "\\iota",
            "κ" to "\\kappa",
            "λ" to "\\lambda",
            "μ" to "\\mu",
            "ν" to "\\nu",
            "ξ" to "\\xi",
            "π" to "\\pi",
            "ρ" to "\\rho",
            "σ" to "\\sigma",
            "τ" to "\\tau",
            "υ" to "\\upsilon",
            "φ" to "\\phi",
            "χ" to "\\chi",
            "ψ" to "\\psi",
            "ω" to "\\omega",
            // Greek uppercase
            "Γ" to "\\Gamma",
            "Δ" to "\\Delta",
            "Θ" to "\\Theta",
            "Λ" to "\\Lambda",
            "Ξ" to "\\Xi",
            "Π" to "\\Pi",
            "Σ" to "\\Sigma",
            "Υ" to "\\Upsilon",
            "Φ" to "\\Phi",
            "Ψ" to "\\Psi",
            "Ω" to "\\Omega"
        )
    }
}
