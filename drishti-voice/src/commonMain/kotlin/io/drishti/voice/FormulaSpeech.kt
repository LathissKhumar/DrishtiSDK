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

import io.drishti.core.FormulaContent
import io.drishti.core.FormulaType
import io.drishti.formula.FormulaParseException
import io.drishti.formula.LatexParser
import io.drishti.formula.SpeechRuleEngine

/**
 * Converts LaTeX formulas and [FormulaContent] items to natural speech
 * following MathCAT conventions for STEM accessibility.
 *
 * MathCAT verbalization patterns:
 * - Fractions: "numerator over denominator"
 * - Square roots: "square root of ..."
 * - Superscripts: "x squared", "x to the power of n"
 * - Subscripts: "x sub n", "x subscript 1"
 * - Integrals: "integral from a to b of ..."
 * - Summations: "sum from i equals 1 to n of ..."
 * - Greek letters: spoken by name (alpha, beta, etc.)
 *
 * Usage:
 * ```
 * val speech = FormulaSpeech.fromLatex("\\frac{a}{b}")
 * // "a over b"
 *
 * val speech = FormulaSpeech.fromContent(formulaContent)
 * // "This is an algebraic formula: x plus y equals z"
 * ```
 */
public object FormulaSpeech {

    /**
     * Convert a LaTeX string to MathCAT-style speech text.
     *
     * @param latex LaTeX math expression
     * @return Natural language speech description
     */
    public fun fromLatex(latex: String): String {
        return try {
            val ast = LatexParser.parse(latex)
            SpeechRuleEngine.toSpeech(ast)
        } catch (_: FormulaParseException) {
            latex
        }
    }

    /**
     * Convert a [FormulaContent] to a complete speech description.
     *
     * Produces a natural-language intro followed by the MathCAT verbalization
     * of the expression.
     *
     * @param content Formula content from the vision pipeline
     * @return Complete speech text for TTS playback
     */
    public fun fromContent(content: FormulaContent): String {
        val typeLabel = describeFormulaType(content.formulaType)
        val expressionSpeech = fromLatex(content.expression)
        return "This is $typeLabel formula: $expressionSpeech"
    }

    /**
     * Convert a [FormulaContent] to just the expression speech (no intro).
     *
     * Useful when the caller wants to add their own context.
     *
     * @param content Formula content from the vision pipeline
     * @return Speech text for just the mathematical expression
     */
    public fun expressionOnly(content: FormulaContent): String {
        return fromLatex(content.expression)
    }

    /**
     * Convert a list of formula symbols to a sequential speech description.
     *
     * Reads symbols in left-to-right, top-to-bottom order (reading order),
     * grouping consecutive same-type symbols for natural phrasing.
     *
     * @param symbols Formula symbols sorted by position
     * @return Sequential speech text
     */
    public fun fromSymbols(symbols: List<io.drishti.core.FormulaSymbol>): String {
        if (symbols.isEmpty()) return "empty expression"
        return symbols.joinToString(" ") { symbol ->
            symbolToSpeech(symbol.type.name, symbol.value)
        }
    }

    /**
     * Describe a formula type in natural language for speech output.
     */
    private fun describeFormulaType(type: FormulaType): String = when (type) {
        FormulaType.ALGEBRAIC -> "an algebraic"
        FormulaType.TRIGONOMETRIC -> "a trigonometric"
        FormulaType.CALCULUS -> "a calculus"
        FormulaType.MATHEMATICAL -> "a mathematical"
        FormulaType.NOTATION -> "a notation"
    }

    /**
     * Convert a symbol type + value to speech text.
     */
    private fun symbolToSpeech(typeName: String, value: String): String = when (typeName) {
        "NUMBER" -> value
        "VARIABLE" -> value
        "OPERATOR" -> operatorToSpeech(value)
        "FUNCTION" -> "$value of"
        "BRACKET" -> ""
        "SUBSCRIPT" -> "subscript $value"
        "SUPERSCRIPT" -> "superscript $value"
        "FRACTION" -> "fraction"
        "SUMMATION" -> "sum"
        "INTEGRAL" -> "integral"
        "GREEK_LETTER" -> greekLetterToSpeech(value)
        "EQUALS" -> "equals"
        "RELATION" -> operatorToSpeech(value)
        else -> value
    }

    private fun operatorToSpeech(op: String): String = when (op) {
        "+" -> "plus"
        "-" -> "minus"
        "*" -> "times"
        "/" -> "over"
        "=" -> "equals"
        "<" -> "less than"
        ">" -> "greater than"
        "leq" -> "less than or equal to"
        "geq" -> "greater than or equal to"
        "neq" -> "not equal to"
        else -> op
    }

    private fun greekLetterToSpeech(name: String): String = when (name) {
        "alpha" -> "alpha"
        "beta" -> "beta"
        "gamma" -> "gamma"
        "delta" -> "delta"
        "epsilon" -> "epsilon"
        "theta" -> "theta"
        "lambda" -> "lambda"
        "mu" -> "mu"
        "sigma" -> "sigma"
        "phi" -> "phi"
        "omega" -> "omega"
        "psi" -> "psi"
        "pi" -> "pi"
        "tau" -> "tau"
        "rho" -> "rho"
        "xi" -> "xi"
        "Gamma" -> "capital gamma"
        "Delta" -> "capital delta"
        "Theta" -> "capital theta"
        "Lambda" -> "capital lambda"
        "Pi" -> "capital pi"
        "Sigma" -> "capital sigma"
        "Phi" -> "capital phi"
        "Omega" -> "capital omega"
        else -> name
    }
}
