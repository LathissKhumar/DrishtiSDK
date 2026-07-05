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
import io.drishti.core.FormulaContent
import io.drishti.core.FormulaContentItem
import io.drishti.core.FormulaSymbol
import io.drishti.core.FormulaType
import io.drishti.core.Geometry
import io.drishti.core.SymbolType
import kotlin.coroutines.cancellation.CancellationException

/**
 * Enriched formula representation produced by [FormulaDetector].
 *
 * Wraps the core [FormulaContent] with LaTeX-specific data: parsed AST,
 * optional numeric evaluation, and accessible speech text. This is the
 * primary data type that flows through the rendering pipeline.
 *
 * @property latex Original LaTeX input string
 * @property ast Parsed abstract syntax tree
 * @property evaluationResult Numeric evaluation (null if expression has unbound variables)
 * @property speechText Harvard-sentence-style accessible description
 * @property formulaContent Underlying [FormulaContent] for backward compatibility
 */
public data class ParsedFormula(
    val latex: String,
    val ast: FormulaNode,
    val evaluationResult: Double? = null,
    val speechText: String = "",
    val formulaContent: FormulaContent
) : FormulaContentItem {

    override val contentType: ContentType = ContentType.FORMULA

    override val confidence: Float = 0.95f

    /** Convenience accessor for formula type. */
    override val formulaType: FormulaType get() = formulaContent.formulaType

    /** Convenience accessor for expression string. */
    override val expression: String get() = formulaContent.expression

    /** Convenience accessor for symbols. */
    override val symbols: List<FormulaSymbol> get() = formulaContent.symbols

    /** Convenience accessor for geometry. */
    override val geometry: Geometry? get() = formulaContent.geometry

    /** True if the expression has been successfully evaluated. */
    val isEvaluable: Boolean get() = evaluationResult != null

    /** True if the formula has a speech description. */
    val isSpeechAvailable: Boolean get() = speechText.isNotEmpty()

    public companion object {

        /**
         * Create a [ParsedFormula] from a LaTeX string.
         *
         * Parses the LaTeX, evaluates if possible, and generates speech text.
         */
        public fun fromLatex(
            latex: String,
            formulaType: FormulaType = FormulaType.MATHEMATICAL,
            variables: Map<String, Double> = emptyMap()
        ): ParsedFormula {
            val ast = LatexParser.parse(latex)
            val evaluationResult = FormulaEvaluator.evaluate(ast, variables)
            val speechText = SpeechRuleEngine.toSpeech(ast)
            val symbols = extractSymbolsFromAst(ast)
            val formulaContent = FormulaContent(
                formulaType = formulaType,
                expression = latex,
                symbols = symbols,
                confidence = 1.0f
            )
            return ParsedFormula(
                latex = latex,
                ast = ast,
                evaluationResult = evaluationResult,
                speechText = speechText,
                formulaContent = formulaContent
            )
        }

        /**
         * Create a [ParsedFormula] from a [FormulaContent] (backward compatibility).
         */
        public fun fromFormulaContent(formulaContent: FormulaContent): ParsedFormula {
            val latex = formulaContent.expression
            val ast = try {
                LatexParser.parse(latex)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                FormulaNode.Group(
                    formulaContent.symbols.map {
                        FormulaNode.Variable(it.value)
                    }.ifEmpty { listOf(FormulaNode.Number("0")) }
                )
            }
            val evaluationResult = FormulaEvaluator.evaluate(ast)
            val speechText = SpeechRuleEngine.toSpeech(ast)
            return ParsedFormula(
                latex = latex,
                ast = ast,
                evaluationResult = evaluationResult,
                speechText = speechText,
                formulaContent = formulaContent
            )
        }

        private fun extractSymbolsFromAst(ast: FormulaNode): List<FormulaSymbol> {
            val symbols = mutableListOf<FormulaSymbol>()
            extractSymbolsRecursive(ast, symbols, 0f, 0f)
            return symbols
        }

        /**
         * Recursively extracts [FormulaSymbol]s from an AST node, advancing
         * the horizontal offset as each symbol is placed.
         *
         * Returns the x-position immediately after the last symbol placed by
         * this subtree so that callers can chain siblings horizontally
         * without overlapping.
         */
        private fun extractSymbolsRecursive(
            node: FormulaNode,
            acc: MutableList<FormulaSymbol>,
            x: Float,
            y: Float
        ): Float {
            var offsetX = x
            when (node) {
                is FormulaNode.Number -> {
                    acc.add(FormulaSymbol(SymbolType.NUMBER, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, node.value.length * 8f, 16f), node.value))
                    offsetX += node.value.length * 8f
                }
                is FormulaNode.Variable -> {
                    acc.add(FormulaSymbol(SymbolType.VARIABLE, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 10f, 16f), node.name))
                    offsetX += 10f
                }
                is FormulaNode.NamedSymbol -> {
                    val type = node.symbolType
                    acc.add(FormulaSymbol(type, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 12f, 16f), node.name))
                    offsetX += 12f
                }
                is FormulaNode.BinaryOp -> {
                    val afterLeft = extractSymbolsRecursive(node.left, acc, offsetX, y)
                    acc.add(FormulaSymbol(SymbolType.OPERATOR, io.drishti.core.Point(afterLeft, y), io.drishti.core.BoundingBox(afterLeft, y, 8f, 16f), node.operator))
                    val afterOp = afterLeft + 8f
                    offsetX = extractSymbolsRecursive(node.right, acc, afterOp, y)
                }
                is FormulaNode.Fraction -> {
                    acc.add(FormulaSymbol(SymbolType.FRACTION, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 20f, 24f), "/"))
                    extractSymbolsRecursive(node.numerator, acc, offsetX, y - 12f)
                    extractSymbolsRecursive(node.denominator, acc, offsetX, y + 12f)
                    offsetX += 20f
                }
                is FormulaNode.FunctionCall -> {
                    acc.add(FormulaSymbol(SymbolType.FUNCTION, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, node.name.length * 8f, 16f), node.name))
                    offsetX += node.name.length * 8f
                    offsetX = extractSymbolsRecursive(node.argument, acc, offsetX, y)
                }
                is FormulaNode.Integral -> {
                    acc.add(FormulaSymbol(SymbolType.INTEGRAL, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 12f, 24f), "integral"))
                    if (node.lower != null) extractSymbolsRecursive(node.lower, acc, offsetX, y + 16f)
                    if (node.upper != null) extractSymbolsRecursive(node.upper, acc, offsetX, y - 16f)
                    val afterIntegrand = extractSymbolsRecursive(node.integrand, acc, offsetX + 16f, y)
                    offsetX = afterIntegrand
                    if (node.differential != null) {
                        offsetX = extractSymbolsRecursive(node.differential, acc, afterIntegrand, y)
                    }
                }
                is FormulaNode.Summation -> {
                    acc.add(FormulaSymbol(SymbolType.SUMMATION, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 12f, 24f), "sum"))
                    if (node.lower != null) extractSymbolsRecursive(node.lower, acc, offsetX, y + 16f)
                    if (node.upper != null) extractSymbolsRecursive(node.upper, acc, offsetX, y - 16f)
                    offsetX = extractSymbolsRecursive(node.term, acc, offsetX + 16f, y)
                }
                is FormulaNode.UnaryMinus -> {
                    acc.add(FormulaSymbol(SymbolType.OPERATOR, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 8f, 16f), "-"))
                    offsetX = extractSymbolsRecursive(node.operand, acc, offsetX + 8f, y)
                }
                is FormulaNode.Power -> {
                    val afterBase = extractSymbolsRecursive(node.base, acc, offsetX, y)
                    acc.add(FormulaSymbol(SymbolType.SUPERSCRIPT, io.drishti.core.Point(afterBase, y - 8f), io.drishti.core.BoundingBox(afterBase, y - 8f, 6f, 10f), "^"))
                    extractSymbolsRecursive(node.exponent, acc, afterBase, y - 8f)
                    offsetX = afterBase
                }
                is FormulaNode.Subscript -> {
                    val afterBase = extractSymbolsRecursive(node.base, acc, offsetX, y)
                    acc.add(FormulaSymbol(SymbolType.SUBSCRIPT, io.drishti.core.Point(afterBase, y + 8f), io.drishti.core.BoundingBox(afterBase, y + 8f, 6f, 10f), "_"))
                    extractSymbolsRecursive(node.index, acc, afterBase, y + 8f)
                    offsetX = afterBase
                }
                is FormulaNode.SquareRoot -> {
                    acc.add(FormulaSymbol(SymbolType.FUNCTION, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 12f, 16f), "sqrt"))
                    offsetX = extractSymbolsRecursive(node.content, acc, offsetX + 16f, y)
                }
                is FormulaNode.Limit -> {
                    acc.add(FormulaSymbol(SymbolType.FUNCTION, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 20f, 16f), "lim"))
                    offsetX = extractSymbolsRecursive(node.body, acc, offsetX + 24f, y)
                }
                is FormulaNode.Group -> {
                    var currentX = offsetX
                    for (child in node.children) {
                        currentX = extractSymbolsRecursive(child, acc, currentX, y)
                    }
                    offsetX = currentX
                }
                is FormulaNode.AbsoluteValue -> {
                    acc.add(FormulaSymbol(SymbolType.BRACKET, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 4f, 16f), "|"))
                    extractSymbolsRecursive(node.content, acc, offsetX + 4f, y)
                    acc.add(FormulaSymbol(SymbolType.BRACKET, io.drishti.core.Point(offsetX + 20f, y), io.drishti.core.BoundingBox(offsetX + 20f, y, 4f, 16f), "|"))
                    offsetX += 24f
                }
                is FormulaNode.Accent -> {
                    offsetX = extractSymbolsRecursive(node.content, acc, offsetX, y)
                }
                is FormulaNode.Binomial -> {
                    acc.add(FormulaSymbol(SymbolType.FUNCTION, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 20f, 24f), "binom"))
                    extractSymbolsRecursive(node.n, acc, offsetX, y - 8f)
                    extractSymbolsRecursive(node.k, acc, offsetX, y + 8f)
                    offsetX += 20f
                }
                is FormulaNode.Cases -> {
                    acc.add(FormulaSymbol(SymbolType.BRACKET, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 4f, 16f), "{"))
                    node.branches.forEachIndexed { i, (condition, value) ->
                        extractSymbolsRecursive(condition, acc, offsetX + 4f, y + i * 16f)
                        acc.add(FormulaSymbol(SymbolType.RELATION, io.drishti.core.Point(offsetX + 40f, y + i * 16f), io.drishti.core.BoundingBox(offsetX + 40f, y + i * 16f, 8f, 16f), ","))
                        extractSymbolsRecursive(value, acc, offsetX + 52f, y + i * 16f)
                    }
                    offsetX += 52f
                }
                is FormulaNode.Matrix -> {
                    acc.add(FormulaSymbol(SymbolType.BRACKET, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 4f, 16f), "["))
                    node.entries.forEachIndexed { rowIdx, row ->
                        row.forEachIndexed { colIdx, entry ->
                            extractSymbolsRecursive(entry, acc, offsetX + 4f + colIdx * 20f, y + rowIdx * 16f)
                        }
                    }
                    val closingX = offsetX + 4f + node.columns * 20f
                    acc.add(FormulaSymbol(SymbolType.BRACKET, io.drishti.core.Point(closingX, y), io.drishti.core.BoundingBox(closingX, y, 4f, 16f), "]"))
                    offsetX = closingX + 4f
                }
                is FormulaNode.Product -> {
                    acc.add(FormulaSymbol(SymbolType.SUMMATION, io.drishti.core.Point(offsetX, y), io.drishti.core.BoundingBox(offsetX, y, 12f, 24f), "prod"))
                    if (node.lower != null) extractSymbolsRecursive(node.lower, acc, offsetX, y + 16f)
                    if (node.upper != null) extractSymbolsRecursive(node.upper, acc, offsetX, y - 16f)
                    offsetX = extractSymbolsRecursive(node.term, acc, offsetX + 16f, y)
                }
            }
            return offsetX
        }
    }
}
