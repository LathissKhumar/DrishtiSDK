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

import kotlin.coroutines.cancellation.CancellationException

/**
 * Evaluates [FormulaNode] ASTs to numeric results via [MxparserBridge].
 *
 * Converts the AST to an mXparser-compatible expression string, then evaluates.
 * Returns `null` when the expression contains unbound variables or is otherwise
 * non-evaluable.
 */
public object FormulaEvaluator {

    public fun evaluate(ast: FormulaNode, variables: Map<String, Double> = emptyMap()): Double? {
        // Handle node types that need direct evaluation (not expressible as mXparser strings)
        val directResult = evaluateDirectly(ast, variables)
        if (directResult != null) return directResult

        val expr = toMxparserExpression(ast) ?: return null
        return try {
            val result = MxparserBridge.evaluate(expr, variables)
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun evaluateDirectly(ast: FormulaNode, variables: Map<String, Double>): Double? = when (ast) {
        is FormulaNode.Limit -> evaluateLimit(ast, variables)
        is FormulaNode.Cases -> evaluateCases(ast, variables)
        is FormulaNode.Matrix -> evaluateMatrix(ast, variables)
        is FormulaNode.Product -> evaluateProduct(ast, variables)
        else -> null
    }

    private fun evaluateLimit(ast: FormulaNode.Limit, variables: Map<String, Double>): Double? {
        val varName = (ast.variable as? FormulaNode.Variable)?.name ?: return null
        val targetVal = evaluate(ast.target, variables) ?: return null
        val body = ast.body

        val epsilon = 1e-7
        val varsPlus = variables + (varName to targetVal + epsilon)
        val varsMinus = variables + (varName to targetVal - epsilon)

        val fromPlus = evaluate(body, varsPlus)
        val fromMinus = evaluate(body, varsMinus)

        return when {
            fromPlus != null && fromMinus != null -> (fromPlus + fromMinus) / 2.0
            fromPlus != null -> fromPlus
            fromMinus != null -> fromMinus
            else -> null
        }
    }

    private fun evaluateCases(ast: FormulaNode.Cases, variables: Map<String, Double>): Double? {
        for ((_, value) in ast.branches) {
            val result = evaluate(value, variables)
            if (result != null) return result
        }
        return null
    }

    private fun evaluateMatrix(ast: FormulaNode.Matrix, variables: Map<String, Double>): Double? {
        if (ast.rows == 2 && ast.columns == 2) {
            val a = evaluate(ast.entries[0][0], variables) ?: return null
            val b = evaluate(ast.entries[0][1], variables) ?: return null
            val c = evaluate(ast.entries[1][0], variables) ?: return null
            val d = evaluate(ast.entries[1][1], variables) ?: return null
            return a * d - b * c
        }
        return ast.entries.firstOrNull()?.firstOrNull()?.let { evaluate(it, variables) }
    }

    private fun evaluateProduct(ast: FormulaNode.Product, variables: Map<String, Double>): Double? {
        val lower = ast.lower?.let { evaluate(it, variables) }?.toInt() ?: return null
        val upper = ast.upper?.let { evaluate(it, variables) }?.toInt() ?: return null
        val term = ast.term

        var product = 1.0
        for (i in lower..upper) {
            val vars = variables + ("i" to i.toDouble())
            val value = evaluate(term, vars) ?: return null
            product *= value
        }
        return product
    }

    public fun toMxparserExpression(ast: FormulaNode): String? {
        return when (ast) {
            is FormulaNode.Number -> ast.value
            is FormulaNode.Variable -> ast.name
            is FormulaNode.NamedSymbol -> GREEK_TO_MXPARSER[ast.name] ?: ast.name
            is FormulaNode.UnaryMinus -> {
                val inner = toMxparserExpression(ast.operand) ?: return null
                "(-$inner)"
            }
            is FormulaNode.BinaryOp -> {
                val l = toMxparserExpression(ast.left) ?: return null
                val r = toMxparserExpression(ast.right) ?: return null
                val op = BINARY_OP_MAP[ast.operator] ?: ast.operator
                "($l)$op($r)"
            }
            is FormulaNode.FunctionCall -> {
                val arg = toMxparserExpression(ast.argument) ?: return null
                "${FUNCTION_MAP[ast.name] ?: ast.name}($arg)"
            }
            is FormulaNode.Fraction -> {
                val n = toMxparserExpression(ast.numerator) ?: return null
                val d = toMxparserExpression(ast.denominator) ?: return null
                "($n)/($d)"
            }
            is FormulaNode.SquareRoot -> {
                val content = toMxparserExpression(ast.content) ?: return null
                if (ast.index != null) {
                    val idx = toMxparserExpression(ast.index) ?: return null
                    "root($idx, $content)"
                } else {
                    "sqrt($content)"
                }
            }
            is FormulaNode.Power -> {
                val b = toMxparserExpression(ast.base) ?: return null
                val e = toMxparserExpression(ast.exponent) ?: return null
                "$b^($e)"
            }
            is FormulaNode.Subscript -> {
                val base = toMxparserExpression(ast.base) ?: return null
                val idx = toMxparserExpression(ast.index) ?: return null
                "${base}_$idx"
            }
            is FormulaNode.Integral -> {
                val integrandExpr = toMxparserExpression(ast.integrand) ?: return null
                if (ast.lower != null && ast.upper != null) {
                    val lowerExpr = toMxparserExpression(ast.lower) ?: return null
                    val upperExpr = toMxparserExpression(ast.upper) ?: return null
                    val diffVar = ast.differential?.let { toMxparserExpression(it) }
                    val variable = when {
                        diffVar != null -> diffVar
                        else -> {
                            val vars = collectVariables(ast.integrand)
                            when {
                                vars.contains("x") -> "x"
                                vars.isNotEmpty() -> vars.first()
                                else -> "x"
                            }
                        }
                    }
                    "int($integrandExpr, $variable, $lowerExpr, $upperExpr)"
                } else {
                    integrandExpr
                }
            }
            is FormulaNode.Summation -> {
                val termExpr = toMxparserExpression(ast.term) ?: return null
                if (ast.lower != null && ast.upper != null) {
                    val upperExpr = toMxparserExpression(ast.upper) ?: return null
                    val variable: String
                    val lowerExpr: String
                    val lower = ast.lower
                    if (lower is FormulaNode.BinaryOp && lower.operator == "=") {
                        val left = lower.left
                        variable = if (left is FormulaNode.Variable) left.name else "i"
                        lowerExpr = toMxparserExpression(lower.right) ?: return null
                    } else {
                        val vars = collectVariables(ast.term)
                        variable = when {
                            vars.contains("i") -> "i"
                            vars.contains("n") -> "n"
                            vars.isNotEmpty() -> vars.first()
                            else -> "i"
                        }
                        lowerExpr = toMxparserExpression(lower) ?: return null
                    }
                    "sum($variable, $lowerExpr, $upperExpr, $termExpr)"
                } else {
                    termExpr
                }
            }
            is FormulaNode.Limit -> {
                val varName = (ast.variable as? FormulaNode.Variable)?.name
                val targetExpr = toMxparserExpression(ast.target)
                val bodyExpr = toMxparserExpression(ast.body)
                if (varName != null && targetExpr != null && bodyExpr != null) {
                    "limit($varName, $targetExpr, $bodyExpr)"
                } else {
                    toMxparserExpression(ast.body)
                }
            }
            is FormulaNode.Group -> {
                if (ast.children.isEmpty()) return null
                val parts = ast.children.mapNotNull { toMxparserExpression(it) }
                if (parts.size != ast.children.size) return null
                parts.joinToString("")
            }
            is FormulaNode.AbsoluteValue -> {
                val content = toMxparserExpression(ast.content) ?: return null
                "abs($content)"
            }
            is FormulaNode.Accent -> toMxparserExpression(ast.content)
            is FormulaNode.Binomial -> {
                val n = toMxparserExpression(ast.n) ?: return null
                val k = toMxparserExpression(ast.k) ?: return null
                "choose($n, $k)"
            }
            is FormulaNode.Cases -> null // handled by evaluateDirectly
            is FormulaNode.Matrix -> null // handled by evaluateDirectly
            is FormulaNode.Product -> null // handled by evaluateDirectly
        }
    }

    public fun collectVariables(ast: FormulaNode): Set<String> {
        return when (ast) {
            is FormulaNode.Number -> emptySet()
            is FormulaNode.Variable -> setOf(ast.name)
            is FormulaNode.NamedSymbol -> emptySet()
            is FormulaNode.UnaryMinus -> collectVariables(ast.operand)
            is FormulaNode.BinaryOp -> collectVariables(ast.left) + collectVariables(ast.right)
            is FormulaNode.FunctionCall -> collectVariables(ast.argument)
            is FormulaNode.Fraction -> collectVariables(ast.numerator) + collectVariables(ast.denominator)
            is FormulaNode.SquareRoot -> collectVariables(ast.content) +
                (ast.index?.let { collectVariables(it) } ?: emptySet())
            is FormulaNode.Power -> collectVariables(ast.base) + collectVariables(ast.exponent)
            is FormulaNode.Subscript -> collectVariables(ast.base) + collectVariables(ast.index)
            is FormulaNode.Integral -> (ast.lower?.let { collectVariables(it) } ?: emptySet()) +
                (ast.upper?.let { collectVariables(it) } ?: emptySet()) +
                collectVariables(ast.integrand) +
                (ast.differential?.let { collectVariables(it) } ?: emptySet())
            is FormulaNode.Summation -> (ast.lower?.let { collectVariables(it) } ?: emptySet()) +
                (ast.upper?.let { collectVariables(it) } ?: emptySet()) +
                collectVariables(ast.term)
            is FormulaNode.Limit -> collectVariables(ast.variable) +
                collectVariables(ast.target) + collectVariables(ast.body)
            is FormulaNode.Group -> ast.children.flatMapTo(mutableSetOf()) { collectVariables(it) }
            is FormulaNode.AbsoluteValue -> collectVariables(ast.content)
            is FormulaNode.Accent -> collectVariables(ast.content)
            is FormulaNode.Binomial -> collectVariables(ast.n) + collectVariables(ast.k)
            is FormulaNode.Cases -> ast.branches.flatMapTo(mutableSetOf()) {
                collectVariables(it.first) + collectVariables(it.second)
            }
            is FormulaNode.Matrix -> ast.entries.flatMapTo(mutableSetOf()) { row ->
                row.flatMapTo(mutableSetOf()) { collectVariables(it) }
            }
            is FormulaNode.Product -> (ast.lower?.let { collectVariables(it) } ?: emptySet()) +
                (ast.upper?.let { collectVariables(it) } ?: emptySet()) +
                collectVariables(ast.term)
        }
    }

    private val BINARY_OP_MAP = mapOf(
        "+" to "+",
        "-" to "-",
        "*" to "*",
        "/" to "/",
        "=" to "=",
        "<" to "<",
        ">" to ">",
        "leq" to "<=",
        "geq" to ">=",
        "neq" to "!=",
        "cdot" to "*",
        "times" to "*",
        "div" to "/",
        "pm" to "+-",
        "mp" to "-+"
    )

    private val FUNCTION_MAP = mapOf(
        "sin" to "sin",
        "cos" to "cos",
        "tan" to "tan",
        "cot" to "cot",
        "sec" to "sec",
        "csc" to "csc",
        "arcsin" to "asin",
        "arccos" to "acos",
        "arctan" to "atan",
        "sinh" to "sinh",
        "cosh" to "cosh",
        "tanh" to "tanh",
        "log" to "log10",
        "ln" to "ln",
        "exp" to "exp",
        "sqrt" to "sqrt",
        "abs" to "abs",
        "min" to "min",
        "max" to "max",
        "det" to "det",
        "dim" to "dim",
        "ker" to "ker"
    )

    private val GREEK_TO_MXPARSER = mapOf(
        "pi" to "pi",
        "e" to "euler",
        "alpha" to "alpha",
        "beta" to "beta",
        "gamma" to "gamma",
        "delta" to "delta",
        "epsilon" to "epsilon",
        "zeta" to "zeta",
        "eta" to "eta",
        "theta" to "theta",
        "iota" to "iota",
        "kappa" to "kappa",
        "lambda" to "lambda",
        "mu" to "mu",
        "nu" to "nu",
        "xi" to "xi",
        "rho" to "rho",
        "sigma" to "sigma",
        "tau" to "tau",
        "upsilon" to "upsilon",
        "phi" to "phi",
        "chi" to "chi",
        "psi" to "psi",
        "omega" to "omega",
        "infty" to "inf"
    )
}
