package io.drishti.formula

/**
 * Converts [FormulaNode] ASTs into accessible speech text following
 * Harvard sentence structure for STEM accessibility.
 *
 * Follows the [DIAGRAM Center](https://diagramcenter.org/) guidelines for
 * mathematical speech: structural description first, then reading order.
 *
 * Usage:
 * ```
 * val ast = LatexParser.parse("\\frac{a}{b}")
 * val speech = SpeechRuleEngine.toSpeech(ast) // "a over b"
 * ```
 */
object SpeechRuleEngine {

    /**
     * Convert a LaTeX string directly to speech text.
     *
     * @param latex LaTeX math expression
     * @return Accessible speech description
     */
    fun speechFromLatex(latex: String): String {
        val ast = try {
            LatexParser.parse(latex)
        } catch (_: Exception) {
            return latex
        }
        return toSpeech(ast)
    }

    /**
     * Convert an AST node to accessible speech text.
     *
     * @param node Root AST node
     * @return Harvard-sentence-style speech description
     */
    fun toSpeech(node: FormulaNode): String = when (node) {
        is FormulaNode.Number -> node.value
        is FormulaNode.Variable -> node.name
        is FormulaNode.NamedSymbol -> describeSymbol(node)
        is FormulaNode.UnaryMinus -> "negative ${toSpeech(node.operand)}"
        is FormulaNode.BinaryOp -> describeBinaryOp(node)
        is FormulaNode.FunctionCall -> describeFunction(node)
        is FormulaNode.Fraction -> describeFraction(node)
        is FormulaNode.SquareRoot -> describeSqrt(node)
        is FormulaNode.Power -> describePower(node)
        is FormulaNode.Subscript -> describeSubscript(node)
        is FormulaNode.Integral -> describeIntegral(node)
        is FormulaNode.Summation -> describeSummation(node)
        is FormulaNode.Limit -> describeLimit(node)
        is FormulaNode.Group -> node.children.joinToString(" ") { toSpeech(it) }
        is FormulaNode.AbsoluteValue -> "absolute value of ${toSpeech(node.content)}"
        is FormulaNode.Matrix -> describeMatrix(node)
        is FormulaNode.Binomial -> "${toSpeech(node.n)} choose ${toSpeech(node.k)}"
        is FormulaNode.Product -> describeProduct(node)
        is FormulaNode.Accent -> describeAccent(node)
        is FormulaNode.Cases -> describeCases(node)
    }

    private fun describeSymbol(node: FormulaNode.NamedSymbol): String = when (node.name) {
        "pi" -> "pi"
        "infty" -> "infinity"
        "partial" -> "partial"
        "nabla" -> "nabla"
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
        "chi" -> "chi"
        "tau" -> "tau"
        "rho" -> "rho"
        "xi" -> "xi"
        "zeta" -> "zeta"
        "eta" -> "eta"
        "iota" -> "iota"
        "kappa" -> "kappa"
        "nu" -> "nu"
        "upsilon" -> "upsilon"
        "Gamma" -> "capital gamma"
        "Delta" -> "capital delta"
        "Theta" -> "capital theta"
        "Lambda" -> "capital lambda"
        "Xi" -> "capital xi"
        "Pi" -> "capital pi"
        "Sigma" -> "capital sigma"
        "Upsilon" -> "capital upsilon"
        "Phi" -> "capital phi"
        "Psi" -> "capital psi"
        "Omega" -> "capital omega"
        "approx" -> "is approximately equal to"
        "equiv" -> "is equivalent to"
        // Set operators
        "in" -> "in"
        "notin" -> "not in"
        "subset" -> "is a subset of"
        "supset" -> "is a superset of"
        "cup" -> "union"
        "cap" -> "intersection"
        "emptyset" -> "empty set"
        // Logic symbols
        "forall" -> "for all"
        "exists" -> "there exists"
        "neg" -> "not"
        "land" -> "and"
        "lor" -> "or"
        "implies" -> "implies"
        "iff" -> "if and only if"
        // Arrows
        "rightarrow" -> "goes to"
        "leftarrow" -> "comes from"
        "Rightarrow" -> "implies"
        "Leftarrow" -> "is implied by"
        "Leftrightarrow" -> "if and only if"
        "mapsto" -> "maps to"
        // Floor/ceiling
        "lfloor" -> "floor of"
        "rfloor" -> "end floor"
        "lceil" -> "ceiling of"
        "rceil" -> "end ceiling"
        else -> node.name
    }

    private fun describeBinaryOp(node: FormulaNode.BinaryOp): String {
        val left = toSpeech(node.left)
        val right = toSpeech(node.right)
        val opWord = when (node.operator) {
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
            "cdot" -> "dot"
            "times" -> "times"
            "div" -> "divided by"
            "pm" -> "plus or minus"
            "mp" -> "minus or plus"
            "in" -> "in"
            "notin" -> "not in"
            "subset" -> "is a subset of"
            "supset" -> "is a superset of"
            "rightarrow" -> "goes to"
            "leftarrow" -> "comes from"
            "Rightarrow" -> "implies"
            "Leftarrow" -> "is implied by"
            "approx" -> "is approximately equal to"
            "equiv" -> "is equivalent to"
            "cup" -> "union"
            "cap" -> "intersection"
            "neg" -> "not"
            "land" -> "and"
            "lor" -> "or"
            "implies" -> "implies"
            "iff" -> "if and only if"
            "mapsto" -> "maps to"
            else -> node.operator
        }
        return "$left $opWord $right"
    }

    private fun describeFunction(node: FormulaNode.FunctionCall): String {
        val funcName = when (node.name) {
            "sin" -> "sine"
            "cos" -> "cosine"
            "tan" -> "tangent"
            "cot" -> "cotangent"
            "sec" -> "secant"
            "csc" -> "cosecant"
            "arcsin" -> "arc sine"
            "arccos" -> "arc cosine"
            "arctan" -> "arc tangent"
            "sinh" -> "hyperbolic sine"
            "cosh" -> "hyperbolic cosine"
            "tanh" -> "hyperbolic tangent"
            "log" -> "logarithm"
            "ln" -> "natural logarithm"
            "exp" -> "exponential"
            "abs" -> "absolute value"
            "min" -> "minimum"
            "max" -> "maximum"
            else -> node.name
        }
        return "$funcName of ${toSpeech(node.argument)}"
    }

    private fun describeFraction(node: FormulaNode.Fraction): String {
        return "${toSpeech(node.numerator)} over ${toSpeech(node.denominator)}"
    }

    private fun describeSqrt(node: FormulaNode.SquareRoot): String {
        return if (node.index != null) {
            "${toSpeech(node.index)} root of ${toSpeech(node.content)}"
        } else {
            "square root of ${toSpeech(node.content)}"
        }
    }

    private fun describePower(node: FormulaNode.Power): String {
        val base = toSpeech(node.base)
        val exp = toSpeech(node.exponent)
        return when {
            exp == "2" -> "$base squared"
            exp == "3" -> "$base cubed"
            else -> "$base to the power $exp"
        }
    }

    private fun describeSubscript(node: FormulaNode.Subscript): String {
        return "${toSpeech(node.base)} sub ${toSpeech(node.index)}"
    }

    private fun describeIntegral(node: FormulaNode.Integral): String {
        val lower = node.lower?.let { toSpeech(it) }
        val upper = node.upper?.let { toSpeech(it) }
        val body = toSpeech(node.integrand)
        val baseDesc = when {
            lower != null && upper != null ->
                "integral from $lower to $upper of $body"
            lower != null ->
                "integral from $lower of $body"
            upper != null ->
                "integral to $upper of $body"
            else ->
                "integral of $body"
        }
        return if (node.differential != null) {
            "$baseDesc with respect to ${toSpeech(node.differential)}"
        } else {
            baseDesc
        }
    }

    private fun describeSummation(node: FormulaNode.Summation): String {
        val lower = node.lower?.let { toSpeech(it) }
        val upper = node.upper?.let { toSpeech(it) }
        val body = toSpeech(node.term)
        return when {
            lower != null && upper != null ->
                "sum from $lower to $upper of $body"
            lower != null ->
                "sum from $lower of $body"
            upper != null ->
                "sum to $upper of $body"
            else ->
                "sum of $body"
        }
    }

    private fun describeLimit(node: FormulaNode.Limit): String {
        val variable = toSpeech(node.variable)
        val target = toSpeech(node.target)
        val body = toSpeech(node.body)
        return "limit as $variable approaches $target of $body"
    }

    private fun describeMatrix(node: FormulaNode.Matrix): String {
        val rows = node.entries.map { row ->
            row.joinToString(" comma ") { toSpeech(it) }
        }
        return "open matrix ${node.rows} by ${node.columns}: ${rows.joinToString(" semicolon ")} close matrix"
    }

    private fun describeCases(node: FormulaNode.Cases): String {
        val branches = node.branches.joinToString(" or ") { (condition, value) ->
            "${toSpeech(value)} if ${toSpeech(condition)}"
        }
        return "open cases: $branches close cases"
    }

    private fun describeProduct(node: FormulaNode.Product): String {
        val lower = node.lower?.let { toSpeech(it) }
        val upper = node.upper?.let { toSpeech(it) }
        val body = toSpeech(node.term)
        return when {
            lower != null && upper != null ->
                "product from $lower to $upper of $body"
            lower != null ->
                "product from $lower of $body"
            upper != null ->
                "product to $upper of $body"
            else ->
                "product of $body"
        }
    }

    private fun describeAccent(node: FormulaNode.Accent): String {
        val content = toSpeech(node.content)
        val accentWord = when (node.type) {
            FormulaNode.AccentType.HAT -> "hat"
            FormulaNode.AccentType.TILDE -> "tilde"
            FormulaNode.AccentType.BAR -> "bar"
            FormulaNode.AccentType.DOT -> "dot"
            FormulaNode.AccentType.DOTDOT -> "double dot"
            FormulaNode.AccentType.VEC -> "vector"
            FormulaNode.AccentType.OVERLINE -> "overline"
            FormulaNode.AccentType.UNDERLINE -> "underline"
        }
        return "$accentWord over $content"
    }
}
