package io.drishti.formula

import io.drishti.core.SymbolType

/**
 * Recursive descent parser that converts LaTeX math strings into [FormulaNode] ASTs.
 *
 * Supports: fractions, roots, integrals, summations, limits, Greek letters,
 * trigonometric / logarithmic functions, superscripts, subscripts, standard arithmetic,
 * absolute value, matrix/cases environments, and delimited groups via \left/\right.
 *
 * Usage:
 * ```
 * val ast = LatexParser.parse("\\frac{a}{b}")
 * val ast2 = LatexParser.parse("\\int_{0}^{1} x^{2} \\, dx")
 * ```
 */
object LatexParser {

    private const val MAX_BRACE_DEPTH = 50
    private const val MAX_UNARY_DEPTH = 50

    /**
     * Parse a LaTeX math string into a [FormulaNode] AST.
     *
     * @param latex LaTeX math expression (e.g. `\frac{1}{2}`, `x^{2}`)
     * @return Root AST node
     * @throws FormulaParseException if the input is empty, malformed, or cannot be parsed
     */
    fun parse(latex: String): FormulaNode {
        if (latex.isBlank()) {
            throw FormulaParseException("Cannot parse empty or blank LaTeX expression")
        }
        val tokens = tokenize(latex)
        val parser = ParserState(tokens)
        val node = parser.parseExpression()
        if (parser.peek() !is Token.EOF) {
            throw FormulaParseException(
                "Unexpected token '${parser.peek()}' at position ${parser.position}"
            )
        }
        return node
    }

    // ── Token types ───────────────────────────────────────────────────

    internal sealed class Token {
        data class Command(val name: String) : Token()
        data class Number(val value: String) : Token()
        data class Letter(val ch: Char) : Token()
        data class Other(val value: String) : Token()
        object LBrace : Token()
        object RBrace : Token()
        object LParen : Token()
        object RParen : Token()
        object LBracket : Token()
        object RBracket : Token()
        object Caret : Token()
        object Underscore : Token()
        object Comma : Token()
        object RowSep : Token()
        object ColumnSep : Token()
        object EOF : Token()
    }

    // ── Tokenizer ─────────────────────────────────────────────────────

    internal fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            when (val ch = input[i]) {
                '\\' -> {
                    i++
                    if (i < input.length && input[i] == '\\') {
                        tokens.add(Token.RowSep)
                        i++
                    } else if (i < input.length && input[i].isLetter()) {
                        val start = i
                        while (i < input.length && input[i].isLetter()) i++
                        tokens.add(Token.Command(input.substring(start, i)))
                    } else if (i < input.length) {
                        // Single-char command like \, or \;  (spacing) — skip
                        i++
                    }
                }
                '{' -> { tokens.add(Token.LBrace); i++ }
                '}' -> { tokens.add(Token.RBrace); i++ }
                '(' -> { tokens.add(Token.LParen); i++ }
                ')' -> { tokens.add(Token.RParen); i++ }
                '[' -> { tokens.add(Token.LBracket); i++ }
                ']' -> { tokens.add(Token.RBracket); i++ }
                '^' -> { tokens.add(Token.Caret); i++ }
                '_' -> { tokens.add(Token.Underscore); i++ }
                ',' -> { tokens.add(Token.Comma); i++ }
                '&' -> { tokens.add(Token.ColumnSep); i++ }
                in '0'..'9', '.' -> {
                    val start = i
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                    tokens.add(Token.Number(input.substring(start, i)))
                }
                in 'a'..'z', in 'A'..'Z' -> {
                    tokens.add(Token.Letter(ch)); i++
                }
                '+', '-', '*', '/', '=', '<', '>', '!' -> {
                    tokens.add(Token.Other(ch.toString())); i++
                }
                '|' -> {
                    tokens.add(Token.Other("|")); i++
                }
                ' ', '\t', '\n', '\r' -> i++ // skip whitespace
                else -> i++ // skip unknown characters
            }
        }
        tokens.add(Token.EOF)
        return tokens
    }

    // ── Parser ────────────────────────────────────────────────────────

    private class ParserState(private val tokens: List<Token>) {
        var position: Int = 0
        var braceDepth: Int = 0

        fun peek(): Token = tokens[position]

        fun advance(): Token {
            val tok = tokens[position]
            if (tok !is Token.EOF) position++
            return tok
        }

        fun expect(type: Token): Token {
            val tok = advance()
            if (tok::class != type::class) {
                throw FormulaParseException(
                    "Expected ${type::class.simpleName} but got $tok at position $position",
                    position
                )
            }
            return tok
        }

        fun atBlock(): Boolean = peek() is Token.LBrace

        fun parseBlock(): FormulaNode {
            advance() // consume {
            braceDepth++
            if (braceDepth > MAX_BRACE_DEPTH) {
                val depth = braceDepth
                braceDepth-- // restore before throwing
                throw FormulaParseException(
                    "Maximum brace nesting depth ($MAX_BRACE_DEPTH) exceeded at position $position",
                    position
                )
            }
            val nodes = mutableListOf<FormulaNode>()
            while (peek() !is Token.RBrace && peek() !is Token.EOF) {
                nodes.add(parseExpression())
            }
            if (peek() !is Token.RBrace) {
                braceDepth--
                throw FormulaParseException(
                    "Missing closing brace '}' at position $position",
                    position
                )
            }
            advance() // consume }
            braceDepth--
            return if (nodes.size == 1) nodes[0] else FormulaNode.Group(nodes)
        }

        fun parseGroup(): FormulaNode = parseBlock()

        // expression = comparison (('=' | '<' | '>' | '\\leq' | '\\geq') comparison)*
        fun parseExpression(): FormulaNode {
            var left = parseAddSub()
            while (true) {
                when (peek()) {
                    is Token.Other -> {
                        val op = (peek() as Token.Other).value
                        if (op == "=") {
                            advance()
                            val right = parseAddSub()
                            left = FormulaNode.BinaryOp("=", left, right)
                        } else break
                    }
                    is Token.Command -> {
                        val name = (peek() as Token.Command).name
                        if (name in RELATION_COMMANDS) {
                            advance()
                            val right = parseAddSub()
                            left = FormulaNode.BinaryOp(name, left, right)
                        } else break
                    }
                    else -> break
                }
            }
            return left
        }

        // addSub = mulDiv (('+' | '-') mulDiv)*
        fun parseAddSub(): FormulaNode {
            var left = parseMulDiv()
            while (true) {
                when (peek()) {
                    is Token.Other -> {
                        val op = (peek() as Token.Other).value
                        if (op == "+" || op == "-") {
                            advance()
                            val right = parseMulDiv()
                            left = FormulaNode.BinaryOp(op, left, right)
                        } else break
                    }
                    is Token.Command -> {
                        val name = (peek() as Token.Command).name
                        if (name == "cdot" || name == "times") {
                            advance()
                            val right = parseMulDiv()
                            left = FormulaNode.BinaryOp("*", left, right)
                        } else if (name == "div") {
                            advance()
                            val right = parseMulDiv()
                            left = FormulaNode.BinaryOp("/", left, right)
                        } else break
                    }
                    else -> break
                }
            }
            return left
        }

        // mulDiv = unary (('*' | '/' | '\cdot' | '\times' | '\div') unary)*
        fun parseMulDiv(): FormulaNode {
            var left = parseUnary()
            while (true) {
                when (peek()) {
                    is Token.Other -> {
                        val op = (peek() as Token.Other).value
                        if (op == "*" || op == "/") {
                            advance()
                            val right = parseUnary()
                            left = FormulaNode.BinaryOp(op, left, right)
                        } else break
                    }
                    is Token.Command -> {
                        val name = (peek() as Token.Command).name
                        if (name == "cdot" || name == "times") {
                            advance()
                            val right = parseUnary()
                            left = FormulaNode.BinaryOp("*", left, right)
                        } else if (name == "div") {
                            advance()
                            val right = parseUnary()
                            left = FormulaNode.BinaryOp("/", left, right)
                        } else break
                    }
                    else -> break
                }
            }
            return left
        }

        // unary = '-' unary | postfix
        fun parseUnary(): FormulaNode = parseUnaryWithDepth(0)

        private fun parseUnaryWithDepth(depth: Int): FormulaNode {
            if (peek() is Token.Other && (peek() as Token.Other).value == "-") {
                if (depth >= MAX_UNARY_DEPTH) {
                    throw FormulaParseException(
                        "Too many consecutive unary minus operators (max $MAX_UNARY_DEPTH) at position $position",
                        position
                    )
                }
                advance()
                return FormulaNode.UnaryMinus(parseUnaryWithDepth(depth + 1))
            }
            return parsePostfix()
        }

        // postfix = atom ('^' atom | '_' atom)*
        fun parsePostfix(): FormulaNode {
            var node = parseAtom()
            while (peek() is Token.Caret || peek() is Token.Underscore) {
                if (peek() is Token.Caret) {
                    advance()
                    val exp = parseAtom()
                    node = FormulaNode.Power(node, exp)
                } else if (peek() is Token.Underscore) {
                    advance()
                    val idx = parseAtom()
                    node = FormulaNode.Subscript(node, idx)
                }
            }
            return node
        }

        // atom = number | group | command | letter | '|'
        fun parseAtom(): FormulaNode {
            return when (val tok = peek()) {
                is Token.Number -> {
                    advance()
                    FormulaNode.Number(tok.value)
                }
                is Token.LParen -> {
                    advance()
                    val children = mutableListOf<FormulaNode>()
                    while (peek() !is Token.RParen && peek() !is Token.EOF) {
                        children.add(parseExpression())
                    }
                    if (peek() is Token.EOF) {
                        throw FormulaParseException(
                            "Missing closing parenthesis ')' at position $position",
                            position
                        )
                    }
                    advance() // consume )
                    if (children.size == 1) children[0] else FormulaNode.Group(children)
                }
                is Token.LBracket -> {
                    advance()
                    val children = mutableListOf<FormulaNode>()
                    while (peek() !is Token.RBracket && peek() !is Token.EOF) {
                        children.add(parseExpression())
                    }
                    if (peek() is Token.EOF) {
                        throw FormulaParseException(
                            "Missing closing bracket ']' at position $position",
                            position
                        )
                    }
                    advance() // consume ]
                    if (children.size == 1) children[0] else FormulaNode.Group(children)
                }
                is Token.LBrace -> parseBlock()
                is Token.Command -> parseCommand()
                is Token.Letter -> {
                    advance()
                    FormulaNode.Variable(tok.ch.toString())
                }
                is Token.Other -> {
                    if (tok.value == "|") {
                        advance()
                        val content = parseExpression()
                        if (peek() is Token.Other && (peek() as Token.Other).value == "|") {
                            advance() // consume closing |
                            FormulaNode.AbsoluteValue(content)
                        } else {
                            throw FormulaParseException(
                                "Missing closing '|' for absolute value at position $position",
                                position
                            )
                        }
                    } else {
                        // treat single-char operator as a variable (e.g., in unusual contexts)
                        advance()
                        FormulaNode.Variable(tok.value)
                    }
                }
                is Token.EOF -> throw FormulaParseException(
                    "Unexpected end of expression at position $position",
                    position
                )
                else -> throw FormulaParseException(
                    "Unexpected token '$tok' at position $position",
                    position
                )
            }
        }

        fun parseCommand(): FormulaNode {
            val tok = advance() as Token.Command
            return when (tok.name) {
                "frac" -> {
                    val num = parseBlock()
                    val den = parseBlock()
                    FormulaNode.Fraction(num, den)
                }
                "sqrt" -> {
                    val index = if (peek() is Token.LBracket) {
                        advance() // consume [
                        val idx = parseExpression()
                        expect(Token.RBracket) // consume and assert ]
                        idx
                    } else null
                    val content = parseBlock()
                    FormulaNode.SquareRoot(content, index)
                }
                "int" -> {
                    val lower = if (peek() is Token.Underscore) {
                        advance(); parseAtom()
                    } else null
                    val upper = if (peek() is Token.Caret) {
                        advance(); parseAtom()
                    } else null
                    val integrand = if (peek() !is Token.Command || (peek() as Token.Command).name != "int") {
                        parseMulDiv()
                    } else FormulaNode.Number("1")
                    
                    var differential: FormulaNode? = null
                    if (peek() is Token.Letter && (peek() as Token.Letter).ch == 'd') {
                        val nextPos = position + 1
                        if (nextPos < tokens.size && tokens[nextPos] is Token.Letter) {
                            advance() // consume 'd'
                            val varTok = advance() as Token.Letter
                            differential = FormulaNode.Variable(varTok.ch.toString())
                        }
                    }
                    while (peek() is Token.Letter) advance()
                    FormulaNode.Integral(lower, upper, integrand, differential)
                }
                "sum" -> {
                    val lower = if (peek() is Token.Underscore) {
                        advance(); parseAtom()
                    } else null
                    val upper = if (peek() is Token.Caret) {
                        advance(); parseAtom()
                    } else null
                    val term = parseMulDiv()
                    FormulaNode.Summation(lower, upper, term)
                }
                "prod" -> {
                    val lower = if (peek() is Token.Underscore) {
                        advance()
                        if (peek() is Token.LBrace) parseGroup() else parseAtom()
                    } else null
                    val upper = if (peek() is Token.Caret) {
                        advance()
                        if (peek() is Token.LBrace) parseGroup() else parseAtom()
                    } else null
                    val term = parseMulDiv()
                    FormulaNode.Product(lower, upper, term)
                }
                "binom" -> {
                    val n = parseGroup()
                    val k = parseGroup()
                    FormulaNode.Binomial(n, k)
                }
                "mathbb" -> parseBlock()
                "hat" -> FormulaNode.Accent(FormulaNode.AccentType.HAT, parseAtom())
                "tilde" -> FormulaNode.Accent(FormulaNode.AccentType.TILDE, parseAtom())
                "bar" -> FormulaNode.Accent(FormulaNode.AccentType.BAR, parseAtom())
                "dot" -> FormulaNode.Accent(FormulaNode.AccentType.DOT, parseAtom())
                "ddot" -> FormulaNode.Accent(FormulaNode.AccentType.DOTDOT, parseAtom())
                "vec" -> FormulaNode.Accent(FormulaNode.AccentType.VEC, parseAtom())
                "overline" -> FormulaNode.Accent(FormulaNode.AccentType.OVERLINE, parseAtom())
                "underline" -> FormulaNode.Accent(FormulaNode.AccentType.UNDERLINE, parseAtom())
                "left" -> parseLeftRight()
                "lim" -> parseLimit()
                "begin" -> parseEnvironment()
                // Greek letters
                in GREEK_LETTERS -> {
                    FormulaNode.NamedSymbol(tok.name, SymbolType.GREEK_LETTER)
                }
                // Trig / log functions
                in FUNCTION_COMMANDS -> {
                    val arg = if (peek() is Token.LParen) {
                        advance() // (
                        val expr = parseExpression()
                        advance() // )
                        expr
                    } else {
                        parseUnary()
                    }
                    FormulaNode.FunctionCall(tok.name, arg)
                }
                // Special constants
                "infty" -> FormulaNode.NamedSymbol("infty", SymbolType.VARIABLE)
                "partial" -> FormulaNode.NamedSymbol("partial", SymbolType.VARIABLE)
                "nabla" -> FormulaNode.NamedSymbol("nabla", SymbolType.VARIABLE)
                "to", "rightarrow", "leftarrow", "Rightarrow", "Leftarrow",
                "cdot", "times", "div",
                "leq", "geq", "neq", "pm", "mp",
                "in", "notin", "subset", "supset",
                "forall", "exists",
                "emptyset",
                "approx", "equiv", "cup", "cap", "neg", "land", "lor", "implies", "iff", "mapsto",
                "lfloor", "rfloor", "lceil", "rceil" -> {
                    FormulaNode.NamedSymbol(tok.name, SymbolType.OPERATOR)
                }
                "end" -> throw FormulaParseException(
                    "Unexpected \\end without matching \\begin at position $position",
                    position
                )
                else -> FormulaNode.NamedSymbol(tok.name, SymbolType.VARIABLE)
            }
        }

        // ── \left / \right ─────────────────────────────────────────────

        private fun parseLeftRight(): FormulaNode {
            advance() // consume the left delimiter char
            val children = mutableListOf<FormulaNode>()
            var matched = false
            while (peek() !is Token.EOF) {
                if (peek() is Token.Command && (peek() as Token.Command).name == "right") {
                    advance() // consume \right
                    advance() // consume the right delimiter char
                    matched = true
                    break
                }
                children.add(parseExpression())
            }
            if (!matched) {
                throw FormulaParseException(
                    "Unmatched \\left without corresponding \\right at position $position",
                    position
                )
            }
            return FormulaNode.Group(children)
        }

        // ── \lim ───────────────────────────────────────────────────────

        private fun parseLimit(): FormulaNode {
            val variable: FormulaNode
            val target: FormulaNode
            if (peek() is Token.Underscore) {
                advance()
                if (peek() is Token.LBrace) {
                    advance() // {
                    variable = parseAtom()
                    // consume \to or \rightarrow
                    if (peek() is Token.Command &&
                        ((peek() as Token.Command).name == "to" ||
                            (peek() as Token.Command).name == "rightarrow")
                    ) {
                        advance()
                    }
                    target = parseAtom()
                    advance() // }
                } else {
                    variable = parseAtom()
                    if (peek() is Token.Command &&
                        ((peek() as Token.Command).name == "to" ||
                            (peek() as Token.Command).name == "rightarrow")
                    ) {
                        advance()
                    }
                    target = parseAtom()
                }
            } else {
                variable = FormulaNode.Variable("x")
                target = FormulaNode.NamedSymbol("infty", SymbolType.VARIABLE)
            }
            val body = parseMulDiv()
            return FormulaNode.Limit(variable, target, body)
        }

        // ── \begin{env}...\end{env} ───────────────────────────────────

        private fun readEnvName(): String {
            val name = StringBuilder()
            while (peek() is Token.Letter) {
                name.append((advance() as Token.Letter).ch)
            }
            if (peek() is Token.RBrace) {
                advance() // consume }
            }
            return name.toString()
        }

        private fun parseEnvironment(): FormulaNode {
            // We need to read {envName}
            if (peek() !is Token.LBrace) {
                throw FormulaParseException(
                    "Expected '{' after \\begin at position $position",
                    position
                )
            }
            advance() // consume {
            val envName = readEnvName()

            val result = when (envName) {
                "matrix", "bmatrix", "pmatrix", "vmatrix", "Bmatrix", "Vmatrix" -> {
                    val entries = parseEnvironmentEntries()
                    val rows = entries.size
                    val cols = entries.maxOfOrNull { it.size } ?: 0
                    FormulaNode.Matrix(rows, cols, entries)
                }
                "cases" -> {
                    val entries = parseEnvironmentEntries()
                    val branches = entries.map { row ->
                        val value = row.getOrElse(0) { FormulaNode.Number("0") }
                        val condition = row.getOrElse(1) { FormulaNode.Number("1") }
                        condition to value
                    }
                    FormulaNode.Cases(branches)
                }
                else -> throw FormulaParseException(
                    "Unsupported LaTeX environment '\\begin{$envName}' at position $position",
                    position
                )
            }

            // Consume \end{envName}
            if (peek() is Token.Command && (peek() as Token.Command).name == "end") {
                advance() // consume \end
                if (peek() is Token.LBrace) {
                    advance() // consume {
                    val endName = readEnvName()
                    if (endName != envName) {
                        throw FormulaParseException(
                            "Mismatched environment: \\begin{$envName} closed by \\end{$endName} at position $position",
                            position
                        )
                    }
                }
            } else {
                throw FormulaParseException(
                    "Missing \\end{$envName} at position $position",
                    position
                )
            }

            return result
        }

        private fun parseEnvironmentEntries(): List<List<FormulaNode>> {
            val rows = mutableListOf<List<FormulaNode>>()
            var currentRow = mutableListOf<FormulaNode>()

            while (true) {
                when {
                    peek() is Token.EOF -> break
                    peek() is Token.Command && (peek() as Token.Command).name == "end" -> break
                    peek() is Token.RowSep -> {
                        advance()
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                    }
                    peek() is Token.ColumnSep -> {
                        advance()
                    }
                    else -> {
                        currentRow.add(parseExpression())
                    }
                }
            }

            if (currentRow.isNotEmpty()) {
                rows.add(currentRow)
            }
            return rows
        }
    }

    // ── Command sets ──────────────────────────────────────────────────

    internal val GREEK_LETTERS = setOf(
        "alpha", "beta", "gamma", "delta", "epsilon", "varepsilon",
        "zeta", "eta", "theta", "vartheta", "iota", "kappa",
        "lambda", "mu", "nu", "xi", "pi", "varpi", "rho",
        "varrho", "sigma", "varsigma", "tau", "upsilon", "phi",
        "varphi", "chi", "psi", "omega",
        "Gamma", "Delta", "Theta", "Lambda", "Xi", "Pi",
        "Sigma", "Upsilon", "Phi", "Psi", "Omega"
    )

    internal val FUNCTION_COMMANDS = setOf(
        "sin", "cos", "tan", "cot", "sec", "csc",
        "arcsin", "arccos", "arctan",
        "sinh", "cosh", "tanh", "coth",
        "log", "ln", "exp", "lg",
        "min", "max", "det", "dim", "ker"
    )

    private val RELATION_COMMANDS = setOf(
        "leq", "geq", "neq",
        "in", "notin", "subset", "supset",
        "rightarrow", "leftarrow", "Rightarrow", "Leftarrow",
        "approx", "equiv", "cup", "cap", "neg", "land", "lor", "implies", "iff", "mapsto",
        "lfloor", "rfloor", "lceil", "rceil"
    )
}
