package io.drishti.formula

/**
 * Evaluates [FormulaNode] ASTs to numeric results via [MxparserBridge].
 *
 * Converts the AST to an mXparser-compatible expression string, then evaluates.
 * Returns `null` when the expression contains unbound variables or is otherwise
 * non-evaluable.
 */
object FormulaEvaluator {

    fun evaluate(ast: FormulaNode, variables: Map<String, Double> = emptyMap()): Double? {
        val expr = toMxparserExpression(ast) ?: return null
        return try {
            val result = MxparserBridge.evaluate(expr, variables)
            if (result.isNaN()) null else result
        } catch (_: Exception) {
            null
        }
    }

    fun toMxparserExpression(ast: FormulaNode): String? {
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
            is FormulaNode.Limit -> toMxparserExpression(ast.body)
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
            is FormulaNode.Cases -> null // piecewise not natively supported by mXparser
            is FormulaNode.Matrix -> null // matrix not natively supported by mXparser
            is FormulaNode.Product -> null // product not natively supported by mXparser
        }
    }

    fun collectVariables(ast: FormulaNode): Set<String> {
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
