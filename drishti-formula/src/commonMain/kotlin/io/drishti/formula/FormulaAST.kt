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

import io.drishti.core.SymbolType

/**
 * Abstract Syntax Tree node for parsed LaTeX formulas.
 *
 * Each node represents a mathematical construct extracted from LaTeX input.
 * The tree structure enables traversal for haptic mapping, audio density calculation,
 * and accessible speech generation.
 */
public sealed class FormulaNode {

    /** Numeric literal: `42`, `3.14`. */
    public data class Number(val value: String) : FormulaNode()

    /** Single-letter variable: `x`, `y`, `t`. */
    public data class Variable(val name: String) : FormulaNode()

    /** Named symbol: Greek letters, operators, constants. */
    public data class NamedSymbol(val name: String, val symbolType: SymbolType) : FormulaNode()

    /** Binary operation: `+`, `-`, `*`, `/`, `=`, `<`, `>`. */
    public data class BinaryOp(
        val operator: String,
        val left: FormulaNode,
        val right: FormulaNode
    ) : FormulaNode()

    /** Unary minus: `-x`. */
    public data class UnaryMinus(val operand: FormulaNode) : FormulaNode()

    /** Function call: `\sin(x)`, `\cos(\theta)`. */
    public data class FunctionCall(val name: String, val argument: FormulaNode) : FormulaNode()

    /** Fraction: `\frac{numerator}{denominator}`. */
    public data class Fraction(val numerator: FormulaNode, val denominator: FormulaNode) : FormulaNode()

    /** Square root: `\sqrt{x}`, `\sqrt[n]{x}`. */
    public data class SquareRoot(val content: FormulaNode, val index: FormulaNode? = null) : FormulaNode()

    /** Power / superscript: `x^{exponent}`. */
    public data class Power(val base: FormulaNode, val exponent: FormulaNode) : FormulaNode()

    /** Subscript: `x_{index}`. */
    public data class Subscript(val base: FormulaNode, val index: FormulaNode) : FormulaNode()

    /** Definite integral: `\int_{lower}^{upper} integrand d[var]`. */
    public data class Integral(
        val lower: FormulaNode?,
        val upper: FormulaNode?,
        val integrand: FormulaNode,
        val differential: FormulaNode? = null
    ) : FormulaNode()

    /** Summation: `\sum_{lower}^{upper} term`. */
    public data class Summation(
        val lower: FormulaNode?,
        val upper: FormulaNode?,
        val term: FormulaNode
    ) : FormulaNode()

    /** Limit: `\lim_{variable \to target} body`. */
    public data class Limit(
        val variable: FormulaNode,
        val target: FormulaNode,
        val body: FormulaNode
    ) : FormulaNode()

    /** Parenthesised / braced group of expressions. */
    public data class Group(val children: List<FormulaNode>) : FormulaNode()

    /** Absolute value: `|x|`. */
    public data class AbsoluteValue(val content: FormulaNode) : FormulaNode()

    /** Matrix: `\begin{matrix} ... \end{matrix}`. */
    public data class Matrix(
        val rows: Int,
        val columns: Int,
        val entries: List<List<FormulaNode>>
    ) : FormulaNode()

    /** Binomial coefficient: `\binom{n}{k}`. */
    public data class Binomial(
        val n: FormulaNode,
        val k: FormulaNode
    ) : FormulaNode()

    /** Product: `\prod_{lower}^{upper} term`. */
    public data class Product(
        val lower: FormulaNode? = null,
        val upper: FormulaNode? = null,
        val term: FormulaNode
    ) : FormulaNode()

    /** Accent decoration: `\hat{x}`, `\tilde{x}`, `\bar{x}`, etc. */
    public data class Accent(
        val type: AccentType,
        val content: FormulaNode
    ) : FormulaNode()

    /** Types of accent decorations. */
    public enum class AccentType { HAT, TILDE, BAR, DOT, DOTDOT, VEC, OVERLINE, UNDERLINE }

    /** Cases / piecewise: `\begin{cases} ... \end{cases}`. */
    public data class Cases(
        val branches: List<Pair<FormulaNode, FormulaNode>>
    ) : FormulaNode()
}

/**
 * Depth of the AST (longest root-to-leaf path).
 */
public fun FormulaNode.depth(): Int = when (this) {
    is FormulaNode.Number -> 1
    is FormulaNode.Variable -> 1
    is FormulaNode.NamedSymbol -> 1
    is FormulaNode.UnaryMinus -> 1 + operand.depth()
    is FormulaNode.BinaryOp -> 1 + maxOf(left.depth(), right.depth())
    is FormulaNode.FunctionCall -> 1 + argument.depth()
    is FormulaNode.Fraction -> 1 + maxOf(numerator.depth(), denominator.depth())
    is FormulaNode.SquareRoot -> 1 + maxOf(content.depth(), index?.depth() ?: 0)
    is FormulaNode.Power -> 1 + maxOf(base.depth(), exponent.depth())
    is FormulaNode.Subscript -> 1 + maxOf(base.depth(), index.depth())
    is FormulaNode.Integral -> 1 + maxOf(
        lower?.depth() ?: 0,
        upper?.depth() ?: 0,
        integrand.depth(),
        differential?.depth() ?: 0
    )
    is FormulaNode.Summation -> 1 + maxOf(
        lower?.depth() ?: 0,
        upper?.depth() ?: 0,
        term.depth()
    )
    is FormulaNode.Limit -> 1 + maxOf(variable.depth(), target.depth(), body.depth())
    is FormulaNode.Group -> 1 + (children.maxOfOrNull { it.depth() } ?: 0)
    is FormulaNode.AbsoluteValue -> 1 + content.depth()
    is FormulaNode.Matrix -> 1 + (entries.maxOfOrNull { row -> row.maxOf { it.depth() } } ?: 0)
    is FormulaNode.Binomial -> 1 + maxOf(n.depth(), k.depth())
    is FormulaNode.Product -> 1 + maxOf(lower?.depth() ?: 0, upper?.depth() ?: 0, term.depth())
    is FormulaNode.Accent -> 1 + content.depth()
    is FormulaNode.Cases -> 1 + (branches.maxOfOrNull { maxOf(it.first.depth(), it.second.depth()) } ?: 0)
}

/**
 * Count all leaf (terminal) nodes.
 */
public fun FormulaNode.leafCount(): Int = when (this) {
    is FormulaNode.Number -> 1
    is FormulaNode.Variable -> 1
    is FormulaNode.NamedSymbol -> 1
    is FormulaNode.UnaryMinus -> operand.leafCount()
    is FormulaNode.BinaryOp -> left.leafCount() + right.leafCount()
    is FormulaNode.FunctionCall -> argument.leafCount()
    is FormulaNode.Fraction -> numerator.leafCount() + denominator.leafCount()
    is FormulaNode.SquareRoot -> content.leafCount() + (index?.leafCount() ?: 0)
    is FormulaNode.Power -> base.leafCount() + exponent.leafCount()
    is FormulaNode.Subscript -> base.leafCount() + index.leafCount()
    is FormulaNode.Integral -> (lower?.leafCount() ?: 0) +
        (upper?.leafCount() ?: 0) + integrand.leafCount() + (differential?.leafCount() ?: 0)
    is FormulaNode.Summation -> (lower?.leafCount() ?: 0) +
        (upper?.leafCount() ?: 0) + term.leafCount()
    is FormulaNode.Limit -> variable.leafCount() + target.leafCount() + body.leafCount()
    is FormulaNode.Group -> children.sumOf { it.leafCount() }
    is FormulaNode.AbsoluteValue -> content.leafCount()
    is FormulaNode.Matrix -> entries.sumOf { row -> row.sumOf { it.leafCount() } }
    is FormulaNode.Binomial -> n.leafCount() + k.leafCount()
    is FormulaNode.Product -> (lower?.leafCount() ?: 0) + (upper?.leafCount() ?: 0) + term.leafCount()
    is FormulaNode.Accent -> content.leafCount()
    is FormulaNode.Cases -> branches.sumOf { it.first.leafCount() + it.second.leafCount() }
}

/**
 * Visit all nodes in the AST recursively, passing coordinates.
 */
public fun FormulaNode.visit(x: Float, y: Float, action: (node: FormulaNode, x: Float, y: Float) -> Unit) {
    action(this, x, y)
    when (this) {
        is FormulaNode.BinaryOp -> {
            left.visit(x - 0.1f, y, action)
            right.visit(x + 0.1f, y, action)
        }
        is FormulaNode.Fraction -> {
            numerator.visit(x, y - 0.1f, action)
            denominator.visit(x, y + 0.1f, action)
        }
        is FormulaNode.FunctionCall -> {
            argument.visit(x + 0.15f, y, action)
        }
        is FormulaNode.SquareRoot -> {
            content.visit(x + 0.1f, y, action)
        }
        is FormulaNode.Power -> {
            base.visit(x, y, action)
            exponent.visit(x + 0.05f, y - 0.1f, action)
        }
        is FormulaNode.Integral -> {
            lower?.visit(x, y + 0.15f, action)
            upper?.visit(x, y - 0.15f, action)
            integrand.visit(x + 0.15f, y, action)
            differential?.visit(x + 0.3f, y, action)
        }
        is FormulaNode.Summation -> {
            lower?.visit(x, y + 0.15f, action)
            upper?.visit(x, y - 0.15f, action)
            term.visit(x + 0.15f, y, action)
        }
        is FormulaNode.Limit -> {
            body.visit(x + 0.15f, y, action)
        }
        is FormulaNode.Group -> {
            children.forEachIndexed { i, child ->
                child.visit(x + i * 0.1f, y, action)
            }
        }
        is FormulaNode.UnaryMinus -> {
            operand.visit(x + 0.05f, y, action)
        }
        is FormulaNode.Subscript -> {
            base.visit(x, y, action)
            index.visit(x + 0.05f, y + 0.1f, action)
        }
        is FormulaNode.AbsoluteValue -> {
            content.visit(x, y, action)
        }
        is FormulaNode.Accent -> {
            content.visit(x, y, action)
        }
        is FormulaNode.Binomial -> {
            n.visit(x, y - 0.1f, action)
            k.visit(x, y + 0.1f, action)
        }
        is FormulaNode.Cases -> {
            branches.forEachIndexed { i, (condition, value) ->
                condition.visit(x, y + i * 0.1f, action)
                value.visit(x + 0.15f, y + i * 0.1f, action)
            }
        }
        is FormulaNode.Matrix -> {
            entries.forEachIndexed { rowIdx, row ->
                row.forEachIndexed { colIdx, entry ->
                    entry.visit(x + colIdx * 0.1f, y + rowIdx * 0.1f, action)
                }
            }
        }
        is FormulaNode.Product -> {
            lower?.visit(x, y + 0.15f, action)
            upper?.visit(x, y - 0.15f, action)
            term.visit(x + 0.15f, y, action)
        }
        is FormulaNode.Number, is FormulaNode.Variable, is FormulaNode.NamedSymbol -> {}
    }
}
